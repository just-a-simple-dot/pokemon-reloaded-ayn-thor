package com.klisee.reloaded

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import java.io.File

/** The pads Android is reporting right now. */
fun connectedPads(context: Context): List<String> {
    val manager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
    val names = mutableListOf<String>()
    for (id in manager.inputDeviceIds) {
        val device = manager.getInputDevice(id) ?: continue
        val gamepad = device.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
        val joystick = device.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
        if (!device.isVirtual && (gamepad || joystick)) names.add(device.name)
    }
    return names
}

/**
 * The pad buttons the player can give a key to, with the key each one presses out of the box.
 *
 * The game only ever reads the keyboard, so controller support is a pad pressing keys. Android
 * reports the same key codes for every brand of pad, which is what lets one layout cover an Xbox
 * pad, a PlayStation one and a generic one at the same time.
 */
enum class PadButton(val code: Int, val label: String, val defaultKey: String?) {
    A(96, "A / cruz", "X"),
    B(97, "B / círculo", "C"),
    X(99, "X / cuadrado", "Z"),
    Y(100, "Y / triángulo", "V"),
    L1(102, "LB / L1", "CONTROL"),
    R1(103, "RB / R1", "B"),
    L2(104, "LT / L2", "<"),
    R2(105, "RT / R2", "O"),
    L3(106, "Pulsar stick izquierdo", "P"),
    R3(107, "Pulsar stick derecho", "SPACE"),
    SELECT(109, "Select / View", "RETURN"),
    START(108, "Start / Menu", "SPACE"),
}

/** The name of a pad button, for showing it on screen. */
fun buttonName(code: Int): String = PadButton.entries.firstOrNull { it.code == code }?.label ?: "Botón $code"

/**
 * Writes the layout the emulator reads when it starts: every pad button with its key, or `NONE`
 * for one the player left without a key.
 */
fun writeControlsFile(filesDir: File, keys: Map<PadButton, String?>, stick: Boolean, padConnected: Boolean) {
    val text = buildString {
        appendLine("# Escrito por la pantalla de Controles. El emulador lo lee al arrancar.")
        for (button in PadButton.entries) {
            appendLine("${button.code}=${keys[button] ?: "NONE"}")
        }
        appendLine("stick=${if (stick) 1 else 0}")
        // With a pad connected the game starts with the touch controls hidden; touching the screen
        // brings them back.
        appendLine("pad=${if (padConnected) 1 else 0}")
    }
    runCatching { File(filesDir, "controls.conf").writeText(text) }
}
