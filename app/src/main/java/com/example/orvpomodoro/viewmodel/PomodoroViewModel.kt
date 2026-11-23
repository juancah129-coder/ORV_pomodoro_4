package com.example.orvpomodoro.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.orvpomodoro.engine.PomodoroEngine
import com.example.orvpomodoro.model.Durations
import com.example.orvpomodoro.model.PomodoroState
import com.example.orvpomodoro.model.ShopItem
import com.example.orvpomodoro.service.PomodoroService
import kotlinx.coroutines.flow.StateFlow
import com.example.orvpomodoro.model.VibrationLevel

class PomodoroViewModel(app: Application) : AndroidViewModel(app) {

    init {
        PomodoroEngine.init(app)   // carga lo guardado antes de dibujar nada
    }

    val state: StateFlow<PomodoroState> = PomodoroEngine.state

    fun toggle() {
        val wasRunning = PomodoroEngine.state.value.isRunning
        PomodoroEngine.toggle()
        if (!wasRunning && PomodoroEngine.state.value.isRunning) {
            PomodoroService.start(getApplication())
        }
    }

    fun reset() = PomodoroEngine.reset()
    fun skip() = PomodoroEngine.skip()
    fun updateDurations(new: Durations) = PomodoroEngine.updateDurations(new)
    fun buy(item: ShopItem) = PomodoroEngine.buy(item)
    fun buyCosmetic(id: String) = PomodoroEngine.buyCosmetic(id)
    fun equip(id: String) = PomodoroEngine.equip(id)
    fun setMiniEnabled(enabled: Boolean) = PomodoroEngine.setMiniEnabled(enabled)
    fun setVibrationLevel(level: VibrationLevel) = PomodoroEngine.setVibrationLevel(level)
    fun toggleMark(index: Int) = PomodoroEngine.toggleMark(index)
    fun clearMarks() = PomodoroEngine.clearMarks()
}