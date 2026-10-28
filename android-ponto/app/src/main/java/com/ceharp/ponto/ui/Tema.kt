package com.ceharp.ponto.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.util.Locale

val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

private val Ouro = Color(0xFFF5C842)

private val Escuro = darkColorScheme(
    primary = Ouro,
    onPrimary = Color(0xFF1A1400),
    primaryContainer = Color(0xFF3A3010),
    onPrimaryContainer = Ouro,
    secondary = Color(0xFF38BDF8),
    background = Color(0xFF0B0F1A),
    onBackground = Color(0xFFE8EDF5),
    surface = Color(0xFF0B0F1A),
    onSurface = Color(0xFFE8EDF5),
    surfaceVariant = Color(0xFF1F2645),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0B0F1A),
    surfaceContainerLow = Color(0xFF141929),
    surfaceContainer = Color(0xFF141929),
    surfaceContainerHigh = Color(0xFF1A2035),
    surfaceContainerHighest = Color(0xFF1F2645),
    outline = Color(0xFF5A6780),
    error = Color(0xFFF87171),
)

private val Claro = lightColorScheme(
    primary = Color(0xFF8A6500),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE08A),
    onPrimaryContainer = Color(0xFF2A1F00),
    secondary = Color(0xFF0369A1),
)

@Composable
fun PontoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Escuro else Claro,
        content = content,
    )
}

/** Cores do calendário por situação do dia. */
object CoresStatus {
    val ok = Color(0xFF34D399)
    val extra = Color(0xFF38BDF8)
    val devendo = Color(0xFFFB923C)
    val incompleto = Color(0xFFF5C842)
    val falta = Color(0xFFF87171)
    val abonado = Color(0xFFA78BFA)
}
