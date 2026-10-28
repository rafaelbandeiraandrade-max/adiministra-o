package com.ceharp.ponto

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ceharp.ponto.data.DiaRegistro
import com.ceharp.ponto.data.TipoDia
import com.ceharp.ponto.ui.PontoTheme
import com.ceharp.ponto.ui.nomeDoMes
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Gera imagens das telas com dados de exemplo (pasta app/telas-geradas). */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi-night")
class CapturaTelasTest {

    @get:Rule
    val regra = createAndroidComposeRule<ComponentActivity>()

    private fun t(s: String) = LocalTime.parse(s)

    private fun preencherExemplo(vm: PontoViewModel): LocalDate? {
        val hoje = LocalDate.now()
        val mes = YearMonth.from(hoje)
        var diaParaAbrir: LocalDate? = null
        for (d in 1 until hoje.dayOfMonth) {
            val data = mes.atDay(d)
            if (data.dayOfWeek == DayOfWeek.SATURDAY || data.dayOfWeek == DayOfWeek.SUNDAY) continue
            val dia = when {
                d == 12 -> DiaRegistro(data, tipo = TipoDia.FERIADO, obs = "Feriado")
                d == 20 -> null // falta
                d == 22 -> DiaRegistro(data, listOf(t("08:00"), t("12:00"), t("13:00")))
                d % 5 == 1 -> DiaRegistro(data, listOf(t("07:45"), t("12:00"), t("13:00"), t("18:10")), obs = "Reunião no fim do dia")
                d % 6 == 2 -> DiaRegistro(data, listOf(t("08:30"), t("12:00"), t("13:00"), t("16:30")))
                else -> DiaRegistro(data, listOf(t("08:00"), t("12:00"), t("13:00"), t("17:00")))
            } ?: continue
            vm.atualizar(dia)
            if (dia.obs.isNotEmpty() && dia.tipo == TipoDia.NORMAL) diaParaAbrir = data
        }
        vm.atualizar(DiaRegistro(hoje, listOf(t("08:02"), t("12:00"), t("13:05"))))
        return diaParaAbrir
    }

    private fun avancar() {
        regra.mainClock.advanceTimeBy(600)
        regra.waitForIdle()
    }

    @Test
    fun capturarTelas() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("ponto", Context.MODE_PRIVATE).edit().clear().commit()
        val vm = PontoViewModel(app)
        val diaParaAbrir = preencherExemplo(vm)

        // O relógio da tela Hoje atualiza em loop; o tempo do teste avança manualmente.
        regra.mainClock.autoAdvance = false
        regra.setContent { PontoTheme { AppPonto(vm) } }
        avancar()
        captureScreenRoboImage("telas-geradas/1-hoje.png")

        regra.onNodeWithText("Calendário").performClick()
        avancar()
        captureScreenRoboImage("telas-geradas/2-calendario.png")

        regra.onNodeWithText("Exportar relatório do mês (CSV)").performScrollTo()
        avancar()
        captureScreenRoboImage("telas-geradas/3-calendario-resumo.png")

        if (diaParaAbrir != null) {
            regra.onNodeWithText(nomeDoMes(YearMonth.now())).performScrollTo()
            avancar()
            regra.onAllNodesWithText(diaParaAbrir.dayOfMonth.toString()).onFirst().performClick()
            avancar()
            captureScreenRoboImage("telas-geradas/4-editar-dia.png")
            regra.onNodeWithText("Cancelar").performClick()
            avancar()
        }

        regra.onNodeWithText("Ajustes").performClick()
        avancar()
        captureScreenRoboImage("telas-geradas/5-ajustes.png")
    }
}
