package com.malaikatmaut.imagecompression.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.malaikatmaut.imagecompression.ui.CompressFragment
import com.malaikatmaut.imagecompression.ui.DecompressFragment

@ExperimentalUnsignedTypes
class ViewPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CompressFragment()
            else -> DecompressFragment()
        }
    }
}