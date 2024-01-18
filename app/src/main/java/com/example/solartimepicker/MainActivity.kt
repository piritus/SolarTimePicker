package com.example.solartimepicker

import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.solartimepicker.solarseekbar.BiDirectionalSeekBar


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        val sampleText = findViewById<TextView>(R.id.sampleText)
        val seekBar = findViewById<BiDirectionalSeekBar>(R.id.seekBar)

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
            }

            override fun onStopTrackingTouch(seekBar: BiDirectionalSeekBar?) {
                Log.d("TAG", "stop touch: ")
            }
        })
    }
}

inline val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
