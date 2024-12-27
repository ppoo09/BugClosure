package com.keyolla.bugclosure

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window

class TouchEventLogger(private val originalCallback: Window.Callback, private val activity: Activity) : Window.Callback by originalCallback {

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val rootView = activity.window.decorView.rootView // 앱 전체 뷰의 최상위 View
            val touchedView = findViewAt(rootView, event.rawX.toInt(), event.rawY.toInt())
            touchedView?.let {
                val viewId = try {
                    it.resources.getResourceEntryName(it.id)
                } catch (e: Exception) {
                    "Unknown ID"
                }
                BugClosure.i("TouchEvent == Touched View ID: $viewId")
            }
        }
        return originalCallback.dispatchTouchEvent(event)
    }

    private fun findViewAt(rootView: View, x: Int, y: Int): View? {
        val location = IntArray(2)
        rootView.getLocationOnScreen(location)

        val left = location[0]
        val top = location[1]
        val right = left + rootView.width
        val bottom = top + rootView.height

        // 터치 좌표가 현재 View 안에 있는지 확인
        if (x in left..right && y in top..bottom) {
            // ViewGroup이면 하위 뷰를 재귀적으로 탐색
            if (rootView is ViewGroup) {
                for (i in 0 until rootView.childCount) {
                    val child = rootView.getChildAt(i)
                    val target = findViewAt(child, x, y) // 재귀 호출
                    if (target != null) {
                        return target
                    }
                }
            }
            // ViewGroup이 아니면 현재 View를 반환
            return rootView
        }
        return null
    }
}