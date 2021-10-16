package com.bignerdranch.android.sunset

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var sceneView: View
    private lateinit var seaView: View
    private lateinit var sunView: View
    private lateinit var sunView2: View
    private lateinit var skyView: View
    private var animatorDirection: Boolean = false
    var animatorSet: AnimatorSet? = null

    private val blueSkyColor: Int by lazy {
        ContextCompat.getColor(this, R.color.blue_sky)
    }
    private val sunsetSkyColor: Int by lazy {
        ContextCompat.getColor(this, R.color.sunset_sky)
    }
    private val nightSkyColor: Int by lazy {
        ContextCompat.getColor(this, R.color.night_sky)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        seaView = findViewById(R.id.sea)
        sceneView = findViewById(R.id.scene)
        sunView = findViewById(R.id.sun)
        sunView2 = findViewById(R.id.sun2)
        skyView = findViewById(R.id.sky)

        sceneView.setOnClickListener {
            if (animatorDirection) startAnimationBack()
            else startAnimation()
            animatorDirection = !animatorDirection
        }
    }

    private fun startAnimation() {
        animatorSet?.cancel()

        val sun2YStart = sunView2.top.toFloat()
        val sun2YEnd: Float = (-sunView2.height).toFloat()

        val height2Animator = ObjectAnimator
            .ofFloat(sunView2, "y", sun2YStart, sun2YEnd)
            .setDuration(3000)
        height2Animator.interpolator = AccelerateInterpolator()

        val sunYStart = sunView.top.toFloat()
        val sunYEnd = skyView.height.toFloat()

        val heightAnimator = ObjectAnimator
            .ofFloat(sunView, "y", sunYStart, sunYEnd)
            .setDuration(3000)
        heightAnimator.interpolator = AccelerateInterpolator()


        val jerkAnimator = ObjectAnimator
            .ofFloat(sunView, "x", sunView.left + 20f, sunView.left - 20f)
            .setDuration(50)
        jerkAnimator.repeatMode = ValueAnimator.REVERSE
        jerkAnimator.repeatCount = 100

        val sunsetSkyAnimator = ObjectAnimator
            .ofInt(skyView, "backgroundColor", blueSkyColor, sunsetSkyColor)
            .setDuration(3000)

        sunsetSkyAnimator.setEvaluator(ArgbEvaluator())

        val nightSkyAnimator = ObjectAnimator
            .ofInt(skyView, "backgroundColor", sunsetSkyColor, nightSkyColor)
            .setDuration(1500)
        nightSkyAnimator.setEvaluator(ArgbEvaluator())

        animatorSet = AnimatorSet()
        animatorSet!!.play(heightAnimator)
            .with(sunsetSkyAnimator)
            .with(jerkAnimator)
            .with(height2Animator)
            .before(nightSkyAnimator)
        animatorSet!!.start()
    }

    private fun startAnimationBack() {
        animatorSet?.cancel()

        val sun2YStart: Float = (-sunView2.height).toFloat()
        val sun2YEnd = sunView2.top.toFloat()

        val height2Animator = ObjectAnimator
            .ofFloat(sunView2, "y", sun2YStart, sun2YEnd)
            .setDuration(3000)
        height2Animator.interpolator = AccelerateInterpolator()

        val sunYStart = skyView.height.toFloat()
        val sunYEnd = sunView.top.toFloat()

        val heightAnimator = ObjectAnimator
            .ofFloat(sunView, "y", sunYStart, sunYEnd)
            .setDuration(3000)
        heightAnimator.interpolator = AccelerateDecelerateInterpolator()

        val sunsetSkyAnimator = ObjectAnimator
            .ofInt(skyView, "backgroundColor", sunsetSkyColor, blueSkyColor)
            .setDuration(3000)

        sunsetSkyAnimator.setEvaluator(ArgbEvaluator())

        val nightSkyAnimator = ObjectAnimator
            .ofInt(skyView, "backgroundColor", nightSkyColor, sunsetSkyColor)
            .setDuration(1500)
        nightSkyAnimator.setEvaluator(ArgbEvaluator())

        animatorSet = AnimatorSet()
        animatorSet!!.play(nightSkyAnimator)
            .before(heightAnimator)
            .before(sunsetSkyAnimator)
            .before(height2Animator)
        animatorSet!!.start()
    }
}