/*
 * Modern UI.
 * Copyright (C) 2019-2024 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package calebxzhou.rdi.ui2

import calebxzhou.rdi.lgr
import calebxzhou.rdi.logMarker
import icyllis.arc3d.core.ColorInfo
import icyllis.arc3d.core.ColorSpace
import icyllis.arc3d.core.ImageInfo
import icyllis.arc3d.core.RefCnt
import icyllis.arc3d.engine.Engine
import icyllis.arc3d.engine.ImmediateContext
import icyllis.arc3d.granite.GraniteSurface
import icyllis.arc3d.granite.Recording
import icyllis.arc3d.sketch.Surface
import icyllis.modernui.ModernUI
import icyllis.modernui.R
import icyllis.modernui.annotation.MainThread
import icyllis.modernui.annotation.UiThread
import icyllis.modernui.app.Activity
import icyllis.modernui.core.ActivityWindow
import icyllis.modernui.core.Core
import icyllis.modernui.core.Handler
import icyllis.modernui.core.Looper
import icyllis.modernui.core.Monitor
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.fragment.FragmentContainerView
import icyllis.modernui.fragment.FragmentController
import icyllis.modernui.fragment.FragmentHostCallback
import icyllis.modernui.fragment.FragmentTransaction
import icyllis.modernui.fragment.OnBackPressedDispatcher
import icyllis.modernui.fragment.OnBackPressedDispatcherOwner
import icyllis.modernui.graphics.BitmapFactory
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Image
import icyllis.modernui.graphics.LightingInfo
import icyllis.modernui.graphics.Rect
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.graphics.pipeline.ArcCanvas
import icyllis.modernui.graphics.text.FontFamily
import icyllis.modernui.lifecycle.Lifecycle
import icyllis.modernui.lifecycle.LifecycleOwner
import icyllis.modernui.lifecycle.LifecycleRegistry
import icyllis.modernui.lifecycle.ViewModelStore
import icyllis.modernui.lifecycle.ViewModelStoreOwner
import icyllis.modernui.resources.ResourceId
import icyllis.modernui.resources.Resources
import icyllis.modernui.resources.ResourcesBuilder
import icyllis.modernui.resources.SystemTheme
import icyllis.modernui.resources.TypedValue
import icyllis.modernui.text.Typeface
import icyllis.modernui.util.DisplayMetrics
import icyllis.modernui.view.KeyEvent
import icyllis.modernui.view.MotionEvent
import icyllis.modernui.view.PointerIcon
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewRoot
import icyllis.modernui.view.WindowGroup
import icyllis.modernui.view.WindowManager
import icyllis.modernui.view.menu.ContextMenuBuilder
import icyllis.modernui.view.menu.MenuHelper
import icyllis.modernui.widget.TextView
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33C
import org.lwjgl.system.Configuration
import org.lwjgl.system.Platform
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.util.LinkedHashSet
import java.util.Locale
import java.util.Objects
import java.util.Properties
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.LongConsumer
import kotlin.jvm.Volatile
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.system.exitProcess

class RodernUI : ModernUI(), AutoCloseable, LifecycleOwner, ViewModelStoreOwner, OnBackPressedDispatcherOwner {
    val fontsDir = "./resourcepacks/fonts/assets/modernui/font/"
    private val marker = logMarker("ui")

    companion object {
        const val ID = "modernui"
        const val NAME_CPT = "ModernUI"

        @JvmField
        val props: Properties = Properties()

        @Volatile
        private var instance: RodernUI? = null

        private const val FRAGMENT_CONTAINER = 0x01020007

        init {
            if (Runtime.version().feature() < 17) {
                throw RuntimeException("JRE 17 or above is required")
            }
        }

        @JvmStatic
        fun getInstance(): RodernUI = instance!!

    }

    private lateinit var window: ActivityWindow
    private lateinit var viewRoot: ViewRootImpl
    private lateinit var decor: WindowGroup
    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var onBackPressedDispatcher: OnBackPressedDispatcher
    private lateinit var viewModelStore: ViewModelStore
    lateinit var fragmentController: FragmentController

    private var defaultTypeface: Typeface? = null

    @Volatile
    private var renderThread: Thread? = null

    @Volatile
    private var renderLooper: Looper? = null

    @Volatile
    private var renderHandler: Handler? = null

    private val themeLock = Any()

    private val resourcesInternal: Resources
    private var theme: Resources.Theme? = null
    private var themeResource: ResourceId? = null

    private var backgroundImage: Image? = null

    private val renderLock = java.lang.Object()
    private val stopRequested = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    init {
        synchronized(RodernUI::class.java) {
            if (instance == null) {
                instance = this
                ModernUI::class.java.getDeclaredField("sInstance").apply { isAccessible = true }.set(null, this)
            } else {
                throw RuntimeException("Multiple instances")
            }
        }
        val builder = ResourcesBuilder()
        SystemTheme.addToResources(builder)
        resourcesInternal = builder.build()
    }

    @MainThread
    override fun run(fragment: Fragment) {
        run(fragment, null)
    }

    @TestOnly
    @MainThread
    override fun run(fragment: Fragment, windowCallback: LongConsumer?) {
        Thread.currentThread().name = "Main-Thread"

        Core.initialize()

        lgr.debug(marker, "Preparing main thread")
        Looper.prepareMainLooper()

        val loadTypefaceFuture = CompletableFuture.runAsync { loadDefaultTypeface() }

        lgr.debug(marker, "Initializing window system")
        val monitor = Monitor.getPrimary()

        Configuration.OPENGL_LIBRARY_NAME.get()?.let { name ->
            lgr.debug(marker, "OpenGL library: {}", name)
            Objects.requireNonNull(GL.getFunctionProvider(), "Implicit OpenGL loading is required")
        }

        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API)
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE)
        glfwWindowHint(GLFW_DEPTH_BITS, 0)
        glfwWindowHint(GLFW_STENCIL_BITS, 0)
        glfwWindowHintString(GLFW_X11_CLASS_NAME, NAME_CPT)
        glfwWindowHintString(GLFW_X11_INSTANCE_NAME, NAME_CPT)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        if (Platform.get() == Platform.MACOSX) {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1)
        } else {
            findHighestGLVersion()
        }

        window = if (monitor == null) {
            lgr.info(marker, "No monitor connected")
            ActivityWindow.createMainWindow("Modern UI", 1280, 720)
        } else {
            val mode = monitor.currentMode
            ActivityWindow.createMainWindow(
                "Modern UI",
                (mode.width * 0.75).toInt(),
                (mode.height * 0.75).toInt()
            )
        }

        val latch = CountDownLatch(1)

        lgr.debug(marker, "Preparing render thread")
        renderThread = Thread({ runRender(latch) }, "Render-Thread").also { it.start() }

        windowCallback?.accept(window.handle)

        CompletableFuture.supplyAsync {
            try {
                arrayOf(
                    BitmapFactory.decodeStream(getResourceStream(ID, "AppLogo16x.png")),
                    BitmapFactory.decodeStream(getResourceStream(ID, "AppLogo32x.png")),
                    BitmapFactory.decodeStream(getResourceStream(ID, "AppLogo48x.png"))
                )
            } catch (t: Throwable) {
                lgr.info(marker, "Failed to load window icons", t)
                null
            }
        }.thenAcceptAsync({ icons ->
            icons?.let { window.setIcon(*it) }
        }, Core.getMainThreadExecutor())

        monitor?.let { primary ->
            window.center(primary)
            val physWidth = IntArray(1)
            val physHeight = IntArray(1)
            glfwGetMonitorPhysicalSize(primary.handle, physWidth, physHeight)
            val xscale = FloatArray(1)
            val yscale = FloatArray(1)
            glfwGetMonitorContentScale(primary.handle, xscale, yscale)
            val metrics = DisplayMetrics().apply { setToDefaults() }
            metrics.widthPixels = window.width
            metrics.heightPixels = window.height
            val mode = primary.currentMode
            metrics.xdpi = 25.4f * mode.width / physWidth[0]
            metrics.ydpi = 25.4f * mode.height / physHeight[0]
            lgr.info(
                marker,
                "Primary monitor physical size: {}x{} mm, xScale: {}, yScale: {}",
                physWidth[0],
                physHeight[0],
                xscale[0],
                yscale[0]
            )
            val densityBase = (DisplayMetrics.DENSITY_DEFAULT * xscale[0] / 12f).roundToInt() * 12
            metrics.density = densityBase * DisplayMetrics.DENSITY_DEFAULT_SCALE
            metrics.densityDpi = densityBase
            metrics.scaledDensity = metrics.density
            lgr.info(marker, "Display metrics: {}", metrics)
            resourcesInternal.updateMetrics(metrics)
        }

        glfwSetWindowCloseCallback(window.handle) {
            lgr.debug(marker, "Window closed from callback")
            window.setShouldClose(true)
            stop()
        }

        try {
            latch.await()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException(e)
        }

        lgr.debug(marker, "Initializing UI system")

        Core.initUiThread()

        viewRoot = ViewRootImpl().apply {
            loadSystemProperties { java.lang.Boolean.getBoolean("icyllis.modernui.display.debug.layout") }
        }

        decor = WindowGroup(this).apply {
            setWillNotDraw(true)
            id = R.id.content
        }

        val typedValue = TypedValue()
        getTheme().resolveAttribute(R.ns, R.attr.colorSurfaceContainerLowest, typedValue, true)
        decor.setBackground(ColorDrawable(typedValue.data))

        fragmentContainerView = FragmentContainerView(this).apply {
            layoutParams = WindowManager.LayoutParams()
            setWillNotDraw(true)
            id = FRAGMENT_CONTAINER
        }

        decor.addView(fragmentContainerView)
        decor.layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        decor.setIsRootNamespace(true)

        viewRoot.view = decor

        lgr.debug(marker, "Installing view protocol")
        window.install(viewRoot)

        lifecycleRegistry = LifecycleRegistry(this)
        onBackPressedDispatcher = OnBackPressedDispatcher {
            window.setShouldClose(true)
            stop()
        }
        viewModelStore = ViewModelStore()
        fragmentController = FragmentController.createController(HostCallbacks())
        fragmentController.attachHost(null)

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fragmentController.dispatchCreate()

        fragmentController.dispatchActivityCreated()
        fragmentController.execPendingActions()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        fragmentController.dispatchStart()

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fragmentController.dispatchResume()

        fragmentController.fragmentManager.beginTransaction()
            .add(FRAGMENT_CONTAINER, fragment, "main")
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .addToBackStack("main")
            .commit()

        window.show()

        loadTypefaceFuture.join()

        lgr.info(marker, "Looping main thread")
        try {
            Looper.loop()
        }catch (e: Exception){e.printStackTrace()} finally {
            viewRoot.surface = RefCnt.move(viewRoot.surface)
            Core.requireUiRecordingContext().unref()
            close()
            lgr.info(marker, "Quited main thread")
            exitProcess(0)
        }
    }

    private fun findHighestGLVersion() {
        val previousCallback = glfwSetErrorCallback(null)
        val versions = arrayOf(
            intArrayOf(4, 6), intArrayOf(4, 5), intArrayOf(4, 4), intArrayOf(4, 3),
            intArrayOf(4, 2), intArrayOf(4, 1), intArrayOf(4, 0), intArrayOf(3, 3)
        )
        var tempWindow = 0L
        try {
            for ((major, minor) in versions) {
                glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, major)
                glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, minor)
                lgr.debug(marker, "Trying OpenGL {}.{}", major, minor)
                tempWindow = glfwCreateWindow(640, 480, "System Testing", 0, 0)
                if (tempWindow != 0L) {
                    lgr.info(marker, "Will use OpenGL {}.{} Core Profile", major, minor)
                    return
                }
            }
            throw RuntimeException("OpenGL 3.3 or OpenGL ES 3.0 is required")
        } catch (_: Exception) {
            throw RuntimeException("OpenGL 3.3 or OpenGL ES 3.0 is required")
        } finally {
            if (tempWindow != 0L) {
                glfwDestroyWindow(tempWindow)
            }
            glfwSetErrorCallback(previousCallback)?.free()
        }
    }

    private fun runRender(latch: CountDownLatch) {
        val renderWindow = window
        renderWindow.makeCurrent()
        try {
            if (!Core.initOpenGL()) {
                Core.glShowCapsErrorDialog()
                throw IllegalStateException("Failed to initialize OpenGL")
            }
            renderLooper = Looper.prepare()
            renderHandler = Handler(renderLooper!!)
            Core.glSetupDebugCallback()
        } finally {
            latch.countDown()
        }

        renderWindow.swapInterval(1)
        lgr.info(marker, "Looping render thread")

        try {
            Looper.loop()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        synchronized(Core::class.java) {
            backgroundImage?.close()
            backgroundImage = null
        }

        Core.requireImmediateContext().unref()
        lgr.info(marker, "Quited render thread")
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    override fun getResources(): Resources = resourcesInternal

    override fun setTheme(resId: ResourceId?) {
        synchronized(themeLock) {
            themeResource = resId
            val currentTheme = theme ?: return
            currentTheme.clear()
            themeResource = Resources.selectDefaultTheme(themeResource)
            currentTheme.applyStyle(themeResource, true)
        }
    }

    override fun getTheme(): Resources.Theme {
        synchronized(themeLock) {
            theme?.let { return it }
            val newTheme = resourcesInternal.newTheme()
            theme = newTheme
            themeResource = Resources.selectDefaultTheme(themeResource)
            newTheme.applyStyle(themeResource, true)
            return newTheme
        }
    }

    override fun onGetSelectedLocale(): Locale = Locale.getDefault()

    override fun onGetSelectedTypeface(): Typeface = defaultTypeface ?: Typeface.SANS_SERIF

    override fun hasRtlSupport(): Boolean = true

    @ApiStatus.Experimental
    @Throws(IOException::class)
    override fun getResourceStream(namespace: String, path: String): InputStream {
        return RodernUI::class.java.getResourceAsStream("/assets/$namespace/$path")
            ?: throw FileNotFoundException()
    }

    @ApiStatus.Experimental
    @Throws(IOException::class)
    override fun getResourceChannel(namespace: String, path: String): ReadableByteChannel {
        return Channels.newChannel(getResourceStream(namespace, path))
    }

    override fun getWindowManager(): WindowManager = decor

    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        try {
            renderLooper?.quit()
            synchronized(renderLock) {
                renderLock.notifyAll()
            }
            renderThread?.let { thread ->
                if (thread.isAlive) {
                    try {
                        thread.join(1000)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }
            if (::window.isInitialized) {
                window.close()
                lgr.debug(marker, "Closed main window")
            }
            glfwSetMonitorCallback(null)?.free()
        } finally {
            Core.terminate()
        }
    }

    override fun getViewModelStore(): ViewModelStore = viewModelStore

    override fun getOnBackPressedDispatcher(): OnBackPressedDispatcher = onBackPressedDispatcher

    private fun loadDefaultTypeface() {
        //首选字体的文件名是1开头
        val tfs= File(fontsDir).listFiles { it.extension.matches(Regex("([to])tf")) }.map { file ->
            FontFamily.createFamily(file, true)
        }
        defaultTypeface = Typeface.createTypeface(*tfs.toTypedArray())

    }

    private fun stop() {
        if (!stopRequested.compareAndSet(false, true)) {
            return
        }
        decor.setBackground(null)
        fragmentController.dispatchStop()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

        fragmentController.dispatchDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        Looper.getMainLooper().quitSafely()
    }

    @UiThread
    inner class ViewRootImpl : ViewRoot() {
        private val globalRect = Rect()
        var surface: Surface? = null
        var lastFrameTask: Recording? = null

        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val focused = mView.findFocus()
                if (focused is TextView && focused.movementMethod != null) {
                    focused.getGlobalVisibleRect(globalRect)
                    if (!globalRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        focused.clearFocusInternal(null, true, false)
                    }
                }
            }
            return super.dispatchTouchEvent(event)
        }

        override fun onKeyEvent(event: KeyEvent) {
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEY_ESCAPE) {
                val focused = mView.findFocus()
                if (focused is TextView && focused.movementMethod != null) {
                    focused.clearFocusInternal(null, true, false)
                } else {
                    this@RodernUI.onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        override fun setFrame(width: Int, height: Int) {
            super.setFrame(width, height)
            val current = surface
            if (current == null || current.width != width || current.height != height) {
                if (width > 0 && height > 0) {
                    surface = RefCnt.move(
                        surface,
                        GraniteSurface.makeRenderTarget(
                            Core.requireUiRecordingContext(),
                            ImageInfo.make(
                                width,
                                height,
                                ColorInfo.CT_RGBA_8888,
                                ColorInfo.AT_PREMUL,
                                ColorSpace.get(ColorSpace.Named.SRGB)
                            ),
                            false,
                            Engine.SurfaceOrigin.kLowerLeft,
                            null
                        )
                    )

                    val metrics = this@RodernUI.resources.displayMetrics
                    val zRatio = min(width, height) / (450f * metrics.density)
                    val zWeightedAdjustment = (zRatio + 2f) / 3f
                    val lightZ =
                        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, 500f, metrics) * zWeightedAdjustment
                    val lightRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DP, 800f, metrics)
                    LightingInfo.setLightGeometry(width / 2f, 0f, lightZ, lightRadius)
                }
            }
        }

        override fun beginDrawLocked(width: Int, height: Int): Canvas? {
            val currentSurface = surface
            return if (currentSurface != null && width > 0 && height > 0) {
                ArcCanvas(currentSurface.canvas)
            } else {
                null
            }
        }

        override fun endDrawLocked(canvas: Canvas) {
            val recording = Core.requireUiRecordingContext().snap()
            synchronized(renderLock) {
                lastFrameTask?.close()
                lastFrameTask = recording
                renderHandler?.post { render() }
                try {
                    renderLock.wait()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                lastFrameTask?.close()
                lastFrameTask = null
            }
        }

        private fun render() {
            val context: ImmediateContext = Core.requireImmediateContext()
            val width: Int
            val height: Int
            val recording: Recording?
            synchronized(renderLock) {
                val currentSurface = surface ?: return
                width = currentSurface.width
                height = currentSurface.height
                recording = lastFrameTask
                lastFrameTask = null
                renderLock.notifyAll()
            }

            if (recording == null) {
                return
            }

            val added = context.addTask(recording)
            recording.close()
            if (added) {
                GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, 0)
                GL33C.glBlitFramebuffer(
                    0,
                    0,
                    width,
                    height,
                    0,
                    0,
                    width,
                    height,
                    GL33C.GL_COLOR_BUFFER_BIT,
                    GL33C.GL_NEAREST
                )
                context.submit()
                this@RodernUI.window.swapBuffers()
            } else {
                lgr.error(marker, "Failed to add draw commands")
            }
        }

        override fun playSoundEffect(effectId: Int) {
        }

        override fun performHapticFeedback(effectId: Int, always: Boolean): Boolean = false

        override fun applyPointerIcon(pointerType: Int) {
            Core.executeOnMainThread {
                glfwSetCursor(this@RodernUI.window.handle, PointerIcon.getSystemIcon(pointerType).handle)
            }
        }

        var contextMenu: ContextMenuBuilder? = null
        var contextMenuHelper: MenuHelper? = null

        override fun showContextMenuForChild(originalView: View, x: Float, y: Float): Boolean {
            contextMenuHelper?.dismiss()
            contextMenuHelper = null

            val menu = contextMenu ?: ContextMenuBuilder(this@RodernUI).also { contextMenu = it }
            menu.clearAll()

            val helper = if (!x.isNaN() && !y.isNaN()) {
                menu.showPopup(this@RodernUI, originalView, x, y)
            } else {
                menu.showPopup(this@RodernUI, originalView, 0f, 0f)
            }

            contextMenuHelper = helper
            return helper != null
        }
    }

    inner class HostCallbacks :
        FragmentHostCallback<Any?>(
            this@RodernUI,
            Handler(Looper.myLooper() ?: throw IllegalStateException("No UI looper"))
        ),
        ViewModelStoreOwner,
        OnBackPressedDispatcherOwner,
        LifecycleOwner {
        override fun onGetHost(): Any? = null

        override fun onFindViewById(id: Int): View? = this@RodernUI.decor.findViewById(id)

        override fun getViewModelStore(): ViewModelStore = this@RodernUI.viewModelStore

        override fun getOnBackPressedDispatcher(): OnBackPressedDispatcher = this@RodernUI.onBackPressedDispatcher

        override fun getLifecycle(): Lifecycle = this@RodernUI.lifecycleRegistry
    }
}