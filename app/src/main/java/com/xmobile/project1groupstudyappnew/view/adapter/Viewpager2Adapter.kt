package com.xmobile.project1groupstudyappnew.view.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.xmobile.project1groupstudyappnew.view.fragment.CalendarFragment
import com.xmobile.project1groupstudyappnew.view.fragment.HomeFragment
import com.xmobile.project1groupstudyappnew.view.fragment.NotiFragment
import com.xmobile.project1groupstudyappnew.view.fragment.ProfileFragment
import javax.annotation.Nonnull

class Viewpager2Adapter(
    @Nonnull
    fragmentActivity: FragmentActivity
): FragmentStateAdapter(fragmentActivity) {

    @Nonnull
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> CalendarFragment()
            2 -> NotiFragment()
            3 -> ProfileFragment()
            else -> HomeFragment()
        }
    }

    override fun getItemCount(): Int {
        return 4
    }
}