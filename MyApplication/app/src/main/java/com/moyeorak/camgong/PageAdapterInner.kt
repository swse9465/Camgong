package com.moyeorak.camgong

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.moyeorak.camgong.tabviewpager.FragmentTabCalendar
import com.moyeorak.camgong.tabviewpagerinner.FragmentTabDaily
import com.moyeorak.camgong.tabviewpagerinner.FragmentTabMonthly
import com.moyeorak.camgong.tabviewpagerinner.FragmentTabWeekly

class PageAdapterInner(fragmentActivity: FragmentTabCalendar):
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> FragmentTabDaily()
            1 -> FragmentTabMonthly()
            else -> FragmentTabWeekly()
        }
    }

}
