package com.example.solartimepicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Scroller
import androidx.core.content.withStyledAttributes
import androidx.core.view.GestureDetectorCompat
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.properties.Delegates

class HorizontalWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    fun interface OnRotateListener {
        fun onRotate(rangePercent: Float, tick: Int, tickOffset: Float)
    }

    private var tickDegreesDelta = 0f
    private var tickCenterY = 0f
    private var wheelCenterX = 0f
    private var wheelCenterY = 0f
    private var wheelArcLength = 0f
    private var isScrolling = false

    private var radius = 0f
        set(value) { field = value; recalculateWheelArcLength() }

    private var scrollDistance = 0f
        set(value) {
            field = value.coerceIn(0f, wheelArcLength)
            onRotateListener?.let {
                val tickIndex = calculateClosestTickIndex(field)
                val tickScrollDelta = calculateTickScrollDistance(1)
                val wheelArcLength = wheelArcLength
                if (tickIndex >= 0 && tickScrollDelta > 0 && wheelArcLength > 0) {
                    val percent = field / wheelArcLength
                    val tickScrollDistance = calculateTickScrollDistance(tickIndex)
                    val scrollDelta = tickScrollDistance - field
                    if (scrollDelta > 0) {
                        it.onRotate(percent, tickIndex - 1, 1 - scrollDelta / tickScrollDelta)
                    } else {
                        it.onRotate(percent, tickIndex, scrollDelta.absoluteValue / tickScrollDelta)
                    }
                }
            }
        }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val intermediateTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val labelTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    var tickHeight: Int = 24

    var tickWidth: Int by Delegates.observable(2) { _, _, newValue ->
        val strokeWidth = newValue.coerceAtLeast(1).toFloat()
        tickPaint.strokeWidth = strokeWidth
        intermediateTickPaint.strokeWidth = strokeWidth
        labelTickPaint.strokeWidth = strokeWidth * 2f
    }

    var tickCount: Int = 72
        set(value) {
            field = value.coerceAtLeast(1)
            recalculateTickDegreesDelta()
        }

    var intermediateTickCount: Int = 5
        set(value) { field = value.coerceAtLeast(0) }

    var tickDegreesRange: Int = 120
        set(value) {
            field = value.coerceIn(0, 360)
            recalculateWheelArcLength()
            recalculateTickDegreesDelta()
        }

    var tickFadingEdgePercent: Int = 40
        set(value) { field = value.coerceIn(0, 100) }

    var onRotateListener: OnRotateListener? = null

    private val scroller = Scroller(context, AccelerateDecelerateInterpolator())

    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            isScrolling = false
            scroller.fling(
                scrollDistance.toInt(), 0,
                -velocityX.toInt(), 0,
                0, wheelArcLength.toInt(),
                0, 0
            )
            postInvalidateOnAnimation()
            return true
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            isScrolling = true
            scrollDistance += distanceX
            postInvalidate()
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            isScrolling = false
            if (!scroller.isFinished) scroller.abortAnimation()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val distanceDelta = coerceDistanceDelta(e.x - wheelCenterX).toInt()
            return if (distanceDelta != 0) {
                scroller.startScroll(scrollDistance.toInt(), 0, distanceDelta, 0)
                postInvalidateOnAnimation()
                performClick()
                true
            } else {
                false
            }
        }
    }

    private val gestureDetector = GestureDetectorCompat(context, gestureListener)

    init {
        recalculateTickDegreesDelta()
        context.withStyledAttributes(attrs, R.styleable.HorizontalWheelView, defStyleAttr) {
            tickPaint.color =
                getColor(R.styleable.HorizontalWheelView_tickColor, Color.DKGRAY)
            intermediateTickPaint.color =
                getColor(R.styleable.HorizontalWheelView_intermediateTickColor, Color.GRAY)
            labelTickPaint.color =
                getColor(R.styleable.HorizontalWheelView_labelTickColor, Color.BLACK)
            tickWidth = getDimensionPixelSize(R.styleable.HorizontalWheelView_tickWidth, 2)
            tickHeight = getDimensionPixelSize(R.styleable.HorizontalWheelView_tickHeight, 24)
            tickDegreesRange = getInt(R.styleable.HorizontalWheelView_tickDegreesRange, 120)
            tickFadingEdgePercent = getInt(R.styleable.HorizontalWheelView_tickFadingEdgePercent, 40)
            tickCount = getInt(R.styleable.HorizontalWheelView_tickCount, 72)
            intermediateTickCount = getInt(R.styleable.HorizontalWheelView_intermediateTickCount, 5)
        }
    }

    fun setRotate(percent: Float, coerced: Boolean = false) {
        val targetScrollDistance = percent.coerceIn(0f, 100f) * wheelArcLength //TODO скорее всего, coerceIn(0f, 1f)
        if (coerced) {
            if (!scroller.isFinished) scroller.abortAnimation()
            isScrolling = false
            val distanceDelta = coerceDistanceDelta(targetScrollDistance - scrollDistance).toInt()
            if (distanceDelta != 0) {
                scroller.startScroll(scrollDistance.toInt(), 0, distanceDelta, 0)
                postInvalidateOnAnimation()
            }
        } else {
            scrollDistance = targetScrollDistance
            postInvalidate()
        }
    }

    fun coerceRotate() {
        if (!scroller.isFinished) scroller.abortAnimation()
        isScrolling = false
        coerceScrollDistance()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) return true

        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isScrolling) {
                    isScrolling = false
                    computeScroll()
                    return true
                }
            }
        }

        return false
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollDistance = scroller.currX.toFloat()
            postInvalidateOnAnimation()
        } else if (!isScrolling) {
            coerceScrollDistance()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val paddedWidth = w - paddingLeft - paddingRight
        radius = paddedWidth * 2f
        tickCenterY = paddingTop + (h - paddingTop - paddingBottom) * 0.4f
        wheelCenterY = radius + tickCenterY
        wheelCenterX = paddingLeft + paddedWidth * 0.5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val tickDegreesDelta = tickDegreesDelta
        val wheelArcLength = wheelArcLength
        val tickLineStartY = tickCenterY - tickHeight * 0.7f
        val tickLineStopY = tickCenterY + tickHeight + 1.3f

        if (wheelArcLength > 0 && tickDegreesDelta > 0) {
            val currentRotateDegrees = -scrollDistance * tickDegreesRange / wheelArcLength
            val halfTickHeight = tickHeight / 2f
            val intermediateTickLineStartY = tickCenterY - halfTickHeight
            val intermediateTickLineStopY = tickCenterY + halfTickHeight
            val endFadingDelta = wheelCenterX - paddingLeft
            val startFadingDelta = endFadingDelta * (1 - tickFadingEdgePercent / 100f)
            val fadingDelta = endFadingDelta - startFadingDelta

            canvas.save()
            canvas.rotate(currentRotateDegrees, wheelCenterX, wheelCenterY)
            for (index in 0..tickCount) {
                val paintAlpha = if (fadingDelta > 0) {
                    val tickScrollDistance = degreesToScrollDistance(tickDegreesDelta * index)
                    val scrollDelta = (scrollDistance - tickScrollDistance).absoluteValue
                    when {
                        scrollDelta < startFadingDelta -> 255
                        scrollDelta < endFadingDelta ->
                            (255 * (endFadingDelta - scrollDelta) / fadingDelta).roundToInt()
                        else -> 0
                    }
                } else {
                    255
                }
                if (intermediateTickCount == 0 || index % (intermediateTickCount + 1) == 0) {
                    tickPaint.alpha = paintAlpha
                    canvas.drawLine(
                        wheelCenterX, tickLineStartY,
                        wheelCenterX, tickLineStopY,
                        tickPaint
                    )
                } else {
                    intermediateTickPaint.alpha = paintAlpha
                    canvas.drawLine(
                        wheelCenterX, intermediateTickLineStartY,
                        wheelCenterX, intermediateTickLineStopY,
                        intermediateTickPaint
                    )
                }
                canvas.rotate(tickDegreesDelta, wheelCenterX, wheelCenterY)
            }
            canvas.restore()
        }

        canvas.drawLine(wheelCenterX, 0f, wheelCenterX, tickLineStopY, labelTickPaint)
    }

    private fun recalculateWheelArcLength() {
        wheelArcLength = (2 * Math.PI * radius * tickDegreesRange / 360).toFloat()
    }

    private fun recalculateTickDegreesDelta() {
        tickDegreesDelta = if (tickCount > 0) tickDegreesRange / tickCount.toFloat() else 0f
    }

    private fun coerceScrollDistance() {
        val distanceDelta = coerceDistanceDelta(0f)
        if (distanceDelta.absoluteValue > 1) {
            scroller.startScroll(scrollDistance.toInt(), 0, distanceDelta.toInt(), 0)
            postInvalidateOnAnimation()
        } else {
            scrollDistance += distanceDelta
        }
    }

    private fun calculateClosestTickIndex(scrollDistance: Float): Int {
        val tickDegreesDelta = tickDegreesDelta
        if (tickDegreesDelta <= 0) return -1

        return (scrollDistanceToDegrees(scrollDistance) / tickDegreesDelta).roundToInt()
    }

    private fun calculateTickScrollDistance(tickIndex: Int): Float {
        val tickDegreesDelta = tickDegreesDelta
        if (tickIndex < 0 || tickDegreesDelta <= 0) return scrollDistance

        return degreesToScrollDistance(tickDegreesDelta * tickIndex)
    }

    private fun coerceDistanceDelta(distanceDelta: Float): Float {
        val targetTickIndex = calculateClosestTickIndex(scrollDistance + distanceDelta)
        if (targetTickIndex < 0) return 0f

        val coercedScrollDistance = calculateTickScrollDistance(targetTickIndex)
        return coercedScrollDistance - scrollDistance
    }

    private fun degreesToScrollDistance(degrees: Float) =
        if (tickDegreesRange > 0) degrees * wheelArcLength / tickDegreesRange else 0f

    private fun scrollDistanceToDegrees(scrollDistance: Float) =
        if (wheelArcLength > 0) scrollDistance * tickDegreesRange / wheelArcLength else 0f
}