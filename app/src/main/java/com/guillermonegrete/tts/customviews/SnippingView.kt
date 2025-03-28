/*
* -------------------------Reference: https://stackoverflow.com/questions/8974088/how-to-create-a-resizable-rectangle-with-user-touch-events-on-android
* */

package com.guillermonegrete.tts.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guillermonegrete.tts.R
import timber.log.Timber
import kotlin.math.sqrt

class SnippingView : View {

    /**
     * point1 and point 3 are of same group and same as point 2 and point4
     */
    private var groupId = -1
    private var colorBalls = arrayListOf<ColorBall>()
    private var pressedBallID = 0
    private var paint = Paint()
    private var fillPaint = Paint()

    private var wParent: Int = 0
    private var hParent: Int = 0
    
    private var snipLeft: Int = 0
    private var snipTop: Int = 0
    private var snipRight: Int = 0
    private var snipBottom: Int = 0

    private var isDragging = false
    private var hasInitialState = true
    
    private var bitmaps = arrayOf(
        R.drawable.corner_topleft,
        R.drawable.corner_bottomleft,
        R.drawable.corner_bottomright,
        R.drawable.corner_topright)

    val snipRectangle: Rect
        get() = Rect(snipLeft, snipTop, snipRight, snipBottom)

    init {
        isFocusable = true
        paint.apply {
            isAntiAlias = true
            isDither = true
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
            color = Color.parseColor("#AA000000")
            strokeWidth = 2f
        }

        fillPaint.apply {
            isAntiAlias = true
            isDither = true
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.FILL
            color = Color.parseColor("#55DB1255")
            strokeWidth = 0f
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        val sizes = getWindowSize()
        Timber.d(sizes.toString())
        if (!hasInitialState) {
            ensureSnipViewFitsScreen()
            updateBottom()
            updateRight()
        }
        Timber.d(snipRectangle.toString())
    }

    private fun setBitmaps(insets: Insets) {

        val firstBitmap = BitmapFactory.decodeResource(context.resources, bitmaps.first())
        val bitmapWidth = firstBitmap.width
        val bitmapHeight = firstBitmap.height

        val cornerPoints = arrayListOf<Point>().apply {
            add(Point(insets.left, insets.top)) // top left
            add(Point(insets.left, hParent - bitmapHeight)) // bottom left
            add(Point(wParent - bitmapWidth - insets.right, hParent - bitmapHeight)) // bottom right
            add(Point(wParent - bitmapWidth - insets.right, insets.top)) // top right
        }

        colorBalls.clear()
        for ((j, point) in cornerPoints.withIndex()) {
            val bitmap = BitmapFactory.decodeResource(context.resources, bitmaps[j])
            colorBalls.add(ColorBall(bitmap, j, point))
        }

        invalidate()
    }

    fun prepareLayout() {

        if (colorBalls.isNotEmpty()) {
            // The usable window height may have shrunk, if that's the case adjust the bottom
            if (snipBottom > hParent) {
                updateBottom()
                invalidate()
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                if (!hasInitialState) return@setOnApplyWindowInsetsListener insets
                val sizes = getWindowSize()
                // Recalculate height because nav size might have changed
                // Don't use the nav height from the insets because it always return 0 when using an overlay layout
                val sysInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                Timber.d("All insets: $sysInsets")
                val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                val navBarVisible = insets.isVisible(WindowInsetsCompat.Type.navigationBars())
                Timber.d("Nav (vis: $navBarVisible) insets: $navInsets, orientation: ${context.display.rotation}")
                when (context.display.rotation) {
                    Surface.ROTATION_90, Surface.ROTATION_180 -> wParent = sizes.width - if (navBarVisible) sizes.navHeight else 0
                    else -> hParent = sizes.height - if (navBarVisible) sizes.navHeight else 0
                }
//                val navSize = if (navBarVisible) sizes.navHeight else 0
//                hParent = sizes.height - navSize

                val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                val statusBarHeight = statusBarInsets.top

                val statusBarVisible = insets.isVisible(WindowInsetsCompat.Type.statusBars())
                Timber.d("Status bar (vis: $statusBarVisible) insets: $statusBarInsets")
                snipTop = sysInsets.top
                snipRight = wParent - sysInsets.right
                snipBottom = hParent
                snipLeft = sysInsets.left
                setBitmaps(sysInsets)

                insets
            }
        } else {
            val sizes = getWindowSize()
            val statusBarHeight = sizes.statusHeight

            snipTop = statusBarHeight
            snipRight = wParent
            snipBottom = hParent

            setBitmaps(Insets.of(0, statusBarHeight, 0, 0))
        }
    }

    fun removeInsetsListener() {
        ViewCompat.setOnApplyWindowInsetsListener(this, null)
    }

    private fun getWindowSize(): Sizes {

        val sizes: Sizes

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = wm.currentWindowMetrics
            val windowInsets = windowMetrics.windowInsets

            val insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val navBar = windowInsets.getInsets(WindowInsets.Type.navigationBars())
            Timber.d("Nav bar only: $navBar")
            val insetsHeight = insets.bottom // Ignore top inset because the app draws over it

            val b = windowMetrics.bounds
            wParent = b.width()
            hParent = b.height() - insetsHeight

            sizes = Sizes(b.width(), b.height(), insets.top, insets.bottom)
        } else {
            wParent = context.resources.displayMetrics.widthPixels
            hParent = context.resources.displayMetrics.heightPixels

            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            val navHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            val statusResourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (statusResourceId > 0) resources.getDimensionPixelSize(statusResourceId) else 0
            sizes = Sizes(wParent, hParent, statusBarHeight, navHeight)
        }

        return sizes
    }

    data class Sizes(val width: Int, val height: Int, val statusHeight: Int, val navHeight: Int)

    override fun onDraw(canvas: Canvas) {
        // Draw snipping rectangle border
        canvas.drawRect(snipLeft.toFloat(), snipTop.toFloat(), snipRight.toFloat(), snipBottom.toFloat(), paint)

        // Fill rectangle
        canvas.drawRect(snipLeft.toFloat(), snipTop.toFloat(), snipRight.toFloat(), snipBottom.toFloat(), fillPaint)

        //draw the corners
        // draw the balls on the canvas
        for (ball in colorBalls) {
            canvas.drawBitmap(ball.bitmap, ball.left.toFloat(), ball.top.toFloat(), null)
        }
    }

    private var initialX: Int = 0
    private var initialY: Int = 0

    private var snipWidth = 0
    private var snipHeight = 0


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        var x = event.x.toInt()
        var y = event.y.toInt()

        when (event.action) {

            MotionEvent.ACTION_DOWN -> {

                pressedBallID = -1
                groupId = -1
                for (ball in colorBalls.reversed()) {
                    // check if inside the bounds of the ball (circle)
                    // get the center for the ball
                    val centerX = ball.centerX
                    val centerY = ball.centerY

                    // Calculate the radius from the touch to the center of the ball
                    val radiusCircle = sqrt(((centerX - x) * (centerX - x) + (centerY - y) * (centerY - y)).toDouble())

                    if (radiusCircle < ball.width) {

                        pressedBallID = ball.id
                        groupId = if (pressedBallID == 1 || pressedBallID == 3) 2 else 1
                        invalidate()
                        break
                    }
                    invalidate()
                }

                if(snipRectangle.contains(x, y)){
                    isDragging = true
                    initialX = x
                    initialY = y

                    snipWidth = snipRectangle.width()
                    snipHeight = snipRectangle.height()
                }
            }

            MotionEvent.ACTION_MOVE ->

                if (pressedBallID > -1) {
                    hasInitialState = false

                    val colorBall = colorBalls[pressedBallID]
                    val halfWidth = colorBall.halfWidth
                    val halfHeight = colorBall.halfHeight

                    if (pressedBallID > 1) { // Right points
                        val minLeft = colorBalls[0].centerX
                        val maxLeft =  wParent - halfWidth
                        x = ensureRange(x, minLeft, maxLeft)
                        snipRight = x + halfWidth
                    } else { // Left points
                        val maxLeft = colorBalls[2].centerX
                        x = ensureRange(x, halfWidth, maxLeft)
                        snipLeft = x - halfWidth
                    }

                    when(pressedBallID){
                        0, 3 -> { // Top points
                            val maxTop = colorBalls[1].centerY
                            y = ensureRange(y, halfHeight, maxTop)
                            snipTop = y - halfHeight
                        }
                        else ->{ // Bottom points
                            val minTop = colorBalls[0].centerY
                            val maxTop = hParent - halfHeight
                            y = ensureRange(y, minTop, maxTop)
                            snipBottom = y + halfHeight
                        }
                    }

                    colorBalls[pressedBallID].left = x - colorBall.halfWidth
                    colorBalls[pressedBallID].top = y - colorBall.halfHeight

                    if (groupId == 1) {

                        colorBalls[1].left = colorBalls[0].left
                        colorBalls[1].top = colorBalls[2].top
                        colorBalls[3].left = colorBalls[2].left
                        colorBalls[3].top = colorBalls[0].top
                    } else {

                        colorBalls[0].left = colorBalls[1].left
                        colorBalls[0].top = colorBalls[3].top
                        colorBalls[2].left = colorBalls[3].left
                        colorBalls[2].top = colorBalls[1].top
                    }

                    invalidate()
                } else if (isDragging){
                    hasInitialState = false
                    val deltaX = x - initialX
                    val deltaY = y - initialY
                    initialX = x
                    initialY = y

                    snipLeft = ensureRange(snipLeft + deltaX, 0, wParent - snipWidth)
                    snipRight = snipLeft + snipWidth
                    snipTop = ensureRange(snipTop + deltaY, 0, hParent - snipHeight)
                    snipBottom = snipTop + snipHeight

                    colorBalls[0].left = snipLeft
                    colorBalls[0].top = snipTop
                    colorBalls[1].left = snipLeft
                    colorBalls[1].top = snipBottom - colorBalls[1].height
                    colorBalls[2].left = snipRight - colorBalls[2].width
                    colorBalls[2].top = snipBottom - colorBalls[2].height
                    colorBalls[3].left = snipRight - colorBalls[3].width
                    colorBalls[3].top = snipTop

                    invalidate()
                }

            MotionEvent.ACTION_UP -> {isDragging = false}
        }
        invalidate()
        return true

    }

    private fun ensureSnipViewFitsScreen() {
        if (snipTop >= hParent) {
            snipTop = 0
            colorBalls[0].top = snipTop
            colorBalls[3].top = snipTop
        }

        if (snipLeft >= wParent) {
            snipLeft = 0
            colorBalls[0].left = snipLeft
            colorBalls[1].left = snipLeft
        }
    }

    /**
     * Updates the position of the snip bottom along with the bottom corners.
     */
    private fun updateBottom() {
        if (snipBottom > hParent) {
            snipBottom = hParent
            colorBalls[1].top = snipBottom - colorBalls[1].height
            colorBalls[2].top = snipBottom - colorBalls[2].height
        }
    }

    private fun updateRight() {
        if (snipRight > wParent) {
            snipRight = wParent
            colorBalls[2].left = snipRight - colorBalls[2].width
            colorBalls[3].left = snipRight - colorBalls[2].width
        }
    }

    private fun ensureRange(value: Int, min: Int, max: Int) = minOf(maxOf(value, min), max)


    // Add top left right bottom member variables
    class ColorBall internal constructor(
        internal val bitmap: Bitmap,
        internal val id: Int,
        pointTopLeft: Point
    ) {

        internal val width = bitmap.width
        internal val height = bitmap.height

        val halfWidth = width / 2
        val halfHeight = height / 2

        var left = pointTopLeft.x
        var top = pointTopLeft.y

        val centerX : Int
            get() = left + width / 2
        val centerY : Int
            get() = top + height / 2

        val right get() = left + width
        val bottom get() = top + height

    }
}