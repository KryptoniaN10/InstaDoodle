package com.instadoodle.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Sends one planned stroke to the system; returns false if it was rejected outright. */
fun interface GestureDispatcher {
    fun dispatch(stroke: PlannedStroke, onCompleted: () -> Unit, onCancelled: () -> Unit): Boolean
}

/** Delayed execution on the main thread (a Handler in production, a fake in tests). */
interface Scheduler {
    fun schedule(delayMs: Long, block: () -> Unit)
    fun cancelAll()
}

enum class PlayerState { IDLE, PLAYING, PAUSED, FINISHED, STOPPED }

data class PlayerProgress(val state: PlayerState, val done: Int, val total: Int, val skipped: Int = 0)

/**
 * Replays strokes one gesture at a time, chaining the next stroke from the completion
 * callback. Cancelled strokes are retried up to [maxRetries] times, then skipped.
 * A cancel usually means a real finger touched the screen, so the next dispatch waits
 * [cancelBackoffMs] to let that tap (e.g. on Pause/Stop) finish instead of cancelling it.
 * A generation counter makes callbacks from stopped/restarted runs harmless.
 * Not thread-safe: call from the main thread only.
 */
class GesturePlayer(
    private val dispatcher: GestureDispatcher,
    private val scheduler: Scheduler,
    private val maxRetries: Int = 2,
    private val cancelBackoffMs: Long = 400,
) {
    private val _progress = MutableStateFlow(PlayerProgress(PlayerState.IDLE, 0, 0))
    val progress: StateFlow<PlayerProgress> = _progress.asStateFlow()

    private var strokes: List<PlannedStroke> = emptyList()
    private var index = 0
    private var retries = 0
    private var skipped = 0
    private var generation = 0
    private var interStrokeDelayMs = 0L
    private var inFlight = false
    private var state = PlayerState.IDLE

    fun start(planned: List<PlannedStroke>, interStrokeDelayMs: Long) {
        generation++
        scheduler.cancelAll()
        strokes = planned
        this.interStrokeDelayMs = interStrokeDelayMs.coerceAtLeast(0)
        index = 0
        retries = 0
        skipped = 0
        inFlight = false
        setState(PlayerState.PLAYING)
        dispatchCurrent()
    }

    /** Stops after the in-flight stroke finishes; [resume] continues with the next one. */
    fun pause() {
        if (state != PlayerState.PLAYING) return
        scheduler.cancelAll()
        setState(PlayerState.PAUSED)
    }

    fun resume() {
        if (state != PlayerState.PAUSED) return
        setState(PlayerState.PLAYING)
        if (!inFlight) dispatchCurrent()
    }

    /** Drops every pending stroke immediately and ignores the in-flight stroke's callback. */
    fun stop() {
        if (state == PlayerState.IDLE) return
        generation++
        scheduler.cancelAll()
        inFlight = false
        setState(PlayerState.STOPPED)
    }

    private fun dispatchCurrent() {
        if (state != PlayerState.PLAYING) return
        if (index >= strokes.size) {
            setState(PlayerState.FINISHED)
            return
        }
        val gen = generation
        inFlight = true
        val accepted = dispatcher.dispatch(
            strokes[index],
            onCompleted = { if (gen == generation) onStrokeCompleted() },
            onCancelled = { if (gen == generation) onStrokeCancelled() },
        )
        if (!accepted && gen == generation) onStrokeCancelled()
    }

    private fun onStrokeCompleted() {
        inFlight = false
        index++
        retries = 0
        publish()
        scheduleNext()
    }

    private fun onStrokeCancelled() {
        inFlight = false
        if (retries < maxRetries) {
            retries++
        } else {
            index++
            retries = 0
            skipped++
            publish()
        }
        scheduleNext(maxOf(interStrokeDelayMs, cancelBackoffMs))
    }

    private fun scheduleNext(delayMs: Long = interStrokeDelayMs) {
        if (state != PlayerState.PLAYING) return
        val gen = generation
        scheduler.schedule(delayMs) { if (gen == generation) dispatchCurrent() }
    }

    private fun setState(newState: PlayerState) {
        state = newState
        publish()
    }

    private fun publish() {
        _progress.value = PlayerProgress(state, index, strokes.size, skipped)
    }
}
