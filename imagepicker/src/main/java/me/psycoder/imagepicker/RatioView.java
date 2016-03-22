package me.psycoder.imagepicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by sourabh on 24/03/16.
 */
public class RatioView extends FrameLayout {

    private float mAspectRatio;

    public RatioView(Context context) {
        super(context);
    }

    public RatioView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RatioView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RatioView);
        mAspectRatio = a.getFloat(R.styleable.RatioView_ratio, -1);
        a.recycle();
    }

    public void setRatio(int ratio) {
        mAspectRatio = ratio;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
        if (mAspectRatio != -1) {
            int measuredHeight = (int) (MeasureSpec.getSize(widthMeasureSpec) / mAspectRatio);
            int measureMode = MeasureSpec.getMode(heightMeasureSpec);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(measuredHeight, measureMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}