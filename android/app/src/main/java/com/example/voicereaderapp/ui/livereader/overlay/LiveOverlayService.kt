package com.example.voicereaderapp.ui.livereader.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import android.view.Gravity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import com.example.voicereaderapp.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.voicereaderapp.ui.theme.VoiceReaderAppTheme
import com.example.voicereaderapp.utils.LocaleHelper

@AndroidEntryPoint
class LiveOverlayService : LifecycleService() {
    private val TAG = "LiveOverlayServiceDebug"
    private lateinit var windowManager: WindowManager

    @Inject
    lateinit var viewModel: LiveOverlayViewModel

    @Inject
    lateinit var getVoiceSettingsUseCase: com.example.voicereaderapp.domain.usecase.GetVoiceSettingsUseCase

    // Cửa sổ cho EdgeBar hoặc CircleButton
    private var controlView: ComposeView? = null
    private lateinit var controlLayoutParams: WindowManager.LayoutParams

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
        // viewModel is injected by Hilt via @Inject
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


        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        // Bắt buộc phải tạo Notification Channel cho Android 8 (API 26) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Live Reader Service Channel",
                NotificationManager.IMPORTANCE_LOW // Dùng IMPORTANCE_LOW để không có âm thanh
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }

        // Tạo PendingIntent để khi người dùng nhấn vào thông báo sẽ mở lại app
        // val notificationIntent = Intent(this, YourMainActivity::class.java)
        // val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VoiceReader is Active")
            .setContentText("Live Scan is running in the background.")
            .setSmallIcon(R.drawable.logo_2) // <-- THAY BẰNG ICON CỦA BẠN
            // .setContentIntent(pendingIntent)
            .build()
    }

    private fun initializeLayoutParams() {
        // Cấu hình cho Control (EdgeBar or CircleButton)
        // Use WRAP_CONTENT to only block touches on the actual control, not the whole screen
        controlLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,

            // Không focus, không modal, cho phép chạm xuyên qua vùng trong suốt
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Position at right center by default (for EdgeBar)
            gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
            y = 200 // Offset from center
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
        // Apply locale to service context for proper localization
        val localizedContext = LocaleHelper.applyLocale(this)
        val composeView = ComposeView(localizedContext)
        val lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.onCreate()
        lifecycleOwner.onResume()

        composeView.apply {
            // Make sure background is transparent to allow touch pass-through
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                // Apply theme based on settings
                val themeMode by viewModel.themeMode.collectAsState()
                val isSystemDark = isSystemInDarkTheme()
                val isDarkTheme = when (themeMode) {
                    com.example.voicereaderapp.domain.model.ThemeMode.LIGHT -> false
                    com.example.voicereaderapp.domain.model.ThemeMode.DARK -> true
                    com.example.voicereaderapp.domain.model.ThemeMode.SYSTEM -> isSystemDark
                }

                VoiceReaderAppTheme(darkTheme = isDarkTheme) {
                    content()
                }
            }
        }
        return composeView
    }

    private fun showEdgeBar() {
        // Force cleanup if view already exists (defensive)
        controlView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing existing control view", e)
            }
            controlView = null
        }

        // Read settings to determine which control to show
        val settings = runBlocking {
            getVoiceSettingsUseCase().first()
        }

        Log.d(TAG, "🔧 Creating control view with style: ${settings.liveScanBarStyle}")

        // Get screen dimensions for boundary checking
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Configure layout params based on control type
        when (settings.liveScanBarStyle) {
            com.example.voicereaderapp.domain.model.LiveScanBarStyle.EDGE_BAR -> {
                Log.d(TAG, "📍 Setting up EdgeBar at right center")
                // EdgeBar: positioned at right center
                controlLayoutParams.gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
                controlLayoutParams.y = 200
                controlLayoutParams.x = 0

                controlView = createComposeView {
                    ControlEdgeBarSimple(
                        viewModel = viewModel,
                        onPositionChange = { deltaX, deltaY ->
                            // Update vertical position when edge bar is dragged
                            controlLayoutParams.y += deltaY.toInt()

                            // Keep within screen bounds (with some padding)
                            val edgeBarHeight = 90 * resources.displayMetrics.density // 90.dp in pixels
                            val minY = -(screenHeight / 2 - edgeBarHeight.toInt() / 2)
                            val maxY = (screenHeight / 2 - edgeBarHeight.toInt() / 2)
                            controlLayoutParams.y = controlLayoutParams.y.coerceIn(minY, maxY)

                            controlView?.let { view ->
                                windowManager.updateViewLayout(view, controlLayoutParams)
                                Log.d(TAG, "🔄 Edge bar moved to y: ${controlLayoutParams.y}")
                            }
                        }
                    )
                }
            }
            com.example.voicereaderapp.domain.model.LiveScanBarStyle.CIRCLE_BUTTON -> {
                Log.d(TAG, "📍 Setting up CircleButton at absolute position")
                // CircleButton: absolute positioning from top-left
                // Start at right edge, vertically centered
                val buttonSize = (60 * resources.displayMetrics.density).toInt() // 60.dp in pixels
                controlLayoutParams.gravity = android.view.Gravity.TOP or android.view.Gravity.START
                controlLayoutParams.x = screenWidth - buttonSize - (16 * resources.displayMetrics.density).toInt()
                controlLayoutParams.y = screenHeight / 2 - buttonSize / 2

                controlView = createComposeView {
                    CircleControlButtonSimple(
                        viewModel = viewModel,
                        onPositionChange = { deltaX, deltaY ->
                            // Update window position when circle button is dragged
                            controlLayoutParams.x += deltaX.toInt()
                            controlLayoutParams.y += deltaY.toInt()

                            // Keep within screen bounds during drag
                            controlLayoutParams.x = controlLayoutParams.x.coerceIn(0, screenWidth - buttonSize)
                            controlLayoutParams.y = controlLayoutParams.y.coerceIn(0, screenHeight - buttonSize)

                            controlView?.let { view ->
                                windowManager.updateViewLayout(view, controlLayoutParams)
                                Log.d(TAG, "🔄 Circle button moved to (${controlLayoutParams.x}, ${controlLayoutParams.y})")
                            }
                        },
                        onDragEnd = {
                            // Snap to nearest edge (left or right) when drag ends
                            val centerX = controlLayoutParams.x + buttonSize / 2
                            val snapToLeft = centerX < screenWidth / 2

                            val padding = (16 * resources.displayMetrics.density).toInt()
                            controlLayoutParams.x = if (snapToLeft) {
                                padding // Snap to left edge with padding
                            } else {
                                screenWidth - buttonSize - padding // Snap to right edge with padding
                            }

                            // Keep Y within bounds with padding
                            val topPadding = (50 * resources.displayMetrics.density).toInt()
                            val bottomPadding = (50 * resources.displayMetrics.density).toInt()
                            controlLayoutParams.y = controlLayoutParams.y.coerceIn(
                                topPadding,
                                screenHeight - buttonSize - bottomPadding
                            )

                            controlView?.let { view ->
                                windowManager.updateViewLayout(view, controlLayoutParams)
                                Log.d(TAG, "✨ Circle button snapped to ${if (snapToLeft) "left" else "right"} edge at (${controlLayoutParams.x}, ${controlLayoutParams.y})")
                            }
                        }
                    )
                }
            }
        }

        windowManager.addView(controlView, controlLayoutParams)
        Log.d(TAG, "✅ Control view added to window manager")
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

        // Cleanup ViewModel resources (TTS, coroutines)
        viewModel.cleanup()

        // Remove overlay views
        controlView?.let { windowManager.removeView(it) }
        expandedOverlayView?.let { windowManager.removeView(it) }
        micView?.let { windowManager.removeView(it) }
        controlView = null
        expandedOverlayView = null
        micView = null

        stopForeground(true)
    }

    companion object {
        private const val ACTION_START = "com.example.voicereaderapp.ACTION_START"
        private const val ACTION_STOP = "com.example.voicereaderapp.ACTION_STOP"
        private const val EXTRA_TEXT_TO_READ = "EXTRA_TEXT_TO_READ"

        private const val NOTIFICATION_ID = 3636 // ID duy nhất cho thông báo
        private const val CHANNEL_ID = "LiveReaderChannel"

        fun start(context: Context, textToRead: String) {
            val intent = Intent(context, LiveOverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TEXT_TO_READ, textToRead)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LiveOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
