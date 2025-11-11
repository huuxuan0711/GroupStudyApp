package com.xmobile.project1groupstudyappnew.view.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R

class FeatureAdapter(
    private val featureChooses: List<String>,
    private val context: Context,
    private val type: Int,
    private val onItemClick: (position: Int) -> Unit
): RecyclerView.Adapter<FeatureAdapter.FeatureViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FeatureViewHolder {
        return FeatureViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_feature_setting, parent, false))
    }

    override fun onBindViewHolder(
        holder: FeatureViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        holder.txtNameFeature.text = featureChooses[position]
        var choose = ""
        when (type) {
            1 -> { //chế độ nền
                val chooseResID = context.getSharedPreferences("choose", Context.MODE_PRIVATE)
                    .getInt("chooseTheme", R.string.light)
                choose = context.getString(chooseResID)
            }
            2 -> { //font hệ thống
                choose = context.getSharedPreferences("choose", Context.MODE_PRIVATE)
                    .getString("chooseFont", "Roboto")!!
                var typeface: Typeface?
                typeface = when (featureChooses[position]) {
                    "Inter" -> ResourcesCompat.getFont(context, R.font.inter_regular)
                    "Lato" -> ResourcesCompat.getFont(context, R.font.lato_regular)
                    "Nunito Sans" -> ResourcesCompat.getFont(context, R.font.nunito_sans_regular)

                    "Open Sans" -> ResourcesCompat.getFont(context, R.font.open_sans_regular)
                    else -> ResourcesCompat.getFont(context, R.font.roboto_regular)
                }

                // Fallback nếu font bị null
                if (typeface == null) {
                    typeface = Typeface.SANS_SERIF
                }
                holder.txtNameFeature.setTypeface(typeface)
            }
            3 -> { //ngôn ngữ hệ thống
                var chooseResID = context.getSharedPreferences("choose", Context.MODE_PRIVATE)
                    .getInt("chooseLanguage", 0)
                if (chooseResID == 0) {
                    chooseResID = R.string.vietnamese
                }
                choose = context.getString(chooseResID)
                Log.d("choose", choose)
            }
        }

        if (featureChooses[position] == choose) {
            holder.imgChoose.setVisibility(View.VISIBLE)
        } else {
            holder.imgChoose.setVisibility(View.INVISIBLE)
        }

        holder.itemView.setOnClickListener { onItemClick(position) }
    }

    override fun getItemCount(): Int {
        return featureChooses.size
    }

    inner class FeatureViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNameFeature: TextView = itemView.findViewById(R.id.choose_name)
        val imgChoose: ImageView = itemView.findViewById(R.id.imgCheck)
    }
}