package com.ceharp.ponto.ui

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceharp.ponto.PontoViewModel
import com.ceharp.ponto.data.Calculo
import com.ceharp.ponto.data.DiaRegistro
import com.ceharp.ponto.data.TipoDia
import com.ceharp.ponto.data.formatarHora
import com.ceharp.ponto.data.formatarMinutos
import java.time.LocalDate
import java.time.LocalTime

private fun escolherHora(contexto: Context, inicial: LocalTime, aoEscolher: (LocalTime) -> Unit) {
    TimePickerDialog(
        contexto,
        { _, h, m -> aoEscolher(LocalTime.of(h, m)) },
        inicial.hour,
        inicial.minute,
        true,
    ).show()
}

/** Edição manual de um dia: ajustar/incluir/remover batidas, tipo do dia e observação. */
@Composable
fun DialogoDia(vm: PontoViewModel, data: LocalDate, onFechar: () -> Unit) {
    val contexto = LocalContext.current
    val original = vm.dia(data)
    var batidas by remember(data) { mutableStateOf(original.batidas.sorted()) }
    var tipo by remember(data) { mutableStateOf(original.tipo) }
    var obs by remember(data) { mutableStateOf(original.obs) }

    val rascunho = DiaRegistro(data, batidas, tipo, obs)
    val trabalhado = Calculo.minutosTrabalhados(rascunho)
    val esperado = Calculo.minutosEsperados(data, tipo, vm.config)

    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text(dataPorExtenso(data), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Batidas", fontWeight = FontWeight.SemiBold)
                if (batidas.isEmpty()) {
                    Text("Nenhuma batida.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                batidas.forEachIndexed { i, hora ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (i % 2 == 0) "Entrada" else "Saída",
                            Modifier.weight(1f),
                            color = if (i % 2 == 0) CoresStatus.ok else CoresStatus.devendo,
                        )
                        TextButton(onClick = {
                            escolherHora(contexto, hora) { nova ->
                                batidas = (batidas - hora + nova).distinct().sorted()
                            }
                        }) {
                            Text(formatarHora(hora), fontFamily = FontFamily.Monospace, fontSize = 18.sp)
                        }
                        IconButton(onClick = { batidas = batidas - hora }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remover batida")
                        }
                    }
                }
                TextButton(onClick = {
                    val sugestao = batidas.lastOrNull()?.plusHours(1) ?: LocalTime.of(8, 0)
                    escolherHora(contexto, sugestao) { nova -> batidas = (batidas + nova).distinct().sorted() }
                }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Adicionar batida")
                }
                if (rascunho.incompleto) {
                    Text(
                        "Falta registrar uma saída.",
                        color = CoresStatus.incompleto,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(4.dp))
                Text("Tipo do dia", fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TipoDia.entries.forEach { t ->
                        FilterChip(
                            selected = tipo == t,
                            onClick = { tipo = t },
                            label = { Text(t.rotulo) },
                        )
                    }
                }

                OutlinedTextField(
                    value = obs,
                    onValueChange = { obs = it },
                    label = { Text("Observação") },
                    modifier = Modifier.fillMaxWidth(),
                )

                LinhaValor("Trabalhado", formatarMinutos(trabalhado))
                LinhaValor("Esperado", formatarMinutos(esperado))
                LinhaValor(
                    "Saldo do dia",
                    formatarMinutos(trabalhado - esperado, sinal = true),
                    if (trabalhado >= esperado) CoresStatus.ok else CoresStatus.falta,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.atualizar(rascunho)
                onFechar()
            }) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onFechar) { Text("Cancelar") }
        },
    )
}
