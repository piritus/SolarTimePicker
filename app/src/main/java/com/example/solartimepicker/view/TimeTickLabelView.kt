package com.example.solartimepicker.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import com.example.solartimepicker.R
import com.example.solartimepicker.databinding.TimeTickLabelViewBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.properties.Delegates

@SuppressLint("ViewConstructor")
internal class TimeTickLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val bind = TimeTickLabelViewBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    private val dateFormat by lazy { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    private val tickViews: List<TextView>

    var minPerTick: Int = 0
    private var startTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 7)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    var startTimeString: String by Delegates.observable("00:00") { _, _, newValue ->
        runCatching {
            dateFormat.parse(newValue)?.also {
                startTime = Calendar.getInstance().apply {
                    time = it
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
        }
    }

    init {
        tickViews = listOf(bind.tickText1, bind.tickText2)

        context.withStyledAttributes(attrs, R.styleable.TimeTickLabelView, defStyleAttr) {
            minPerTick = getInt(R.styleable.TimeTickLabelView_TTL_minPerTick, 10)
            getString(R.styleable.TimeTickLabelView_TTL_startTime)?.also {
                startTimeString = it
            }

            getResourceId(R.styleable.TimeTickLabelView_TTL_textAppearance, 0)
                .takeIf { it > 0 }
                ?.let {
                    bind.tickText1.setTextAppearance(it)
                    bind.tickText2.setTextAppearance(it)
                }
            getDimension(R.styleable.TimeTickLabelView_TTL_textSize, 0f)
                .takeIf { it > 0 }
                ?.let {
                    bind.tickText1.setTextSize(TypedValue.COMPLEX_UNIT_PX, it)
                    bind.tickText2.setTextSize(TypedValue.COMPLEX_UNIT_PX, it)
                }
            getColor(R.styleable.TimeTickLabelView_TTL_textColor, Color.BLACK).let {
                    bind.tickText1.setTextColor(it)
                    bind.tickText2.setTextColor(it)
                }
        }
    }

    fun setTickLabel(tick: Int, offset: Float) {
        val viewHeight = bind.tickText1.height
        val translation = viewHeight * -offset
        val closestTickView = tickViews.minBy { (it.translationY - translation).absoluteValue }
        val otherTickView = tickViews.first { it != closestTickView }
        closestTickView.updateTickView(tick, viewHeight * -offset)
        otherTickView.updateTickView(tick + 1, viewHeight * (1 - offset))
    }

    private fun Int.time(): String {
        val time = (startTime.clone() as? Calendar)?.apply {
            add(Calendar.MINUTE, this@time * minPerTick)
        }?.time ?: return ""

        return dateFormat.format(time.time)
    }

    private fun TextView.updateTickView(tick: Int, translation: Float) {
        text = tick.time()
        translationY = translation
    }
}