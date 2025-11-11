package com.xmobile.project1groupstudyappnew.utils.ui.dialog

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.xmobile.project1groupstudyappnew.R

object ConfirmDialog {

    fun showCustomDialog(
        context: Context,
        title: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null,
    ) {
        val builder = AlertDialog.Builder(context)
        val view = LayoutInflater.from(context).inflate(R.layout.layout_confirm, null)
        builder.setView(view)
        val dialog = builder.create()

        val txtTitle = view.findViewById<TextView>(R.id.txtTitle)
        txtTitle.text = title
        val txtCancel = view.findViewById<TextView>(R.id.txtCancel)
        val txtConfirm = view.findViewById<TextView>(R.id.txtConfirm)

        txtCancel.setOnClickListener {
            onCancel?.invoke()
            dialog.dismiss()
        }

        txtConfirm.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }

        dialog.show()
    }
}