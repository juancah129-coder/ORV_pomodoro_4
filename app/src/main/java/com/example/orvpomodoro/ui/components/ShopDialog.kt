package com.example.orvpomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.orvpomodoro.model.ConstellationTheme
import com.example.orvpomodoro.model.Cosmetics
import com.example.orvpomodoro.model.PomodoroState
import com.example.orvpomodoro.model.ShopItem
import com.example.orvpomodoro.ui.theme.Abismo
import com.example.orvpomodoro.ui.theme.HiloDorado
import com.example.orvpomodoro.ui.theme.Papel
import com.example.orvpomodoro.ui.theme.Vacio

private enum class ShopTab(val label: String) {
    ITEMS("Objetos"),
    THEMES("Temáticas"),
    PALETTES("Colores"),
    FLARES("Destellos")
}

@Composable
fun ShopDialog(
    state: PomodoroState,
    accent: Color,
    onBuyItem: (ShopItem) -> Unit,
    onBuyCosmetic: (String) -> Unit,
    onEquip: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tab by remember { mutableStateOf(ShopTab.ITEMS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp),
        containerColor = Abismo,
        title = {
            Column {
                Text("Tienda de monedas", color = Papel)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tienes ${state.coins} monedas",
                    color = HiloDorado,
                    fontFamily = FontFamily.Serif,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column {
                ScrollableTabRow(
                    selectedTabIndex = tab.ordinal,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = accent,
                    divider = {}
                ) {
                    ShopTab.entries.forEach { t ->
                        Tab(
                            selected = tab == t,
                            onClick = { tab = t },
                            text = { Text(t.label, fontSize = 14.sp) },
                            selectedContentColor = accent,
                            unselectedContentColor = Papel.copy(alpha = 0.65f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    when (tab) {
                        ShopTab.ITEMS -> ShopItem.entries.forEach { item ->
                            val lock = state.lockReason(item)
                            ShopRow(
                                title = item.label,
                                description = item.description,
                                footer = if (item == ShopItem.SHIELD)
                                    "${item.cost} monedas, tienes ${state.shields}"
                                else
                                    "${item.cost} monedas",
                                buttonText = lock ?: "Comprar",
                                buttonEnabled = lock == null,
                                accent = accent,
                                onClick = { onBuyItem(item) }
                            )
                        }

                        ShopTab.THEMES -> Cosmetics.themes.forEach { theme ->
                            CosmeticRow(
                                id = theme.id,
                                title = theme.name,
                                description = theme.description,
                                cost = theme.cost,
                                state = state,
                                accent = accent,
                                onBuy = onBuyCosmetic,
                                onEquip = onEquip,
                                extra = { ThemeLadder(theme) }
                            )
                        }

                        ShopTab.PALETTES -> Cosmetics.palettes.forEach { palette ->
                            // La paleta original muestra los colores de la temática equipada
                            val dots = palette.colors
                                ?: Cosmetics.theme(state.equippedTheme).tiers.map { it.color }
                            CosmeticRow(
                                id = palette.id,
                                title = palette.name,
                                description = palette.description,
                                cost = palette.cost,
                                state = state,
                                accent = accent,
                                onBuy = onBuyCosmetic,
                                onEquip = onEquip,
                                extra = { ColorDots(dots) }
                            )
                        }

                        ShopTab.FLARES -> Cosmetics.flares.forEach { flare ->
                            CosmeticRow(
                                id = flare.id,
                                title = flare.name,
                                description = flare.description,
                                cost = flare.cost,
                                state = state,
                                accent = accent,
                                onBuy = onBuyCosmetic,
                                onEquip = onEquip,
                                leading = {
                                    Canvas(Modifier.size(56.dp)) {
                                        drawFlare(
                                            style = flare.style,
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            v = 0.5f,
                                            color = accent
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar", color = accent) }
        }
    )
}

@Composable
private fun CosmeticRow(
    id: String,
    title: String,
    description: String,
    cost: Int,
    state: PomodoroState,
    accent: Color,
    onBuy: (String) -> Unit,
    onEquip: (String) -> Unit,
    leading: (@Composable () -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null
) {
    val owned = state.isOwned(id)
    val equipped = state.isEquipped(id)
    val lock = if (owned) null else state.buyLock(id)

    ShopRow(
        title = title,
        description = description,
        footer = when {
            equipped -> "En uso"
            owned -> "Ya lo tienes"
            else -> "$cost monedas"
        },
        buttonText = when {
            equipped -> "Equipado"
            owned -> "Equipar"
            else -> lock ?: "Comprar"
        },
        buttonEnabled = !equipped && (owned || lock == null),
        accent = accent,
        onClick = { if (owned) onEquip(id) else onBuy(id) },
        leading = leading,
        extra = extra
    )
}

@Composable
private fun ShopRow(
    title: String,
    description: String,
    footer: String?,
    buttonText: String,
    buttonEnabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null
) {
    Column {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = Papel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    description,
                    color = Papel.copy(alpha = 0.75f),
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
                if (footer != null) {
                    Text(footer, color = HiloDorado, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onClick,
                enabled = buttonEnabled,
                shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Vacio,
                    disabledContainerColor = Papel.copy(alpha = 0.12f),
                    disabledContentColor = Papel.copy(alpha = 0.65f)
                )
            ) {
                Text(buttonText, fontSize = 13.sp)
            }
        }
        extra?.invoke()
    }
}

/** Las 7 constelaciones de una temática, de menos a más estrellas. */
@Composable
private fun ThemeLadder(theme: ConstellationTheme) {
    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        theme.tiers.forEach { c ->
            val label = c.detail?.let { "${c.name}, $it" } ?: c.name
            Text(
                text = "${c.points.size} estrellas: $label",
                color = c.color,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ColorDots(colors: List<Color>) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        colors.forEach { c ->
            Box(
                Modifier
                    .size(14.dp)
                    .background(c, CircleShape)
            )
        }
    }
}