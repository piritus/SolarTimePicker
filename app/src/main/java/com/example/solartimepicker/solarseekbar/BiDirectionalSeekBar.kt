package com.example.solartimepicker.solarseekbar

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.example.solartimepicker.R
import com.example.solartimepicker.dp

class BiDirectionalSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object{
        val STICK_WIDTH = 4.dp

        val STICK_HEIGHT_LINEAR = 16.dp
        val INDICATOR_HEIGHT_LINEAR = 23.dp
        val STICK_MIN_HEIGHT = 30.dp
    }

    var mStickGap: Float
    var mIndicatorColor: Int
    var mMinVal: Int
    var mMaxVal: Int
    var mProgress: Int
    var mLabelColor: Int

    var labelView: LabelView? = null
    var mSeekBarChangeListener: OnSeekBarChangeListener? = null
    private var mStickColor: Int
    private var mZeroStickColor: Int
    private var parentWidth = 0
    private var innerContainer: RelativeLayout? = null
    private var mProgressChangeListener: OnProgressChangeListener? = null
    private var mStickScroller: StickScroller? = null
    private var stickContainer: LinearLayout? = null
    private var scrollView: HorizontalScrollView? = null
    private var finalDimenSet = false
    private var layoutReady = false

    init {
        val theme = getContext().theme
        val a =
            context.obtainStyledAttributes(attrs, R.styleable.BiDirectionalSeekBar, defStyleAttr, 0)
        val a2 = theme.obtainStyledAttributes(
            intArrayOf(
                android.R.attr.textColorPrimary,
                android.R.attr.textColorSecondaryInverse
            )
        )
        val priTextColor = a2.getColor(0, -0x1000000)
        val secTextColor = a2.getColor(0, -0xddddde)
        a2.recycle()
        mMinVal = a.getInt(R.styleable.BiDirectionalSeekBar_BDS_minValue, 0)
        mMaxVal = a.getInt(R.styleable.BiDirectionalSeekBar_BDS_maxValue, 100)
        mProgress = a.getInt(R.styleable.BiDirectionalSeekBar_BDS_progress, 0)
        mIndicatorColor = a.getColor(
            R.styleable.BiDirectionalSeekBar_BDS_indicatorColor,
            -0x9dff12
        )
        mStickColor = a.getColor(R.styleable.BiDirectionalSeekBar_BDS_stickColor, secTextColor)
        mZeroStickColor =
            a.getColor(R.styleable.BiDirectionalSeekBar_BDS_zeroStickColor, priTextColor)
        mLabelColor = a.getColor(R.styleable.BiDirectionalSeekBar_BDS_labelColor, priTextColor)
        mStickGap =
            a.getDimension(R.styleable.BiDirectionalSeekBar_BDS_stickGap, STICK_WIDTH.toFloat())
        a.recycle()
        init(context)
    }

    private fun init(context: Context) {
        initThis()
        initInnerContainer(context)
    }

    private fun initThis() {
        orientation = VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setPadding(0, 0, 0, 5)
    }

    private fun initInnerContainer(context: Context) {
        innerContainer = RelativeLayout(context)
        val params = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            STICK_HEIGHT_LINEAR * 3
        )
        innerContainer!!.layoutParams = params
        initStickScroller()
        initLabel()
        addView(innerContainer)
    }

    private fun updateInnerParams() {
        val params = innerContainer!!.layoutParams
        params.height = STICK_HEIGHT_LINEAR * 3
        innerContainer!!.layoutParams = params
    }

    private fun initFinalDimen() {
        if (finalDimenSet) return
        finalDimenSet = true
        mStickScroller?.initPadding(parentWidth shr 1)
    }

    private fun initStickScroller() {
        mStickScroller = StickScroller(this, context)
        stickContainer = mStickScroller?.stickContainer
        scrollView = mStickScroller?.scrollView
        innerContainer?.addView(mStickScroller)
    }

    private fun initLabel() {
        labelView = LabelView(this, context)
        labelView?.setText(mProgress.toString())
        innerContainer?.addView(labelView)
    }

    fun changeProgress(progress: Int, internal: Boolean, fromUser: Boolean) {
        val actProgress = progress.coerceAtLeast(mMinVal)
            .coerceAtMost(mMaxVal)
        mProgress = actProgress
        labelView?.setText(actProgress.toString())
        if (mProgressChangeListener != null && !internal) mProgressChangeListener?.onProgressChanged(
            this,
            actProgress,
            fromUser
        )
        if (mSeekBarChangeListener != null && !internal) mSeekBarChangeListener?.onProgressChanged(
            this,
            actProgress,
            fromUser
        )
    }

    fun calculateScroll(): Int {
        val progress = mProgress - mMinVal
        val stick = stickContainer!!.getChildAt(progress)
        if (stick is ProgressStick) {
            val stickWidth = stick.getWidth()
            return progress * stickWidth + (stickWidth shr 1)
        }
        return 0
    }

    fun stickColor(): Int {
        return mStickColor
    }

    fun zeroStickColor(): Int {
        return mZeroStickColor
    }

    fun setIndicatorColor(color: Int) {
        mIndicatorColor = color
        labelView!!.setLabelBGColor()
    }

    fun setStickColor(color: Int) {
        mStickColor = color
    }

    fun setZeroStickColor(color: Int) {
        mZeroStickColor = color
    }

    fun setLabelColor(color: Int) {
        mLabelColor = color
        labelView!!.setLabelColor(color)
    }

    var maxValue: Int
        get() = mMaxVal
        set(maxValue) {
            mMaxVal = maxValue
            mStickScroller?.initSticks()
            changeProgress(mProgress, true, false)
            mStickScroller?.refreshProgress(false)
        }
    var minValue: Int
        get() = mMinVal
        set(minValue) {
            mMinVal = minValue
            mStickScroller?.initSticks()
            changeProgress(mProgress, true, false)
            mStickScroller?.refreshProgress(false)
        }
    var progress: Int
        get() = mProgress
        set(progress) {
            setProgress(progress, true)
        }

    fun setProgress(progress: Int, animate: Boolean) {
        changeProgress(progress, false, false)
        mStickScroller?.refreshProgress(animate && layoutReady)
    }

    fun setStickGap(gap: Int) {
        mStickGap = gap.toFloat()
        mStickScroller?.initSticks()
    }

    fun setOnProgressChangeListener(listener: OnProgressChangeListener) {
        mProgressChangeListener = listener
    }

    fun setOnSeekBarChangeListener(listener: OnSeekBarChangeListener) {
        mSeekBarChangeListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (finalDimenSet) return
        parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        initFinalDimen()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (!layoutReady) {
            scrollView?.scrollTo(calculateScroll(), 0)
        }
        layoutReady = true
    }

    interface OnProgressChangeListener {
        fun onProgressChanged(seekBar: BiDirectionalSeekBar?, progress: Int, fromUser: Boolean)
    }

    interface OnSeekBarChangeListener {
        fun onStartTrackingTouch(seekBar: BiDirectionalSeekBar?)

        fun onProgressChanged(seekBar: BiDirectionalSeekBar?, progress: Int, fromUser: Boolean)

        fun onStopTrackingTouch(seekBar: BiDirectionalSeekBar?)
    }
}