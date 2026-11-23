package com.example.orvpomodoro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.orvpomodoro.MainActivity
import com.example.orvpomodoro.R
import com.example.orvpomodoro.engine.PomodoroEngine
import com.example.orvpomodoro.model.Phase
import com.example.orvpomodoro.model.PomodoroState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PomodoroService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var overlay: MiniOverlay
    private var appVisible = true

    // Sabe si la app está en pantalla: la mini ventana solo sale cuando no lo está
    private val processObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> appVisible = true
            Lifecycle.Event.ON_STOP -> appVisible = false
            else -> return@LifecycleEventObserver
        }
        refreshOverlay()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        PomodoroEngine.init(this)
        createChannels()
        overlay = MiniOverlay(this)
        val process = ProcessLifecycleOwner.get().lifecycle
        appVisible = process.currentState.isAtLeast(Lifecycle.State.STARTED)
        process.addObserver(processObserver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android exige llamar a startForeground enseguida
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_TIMER, buildTimerNotification(PomodoroEngine.state.value), type)
        acquireWakeLock()
        observeEngine()
        return START_NOT_STICKY
    }

    private fun observeEngine() {
        observeJob?.cancel()
        observeJob = scope.launch {
            launch {
                PomodoroEngine.state
                    .map { Triple(it.isRunning, it.phase, it.endAtMs) }
                    .distinctUntilChanged()
                    .collect { (running, _, _) ->
                        if (!running) {
                            stopSelf()   // nada que mantener vivo
                        } else {
                            getSystemService(NotificationManager::class.java)
                                .notify(NOTIF_TIMER, buildTimerNotification(PomodoroEngine.state.value))
                            refreshOverlay()
                        }
                    }
            }
            launch {
                PomodoroEngine.phaseEnded.collect { phase ->
                    showPhaseEndAlert(phase)
                    Haptics.play(this@PomodoroService, PomodoroEngine.state.value.vibrationLevel)
                }
            }
            launch {
                PomodoroEngine.state
                    .map { it.miniEnabled }
                    .distinctUntilChanged()
                    .collect { refreshOverlay() }
            }
            launch {
                PomodoroEngine.markReached.collect {
                    Haptics.tick(this@PomodoroService, PomodoroEngine.state.value.vibrationLevel)
                }
            }
        }
    }

    private fun refreshOverlay() {
        val s = PomodoroEngine.state.value
        val show = s.isRunning && s.miniEnabled && !appVisible && Settings.canDrawOverlays(this)
        if (show) overlay.show() else overlay.hide()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "orvpomodoro:timer").apply {
            acquire(4 * 60 * 60 * 1000L)   // tope de seguridad: 4 h
        }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun buildTimerNotification(s: PomodoroState): Notification {
        val constellation = s.constellation
        val remainingMs = (s.endAtMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        val title = if (s.phase == Phase.FOCUS) "Escenario #${s.completedScenarios + 1} en curso" else s.phase.label

        return NotificationCompat.Builder(this, CH_TIMER)
            .setSmallIcon(R.drawable.ic_stat_star)
            .setColor(constellation.color.toArgb())
            .setContentTitle(title)
            .setContentText("Te observa: ${constellation.name}")
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Cuenta atrás que dibuja el sistema solo, sin gastar batería
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(System.currentTimeMillis() + remainingMs)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun showPhaseEndAlert(phase: Phase) {
        val text = if (phase == Phase.FOCUS)
            "Escenario superado. Las constelaciones celebran."
        else
            "El descanso terminó. Un nuevo escenario te espera."

        val notification = NotificationCompat.Builder(this, CH_ALERT)
            .setSmallIcon(R.drawable.ic_stat_star)
            .setContentTitle(phase.label)
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // Antes: .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setDefaults(NotificationCompat.DEFAULT_SOUND)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIF_ALERT, notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < 26) return

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_TIMER, "Temporizador activo", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Muestra el escenario en curso"
            }
        )
        nm.deleteNotificationChannel("alert")   // el canal viejo vibraba por su cuenta
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Fin de fase", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Avisa cuando termina un escenario o un descanso"
                enableVibration(false)           // la vibración se configura dentro de la app
            }
        )
    }

    override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
        overlay.hide()
        wakeLock?.let { if (it.isHeld) it.release() }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CH_TIMER = "timer"
        private const val CH_ALERT = "alert_v2"      // antes: "alert"
        private const val NOTIF_TIMER = 1
        private const val NOTIF_ALERT = 2

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, PomodoroService::class.java))
        }
    }
}