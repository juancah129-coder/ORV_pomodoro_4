package com.example.orvpomodoro.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import com.example.orvpomodoro.service.Haptics
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.orvpomodoro.model.Economy
import com.example.orvpomodoro.model.Phase
import com.example.orvpomodoro.ui.components.ConstellationProgress
import com.example.orvpomodoro.ui.components.SettingsDialog
import com.example.orvpomodoro.ui.components.ShopDialog
import com.example.orvpomodoro.ui.components.StarField
import com.example.orvpomodoro.ui.theme.Abismo
import com.example.orvpomodoro.ui.theme.HiloDorado
import com.example.orvpomodoro.ui.theme.Papel
import com.example.orvpomodoro.ui.theme.Vacio
import com.example.orvpomodoro.viewmodel.PomodoroViewModel
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.orvpomodoro.ui.components.OverlayPermissionDialog
import com.example.orvpomodoro.ui.components.MarkDialog

private val WindowShape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp)
private val ButtonShape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)

@Composable
fun PomodoroScreen(vm: PomodoroViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var showOverlayDialog by remember { mutableStateOf(false) }
    var askedOverlay by rememberSaveable { mutableStateOf(false) }
    var showMarks by remember { mutableStateOf(false) }

    // Permiso para las notificaciones (Android 13 o superior)
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var showShop by remember { mutableStateOf(false) }

    // La constelación (y su color) dependen de los minutos de la fase actual
    val constellation = state.constellation
    val accent by animateColorAsState(
        targetValue = constellation.color,
        animationSpec = tween(600),
        label = "accent"
    )

    val elapsed = 1f - state.remainingSeconds / state.totalSeconds.toFloat()
    val timeText = "%02d:%02d".format(state.remainingSeconds / 60, state.remainingSeconds % 60)
    val windowTitle =
        if (state.phase == Phase.FOCUS) "Escenario #${state.completedScenarios + 1}"
        else state.phase.label

    val activeEffects = buildList {
        if (state.blessingActive) add("Bendición activa: monedas x2")
        if (state.shields > 0) add("Escudos: ${state.shields}")
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Vacio, Abismo)))
    ) {
        StarField(Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            SystemWindow(title = windowTitle, message = state.message, accent = accent)

            ConstellationProgress(
                elapsed = elapsed,
                points = constellation.points,
                color = accent,
                description = "${constellation.name}: ${(elapsed * 100).toInt()} por ciento completado",
                celebrate = state.isCelebrating,
                flareStyle = state.flareStyle,
                marks = state.markedStars,              // ← NUEVA
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Text(
                text = "Te observa: ${constellation.name}",
                color = accent,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp
            )

            Text(
                text = timeText,
                style = TextStyle(
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Light,
                    fontFeatureSettings = "tnum",
                    color = accent
                )
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    // La primera vez que se inicia, ofrece activar la mini ventana
                    // Antes: if (!state.isRunning && !askedOverlay && !Settings.canDrawOverlays(context)) {
                    if (state.miniEnabled && !state.isRunning && !askedOverlay && !Settings.canDrawOverlays(context)) {
                        askedOverlay = true
                        showOverlayDialog = true
                    }
                    vm.toggle()
                },
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Vacio),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    if (state.isRunning) "Pausar" else "Iniciar",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                QuietButton("Reiniciar", onClick = vm::reset)
                QuietButton("Saltar", onClick = vm::skip)
                QuietButton("Marcar") { showMarks = true }
                QuietButton("Tiempos") { showSettings = true }
            }

            if (activeEffects.isNotEmpty()) {
                Text(
                    text = activeEffects.joinToString(", "),
                    color = HiloDorado,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Stat("Escenarios", state.completedScenarios.toString(), Papel)
                Stat("Racha", "${state.streak}/${Economy.STREAK_FOR_LONG_BREAK}", accent)
                Stat("Monedas", state.coins.toString(), HiloDorado)
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = { showShop = true },
                    shape = ButtonShape,
                    border = BorderStroke(1.dp, HiloDorado.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HiloDorado)
                ) { Text("Tienda") }
            }
        }
        if (showMarks) {
            MarkDialog(
                state = state,
                accent = accent,
                onToggle = vm::toggleMark,
                onClear = vm::clearMarks,
                onDismiss = { showMarks = false }
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            current = state.durations,
            themeId = state.equippedTheme,
            paletteId = state.equippedPalette,
            locked = state.isRunning,
            miniEnabled = state.miniEnabled,
            onMiniEnabledChange = { enabled ->
                vm.setMiniEnabled(enabled)
                // Si lo activa y falta el permiso de Android, lo pedimos
                if (enabled && !Settings.canDrawOverlays(context)) showOverlayDialog = true
            },
            vibrationLevel = state.vibrationLevel,
            onVibrationLevelChange = { level ->
                vm.setVibrationLevel(level)
                Haptics.play(context, level)      // vibra al elegir, para que la sientas
            },
            initialPhase = state.phase,
            onDismiss = { showSettings = false },
            onSave = {
                vm.updateDurations(it)
                showSettings = false
            }
        )
    }

    if (showShop) {
        ShopDialog(
            state = state,
            accent = accent,
            onBuyItem = vm::buy,
            onBuyCosmetic = vm::buyCosmetic,
            onEquip = vm::equip,
            onDismiss = { showShop = false }
        )
    }
    if (showOverlayDialog) {
        OverlayPermissionDialog(
            onAccept = {
                showOverlayDialog = false
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            },
            onDismiss = { showOverlayDialog = false }
        )
    }
}

@Composable
private fun QuietButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = Papel.copy(alpha = 0.8f),
            disabledContentColor = Papel.copy(alpha = 0.3f)
        )
    ) { Text(text) }
}

@Composable
private fun SystemWindow(title: String, message: String, accent: Color) {
    Surface(
        color = Vacio.copy(alpha = 0.65f),
        shape = WindowShape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(title, color = accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                color = Papel,
                fontFamily = FontFamily.Serif,
                fontSize = 17.sp,
                lineHeight = 26.sp
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, valueColor: Color) {
    Column {
        Text(value, color = valueColor, fontFamily = FontFamily.Serif, fontSize = 26.sp)
        Text(label, color = Papel.copy(alpha = 0.65f), fontSize = 12.sp)
    }
}