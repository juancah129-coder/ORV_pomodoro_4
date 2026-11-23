package com.example.orvpomodoro.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.orvpomodoro.MainActivity
import com.example.orvpomodoro.ui.components.MiniOverlayContent
import kotlin.math.hypot

/** Compose necesita un "ciclo de vida" para funcionar; una ventana flotante no trae uno. */
internal class OverlayLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun start() {
        savedStateController.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        registry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}

class MiniOverlay(private val context: Context) {

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = context.getSharedPreferences("mini_overlay", Context.MODE_PRIVATE)
    private val dm = context.resources.displayMetrics
    private fun dp(value: Int) = (value * dm.density).toInt()

    private val widthPx = dp(132)
    private val heightPx = dp(92)
    private val margin = dp(12)
    private val topInset = dp(56)      // por debajo de la barra de estado
    private val bottomInset = dp(80)   // por encima de la barra de gestos
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var view: ComposeView? = null
    private var owner: OverlayLifecycleOwner? = null
    private var snapAnimator: ValueAnimator? = null

    private fun cornerX(right: Boolean) = if (right) dm.widthPixels - widthPx - margin else margin
    private fun cornerY(bottom: Boolean) = if (bottom) dm.heightPixels - heightPx - bottomInset else topInset

    @Suppress("DEPRECATION")
    fun show() {
        if (view != null) return

        val lifecycleOwner = OverlayLifecycleOwner().also { it.start() }
        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = cornerX(prefs.getBoolean(KEY_RIGHT, true))     // por defecto: arriba a la derecha
            y = cornerY(prefs.getBoolean(KEY_BOTTOM, false))
        }

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent { MiniOverlayContent() }
            setOnTouchListener(dragListener(params))
        }

        wm.addView(composeView, params)
        view = composeView
        owner = lifecycleOwner
    }

    fun hide() {
        snapAnimator?.cancel()
        view?.let { runCatching { wm.removeView(it) } }
        owner?.stop()
        view = null
        owner = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun dragListener(params: WindowManager.LayoutParams) = object : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var downX = 0f
        private var downY = 0f
        private var dragging = false

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
                    startX = params.x
                    startY = params.y
                    downX = e.rawX
                    downY = e.rawY
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && hypot(dx, dy) > touchSlop) dragging = true
                    if (dragging) {
                        params.x = (startX + dx).toInt()
                        params.y = (startY + dy).toInt()
                        wm.updateViewLayout(v, params)
                    }
                }
                MotionEvent.ACTION_UP -> if (dragging) snapToCorner(v, params) else openApp()
                MotionEvent.ACTION_CANCEL -> if (dragging) snapToCorner(v, params)
            }
            return true
        }
    }

    /** Al soltar, la ventana se acomoda en la esquina más cercana y la recuerda. */
    private fun snapToCorner(v: View, params: WindowManager.LayoutParams) {
        val right = params.x + widthPx / 2 > dm.widthPixels / 2
        val bottom = params.y + heightPx / 2 > dm.heightPixels / 2
        prefs.edit().putBoolean(KEY_RIGHT, right).putBoolean(KEY_BOTTOM, bottom).apply()

        val fromX = params.x
        val fromY = params.y
        val toX = cornerX(right)
        val toY = cornerY(bottom)

        snapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                if (!v.isAttachedToWindow) return@addUpdateListener
                val f = it.animatedValue as Float
                params.x = (fromX + (toX - fromX) * f).toInt()
                params.y = (fromY + (toY - fromY) * f).toInt()
                wm.updateViewLayout(v, params)
            }
            start()
        }
    }

    private fun openApp() {
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
    }

    private companion object {
        const val KEY_RIGHT = "right"
        const val KEY_BOTTOM = "bottom"
    }
}