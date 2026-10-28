package com.ceharp.ponto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ceharp.ponto.ui.PontoTheme
import com.ceharp.ponto.ui.TelaAjustes
import com.ceharp.ponto.ui.TelaCalendario
import com.ceharp.ponto.ui.TelaHoje

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PontoTheme {
                AppPonto()
            }
        }
    }
}

private data class Aba(val rotulo: String, val titulo: String, val icone: ImageVector)

private val abas = listOf(
    Aba("Hoje", "Controle de Ponto", Icons.Filled.Home),
    Aba("Calendário", "Calendário mensal", Icons.Filled.DateRange),
    Aba("Ajustes", "Ajustes", Icons.Filled.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPonto(vm: PontoViewModel = viewModel(), abaInicial: Int = 0) {
    var aba by rememberSaveable { mutableIntStateOf(abaInicial) }
    Scaffold(
        topBar = { TopAppBar(title = { Text(abas[aba].titulo) }) },
        bottomBar = {
            NavigationBar {
                abas.forEachIndexed { i, a ->
                    NavigationBarItem(
                        selected = aba == i,
                        onClick = { aba = i },
                        icon = { Icon(a.icone, contentDescription = null) },
                        label = { Text(a.rotulo) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (aba) {
                0 -> TelaHoje(vm)
                1 -> TelaCalendario(vm)
                else -> TelaAjustes(vm)
            }
        }
    }
}
