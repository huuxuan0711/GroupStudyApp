package com.xmobile.project1groupstudyappnew.utils.ui.menu_popup

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.xmobile.project1groupstudyappnew.R

object DeadlineTypePopupHelper {
    fun show(
        context: Context,
        anchorView: View,
        currentOption: String,
        onSelect: (selectedId: Int) -> Unit
    ) {
        val layout = LayoutInflater.from(context).inflate(R.layout.layout_option_deadline, null)
        val popup = PopupWindow(
            layout,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        popup.showAtLocation(
            anchorView,
            Gravity.NO_GRAVITY,
            location[0] + anchorView.width,
            location[1] + anchorView.height + 15
        )

        val ids = listOf(
            R.id.txt_amount_of_time,
            R.id.txt_choose_day_hour,
            R.id.txt_choose_day
        )

        for (id in ids) {
            val tv = layout.findViewById<TextView>(id)
            // highlight option hiện tại
            if (tv.text.toString() == currentOption) {
                tv.setTextColor(context.resources.getColor(R.color.blue, context.theme))
            } else {
                val typedValue = TypedValue()
                context.theme.resolveAttribute(R.attr.colorText2, typedValue, true)
                tv.setTextColor(typedValue.data)
            }
            tv.setOnClickListener {
                onSelect(id)
                popup.dismiss()
            }
        }
    }
}