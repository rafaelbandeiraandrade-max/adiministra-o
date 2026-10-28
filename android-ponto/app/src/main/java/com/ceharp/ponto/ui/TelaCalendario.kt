package com.ceharp.ponto.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ceharp.ponto.PontoViewModel
import com.ceharp.ponto.data.Calculo
import com.ceharp.ponto.data.StatusDia
import com.ceharp.ponto.data.formatarMinutos
import java.time.LocalDate
import java.time.YearMonth

private val cabecalhoSemana = listOf("D", "S", "T", "Q", "Q", "S", "S")

fun corDoStatus(status: StatusDia): Color? = when (status) {
    StatusDia.OK -> CoresStatus.ok
    StatusDia.EXTRA -> CoresStatus.extra
    StatusDia.DEVENDO -> CoresStatus.devendo
    StatusDia.INCOMPLETO -> CoresStatus.incompleto
    StatusDia.FALTA -> CoresStatus.falta
    StatusDia.ABONADO -> CoresStatus.abonado
    StatusDia.FUTURO, StatusDia.SEM_EXPEDIENTE -> null
}

@Composable
fun TelaCalendario(vm: PontoViewModel) {
    val contexto = LocalContext.current
    var mes by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var diaAberto by remember { mutableStateOf<LocalDate?>(null) }
    val hoje = LocalDate.now()
    val resumo = Calculo.resumoMes(mes, vm.registros, vm.config, hoje)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { mes = mes.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
            }
            Text(
                nomeDoMes(mes),
                Modifier
                    .weight(1f)
                    .clickable { mes = YearMonth.now() },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { mes = mes.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
            }
        }

        Column {
            Row(Modifier.fillMaxWidth()) {
                cabecalhoSemana.forEach {
                    Text(
                        it,
                        Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            // Domingo = coluna 0
            val deslocamento = mes.atDay(1).dayOfWeek.value % 7
            val semanas = (deslocamento + mes.lengthOfMonth() + 6) / 7
            for (s in 0 until semanas) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0..6) {
                        val numero = s * 7 + c - deslocamento + 1
                        if (numero in 1..mes.lengthOfMonth()) {
                            val data = mes.atDay(numero)
                            CelulaDia(
                                vm = vm,
                                data = data,
                                hoje = hoje,
                                modifier = Modifier.weight(1f),
                                onClick = { diaAberto = data },
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Legenda()

        CardSaldoMes(resumo.trabalhadoMin, resumo.esperadoMin, resumo.saldoMin, "Resumo de ${nomeDoMes(mes)}")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinhaValor("Dias trabalhados", resumo.diasTrabalhados.toString())
                LinhaValor(
                    "Faltas",
                    resumo.faltas.toString(),
                    if (resumo.faltas > 0) CoresStatus.falta else Color.Unspecified,
                )
            }
        }

        OutlinedButton(
            onClick = {
                val envio = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Ponto - ${nomeDoMes(mes)}")
                    putExtra(Intent.EXTRA_TEXT, vm.csvMes(mes))
                }
                contexto.startActivity(Intent.createChooser(envio, "Exportar relatório do mês"))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Exportar relatório do mês (CSV)")
        }
    }

    diaAberto?.let { data ->
        DialogoDia(vm = vm, data = data, onFechar = { diaAberto = null })
    }
}

@Composable
private fun CelulaDia(
    vm: PontoViewModel,
    data: LocalDate,
    hoje: LocalDate,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val dia = vm.dia(data)
    val status = Calculo.status(dia, vm.config, hoje)
    val cor = corDoStatus(status)
    val trabalhado = Calculo.minutosTrabalhados(dia)
    val ehHoje = data == hoje

    Box(
        modifier
            .aspectRatio(0.8f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cor?.copy(alpha = 0.22f) ?: MaterialTheme.colorScheme.surfaceContainerLow)
            .then(
                if (ehHoje) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                data.dayOfMonth.toString(),
                fontWeight = if (ehHoje) FontWeight.Bold else FontWeight.Medium,
                color = if (status == StatusDia.SEM_EXPEDIENTE) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
            )
            val detalhe = when {
                dia.tipo.abonado -> dia.tipo.sigla
                dia.batidas.isNotEmpty() -> formatarMinutos(trabalhado)
                status == StatusDia.FALTA -> "falta"
                else -> ""
            }
            if (detalhe.isNotEmpty()) {
                Text(detalhe, fontSize = 10.sp, color = cor ?: MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Legenda() {
    val itens = listOf(
        "Jornada ok" to CoresStatus.ok,
        "Hora extra" to CoresStatus.extra,
        "Abaixo" to CoresStatus.devendo,
        "Incompleto" to CoresStatus.incompleto,
        "Falta" to CoresStatus.falta,
        "Abonado" to CoresStatus.abonado,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        itens.chunked(3).forEach { linha ->
            Row(Modifier.fillMaxWidth()) {
                linha.forEach { (rotulo, cor) ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(cor),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(rotulo, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
