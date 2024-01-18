package com.example.solartimepicker.solarseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.widget.RelativeLayout
import com.example.solartimepicker.dp

@SuppressLint("ViewConstructor")
class Indicator(private val seekBar: BiDirectionalSeekBar, context: Context?) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private var mWidth = 0
    private val dimenH: Int = BiDirectionalSeekBar.STICK_WIDTH
    private var dimenV: Int = BiDirectionalSeekBar.INDICATOR_HEIGHT_LINEAR
    private var rectF: RectF? = null

    init {
        init()
    }

    private fun init() {
        val params = RelativeLayout.LayoutParams(dimenH, dimenV)
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        layoutParams = params
        initPaint()
    }

    private fun initPaint() {
        initRect()
        paint.style = Paint.Style.FILL
    }

    private fun initRect() {
        val centerH = dimenH / 2
        val l = centerH - 2.dp.toFloat()/2
        val r = centerH + 2.dp.toFloat()/2
        mWidth = r.toInt()
        rectF = RectF(l, 0f, r, dimenV.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        paint.color = seekBar.mIndicatorColor
        canvas.drawRoundRect(rectF!!, 0f, 0f, paint)
        super.onDraw(canvas)
    }
}