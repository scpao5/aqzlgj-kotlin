package com.sbby.aqzlgj.kotlin.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.res.ColorStateList
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.sbby.aqzlgj.kotlin.R
import com.sbby.aqzlgj.kotlin.data.CodeData
import com.sbby.aqzlgj.kotlin.data.CodeItem
import com.sbby.aqzlgj.kotlin.data.PrivilegeManager
import com.sbby.aqzlgj.kotlin.ui.util.AppToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * 游戏内悬浮窗服务（传统 View 方案）
 * - 拖动 / 展开折叠 / 搜索 / 分类浏览 / 一键执行
 * - 颜色适配应用主题（跟随 color_mode 的明暗）
 */
class FloatWindowService : Service() {

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set
    }

    /** 悬浮窗配色（跟随应用明暗主题） */
    private data class FloatColors(
        val surface: Int,
        val surfaceVariant: Int,
        val onSurface: Int,
        val textSecondary: Int,
        val hint: Int,
        val primary: Int,
        val error: Int,
    )

    private lateinit var c: FloatColors
    private var isDark = false

    private var windowManager: WindowManager? = null
    private var floatView: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var floatHeader: LinearLayout? = null
    private var floatTitle: TextView? = null
    private var floatArrowIv: ImageView? = null
    private var floatCloseIv: ImageView? = null
    private var floatBackIv: ImageView? = null
    private var floatExpandArea: LinearLayout? = null
    private var floatInput: EditText? = null
    private var floatSearchBtn: Button? = null
    private var floatListContainer: LinearLayout? = null
    private var floatListScroll: ScrollView? = null

    private var expanded = false
    private var listState = 0
    private var currentCategory: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var allCommands: List<CodeItem> = emptyList()

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0
    private var initialTouchY = 0

    private var closeConfirmView: View? = null
    private var closeConfirmLp: WindowManager.LayoutParams? = null

    /** 当前展开/折叠动画（互斥，防止连续点击动画打架） */
    private var heightAnimator: ValueAnimator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        c = loadColors()
        isDark = c.surface != 0xFFFFFFFF.toInt()
        startInForeground()

        allCommands = runBlocking(Dispatchers.IO) { CodeData.loadCodes(this@FloatWindowService) }
        showFloatWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (floatView != null && expanded) updateLayoutByContent()
        } catch (_: Exception) {
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        dismissCloseConfirmDialog()
        val wm = windowManager
        val fv = floatView
        if (wm != null && fv != null) {
            try {
                wm.removeView(fv)
            } catch (_: Exception) {
            }
        }
        floatView = null
        super.onDestroy()
    }

    /** 读取应用设置，计算悬浮窗配色 */
    private fun loadColors(): FloatColors {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val colorMode = prefs.getInt("color_mode", 0)
        val dark = when (colorMode) {
            2, 5, 6 -> true
            1, 4 -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
        return if (dark) {
            FloatColors(
                surface = 0xFF1C1C1E.toInt(),
                surfaceVariant = 0xFF2C2C2E.toInt(),
                onSurface = 0xFFFFFFFF.toInt(),
                textSecondary = 0xFFD1D1D6.toInt(),
                hint = 0xFF8A8A8E.toInt(),
                primary = 0xFF0A84FF.toInt(),
                error = 0xFFFF453A.toInt(),
            )
        } else {
            FloatColors(
                surface = 0xFFFFFFFF.toInt(),
                surfaceVariant = 0xFFF5F5F5.toInt(),
                onSurface = 0xFF212121.toInt(),
                textSecondary = 0xFF424242.toInt(),
                hint = 0xFF9E9E9E.toInt(),
                primary = 0xFF2196F3.toInt(),
                error = 0xFFF44336.toInt(),
            )
        }
    }

    // ========== 前台通知 ==========
    private fun startInForeground() {
        val channelId = "float_window_channel"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "悬浮窗", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        val notification = builder
            .setContentTitle("暗区指令工具")
            .setContentText("悬浮窗运行中")
            .setSmallIcon(R.drawable.ic_logo)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    // ========== 构建悬浮窗 UI ==========
    private fun showFloatWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val borderColor = if (isDark) 0xFF3A3A3C.toInt() else 0xFFE0E0E0.toInt()
        floatView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundDrawable(c.surface, dp(18f), borderColor)
        }

        floatHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(6), 0)
            minimumHeight = dp(44)
        }
        floatView!!.addView(floatHeader, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // 标题
        floatTitle = TextView(this).apply {
            text = "指令工具"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(c.primary)
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        floatHeader!!.addView(floatTitle, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        // 箭头图标
        floatArrowIv = ImageView(this).apply {
            setImageResource(R.drawable.ic_arrow_down)
            setColorFilter(c.hint)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            scaleType = ImageView.ScaleType.CENTER
            setOnClickListener {
                releaseInputFocusAndRestore()
                toggleExpand()
            }
        }
        floatHeader!!.addView(floatArrowIv, LinearLayout.LayoutParams(dp(28), dp(28)))

        // 关闭图标
        floatCloseIv = ImageView(this).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(c.error)
            setPadding(dp(8), dp(8), dp(8), dp(8))
            scaleType = ImageView.ScaleType.CENTER
            setOnClickListener { showCloseConfirmDialog() }
        }
        floatHeader!!.addView(floatCloseIv, LinearLayout.LayoutParams(dp(28), dp(28)))

        // 展开区
        floatExpandArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(6), dp(2), dp(6), dp(6))
        }
        floatView!!.addView(floatExpandArea, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // 搜索行
        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        floatExpandArea!!.addView(inputRow, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        floatInput = EditText(this).apply {
            hint = "搜索指令..."
            textSize = 12f
            setTextColor(c.onSurface)
            setHintTextColor(c.hint)
            background = createRoundDrawable(c.surfaceVariant, dp(15f))
            setPadding(dp(10), dp(6), dp(10), dp(6))
            maxLines = 1
            imeOptions = 6
            inputType = 1
        }
        inputRow.addView(floatInput, LinearLayout.LayoutParams(0, dp(30), 1f))

        floatSearchBtn = Button(this).apply {
            text = "搜索"
            textSize = 12f
            setTextColor(0xFFFFFFFF.toInt())
            background = ripple(0x33FFFFFF.toInt(), createRoundDrawable(c.primary, dp(15f)), 15)
            setPadding(dp(14), dp(4), dp(14), dp(4))
            setOnClickListener {
                releaseInputFocusAndRestore()
                doSearch()
            }
        }
        val btnParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(30))
        btnParams.leftMargin = dp(6)
        inputRow.addView(floatSearchBtn, btnParams)

        // 列表区
        floatListScroll = ScrollView(this).apply {
            setBackgroundColor(c.surface)
            isFillViewport = true
        }
        val scrollParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(200))
        scrollParams.topMargin = dp(6)
        floatExpandArea!!.addView(floatListScroll, scrollParams)

        floatListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
        }
        floatListScroll!!.addView(floatListContainer, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        floatInput!!.setOnEditorActionListener { _, _, _ ->
            doSearch()
            true
        }

        floatView!!.setOnClickListener { releaseInputFocusAndRestore() }

        attachDragListener(floatHeader!!)
        attachInputClickListener()

        layoutParams = WindowManager.LayoutParams(
            dp(220), dp(44),
            getLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dp(80)
        }

        try {
            windowManager!!.addView(floatView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
            AppToast.show(this, "悬浮窗添加失败")
            stopSelf()
        }
    }

    // ========== 展开/折叠 ==========
    private fun toggleExpand() {
        // 动画进行中忽略快速连点，避免多个动画同时更新窗口
        if (heightAnimator?.isRunning == true) return
        expanded = !expanded
        if (expanded) {
            floatExpandArea!!.visibility = View.VISIBLE
            floatArrowIv!!.setImageResource(R.drawable.ic_arrow_up)
            floatArrowIv!!.setColorFilter(c.hint)
            if (floatListContainer!!.childCount == 0) {
                showCategoryList()
            }
            val target = computeTargetHeight()
            // 先回折叠态，再平滑展开
            layoutParams!!.height = dp(44)
            try {
                windowManager!!.updateViewLayout(floatView, layoutParams)
            } catch (_: Exception) {
            }
            animateHeight(target)
        } else {
            floatArrowIv!!.setImageResource(R.drawable.ic_arrow_down)
            floatArrowIv!!.setColorFilter(c.hint)
            animateHeight(dp(44)) {
                floatExpandArea!!.visibility = View.GONE
            }
        }
    }

    /** 高度平滑动画（互斥：启动前取消旧动画） */
    private fun animateHeight(target: Int, onEnd: (() -> Unit)? = null) {
        heightAnimator?.cancel()
        val animator = ValueAnimator.ofInt(layoutParams!!.height, target)
        animator.duration = 220
        animator.addUpdateListener {
            layoutParams!!.height = it.animatedValue as Int
            try {
                windowManager!!.updateViewLayout(floatView, layoutParams)
            } catch (_: Exception) {
            }
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                heightAnimator = null
                onEnd?.invoke()
            }

            override fun onAnimationCancel(animation: Animator) {
                heightAnimator = null
            }
        })
        heightAnimator = animator
        animator.start()
    }

    /** 计算展开目标高度（不直接设置） */
    private fun computeTargetHeight(): Int {
        return try {
            val screenH = try {
                val p = android.graphics.Point()
                windowManager!!.defaultDisplay.getRealSize(p)
                p.y
            } catch (_: Exception) {
                resources.displayMetrics.heightPixels
            }
            val maxH = (screenH * 0.85f).toInt()
            val widthSpec = View.MeasureSpec.makeMeasureSpec(layoutParams!!.width, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            floatView!!.measure(widthSpec, heightSpec)
            var naturalH = floatView!!.measuredHeight
            if (naturalH <= 0) naturalH = dp(44 + 200)
            if (floatListScroll != null && naturalH > maxH) {
                val overflow = naturalH - maxH
                val currentScrollH = floatListScroll!!.height
                var newScrollH = currentScrollH - overflow
                if (newScrollH < dp(80)) newScrollH = dp(80)
                val lp = floatListScroll!!.layoutParams
                if (lp != null) {
                    lp.height = newScrollH
                    floatListScroll!!.layoutParams = lp
                }
            }
            var h = minOf(naturalH, maxH)
            if (h < dp(44 + 80)) h = dp(44 + 80)
            h
        } catch (e: Exception) {
            dp(44 + 200)
        }
    }

    private fun updateLayoutByContent() {
        val h = computeTargetHeight()
        layoutParams!!.height = h
        try {
            windowManager!!.updateViewLayout(floatView, layoutParams)
        } catch (_: Exception) {
        }
    }

    // ========== 数据展示 ==========
    private fun showCategoryList() {
        listState = 0
        currentCategory = null
        updateFloatTitle()
        floatListContainer!!.removeAllViews()
        floatBackIv?.visibility = View.GONE
        CodeData.getCategories().forEach { cat ->
            if (cat == CodeData.CATEGORY_MIX) return@forEach
            val row = makeRowButton(cat)
            row.setOnClickListener { showCategoryItems(cat) }
            floatListContainer!!.addView(row)
        }
        scrollListToTop()
        updateLayoutByContent()
    }

    private fun showCategoryItems(category: String) {
        listState = 2
        currentCategory = category
        updateFloatTitle()
        floatListContainer!!.removeAllViews()
        ensureBackBtn()
        floatBackIv!!.visibility = View.VISIBLE
        val items = allCommands.filter { it.category == category }
        if (items.isEmpty()) {
            val empty = TextView(this).apply {
                text = "该分区暂无指令"
                textSize = 12f
                setTextColor(c.hint)
                setPadding(dp(6), dp(8), dp(6), dp(6))
            }
            floatListContainer!!.addView(empty)
        } else {
            items.forEach { addItemRow(null, it.title, it.code) }
        }
        scrollListToTop()
        updateLayoutByContent()
    }

    private fun updateFloatTitle() {
        floatTitle!!.text = when (listState) {
            0 -> "指令分类"
            1 -> "搜索结果"
            2 -> currentCategory ?: "指令工具"
            else -> "指令工具"
        }
    }

    private fun doSearch() {
        val kw = floatInput!!.text.toString().trim()
        if (kw.isEmpty()) {
            AppToast.show(this, "请输入关键词")
            return
        }
        listState = 1
        currentCategory = null
        floatTitle!!.text = "搜索结果"
        floatListContainer!!.removeAllViews()
        ensureBackBtn()
        floatBackIv!!.visibility = View.VISIBLE
        val results = allCommands.filter {
            it.category != CodeData.CATEGORY_MIX &&
                (it.title.contains(kw, ignoreCase = true) || it.code.contains(kw, ignoreCase = true))
        }
        if (results.isEmpty()) {
            val empty = TextView(this).apply {
                text = "未找到相关指令"
                textSize = 12f
                setTextColor(c.hint)
                setPadding(dp(6), dp(8), dp(6), dp(6))
            }
            floatListContainer!!.addView(empty)
        } else {
            results.forEach { addItemRow(it.category, it.title, it.code) }
        }
        scrollListToTop()
        updateLayoutByContent()
    }

    private fun ensureBackBtn() {
        if (floatBackIv == null) {
            floatBackIv = ImageView(this).apply {
                setImageResource(R.drawable.ic_arrow_back)
                setColorFilter(c.hint)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                scaleType = ImageView.ScaleType.CENTER
                setOnClickListener { showCategoryList() }
            }
            val lp = LinearLayout.LayoutParams(dp(32), dp(30))
            lp.leftMargin = dp(4)
            floatBackIv!!.layoutParams = lp
            val parent = floatInput!!.parent as? ViewGroup
            if (parent != null) {
                parent.addView(floatBackIv, 0)
            }
        }
    }

    private fun addItemRow(categoryTag: String?, title: String, code: String) {
        if (categoryTag == CodeData.CATEGORY_MIX) return
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(6)
            layoutParams = lp
            background = ripple(0x26000000.toInt(), createRoundDrawable(c.surfaceVariant, dp(14f)), 14)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            isClickable = true
        }

        if (!categoryTag.isNullOrEmpty()) {
            val tagView = TextView(this).apply {
                text = categoryTag
                textSize = 10f
                setTextColor(c.primary)
                setPadding(0, 0, 0, dp(2))
            }
            card.addView(tagView)
        }
        val titleView = TextView(this).apply {
            text = title
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(c.onSurface)
        }
        card.addView(titleView)

        val codeView = TextView(this).apply {
            text = code
            textSize = 11f
            setTextColor(c.textSecondary)
            typeface = Typeface.MONOSPACE
            background = createRoundDrawable(c.surfaceVariant, dp(8f))
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }
        val codeLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        codeLp.topMargin = dp(4)
        card.addView(codeView, codeLp)

        val clickListener = View.OnClickListener {
            PrivilegeManager.execute(this, code)
        }
        // 卡片整体点击执行；代码块不再单独消费点击，触摸会冒泡到卡片（按压反馈正常显示）
        card.setOnClickListener(clickListener)
        floatListContainer!!.addView(card)
    }

    private fun makeRowButton(text: String): TextView {
        return TextView(this).apply {
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(8)
            layoutParams = lp
            this.text = text
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFFFFFFFF.toInt())
            background = ripple(0x33FFFFFF.toInt(), createGradient(c.primary, dp(22f)), 22)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
    }

    private fun scrollListToTop() {
        floatListScroll?.scrollTo(0, 0)
    }

    // ========== 拖动 ==========
    private fun attachDragListener(dragView: View) {
        dragView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams!!.x
                    initialY = layoutParams!!.y
                    initialTouchX = event.rawX.toInt()
                    initialTouchY = event.rawY.toInt()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - initialTouchX
                    val dy = event.rawY.toInt() - initialTouchY
                    layoutParams!!.x = initialX + dx
                    layoutParams!!.y = initialY + dy
                    try {
                        windowManager!!.updateViewLayout(floatView, layoutParams)
                    } catch (_: Exception) {
                    }
                    true
                }
                else -> false
            }
        }
    }

    // ========== 输入法 ==========
    private fun attachInputClickListener() {
        floatInput!!.setOnClickListener { requestInputFocusAndShowIme() }
        floatInput!!.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                ensureImeShown()
            } else {
                releaseInputFocusAndRestore()
            }
        }
    }

    private fun requestInputFocusAndShowIme() {
        try {
            val oldFlags = layoutParams!!.flags
            val newFlags = oldFlags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            if (newFlags != oldFlags) {
                layoutParams!!.flags = newFlags
                windowManager!!.updateViewLayout(floatView, layoutParams)
            }
            floatInput!!.isFocusable = true
            floatInput!!.isFocusableInTouchMode = true
            floatInput!!.requestFocus()
            ensureImeShown()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun ensureImeShown() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(floatInput, InputMethodManager.SHOW_IMPLICIT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseInputFocusAndRestore() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(floatInput!!.windowToken, 0)
            floatInput!!.clearFocus()
            val lp = layoutParams
            if (lp != null) {
                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager!!.updateViewLayout(floatView, lp)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ========== 关闭确认弹窗 ==========
    private fun showCloseConfirmDialog() {
        if (closeConfirmView != null) return
        releaseInputFocusAndRestore()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundDrawable(c.surface, dp(16f))
            val pad = dp(16)
            setPadding(pad, pad, pad, pad)
        }

        val title = TextView(this).apply {
            text = "关闭悬浮窗"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(c.onSurface)
        }
        root.addView(title)

        val msg = TextView(this).apply {
            text = "确认要关闭悬浮窗吗？"
            textSize = 13f
            setTextColor(c.textSecondary)
        }
        val msgLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        msgLp.topMargin = dp(8)
        root.addView(msg, msgLp)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.RIGHT
        }
        val rowLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        rowLp.topMargin = dp(16)
        root.addView(btnRow, rowLp)

        val cancelBtn = Button(this).apply {
            text = "取消"
            textSize = 13f
            setTextColor(c.primary)
            background = ripple(0x26000000.toInt(), createRoundDrawable(c.surfaceVariant, dp(8f)), 8)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { dismissCloseConfirmDialog() }
        }
        btnRow.addView(cancelBtn)

        val okBtn = Button(this).apply {
            text = "确认"
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            background = ripple(0x33FFFFFF.toInt(), createRoundDrawable(c.primary, dp(8f)), 8)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener {
                dismissCloseConfirmDialog()
                stopSelf()
            }
        }
        val okLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        okLp.leftMargin = dp(8)
        btnRow.addView(okBtn, okLp)

        closeConfirmLp = WindowManager.LayoutParams(
            dp(240), ViewGroup.LayoutParams.WRAP_CONTENT,
            getLayoutType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        closeConfirmView = root
        windowManager!!.addView(closeConfirmView, closeConfirmLp)
    }

    private fun dismissCloseConfirmDialog() {
        try {
            val v = closeConfirmView
            val wm = windowManager
            if (v != null && wm != null) {
                wm.removeView(v)
            }
        } catch (_: Exception) {
        }
        closeConfirmView = null
        closeConfirmLp = null
    }

    // ========== 工具 ==========
    private fun getLayoutType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun createRoundDrawable(color: Int, radius: Int, stroke: Int = 0): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setCornerRadius(radius.toFloat())
            setColor(color)
            if (stroke != 0) setStroke(dp(1), stroke)
        }
    }

    /** 主色渐变（左浅右深） */
    private fun createGradient(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(lighten(color, 0.25f), color)
        ).apply {
            cornerRadius = radius.toFloat()
        }
    }

    /** 按压水波反馈（RippleDrawable + 圆角 mask，content 正常绘制不会丢背景） */
    private fun ripple(overlay: Int, bg: GradientDrawable, radius: Int): RippleDrawable {
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setCornerRadius(dp(radius).toFloat())
            setColor(0xFFFFFFFF.toInt())
        }
        return RippleDrawable(ColorStateList.valueOf(overlay), bg, mask)
    }

    private fun lighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt()
        val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt()
        val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt()
        return Color.rgb(r, g, b)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun dp(value: Float): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
