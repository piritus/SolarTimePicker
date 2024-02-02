package com.example.solartimepicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.withStyledAttributes
import com.example.solartimepicker.model.ShadowMap
import com.google.android.material.textview.MaterialTextView
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.properties.Delegates


class SunCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    fun interface OnRotateListener {
        fun onRotate(factor: Float)
    }

    // Переменные для перемещения иконки жестом
    private var startX = 0f
    private var startY = 0f
    private var isIconTouched = false

    // Отступ от бейджа (восход/закат) по вертикали
    private val badgeVerticalMargin = 16.dp
    private var currentFact = 0f
    private var iconRect = Rect()
    private var pathPadding = 0
    private var sweepAngle = 180f
    private var angle = 0f
    private var angleStep = 2f
    private var fixAngle = -90f

    private var icon: Drawable? = null

    private var isInitialized = false
    private var isDraggable = false
    private var isScrolling = false

    private var radius: Float by Delegates.notNull()
    private var centerX: Float by Delegates.notNull()
    private var centerY: Float by Delegates.notNull()

    private var endAngle = 0f
    private var direction: ShadowMap.Direction = ShadowMap.Direction.CLOCKWISE
    private var startAngle: Float by Delegates.observable(0f) { _, _, newValue ->
        angle = newValue
    }

    var onRotateListener: OnRotateListener? = null

    // Направление движения
    private var isClockwise: Boolean by Delegates.observable(true) { _, _, newValue ->
        sweepAngle *= -1
        angleStep *= -1
    }

    // Ширина обводки
    private var strokeWidth: Int by Delegates.observable(1.dp) { _, _, newValue ->
        val strokeWidth = newValue.coerceAtLeast(1.dp).toFloat()
        circlePaint.strokeWidth = strokeWidth
    }

    // Длина пунктира
    private var dashLength: Int by Delegates.observable(10.dp) { _, _, newValue ->
        val dashLength = newValue.coerceAtLeast(1.dp).toFloat()

        val dashPathEffect = DashPathEffect(floatArrayOf(dashLength, gapLength.toFloat()), 0f)
        circlePaint.pathEffect = dashPathEffect
    }

    // Расстояние между пунктирами
    private var gapLength: Int by Delegates.observable(5.5.dp) { _, _, newValue ->
        val gapLength = newValue.coerceAtLeast(0.dp).toFloat()

        val dashPathEffect = DashPathEffect(floatArrayOf(dashLength.toFloat(), gapLength), 0f)
        circlePaint.pathEffect = dashPathEffect
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = this@SunCircleView.strokeWidth.toFloat()
        style = Paint.Style.STROKE

        val dashPathEffect =
            DashPathEffect(floatArrayOf(dashLength.toFloat(), gapLength.toFloat()), 0f)
        pathEffect = dashPathEffect
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val badges: List<View>

    init {
        // Создаем бейджи (sunrise/sunset)
        badges = generateBadges(context, attrs, defStyleAttr)
        badges.forEach { addView(it) }

        context.withStyledAttributes(attrs, R.styleable.SunCircleView, defStyleAttr) {
            isDraggable = getBoolean(R.styleable.SunCircleView_SCV_isDraggable, false)
            circlePaint.color =
                getColor(R.styleable.SunCircleView_SCV_pathColor, Color.DKGRAY)

            pathPadding = getDimensionPixelSize(R.styleable.SunCircleView_SCV_padding, 0.dp)
            strokeWidth = getDimensionPixelSize(R.styleable.SunCircleView_SCV_strokeWidth, 1.dp)
            dashLength = getDimensionPixelSize(R.styleable.SunCircleView_SCV_dashLength, 10.dp)
            gapLength = getDimensionPixelSize(R.styleable.SunCircleView_SCV_gapLength, 5.5.dp)
            icon =
                getDrawable(R.styleable.SunCircleView_SCV_icon) ?: AppCompatResources.getDrawable(
                    context,
                    R.drawable.ic_sun
                )
        }
    }

    /** Генерация бейджей по энаму BadgeType */
    private fun generateBadges(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ): List<View> {

        return buildList {
            BadgeType.entries.forEach {
                val tvBadge = MaterialTextView(context, attrs, defStyleAttr).apply {
                    layoutParams =
                        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

                    context.withStyledAttributes(attrs, R.styleable.SunCircleView, defStyleAttr) {
                        (getDrawable(R.styleable.SunCircleView_SCV_label_background)
                            ?: AppCompatResources.getDrawable(
                                context,
                                R.drawable.label_stroke_bg
                            ))?.let {
                            this@apply.background = it
                        }

                        (getString(
                            when (it) {
                                BadgeType.SUNRISE -> R.styleable.SunCircleView_SCV_sunriseText
                                BadgeType.SUNSET -> R.styleable.SunCircleView_SCV_sunsetText
                            }
                        ) ?: run {
                            context.getString(
                                when (it) {
                                    BadgeType.SUNRISE -> R.string.scv_sunrise_text
                                    BadgeType.SUNSET -> R.string.scv_sunset_text
                                }
                            )
                        }).let {
                            this@apply.text = it
                        }

                        getResourceId(R.styleable.SunCircleView_SCV_label_textAppearance, 0)
                            .takeIf { it > 0 }
                            ?.let {
                                setTextAppearance(it)
                            }
                    }

                    tag = it
                }

                add(tvBadge)
            }
        }
    }

    /** Обработка тача для перемещения иконки по окружности */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (!isInitialized || !isDraggable) {
            return false
        }

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            isScrolling = true
            startX = event.x
            startY = event.y

            isIconTouched = iconRect.contains(
                startX.toInt(),
                startY.toInt()
            )
            return true
        } else if (event.actionMasked == MotionEvent.ACTION_UP) {
            isScrolling = false
        } else if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            if (!isIconTouched) {
                return false
            }

            val x = event.x - centerX
            val y = centerY - event.y
            if (x != 0f && y != 0f) {
                val angle = computeAngle(x, y).toFloat()

                val fixStartAngle = startAngle - fixAngle
                val fixEndAngle = endAngle - fixAngle
                val fixAngle = (angle - fixAngle).takeIf { it < 360 } ?: (angle - fixAngle - 360)

                val resultAngle = (getAngleBetween(
                    fixStartAngle,
                    fixEndAngle,
                    fixAngle,
                    direction
                ) ?: return false) + this.fixAngle

                if (this.angle == resultAngle) {
                    return false
                }

//                if (BuildConfig.DEBUG) {
//                    Log.d(
//                        "SunCircleView",
//                        "angle: ${fixAngle}, startAngle: ${fixStartAngle}, endAngle: ${fixEndAngle}, result: ${resultAngle - this.fixAngle}"
//                    )
//                }

                val factor = getFactorByAngle(
                    fixStartAngle,
                    fixEndAngle,
                    fixAngle,
                    direction
                ).let {
                    when (direction) {
                        ShadowMap.Direction.CLOCKWISE -> it
                        ShadowMap.Direction.COUNTERCLOCKWISE -> 1 - it
                    }
                }
                if (BuildConfig.DEBUG) {
                    Log.d(
                        "SunCircleView",
                        "factor: $factor"
                    )
                }
                onRotateListener?.onRotate(factor)

                this.angle = resultAngle

                this.invalidate()
            }
        }

        return true
    }

    private fun getFactorByAngle(
        start: Float,
        end: Float,
        middle: Float,
        direction: ShadowMap.Direction
    ): Float {
        val (start, end) = when (direction) {
            ShadowMap.Direction.CLOCKWISE -> Pair(start, end)
            ShadowMap.Direction.COUNTERCLOCKWISE -> Pair(end, start)
        }
        val modEnd = if ((end - start) < 0f) end - start + 360f else end - start
        val modMid = if ((middle - start) < 0f) middle - start + 360f else middle - start

        val resultMid = if (modMid in modEnd..(modEnd + 10)) modEnd
        else if (modMid > modEnd) 0f
        else modMid

        return resultMid / modEnd
    }

    private fun getAngleBetween(
        s: Float,
        e: Float,
        mid: Float,
        direction: ShadowMap.Direction
    ): Float? {
        val modDigress = 10f
        val (start, end) = when (direction) {
            ShadowMap.Direction.CLOCKWISE -> Pair(s - modDigress, e + modDigress)
            ShadowMap.Direction.COUNTERCLOCKWISE -> Pair(e - modDigress, s + modDigress)
        }
        val modEnd = if ((end - start) < 0f) end - start + 360f else end - start
        val modMid = if ((mid - start) < 0f) mid - start + 360f else mid - start

        return if (modMid < modEnd) {
            if (modMid in (min(modEnd - modDigress, modEnd)..max(modEnd - modDigress, modEnd))) {
                when (direction) {
                    ShadowMap.Direction.CLOCKWISE -> min(mid, e)
                    ShadowMap.Direction.COUNTERCLOCKWISE -> min(mid, s)
                }
            } else if (modMid in 0f..<abs(modDigress)) {
                when (direction) {
                    ShadowMap.Direction.CLOCKWISE -> max(mid, s)
                    ShadowMap.Direction.COUNTERCLOCKWISE -> max(mid, e)
                }
            } else {
                mid
            }
        } else {
            null
        }
    }

    private fun computeAngle(x: Float, y: Float): Double {
        var result = atan2(y.toDouble(), x.toDouble()) * RADS_TO_DEGREES
        if (result < 0) {
            result += 360
        }
        return 360 - result
    }

    fun setSunAngle(factor: Float) {
        if (isScrolling) {
            return
        }

        val factor = factor.coerceIn(0f, 1f)

        angle = startAngle + sweepAngle * factor
        postInvalidate()
    }

    private fun updateSunAngle(factor: Float) {
        angle = startAngle + sweepAngle * (factor + currentFact).coerceIn(0f, 1f)
        postInvalidate()
    }

    fun initialize(shadowMap: ShadowMap) {
        startAngle = shadowMap.sunriseAngle + fixAngle
        endAngle = shadowMap.sunsetAngle + fixAngle
        sweepAngle =
            (abs(shadowMap.sunriseAngle - shadowMap.sunsetAngle).takeIf { it != 0 } ?: 1).toFloat()

        when (shadowMap.direction) {
            ShadowMap.Direction.CLOCKWISE -> {
                if (shadowMap.sunriseAngle > shadowMap.sunsetAngle) {
                    sweepAngle = 360f - shadowMap.sunriseAngle + shadowMap.sunsetAngle
                }
            }

            ShadowMap.Direction.COUNTERCLOCKWISE -> {
                if (shadowMap.sunsetAngle > shadowMap.sunriseAngle) {
                    sweepAngle = 360f - shadowMap.sunsetAngle + shadowMap.sunriseAngle
                }
                isClockwise = false
            }
        }

        direction = shadowMap.direction

        isInitialized = true
    }

    private fun calcPoint(angle: Float): Pair<Float, Float> {
        val pointX = centerX + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val pointY = centerY + radius * sin(Math.toRadians(angle.toDouble())).toFloat()

        return pointX to pointY
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)

        val iconDelta = (icon?.intrinsicWidth?.coerceAtMost(icon?.intrinsicHeight ?: 0) ?: 0) / 2
        radius = (w / 2f) - pathPadding.coerceAtLeast(0.dp).toFloat() - iconDelta
        centerX = w / 2f
        centerY = h / 2f

        badges.forEach {
            measureChild(it, widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // Размещаем бейджи восход/закат внутри контейнера
        badges.forEach { badge ->
            val badgeType = (badge.tag as? BadgeType) ?: return@forEach

            val childSemiWidth = badge.measuredWidth / 2
            val childSemiHeight = badge.measuredHeight / 2

            val pointX: Float
            var pointY: Float

            val iconSemiHeight = (icon?.intrinsicHeight ?: 0) / 2

            val (x, y) = calcPoint(
                when (badgeType) {
                    BadgeType.SUNRISE -> angle
                    BadgeType.SUNSET -> angle + sweepAngle
                }
            )
            pointX =
                if ((x - childSemiWidth) < (pathPadding)) {
                    (l + pathPadding + childSemiWidth).toFloat()
                } else if ((x + childSemiWidth) > (r - l - pathPadding)) {
                    (r - pathPadding - childSemiWidth).toFloat()
                } else {
                    x
                }

            val topY = y - badgeVerticalMargin - iconSemiHeight
            val bottomY = y + badgeVerticalMargin + iconSemiHeight
            val centerCircle = 180 + fixAngle
            pointY = when (direction) {
                ShadowMap.Direction.CLOCKWISE -> {
                    if (startAngle > endAngle) {
                        when (badgeType) {
                            BadgeType.SUNRISE -> {
                                if (startAngle > centerCircle) bottomY
                                else topY
                            }

                            BadgeType.SUNSET -> {
                                if (endAngle < centerCircle) bottomY
                                else topY
                            }
                        }
                    } else {
                        when (badgeType) {
                            BadgeType.SUNRISE -> {
                                if (startAngle < centerCircle) topY
                                else bottomY
                            }

                            BadgeType.SUNSET -> {
                                if (endAngle > centerCircle) topY
                                else bottomY
                            }
                        }
                    }
                }

                ShadowMap.Direction.COUNTERCLOCKWISE -> {
                    if (startAngle > endAngle) {
                        when (badgeType) {
                            BadgeType.SUNRISE -> {
                                if (startAngle > centerCircle) topY
                                else bottomY
                            }

                            BadgeType.SUNSET -> {
                                if (endAngle < centerCircle) topY
                                else bottomY
                            }
                        }
                    } else {
                        when (badgeType) {
                            BadgeType.SUNRISE -> {
                                if (startAngle < centerCircle) bottomY
                                else topY
                            }

                            BadgeType.SUNSET -> {
                                if (endAngle > centerCircle) bottomY
                                else topY
                            }
                        }
                    }
                }
            }

            if ((pointY + childSemiHeight) > (b - t - pathPadding)) {
                pointY = topY
            } else if ((pointY - childSemiHeight) < pathPadding) {
                pointY = bottomY
            }

            val left = pointX.toInt() - childSemiWidth
            val top = pointY.toInt() - childSemiHeight
            val right = pointX.toInt() + childSemiWidth
            val bottom = pointY.toInt() + childSemiHeight

            badge.layout(left, top, right, bottom)
        }
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        return super.drawChild(canvas, child, drawingTime)
    }

    @SuppressLint("DrawAllocation")
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        if (!isInitialized) {
            return
        }

        // Рисуем окружность
        canvas.drawArc(
            centerX - radius, centerY - radius, centerX + radius, centerY + radius,
            startAngle, sweepAngle, false, circlePaint
        )

        // Вычисляем координаты иконки на окружности
        val (pointX, pointY) = calcPoint(angle)

        // Рисуем иконку
        iconRect = icon?.let {
            val left = pointX.toInt() - it.intrinsicWidth / 2
            val top = pointY.toInt() - it.intrinsicHeight / 2
            val right = pointX.toInt() + it.intrinsicWidth / 2
            val bottom = pointY.toInt() + it.intrinsicHeight / 2
            it.setBounds(left, top, right, bottom)
            it.draw(canvas)

            Rect(left, top, right, bottom)
        } ?: run {
            val iconRadius = 10f
            canvas.drawCircle(pointX, pointY, iconRadius, pointPaint)
            Rect(
                (pointX - iconRadius).toInt(),
                (pointY - iconRadius).toInt(),
                (pointX + iconRadius).toInt(),
                (pointY + iconRadius).toInt(),
            )
        }
    }

    private enum class BadgeType {
        SUNRISE, SUNSET
    }

    companion object {
        private const val RADS_TO_DEGREES = 360 / (Math.PI * 2)
    }
}