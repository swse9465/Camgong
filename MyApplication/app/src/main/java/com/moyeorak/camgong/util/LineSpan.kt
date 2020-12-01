package com.moyeorak.camgong.util

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.LineBackgroundSpan

class LineSpan(clr: Int = 0) : LineBackgroundSpan{

    private val color = clr

    override fun drawBackground(
        canvas: Canvas, paint: Paint, left: Int, right: Int, top: Int, bottom: Int,
        baseline: Int, text: CharSequence, start: Int, end: Int, lineNumber: Int
    ) {
        val oldColor = paint.color
        if(color != 0)
            paint.color = color
        canvas.drawRect(left.toFloat(), bottom.toFloat()+20, right.toFloat(), bottom.toFloat()+35, paint)

        paint.color = oldColor
    }

}