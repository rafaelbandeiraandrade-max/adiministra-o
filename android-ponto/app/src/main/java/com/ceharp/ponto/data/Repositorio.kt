package com.ceharp.ponto.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Persistência local (SharedPreferences + JSON). Uma chave por dia: "d_2026-10-28". */
class Repositorio(context: Context) {

    private val prefs = context.getSharedPreferences("ponto", Context.MODE_PRIVATE)

    fun carregarTodos(): Map<LocalDate, DiaRegistro> {
        val mapa = mutableMapOf<LocalDate, DiaRegistro>()
        for ((chave, valor) in prefs.all) {
            if (!chave.startsWith(PREFIXO_DIA) || valor !is String) continue
            runCatching {
                val data = LocalDate.parse(chave.removePrefix(PREFIXO_DIA))
                mapa[data] = deJson(data, JSONObject(valor))
            }
        }
        return mapa
    }

    fun salvar(dia: DiaRegistro) {
        val chave = PREFIXO_DIA + dia.data
        prefs.edit().apply {
            if (dia.vazio) remove(chave) else putString(chave, paraJson(dia).toString())
        }.apply()
    }

    fun carregarConfig(): Config {
        val padrao = Config()
        val dias = prefs.getString(CHAVE_DIAS_UTEIS, null)
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull()?.let(DayOfWeek::of) }
            ?.toSet()
            ?: padrao.diasUteis
        return Config(
            jornadaMin = prefs.getInt(CHAVE_JORNADA, padrao.jornadaMin),
            diasUteis = dias,
            toleranciaMin = prefs.getInt(CHAVE_TOLERANCIA, padrao.toleranciaMin),
        )
    }

    fun salvarConfig(config: Config) {
        prefs.edit()
            .putInt(CHAVE_JORNADA, config.jornadaMin)
            .putInt(CHAVE_TOLERANCIA, config.toleranciaMin)
            .putString(CHAVE_DIAS_UTEIS, config.diasUteis.map { it.value }.sorted().joinToString(","))
            .apply()
    }

    private fun paraJson(dia: DiaRegistro) = JSONObject()
        .put("b", JSONArray(dia.batidas.sorted().map(::formatarHora)))
        .put("t", dia.tipo.name)
        .put("o", dia.obs)

    private fun deJson(data: LocalDate, json: JSONObject): DiaRegistro {
        val arr = json.optJSONArray("b") ?: JSONArray()
        val batidas = (0 until arr.length()).map { LocalTime.parse(arr.getString(it)) }
        val tipo = runCatching { TipoDia.valueOf(json.optString("t")) }.getOrDefault(TipoDia.NORMAL)
        return DiaRegistro(data, batidas, tipo, json.optString("o"))
    }

    private companion object {
        const val PREFIXO_DIA = "d_"
        const val CHAVE_JORNADA = "cfg_jornada"
        const val CHAVE_TOLERANCIA = "cfg_tolerancia"
        const val CHAVE_DIAS_UTEIS = "cfg_dias_uteis"
    }
}
