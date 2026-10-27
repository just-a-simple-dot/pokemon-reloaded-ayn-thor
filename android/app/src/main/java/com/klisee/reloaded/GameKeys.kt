package com.klisee.reloaded

/**
 * A keyboard key the game can be sent. `id` is the name the engine understands (in `controls.conf`
 * and through `nativeKey`); `label` is what the player sees.
 */
class GameKey(val id: String, val label: String, val group: String)

/** Every key the engine can press, grouped the way the key picker shows them. */
val GAME_KEYS: List<GameKey> = buildList {
    val special = "Especiales"
    add(GameKey("RETURN", "Enter", special))
    add(GameKey("SPACE", "Espacio", special))
    add(GameKey("ESCAPE", "Esc", special))
    add(GameKey("TAB", "Tab", special))
    add(GameKey("BACKSPACE", "Borrar", special))
    add(GameKey("SHIFT", "Shift izq.", special))
    add(GameKey("CONTROL", "Ctrl", special))
    add(GameKey("ALT", "Alt", special))
    add(GameKey("INSERT", "Insert", special))
    add(GameKey("DELETE", "Supr", special))
    add(GameKey("HOME", "Inicio", special))
    add(GameKey("END", "Fin", special))
    add(GameKey("PAGEUP", "Re Pág", special))
    add(GameKey("PAGEDOWN", "Av Pág", special))

    val arrows = "Flechas"
    add(GameKey("UP", "↑", arrows))
    add(GameKey("DOWN", "↓", arrows))
    add(GameKey("LEFT", "←", arrows))
    add(GameKey("RIGHT", "→", arrows))

    for (c in 'A'..'Z') add(GameKey(c.toString(), c.toString(), "Letras"))
    for (d in '0'..'9') add(GameKey(d.toString(), d.toString(), "Números"))

    val symbols = "Símbolos"
    add(GameKey("<", "<", symbols))
    add(GameKey("COMMA", ",", symbols))
    add(GameKey("PERIOD", ".", symbols))
    add(GameKey("MINUS", "-", symbols))
    add(GameKey("PLUS", "+", symbols))

    val keypad = "Teclado numérico"
    for (d in 0..9) add(GameKey("NUM$d", "Num $d", keypad))
    add(GameKey("NUMADD", "Num +", keypad))
    add(GameKey("NUMSUB", "Num -", keypad))
    add(GameKey("NUMMUL", "Num *", keypad))
    add(GameKey("NUMDIV", "Num /", keypad))
    add(GameKey("NUMDEC", "Num .", keypad))

    for (n in 1..12) add(GameKey("F$n", "F$n", "Funciones"))
}

private val KEYS_BY_ID = GAME_KEYS.associateBy { it.id }

/** What the player sees for a key id; null (no key) reads as "Sin asignar". */
fun keyLabel(id: String?): String = if (id == null) "Sin asignar" else KEYS_BY_ID[id]?.label ?: id

/** What a key does in Pokémon Reloaded, for the ones that are known. */
fun keyHint(id: String?): String = when (id) {
    "Z" -> "Interactuar y confirmar"
    "X" -> "Cancelar y correr"
    "RETURN" -> "Menú"
    "SPACE" -> "Pokémon"
    "B" -> "Pokéball"
    "CONTROL" -> "Modificadores"
    "O" -> "Objetos"
    "SHIFT" -> "Ataques"
    "P" -> "Prob. captura"
    "E" -> "Escapar combate"
    "M" -> "Mapa / Mega"
    "F" -> "Flauta"
    "1" -> "Corte"
    "2" -> "Vuelo"
    "3" -> "Surf"
    "4" -> "Fuerza"
    "5" -> "Destello"
    "6" -> "Golpe roca"
    "7" -> "Buceo"
    "8" -> "Menú habilidades"
    else -> ""
}
