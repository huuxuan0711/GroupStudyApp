package com.xmobile.project1groupstudyappnew.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.xmobile.project1groupstudyappnew.R

object FontManager {

    private const val TAG = "FontManager"
    private val fontCache = HashMap<String, Typeface>()

    fun getTypeface(context: Context, style: Int): Typeface {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("choose", Context.MODE_PRIVATE)
        val font = sharedPreferences.getString("chooseFont", "") ?: ""

        return try {
            if (font.isEmpty()) {
                return when (style) {
                    Typeface.BOLD -> safeGetFont(context, R.font.roboto_bold)
                    Typeface.NORMAL -> safeGetFont(context, R.font.roboto_regular)
                    else -> Typeface.SANS_SERIF
                }
            }

            when (font) {
                "Inter" -> {
                    return if (style == Typeface.BOLD) {
                        safeGetFont(context, R.font.inter_bold)
                    } else {
                        safeGetFont(context, R.font.inter_regular)
                    }
                }

                "Lato" -> {
                    return if (style == Typeface.BOLD) {
                        safeGetFont(context, R.font.lato_bold)
                    } else {
                        safeGetFont(context, R.font.lato_regular)
                    }
                }

                "Nunito Sans" -> {
                    return if (style == Typeface.BOLD) {
                        safeGetFont(context, R.font.nunito_sans_bold)
                    } else {
                        safeGetFont(context, R.font.nunito_sans_regular)
                    }
                }

                "Open Sans" -> {
                    return if (style == Typeface.BOLD) {
                        safeGetFont(context, R.font.open_sans_bold)
                    } else {
                        safeGetFont(context, R.font.open_sans_regular)
                    }
                }

                "Roboto" -> {
                    return if (style == Typeface.BOLD) {
                        safeGetFont(context, R.font.roboto_bold)
                    } else {
                        safeGetFont(context, R.font.roboto_regular)
                    }
                }

                else -> Typeface.SANS_SERIF
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load font: $font", e)
            Typeface.SANS_SERIF
        }
    }

    private fun safeGetFont(context: Context, fontResId: Int): Typeface {
        val key = context.resources.getResourceEntryName(fontResId)

        fontCache[key]?.let { return it }

        return try {
            val typeface = ResourcesCompat.getFont(context, fontResId)
                ?: throw NullPointerException("Typeface is null")
            fontCache[key] = typeface
            typeface
        } catch (e: Exception) {
            Typeface.SANS_SERIF
        }
    }
}