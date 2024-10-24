package com.guillermonegrete.tts.common.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewPropertyAnimator
import androidx.annotation.Dimension
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import com.google.android.material.R
import com.google.android.material.animation.AnimationUtils
import com.google.android.material.motion.MotionUtils
import java.util.LinkedHashSet

/**
 *
 */
internal class NestedHideViewOnScrollBehavior<V : View> : CoordinatorLayout.Behavior<V> {

    interface OnScrollStateChangedListener {
        /**
         * Called when the bottom view changes its scrolled state.
         *
         * @param bottomView The bottom view.
         * @param newState The new state. This will be one of [.STATE_SCROLLED_UP] or [     ][.STATE_SCROLLED_DOWN].
         */
        fun onStateChanged(bottomView: View, @ScrollState newState: Int)
    }

    private val onScrollStateChangedListeners = LinkedHashSet<OnScrollStateChangedListener>()

    private var enterAnimDuration = 0
    private var exitAnimDuration = 0
    private var enterAnimInterpolator: TimeInterpolator? = null
    private var exitAnimInterpolator: TimeInterpolator? = null

    private var height = 0

    /**
     * Positions the scroll state can be set to.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(STATE_SCROLLED_DOWN, STATE_SCROLLED_UP)
    annotation class ScrollState

    @ScrollState
    private var currentState = STATE_SCROLLED_UP
    private var additionalHiddenOffsetY = 0
    private var currentAnimator: ViewPropertyAnimator? = null

    constructor()

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    @SuppressLint("RestrictedApi")
    override fun onLayoutChild(
        parent: CoordinatorLayout, child: V, layoutDirection: Int
    ): Boolean {
        val paramsCompat =
            child.layoutParams as MarginLayoutParams
        height = child.measuredHeight + paramsCompat.bottomMargin
        enterAnimDuration =
            MotionUtils.resolveThemeDuration(
                child.context, ENTER_ANIM_DURATION_ATTR, DEFAULT_ENTER_ANIMATION_DURATION_MS
            )
        exitAnimDuration =
            MotionUtils.resolveThemeDuration(
                child.context, EXIT_ANIM_DURATION_ATTR, DEFAULT_EXIT_ANIMATION_DURATION_MS
            )
        enterAnimInterpolator =
            MotionUtils.resolveThemeInterpolator(
                child.context,
                ENTER_EXIT_ANIM_EASING_ATTR,
                AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
            )
        exitAnimInterpolator =
            MotionUtils.resolveThemeInterpolator(
                child.context,
                ENTER_EXIT_ANIM_EASING_ATTR,
                AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
            )
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    /**
     * Sets an additional offset for the y position used to hide the view.
     *
     * @param child the child view that is hidden by this behavior
     * @param offset the additional offset in pixels that should be added when the view slides away
     */
    fun setAdditionalHiddenOffsetY(child: V, @Dimension offset: Int) {
        additionalHiddenOffsetY = offset

        if (currentState == STATE_SCROLLED_DOWN) {
            child.translationY = (height + additionalHiddenOffsetY).toFloat()
        }
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int,
        type: Int
    ): Boolean {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int,
        consumed: IntArray
    ) {
        if (dyConsumed > 0) {
            slideDown(child)
        } else if (dyConsumed < 0) {
            slideUp(child)
        }
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        onNestedScroll(coordinatorLayout, child, target, dx, dy, consumed[0], consumed[1], type)
    }

    @Deprecated("")
    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        bottomNavigationView: V,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        if (currentState != STATE_SCROLLED_DOWN && dyConsumed > 0) {
            slideDown(bottomNavigationView)
        } else if (currentState != STATE_SCROLLED_UP && dyConsumed < 0) {
            slideUp(bottomNavigationView)
        }
    }

    /** Returns true if the current state is scrolled up.  */
    fun isScrolledUp(): Boolean {
        return currentState == STATE_SCROLLED_UP
    }

    /**
     * Slides the child with or without animation from its current position to be totally on the
     * screen.
     *
     * @param animate `true` to slide with animation.
     */
    @JvmOverloads
    fun slideUp(child: V, animate: Boolean = true) {
        if (isScrolledUp()) {
            return
        }

        if (currentAnimator != null) {
            currentAnimator!!.cancel()
            child.clearAnimation()
        }
        updateCurrentState(child, STATE_SCROLLED_UP)
        val targetTranslationY = 0
        if (animate) {
            animateChildTo(
                child,
                targetTranslationY,
                enterAnimDuration.toLong(),
                enterAnimInterpolator
            )
        } else {
            child.translationY = targetTranslationY.toFloat()
        }
    }

    /** Returns true if the current state is scrolled down.  */
    fun isScrolledDown(): Boolean {
        return currentState == STATE_SCROLLED_DOWN
    }

    /**
     * Slides the child with or without animation from its current position to be totally off the
     * screen.
     *
     * @param animate `true` to slide with animation.
     */
    @JvmOverloads
    fun slideDown(child: V, animate: Boolean = true) {
        if (isScrolledDown()) {
            return
        }

        if (currentAnimator != null) {
            currentAnimator!!.cancel()
            child.clearAnimation()
        }
        updateCurrentState(child, STATE_SCROLLED_DOWN)
        val targetTranslationY = height + additionalHiddenOffsetY
        if (animate) {
            animateChildTo(
                child,
                targetTranslationY,
                exitAnimDuration.toLong(),
                exitAnimInterpolator
            )
        } else {
            child.translationY = targetTranslationY.toFloat()
        }
    }

    private fun updateCurrentState(child: V, @ScrollState state: Int) {
        currentState = state
        for (listener in onScrollStateChangedListeners) {
            listener.onStateChanged(child, currentState)
        }
    }

    private fun animateChildTo(
        child: V, targetY: Int, duration: Long, interpolator: TimeInterpolator?
    ) {
        currentAnimator =
            child
                .animate()
                .translationY(targetY.toFloat())
                .setInterpolator(interpolator)
                .setDuration(duration)
                .setListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            currentAnimator = null
                        }
                    })
    }

    /**
     * Adds a listener to be notified of bottom view scroll state changes.
     *
     * @param listener The listener to notify when bottom view scroll state changes.
     */
    fun addOnScrollStateChangedListener(listener: OnScrollStateChangedListener) {
        onScrollStateChangedListeners.add(listener)
    }

    /**
     * Removes a previously added listener.
     *
     * @param listener The listener to remove.
     */
    fun removeOnScrollStateChangedListener(listener: OnScrollStateChangedListener) {
        onScrollStateChangedListeners.remove(listener)
    }

    /** Remove all previously added [OnScrollStateChangedListener]s.  */
    fun clearOnScrollStateChangedListeners() {
        onScrollStateChangedListeners.clear()
    }


    companion object {
        private const val DEFAULT_ENTER_ANIMATION_DURATION_MS = 225
        private const val DEFAULT_EXIT_ANIMATION_DURATION_MS = 175
        private val ENTER_ANIM_DURATION_ATTR = R.attr.motionDurationLong2
        private val EXIT_ANIM_DURATION_ATTR = R.attr.motionDurationMedium4
        private val ENTER_EXIT_ANIM_EASING_ATTR = R.attr.motionEasingEmphasizedInterpolator

        /** State of the bottom view when it's scrolled down.  */
        const val STATE_SCROLLED_DOWN: Int = 1

        /**
         * State of the bottom view when it's scrolled up.
         */
        const val STATE_SCROLLED_UP: Int = 2
    }
}
