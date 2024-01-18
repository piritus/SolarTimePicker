package com.example.solartimepicker.solarseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.example.solartimepicker.dp

@SuppressLint("ViewConstructor")
class ProgressStick(
    private val seekBar: BiDirectionalSeekBar,
    private val stickScroller: StickScroller,
    context: Context?,
    private val progressValue: Int
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val dimenH: Int = BiDirectionalSeekBar.STICK_WIDTH.toFloat().coerceAtLeast(seekBar.mStickGap).toInt()
    private var rectF: RectF? = null
    private var dimenV: Int = BiDirectionalSeekBar.STICK_HEIGHT_LINEAR / 2
    private var dimenVZero: Int = BiDirectionalSeekBar.STICK_HEIGHT_LINEAR

    init {
        initStick()
    }

    private fun initStick() {
        initThis()
        initPaint()
    }

    private fun initThis() {
        val params =
            LinearLayout.LayoutParams(
                dimenH,
                if (progressValue == 0 || progressValue % 6 == 0) dimenVZero else dimenV
            )
        params.gravity = Gravity.CENTER_VERTICAL
        layoutParams = params
        setOnClickListener {
            seekBar.changeProgress(progressValue, false, true)
            stickScroller.refreshProgress(true)
        }
    }

    private fun initPaint() {
        paint.style = Paint.Style.FILL
        val centerH = dimenH / 2
        rectF = if (progressValue == 0 || progressValue % 6 == 0) RectF(
            centerH - 1.dp.toFloat() / 2,
            0f,
            centerH + 1.dp.toFloat() / 2,
            dimenVZero.toFloat()
        ) else RectF(
            centerH - 1.dp.toFloat() / 2,
            0f,
            centerH + 1.dp.toFloat() / 2,
            dimenV.toFloat()
        )
    }

    override fun onDraw(canvas: Canvas) {
        paint.color =
            when (progressValue) {
                0 -> {
                    seekBar.zeroStickColor()
                }

                else -> {
                    if (progressValue % 6 == 0) {
                        seekBar.zeroStickColor()
                    } else {
                        seekBar.stickColor()
                    }
                }
            }
        canvas.drawRoundRect(rectF!!, 0f, 0f, paint)
        super.onDraw(canvas)
    }
}