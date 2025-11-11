package com.xmobile.project1groupstudyappnew.helper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.xmobile.project1groupstudyappnew.R
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import androidx.core.graphics.createBitmap

@SuppressLint("ClickableViewAccessibility")
abstract class MySwipeHelper(
    context: Context,
    private val recyclerView: RecyclerView,
    private val buttonWidth: Int
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private var buttonList: MutableList<MyButton> = ArrayList()
    private var swipePosition = -1 // vị trí item đang swipe
    private var swipeThreshold = 0.5f // ngưỡng kích hoạt
    private var buttonBuffer: MutableMap<Int, MutableList<MyButton>> = HashMap()
    private var removeQueue: Queue<Int> = object : LinkedList<Int>() {
        override fun add(element: Int): Boolean {
            return if (contains(element)) {
                false
            } else super.add(element)
        }
    }

    // hỗ trợ click
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            for (button in buttonList) {
                if (button.onClick(e.x, e.y)) break
            }
            return true
        }
    }

    private var gestureDetector: GestureDetector = GestureDetector(context, gestureListener)

    // xử lý chạm trên item được swipe
    private val onTouchListener = View.OnTouchListener { v, event ->
        if (swipePosition < 0) return@OnTouchListener false
        val point = Point(event.rawX.toInt(), event.rawY.toInt())

        val swipeViewHolder = recyclerView.findViewHolderForAdapterPosition(swipePosition)
        val swipedItem = swipeViewHolder?.itemView ?: return@OnTouchListener false
        val rect = Rect()
        swipedItem.getGlobalVisibleRect(rect)

        if (event.action == MotionEvent.ACTION_DOWN ||
            event.action == MotionEvent.ACTION_UP ||
            event.action == MotionEvent.ACTION_MOVE
        ) {
            if (rect.top < point.y && rect.bottom > point.y) {
                // chạm bên trong
                gestureDetector.onTouchEvent(event)
            } else {
                // chạm bên ngoài -> khôi phục trạng thái
                removeQueue.add(swipePosition)
                swipePosition = -1
                recoverSwipedItem()
                swipeViewHolder.itemView.setBackgroundResource(R.drawable.bg_group)
            }
        }

        if (event.action == MotionEvent.ACTION_UP) {
            v.performClick()   // 👈 thêm dòng này để hết warning
        }
        false
    }

    init {
        recyclerView.setOnTouchListener(onTouchListener)
        attachSwipe()
    }

    private fun attachSwipe() {
        val itemTouchHelper = ItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    inner class MyButton(
        private val context: Context,
        private val text: String,
        private val textSize: Int,
        private val imageResId: Int,
        private val color: Int,
        private val listener: MyButtonClickListener
    ) {
        private var pos = 0
        private var clickRegion: RectF? = null

        // thực hiện click
        fun onClick(x: Float, y: Float): Boolean {
            if (clickRegion != null && clickRegion!!.contains(x, y)) {
                listener.onClick(pos)
                return true
            }
            return false
        }

        // vẽ hình chữ nhật lên canvas có chứa chữ/hình ảnh được căn giữa
        fun onDraw(c: Canvas, rectF: RectF, pos: Int) {
            val p = Paint()
            p.color = color
            c.drawRect(rectF, p)

            // text
            p.color = Color.WHITE
            p.textSize = textSize.toFloat()

            val r = Rect()
            val cHeight = rectF.height()
            val cWidth = rectF.width()
            p.textAlign = Paint.Align.LEFT
            p.getTextBounds(text, 0, text.length, r)
            var x: Float
            var y: Float

            if (imageResId == 0) { // chỉ hiển thị text
                x = cWidth / 2f - r.width() / 2f - r.left
                y = cHeight / 2f + r.height() / 2f - r.bottom
                c.drawText(text, rectF.left + x, rectF.top + y, p)
            } else {
                val d = ContextCompat.getDrawable(context, imageResId)?.mutate()
                d?.let { drawable ->
                    androidx.core.graphics.drawable.DrawableCompat.setTint(drawable, Color.WHITE)
                    androidx.core.graphics.drawable.DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_ATOP)
                    val bitmap = drawableToBitmap(drawable)
                    val bitmapX = (rectF.left + rectF.right) / 2 - (bitmap.width.toFloat() / 2)
                    val bitmapY = (rectF.top + rectF.bottom) / 2 - (bitmap.height.toFloat() / 2)
                    c.drawBitmap(bitmap, bitmapX, bitmapY, p)
                }
            }
            clickRegion = rectF
            this.pos = pos
        }
    }

    // vẽ drawable lên bitmap
    private fun drawableToBitmap(d: Drawable): Bitmap {
        if (d is BitmapDrawable) {
            return d.bitmap
        }
        val bitmap = createBitmap(d.intrinsicWidth, d.intrinsicHeight)
        val canvas = Canvas(bitmap)
        d.setBounds(0, 0, canvas.width, canvas.height)
        d.draw(canvas)
        return bitmap
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    // xử lý trạng thái khi swipe
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.absoluteAdapterPosition
        viewHolder.itemView.setBackgroundResource(R.drawable.bg_group_swipe)
        if (swipePosition != pos) {
            removeQueue.add(swipePosition)
        }
        swipePosition = pos
        buttonList = if (buttonBuffer.containsKey(swipePosition)) {
            buttonBuffer[swipePosition] ?: ArrayList()
        } else {
            buttonList.clear()
            buttonList
        }
        buttonBuffer.clear()
        swipeThreshold = 0.5f * buttonList.size * buttonWidth
        recoverSwipedItem()
    }

    // phục hồi item đã bị swipe
    @Synchronized
    private fun recoverSwipedItem() {
        while (!removeQueue.isEmpty()) {
            val pos = removeQueue.poll()
            if (pos != null) {
                if (pos > -1) {
                    recyclerView.adapter?.notifyItemChanged(pos)
                }
            }
        }
    }

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return swipeThreshold
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 0.1f * defaultValue
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return 0.5f * defaultValue
    }

    // xử lý hiển thị button khi vuốt
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val pos = viewHolder.absoluteAdapterPosition
        var translationX = dX
        val itemView = viewHolder.itemView
        if (pos < 0) {
            swipePosition = pos
            return
        }
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                val buffer: MutableList<MyButton> =
                    if (!buttonBuffer.containsKey(pos)) {
                        val temp = ArrayList<MyButton>()
                        instantiateMyButton(viewHolder, temp)
                        buttonBuffer[pos] = temp
                        temp
                    } else {
                        buttonBuffer[pos]!!
                    }
                translationX = dX * buffer.size * buttonWidth / itemView.width
                drawButton(c, itemView, buffer, pos, translationX)
            } else if (dX == 0f) {
                viewHolder.itemView.setBackgroundResource(R.drawable.bg_group)
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive)
    }

    // vẽ các nút
    private fun drawButton(
        c: Canvas,
        itemView: View,
        buffer: List<MyButton>,
        pos: Int,
        translationX: Float
    ) {
        var right = itemView.right.toFloat()
        val dButtonWidth = -1 * translationX / buffer.size
        for (button in buffer) {
            val left = right - dButtonWidth
            button.onDraw(c, RectF(left, itemView.top.toFloat(), right, itemView.bottom.toFloat()), pos)
            right = left
        }
    }

    abstract fun instantiateMyButton(viewHolder: RecyclerView.ViewHolder, buffer: MutableList<MyButton>)
}