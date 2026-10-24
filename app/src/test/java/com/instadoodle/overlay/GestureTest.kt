package com.instadoodle.overlay

import com.instadoodle.engine.Point
import com.instadoodle.engine.Stroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureMapperTest {
    private val timing = GestureTiming(speedPxPerSec = 1000f, minStrokeMs = 50, maxStrokeMs = 3000, dotMs = 40, maxGestureMs = 60_000)

    @Test
    fun mapsIntoCentredSquareWithOffset() {
        val rect = PlacementRect(100f, 200f, 400f, 200f)
        val planned = GestureMapper.map(listOf(Stroke(listOf(Point(0f, 0f), Point(1f, 1f)))), rect, 5f, -5f, timing)
        assertEquals(Point(205f, 195f), planned[0].points[0])
        assertEquals(Point(405f, 395f), planned[0].points[1])
    }

    @Test
    fun singlePointBecomesDotWithDotDuration() {
        val planned = GestureMapper.map(listOf(Stroke(listOf(Point(0.5f, 0.5f)))), PlacementRect(0f, 0f, 100f, 100f), 0f, 0f, timing)
        assertTrue(planned[0].isDot)
        assertEquals(40L, planned[0].durationMs)
    }

    @Test
    fun durationIsProportionalAndClamped() {
        assertEquals(500L, GestureMapper.durationFor(500f, timing))
        assertEquals(50L, GestureMapper.durationFor(10f, timing))
        assertEquals(3000L, GestureMapper.durationFor(100_000f, timing))
        assertEquals(1000L, GestureMapper.durationFor(100_000f, timing.copy(maxGestureMs = 1000)))
    }

    @Test
    fun negativeCoordinatesAreClamped() {
        val planned = GestureMapper.map(listOf(Stroke(listOf(Point(0f, 0f), Point(1f, 0f)))), PlacementRect(0f, 0f, 100f, 100f), -20f, -20f, timing)
        assertTrue(planned[0].points.all { it.x >= 0f && it.y >= 0f })
    }
}

class GesturePlayerTest {

    private class FakeScheduler : Scheduler {
        val queue = ArrayDeque<() -> Unit>()
        override fun schedule(delayMs: Long, block: () -> Unit) { queue.addLast(block) }
        override fun cancelAll() = queue.clear()
        fun runAll() { while (queue.isNotEmpty()) queue.removeFirst()() }
    }

    private class FakeDispatcher(val cancelFirst: Int = 0) : GestureDispatcher {
        var calls = 0
        var pending: Pair<() -> Unit, () -> Unit>? = null
        override fun dispatch(stroke: PlannedStroke, onCompleted: () -> Unit, onCancelled: () -> Unit): Boolean {
            calls++
            pending = onCompleted to onCancelled
            return true
        }
        fun complete() = pending!!.also { pending = null }.first()
        fun cancel() = pending!!.also { pending = null }.second()
    }

    private val strokes = List(3) { PlannedStroke(listOf(Point(0f, 0f), Point(1f, 1f)), 10) }

    @Test
    fun chainsStrokesUntilFinished() {
        val d = FakeDispatcher(); val s = FakeScheduler()
        val p = GesturePlayer(d, s)
        p.start(strokes, 0)
        repeat(3) { d.complete(); s.runAll() }
        assertEquals(PlayerProgress(PlayerState.FINISHED, 3, 3), p.progress.value)
        assertEquals(3, d.calls)
    }

    @Test
    fun retriesCancelledStrokeTwiceThenSkips() {
        val d = FakeDispatcher(); val s = FakeScheduler()
        val p = GesturePlayer(d, s)
        p.start(strokes.take(1), 0)
        repeat(3) { d.cancel(); s.runAll() }
        assertEquals(3, d.calls)
        assertEquals(PlayerState.FINISHED, p.progress.value.state)
        assertEquals(1, p.progress.value.skipped)
    }

    @Test
    fun pauseHoldsAndResumeContinues() {
        val d = FakeDispatcher(); val s = FakeScheduler()
        val p = GesturePlayer(d, s)
        p.start(strokes, 0)
        p.pause()
        d.complete(); s.runAll()
        assertEquals(1, d.calls)
        assertEquals(PlayerState.PAUSED, p.progress.value.state)
        p.resume()
        assertEquals(2, d.calls)
    }

    @Test
    fun stopDropsPendingAndIgnoresLateCallback() {
        val d = FakeDispatcher(); val s = FakeScheduler()
        val p = GesturePlayer(d, s)
        p.start(strokes, 0)
        p.stop()
        d.complete(); s.runAll()
        assertEquals(1, d.calls)
        assertEquals(PlayerProgress(PlayerState.STOPPED, 0, 3), p.progress.value)
    }
}
