package br.android.cericatto.loadingstatus

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes

class LoadingButton @JvmOverloads constructor(
	context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

	// Main Attributes
	private var widthSize = 0
	private var heightSize = 0
	private var buttonState : ButtonState
	private var percentage = 0.0

	// Colors
	private var unClickedButtonColor = 0
	private var loadingButtonColor = 0
	private var completedButtonColor = 0

	// Paint
	private val paint = Paint().apply {
		style = Paint.Style.FILL
		textAlign = Paint.Align.CENTER
		textSize = 50.0f
	}

	init {
		isClickable = true
		buttonState = ButtonState.UnClicked
		context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
			unClickedButtonColor = getColor(R.styleable.LoadingButton_color_unclicked, 0)
			loadingButtonColor = getColor(R.styleable.LoadingButton_color_loading, 0)
			completedButtonColor = getColor(R.styleable.LoadingButton_color_completed, 0)
		}
	}

	override fun onDraw(canvas: Canvas?) {
		super.onDraw(canvas)

		animateLoadingButton(canvas)
		animateButtonText(canvas)
	}

	override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
		val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
		val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
		val h: Int = resolveSizeAndState(
			MeasureSpec.getSize(w),
			heightMeasureSpec,
			0
		)
		widthSize = w
		heightSize = h
		setMeasuredDimension(w, h)
	}

	override fun performClick(): Boolean {
		super.performClick()
		buttonState = ButtonState.Loading
		if (buttonState == ButtonState.Loading) {
			initValueAnimator()
		}
		invalidate()
		return true
	}

	private fun initValueAnimator() {
		val valueAnimator = ValueAnimator.ofFloat(0f, measuredWidth.toFloat())

		val updateListener = ValueAnimator.AnimatorUpdateListener { animated ->
			percentage = (animated.animatedValue as Float).toDouble()
			invalidate()
		}

		valueAnimator.duration = 3000
		valueAnimator.addUpdateListener(updateListener)
		valueAnimator.addListener(object : AnimatorListenerAdapter() {
			override fun onAnimationEnd(animation: Animator) {
				super.onAnimationEnd(animation)
				buttonState = ButtonState.UnClicked
			}
		})
		valueAnimator.start()
	}

	private fun animateLoadingButton(canvas: Canvas?) {
		paint.color = unClickedButtonColor
		canvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
		if (buttonState == ButtonState.Loading) {
			implementLoadingState(canvas)
		}
	}

	private fun implementLoadingState(canvas: Canvas?) {
		paint.color = loadingButtonColor
		val right = (width * (percentage / 100)).toFloat()
		val bottom = height.toFloat()
		canvas?.drawRect(0f, 0f, right, bottom, paint)

		val rect = RectF(0f, 0f, 80f, 80f)
		canvas?.save()
		canvas?.translate((width / 2 + 220).toFloat(), 40f)
		paint.color = completedButtonColor
		canvas?.drawArc(rect, 0f, (360 * (percentage / 100)).toFloat(), true, paint)
		canvas?.restore()
	}

	private fun animateButtonText(canvas: Canvas?) {
		paint.color = Color.WHITE
		val text = when (buttonState) {
			ButtonState.UnClicked -> context.getString(R.string.download)
			ButtonState.Loading -> context.getString(R.string.loading)
			ButtonState.Completed -> context.getString(R.string.download)
		}
		canvas?.drawText(text, (width / 2).toFloat(), ((height + 30) / 2).toFloat(), paint)
	}
}