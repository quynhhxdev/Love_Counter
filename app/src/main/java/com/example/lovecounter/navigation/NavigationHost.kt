package com.example.lovecounter.navigation

import androidx.fragment.app.Fragment

interface NavigationHost {
    val navigationManager: NavigationManager
}

val Fragment.navigationManager: NavigationManager
    get() = (activity as? NavigationHost)?.navigationManager
        ?: error("Activity ${activity?.javaClass?.simpleName} must implement NavigationHost")
