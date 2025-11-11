package com.xmobile.project1groupstudyappnew.utils

object RandomColor {
    fun getRandomColor(): Int {
        val colors = listOf(
            0xFFE57373.toInt(), // Red
            0xFFF06292.toInt(), // Pink
            0xFFBA68C8.toInt(), // Purple
            0xFF64B5F6.toInt(), // Blue
            0xFF4DB6AC.toInt(), // Teal
            0xFF81C784.toInt(), // Green
            0xFFFFD54F.toInt(), // Yellow
            0xFFFFB74D.toInt(), // Orange
            0xFFA1887F.toInt(), // Brown
            0xFF90A4AE.toInt()  // Grey
        )
        return colors.random()
    }
}