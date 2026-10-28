package com.ceharp.ponto.data

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.abs

/** Tipo do dia. Dias "abonados" não geram horas esperadas (não contam como falta). */
enum class TipoDia(val rotulo: String, val sigla: String, val abonado: Boolean) {
    NORMAL("Normal", "", false),
    FOLGA("Folga", "FOL", true),
    FERIADO("Feriado", "FER", true),
    FERIAS("Férias", "FÉR", true),
    ATESTADO("Atestado", "ATE", true),
}

data class DiaRegistro(
    val data: LocalDate,
    val batidas: List<LocalTime> = emptyList(),
    val tipo: TipoDia = TipoDia.NORMAL,
    val obs: String = "",
) {
    /** Número ímpar de batidas: falta registrar uma saída. */
    val incompleto: Boolean get() = batidas.size % 2 == 1

    val vazio: Boolean get() = batidas.isEmpty() && tipo == TipoDia.NORMAL && obs.isBlank()
}

data class Config(
    /** Jornada diária esperada, em minutos. */
    val jornadaMin: Int = 8 * 60,
    val diasUteis: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    ),
    /** Diferença (em minutos) tolerada antes de marcar o dia como extra/devendo. */
    val toleranciaMin: Int = 10,
)

enum class StatusDia { FUTURO, SEM_EXPEDIENTE, ABONADO, FALTA, INCOMPLETO, OK, EXTRA, DEVENDO }

data class ResumoMes(
    val trabalhadoMin: Int,
    val esperadoMin: Int,
    val diasTrabalhados: Int,
    val faltas: Int,
) {
    val saldoMin: Int get() = trabalhadoMin - esperadoMin
}

object Calculo {

    /**
     * Soma os intervalos entrada→saída (batidas ordenadas, em pares).
     * Se o dia é hoje e há uma entrada em aberto, conta até [agora].
     */
    fun minutosTrabalhados(dia: DiaRegistro, agora: LocalDateTime? = null): Int {
        val b = dia.batidas.sorted()
        var total = 0L
        var i = 0
        while (i + 1 < b.size) {
            total += Duration.between(b[i], b[i + 1]).toMinutes()
            i += 2
        }
        if (b.size % 2 == 1 && agora != null && agora.toLocalDate() == dia.data) {
            val ultima = b.last()
            val atual = agora.toLocalTime()
            if (atual.isAfter(ultima)) total += Duration.between(ultima, atual).toMinutes()
        }
        return total.toInt()
    }

    fun minutosEsperados(data: LocalDate, tipo: TipoDia, config: Config): Int =
        if (tipo.abonado || data.dayOfWeek !in config.diasUteis) 0 else config.jornadaMin

    /** O dia entra no saldo? Dias passados sim; hoje só depois de fechado (batidas pares). */
    private fun contaNoSaldo(dia: DiaRegistro, hoje: LocalDate): Boolean = when {
        dia.data.isBefore(hoje) -> true
        dia.data == hoje -> dia.batidas.isNotEmpty() && !dia.incompleto
        else -> false
    }

    fun status(dia: DiaRegistro, config: Config, hoje: LocalDate): StatusDia {
        val esperado = minutosEsperados(dia.data, dia.tipo, config)
        return when {
            dia.tipo.abonado -> StatusDia.ABONADO
            dia.incompleto -> StatusDia.INCOMPLETO
            dia.batidas.isEmpty() && dia.data.isAfter(hoje) -> StatusDia.FUTURO
            dia.batidas.isEmpty() && esperado == 0 -> StatusDia.SEM_EXPEDIENTE
            dia.batidas.isEmpty() && dia.data == hoje -> StatusDia.FUTURO
            dia.batidas.isEmpty() -> StatusDia.FALTA
            else -> {
                val saldo = minutosTrabalhados(dia) - esperado
                when {
                    abs(saldo) <= config.toleranciaMin -> StatusDia.OK
                    saldo > 0 -> StatusDia.EXTRA
                    else -> StatusDia.DEVENDO
                }
            }
        }
    }

    fun resumoMes(
        mes: YearMonth,
        registros: Map<LocalDate, DiaRegistro>,
        config: Config,
        hoje: LocalDate,
    ): ResumoMes {
        var trabalhado = 0
        var esperado = 0
        var diasTrabalhados = 0
        var faltas = 0
        for (d in 1..mes.lengthOfMonth()) {
            val data = mes.atDay(d)
            val dia = registros[data] ?: DiaRegistro(data)
            if (!contaNoSaldo(dia, hoje)) continue
            trabalhado += minutosTrabalhados(dia)
            esperado += minutosEsperados(data, dia.tipo, config)
            if (dia.batidas.isNotEmpty()) diasTrabalhados++
            if (status(dia, config, hoje) == StatusDia.FALTA) faltas++
        }
        return ResumoMes(trabalhado, esperado, diasTrabalhados, faltas)
    }
}

/** 510 → "08:30"; -75 → "-01:15"; com [sinal], 75 → "+01:15". */
fun formatarMinutos(min: Int, sinal: Boolean = false): String {
    val a = abs(min)
    val texto = "%02d:%02d".format(a / 60, a % 60)
    return when {
        min < 0 -> "-$texto"
        sinal && min > 0 -> "+$texto"
        else -> texto
    }
}

fun formatarHora(t: LocalTime): String = "%02d:%02d".format(t.hour, t.minute)
