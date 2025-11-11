package com.xmobile.project1groupstudyappnew.utils.ui.menu_popup

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper

object PopupMenuHelper {
    @SuppressLint("RestrictedApi")
    fun show(
        context: Context,
        menuInflater: MenuInflater,
        anchorView: View,
        menuRes: Int,
        onItemSelected: (itemId: Int) -> Unit,
        prepareMenu: ((menuBuilder: MenuBuilder) -> Unit)? = null
    ) {
        val menuBuilder = MenuBuilder(context)
        menuInflater.inflate(menuRes, menuBuilder)

        // Cho phép tuỳ chỉnh menu trước khi show (ẩn hiện item, đổi title…)
        prepareMenu?.invoke(menuBuilder)

        val menuHelper = MenuPopupHelper(context, menuBuilder, anchorView)
        menuHelper.gravity = Gravity.FILL
        menuHelper.setForceShowIcon(true)

        menuBuilder.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                onItemSelected(item.itemId)
                menuHelper.dismiss()
                return true
            }
            override fun onMenuModeChange(menu: MenuBuilder) {}
        })

        menuHelper.show()
    }
}