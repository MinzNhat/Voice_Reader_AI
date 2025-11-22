package com.example.voicereaderapp.domain.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.SuppressLint
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.voicereaderapp.ui.livereader.overlay.LiveOverlayViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("AccessibilityPolicy")
class ScreenReaderAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ScreenReaderAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("AccessService", "Service Connected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Logic: Nếu phát hiện người dùng chạm hoặc scroll, báo cho ViewModel để dừng đọc
        if (event == null) return

        // Lọc bỏ các sự kiện do chính App mình gây ra (để tránh vòng lặp tự scroll -> tự dừng)
        if (event.packageName == packageName) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            // Bắn event về ViewModel: "User đang tương tác, dừng Auto-read đi!"
            // LiveOverlayViewModel.instance?.onUserInteracted()
        }
    }

    override fun onInterrupt() {}

    /**
     * Thực hiện thao tác vuốt màn hình
     * @param startY: Điểm bắt đầu vuốt (thường là gần cuối màn hình)
     * @param endY: Điểm kết thúc (thường là đầu màn hình để scroll xuống)
     */
    fun performScroll(x: Float, startY: Float, endY: Float) {
        if (x < 0 || startY < 0 || endY < 0) {
            Log.e("AccessService", "❌ Tọa độ không hợp lệ: x=$x, y=$startY->$endY")
            return
        }

        val path = Path()
        path.moveTo(x, startY)
        path.lineTo(x, endY)

        val builder = GestureDescription.Builder()

        val stroke = GestureDescription.StrokeDescription(path, 0, 1000)

        builder.addStroke(stroke)

        val gesture = builder.build()

        val result = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d("AccessService", "✅ Vuốt thành công!")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.e("AccessService", "❌ Vuốt bị hủy (Cancelled)! Có thể do vướng view khác.")
            }
        }, null)

        Log.d("AccessService", "Gửi lệnh vuốt: Kết quả=$result")
    }

    /**
     * Chụp màn hình (Yêu cầu Android 11 - API 30 trở lên)
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun captureScreen(onBitmapReady: (Bitmap) -> Unit) {
        takeScreenshot(0, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                // Cần copy bitmap vì HardwareBuffer không thể truy cập trực tiếp ở một số nơi
                onBitmapReady(bitmap?.copy(Bitmap.Config.ARGB_8888, true) ?: return)
                screenshot.hardwareBuffer.close()
            }

            override fun onFailure(errorCode: Int) {
                Log.e("AccessService", "Screenshot failed code: $errorCode")
            }
        })
    }
}
