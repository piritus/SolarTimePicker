package com.example.solartimepicker.solarseekbar

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import com.example.solartimepicker.dp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class ArcLinearLayout : LinearLayout {
    private var radiusPow2 = 0f
    private var radius = 0f
    private var containerWidth = 0
    private var containerHeight = 0
    private var startOffset = 0

    constructor(context: Context) : super(context) {
        starUp(context, null)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        starUp(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        starUp(context, attrs)
    }

    private fun starUp(
        context: Context,
        attrs: AttributeSet?
    ) {
        val minimumPadding = BiDirectionalSeekBar.STICK_MIN_HEIGHT
        orientation = HORIZONTAL

        setPadding(
            minimumPadding + paddingLeft,
            paddingTop,
            minimumPadding + paddingRight,
            paddingBottom
        )
    }

    fun getWidthOfTheVisibleCircle(radius: Float, strokeWidth: Int): Int {
        radiusPow2 = radius.toDouble().pow(2.0).toFloat()

        return (2 * sqrt(2 * (radius * strokeWidth) - strokeWidth * strokeWidth.toDouble())).toInt()
    }

    fun headsUp() {
        if (width != 0) {
            val yPow2 = (containerHeight - radius.toDouble()).pow(2.0).toFloat()
            var x = (-(sqrt(
                abs(radiusPow2 - yPow2).toDouble()
            ) - containerWidth / 2)).toFloat()
            if (containerWidth > width) {
                x += containerWidth / 2 - x - width / 2
                val config = resources.configuration
                translationX = if (config.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                    -x
                } else x
            } else {
                try {
                    (parent.parent as HorizonScrollView).scrollTo(
                        (width - containerWidth) / 2,
                        0
                    )
                } catch (e: ClassCastException) {
                    throw RuntimeException("ArcLinearLayout not found")
                }
            }
        }
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int
    ) {
        super.onLayout(changed, l, t, r, b)
        headsUp()
        notifyKids(startOffset)
    }

    fun notifyKids(startOffset: Int) {
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val screenWidth = size.x
        val screenWidthPow2 = screenWidth.toDouble().pow(2).toFloat()

        this.startOffset = startOffset
        val pos = IntArray(2)
        val count = childCount
        var currChild: View
        var y: Float
        radiusPow2 = radius.toDouble().pow(2.0).toFloat()

        for (i in 0 until count) {
            currChild = getChildAt(i)
            currChild.y = containerHeight.toFloat()
            currChild.getLocationOnScreen(pos)

            pos[0] -= screenWidth/2 //startOffset

            if (currChild.width != 0 && radius > 0) {
                    val xPow2 = (pos[0] + currChild.width / 2.toDouble()).pow(2.0).toFloat()
                    y = abs(sqrt(abs(radiusPow2 - xPow2 - screenWidthPow2/2).toDouble()) - radius).toFloat()
                    currChild.y = y// * 0.5f + elevation + itemsOffset
            }
        }
    }

    fun setRadius(radius: Float) {
        this.radius = radius
    }

    fun setContainerHeight(containerHeight: Int) {
        this.containerHeight = containerHeight
    }

    fun setContainerWidth(containerWidth: Int) {
        this.containerWidth = containerWidth
    }
}