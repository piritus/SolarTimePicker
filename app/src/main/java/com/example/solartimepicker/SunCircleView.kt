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
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.withStyledAttributes
import com.example.solartimepicker.model.ShadowMap
import com.google.android.material.textview.MaterialTextView
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.properties.Delegates


class SunCircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

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

    private var radius: Float by Delegates.notNull()
    private var centerX: Float by Delegates.notNull()
    private var centerY: Float by Delegates.notNull()

    private var endAngle = 0f
    private var direction: ShadowMap.Direction = ShadowMap.Direction.CLOCKWISE
    private var startAngle: Float by Delegates.observable(0f) { _, _, newValue ->
        angle = newValue
    }

    // Ширина обводки
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

    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            var startX = 0f
            var startY = 0f
            var isIconTouched = false

            override fun onDown(e: MotionEvent): Boolean {
                startX = e.x
                startY = e.y

                isIconTouched = iconRect.contains(
                    startX.toInt(),
                    startY.toInt()
                )

                if (isIconTouched) {
                    currentFact = (angle - startAngle) / sweepAngle
                }

                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!isIconTouched) {
                    return false
                }

                val distX = e2.x - (e1?.x ?: width.toFloat())
                val distY = (e1?.y ?: height.toFloat()) - e2.y
                val commonFactor = distX / width + distY / height
                updateSunAngle(commonFactor)

                return true
            }
        })

    private val badges: List<View>

    init {
        // Создаем бейджи (sunrise/sunset)
        badges = generateBadges(context, attrs, defStyleAttr)
        badges.forEach { addView(it) }

        context.withStyledAttributes(attrs, R.styleable.SunCircleView, defStyleAttr) {
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
                        getDrawable(R.styleable.SunCircleView_SCV_label_background)?.let {
                            this@apply.background = it
                        }

                        getString(
                            when (it) {
                                BadgeType.SUNRISE -> R.styleable.SunCircleView_SCV_sunriseText
                                BadgeType.SUNSET -> R.styleable.SunCircleView_SCV_sunsetText
                            }
                        )?.let {
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

    /** Раскомментировать, если нужно будет двигать за иконку */
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        return gestureDetector.onTouchEvent(event)
//    }

    fun setSunAngle(factor: Float) {
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
                } else if ((x + childSemiWidth) > (r -l - pathPadding)) {
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
}