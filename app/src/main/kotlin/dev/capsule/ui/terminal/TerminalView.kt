package dev.capsule.ui.terminal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import kotlinx.coroutines.*
import java.nio.ByteBuffer

class TerminalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var onDataReady: ((ByteArray) -> Unit)? = null

    private val charWidth = 12f
    private val charHeight = 20f
    private val columns = 80
    private val rows = 24

    private val backgroundPaint = Paint().apply {
        color = 0xFF0D1117.toInt()
        style = Paint.Style.FILL
    }

    private val foregroundPaint = Paint().apply {
        color = 0xFF00FF41.toInt()
        typeface = Typeface.MONOSPACE
        textSize = 16f
        isAntiAlias = true
    }

    private val cursorPaint = Paint().apply {
        color = 0xFF00FF41.toInt()
        style = Paint.Style.FILL
    }

    private val charBuffer = Array(rows) { CharArray(columns) }
    private val colorBuffer = Array(rows) { IntArray(columns) }
    private var cursorRow = 0
    private var cursorCol = 0
    private var cursorVisible = true

    private var fontSize = 16f

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        for (row in 0 until rows) {
            for (col in 0 until columns) {
                if (charBuffer[row][col] != ' ') {
                    foregroundPaint.color = colorBuffer[row][col]
                    canvas.drawText(
                        charBuffer[row][col].toString(),
                        col * charWidth + 4,
                        row * charHeight + charHeight - 4,
                        foregroundPaint
                    )
                }
            }
        }

        if (cursorVisible) {
            canvas.drawRect(
                cursorCol * charWidth + 2,
                cursorRow * charHeight + 2,
                cursorCol * charWidth + charWidth - 2,
                cursorRow * charHeight + charHeight - 2,
                cursorPaint
            )
        }

        cursorVisible = !cursorVisible
        postInvalidateDelayed(500)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
    }

    fun append(data: ByteArray) {
        val text = String(data, Charsets.UTF_8)
        for (c in text) {
            when {
                c == '\r' -> cursorCol = 0
                c == '\n' -> {
                    cursorRow++
                    if (cursorRow >= rows) {
                        scrollUp()
                        cursorRow = rows - 1
                    }
                }
                c == '\t' -> cursorCol = (cursorCol + 8) / 8 * 8
                c.code >= 32 -> {
                    charBuffer[cursorRow][cursorCol] = c
                    colorBuffer[cursorRow][cursorCol] = 0xFF00FF41.toInt()
                    cursorCol++
                    if (cursorCol >= columns) {
                        cursorCol = 0
                        cursorRow++
                        if (cursorRow >= rows) {
                            scrollUp()
                            cursorRow = rows - 1
                        }
                    }
                }
                c == 127 -> {
                    if (cursorCol > 0) {
                        cursorCol--
                        charBuffer[cursorRow][cursorCol] = ' '
                    }
                }
                c.code < 32 -> {
                    handleControlChar(c.code)
                }
            }
        }
        invalidate()
    }

    private fun handleControlChar(code: Int) {
        when (code) {
            7 -> {}
            8 -> if (cursorCol > 0) cursorCol--
            27 -> {}
        }
    }

    private fun scrollUp() {
        for (row in 0 until rows - 1) {
            charBuffer[row] = charBuffer[row + 1].copyOf()
            colorBuffer[row] = colorBuffer[row + 1].copyOf()
        }
        charBuffer[rows - 1] = CharArray(columns)
        colorBuffer[rows - 1] = IntArray(columns) { 0xFF00FF41.toInt() }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val keyChar = event?.unicodeChar ?: 0
        if (keyChar > 0) {
            val bytes = ByteBuffer.allocate(4).putChar(keyChar.toChar()).array()
            onDataReady?.invoke(bytes.filter { it != 0.toByte() }.toByteArray())
            return true
        }

        when (keyCode) {
            KeyEvent.KEYCODE_ENTER -> onDataReady?.invoke(byteArrayOf(10))
            KeyEvent.KEYCODE_DEL -> onDataReady?.invoke(byteArrayOf(127))
            KeyEvent.KEYCODE_TAB -> onDataReady?.invoke(byteArrayOf(9))
            KeyEvent.KEYCODE_ESCAPE -> onDataReady?.invoke(byteArrayOf(27))
            KeyEvent.KEYCODE_DPAD_UP -> onDataReady?.invoke(byteArrayOf(27, 91, 65))
            KeyEvent.KEYCODE_DPAD_DOWN -> onDataReady?.invoke(byteArrayOf(27, 91, 66))
            KeyEvent.KEYCODE_DPAD_RIGHT -> onDataReady?.invoke(byteArrayOf(27, 91, 67))
            KeyEvent.KEYCODE_DPAD_LEFT -> onDataReady?.invoke(byteArrayOf(27, 91, 68))
            else -> return false
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        return object : BaseInputConnection(this, true) {
            override fun commitText(text: CharSequence?, cursorPosition: Int): Boolean {
                text?.let {
                    val bytes = it.toString().toByteArray(Charsets.UTF_8)
                    onDataReady?.invoke(bytes)
                }
                return true
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                onDataReady?.invoke(byteArrayOf(127))
                return true
            }
        }
    }

    fun resize(cols: Int, rows: Int) {
        invalidate()
    }
}