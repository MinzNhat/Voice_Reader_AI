package com.example.voicereaderapp.ui.livereader.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.voicereaderapp.ui.livereader.overlay.window.ServiceLifecycleOwner
import android.util.Log
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.view.Gravity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

class LiveOverlayService : LifecycleService() {
    private val TAG = "LiveOverlayServiceDebug"
    private lateinit var windowManager: WindowManager
    private lateinit var viewModel: LiveOverlayViewModel

    // Cửa sổ cho EdgeBar
    private var edgeBarView: ComposeView? = null
    private lateinit var edgeBarLayoutParams: WindowManager.LayoutParams

    // Cửa sổ cho Panel Mở rộng
    private var expandedOverlayView: ComposeView? = null
    private lateinit var expandedOverlayLayoutParams: WindowManager.LayoutParams

    // Cửa sổ riêng cho Mic
    private var micView: ComposeView? = null
    private lateinit var micLayoutParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "====== Service onCreate() được gọi! ======")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        viewModel = LiveOverlayViewModel()
        initializeLayoutParams()

        // Lắng nghe trạng thái FOCUS (để hiện bàn phím)
        lifecycleScope.launch {
            viewModel.isNoteOverlayVisible.collectLatest { isNoteVisible ->
                updateExpandedOverlayFocusable(isNoteVisible)
            }
        }

        // Lắng nghe để hiển thị/ẩn Panel Mở rộng
        lifecycleScope.launch {
            viewModel.isExpanded.collectLatest { isExpanded ->
                if (isExpanded) {
                    showExpandedOverlay()
                } else {
                    hideExpandedOverlay()
                }
            }
        }

        // Lắng nghe để hiển thị/ẩn Mic
        lifecycleScope.launch {
            viewModel.isListening.collectLatest { isListening ->
                if (isListening) {
                    showMicView()
                } else {
                    hideMicView()
                }
            }
        }
    }

    private fun initializeLayoutParams() {
        // Cấu hình cho EdgeBar
        edgeBarLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,

            // Không focus, không modal (cho phép chạm xuyên qua vùng trong suốt)
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Đặt ở góc trên bên phải, offset Y
            gravity = Gravity.TOP or Gravity.END
            y = 600
        }

        // Cấu hình cho lớp phủ mở rộng (toàn màn hình, trong lớp này có Panel)
        expandedOverlayLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,

            // Không focus, nhưng LÀ MODAL (chặn chạm xuyên qua, để nhận gesture tap ra ngoài đóng panel)
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Cấu hình cho Mic
        micLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand() được gọi với action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "=> Nhận được ACTION_START.")
                val textToRead = intent.getStringExtra(EXTRA_TEXT_TO_READ) ?: ""
                Log.d(TAG, "Văn bản nhận được: '$textToRead'")
                viewModel.setReadingText(textToRead)
                showEdgeBar()
            }
            ACTION_STOP -> {
                Log.d(TAG, "=> Nhận được ACTION_STOP. Dừng service.")
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createComposeView(content: @Composable () -> Unit): ComposeView {
        val composeView = ComposeView(this)
        val lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.onCreate()
        lifecycleOwner.onResume()

        composeView.apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent(content)
        }
        return composeView
    }

    private fun showEdgeBar() {
        if (edgeBarView == null) {
            edgeBarView = createComposeView {
                ControlEdgeBar(viewModel = viewModel)
            }
            windowManager.addView(edgeBarView, edgeBarLayoutParams)
        }
    }

    private fun showMicView() {
        if (micView == null) {
            micView = createComposeView {
                Box(modifier = Modifier.fillMaxSize()) {
                    VoiceInteractionPad(viewModel = viewModel)
                }
            }
            windowManager.addView(micView, micLayoutParams)
        }
    }

    private fun hideMicView() {
        micView?.let {
            windowManager.removeView(it)
            micView = null
        }
    }

    private fun showExpandedOverlay() {
        if (expandedOverlayView == null) {
            expandedOverlayView = createComposeView {
                // UI mới cho lớp phủ toàn màn hình
                ExpandedOverlayUI(viewModel = viewModel)
            }
            windowManager.addView(expandedOverlayView, expandedOverlayLayoutParams)
        }
    }

    private fun hideExpandedOverlay() {
        expandedOverlayView?.let {
            windowManager.removeView(it)
            expandedOverlayView = null
        }
    }

    private fun updateExpandedOverlayFocusable(isFocusable: Boolean) {
        if (expandedOverlayView == null) return

        if (isFocusable) {
            Log.d(TAG, "Cập nhật cờ cho expandedOverlay: CHO PHÉP FOCUS")
            // Gỡ bỏ cờ FLAG_NOT_FOCUSABLE
            expandedOverlayLayoutParams.flags = expandedOverlayLayoutParams.flags and
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            Log.d(TAG, "Cập nhật cờ cho expandedOverlay: KHÔNG CHO PHÉP FOCUS")
            // Thêm lại cờ FLAG_NOT_FOCUSABLE
            expandedOverlayLayoutParams.flags = expandedOverlayLayoutParams.flags or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        // Áp dụng thay đổi vào cửa sổ đang hiển thị
        windowManager.updateViewLayout(expandedOverlayView, expandedOverlayLayoutParams)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "====== Service onDestroy() được gọi! ======")
        edgeBarView?.let { windowManager.removeView(it) }
        expandedOverlayView?.let { windowManager.removeView(it) }
        micView?.let { windowManager.removeView(it) } // ✅ Dọn dẹp mic view
        edgeBarView = null
        expandedOverlayView = null
        micView = null // ✅ Dọn dẹp mic view
    }

    companion object {
        private const val ACTION_START = "com.example.voicereaderapp.ACTION_START"
        private const val ACTION_STOP = "com.example.voicereaderapp.ACTION_STOP"
        private const val EXTRA_TEXT_TO_READ = "EXTRA_TEXT_TO_READ"

        fun start(context: Context, textToRead: String) {
            val intent = Intent(context, LiveOverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TEXT_TO_READ, textToRead)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, LiveOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
