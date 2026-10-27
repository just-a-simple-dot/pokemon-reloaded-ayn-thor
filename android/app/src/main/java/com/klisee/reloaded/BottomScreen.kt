package com.klisee.reloaded

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.view.Display
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/** One shortcut of the second screen: the game key it presses and what it does in the game. */
private class Shortcut(val key: String, val keyLabel: String, val text: String)

/** The field moves, on the number row. */
private val MOVES = listOf(
    Shortcut("1", "1", "Corte"),
    Shortcut("2", "2", "Vuelo"),
    Shortcut("3", "3", "Surf"),
    Shortcut("4", "4", "Fuerza"),
    Shortcut("5", "5", "Destello"),
    Shortcut("6", "6", "Golpe roca"),
    Shortcut("7", "7", "Buceo"),
    Shortcut("8", "8", "Menú habilidades"),
)

/** The game's menus and tools. */
private val TOOLS = listOf(
    Shortcut("RETURN", "Enter", "Menú"),
    Shortcut("SPACE", "Espacio", "Pokémon"),
    Shortcut("O", "O", "Objetos"),
    Shortcut("B", "B", "Pokéball"),
    Shortcut("<", "<", "Ataques"),
    Shortcut("P", "P", "Prob. captura"),
    Shortcut("F", "F", "Flauta"),
    Shortcut("M", "M", "Mapa / Mega"),
    Shortcut("CONTROL", "Ctrl", "Modificadores"),
)

/** A tap shorter than this is held until it, so the game (at 30 steps/s) cannot miss the key. */
private const val MIN_HOLD_MS = 80L

private const val BACKGROUND = 0xFF101418.toInt()
private const val MOVE_COLOR = 0xFF1F4E79.toInt()
private const val TOOL_COLOR = 0xFF3A3F47.toInt()
private const val PRESSED_COLOR = 0xFFE8A33D.toInt()

/**
 * The shortcut panel shown on a second screen, such as the bottom one of an AYN Thor.
 *
 * It is a `Presentation` owned by the game activity, so it lives and dies with the game. Its window
 * never takes the input focus: if it did, touching it would move the focus to that screen and the
 * built-in pad would stop reaching the game.
 */
class BottomScreen(
    context: Context,
    display: Display,
    private val press: (key: String, down: Boolean) -> Unit,
) : Presentation(context, display) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BACKGROUND)
            val pad = dp(8)
            setPadding(pad, pad, pad, pad)
        }
        root.addView(title("Movimientos"))
        addGrid(root, MOVES, columns = 4, color = MOVE_COLOR)
        root.addView(title("Accesos"))
        addGrid(root, TOOLS, columns = 3, color = TOOL_COLOR)
        setContentView(root)
    }

    private fun title(text: String) = TextView(context).apply {
        this.text = text
        setTextColor(0xFF9AA4AE.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(6), dp(6), dp(6), dp(2))
    }

    private fun addGrid(root: LinearLayout, shortcuts: List<Shortcut>, columns: Int, color: Int) {
        for (row in shortcuts.chunked(columns)) {
            val line = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            for (shortcut in row) line.addView(button(shortcut, color), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            // A short last row keeps the same button width as the others.
            repeat(columns - row.size) { line.addView(View(context), LinearLayout.LayoutParams(0, 0, 1f)) }
            root.addView(line, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun button(shortcut: Shortcut, color: Int): View {
        val shape = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(color)
        }
        val view = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape
            addView(TextView(context).apply {
                text = shortcut.text
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = shortcut.keyLabel
                setTextColor(0xB3FFFFFF.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                gravity = Gravity.CENTER
            })
        }
        var downAt = 0L
        val release = Runnable {
            shape.setColor(color)
            press(shortcut.key, false)
        }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.removeCallbacks(release)
                    downAt = SystemClock.uptimeMillis()
                    shape.setColor(PRESSED_COLOR)
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    press(shortcut.key, true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val held = SystemClock.uptimeMillis() - downAt
                    v.postDelayed(release, (MIN_HOLD_MS - held).coerceAtLeast(0))
                }
            }
            true
        }
        // Wrapped so the margins apply inside the weighted cell.
        return LinearLayout(context).apply {
            val margin = dp(4)
            addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                setMargins(margin, margin, margin, margin)
            })
        }
    }

    private fun dp(value: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics).toInt()

    companion object {
        /** The first screen other than the one the game is on, or null on a single-screen device. */
        fun findDisplay(context: Context, gameDisplay: Display?): Display? {
            val manager = context.getSystemService(DisplayManager::class.java) ?: return null
            val gameId = gameDisplay?.displayId ?: Display.DEFAULT_DISPLAY
            return manager.displays.firstOrNull { it.displayId != gameId && it.state != Display.STATE_OFF }
        }
    }
}
