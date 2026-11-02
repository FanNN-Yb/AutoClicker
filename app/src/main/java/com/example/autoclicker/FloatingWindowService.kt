package com.example.autoclicker

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import kotlin.math.roundToInt
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import com.example.autoclicker.databinding.FloatingWindowBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FloatingWindowService : Service() {

    private lateinit var binding: FloatingWindowBinding
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private lateinit var windowParams: WindowManager.LayoutParams

    // 配置列表
    private val configs = mutableListOf<MacroConfig>()
    private var selectedConfigIndex = -1
    // 缓存已创建的配置项 View，避免反复 removeAllViews + 重建
    private val configItemViews = mutableListOf<View>()
    private var lastConfigCount = -1

    // 录制中的临时数据
    private val recordingPositions = mutableListOf<Triple<Int, Int, Long>>()
    private var isRecording = false
    private var touchInterceptor: View? = null
    // 进入录制时保存原始窗口状态，退出时恢复
    private var savedWindowParams: WindowManager.LayoutParams? = null
    private var savedRootBackground: Drawable? = null

    // 执行状态
    @Volatile private var isExecuting = false
    private var currentRepeat = 0
    private var currentPosition = 0
    private var executionJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 拖动状态
    private var dragStartX = 0
    private var dragStartY = 0
    private var dragStartWindowX = 0
    private var dragStartWindowY = 0
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

        override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(1, createNotification())
        createFloatingWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

        override fun onDestroy() {
        stopExecution()
        stopRecording()
        serviceScope.cancel()
        floatingView?.let { windowManager.removeView(it) }
        instance = null
        super.onDestroy()
    }

        // ==================== 悬浮窗 ====================

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingWindow() {
        val themedContext = ContextThemeWrapper(this, R.style.Theme_AutoClicker)
        binding = FloatingWindowBinding.inflate(LayoutInflater.from(themedContext))
        floatingView = binding.root

        // 根据主题设置背景颜色
        applyThemeToWindow()

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
                    }

                // 标题栏可拖动（只在标题栏区域拖动，不拦截子控件）
        binding.tvTitle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX.toInt()
                    dragStartY = event.rawY.toInt()
                    dragStartWindowX = windowParams.x
                    dragStartWindowY = windowParams.y
                    isDragging = false
                    true  // 必须返回 true，否则后续 MOVE/UP 事件不会传递到这个 View
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - dragStartX
                    val dy = event.rawY.toInt() - dragStartY
                    if (dx * dx + dy * dy > 100) isDragging = true
                    val newX = dragStartWindowX + dx
                    val newY = dragStartWindowY + dy
                    if (newX != windowParams.x || newY != windowParams.y) {
                        windowParams.x = newX
                        windowParams.y = newY
                        windowManager.updateViewLayout(floatingView, windowParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    true
                }
                else -> false
            }
        }

        // 执行面板拖动区域
        binding.execDragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX.toInt()
                    dragStartY = event.rawY.toInt()
                    dragStartWindowX = windowParams.x
                    dragStartWindowY = windowParams.y
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - dragStartX
                    val dy = event.rawY.toInt() - dragStartY
                    if (dx * dx + dy * dy > 100) isDragging = true
                    val newX = dragStartWindowX + dx
                    val newY = dragStartWindowY + dy
                    if (newX != windowParams.x || newY != windowParams.y) {
                        windowParams.x = newX
                        windowParams.y = newY
                        windowManager.updateViewLayout(floatingView, windowParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    isDragging = false
                    true
                }
                else -> false
            }
        }

        // EditText 获取焦点时切换窗口标志，允许弹出输入法
        val focusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                // EditText 获焦时：移除 FLAG_NOT_FOCUSABLE，添加 FLAG_NOT_TOUCH_MODAL
                windowParams.flags = windowParams.flags and
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv() or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                windowManager.updateViewLayout(floatingView, windowParams)
                // 弹出输入法
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            } else {
                // EditText 失焦时：恢复 FLAG_NOT_FOCUSABLE
                windowParams.flags = windowParams.flags or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(floatingView, windowParams)
            }
        }
        binding.etSpeed.onFocusChangeListener = focusChangeListener
        binding.etRepeatCount.onFocusChangeListener = focusChangeListener

        // 关闭按钮
        binding.btnClose.setOnClickListener {
            stopSelf()
        }

        // 返回按钮（执行面板）
        binding.btnExecBack.setOnClickListener {
            stopExecution()
        }

        // 启用按钮
        binding.btnEnable.setOnClickListener {
            showExecutingPanel()
        }

        // 执行按钮
        binding.btnExecMacro.setOnClickListener {
            if (isExecuting) {
                stopExecution()
            } else {
                startExecution()
            }
        }

        // 录制控制按钮
        binding.btnRecordBack.setOnClickListener { cancelRecording() }
        binding.btnRecordReset.setOnClickListener { resetRecording() }
        binding.btnRecordSave.setOnClickListener { saveRecording() }

        // 速度和重复次数输入框监听
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (selectedConfigIndex >= 0 && selectedConfigIndex < configs.size) {
                    val speed = binding.etSpeed.text.toString().toFloatOrNull() ?: 1f
                    val repeat = binding.etRepeatCount.text.toString().toIntOrNull() ?: 1
                    configs[selectedConfigIndex] = configs[selectedConfigIndex].copy(
                        speed = speed.coerceIn(0.1f, 10f),
                        repeatCount = repeat.coerceAtLeast(1)
                    )
                    saveConfigs()
                }
            }
        }
        binding.etSpeed.addTextChangedListener(textWatcher)
        binding.etRepeatCount.addTextChangedListener(textWatcher)

        // 加载配置
        loadConfigs()
        if (configs.isNotEmpty()) {
            selectedConfigIndex = 0
            val config = configs[0]
            binding.etSpeed.setText(config.speed.toString())
            binding.etRepeatCount.setText(config.repeatCount.toString())
        }
                                refreshConfigList()

        // 启用按钮跟随主题色
        applyAccentToButton(binding.btnEnable)

                // 设置初始透明度（使用 View 级别，避免低透明度时触摸失效）
                floatingView?.alpha = ThemeSettings.floatingWindowOpacity

        // 将悬浮窗添加到窗口
        windowManager.addView(floatingView, windowParams)
    }

                /** 更新悬浮窗透明度（从设置页面调用） */
    fun updateWindowOpacity(opacity: Float) {
        try {
            floatingView?.alpha = opacity.coerceIn(0.3f, 1.0f)
        } catch (e: Exception) {
            Log.e("FloatingWindow", "updateWindowOpacity failed", e)
        }
    }

            /** 根据主题设置窗口背景颜色 */
    private fun applyThemeToWindow() {
        val bgColor = android.graphics.Color.argb(230, 28, 28, 30)
        val borderColor = android.graphics.Color.argb(51, 255, 255, 255)

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 22f * resources.displayMetrics.density
            setColor(bgColor)
            setStroke(resources.displayMetrics.density.roundToInt(), borderColor)
        }
        binding.root.background = bgDrawable
    }

        /** 更新悬浮窗主题（从设置页面调用） */
    fun updateWindowTheme() {
        try {
            applyThemeToWindow()
            applyAccentToButton(binding.btnEnable)
        } catch (e: Exception) {
            Log.e("FloatingWindow", "updateWindowTheme failed", e)
        }
    }

    /** 给按钮设置主题色背景 */
    private fun applyAccentToButton(button: android.widget.Button) {
        val accent = ThemeSettings.getAccentColor()
        val accentArgb = android.graphics.Color.argb(
            (accent.alpha * 255).toInt(),
            (accent.red * 255).toInt(),
            (accent.green * 255).toInt(),
            (accent.blue * 255).toInt()
        )
        button.backgroundTintList = android.content.res.ColorStateList.valueOf(accentArgb)
    }

    // ==================== 配置列表 ====================

    private fun loadConfigs() {
        configs.clear()
        configs.addAll(MacroRepository.loadAll())
    }

    private fun saveConfigs() {
        MacroRepository.saveAll(configs)
    }

    /** 如果名字已存在，自动在后面加编号 "(2)" "(3)"... */
    private fun generateUniqueName(base: String): String {
        val existingNames = configs.map { it.name }.toSet()
        if (base !in existingNames) return base
        var i = 2
        while ("$base ($i)" in existingNames) i++
        return "$base ($i)"
    }

    private fun refreshConfigList() {
        val container = binding.configListContainer

        // 仅在配置数量变化时才重建全部 View（新增/删除）
        if (configs.size != lastConfigCount) {
            container.removeAllViews()
            configItemViews.clear()

            configs.forEachIndexed { index, config ->
                val item = createConfigItem(config.name, index)
                container.addView(item)
                configItemViews.add(item)
            }

            // 底部"新建宏"项
            val addItem = createConfigItem("+ 新建宏", -1)
            container.addView(addItem)

            lastConfigCount = configs.size
        } else {
            // 仅选中状态变化，增量更新样式即可
            configItemViews.forEachIndexed { index, view ->
                updateConfigItemStyle(view as android.widget.TextView, index)
            }
        }

        updateEnableButton()
    }

    /** 增量更新单个配置项的选中/未选中样式 */
    private fun updateConfigItemStyle(tv: android.widget.TextView, index: Int) {
        val accentColor = ThemeSettings.getAccentColor()
        val accentArgb = android.graphics.Color.argb(
            (accentColor.alpha * 255).toInt(),
            (accentColor.red * 255).toInt(),
            (accentColor.green * 255).toInt(),
            (accentColor.blue * 255).toInt()
        )
        if (index == selectedConfigIndex) {
            tv.background = createSelectedBackground(accentArgb)
            tv.setTextColor(accentArgb)
        } else {
            tv.setBackgroundResource(R.drawable.config_item_normal)
            tv.setTextColor(android.graphics.Color.argb(153, 255, 255, 255)) // #99FFFFFF，固定亮色适配深色背景
        }
    }

    private fun createConfigItem(name: String, index: Int): View {
        val tv = TextView(this).apply {
            text = name
            textSize = 12f
            setPadding(20, 10, 20, 10)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 4 }
        }

                // 样式 - 使用自定义主题色
        val accentColor = ThemeSettings.getAccentColor()
        val accentArgb = android.graphics.Color.argb(
            (accentColor.alpha * 255).toInt(),
            (accentColor.red * 255).toInt(),
            (accentColor.green * 255).toInt(),
            (accentColor.blue * 255).toInt()
        )
        if (index == selectedConfigIndex) {
            // 选中状态：使用主题色背景
            tv.background = createSelectedBackground(accentArgb)
            tv.setTextColor(accentArgb)
        } else {
            tv.setBackgroundResource(R.drawable.config_item_normal)
            tv.setTextColor(android.graphics.Color.argb(153, 255, 255, 255)) // #99FFFFFF，固定亮色适配深色背景
        }

        tv.setOnClickListener {
            if (isDragging) return@setOnClickListener
            if (index == -1) {
                // 新建宏
                startRecording()
            } else {
                // 选中配置
                selectedConfigIndex = index
                val config = configs[index]
                binding.etSpeed.setText(config.speed.toString())
                binding.etRepeatCount.setText(config.repeatCount.toString())
                refreshConfigList()
            }
        }

        return tv
    }

    /** 创建选中状态的背景 drawable */
    private fun createSelectedBackground(accentColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 22f * resources.displayMetrics.density
            setColor(0x33000000 or (accentColor and 0x00FFFFFF))  // 20% 透明度的主题色
            setStroke(
                (1 * resources.displayMetrics.density).toInt(),
                accentColor
            )
        }
    }

    private fun updateEnableButton() {
        binding.btnEnable.isEnabled = selectedConfigIndex in configs.indices && !isExecuting
    }

    // ==================== 录制 ====================

    @SuppressLint("ClickableViewAccessibility")
    private fun startRecording() {
        isRecording = true
        recordingPositions.clear()
        binding.tvRecordCount.text = "已录 0 个位置"

        binding.panelMain.visibility = View.GONE
        binding.panelRecord.visibility = View.VISIBLE
        applyAccentToButton(binding.btnRecordSave)

        // 保存原始窗口参数和背景
        savedWindowParams = WindowManager.LayoutParams(
            windowParams.width, windowParams.height,
            windowParams.type, windowParams.flags, windowParams.format
        ).apply {
            gravity = windowParams.gravity
            x = windowParams.x
            y = windowParams.y
        }
        savedRootBackground = floatingView?.background
        // 清除悬浮窗根背景 → 全透明，用户能看到背后的页面内容
        floatingView?.background = null
        // 给录制面板设置自己的背景，否则跟着根背景一起变透明了
        applyPanelBackground(binding.panelRecord)

        // 将悬浮窗铺满全屏
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.height = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.x = 0
        windowParams.y = 0
        windowManager.updateViewLayout(floatingView, windowParams)

        // 允许拖动标题栏来移动录制面板
        setupPanelDrag()

        // 绘制录制点的画笔
        val accentArgb = android.graphics.Color.argb(
            (ThemeSettings.getAccentColor().alpha * 255).toInt(),
            (ThemeSettings.getAccentColor().red * 255).toInt(),
            (ThemeSettings.getAccentColor().green * 255).toInt(),
            (ThemeSettings.getAccentColor().blue * 255).toInt()
        )
        val dotRadius = (8 * resources.displayMetrics.density)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            alpha = 180
            style = Paint.Style.FILL
        }
        val dotStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentArgb
            style = Paint.Style.STROKE
            strokeWidth = 2f * resources.displayMetrics.density
        }

        // 预缓存路径：每次新增点时重建，避免 onDraw 每帧遍历所有点
        val cachedDotPath = Path()

        fun rebuildDotPath() {
            cachedDotPath.reset()
            for ((x, y, _) in recordingPositions) {
                cachedDotPath.addCircle(x.toFloat(), y.toFloat(), dotRadius, Path.Direction.CW)
            }
        }

        // 在 floatingView 内部添加透明覆盖层（同一 ViewGroup，非独立 Window）
        // 同时在上面绘制已录制的点
        touchInterceptor = object : View(this) {
            override fun onDraw(canvas: android.graphics.Canvas) {
                super.onDraw(canvas)
                if (recordingPositions.isEmpty()) return
                val loc = IntArray(2)
                getLocationOnScreen(loc)
                canvas.save()
                canvas.translate(-loc[0].toFloat(), -loc[1].toFloat())
                canvas.drawPath(cachedDotPath, dotPaint)
                canvas.drawPath(cachedDotPath, dotStroke)
                canvas.restore()
            }
        }.apply {
            setBackgroundColor(0x00000000)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val tx = event.rawX.toInt()
                    val ty = event.rawY.toInt()
                    // 触摸在录制面板上 → 不拦截，让按钮处理
                    if (isTouchOnView(binding.panelRecord, tx, ty)) {
                        return@setOnTouchListener false
                    }
                    // 面板外 → 录制坐标
                    recordingPositions.add(Triple(tx, ty, System.currentTimeMillis()))
                    rebuildDotPath()
                    val count = recordingPositions.size
                    Handler(Looper.getMainLooper()).post {
                        binding.tvRecordCount.text = "已录 $count 个位置"
                    }
                    invalidate()
                }
                true
            }
        }
        (floatingView as? ViewGroup)?.addView(touchInterceptor)
    }

    /** 给 panelRecord 设置独立的圆角半透明背景（不会跟着根背景一起消失） */
    private fun applyPanelBackground(panel: View) {
        val bgColor = android.graphics.Color.argb(230, 28, 28, 30)
        val borderColor = android.graphics.Color.argb(51, 255, 255, 255)

        panel.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 22f * resources.displayMetrics.density
            setColor(bgColor)
            setStroke(resources.displayMetrics.density.roundToInt(), borderColor)
        }
        // 内边距让内容不贴边
        panel.setPadding(
            (10 * resources.displayMetrics.density).toInt(),
            (10 * resources.displayMetrics.density).toInt(),
            (10 * resources.displayMetrics.density).toInt(),
            (10 * resources.displayMetrics.density).toInt()
        )
    }

    /** 录制面板标题栏可拖动，限制在屏幕边界内 */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupPanelDrag() {
        var panelDragStartX = 0
        var panelDragStartY = 0
        var panelStartTx = 0f
        var panelStartTy = 0f

        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels

        binding.recordTitleBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    panelDragStartX = event.rawX.toInt()
                    panelDragStartY = event.rawY.toInt()
                    panelStartTx = binding.panelRecord.translationX
                    panelStartTy = binding.panelRecord.translationY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val panel = binding.panelRecord
                    val rawTx = panelStartTx + (event.rawX - panelDragStartX)
                    val rawTy = panelStartTy + (event.rawY - panelDragStartY)
                    val pw = if (panel.width > 0) panel.width else panel.layoutParams.width
                    val ph = if (panel.height > 0) panel.height else panel.layoutParams.height
                    // 限制面板不超出屏幕边界（整体必须留在屏幕内）
                    panel.translationX = rawTx.coerceIn(
                        0f,
                        (screenW - pw).toFloat().coerceAtLeast(0f)
                    )
                    panel.translationY = rawTy.coerceIn(
                        0f,
                        (screenH - ph).toFloat().coerceAtLeast(0f)
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun stopRecording() {
        isRecording = false
        // 删除内部覆盖层
        touchInterceptor?.let {
            try { (floatingView as? ViewGroup)?.removeView(it) } catch (_: Exception) {}
        }
        touchInterceptor = null
        // 清除录制面板的背景和拖动监听
        binding.panelRecord.background = null
        binding.panelRecord.translationX = 0f
        binding.panelRecord.translationY = 0f
        binding.recordTitleBar.setOnTouchListener(null)
        // 恢复原始大小、位置和根背景
        floatingView?.background = savedRootBackground
        savedRootBackground = null
        savedWindowParams?.let { saved ->
            try {
                windowParams.width = saved.width
                windowParams.height = saved.height
                windowParams.x = saved.x
                windowParams.y = saved.y
                windowManager.updateViewLayout(floatingView, windowParams)
            } catch (_: Exception) {}
            savedWindowParams = null
        }
    }

    private fun cancelRecording() {
        stopRecording()
        recordingPositions.clear()
        binding.panelRecord.visibility = View.GONE
        binding.panelMain.visibility = View.VISIBLE
    }

    private fun resetRecording() {
        recordingPositions.clear()
        touchInterceptor?.invalidate()
        binding.tvRecordCount.text = "已录 0 个位置"
        Toast.makeText(this, "已重置", Toast.LENGTH_SHORT).show()
    }

    private fun saveRecording() {
        if (recordingPositions.isEmpty()) {
            Toast.makeText(this, "请先录制至少一个点", Toast.LENGTH_SHORT).show()
            return
        }

        val defaultName = "宏 ${configs.size + 1}"
        val density = resources.displayMetrics.density
        val dp = { px: Int -> (px * density).toInt() }

        // 主题色（固定深色模式）
        val bgColor = android.graphics.Color.argb(240, 28, 28, 30)
        val textPrimary = android.graphics.Color.argb(255, 255, 255, 255)
        val textSecondary = android.graphics.Color.argb(200, 255, 255, 255)
        val inputBg = android.graphics.Color.argb(30, 255, 255, 255)
        val accent = ThemeSettings.getAccentColor()
        val accentArgb = android.graphics.Color.argb(
            (accent.alpha * 255).toInt(),
            (accent.red * 255).toInt(),
            (accent.green * 255).toInt(),
            (accent.blue * 255).toInt()
        )
        val dialogWidth = dp(200)

        // === 构建自定义布局 ===
        val title = TextView(this).apply {
            text = "保存宏"
            textSize = 16f
            setTextColor(textPrimary)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(10))
        }

        val input = EditText(this).apply {
            setText(defaultName)
            selectAll()
            setSingleLine()
            gravity = android.view.Gravity.START
            setTextColor(textPrimary)
            setHintTextColor(textSecondary)
            setPadding(dp(16), dp(4), dp(16), dp(4))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f // 胶囊
                setColor(inputBg)
            }
        }

        val btnCancel = android.widget.Button(this).apply {
            text = "取消"
            textSize = 13f
            setTextColor(textSecondary)
            isAllCaps = false
            minimumHeight = 0
            minHeight = 0
            setPadding(dp(12), dp(5), dp(12), dp(5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(android.graphics.Color.TRANSPARENT)
                setStroke((1.5f * density).toInt(), textSecondary)
            }
        }

        val btnSave = android.widget.Button(this).apply {
            text = "保存"
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            isAllCaps = false
            minimumHeight = 0
            minHeight = 0
            setPadding(dp(12), dp(5), dp(12), dp(5))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 100f
                setColor(accentArgb)
            }
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
            addView(btnCancel, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginEnd = dp(8) })
            addView(btnSave, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ))
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(12))
            addView(title)
            addView(input)
            addView(buttonRow)
        }

        // === 创建弹窗 ===
        val dialog = android.app.Dialog(this, R.style.Theme_AutoClicker)
        dialog.setContentView(content)
        dialog.window?.apply {
            setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            setLayout(dialogWidth, LinearLayout.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f * density // 圆角
                setColor(bgColor)
                setStroke((1.5f * density).toInt(),
                    android.graphics.Color.argb(50, 255, 255, 255)
                )
            })
        }

        // === 保存逻辑 ===
        btnSave.setOnClickListener {
            val rawName = input.text.toString().trim().ifEmpty { defaultName }
            val name = generateUniqueName(rawName)
            val positions = recordingPositions.map { Triple(it.first, it.second, it.third) }.toMutableList()
            val config = MacroConfig(name = name, positions = positions)
            configs.add(config)
            selectedConfigIndex = configs.size - 1
            saveConfigs()
            refreshConfigList()
            binding.etSpeed.setText(config.speed.toString())
            binding.etRepeatCount.setText(config.repeatCount.toString())

            recordingPositions.clear()
            stopRecording()
            binding.panelRecord.visibility = View.GONE
            binding.panelMain.visibility = View.VISIBLE

            dialog.dismiss()
            if (name != rawName) {
                Toast.makeText(this, "已保存 \"$name\" (同名已自动编号)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "已保存 \"$name\" (${positions.size} 个点)", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // ==================== 执行 ====================

    private fun showExecutingPanel() {
        if (selectedConfigIndex !in configs.indices) return
        val config = configs[selectedConfigIndex]
        if (config.positions.isEmpty()) {
            Toast.makeText(this, "配置无录制数据", Toast.LENGTH_SHORT).show()
            return
        }
        // 切换到小窗口，不自动播放
        binding.panelMain.visibility = View.GONE
        binding.panelExecuting.visibility = View.VISIBLE
        binding.btnExecMacro.text = config.name
        binding.btnExecMacro.setBackgroundResource(R.drawable.config_item_normal)
        binding.btnExecMacro.setTextColor(getColor(R.color.floating_window_text_secondary))
    }

    private fun startExecution() {
        if (selectedConfigIndex !in configs.indices) return
        val service = AutoClickerService.instance
        if (service == null) {
            Toast.makeText(this, "无障碍服务未连接", Toast.LENGTH_SHORT).show()
            return
        }

        val config = configs[selectedConfigIndex]
        if (config.positions.isEmpty()) return

        isExecuting = true
        currentRepeat = 0
        currentPosition = 0

                // 宏按钮变蓝，表示正在运行（使用自定义主题色）
        val accentColor = ThemeSettings.getAccentColor()
        val accentArgb = android.graphics.Color.argb(
            (accentColor.alpha * 255).toInt(),
            (accentColor.red * 255).toInt(),
            (accentColor.green * 255).toInt(),
            (accentColor.blue * 255).toInt()
        )
        binding.btnExecMacro.background = createSelectedBackground(accentArgb)
        binding.btnExecMacro.setTextColor(accentArgb)

        executionJob = serviceScope.launch {
            try {
                while (isActive && isExecuting && currentRepeat < config.repeatCount) {
                    if (currentPosition < config.positions.size) {
                        val pos = config.positions[currentPosition]
                        val startedAt = android.os.SystemClock.elapsedRealtime()

                        // 必须等手势真的走完再发下一个：fire-and-forget 时后一次 dispatch 会把
                        // 前一次还没跑完的手势取消掉，speed 越高手感越像「漏点」。
                        val clicked = service.performClickAwait(pos.first, pos.second)
                        if (!clicked && isActive) {
                            // 被系统 / 别的无障碍服务打断：隔一帧补一次，别静默吞掉这个点
                            delay(RetryGestureDelayMs)
                            service.performClickAwait(pos.first, pos.second)
                        }
                        currentPosition++

                        if (currentPosition < config.positions.size) {
                            val gapMs = (config.positions[currentPosition].third - pos.third)
                                .coerceAtLeast(0L)
                            val targetMs = (gapMs / config.speed.coerceAtLeast(0.01f)).toLong()
                            // 扣掉手势本身耗时：原来是「点完再等 gap」，节奏会一笔一笔往后拖
                            val remain = targetMs - (android.os.SystemClock.elapsedRealtime() - startedAt)
                            if (remain > 0) delay(remain)
                        }
                    } else {
                        currentRepeat++
                        if (currentRepeat < config.repeatCount) {
                            currentPosition = 0
                            delay(500)
                        }
                    }
                }
            } finally {
                isExecuting = false
                Handler(Looper.getMainLooper()).post {
                    binding.btnExecMacro.text = config.name
                    binding.btnExecMacro.setBackgroundResource(R.drawable.config_item_normal)
                    binding.btnExecMacro.setTextColor(getColor(R.color.floating_window_text_secondary))
                }
            }
        }
    }

    private fun stopExecution() {
        isExecuting = false
        executionJob?.cancel()
        executionJob = null

        // 切回主面板
        binding.panelExecuting.visibility = View.GONE
        binding.panelMain.visibility = View.VISIBLE
        updateEnableButton()
    }

    // ==================== 工具 ====================

    private fun isTouchOnView(view: View, touchX: Int, touchY: Int): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        return touchX >= location[0] && touchX <= location[0] + view.width &&
                touchY >= location[1] && touchY <= location[1] + view.height
    }

    // ==================== 通知 ====================

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "auto_clicker_channel"

        /** 手势被取消后重试前的等待，约一帧 */
        private const val RetryGestureDelayMs = 16L

        var instance: FloatingWindowService? = null
            private set
    }
}
