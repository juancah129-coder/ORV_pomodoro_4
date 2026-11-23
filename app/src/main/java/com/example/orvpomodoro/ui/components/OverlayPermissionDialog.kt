package com.example.orvpomodoro.ui.components

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orvpomodoro.ui.theme.Abismo
import com.example.orvpomodoro.ui.theme.Papel
import com.example.orvpomodoro.ui.theme.TintaSistema

@Composable
fun OverlayPermissionDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
        containerColor = Abismo,
        title = { Text("Constelación flotante", color = Papel) },
        text = {
            Text(
                "Para ver el avance de tu constelación en una mini ventana cuando salgas " +
                        "de la app, Android necesita el permiso «Mostrar sobre otras apps». " +
                        "Se activa en una pantalla de ajustes.",
                color = Papel.copy(alpha = 0.85f),
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) { Text("Activar", color = TintaSistema) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ahora no", color = Papel.copy(alpha = 0.8f)) }
        }
    )
}