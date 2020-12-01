package com.moyeorak.camgong

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.moyeorak.camgong.tabviewpager.FragmentTabCalendar
import com.moyeorak.camgong.tabviewpager.FragmentTabHome
import com.moyeorak.camgong.tabviewpager.FragmentTabStats

class PageAdapter(fragmentActivity: FragmentActivity):
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> FragmentTabCalendar()
            1 -> FragmentTabHome()
            else -> FragmentTabStats()
        }
    }
}