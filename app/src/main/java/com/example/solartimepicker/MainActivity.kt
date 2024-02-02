package com.example.solartimepicker

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.solartimepicker.solarseekbar.BiDirectionalSeekBar
import kotlin.math.absoluteValue


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        val sampleText = findViewById<TextView>(R.id.sampleText)
        val seekBar = findViewById<BiDirectionalSeekBar>(R.id.seekBar)
        val wheelView = findViewById<HorizontalWheelView>(R.id.wheelView)
        val tickText1 = findViewById<TextView>(R.id.tickText1)
        val tickText2 = findViewById<TextView>(R.id.tickText2)

        sampleText.post {
            sampleText.text = seekBar.progress.toString()
        }

        seekBar.setOnSeekBarChangeListener(object : BiDirectionalSeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: BiDirectionalSeekBar?) {
                Log.d("TAG", "start touch: ")
            }

            override fun onProgressChanged(
                seekBar: BiDirectionalSeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                sampleText.text = progress.toString()
                Log.d("TAG", "onCreate: $progress $fromUser")
                wheelView.setRotate(progress / 72f)

            }

            override fun onStopTrackingTouch(seekBar: BiDirectionalSeekBar?) {
                Log.d("TAG", "stop touch: ")
            }
        })

        val tickViews = listOf(tickText1, tickText2)
        wheelView.onRotateListener = HorizontalWheelView.OnRotateListener { percent, tick, offset ->
            Log.d("TAG", "onRotate: percent=$percent, tick=$tick, offset=$offset")
            val viewHeight = tickText1.height
            val translation = viewHeight * -offset
            val closestTickView = tickViews.minBy { (it.translationY - translation).absoluteValue }
            closestTickView.updateTickView(tick, translation)
            val otherTickView = tickViews.first { it != closestTickView }
            otherTickView.updateTickView(tick + 1, viewHeight * (1 - offset))
        }
    }

    private fun TextView.updateTickView(tick: Int, translation: Float) {
        text = tick.toString()
        translationY = translation
    }
}

inline val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
