package com.xmobile.project1groupstudyappnew.utils.ui.bottom_sheet

import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.xmobile.project1groupstudyappnew.R

object BottomSheetRecyclerView {
    fun showBottomSheet(
        rootView: View,
        bottomSheetId: Int,
        recyclerViewId: Int,
        setRecyclerView: (recyclerView: RecyclerView) -> Unit,
        loadData: () -> Unit,
        onItemClick: (position: Int) -> Unit
    ) {
        val bottomSheet = rootView.findViewById<ConstraintLayout>(bottomSheetId)
        val dimView = rootView.findViewById<View>(R.id.dimView)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        bottomSheet.visibility = View.GONE
        bottomSheetBehavior.isHideable = true

        bottomSheet.visibility = View.VISIBLE
        bottomSheet.post { bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED }

        dimView.visibility = View.VISIBLE
        dimView.setOnTouchListener { _, _ -> true }

        val txtCancel = bottomSheet.findViewById<TextView>(R.id.txtCancel)
        val recyclerView = bottomSheet.findViewById<RecyclerView>(recyclerViewId)
        setRecyclerView(recyclerView)

        loadData()

        txtCancel.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        recyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (e.action == MotionEvent.ACTION_UP) {
                    val childView = rv.findChildViewUnder(e.x, e.y)
                    if (childView != null) {
                        val position = rv.getChildAdapterPosition(childView)
                        onItemClick(position)
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        return true
                    }
                }
                return false
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                dimView.visibility = if (newState == BottomSheetBehavior.STATE_EXPANDED) View.VISIBLE else View.GONE
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                dimView.alpha = slideOffset
            }
        })
    }
}