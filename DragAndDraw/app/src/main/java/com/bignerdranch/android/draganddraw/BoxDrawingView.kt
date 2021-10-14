package com.bignerdranch.android.draganddraw

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

private const val TAG = "BoxDrawingView"

class BoxDrawingView(context: Context, attrs: AttributeSet? = null) :
        View(context, attrs) {

    private var currentBox: Box? = null
    private val boxen = mutableListOf<Box>()
    private val boxPaint = Paint().apply {
        color = 0x22ff0000.toInt()
    }
    private val backgroundPaint = Paint().apply {
        color = 0xfff8efe0.toInt()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val state = Bundle()
        state.putParcelable("pp", super.onSaveInstanceState())

        state.putInt("size", boxen.size)
        for (i in 0 until boxen.size) {
            state.putParcelable("boxen_s_$i", boxen[i].start)
            state.putParcelable("boxen_e_$i", boxen[i].end)
        }
        return state;
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable("pp"))

            boxen.clear()
            val size: Int = state.getInt("size", 0)
            for (i in 0 until size) {
                val start = state.getParcelable<PointF>("boxen_s_$i")
                val end = state.getParcelable<PointF>("boxen_e_$i")
                if (start is PointF && end is PointF) {
                    val box = Box(start)
                    box.end = end
                    boxen.add(box)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val current = PointF(event.x, event.y)
        var action = ""
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                action = "ACTION_DOWN"
// Сбросить состояние объекта
                currentBox = Box(current).also {
                    boxen.add(it)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                action = "ACTION_MOVE"
                updateCurrentBox(current)
            }
            MotionEvent.ACTION_UP -> {
                action = "ACTION_UP"
                updateCurrentBox(current)
                currentBox = null
            }
            MotionEvent.ACTION_CANCEL -> {
                action = "ACTION_CANCEL"
                currentBox = null
            }
        }
        Log.i(TAG, "$action at x=${current.x}, y=${current.y}")
        return true
    }

    private fun updateCurrentBox(current: PointF) {
        currentBox?.let {
            it.end = current
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
// Заполнение фона
        canvas.drawPaint(backgroundPaint)
        boxen.forEach { box ->
            canvas.drawRect(box.left, box.top, box.right, box.bottom, boxPaint)
        }
    }
}