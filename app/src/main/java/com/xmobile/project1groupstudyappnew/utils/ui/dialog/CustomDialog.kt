package com.xmobile.project1groupstudyappnew.utils.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog

object CustomDialog {
     fun showCustomDialog(
         context: Context,
        @LayoutRes layoutResId: Int,
        bindViews: (View) -> Unit = {},
        onClickActions: (View, AlertDialog) -> Unit = { _, _ -> },
        onDismiss: (AlertDialog) -> Unit = {}
    ) {
        val builder = AlertDialog.Builder(context)
        val view = LayoutInflater.from(context).inflate(layoutResId, null)
        builder.setView(view)
        val dialog = builder.create()

        bindViews(view)
        onClickActions(view, dialog)
        dialog.setOnDismissListener { onDismiss(dialog) }
        dialog.show()
    }
}