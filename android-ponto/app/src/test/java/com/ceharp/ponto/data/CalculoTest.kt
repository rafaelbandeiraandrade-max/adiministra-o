package com.ceharp.ponto.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class CalculoTest {

    private val config = Config()
    private val segunda = LocalDate.of(2026, 10, 26)
    private val sabado = LocalDate.of(2026, 10, 31)

    private fun t(s: String) = LocalTime.parse(s)

    @Test
    fun somaParesDeBatidas() {
        val dia = DiaRegistro(segunda, listOf(t("13:00"), t("08:00"), t("12:00"), t("17:30")))
        assertEquals(8 * 60 + 30, Calculo.minutosTrabalhados(dia))
    }

    @Test
    fun entradaEmAbertoContaAteAgora() {
        val dia = DiaRegistro(segunda, listOf(t("08:00")))
        assertEquals(0, Calculo.minutosTrabalhados(dia))
        assertEquals(90, Calculo.minutosTrabalhados(dia, segunda.atTime(9, 30)))
    }

    @Test
    fun statusDosDias() {
        val hoje = LocalDate.of(2026, 10, 28)
        assertEquals(StatusDia.FALTA, Calculo.status(DiaRegistro(segunda), config, hoje))
        assertEquals(StatusDia.SEM_EXPEDIENTE, Calculo.status(DiaRegistro(LocalDate.of(2026, 10, 25)), config, hoje))
        assertEquals(StatusDia.FUTURO, Calculo.status(DiaRegistro(sabado), config, hoje))
        assertEquals(StatusDia.ABONADO, Calculo.status(DiaRegistro(segunda, tipo = TipoDia.FERIADO), config, hoje))
        assertEquals(StatusDia.INCOMPLETO, Calculo.status(DiaRegistro(segunda, listOf(t("08:00"))), config, hoje))
        assertEquals(StatusDia.OK, Calculo.status(DiaRegistro(segunda, listOf(t("08:00"), t("16:05"))), config, hoje))
        assertEquals(StatusDia.EXTRA, Calculo.status(DiaRegistro(segunda, listOf(t("08:00"), t("17:00"))), config, hoje))
        assertEquals(StatusDia.DEVENDO, Calculo.status(DiaRegistro(segunda, listOf(t("08:00"), t("15:00"))), config, hoje))
    }

    @Test
    fun resumoDoMesIgnoraFuturoEHojeEmAberto() {
        val mes = YearMonth.of(2026, 10)
        val hoje = LocalDate.of(2026, 10, 2) // sexta; dia 1 é quinta
        val registros = mapOf(
            mes.atDay(1) to DiaRegistro(mes.atDay(1), listOf(t("08:00"), t("18:00"))),
            hoje to DiaRegistro(hoje, listOf(t("08:00"))),
        )
        val r = Calculo.resumoMes(mes, registros, config, hoje)
        assertEquals(600, r.trabalhadoMin)
        assertEquals(480, r.esperadoMin)
        assertEquals(120, r.saldoMin)
        assertEquals(0, r.faltas)
    }

    @Test
    fun formatacao() {
        assertEquals("08:30", formatarMinutos(510))
        assertEquals("-01:15", formatarMinutos(-75))
        assertEquals("+01:15", formatarMinutos(75, sinal = true))
        assertEquals("00:00", formatarMinutos(0, sinal = true))
    }
}
