package com.malaikatmaut.imagecompression.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.malaikatmaut.imagecompression.R
import com.malaikatmaut.imagecompression.util.ViewPagerAdapter
import com.malaikatmaut.imagecompression.databinding.ActivityMainBinding
import com.malaikatmaut.imagecompression.util.ZoomOutPageTransformer

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter =
            ViewPagerAdapter(this)
        binding.viewPager.requestDisallowInterceptTouchEvent(true)
        binding.viewPager.setPageTransformer(ZoomOutPageTransformer())

        TabLayoutMediator(binding.tabLayout, binding.viewPager, true) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.compress)
                }
                else -> {
                    tab.text = getString(R.string.decompress)
                }
            }
        }.attach()
    }

    override fun onBackPressed() {
        if (binding.viewPager.currentItem == 0) {
            super.onBackPressed()
        }
        binding.viewPager.currentItem = binding.viewPager.currentItem - 1
    }
}