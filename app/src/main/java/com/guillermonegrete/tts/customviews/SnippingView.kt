/*
* -------------------------Reference: https://stackoverflow.com/questions/8974088/how-to-create-a-resizable-rectangle-with-user-touch-events-on-android
* */

package com.guillermonegrete.tts.customviews

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guillermonegrete.tts.R
import kotlin.math.sqrt

class SnippingView : View {

    /**
     * point1 and point 3 are of same group and same as point 2 and point4
     */
    private var groupId = -1
    private var colorBalls = arrayListOf<ColorBall>()
    private var pressedBallID = 0
    private var paint = Paint()

    private var wParent: Int = 0
    private var hParent: Int = 0
    
    private var snipLeft: Int = 0
    private var snipTop: Int = 0
    private var snipRight: Int = 0
    private var snipBottom: Int = 0

    private var isDragging = false
    
    private var bitmaps = arrayOf(
        R.drawable.corner_topleft,
        R.drawable.corner_bottomleft,
        R.drawable.corner_bottomright,
        R.drawable.corner_topright)

    val snipRectangle: Rect
        get() = Rect(snipLeft, snipTop, snipRight, snipBottom)

    init {
        isFocusable = true
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private fun setBitmaps(statusBarHeight: Int) {

        val firstBitmap = BitmapFactory.decodeResource(context.resources, bitmaps.first())
        val bitmapWidth = firstBitmap.width
        val bitmapHeight = firstBitmap.height

        val cornerPoints = arrayListOf<Point>().apply {
            add(Point(0, statusBarHeight)) // top left
            add(Point(0, hParent - bitmapHeight)) // bottom left
            add(Point(wParent - bitmapWidth, hParent - bitmapHeight)) // bottom right
            add(Point(wParent - bitmapWidth, statusBarHeight)) // top right
        }

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
                updateBottom(hParent)
                invalidate()
            }
            return
        }

        val sizes = getWindowSize()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
                val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                val statusBarHeight = statusBarInsets.top

                val statusBarVisible = insets.isVisible(WindowInsetsCompat.Type.statusBars())
                if (!statusBarVisible ||
                    statusBarHeight > 0) { // sometimes the status bar is visible but the height is 0, ignore those insets until the height has a value

                    // Recalculate height because nav size might have changed
                    // Don't use the nav height from the insets because it always return 0 when using an overlay layout
                    val navBarVisible = insets.isVisible(WindowInsetsCompat.Type.navigationBars())
                    val navSize = if (navBarVisible) sizes.navHeight else 0
                    hParent = sizes.height - navSize

                    if (colorBalls.isEmpty()) {
                        snipTop = statusBarHeight
                        snipRight = wParent
                        snipBottom = hParent
                        setBitmaps(statusBarHeight)
                    }
                }

                insets
            }
        } else {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0

            snipTop = statusBarHeight
            snipRight = wParent
            snipBottom = hParent

            setBitmaps(statusBarHeight)
        }
    }

    private fun getWindowSize(): Sizes {

        val sizes: Sizes

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = wm.currentWindowMetrics
            val windowInsets = windowMetrics.windowInsets

            val insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout())
            val insetsWidth = insets.right + insets.left
            val insetsHeight = insets.bottom // Ignore top inset because the app draws over it

            val b = windowMetrics.bounds
            wParent = b.width() - insetsWidth
            hParent = b.height() - insetsHeight

            sizes = Sizes(b.width(), b.height(), insets.bottom)
        } else {
            wParent = context.resources.displayMetrics.widthPixels
            hParent = context.resources.displayMetrics.heightPixels

            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            val navHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            sizes = Sizes(wParent, hParent, navHeight)
        }

        return sizes
    }

    data class Sizes(val width: Int, val height: Int, val navHeight: Int)

    override fun onDraw(canvas: Canvas) {

        paint.apply {
            isAntiAlias = true
            isDither = true
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 5f
        }

        // Draw snipping rectangle border
        paint.apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#AA000000")
            strokeWidth = 2f
        }
        canvas.drawRect(snipLeft.toFloat(), snipTop.toFloat(), snipRight.toFloat(), snipBottom.toFloat(), paint)

        // Fill rectangle
        paint.apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#55DB1255")
            strokeWidth = 0f
        }
        canvas.drawRect(snipLeft.toFloat(), snipTop.toFloat(), snipRight.toFloat(), snipBottom.toFloat(), paint)

        //draw the corners
        // draw the balls on the canvas
        paint.color = Color.BLUE
        paint.textSize = 18f
        paint.strokeWidth = 0f
        for (ball in colorBalls) {
            canvas.drawBitmap(
                ball.bitmap, ball.left.toFloat(), ball.top.toFloat(),
                paint
            )
//            drawCornerBorder(canvas, Rect(point.x, point.y, point.x + wBall, point.y))
//            canvas.drawText("" + (i+1), point.x.toFloat(), point.y.toFloat(), paint)
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

                    val colorBall = colorBalls[pressedBallID]
                    val halfWidth = colorBall.width / 2
                    val halfHeight = colorBall.height / 2

                    if(pressedBallID > 1){ // Right points
                        val minLeft = colorBalls[0].centerX
                        val maxLeft =  wParent - halfWidth
                        x = ensureRange(x, minLeft, maxLeft)
                        snipRight = x + halfWidth
                    }else{ // Left points
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

                    colorBalls[pressedBallID].left = x - colorBall.width / 2
                    colorBalls[pressedBallID].top = y - colorBall.height / 2

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

    /**
     * Updates the position of the snip bottom along with the bottom corners.
     */
    private fun updateBottom(bottomPos: Int) {
        snipBottom = bottomPos
        colorBalls[1].top = bottomPos - colorBalls[1].height
        colorBalls[2].top = bottomPos - colorBalls[2].height
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