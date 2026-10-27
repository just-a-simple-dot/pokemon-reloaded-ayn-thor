package com.klisee.reloaded

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.BatteryManager
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
import androidx.core.content.ContextCompat
import java.util.Date

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
    private val fieldPanel: PanelConfig,
    private val battlePanel: PanelConfig,
    battle: Boolean,
    private val press: (key: String, down: Boolean) -> Unit,
) : Presentation(context, display) {

    /** Whether the battle panel is the one on screen. The game tells us when a battle starts or ends. */
    var battle: Boolean = battle
        set(value) {
            if (field == value) return
            field = value
            if (root != null) render()
        }

    private var root: LinearLayout? = null

    /** Keys a finger is holding right now, released if the panel changes under it. */
    private val held = mutableSetOf<String>()

    private fun send(key: String, down: Boolean) {
        if (down) held.add(key) else if (!held.remove(key)) return
        press(key, down)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BACKGROUND)
            val pad = dp(8)
            setPadding(pad, pad, pad, pad)
        }
        setContentView(root!!)
        render()
    }

    /** Lays out the panel for the current state, field or battle. */
    private fun render() {
        val root = root ?: return
        held.toList().forEach { send(it, false) }
        root.removeAllViews()
        val sections = (if (battle) battlePanel else fieldPanel).sections.filter { it.buttons.isNotEmpty() }
        sections.forEachIndexed { index, section ->
            if (section.title.isNotBlank()) root.addView(title(section.title))
            // The first section keeps the blue of the field moves, the rest the grey of the menus.
            val color = if (index == 0) MOVE_COLOR else TOOL_COLOR
            val last = index == sections.lastIndex
            addGrid(root, section.buttons, section.columns, color, trailing = if (last) status() else null)
        }
        // With no buttons at all the clock still has somewhere to go.
        if (sections.isEmpty()) root.addView(status(), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        showClock()
        lastBattery?.let { showBattery(it) }
    }

    // ------------------------------------------------------------------------------------------
    // Clock and battery, in the space the last row of shortcuts leaves free.

    private lateinit var clock: TextView
    private lateinit var battery: TextView

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) showBattery(intent) else showClock()
        }
    }

    private fun status(): View {
        clock = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        battery = TextView(context).apply {
            setTextColor(0xFF9AA4AE.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            addView(clock)
            addView(battery)
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        // The battery broadcast is sticky, so registering hands back the current level at once.
        ContextCompat.registerReceiver(context, statusReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            ?.let { showBattery(it) }
        showClock()
    }

    override fun onStop() {
        held.toList().forEach { send(it, false) }
        runCatching { context.unregisterReceiver(statusReceiver) }
        super.onStop()
    }

    private fun showClock() {
        clock.text = android.text.format.DateFormat.getTimeFormat(context).format(Date())
    }

    /** The last battery report, to fill in a freshly laid out panel without waiting for the next one. */
    private var lastBattery: Intent? = null

    private fun showBattery(intent: Intent) {
        lastBattery = intent
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        battery.text = "Batería ${level * 100 / scale}%" + if (charging) " ⚡" else ""
    }

    private fun title(text: String) = TextView(context).apply {
        this.text = text
        setTextColor(0xFF9AA4AE.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(6), dp(6), dp(6), dp(2))
    }

    /**
     * Lays the shortcuts out in rows. `trailing`, if any, fills what the last row leaves free, or gets
     * a row of its own when the last row is full.
     */
    private fun addGrid(root: LinearLayout, shortcuts: List<PanelButton>, columns: Int, color: Int, trailing: View? = null) {
        if (trailing != null && shortcuts.size % columns == 0) {
            addRows(root, shortcuts, columns, color, null)
            root.addView(trailing, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        } else {
            addRows(root, shortcuts, columns, color, trailing)
        }
    }

    private fun addRows(root: LinearLayout, shortcuts: List<PanelButton>, columns: Int, color: Int, trailing: View?) {
        for (row in shortcuts.chunked(columns)) {
            val line = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            for (shortcut in row) line.addView(button(shortcut, color), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            // A short last row keeps the same button width as the others.
            val free = columns - row.size
            if (free > 0 && trailing != null) {
                line.addView(trailing, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, free.toFloat()))
            } else {
                repeat(free) { line.addView(View(context), LinearLayout.LayoutParams(0, 0, 1f)) }
            }
            root.addView(line, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun button(shortcut: PanelButton, color: Int): View {
        val shape = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(color)
        }
        val view = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = shape
            addView(TextView(context).apply {
                text = shortcut.label
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = keyLabel(shortcut.key)
                setTextColor(0xB3FFFFFF.toInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                gravity = Gravity.CENTER
            })
        }
        var downAt = 0L
        val release = Runnable {
            shape.setColor(color)
            send(shortcut.key, false)
        }
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.removeCallbacks(release)
                    downAt = SystemClock.uptimeMillis()
                    shape.setColor(PRESSED_COLOR)
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    send(shortcut.key, true)
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
