package com.moyeorak.camgong

import androidx.appcompat.app.AppCompatActivity
import android.R
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout


class SplashActivity : AppCompatActivity() {
    var anim: Animation? = null
    var linearLayout: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //  setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); 화면 가로 고정
        setContentView(com.moyeorak.camgong.R.layout.activity_splash)
        linearLayout =
            findViewById<View>(com.moyeorak.camgong.R.id.activity_splash) as LinearLayout // Declare an imageView to show the animation.
        anim = AnimationUtils.loadAnimation(
            applicationContext,
            R.anim.fade_in
        ) // Create the animation.

        anim!!.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
                // HomeActivity.class is the activity to go after showing the splash screen.
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        linearLayout!!.startAnimation(anim)
    }
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        Thread.sleep(500L)
//        val intent = Intent(this, LoginActivity::class.java)
//        startActivity(intent)
//        finish()
//    }
}



