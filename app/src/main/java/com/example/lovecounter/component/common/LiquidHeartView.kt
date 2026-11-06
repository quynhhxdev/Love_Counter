package com.example.lovecounter.component.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.example.lovecounter.R
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tan

class LiquidHeartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), SensorEventListener {

    // Properties
    private var progress: Float = 100f
    private var currentDisplayProgress: Float = 0f
    private var isTiltEnabled: Boolean = true
    private var isWaveEnabled: Boolean = true

    // Colors
    private var liquidColor: Int = Color.parseColor("#FF0845")
    private var liquidSecondaryColor: Int = Color.parseColor("#80FF0845")
    private var heartBackgroundColor: Int = Color.parseColor("#FFE8ED")
    private var strokeColor: Int = Color.parseColor("#FF0845")
    private var strokeWidthPx: Float = 0f

    // Wave & Tilt variables
    private var waveShift: Float = 0f
    private var targetTiltAngle: Float = 0f
    private var currentTiltAngle: Float = 0f
    private var waveAmplitudeRatio: Float = DEFAULT_WAVE_AMPLITUDE_RATIO
    private var waveDurationMs: Long = DEFAULT_WAVE_DURATION_MS

    // Paints & Paths
    private val heartPath = Path()
    private val wavePathPrimary = Path()
    private val wavePathSecondary = Path()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val liquidPrimaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val liquidSecondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Animators
    private var progressAnimator: ValueAnimator? = null
    private var waveAnimator: ValueAnimator? = null

    // Sensor Manager
    private var sensorManager: SensorManager? = null
    private var gravitySensor: Sensor? = null
    private var isSensorRegistered = false

    init {
        val density = context.resources.displayMetrics.density
        strokeWidthPx = 2f * density

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.LiquidHeartView)
            progress = typedArray.getFloat(R.styleable.LiquidHeartView_lh_progress, 100f).coerceIn(0f, 100f)
            currentDisplayProgress = progress
            isTiltEnabled = typedArray.getBoolean(R.styleable.LiquidHeartView_lh_tiltEnabled, true)
            isWaveEnabled = typedArray.getBoolean(R.styleable.LiquidHeartView_lh_waveEnabled, true)
            liquidColor = typedArray.getColor(
                R.styleable.LiquidHeartView_lh_liquidColor,
                ContextCompat.getColor(context, R.color.primary_pink)
            )
            liquidSecondaryColor = typedArray.getColor(
                R.styleable.LiquidHeartView_lh_liquidSecondaryColor,
                Color.parseColor("#80FF0845")
            )
            heartBackgroundColor = typedArray.getColor(
                R.styleable.LiquidHeartView_lh_heartBackgroundColor,
                Color.parseColor("#FFE8ED")
            )
            strokeColor = typedArray.getColor(
                R.styleable.LiquidHeartView_lh_strokeColor,
                ContextCompat.getColor(context, R.color.primary_pink)
            )
            strokeWidthPx = typedArray.getDimension(
                R.styleable.LiquidHeartView_lh_strokeWidth,
                2f * density
            )
            waveAmplitudeRatio = typedArray.getFloat(
                R.styleable.LiquidHeartView_lh_waveAmplitudeRatio,
                DEFAULT_WAVE_AMPLITUDE_RATIO
            )
            waveDurationMs = typedArray.getInt(
                R.styleable.LiquidHeartView_lh_waveDurationMs,
                DEFAULT_WAVE_DURATION_MS.toInt()
            ).toLong()
            typedArray.recycle()
        }

        updatePaints()
        initSensors()
    }

    private fun updatePaints() {
        bgPaint.color = heartBackgroundColor
        strokePaint.color = strokeColor
        strokePaint.strokeWidth = strokeWidthPx
        liquidPrimaryPaint.color = liquidColor
        liquidSecondaryPaint.color = liquidSecondaryColor
    }

    private fun initSensors() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        gravitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildHeartPath(w.toFloat(), h.toFloat())
    }

    /**
     * Builds a smooth, proportional scalable heart path fitting inside (w, h).
     */
    private fun buildHeartPath(width: Float, height: Float) {
        heartPath.reset()
        val padding = strokeWidthPx / 2f + 2f
        val w = width - 2 * padding
        val h = height - 2 * padding

        if (w <= 0 || h <= 0) return

        val centerX = width / 2f
        val topCenterY = padding + h * 0.20f
        val bottomY = padding + h * 0.96f

        heartPath.moveTo(centerX, topCenterY)
        // Top left curve
        heartPath.cubicTo(
            centerX - w * 0.6f, padding,
            padding - w * 0.06f, padding + h * 0.6f,
            centerX, bottomY
        )
        // Top right curve
        heartPath.cubicTo(
            padding + w + w * 0.06f, padding + h * 0.6f,
            centerX + w * 0.6f, padding,
            centerX, topCenterY
        )
        heartPath.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. Draw background heart
        canvas.drawPath(heartPath, bgPaint)

        // 2. Draw liquid inside heart (if progress > 0)
        if (currentDisplayProgress > 0f) {
            canvas.save()
            // Clip canvas to the heart shape
            canvas.clipPath(heartPath)

            // Calculate liquid level Y (0% at bottomY, 100% at topY)
            val padding = strokeWidthPx / 2f + 2f
            val topY = padding + h * 0.13f
            val bottomY = padding + (h - 2 * padding) * 0.96f
            val totalHeight = bottomY - topY
            val waveAmplitude = totalHeight * DEFAULT_WAVE_AMPLITUDE_RATIO
            val waterLevelY = bottomY - (progress / 100f) * totalHeight + waveAmplitude

            // Smooth tilt angle transition
            currentTiltAngle += (targetTiltAngle - currentTiltAngle) * 0.1f

            // Build wave paths
            buildWavePath(
                wavePath = wavePathSecondary,
                width = w,
                height = h,
                waterLevelY = waterLevelY,
                waveShift = -waveShift * 0.8f,
                amplitude = (h * waveAmplitudeRatio * 0.85f),
                frequency = (2 * PI / w).toFloat() * 1.2f,
                tiltAngle = currentTiltAngle
            )

            buildWavePath(
                wavePath = wavePathPrimary,
                width = w,
                height = h,
                waterLevelY = waterLevelY,
                waveShift = waveShift,
                amplitude = (h * waveAmplitudeRatio),
                frequency = (2 * PI / w).toFloat(),
                tiltAngle = currentTiltAngle
            )

            // Draw secondary wave then primary wave
            canvas.drawPath(wavePathSecondary, liquidSecondaryPaint)
            canvas.drawPath(wavePathPrimary, liquidPrimaryPaint)

            canvas.restore()
        }

        // 3. Draw outer heart border
        if (strokeWidthPx > 0f) {
            canvas.drawPath(heartPath, strokePaint)
        }
    }

    private fun buildWavePath(
        wavePath: Path,
        width: Float,
        height: Float,
        waterLevelY: Float,
        waveShift: Float,
        amplitude: Float,
        frequency: Float,
        tiltAngle: Float
    ) {
        wavePath.reset()
        val centerX = width / 2f
        val tanTilt = tan(tiltAngle)

        // Start from top-left off-screen
        wavePath.moveTo(-20f, height + 50f)

        // Draw wave surface line
        var x = -20f
        val step = 10f
        while (x <= width + 20f) {
            val tiltY = tanTilt * (x - centerX)
            val sineY = if (isWaveEnabled) amplitude * sin(frequency * x + waveShift) else 0f
            val y = waterLevelY + tiltY + sineY
            wavePath.lineTo(x, y)
            x += step
        }

        // Complete polygon down to bottom
        wavePath.lineTo(width + 20f, height + 50f)
        wavePath.close()
    }

    // Public APIs
    fun setProgress(newProgress: Float, animate: Boolean = true) {
        val clamped = newProgress.coerceIn(0f, 100f)
        if (progress == clamped && !animate) return

        progress = clamped
        progressAnimator?.cancel()

        if (animate && isAttachedToWindow) {
            progressAnimator = ValueAnimator.ofFloat(currentDisplayProgress, progress).apply {
                duration = 2000L
                interpolator = LinearInterpolator()
                addUpdateListener {
                    currentDisplayProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            currentDisplayProgress = progress
            invalidate()
        }
    }

    fun getProgress(): Float = progress

    fun setTiltEnabled(enabled: Boolean) {
        if (isTiltEnabled != enabled) {
            isTiltEnabled = enabled
            if (!enabled) {
                targetTiltAngle = 0f
            }
            updateSensorRegistration()
        }
    }

    fun setWaveEnabled(enabled: Boolean) {
        if (isWaveEnabled != enabled) {
            isWaveEnabled = enabled
            updateAnimators()
            invalidate()
        }
    }

    fun setWaveConfig(amplitudeRatio: Float = waveAmplitudeRatio, durationMs: Long = waveDurationMs) {
        waveAmplitudeRatio = amplitudeRatio.coerceIn(0.005f, 0.15f)
        waveDurationMs = durationMs.coerceAtLeast(200L)
        waveAnimator?.cancel()
        waveAnimator = null
        updateAnimators()
        invalidate()
    }

    fun setLiquidColor(color: Int, secondaryColor: Int? = null) {
        liquidColor = color
        liquidSecondaryColor = secondaryColor ?: Color.argb(
            128,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
        updatePaints()
        invalidate()
    }

    fun setHeartBackgroundColor(color: Int) {
        heartBackgroundColor = color
        updatePaints()
        invalidate()
    }

    fun setStrokeColor(color: Int) {
        strokeColor = color
        updatePaints()
        invalidate()
    }

    // SensorEventListener
    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTiltEnabled || event == null) return
        if (event.sensor.type == Sensor.TYPE_GRAVITY || event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val gravityX = event.values[0]
            // Calculate tilt angle: 9.8m/s^2 mapped to approx max 40 degree tilt
            val normalizedX = (gravityX / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
            targetTiltAngle = Math.toRadians((normalizedX * 40.0)).toFloat()
            invalidate()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Lifecycle & Animations
    private fun updateAnimators() {
        if (isWaveEnabled && isAttachedToWindow && visibility == VISIBLE) {
            if (waveAnimator == null || !waveAnimator!!.isRunning) {
                waveAnimator = ValueAnimator.ofFloat(0f, (2 * PI).toFloat()).apply {
                    duration = waveDurationMs
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = LinearInterpolator()
                    addUpdateListener {
                        waveShift = it.animatedValue as Float
                        invalidate()
                    }
                    start()
                }
            }
        } else {
            waveAnimator?.cancel()
            waveAnimator = null
        }
    }

    private fun updateSensorRegistration() {
        if (isTiltEnabled && isAttachedToWindow && visibility == VISIBLE) {
            if (!isSensorRegistered && gravitySensor != null) {
                sensorManager?.registerListener(
                    this,
                    gravitySensor,
                    SensorManager.SENSOR_DELAY_GAME
                )
                isSensorRegistered = true
            }
        } else {
            if (isSensorRegistered) {
                sensorManager?.unregisterListener(this)
                isSensorRegistered = false
                targetTiltAngle = 0f
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimators()
        updateSensorRegistration()
    }

    override fun onDetachedFromWindow() {
        progressAnimator?.cancel()
        waveAnimator?.cancel()
        updateSensorRegistration()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimators()
        updateSensorRegistration()
    }

    companion object {
        const val DEFAULT_WAVE_AMPLITUDE_RATIO = 0.02f
        const val DEFAULT_WAVE_DURATION_MS = 4000L
    }
}
