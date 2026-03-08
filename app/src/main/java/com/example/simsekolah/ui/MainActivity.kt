package com.example.simsekolah.ui

import android.R
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.simsekolah.adapter.BannerAdapter
import com.example.simsekolah.databinding.ActivityMainBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    // Use binding instead of manual findViewById
    private lateinit var binding: ActivityMainBinding
    private lateinit var handler: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

        val bannerList = listOf(
            com.example.simsekolah.R.drawable.banner1,
            com.example.simsekolah.R.drawable.banner2,
            com.example.simsekolah.R.drawable.banner3
        )

        val adapter = BannerAdapter(bannerList)

        binding.viewPagerBanner.adapter = adapter
        binding.dotsIndicator.attachTo(binding.viewPagerBanner)

        binding.viewPagerBanner.setPageTransformer { page, position ->
            val scale = 0.9f + (1 - abs(position)) * 0.1f
            page.scaleY = scale
            page.scaleX = scale
            page.alpha = 0.5f + (1 - abs(position)) * 0.5f
        }

        // AUTO SLIDE
        handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                val nextItem = (binding.viewPagerBanner.currentItem + 1) % bannerList.size
                binding.viewPagerBanner.currentItem = nextItem
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(runnable, 3000)
    }
}