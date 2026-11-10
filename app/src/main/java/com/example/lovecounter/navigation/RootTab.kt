package com.example.lovecounter.navigation

import androidx.fragment.app.Fragment
import com.example.lovecounter.component.home.HomeFragment
import com.example.lovecounter.component.lovetest.LoveTestFragment
import com.example.lovecounter.component.memory.MemoryFragment

enum class RootTab(val tag: String) {
    HOME("tab_home") {
        override fun createFragment(): Fragment = HomeFragment()
    },
    LOVE_TEST("tab_love_test") {
        override fun createFragment(): Fragment = LoveTestFragment()
    },
    MEMORY("tab_memory") {
        override fun createFragment(): Fragment = MemoryFragment()
    };

    abstract fun createFragment(): Fragment

    companion object {
        fun fromFragment(fragment: Fragment?): RootTab? = when (fragment) {
            is HomeFragment -> HOME
            is LoveTestFragment -> LOVE_TEST
            is MemoryFragment -> MEMORY
            else -> null
        }

        fun fromTag(tag: String?): RootTab? = entries.firstOrNull { it.tag == tag }
    }
}
