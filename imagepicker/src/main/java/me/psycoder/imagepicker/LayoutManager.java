package me.psycoder.imagepicker;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Created by sourabh on 24/03/16.
 */
public class LayoutManager extends GridLayoutManager {

    private int mExtraLayoutSpace = -1;
    private boolean mCanScrollVertically = true;

    public LayoutManager(Context context, int spanCount, int extraLayoutSpace) {
        super(context, spanCount);
        mExtraLayoutSpace = extraLayoutSpace;
    }

    @Override
    protected int getExtraLayoutSpace(RecyclerView.State state) {
        if (mExtraLayoutSpace > 0) return mExtraLayoutSpace;
        return super.getExtraLayoutSpace(state);
    }

    @Override
    public boolean canScrollVertically() {
        return mCanScrollVertically && super.canScrollVertically();
    }

    public void setCanScrollVertically(boolean canScroll) {
        mCanScrollVertically = canScroll;
    }

}
