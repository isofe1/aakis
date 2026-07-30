package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import kotlinx.coroutines.delay
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.HistoryRepository
import com.example.data.UserPreferences
import com.example.engine.ArabicReshaperEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class FloatingBubbleService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)
    private lateinit var repository: HistoryRepository

    private lateinit var windowManager: WindowManager
    private var bubbleComposeView: ComposeView? = null
    private var popupComposeView: ComposeView? = null
    private var trashComposeView: ComposeView? = null
    private lateinit var userPrefs: UserPreferences

    private val isOverTrashState = MutableStateFlow(false)
    private var isPopupOpen = false

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        userPrefs = UserPreferences(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val db = AppDatabase.getDatabase(this)
        repository = HistoryRepository(db.historyDao())

        bubbleSizeState.value = userPrefs.bubbleSizeDp
        bubbleOpacityState.value = userPrefs.bubbleOpacity

        startForegroundServiceNotification()

        if (Settings.canDrawOverlays(this)) {
            showFloatingBubble()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "arabic_reshaper_bubble_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Floating Bubble Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("عاكس - الفقاعة العائمة")
            .setContentText("انقر للفتح أو تخصيص الإعدادات")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showFloatingBubble() {
        if (bubbleComposeView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = userPrefs.lastBubbleX
            y = userPrefs.lastBubbleY
        }

        bubbleComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(this@FloatingBubbleService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBubbleService)

            setContent {
                val sizeDpVal by bubbleSizeState.collectAsState()
                val opacityVal by bubbleOpacityState.collectAsState()

                val sizeDp = sizeDpVal.dp
                val opacity = opacityVal

                LaunchedEffect(sizeDpVal) {
                    if (bubbleComposeView != null && bubbleComposeView?.isAttachedToWindow == true) {
                        try {
                            windowManager.updateViewLayout(bubbleComposeView, params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(sizeDp)
                        .alpha(opacity)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF4F46E5))
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    showTrashZone()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    params.x += dragAmount.x.toInt()
                                    params.y += dragAmount.y.toInt()
                                    if (bubbleComposeView != null && bubbleComposeView?.isAttachedToWindow == true) {
                                        try {
                                            windowManager.updateViewLayout(bubbleComposeView, params)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }

                                    val screenHeight = resources.displayMetrics.heightPixels
                                    val isNearBottom = params.y > screenHeight * 0.72f
                                    isOverTrashState.value = isNearBottom
                                },
                                onDragEnd = {
                                    if (isOverTrashState.value) {
                                        hideTrashZone()
                                        userPrefs.isFloatingServiceEnabled = false
                                        stopSelf()
                                    } else {
                                        hideTrashZone()
                                        userPrefs.lastBubbleX = params.x
                                        userPrefs.lastBubbleY = params.y
                                    }
                                }
                            )
                        }
                        .clickable {
                            toggleFloatingPopup(params.x, params.y)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ع",
                        color = Color.White,
                        fontSize = (sizeDp.value * 0.45).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        try {
            windowManager.addView(bubbleComposeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showTrashZone() {
        if (trashComposeView != null) return

        val trashParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 80
        }

        trashComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(this@FloatingBubbleService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBubbleService)

            setContent {
                val isOverTrash by isOverTrashState.collectAsState()

                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .size(if (isOverTrash) 80.dp else 68.dp)
                        .scale(if (isOverTrash) 1.15f else 1.0f)
                        .clip(CircleShape)
                        .background(if (isOverTrash) Color(0xFFEF4444) else Color(0xCC1E293B))
                        .border(2.dp, if (isOverTrash) Color.White else Color(0xFF94A3B8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                        modifier = Modifier.size(if (isOverTrash) 38.dp else 30.dp)
                    )
                }
            }
        }

        try {
            windowManager.addView(trashComposeView, trashParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideTrashZone() {
        if (trashComposeView != null) {
            try {
                trashComposeView?.disposeComposition()
                windowManager.removeView(trashComposeView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            trashComposeView = null
            isOverTrashState.value = false
        }
    }

    private fun toggleFloatingPopup(bubbleX: Int, bubbleY: Int) {
        if (isPopupOpen) {
            closePopup()
        } else {
            openPopup(bubbleX, bubbleY)
        }
    }

    private fun openPopup(bubbleX: Int, bubbleY: Int) {
        if (popupComposeView != null) return

        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val density = metrics.density

        val popupWidthDpVal = popupWidthState.value
        val widthPx = (popupWidthDpVal * density).toInt()
        val bubbleSizePx = (bubbleSizeState.value * density).toInt()

        // Position popup anchored next to the bubble
        var popupX = if (bubbleX > screenWidth / 2) {
            bubbleX - widthPx - (8 * density).toInt()
        } else {
            bubbleX + bubbleSizePx + (8 * density).toInt()
        }

        var popupY = bubbleY - (10 * density).toInt()

        val marginPx = (12 * density).toInt()
        popupX = popupX.coerceIn(marginPx, maxOf(marginPx, screenWidth - widthPx - marginPx))
        popupY = popupY.coerceIn(marginPx, maxOf(marginPx, screenHeight - (280 * density).toInt()))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = popupX
            y = popupY
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        }

        popupComposeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(this@FloatingBubbleService)
            setViewTreeSavedStateRegistryOwner(this@FloatingBubbleService)

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    closePopup()
                    true
                } else {
                    false
                }
            }

            setContent {
                val popupWidthVal by popupWidthState.collectAsState()
                val popupOpacityVal by popupOpacityState.collectAsState()
                val autoFocusKeyboardVal by autoFocusKeyboardState.collectAsState()

                var rawInputText by remember { mutableStateOf("") }
                val clipboardManager = LocalClipboardManager.current
                var copyToastVisible by remember { mutableStateOf(false) }

                val focusRequester = remember { FocusRequester() }
                val keyboardController = LocalSoftwareKeyboardController.current

                val reshapedResult = remember(rawInputText) {
                    if (rawInputText.isBlank()) "" else ArabicReshaperEngine.reshape(rawInputText, convertNumbers = userPrefs.convertNumbersToIndic)
                }

                LaunchedEffect(copyToastVisible) {
                    if (copyToastVisible) {
                        delay(2500)
                        copyToastVisible = false
                    }
                }

                LaunchedEffect(Unit) {
                    if (autoFocusKeyboardVal) {
                        delay(150)
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }

                Surface(
                    modifier = Modifier
                        .width(popupWidthVal.dp)
                        .alpha(popupOpacityVal)
                        .shadow(12.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E1B4B),
                    contentColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6366F1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("ع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تشكيل عربي مباشر",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = { closePopup() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Standard Arabic Input Field
                        OutlinedTextField(
                            value = rawInputText,
                            onValueChange = { newValue ->
                                rawInputText = newValue
                            },
                            placeholder = { Text("اكتب أو الصق النص هنا...", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 110.dp)
                                .focusRequester(focusRequester),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedBorderColor = Color(0xFF818CF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Action Buttons Row: Clear, Paste, Copy
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (rawInputText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { rawInputText = "" },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "مسح", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val clip = clipboardManager.getText()?.text
                                        if (!clip.isNullOrBlank()) {
                                            rawInputText = clip
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = "لصق", tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                                }
                            }

                            Button(
                                onClick = {
                                    if (reshapedResult.isNotEmpty()) {
                                        clipboardManager.setText(AnnotatedString(reshapedResult))
                                        copyToastVisible = true
                                        serviceScope.launch {
                                            repository.insert(rawInputText, reshapedResult)
                                        }
                                    }
                                },
                                enabled = reshapedResult.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        AnimatedVisibility(visible = copyToastVisible, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                text = "✓ تم النسخ والحفظ بنجاح!",
                                color = Color(0xFF34D399),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        try {
            windowManager.addView(popupComposeView, params)
            isPopupOpen = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closePopup() {
        if (popupComposeView != null) {
            try {
                popupComposeView?.disposeComposition()
                windowManager.removeView(popupComposeView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            popupComposeView = null
            isPopupOpen = false
        }
    }

    override fun onDestroy() {
        closePopup()
        hideTrashZone()
        if (bubbleComposeView != null) {
            try {
                bubbleComposeView?.disposeComposition()
                windowManager.removeView(bubbleComposeView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bubbleComposeView = null
        }
        serviceJob.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_SERVICE = "com.example.service.STOP_BUBBLE"

        val bubbleSizeState = MutableStateFlow(56)
        val bubbleOpacityState = MutableStateFlow(0.9f)
        val popupWidthState = MutableStateFlow(290)
        val popupOpacityState = MutableStateFlow(0.95f)
        val autoFocusKeyboardState = MutableStateFlow(true)

        fun startService(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.stopService(intent)
        }
    }
}
