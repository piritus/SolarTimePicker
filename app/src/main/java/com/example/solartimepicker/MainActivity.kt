package com.example.solartimepicker

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.solartimepicker.model.ShadowMap
import com.example.solartimepicker.solarseekbar.BiDirectionalSeekBar
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "prefs"
        private const val KEY_SUNRISE = "key_sunrise"
        private const val KEY_SUNSET = "key_sunset"
        private const val KEY_DIRECTION = "key_direction"

    }

    private val dateFormat by lazy { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    private val startTime = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 7)
        set(Calendar.MINUTE, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        val circleView = findViewById<SunCircleView>(R.id.circleView)
        val sampleText = findViewById<TextView>(R.id.sampleText)
        val seekBar = findViewById<BiDirectionalSeekBar>(R.id.seekBar)
        val wheelView = findViewById<HorizontalWheelView>(R.id.wheelView)
        val tickText1 = findViewById<TextView>(R.id.tickText1)
        val tickText2 = findViewById<TextView>(R.id.tickText2)

        val tvSunrise = findViewById<EditText>(R.id.tvSunrise)
        val tvSunset = findViewById<EditText>(R.id.tvSunset)
        val spinner = findViewById<Spinner>(R.id.spinner)
        val btnCommit = findViewById<Button>(R.id.btnCommit)

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

        var lastTick = -1
        wheelView.onRotateListener = HorizontalWheelView.OnRotateListener { percent, tick, offset ->
            Log.d("TAG", "onRotate: percent=$percent, tick=$tick, offset=$offset")
            if(tick != lastTick) {
                lastTick = tick
                tickText1.text = tick.time()
                tickText2.text = (tick + 1).time()
            }
            tickText1.translationY = -tickText2.height * offset
            tickText2.translationY = tickText2.height * (1 - offset)

            circleView.setSunAngle(percent)
        }

        // Sun circular
        val sunriseAnglePref =
            getFromPreferences(KEY_SUNRISE, "").takeIf { it.isNotEmpty() }?.toIntOrNull()
                ?.coerceIn(0, 360)
        val sunsetAnglePref =
            getFromPreferences(KEY_SUNSET, "").takeIf { it.isNotEmpty() }?.toIntOrNull()
                ?.coerceIn(0, 360)
        val directionPosPref =
            getFromPreferences(KEY_DIRECTION, "").takeIf { it.isNotEmpty() }?.toIntOrNull()
                ?.coerceIn(0, 1)
        val directionPref = directionPosPref?.let {
            ShadowMap.Direction.entries.getOrNull(it)
        }

        sunriseAnglePref?.let { tvSunrise.setText(it.toString()) }
        sunsetAnglePref?.let { tvSunset.setText(it.toString()) }
        directionPref?.let { spinner.setSelection(directionPosPref) }

        val shadowMap = ShadowMap(
            sunriseAngle = sunriseAnglePref ?: 220,
            sunsetAngle = sunsetAnglePref ?: 40,
            direction = directionPref ?: ShadowMap.Direction.COUNTERCLOCKWISE
        )
        circleView.onRotateListener = SunCircleView.OnRotateListener {factor->
            wheelView.setRotate(factor)
        }
        circleView.initialize(shadowMap)
        circleView.invalidate()

        // Settings
        btnCommit.setOnClickListener {
            val sunriseAngle = tvSunrise.text.toString().toIntOrNull()?.coerceIn(0, 360)
            val sunsetAngle = tvSunset.text.toString().toIntOrNull()?.coerceIn(0, 360)
            val direction = when (spinner.selectedItemPosition) {
                0 -> {
                    ShadowMap.Direction.CLOCKWISE
                }

                1 -> {
                    ShadowMap.Direction.COUNTERCLOCKWISE
                }

                else -> null
            }

            if (sunriseAngle == null || sunsetAngle == null || direction == null) {
                return@setOnClickListener
            }

            saveToPreferences(KEY_SUNRISE, sunriseAngle.toString())
            saveToPreferences(KEY_SUNSET, sunsetAngle.toString())
            saveToPreferences(KEY_DIRECTION, spinner.selectedItemPosition.toString())

            val updShadowMap = ShadowMap(sunriseAngle, sunsetAngle, direction)
            circleView.initialize(updShadowMap)
//            circleView.invalidate()
            circleView.requestLayout()
        }
    }

    private fun saveToPreferences(key: String, value: String) {
        val sharedPref: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPref.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun getFromPreferences(key: String, defaultValue: String): String {
        val sharedPref: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(key, defaultValue) ?: defaultValue
    }

    private fun Int.time(): String {
        val time = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        time.add(Calendar.MINUTE, this * 10)
        return dateFormat.format(time.time)
    }
}

inline val Number.dp: Int
    get() = (this.toFloat() * Resources.getSystem().displayMetrics.density).toInt()
