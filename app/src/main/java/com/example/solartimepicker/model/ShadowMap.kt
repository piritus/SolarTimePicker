package com.example.solartimepicker.model

data class ShadowMap(
    val sunriseAngle: Int,
    val sunsetAngle: Int,
    val direction: Direction,
) {
    enum class Direction {
        CLOCKWISE,
        COUNTERCLOCKWISE;
    }
}