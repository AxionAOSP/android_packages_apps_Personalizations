package com.custom.settings.fragments

import com.android.settings.R
import com.android.settings.preferences.BaseAppListFragment

class HideAppListSettings : BaseAppListFragment() {

    override fun getSettingsKey(): String = "hide_applist"
    override fun getCategoryTitle(): String = ""
    override fun getExcludedPackages(): List<String> {
        return requireContext().resources.getStringArray(R.array.nt_app_lock_exclude_list).toList()
    }
    override fun getFragmentTitle(): String = getString(R.string.hide_applist_title)
    override fun getSelectedCategoryTitle(): String = getString(R.string.hide_app_list_hidden)
    override fun getUnselectedCategoryTitle(): String = getString(R.string.hide_app_list_visible)
}
