package com.ceharp.ponto.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ceharp.ponto.PontoViewModel
import com.ceharp.ponto.data.formatarMinutos
import java.time.DayOfWeek
import java.time.format.TextStyle

// Ordem de exibição começando no domingo, como no calendário.
private val diasDaSemana = listOf(DayOfWeek.SUNDAY) + DayOfWeek.values().filter { it != DayOfWeek.SUNDAY }

@Composable
fun TelaAjustes(vm: PontoViewModel) {
    val config = vm.config
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Jornada", style = MaterialTheme.typography.titleMedium)
                Ajustador(
                    rotulo = "Jornada diária",
                    valor = formatarMinutos(config.jornadaMin),
                    onMenos = { vm.salvarConfig(config.copy(jornadaMin = (config.jornadaMin - 15).coerceAtLeast(15))) },
                    onMais = { vm.salvarConfig(config.copy(jornadaMin = (config.jornadaMin + 15).coerceAtMost(16 * 60))) },
                )
                Ajustador(
                    rotulo = "Tolerância",
                    valor = "${config.toleranciaMin} min",
                    onMenos = { vm.salvarConfig(config.copy(toleranciaMin = (config.toleranciaMin - 5).coerceAtLeast(0))) },
                    onMais = { vm.salvarConfig(config.copy(toleranciaMin = (config.toleranciaMin + 5).coerceAtMost(60))) },
                )
                Text(
                    "Diferenças dentro da tolerância contam como jornada cumprida no calendário.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dias de trabalho", style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    diasDaSemana.forEach { d ->
                        val marcado = d in config.diasUteis
                        FilterChip(
                            selected = marcado,
                            onClick = {
                                val novos = if (marcado) config.diasUteis - d else config.diasUteis + d
                                vm.salvarConfig(config.copy(diasUteis = novos))
                            },
                            label = { Text(d.getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "")) },
                        )
                    }
                }
                Text(
                    "Dias desmarcados não geram horas esperadas (não contam falta).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Como usar", style = MaterialTheme.typography.titleMedium)
                Text("• Na aba Hoje, toque no botão para registrar entrada e saída (inclusive almoço).")
                Text("• No Calendário, toque em um dia para corrigir batidas, marcar folga, feriado, férias ou atestado.")
                Text("• Exporte o relatório do mês em CSV pelo botão no fim do calendário.")
                Text(
                    "Os dados ficam salvos apenas neste aparelho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Ajustador(rotulo: String, valor: String, onMenos: () -> Unit, onMais: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(rotulo, Modifier.weight(1f))
        FilledTonalButton(onClick = onMenos) { Text("−") }
        Text(
            valor,
            Modifier.width(80.dp),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
        FilledTonalButton(onClick = onMais) { Text("+") }
    }
}
