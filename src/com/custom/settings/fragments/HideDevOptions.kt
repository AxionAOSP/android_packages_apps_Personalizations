package com.custom.settings.fragments

import com.android.settings.R
import com.android.settings.preferences.BaseAppListFragment

class HideDevOptions : BaseAppListFragment() {

    override fun getSettingsKey(): String = "hide_developer_status"
    override fun getCategoryTitle(): String = ""
    override fun getExcludedPackages(): List<String> {
        return requireContext().resources.getStringArray(R.array.nt_app_lock_exclude_list).toList()
    }
    override fun getFragmentTitle(): String = getString(R.string.hide_dev_options_title)
    override fun getSelectedCategoryTitle(): String = getString(R.string.hide_dev_options_hidden)
    override fun getUnselectedCategoryTitle(): String = getString(R.string.hide_dev_options_visible)
}
