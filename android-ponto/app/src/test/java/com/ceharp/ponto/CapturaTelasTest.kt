package com.ceharp.ponto

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ceharp.ponto.data.DiaRegistro
import com.ceharp.ponto.data.TipoDia
import com.ceharp.ponto.ui.PontoTheme
import com.ceharp.ponto.ui.PainelDia
import com.ceharp.ponto.ui.relogioAoVivo
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
@Config(sdk = [34], qualifiers = "w400dp-h860dp-night-xxhdpi")
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

    private fun avancar() = regra.waitForIdle()

    private fun prepararDados(): Pair<PontoViewModel, LocalDate?> {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences("ponto", Context.MODE_PRIVATE).edit().clear().commit()
        val vm = PontoViewModel(app)
        relogioAoVivo = false
        return vm to preencherExemplo(vm)
    }

    @Test
    fun capturarTelas() {
        val (vm, _) = prepararDados()
        regra.setContent { PontoTheme { AppPonto(vm) } }
        avancar()
        captureScreenRoboImage("telas-geradas/1-hoje.png")

        regra.onNodeWithText("Calendário").performClick()
        avancar()
        captureScreenRoboImage("telas-geradas/2-calendario.png")

        regra.onNodeWithText("Exportar relatório do mês (CSV)").performScrollTo()
        avancar()
        captureScreenRoboImage("telas-geradas/3-calendario-resumo.png")

        regra.onNodeWithText("Ajustes").performClick()
        avancar()
        captureScreenRoboImage("telas-geradas/5-ajustes.png")
    }

    /** A janela real (Dialog) não fica ociosa no Robolectric; o mesmo painel é desenhado sobre o calendário. */
    @Test
    fun capturarEdicaoDoDia() {
        val (vm, dia) = prepararDados()
        val data = dia ?: LocalDate.now()
        regra.setContent {
            PontoTheme {
                Box {
                    AppPonto(vm, abaInicial = 1)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 24.dp, vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PainelDia(vm, data, onFechar = {})
                    }
                }
            }
        }
        avancar()
        captureScreenRoboImage("telas-geradas/4-editar-dia.png")
    }
}
