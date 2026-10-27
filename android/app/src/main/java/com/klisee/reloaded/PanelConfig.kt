package com.klisee.reloaded

import org.json.JSONArray
import org.json.JSONObject

/** One shortcut of the second screen: the game key it presses and the name shown on it. */
data class PanelButton(val key: String, val label: String)

/** A titled grid of shortcuts. */
data class PanelSection(val title: String, val columns: Int, val buttons: List<PanelButton>)

/**
 * Everything the second screen shows, top to bottom. There is one for battles and one for the rest
 * of the game; both are edited from Ajustes → Pantalla inferior.
 */
data class PanelConfig(val sections: List<PanelSection>) {

    fun toJson(): String = JSONObject().put(
        "sections",
        JSONArray(sections.map { section ->
            JSONObject()
                .put("title", section.title)
                .put("columns", section.columns)
                .put("buttons", JSONArray(section.buttons.map { JSONObject().put("key", it.key).put("label", it.label) }))
        }),
    ).toString()

    companion object {
        const val MIN_COLUMNS = 2
        const val MAX_COLUMNS = 5

        /** Outside battle, following `lista_botones_general.txt`. */
        val DEFAULT_FIELD = PanelConfig(
            listOf(
                PanelSection(
                    "Movimientos", 4,
                    listOf(
                        PanelButton("1", "Corte"),
                        PanelButton("2", "Vuelo"),
                        PanelButton("3", "Surf"),
                        PanelButton("4", "Fuerza"),
                        PanelButton("5", "Destello"),
                        PanelButton("6", "Golpe roca"),
                        PanelButton("7", "Buceo"),
                        PanelButton("8", "Menú habilidades"),
                    ),
                ),
                PanelSection(
                    "Accesos", 3,
                    listOf(
                        PanelButton("B", "Sacar Pokémon"),
                        PanelButton("RETURN", "Menú"),
                        PanelButton("O", "Objetos"),
                        PanelButton("V", "Bicicleta"),
                        PanelButton("M", "Mapa"),
                        PanelButton("SPACE", "Ver Pokémon"),
                        PanelButton("C", "Caña de pescar"),
                    ),
                ),
            ),
        )

        /** During a battle, following `lista_botones_combate.txt`. */
        val DEFAULT_BATTLE = PanelConfig(
            listOf(
                PanelSection(
                    "Pokémon", 4,
                    listOf(
                        PanelButton("1", "Poke 1"),
                        PanelButton("2", "Poke 2"),
                        PanelButton("3", "Poke 3"),
                        PanelButton("4", "Poke 4"),
                        PanelButton("5", "Poke 5"),
                        PanelButton("6", "Poke 6"),
                        PanelButton("SPACE", "Ver todos"),
                        PanelButton("S", "Siguiente"),
                    ),
                ),
                PanelSection(
                    "Accesos", 3,
                    listOf(
                        PanelButton("B", "Pokéball"),
                        PanelButton("SHIFT", "Ataques"),
                        PanelButton("A", "Objeto asignado"),
                        PanelButton("P", "Prob. captura"),
                        PanelButton("CONTROL", "Modificadores"),
                        PanelButton("O", "Objetos"),
                        PanelButton("E", "Escapar combate"),
                        PanelButton("M", "Mega evolución"),
                        PanelButton("F", "Flauta"),
                    ),
                ),
            ),
        )

        fun default(battle: Boolean) = if (battle) DEFAULT_BATTLE else DEFAULT_FIELD

        /** Reads a saved panel; anything unreadable falls back to the built-in one. */
        fun fromJson(text: String): PanelConfig? = runCatching {
            val sections = JSONObject(text).getJSONArray("sections")
            PanelConfig(
                (0 until sections.length()).map { i ->
                    val section = sections.getJSONObject(i)
                    val buttons = section.getJSONArray("buttons")
                    PanelSection(
                        title = section.optString("title"),
                        columns = section.optInt("columns", 3).coerceIn(MIN_COLUMNS, MAX_COLUMNS),
                        buttons = (0 until buttons.length()).map { j ->
                            val button = buttons.getJSONObject(j)
                            PanelButton(button.getString("key"), button.optString("label"))
                        },
                    )
                },
            )
        }.getOrNull()
    }
}
