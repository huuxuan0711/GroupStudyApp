package com.xmobile.project1groupstudyappnew.view.activity

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.xmobile.project1groupstudyappnew.R
import com.xmobile.project1groupstudyappnew.utils.FontManager

open class BaseActivity : AppCompatActivity() {

    override fun onCreateView(
        name: String,
        context: Context,
        attrs: AttributeSet
    ): View? {
        val view = super.onCreateView(name, context, attrs)

        if (view is TextView) {
            try {
                val style = view.typeface?.style ?: Typeface.NORMAL
                val typeface = if (style == Typeface.BOLD) {
                    FontManager.getTypeface(context, Typeface.BOLD)
                } else {
                    FontManager.getTypeface(context, Typeface.NORMAL)
                }
                view.typeface = typeface
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return view
    }

}