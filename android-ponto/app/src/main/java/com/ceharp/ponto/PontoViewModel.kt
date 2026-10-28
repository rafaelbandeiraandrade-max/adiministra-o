package com.ceharp.ponto

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.ceharp.ponto.data.Calculo
import com.ceharp.ponto.data.Config
import com.ceharp.ponto.data.DiaRegistro
import com.ceharp.ponto.data.Repositorio
import com.ceharp.ponto.data.formatarHora
import com.ceharp.ponto.data.formatarMinutos
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class PontoViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repositorio(app)

    val registros = mutableStateMapOf<LocalDate, DiaRegistro>().apply { putAll(repo.carregarTodos()) }

    var config by mutableStateOf(repo.carregarConfig())
        private set

    fun dia(data: LocalDate): DiaRegistro = registros[data] ?: DiaRegistro(data)

    fun atualizar(dia: DiaRegistro) {
        val ordenado = dia.copy(batidas = dia.batidas.sorted())
        if (ordenado.vazio) registros.remove(dia.data) else registros[dia.data] = ordenado
        repo.salvar(ordenado)
    }

    /** Registra a batida atual. Retorna o horário, ou null se já houver batida neste minuto. */
    fun baterPonto(): LocalTime? {
        val agora = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        val dia = dia(agora.toLocalDate())
        val hora = agora.toLocalTime()
        if (hora in dia.batidas) return null
        atualizar(dia.copy(batidas = dia.batidas + hora))
        return hora
    }

    fun salvarConfig(nova: Config) {
        config = nova
        repo.salvarConfig(nova)
    }

    /** Relatório do mês em CSV (separado por ";", abre direto no Excel em português). */
    fun csvMes(mes: YearMonth): String = buildString {
        appendLine("Data;Tipo;Batidas;Trabalhado;Esperado;Saldo;Observação")
        for (d in 1..mes.lengthOfMonth()) {
            val dia = dia(mes.atDay(d))
            val trabalhado = Calculo.minutosTrabalhados(dia)
            val esperado = Calculo.minutosEsperados(dia.data, dia.tipo, config)
            appendLine(
                listOf(
                    "%02d/%02d/%d".format(d, mes.monthValue, mes.year),
                    dia.tipo.rotulo,
                    dia.batidas.joinToString(" ") { formatarHora(it) },
                    formatarMinutos(trabalhado),
                    formatarMinutos(esperado),
                    formatarMinutos(trabalhado - esperado, sinal = true),
                    dia.obs.replace(";", ",").replace("\n", " "),
                ).joinToString(";"),
            )
        }
        val r = Calculo.resumoMes(mes, registros, config, LocalDate.now())
        appendLine("Total;;;${formatarMinutos(r.trabalhadoMin)};${formatarMinutos(r.esperadoMin)};${formatarMinutos(r.saldoMin, sinal = true)};")
    }
}
