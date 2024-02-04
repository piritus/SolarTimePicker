package com.example.solartimepicker

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.ComponentActivity
import com.example.solartimepicker.model.ShadowMap
import com.example.solartimepicker.view.HorizontalWheelView
import com.example.solartimepicker.view.SunCircleView
import com.example.solartimepicker.view.TimeTickLabelView


class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "prefs"
        private const val KEY_SUNRISE = "key_sunrise"
        private const val KEY_SUNSET = "key_sunset"
        private const val KEY_DIRECTION = "key_direction"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_activity)

        val circleView = findViewById<SunCircleView>(R.id.circleView)
        val wheelView = findViewById<HorizontalWheelView>(R.id.wheelView)
        val labelView = findViewById<TimeTickLabelView>(R.id.labelView)

        val tvSunrise = findViewById<EditText>(R.id.tvSunrise)
        val tvSunset = findViewById<EditText>(R.id.tvSunset)
        val spinner = findViewById<Spinner>(R.id.spinner)
        val btnCommit = findViewById<Button>(R.id.btnCommit)

        wheelView.onRotateListener = HorizontalWheelView.OnRotateListener { percent, tick, offset ->
            Log.d("TAG", "onRotate: percent=$percent, tick=$tick, offset=$offset")

            labelView.setTickLabel(tick, offset)
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
        circleView.onRotateListener = SunCircleView.OnRotateListener { factor ->
            wheelView.setRotate(
                (factor * wheelView.tickCount).toInt() / wheelView.tickCount.toFloat()
            )
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
}

inline val Number.dp: Int
    get() = (this.toFloat() * Resources.getSystem().displayMetrics.density).toInt()
