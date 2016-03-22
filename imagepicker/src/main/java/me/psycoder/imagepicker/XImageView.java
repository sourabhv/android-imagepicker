package me.psycoder.imagepicker;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by sourabh on 25/03/16.
 */
public class XImageView extends ImageView {

    public float mAspectRatio = 1F;

    public XImageView(Context context) {
        this(context, null);
    }

    public XImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (attrs != null) {
            TypedArray array = null;
            try {
                array = context.obtainStyledAttributes(attrs, R.styleable.XImageView);
                mAspectRatio = array.getFloat(R.styleable.XImageView_view_AspectRatio, 1F);
            } finally {
                if (array != null)
                    array.recycle();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredHeight = (int) (MeasureSpec.getSize(widthMeasureSpec) / mAspectRatio);
        int measureMode = MeasureSpec.getMode(heightMeasureSpec);
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(measuredHeight, measureMode);
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

}
