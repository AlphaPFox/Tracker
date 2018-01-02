package br.gov.dpf.tracker.Components;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

public class GridAutoLayoutManager extends GridLayoutManager
{
    private int minColumnWidth;
    private int minRowHeight;
    private DisplayMetrics mMetrics;

    public GridAutoLayoutManager(RecyclerView recyclerView, int minColumnWidth, int minRowHeight)
    {
        /* Initially set spanCount to 1, will be changed automatically later. */
        super(recyclerView.getContext(), 1);
        this.minColumnWidth = minColumnWidth;
        this.minRowHeight = minRowHeight;
        this.mMetrics = Resources.getSystem().getDisplayMetrics();
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state)
    {
        int width = getWidth();
        int height = getHeight();

        if (minColumnWidth > 0 && width > 0 && height > 0 && getItemCount() > 1) {
            int totalSpace;

            if (getOrientation() == VERTICAL) {
                totalSpace = width - getPaddingRight() - getPaddingLeft();
            } else {
                totalSpace = height - getPaddingTop() - getPaddingBottom();
            }

            int spanCount = Math.max(1, (int) Math.floor(totalSpace / minColumnWidth / mMetrics.density));

            setSpanCount(spanCount);
        }

        //Call super method
        super.onLayoutChildren(recycler, state);
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return spanLayoutSize(super.generateDefaultLayoutParams());
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return spanLayoutSize(super.generateLayoutParams(c, attrs));
    }

    @Override
    public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return spanLayoutSize(super.generateLayoutParams(lp));
    }

    private RecyclerView.LayoutParams spanLayoutSize(RecyclerView.LayoutParams layoutParams){

        //Get optimal dimensions for recycler view items
        int size[] = getOptimalDimension();

        layoutParams.width = size[0];
        layoutParams.height = size[1];

        //Get default margin
        int margin = (int) (mMetrics.density * 8);

        //Set layout margin
        layoutParams.setMargins(margin, margin, margin, margin);

        return layoutParams;
    }

    public int[] getOptimalDimension()
    {
        int dimen[] = {minColumnWidth, minRowHeight};

        int columnCount = Math.max((int) (mMetrics.widthPixels / mMetrics.density) / dimen[0], 1);
        int rowCount = Math.max((int) (mMetrics.heightPixels / mMetrics.density) / dimen[1], 1);

        dimen[0] += ((mMetrics.widthPixels / mMetrics.density) - columnCount * dimen[0]) / columnCount - 16 - (columnCount - 1) * 4;
        dimen[1] += ((mMetrics.heightPixels / mMetrics.density) - rowCount * dimen[1]) / rowCount - 16 - (100 / rowCount);

        dimen[0] = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dimen[0], mMetrics);
        dimen[1] = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dimen[1], mMetrics);

        return dimen;
    }

    public void resizeOnOrientationChange()
    {
        for(int i = 0; i < getChildCount(); i++)
        {
            int dimen[] = getOptimalDimension();
            View item = getChildAt(i);

            ViewGroup.LayoutParams layout = item.getLayoutParams();
            layout.width = dimen[0];
            layout.height = dimen[1];

            item.setLayoutParams(layout);
            item.requestLayout();
        }
    }
}
