package com.example.solartimepicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView

class CustomViewToDrawable : ImageView {

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    private fun init() {
        // Установите фон, цвет и другие параметры вашего представления
        setBackgroundColor(resources.getColor(android.R.color.holo_blue_bright))
    }

    fun convertToDrawable(): Drawable {
        // Получаем размеры View
        val width = width
        val height = height

        // Создаем Bitmap с размерами View
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Создаем Canvas для отрисовки на Bitmap
        val canvas = Canvas(bitmap)

        // Отрисовываем содержимое View на Canvas
        draw(canvas)

        // Создаем Drawable из Bitmap
        return BitmapDrawable(resources, bitmap)
    }
}