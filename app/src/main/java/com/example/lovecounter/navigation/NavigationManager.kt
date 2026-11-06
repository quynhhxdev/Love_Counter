package com.example.lovecounter.navigation

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

typealias OnDestinationChangedListener = (Fragment?) -> Unit

/**
 * Centralized NavigationManager based on Android FragmentManager and FragmentTransaction.
 * Handles single-activity navigation, root screens, back stack management, and tab switching.
 */
class NavigationManager(
    private val fragmentManager: FragmentManager,
    @IdRes private val containerId: Int
) {

    private val destinationListeners = mutableListOf<OnDestinationChangedListener>()
    var currentTab: RootTab? = null
        private set

    init {
        fragmentManager.addOnBackStackChangedListener {
            notifyDestinationChanged(getCurrentFragment())
        }
    }

    fun addOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        destinationListeners.add(listener)
    }

    fun removeOnDestinationChangedListener(listener: OnDestinationChangedListener) {
        destinationListeners.remove(listener)
    }

    private fun notifyDestinationChanged(destination: Fragment?) {
        destinationListeners.forEach { it(destination) }
    }

    /**
     * Navigate from the current Fragment to another Fragment.
     * Replaces the current Fragment view, supports back stack, arguments, and custom animations.
     * Avoids duplicate destinations if the top fragment is already of the target class.
     */
    fun navigate(
        fragment: Fragment,
        args: Bundle? = null,
        addToBackStack: Boolean = true,
        tag: String? = null,
        enterAnim: Int = 0,
        exitAnim: Int = 0,
        popEnterAnim: Int = 0,
        popExitAnim: Int = 0
    ): Boolean {
        if (fragmentManager.isStateSaved) return false

        val current = getCurrentFragment()
        if (current != null && current::class == fragment::class) {
            return false
        }

        if (args != null) {
            fragment.arguments = args
        }

        val transaction = fragmentManager.beginTransaction()
        transaction.setReorderingAllowed(true)
        if (enterAnim != 0 || exitAnim != 0 || popEnterAnim != 0 || popExitAnim != 0) {
            transaction.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
        }

        if (current != null && current.isAdded && !current.isHidden) {
            transaction.hide(current)
        }

        val fragmentTag = tag ?: fragment::class.java.simpleName
        transaction.add(containerId, fragment, fragmentTag)

        if (addToBackStack) {
            transaction.addToBackStack(fragmentTag)
        }

        transaction.commitAllowingStateLoss()
        notifyDestinationChanged(fragment)
        return true
    }

    /**
     * Replace current screen with a root Fragment, clearing the previous navigation back stack.
     */
    fun replaceRoot(
        fragment: Fragment,
        args: Bundle? = null,
        tag: String? = null
    ) {
        if (fragmentManager.isStateSaved) return

        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        val rootTab = RootTab.fromFragment(fragment)
        if (rootTab != null) {
            navigateToRoot(rootTab)
            return
        }

        val transaction = fragmentManager.beginTransaction()
        transaction.setReorderingAllowed(true)
        RootTab.entries.forEach { t ->
            fragmentManager.findFragmentByTag(t.tag)?.let { frag ->
                transaction.remove(frag)
            }
        }

        if (args != null) {
            fragment.arguments = args
        }

        val fragmentTag = tag ?: fragment::class.java.simpleName
        transaction.replace(containerId, fragment, fragmentTag)
        transaction.commitAllowingStateLoss()

        currentTab = null
        notifyDestinationChanged(fragment)
    }

    /**
     * Navigate to one of the root tabs (Home, Love Test, Memory).
     * Reuses fragment instances without unnecessary recreation.
     */
    fun navigateToRoot(tab: RootTab) {
        if (fragmentManager.isStateSaved) return

        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        if (currentTab == tab && getCurrentFragment()?.isAdded == true && !getCurrentFragment()!!.isHidden) {
            return
        }

        val transaction = fragmentManager.beginTransaction()
        transaction.setReorderingAllowed(true)

        RootTab.entries.forEach { t ->
            fragmentManager.findFragmentByTag(t.tag)?.let { frag ->
                if (frag.isAdded && !frag.isHidden) {
                    transaction.hide(frag)
                }
            }
        }

        val currentNonTab = fragmentManager.findFragmentById(containerId)
        if (currentNonTab != null && RootTab.fromTag(currentNonTab.tag) == null) {
            transaction.remove(currentNonTab)
        }

        val existing = fragmentManager.findFragmentByTag(tab.tag)
        val target = if (existing != null) {
            transaction.show(existing)
            existing
        } else {
            val newFrag = tab.createFragment()
            transaction.add(containerId, newFrag, tab.tag)
            newFrag
        }

        transaction.commitAllowingStateLoss()
        currentTab = tab
        notifyDestinationChanged(target)
    }

    /**
     * Overload to navigate to a root screen given a Fragment instance.
     */
    fun navigateToRoot(fragment: Fragment, tag: String? = null) {
        val tab = RootTab.fromFragment(fragment)
        if (tab != null) {
            navigateToRoot(tab)
        } else {
            replaceRoot(fragment, fragment.arguments, tag)
        }
    }

    /**
     * Handle Back navigation.
     * Pops the back stack if available.
     * If at a secondary root tab, returns to Home.
     * Returns false if there is no previous destination to delegate to Activity.
     */
    fun goBack(): Boolean {
        if (fragmentManager.isStateSaved) return false

        if (fragmentManager.backStackEntryCount > 0) {
            val popped = fragmentManager.popBackStackImmediate()
            if (popped) {
                notifyDestinationChanged(getCurrentFragment())
            }
            return popped
        }

        if (currentTab != null && currentTab != RootTab.HOME) {
            navigateToRoot(RootTab.HOME)
            return true
        }

        return false
    }

    /**
     * Pop the back stack to a previous destination.
     */
    fun popBackStack(tag: String? = null, inclusive: Boolean = false): Boolean {
        if (fragmentManager.isStateSaved) return false
        val popped = if (tag != null) {
            val flags = if (inclusive) FragmentManager.POP_BACK_STACK_INCLUSIVE else 0
            fragmentManager.popBackStackImmediate(tag, flags)
        } else {
            fragmentManager.popBackStackImmediate()
        }
        if (popped) {
            notifyDestinationChanged(getCurrentFragment())
        }
        return popped
    }

    /**
     * Returns the currently visible Fragment inside the container.
     */
    fun getCurrentFragment(): Fragment? {
        val fragments = fragmentManager.fragments
        for (i in fragments.indices.reversed()) {
            val f = fragments[i]
            if (f.isAdded && !f.isHidden && f.id == containerId) {
                return f
            }
        }
        val tab = currentTab
        if (tab != null) {
            val tabFrag = fragmentManager.findFragmentByTag(tab.tag)
            if (tabFrag != null && tabFrag.isAdded && !tabFrag.isHidden) {
                return tabFrag
            }
        }
        return fragmentManager.findFragmentById(containerId)
    }

    /**
     * Synchronize state after configuration changes or Activity recreation.
     */
    fun onRestoreState() {
        val active = getCurrentFragment()
        currentTab = RootTab.fromFragment(active)
        notifyDestinationChanged(active)
    }
}
