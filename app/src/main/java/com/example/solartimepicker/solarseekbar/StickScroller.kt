package com.example.solartimepicker.solarseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.example.solartimepicker.solarseekbar.HorizonScrollView.OnScrollStartListener
import com.example.solartimepicker.solarseekbar.HorizonScrollView.OnScrollStopListener
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class StickScroller(private val seekBar: BiDirectionalSeekBar, context: Context?) :
    RelativeLayout(context), HorizonScrollView.OnScrollListener, OnScrollStartListener,
    OnScrollStopListener {
    private val gradientPaintL = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientPaintR = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gradientRectL = Rect()
    private val gradientRectR = Rect()
    private val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clipRectL = RectF()
    private val clipRectR = RectF()
    private val arcL = RectF()
    private val arcR = RectF()
    val stickContainer: ArcLinearLayout
    private var stickContainerSuper: LinearLayout? = null
    var scrollView: HorizonScrollView? = null
    private var indicator: Indicator? = null
    private var mSeekBarCenter = 0
    private var fadeLength = 0
    private var fromUser = false
    private var height = 0

    init {
        stickContainer = ArcLinearLayout(context!!)
        init()
    }

    private fun init() {
        initHeight()
        initPaint()
        initThis()
    }

    private fun initPaint() {
        gradientPaintL.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_IN))
        gradientPaintR.setXfermode(PorterDuffXfermode(PorterDuff.Mode.DST_IN))
        clipPaint.setXfermode(null)
    }

    private fun initThis() {
        val params = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        params.addRule(CENTER_HORIZONTAL, TRUE)
        params.addRule(ALIGN_PARENT_BOTTOM, TRUE)
        layoutParams = params
        initScrollView()
        initIndicator()
    }

    private fun initHeight() {
        height = BiDirectionalSeekBar.STICK_HEIGHT_LINEAR * 4
    }

    private fun initScrollView() {
        scrollView = HorizonScrollView(seekBar)
        val params =
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        params.addRule(CENTER_HORIZONTAL, TRUE)
        params.addRule(ALIGN_PARENT_BOTTOM, TRUE)
        scrollView!!.layoutParams = params
        scrollView!!.overScrollMode = OVER_SCROLL_NEVER
        scrollView!!.isHorizontalScrollBarEnabled = false
        scrollView!!.setOnScrollListener(this)
        scrollView!!.setOnScrollStopListener(this)
        scrollView!!.setOnScrollStartListener(this)
        initStickContainer()
        addView(scrollView)
    }

    private fun initIndicator() {
        indicator = Indicator(seekBar, context)
        addView(indicator)
    }

    private fun initStickContainer() {
        stickContainerSuper = LinearLayout(context)
        stickContainerSuper!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            height
        )
        stickContainer.orientation = LinearLayout.HORIZONTAL
        val params =
            LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )//ViewGroup.LayoutParams.WRAP_CONTENT)
        params.addRule(CENTER_IN_PARENT, TRUE)
        stickContainer.layoutParams = params
        stickContainer.gravity = Gravity.CENTER_HORIZONTAL
        stickContainer.clipToPadding = false
        initSticks()
        stickContainerSuper!!.addView(stickContainer)
        scrollView!!.addView(stickContainerSuper)
    }

    fun initSticks() {
        stickContainer.removeAllViews()
        val maxValue = seekBar.maxValue - seekBar.minValue
        for (i in 0..maxValue) {
            val progressStick = ProgressStick(seekBar, this, context, i + seekBar.minValue)
            stickContainer.addView(progressStick)
        }
    }

    fun initPadding(seekBarCenter: Int) {
        mSeekBarCenter = seekBarCenter
        stickContainer.setPadding(seekBarCenter, 0, seekBarCenter, 0)
        fadeLength = seekBarCenter * 4 / 5
    }

    fun getStickContainer(): LinearLayout {
        return stickContainer
    }

    fun getScrollView(): HorizontalScrollView? {
        return scrollView
    }

    fun refreshProgress(animate: Boolean) {
        if (animate) {
            refreshProgressWithAnimation()
            return
        }
        scrollView!!.scrollTo(seekBar.calculateScroll(), 0)
    }

    private fun refreshProgressWithAnimation() {
        val scroll = seekBar.calculateScroll()
        scrollView!!.smoothScrollTo(scroll, 0)
    }

    private fun initFade() {
        val FADE_COLORS = intArrayOf(0x00000000, -0x1000000)
        val FADE_COLORS_REVERSE = intArrayOf(-0x1000000, 0x00000000)
        val actualWidth = width - paddingLeft - paddingRight
        val size = fadeLength.coerceAtMost(actualWidth)
        val l1 = paddingLeft
        val t = paddingTop
        val r1 = l1 + size
        val b = getHeight() - paddingBottom
        gradientRectL[l1, t, r1] = b
        gradientPaintL.setShader(
            LinearGradient(
                l1.toFloat(),
                t.toFloat(),
                r1.toFloat(),
                t.toFloat(),
                FADE_COLORS,
                null,
                Shader.TileMode.CLAMP
            )
        )
        val l2 = paddingLeft + actualWidth - size
        val r2 = l2 + size
        gradientRectR[l2, t, r2] = b
        gradientPaintR.setShader(
            LinearGradient(
                l2.toFloat(),
                t.toFloat(),
                r2.toFloat(),
                t.toFloat(),
                FADE_COLORS_REVERSE,
                null,
                Shader.TileMode.CLAMP
            )
        )
        clipRectL.setEmpty()
        clipRectR.setEmpty()
        arcL.setEmpty()
        arcR.setEmpty()
    }

    override fun dispatchDraw(canvas: Canvas) {
        initFade()
        val count = canvas.saveLayer(
            0.0f,
            0.0f,
            width.toFloat(),
            getHeight().toFloat(),
            null
        )
        super.dispatchDraw(canvas)
        canvas.drawRect(gradientRectL, gradientPaintL)
        canvas.drawRect(gradientRectR, gradientPaintR)
        canvas.restoreToCount(count)
    }

    override fun onScrollChanged(l: Int, t: Int, oldL: Int, oldT: Int) {
        if (!fromUser) return
        val maxVal = seekBar.mMaxVal
        val minVal = seekBar.mMinVal
        val progress = seekBar.mProgress
        val layoutWidth =
            stickContainer.width - (stickContainer.paddingLeft + stickContainer.paddingRight)
        val stickWidth = layoutWidth / stickContainer.childCount
        var stickPosition = l / stickWidth
        if (stickPosition > maxVal + abs(minVal)) stickPosition = maxVal + abs(minVal)
        stickPosition += minVal
        if (stickPosition < minVal) stickPosition = minVal
        if (stickPosition == progress) return
        seekBar.changeProgress(stickPosition, false, fromUser)
    }

    override fun onScrollStart() {
        fromUser = true
    }

    override fun onScrollStopped() {
        refreshProgressWithAnimation()
        fromUser = false
    }
}