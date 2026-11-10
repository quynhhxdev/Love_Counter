package com.ads.admob.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.min

class NativeAdShimmerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val cornerRadius = 4f * density
    private val cardRadius = 16f * density
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = CARD_COLOR
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = BORDER_COLOR
        style = Paint.Style.STROKE
        strokeWidth = density
    }
    private val skeletonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SKELETON_COLOR
    }
    private val shimmerShader = LinearGradient(
        0f,
        0f,
        120f * density,
        0f,
        intArrayOf(SKELETON_COLOR, SHIMMER_HIGHLIGHT_COLOR, SKELETON_COLOR),
        floatArrayOf(0f, 0.5f, 1f),
        Shader.TileMode.CLAMP
    )
    private val shimmerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = shimmerShader
    }
    private val shimmerPath = Path()
    private val shimmerMatrix = Matrix()
    private var shimmerOffset = 0f
    private var shimmerAnimator: ValueAnimator? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (320f * density).toInt()
        val desiredHeight = (CARD_HEIGHT_DP * density).toInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (isAttachedToWindow && visibility == VISIBLE) startShimmer()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (!isAttachedToWindow) return
        if (visibility == VISIBLE) startShimmer() else stopShimmer()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) return

        val halfStroke = borderPaint.strokeWidth / 2f
        canvas.drawRoundRect(
            halfStroke,
            halfStroke,
            width - halfStroke,
            height - halfStroke,
            cardRadius,
            cardRadius,
            cardPaint
        )
        canvas.drawRoundRect(
            halfStroke,
            halfStroke,
            width - halfStroke,
            height - halfStroke,
            cardRadius,
            cardRadius,
            borderPaint
        )

        shimmerPath.reset()
        val inset = 12f * density
        val iconSize = 40f * density
        val contentRight = width - inset
        val mediaTop = 80f * density
        val mediaBottom = min(mediaTop + 120f * density, height - 52f * density)
        val buttonTop = mediaBottom + 10f * density

        drawSkeleton(canvas, inset, inset, inset + iconSize, inset + iconSize, cornerRadius)
        drawSkeleton(
            canvas,
            inset + iconSize + 8f * density,
            inset + 12f * density,
            min(inset + iconSize + 148f * density, contentRight),
            inset + 26f * density,
            cornerRadius
        )
        drawSkeleton(canvas, inset, 59f * density, contentRight * 0.78f, 71f * density, cornerRadius)
        drawSkeleton(canvas, inset, mediaTop, contentRight, mediaBottom, cornerRadius)
        drawSkeleton(canvas, inset, buttonTop, contentRight, min(buttonTop + 40f * density, height - inset), cornerRadius)

        shimmerMatrix.setTranslate(shimmerOffset, 0f)
        shimmerShader.setLocalMatrix(shimmerMatrix)
        val saveCount = canvas.save()
        canvas.clipPath(shimmerPath)
        canvas.drawRect(0f, 0f, width, height, shimmerPaint)
        canvas.restoreToCount(saveCount)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (visibility == VISIBLE) startShimmer()
    }

    override fun onDetachedFromWindow() {
        stopShimmer()
        super.onDetachedFromWindow()
    }

    private fun drawSkeleton(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
        if (right <= left || bottom <= top) return
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, skeletonPaint)
        shimmerPath.addRoundRect(left, top, right, bottom, radius, radius, Path.Direction.CW)
    }

    private fun startShimmer() {
        if (shimmerAnimator?.isRunning == true || width == 0) return
        val bandWidth = 120f * density
        shimmerAnimator = ValueAnimator.ofFloat(-bandWidth, width + bandWidth).apply {
            duration = 1_200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                shimmerOffset = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopShimmer() {
        shimmerAnimator?.cancel()
        shimmerAnimator = null
    }

    companion object {
        private const val CARD_HEIGHT_DP = 262f
        private const val CARD_COLOR = 0xFFFFFFFF.toInt()
        private const val BORDER_COLOR = 0xFFE0E0E0.toInt()
        private const val SKELETON_COLOR = 0xFFE5E5E5.toInt()
        private const val SHIMMER_HIGHLIGHT_COLOR = 0xFFF7F7F7.toInt()
    }
}
