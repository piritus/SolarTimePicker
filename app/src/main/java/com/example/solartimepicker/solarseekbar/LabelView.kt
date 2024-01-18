package com.example.solartimepicker.solarseekbar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.solartimepicker.R

@SuppressLint("ViewConstructor")
class LabelView(private val seekBar: BiDirectionalSeekBar, context: Context?) :
    RelativeLayout(context) {
    private val textView: TextView
    private val drawable: Drawable?

    init {
        textView = TextView(context)
        drawable = ContextCompat.getDrawable(getContext(), R.drawable.bg_seekbar_label)
        init()
    }

    private fun init() {
        initThis()
        initTV()
    }

    private fun initThis() {
        setLabelBGColor()
        val params =
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.addRule(CENTER_HORIZONTAL, TRUE)
        layoutParams = params
        gravity = Gravity.CENTER
    }

    private fun initTV() {
        textView.setTextColor(seekBar.mLabelColor)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        val params =
            LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.addRule(CENTER_IN_PARENT, TRUE)
        textView.layoutParams = params
        textView.gravity = Gravity.CENTER
        textView.setPadding(15, 10, 15, 10)
        addView(textView)
    }

    fun setText(string: String?) {
        textView.text = string
    }

    fun setLabelBGColor() {
        if (drawable != null) {
            DrawableCompat.setTint(drawable, seekBar.mIndicatorColor)
        }
        background = drawable
    }

    fun setLabelColor(color: Int) {
        textView.setTextColor(color)
        invalidate()
    }
}