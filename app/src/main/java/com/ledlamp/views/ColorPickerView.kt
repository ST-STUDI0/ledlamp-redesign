package com.ledlamp.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var selectedColor = Color.WHITE
    private var colorChangeListener: ((Int) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private var selectorX = 0f
    private var selectorY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = min(centerX, centerY) * 0.8f
        
        // Initialize selector at center (white)
        selectorX = centerX
        selectorY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw color wheel
        val colors = IntArray(360)
        for (i in 0 until 360) {
            colors[i] = Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f))
        }

        val sweepGradient = SweepGradient(centerX, centerY, colors, null)
        val radialGradient = RadialGradient(
            centerX, centerY, radius,
            Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP
        )

        paint.shader = ComposeShader(sweepGradient, radialGradient, PorterDuff.Mode.SRC_OVER)
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Draw selector
        canvas.drawCircle(selectorX, selectorY, 20f, selectorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = sqrt(dx * dx + dy * dy)

                if (distance <= radius) {
                    selectorX = event.x
                    selectorY = event.y
                } else {
                    // Constrain to circle edge
                    val angle = atan2(dy, dx)
                    selectorX = centerX + radius * cos(angle)
                    selectorY = centerY + radius * sin(angle)
                }

                selectedColor = getColorAtPoint(selectorX, selectorY)
                colorChangeListener?.invoke(selectedColor)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getColorAtPoint(x: Float, y: Float): Int {
        val dx = x - centerX
        val dy = y - centerY
        val distance = sqrt(dx * dx + dy * dy)
        
        // Calculate hue from angle
        val angle = atan2(dy, dx)
        val hue = (Math.toDegrees(angle.toDouble()) + 360) % 360
        
        // Calculate saturation from distance
        val saturation = min(distance / radius, 1f)
        
        return Color.HSVToColor(floatArrayOf(hue.toFloat(), saturation, 1f))
    }

    fun setOnColorChangeListener(listener: (Int) -> Unit) {
        colorChangeListener = listener
    }

    fun setColor(color: Int) {
        selectedColor = color
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        
        val angle = Math.toRadians(hsv[0].toDouble())
        val dist = hsv[1] * radius
        
        selectorX = centerX + (dist * cos(angle)).toFloat()
        selectorY = centerY + (dist * sin(angle)).toFloat()
        
        invalidate()
    }

    fun getColor(): Int = selectedColor
}
