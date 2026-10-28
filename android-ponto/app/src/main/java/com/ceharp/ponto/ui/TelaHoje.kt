package com.ceharp.ponto.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceharp.ponto.PontoViewModel
import com.ceharp.ponto.data.Calculo
import com.ceharp.ponto.data.formatarHora
import com.ceharp.ponto.data.formatarMinutos
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val formatoRelogio = DateTimeFormatter.ofPattern("HH:mm:ss")

@Composable
fun TelaHoje(vm: PontoViewModel) {
    val contexto = LocalContext.current
    var agora by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            agora = LocalDateTime.now()
            delay(1000)
        }
    }
    val hoje = agora.toLocalDate()
    val dia = vm.dia(hoje)
    val batidas = dia.batidas.sorted()
    val trabalhado = Calculo.minutosTrabalhados(dia, agora)
    val esperado = Calculo.minutosEsperados(hoje, dia.tipo, vm.config)
    val resumo = Calculo.resumoMes(YearMonth.from(hoje), vm.registros, vm.config, hoje)
    val proximaEhEntrada = batidas.size % 2 == 0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    formatoRelogio.format(agora),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(dataPorExtenso(hoje), style = MaterialTheme.typography.bodyLarge)
                if (dia.tipo.abonado) {
                    Spacer(Modifier.height(4.dp))
                    Text("Dia marcado como ${dia.tipo.rotulo}", color = CoresStatus.abonado)
                }
            }
        }

        Button(
            onClick = {
                val hora = vm.baterPonto()
                val msg = if (hora == null) {
                    "Já existe uma batida neste minuto"
                } else {
                    "${if (proximaEhEntrada) "Entrada" else "Saída"} registrada às ${formatarHora(hora)}"
                }
                Toast.makeText(contexto, msg, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                if (proximaEhEntrada) "Registrar ENTRADA" else "Registrar SAÍDA",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Trabalhado hoje", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (esperado > 0) "${formatarMinutos(trabalhado)} / ${formatarMinutos(esperado)}"
                        else formatarMinutos(trabalhado),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                if (esperado > 0) {
                    LinearProgressIndicator(
                        progress = { (trabalhado.toFloat() / esperado).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val falta = esperado - trabalhado
                    Text(
                        if (falta > 0) "Faltam ${formatarMinutos(falta)} para completar a jornada"
                        else "Jornada completa (${formatarMinutos(-falta, sinal = true)} extra)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Batidas de hoje", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (batidas.isEmpty()) {
                    Text(
                        "Nenhuma batida registrada ainda.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                batidas.forEachIndexed { i, hora ->
                    if (i > 0) HorizontalDivider()
                    LinhaBatida(i, hora)
                }
            }
        }

        CardSaldoMes(resumo.trabalhadoMin, resumo.esperadoMin, resumo.saldoMin, "Saldo do mês (banco de horas)")
    }
}

@Composable
internal fun CardSaldoMes(trabalhado: Int, esperado: Int, saldo: Int, titulo: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            LinhaValor("Trabalhado", formatarMinutos(trabalhado))
            LinhaValor("Esperado", formatarMinutos(esperado))
            LinhaValor(
                "Saldo",
                formatarMinutos(saldo, sinal = true),
                if (saldo >= 0) CoresStatus.ok else CoresStatus.falta,
            )
        }
    }
}

@Composable
internal fun LinhaValor(rotulo: String, valor: String, cor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = cor)
    }
}

@Composable
private fun LinhaBatida(indice: Int, hora: java.time.LocalTime) {
    val entrada = indice % 2 == 0
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            if (entrada) "Entrada" else "Saída",
            color = if (entrada) CoresStatus.ok else CoresStatus.devendo,
            fontWeight = FontWeight.SemiBold,
        )
        Text(formatarHora(hora), fontFamily = FontFamily.Monospace, fontSize = 18.sp)
    }
}
