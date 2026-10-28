package com.ceharp.ponto.ui

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val formatoExtenso = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", PT_BR)

private fun String.capitalizar() = replaceFirstChar { it.titlecase(PT_BR) }

fun dataPorExtenso(data: LocalDate): String = formatoExtenso.format(data).capitalizar()

fun nomeDoMes(mes: YearMonth): String =
    mes.month.getDisplayName(TextStyle.FULL, PT_BR).capitalizar() + " de " + mes.year
