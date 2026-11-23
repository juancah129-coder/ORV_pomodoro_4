package com.example.orvpomodoro.engine

import android.content.Context
import android.os.SystemClock
import com.example.orvpomodoro.model.CosmeticKind
import com.example.orvpomodoro.model.Cosmetics
import com.example.orvpomodoro.model.Durations
import com.example.orvpomodoro.model.Economy
import com.example.orvpomodoro.model.Flares
import com.example.orvpomodoro.model.Palettes
import com.example.orvpomodoro.model.Phase
import com.example.orvpomodoro.model.PomodoroState
import com.example.orvpomodoro.model.ShopItem
import com.example.orvpomodoro.model.Themes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.orvpomodoro.model.VibrationLevel

/**
 * Único dueño del temporizador. Vive mientras viva el proceso, así que
 * no depende de la pantalla ni del ViewModel.
 */
object PomodoroEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PomodoroState())
    val state: StateFlow<PomodoroState> = _state.asStateFlow()

    /** Se emite cuando una fase llega a 0 (el servicio lo usa para avisar). */
    private val _phaseEnded = MutableSharedFlow<Phase>(extraBufferCapacity = 4)
    val phaseEnded: SharedFlow<Phase> = _phaseEnded.asSharedFlow()
    /** Se emite con el índice de la estrella cuando el trazo llega a una estrella marcada. */
    private val _markReached = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val markReached: SharedFlow<Int> = _markReached.asSharedFlow()

    /** Marcas que ya vibraron en esta fase, para que no vibren dos veces. */
    private val firedMarks = mutableSetOf<Int>()

    private var timerJob: Job? = null
    private var initialized = false

    /** Carga lo guardado y empieza a guardar los cambios. Es seguro llamarla varias veces. */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val store = SaveStore(context.applicationContext)
        _state.update { store.load(it) }
        scope.launch {
            _state
                .map { it.toSaved() }
                .distinctUntilChanged()
                .collect { store.save(it) }
        }
    }

    fun toggle() {
        val s = _state.value
        if (s.isCelebrating) return
        if (s.isRunning) pause() else start()
    }

    private fun start() {
        val endAt = SystemClock.elapsedRealtime() + _state.value.remainingSeconds * 1000L
        _state.update {
            it.copy(
                isRunning = true,
                endAtMs = endAt,
                message = when (it.phase) {
                    Phase.FOCUS -> "[El escenario ha comenzado. Sobrevive a la distracción.]"
                    else -> "[Has entrado en una zona segura. Recupera fuerzas.]"
                }
            )
        }
        runTimer()
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                // La verdad es la hora de fin, no cuántas veces ha corrido el bucle
                val remainingMs = _state.value.endAtMs - SystemClock.elapsedRealtime()
                if (remainingMs <= 0L) break
                val secs = ((remainingMs + 999) / 1000).toInt()
                _state.update { if (it.remainingSeconds == secs) it else it.copy(remainingSeconds = secs) }
                checkMarks()      // ← NUEVO
                delay(remainingMs - (secs - 1) * 1000L)
            }
            finishPhase()
        }
    }

    private suspend fun finishPhase() {
        val phase = _state.value.phase
        _state.update {
            it.copy(
                remainingSeconds = 0,
                isCelebrating = true,
                message = if (phase == Phase.FOCUS)
                    "[Las constelaciones celebran tu escenario.]"
                else
                    "[La zona segura se desvanece.]"
            )
        }
        _phaseEnded.tryEmit(phase)
        delay(3000)   // trazo final + destello + un respiro
        goToNextPhase(rewardCoins = true)
    }

    private fun pause() {
        timerJob?.cancel()
        _state.update {
            val remaining = (((it.endAtMs - SystemClock.elapsedRealtime()) + 999) / 1000)
                .toInt().coerceAtLeast(0)
            it.copy(
                isRunning = false,
                endAtMs = 0L,
                remainingSeconds = remaining,
                message = "[El escenario está en pausa. Las constelaciones esperan.]"
            )
        }
    }

    /** Reiniciar vuelve a empezar la misma fase: no cuenta como abandono. */
    fun reset() {
        timerJob?.cancel()
        firedMarks.clear()      // ← NUEVO: al reiniciar, las marcas vuelven a vibrar
        _state.update {
            val seconds = it.durations.secondsOf(it.phase)
            it.copy(
                isRunning = false,
                isCelebrating = false,
                endAtMs = 0L,
                remainingSeconds = seconds,
                totalSeconds = seconds,
                message = "[El escenario se ha reiniciado.]"
            )
        }
    }

    /** Saltar un escenario cuenta como abandono: rompe la racha o gasta un escudo. */
    fun skip() {
        if (_state.value.isCelebrating) return
        timerJob?.cancel()
        goToNextPhase(rewardCoins = false)
    }

    /** Solo se permite con el temporizador detenido o en pausa. */
    /** Solo se permite con el temporizador detenido o en pausa. */
    fun updateDurations(new: Durations) {
        if (_state.value.isRunning) return
        _state.update {
            val seconds = new.secondsOf(it.phase)
            it.copy(
                durations = new,
                remainingSeconds = seconds,
                totalSeconds = seconds,
                markedStars = emptySet(),      // ← NUEVO: la constelación puede cambiar
                message = "[Los tiempos del escenario han sido reescritos.]"
            )
        }
        firedMarks.clear()
    }

    /** Muestra u oculta la mini constelación flotante. Se aplica al instante. */
    fun setMiniEnabled(enabled: Boolean) {
        _state.update { it.copy(miniEnabled = enabled) }
    }

    fun setVibrationLevel(level: VibrationLevel) {
        _state.update { it.copy(vibrationLevel = level) }
    }

    /** Marca o desmarca una estrella. Solo valen las intermedias que aún no se han alcanzado. */
    fun toggleMark(index: Int) {
        val s = _state.value
        val n = s.constellation.points.size
        val segments = n - 1
        val elapsed = s.totalSeconds - s.remainingSeconds
        val selectable = index in 1..(n - 2) && elapsed * segments < index * s.totalSeconds
        val isMarked = index in s.markedStars
        if (!isMarked && !selectable) return

        if (isMarked) firedMarks.remove(index)
        _state.update {
            it.copy(markedStars = if (isMarked) it.markedStars - index else it.markedStars + index)
        }
    }

    fun clearMarks() {
        firedMarks.clear()
        _state.update { it.copy(markedStars = emptySet()) }
    }

    /** Se llama en cada segundo: avisa de las marcas que el trazo ya alcanzó. */
    private fun checkMarks() {
        val s = _state.value
        if (s.markedStars.isEmpty()) return
        val n = s.constellation.points.size
        if (n < 2) return
        val segments = n - 1
        val elapsed = s.totalSeconds - s.remainingSeconds

        // La estrella i se alcanza cuando elapsed / total >= i / segments
        val due = s.markedStars.sorted().filter { i ->
            i !in firedMarks && elapsed * segments >= i * s.totalSeconds
        }
        if (due.isEmpty()) return

        firedMarks.addAll(due)
        due.forEach { _markReached.tryEmit(it) }
        _state.update { it.copy(message = "[Marca alcanzada: estrella ${due.last() + 1} de $n.]") }
    }

    fun buy(item: ShopItem) {
        if (_state.value.lockReason(item) != null) return
        _state.update {
            val paid = it.copy(coins = it.coins - item.cost)
            when (item) {
                ShopItem.EXTEND_BREAK -> paid.copy(
                    remainingSeconds = it.remainingSeconds + 300,
                    totalSeconds = it.totalSeconds + 300,
                    endAtMs = if (it.isRunning) it.endAtMs + 300_000L else 0L,
                    message = "[Prórroga concedida. El descanso se alarga 5 minutos.]"
                )
                ShopItem.BLESSING -> paid.copy(
                    blessingActive = true,
                    message = "[La constelación te bendice. Tus próximas monedas se duplican.]"
                )
                ShopItem.SHIELD -> paid.copy(
                    shields = it.shields + 1,
                    message = "[Escudo adquirido. Protegerá tu racha una vez.]"
                )
            }
        }
    }

    /** Compra un cosmético y lo equipa al instante. */
    fun buyCosmetic(id: String) {
        val s = _state.value
        val cost = Cosmetics.costOf(id) ?: return
        if (s.isOwned(id) || s.coins < cost) return
        _state.update { it.copy(coins = it.coins - cost, ownedIds = it.ownedIds + id) }
        equip(id)
    }

    fun equip(id: String) {
        if (id !in _state.value.ownedIds) return
        val name = Cosmetics.nameOf(id) ?: return
        _state.update {
            when (Cosmetics.kindOf(id)) {
                // Otra temática = otro número de estrellas, así que las marcas ya no valen
                CosmeticKind.THEME -> it.copy(
                    equippedTheme = id,
                    markedStars = emptySet(),
                    message = "[Temática equipada: $name.]"
                )
                CosmeticKind.PALETTE -> it.copy(equippedPalette = id, message = "[Colores equipados: $name.]")
                CosmeticKind.FLARE -> it.copy(equippedFlare = id, message = "[Destello equipado: $name.]")
                null -> it
            }
        }
        if (Cosmetics.kindOf(id) == CosmeticKind.THEME) firedMarks.clear()
    }

    private fun goToNextPhase(rewardCoins: Boolean) {
        _state.update { s ->
            val finishedFocus = s.phase == Phase.FOCUS
            val earned = finishedFocus && rewardCoins
            val abandoned = finishedFocus && !rewardCoins

            val completed = if (earned) s.completedScenarios + 1 else s.completedScenarios

            var streak = s.streak
            var shields = s.shields
            var shieldUsed = false
            var streakBroken = false

            if (earned) streak += 1
            if (abandoned && streak > 0) {
                if (shields > 0) {
                    shields -= 1
                    shieldUsed = true
                } else {
                    streak = 0
                    streakBroken = true
                }
            }

            val longBreak = earned && streak >= Economy.STREAK_FOR_LONG_BREAK
            if (longBreak) streak = 0

            val next = when {
                !finishedFocus -> Phase.FOCUS
                longBreak -> Phase.LONG_BREAK
                else -> Phase.SHORT_BREAK
            }

            val reward = Economy.scenarioReward(s.durations.focusMin) *
                    if (s.blessingActive) 2 else 1
            val seconds = s.durations.secondsOf(next)

            val message = when {
                earned -> buildString {
                    append(
                        if (s.blessingActive)
                            "[Escenario superado. La bendición duplica tu recompensa: $reward monedas.]"
                        else
                            "[Escenario superado. Se te otorgan $reward monedas.]"
                    )
                    if (longBreak) append(" [Racha completa. Te espera el Descanso de Constelaciones.]")
                }
                shieldUsed -> "[El escudo absorbió el abandono. Tu racha sigue intacta.]"
                streakBroken -> "[Escenario abandonado. Tu racha se rompió.]"
                next == Phase.FOCUS -> "[Un nuevo escenario está por comenzar.]"
                else -> "[Fase omitida.]"
            }

            s.copy(
                phase = next,
                remainingSeconds = seconds,
                totalSeconds = seconds,
                isRunning = false,
                isCelebrating = false,
                endAtMs = 0L,
                completedScenarios = completed,
                streak = streak,
                shields = shields,
                coins = if (earned) s.coins + reward else s.coins,
                blessingActive = if (earned) false else s.blessingActive,
                markedStars = emptySet(),     // ← NUEVO: las marcas valen solo para esa fase
                message = message
            )
        }
        firedMarks.clear()                    // ← NUEVO
    }

}


// ---------------------- Guardado ----------------------

/** Solo lo que merece guardarse (así no se escribe en disco cada segundo). */
private data class Saved(
    val focusMin: Int,
    val shortMin: Int,
    val longMin: Int,
    val completed: Int,
    val streak: Int,
    val coins: Int,
    val shields: Int,
    val blessing: Boolean,
    val owned: Set<String>,
    val theme: String,
    val palette: String,
    val flare: String,
    val mini: Boolean,
    val vibration: String          // ← NUEVO
)

private fun PomodoroState.toSaved() = Saved(
    focusMin = durations.focusMin,
    shortMin = durations.shortBreakMin,
    longMin = durations.longBreakMin,
    completed = completedScenarios,
    streak = streak,
    coins = coins,
    shields = shields,
    blessing = blessingActive,
    owned = ownedIds,
    theme = equippedTheme,
    palette = equippedPalette,
    flare = equippedFlare,
    mini = miniEnabled,
    vibration = vibrationLevel.name   // ← NUEVO
)

private class SaveStore(context: Context) {
    private val prefs = context.getSharedPreferences("orv_save", Context.MODE_PRIVATE)

    fun load(base: PomodoroState): PomodoroState {
        val durations = Durations(
            focusMin = prefs.getInt("focusMin", 25),
            shortBreakMin = prefs.getInt("shortMin", 5),
            longBreakMin = prefs.getInt("longMin", 15)

        )
        val owned = (prefs.getStringSet("owned", emptySet()) ?: emptySet()) + Cosmetics.defaultOwned

        // Si lo equipado ya no existe o no es tuyo, vuelve al valor por defecto
        fun pick(key: String, default: String): String {
            val id = prefs.getString(key, default) ?: default
            return if (id in owned) id else default
        }

        val seconds = durations.secondsOf(base.phase)
        return base.copy(
            durations = durations,
            remainingSeconds = seconds,
            totalSeconds = seconds,
            completedScenarios = prefs.getInt("completed", 0),
            streak = prefs.getInt("streak", 0),
            coins = prefs.getInt("coins", 0),
            shields = prefs.getInt("shields", 0),
            blessingActive = prefs.getBoolean("blessing", false),
            miniEnabled = prefs.getBoolean("mini", true),
            ownedIds = owned,
            equippedTheme = pick("theme", Themes.STAR_STREAM),
            equippedPalette = pick("palette", Palettes.ORIGINAL),
            equippedFlare = pick("flare", Flares.CLASSIC),
            vibrationLevel = runCatching {
                VibrationLevel.valueOf(prefs.getString("vibration", null) ?: "MEDIUM")
            }.getOrDefault(VibrationLevel.MEDIUM),
        )
    }

    fun save(s: Saved) {
        prefs.edit()
            .putInt("focusMin", s.focusMin)
            .putInt("shortMin", s.shortMin)
            .putInt("longMin", s.longMin)
            .putInt("completed", s.completed)
            .putInt("streak", s.streak)
            .putInt("coins", s.coins)
            .putInt("shields", s.shields)
            .putBoolean("blessing", s.blessing)
            .putStringSet("owned", s.owned)
            .putString("theme", s.theme)
            .putString("palette", s.palette)
            .putString("flare", s.flare)
            .putString("vibration", s.vibration)
            .putBoolean("mini", s.mini)
            .apply()

    }
}