package com.example.solartimepicker.solarseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.example.solartimepicker.dp

@SuppressLint("ViewConstructor")
class HorizonScrollView : HorizontalScrollView {
    private var scrollStart = false
    private lateinit var runnable: Runnable
    private var startListener: OnScrollStartListener? = null
    private var scrollListener: OnScrollListener? = null
    private var stopListener: OnScrollStopListener? = null
    private var prevPosition = 0

    private var circleRadius = 1800.dp.toFloat()
    private var arcWidth = 0
    private var startOffset = 0
    private var arcHeight = 0
    private var prevChildBottom = 0

    private var seekBar: BiDirectionalSeekBar? = null

    constructor(seekBar: BiDirectionalSeekBar) : super(seekBar.context) {
        this.seekBar = seekBar
        startUp(context, null)
    }

    constructor(context: Context) : super(context) {
        startUp(context, null)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        startUp(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        startUp(context, attrs)
    }

    init {
        requestDisallowInterceptTouchEvent(true)
        runnable = Runnable {
            if (prevPosition - scrollX == 0) {
                if (stopListener != null) stopListener?.onScrollStopped()
            } else {
                prevPosition = scrollX
                postDelayed(runnable, 100)
            }
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldL: Int, oldT: Int) {
        if (scrollListener != null) scrollListener?.onScrollChanged(l, t, oldL, oldT)
        if (scrollStart) {
            if (startListener != null) startListener?.onScrollStart()
            scrollStart = false
        }
        super.onScrollChanged(l, t, oldL, oldT)

        notifyScroll()
    }

    fun setOnScrollStartListener(startListener: OnScrollStartListener) {
        this.startListener = startListener
    }

    fun setOnScrollListener(scrollListener: OnScrollListener) {
        this.scrollListener = scrollListener
    }

    fun setOnScrollStopListener(stopListener: OnScrollStopListener) {
        this.stopListener = stopListener
    }

    fun startScrollerTask() {
        prevPosition = scrollX
        postDelayed(runnable, 100)
    }

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        if (seekBar == null) {
            return super.dispatchTouchEvent(e)
        }

        if (e.action == MotionEvent.ACTION_DOWN) {
            scrollStart = true
            if (seekBar!!.mSeekBarChangeListener != null) seekBar!!.mSeekBarChangeListener?.onStartTrackingTouch(
                seekBar
            )
        }
        if (e.action == MotionEvent.ACTION_UP) {
            if (seekBar!!.mSeekBarChangeListener != null) seekBar!!.mSeekBarChangeListener?.onStopTrackingTouch(
                seekBar
            )
            startScrollerTask()
        }
        return super.dispatchTouchEvent(e)
    }


    interface OnScrollStartListener {
        fun onScrollStart()
    }

    interface OnScrollListener {
        fun onScrollChanged(l: Int, t: Int, oldL: Int, oldT: Int)
    }

    interface OnScrollStopListener {
        fun onScrollStopped()
    }

    private fun startUp(
        context: Context,
        attrs: AttributeSet?
    ) {
        if (circleRadius == 0f) throw RuntimeException("You need to specify radius")
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        screenWidth = size.x.toFloat()
        isHorizontalScrollBarEnabled = true//false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var visibleWidth = 0
        if (childCount == 1) {
            val item = (getChildAt(0) as LinearLayout)
            visibleWidth =
                (item.getChildAt(0) as ArcLinearLayout).getWidthOfTheVisibleCircle(
                    circleRadius,
                    prevChildBottom
                )
        }
        if (MeasureSpec.getSize(widthMeasureSpec) < visibleWidth || visibleWidth == 0) {
            super.onMeasure(widthMeasureSpec, measureHeight(heightMeasureSpec))
        } else {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(visibleWidth, MeasureSpec.EXACTLY),
                measureHeight(heightMeasureSpec)
            )
        }
    }

    private fun measureHeight(measureSpec: Int): Int {
        Log.d(TAG, "measureHeight: $measureSpec")
        var result = prevChildBottom
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        }
        return MeasureSpec.makeMeasureSpec(result, MeasureSpec.EXACTLY)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)

        startOffset = left

        val item = (getChildAt(0) as LinearLayout)
        val arcLayout = item.getChildAt(0) as ArcLinearLayout
        arcHeight = arcLayout.height
        arcWidth = arcLayout.width
        if (arcHeight != 0 && arcWidth != 0) {
            setMeasurements()
            arcLayout.headsUp()
            notifyScroll()
        }

    }

    private fun notifyScroll() {
        try {
            val item = (getChildAt(0) as LinearLayout)
            (item.getChildAt(0) as ArcLinearLayout).notifyKids(startOffset)
        } catch (e: ClassCastException) {
            throw RuntimeException("ArcLinearLayout not found")
        }
    }

    private fun setMeasurements() {
        val item = (getChildAt(0) as LinearLayout)
        val child = item.getChildAt(0) as ArcLinearLayout

        child.setRadius(circleRadius)
        child.setContainerHeight(arcHeight)
        child.setContainerWidth(arcWidth)
    }

    companion object {
        private const val TAG = "HorizonScrollView"
        var screenWidth = 0f
    }
}