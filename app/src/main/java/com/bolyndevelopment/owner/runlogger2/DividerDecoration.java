package com.bolyndevelopment.owner.runlogger2;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

/**
 * Created by Bobby Jones on 9/15/2017.
 */

public class DividerDecoration extends RecyclerView.ItemDecoration {
    private static String TAG = "Div Dec";
    private Drawable mDivider, rightDiv, leftDiv;


    public DividerDecoration(Drawable left, Drawable right) {
        this.rightDiv = right;
        this.leftDiv = left;
    }

    /**
     * Draws horizontal or vertical dividers onto the parent RecyclerView.
     *
     * @param canvas The {@link Canvas} onto which dividers will be drawn
     * @param parent The RecyclerView onto which dividers are being added
     * @param state The current RecyclerView.State of the RecyclerView
     */

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        //leftDiv = parent.getContext().getResources().getDrawable(R.drawable.left_divider);
        //rightDiv = parent.getContext().getResources().getDrawable(R.drawable.right_divider);
        int n6px = (int) Utils.convertDpToPixel(80f);
        int parLeft = parent.getPaddingLeft();
        int parentLeft = parent.getPaddingLeft() + n6px;
        int parentRight = parent.getWidth() - parent.getPaddingRight();
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int parentTop = child.getBottom() + params.bottomMargin;
            int parentBottom = parentTop + rightDiv.getIntrinsicHeight();
            rightDiv.setBounds(parentLeft, parentTop, parentRight, parentBottom);
            leftDiv.setBounds(parLeft, parentTop, n6px, parentBottom);
            rightDiv.draw(canvas);
            leftDiv.draw(canvas);
        }
    }

    /**
     * Determines the size and location of offsets between items in the parent
     * RecyclerView.
     *
     * @param outRect The {@link Rect} of offsets to be added around the child
     *                view
     * @param view The child view to be decorated with an offset
     * @param parent The RecyclerView onto which dividers are being added
     * @param state The current RecyclerView.State of the RecyclerView
     */

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (parent.getChildAdapterPosition(view) == 0) {
            return;
        }
        outRect.top = rightDiv.getIntrinsicHeight();
    }

    /**
     * Adds dividers to a RecyclerView with a LinearLayoutManager or its
     * subclass oriented horizontally.
     *
     * @param canvas The {@link Canvas} onto which horizontal dividers will be
     *               drawn
     * @param parent The RecyclerView onto which horizontal dividers are being
     *               added
     */
    private void drawHorizontalDividers(Canvas canvas, RecyclerView parent) {
        Log.d(TAG, "drawHorizontalDividers");
        int parentTop = parent.getPaddingTop();
        int parentBottom = parent.getHeight() - parent.getPaddingBottom();
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int parentLeft = child.getRight() + params.rightMargin;
            int parentRight = parentLeft + mDivider.getIntrinsicWidth();
            mDivider.setBounds(parentLeft, parentTop, parentRight, parentBottom);
            mDivider.draw(canvas);
        }
    }

    /**
     * Adds dividers to a RecyclerView with a LinearLayoutManager or its
     * subclass oriented vertically.
     *
     * @param canvas The {@link Canvas} onto which vertical dividers will be
     *               drawn
     * @param parent The RecyclerView onto which vertical dividers are being
     *               added
     */
    private void drawVerticalDividers(Canvas canvas, RecyclerView parent) {
        Drawable leftDiv = parent.getContext().getResources().getDrawable(R.drawable.left_divider);
        Drawable rightDiv = parent.getContext().getResources().getDrawable(R.drawable.right_divider);
        int n6px = (int) Utils.convertDpToPixel(96f);
        int parLeft = parent.getPaddingLeft();
        int parRight = n6px;
        int parentLeft = parent.getPaddingLeft() + n6px;
        int parentRight = parent.getWidth() - parent.getPaddingRight();
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            int parentTop = child.getBottom() + params.bottomMargin;
            int parentBottom = parentTop + rightDiv.getIntrinsicHeight();
            rightDiv.setBounds(parentLeft, parentTop, parentRight, parentBottom);
            leftDiv.setBounds(parLeft, parentTop, parRight, parentBottom);
            rightDiv.draw(canvas);
            leftDiv.draw(canvas);
        }
    }
}