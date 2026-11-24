package com.example.olahragakuy.pages.steptracker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton state holder ringan berbasis [StateFlow] sebagai jembatan sinkronisasi
 * real-time antara [StepTrackerService] (atau fallback *foreground-only* di UI)
 * dengan layar Compose `StepTrackerScreen`.
 */
object StepTrackerStateHolder {

    private val _liveState = MutableStateFlow(StepTrackerLiveState())
    val liveState: StateFlow<StepTrackerLiveState> = _liveState.asStateFlow()

    private val _completedSession = MutableStateFlow<StepSessionRecord?>(null)
    val completedSession: StateFlow<StepSessionRecord?> = _completedSession.asStateFlow()

    /**
     * Memperbarui [liveState] secara atomik dan thread-safe.
     */
    fun updateLiveState(transform: (StepTrackerLiveState) -> StepTrackerLiveState) {
        _liveState.update(transform)
    }

    /**
     * Menetapkan snapshot [StepTrackerLiveState] terbaru secara langsung.
     */
    fun setLiveState(state: StepTrackerLiveState) {
        _liveState.value = state
    }

    /**
     * Menerbitkan sesi yang baru selesai agar UI Compose segera memunculkan
     * Sheet Ringkasan Sesi (baik dihentikan dari tombol UI maupun tombol notifikasi).
     */
    fun publishCompletedSession(record: StepSessionRecord) {
        _completedSession.value = record
        _liveState.update { current ->
            current.copy(lastCompletedSession = record)
        }
    }

    /**
     * Menandai event sesi selesai telah dikonsumsi oleh UI.
     */
    fun consumeCompletedSession() {
        _completedSession.value = null
    }

    /**
     * Mengembalikan [liveState] ke status [StepTrackingStatus.IDLE] secara thread-safe.
     */
    fun resetToIdle(preserveCurrentLocation: Boolean = true) {
        _liveState.update { current ->
            StepTrackerLiveState(
                status = StepTrackingStatus.IDLE,
                currentLocation = if (preserveCurrentLocation) current.currentLocation else null,
                lastCompletedSession = current.lastCompletedSession,
            )
        }
    }
}
