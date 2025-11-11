package com.xmobile.project1groupstudyappnew.view.fragment

import android.graphics.Typeface
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import com.xmobile.project1groupstudyappnew.utils.FontManager

open class BaseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyFont(view)
    }

    protected fun applyFont(root: View) {
        when (root) {
            is TextView -> {
                val style = root.typeface?.style ?: Typeface.NORMAL
                val typeface = if (style == Typeface.BOLD) {
                    FontManager.getTypeface(requireContext(), Typeface.BOLD)
                } else {
                    FontManager.getTypeface(requireContext(), Typeface.NORMAL)
                }
                root.typeface = typeface
            }

            is ViewGroup -> {
                for (i in 0 until root.childCount) {
                    applyFont(root.getChildAt(i))
                }
            }
        }
    }
}