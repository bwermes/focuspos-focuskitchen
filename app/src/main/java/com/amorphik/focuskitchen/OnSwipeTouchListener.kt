package com.amorphik.focuskitchen

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

class OnSwipeTouchListener(context: Context, val onGesture: (gestureCode: Int) -> Unit) : View.OnTouchListener {
    private val gestureDetector = GestureDetector(context, GestureListener())
    private val SWIPE_RIGHT = 1
    private val SWIPE_LEFT = 2

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener: GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_DISTANCE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 200

        override fun onDown(e: MotionEvent?): Boolean {
            return false
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent?,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val distanceX = e2!!.x - e1!!.x
            val distanceY = e2.y - e1.y

//            Logger.d("recallSwipe","x = $distanceX | y = $distanceY")
//            Logger.d("recallSwipe","velocity X = $velocityX | ")
//
//            Logger.d("recallSwipe","abs x = ${abs(distanceX)} | y = ${abs(distanceY)}")
//            Logger.d("recallSwipe","velocity X = ${abs(velocityX)}")


            if (abs(distanceX) > abs(distanceY) &&
                abs(distanceX) > SWIPE_DISTANCE_THRESHOLD &&
                abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
//                    Logger.d("recallSwipe","gesture passes check")

                val gesture = when (distanceX > 0) {
                    true -> SWIPE_RIGHT
                    false -> SWIPE_LEFT
                }

//                Logger.d("recallSwipe","Gesture = $gesture")
                onGesture.invoke(gesture)
                return false
            }

            return false
        }
    }
}