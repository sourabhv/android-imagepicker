package me.psycoder.imagepicker;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by sourabh on 23/03/16.
 */
public class ImagePickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = ImagePickerAdapter.class.getSimpleName();

    private static final int TYPE_BUCKET = 0;
    private static final int TYPE_IMAGE = 1;

    private boolean mNoItems;
    private boolean mBucketMode;
    private int mMaxCount, mImageSize;
    private String mActiveBucketId;
    private Context mContext;
    private GridLayoutManager mGridLayoutManager;
    private ItemAnimator mXItemAnimator;
    private OnImageSelectListener mOnImageSelectListener;
    private OnImagePreviewListener mOnImagePreviewListener;
    private ArrayList<DiskImage.Bucket> mBuckets;
    private ArrayList<DiskImage> mSelectedImages;

    public ImagePickerAdapter(Context context, int maxCount, GridLayoutManager layoutManager,
                              ItemAnimator animator, OnImageSelectListener selectListener,
                              OnImagePreviewListener previewListener) {
        mContext = context;
        mMaxCount = maxCount;
        mGridLayoutManager = layoutManager;
        mXItemAnimator = animator;
        mNoItems = false;
        mBucketMode = true;
        mActiveBucketId = "";
        mOnImageSelectListener = selectListener;
        mOnImagePreviewListener = previewListener;
        mImageSize = mContext.getResources().getDisplayMetrics().widthPixels / 3;
        mBuckets = new ArrayList<>();
        mSelectedImages = new ArrayList<>();
    }

    public boolean isBucketMode() {
        return mBucketMode;
    }

    public ArrayList<DiskImage.Bucket> getBuckets() {
        return mBuckets;
    }

    public void setBuckets(ArrayList<DiskImage.Bucket> buckets) {
        mBucketMode = true;
        mBuckets = buckets;
    }

    public ArrayList<DiskImage> getSelectedImages() {
        return mSelectedImages;
    }

    public void setSelectedImages(ArrayList<DiskImage> images) {
        mSelectedImages.clear();
        mSelectedImages.addAll(images);
    }

    public void clearSelectedImages() {
        mSelectedImages.clear();
    }

    public void setImages(ArrayList<DiskImage> images) {
        mBucketMode = true;
        mBuckets.clear();
        ArrayMap<String, DiskImage.Bucket> bucketMap = new ArrayMap<>();
        for (DiskImage image : images) {
            DiskImage.Bucket bucket = bucketMap.get(image.bucketId);
            if (bucket == null) {
                bucket = new DiskImage.Bucket(image.bucketId, image.bucketName);
                mBuckets.add(bucket);
                bucketMap.put(image.bucketId, bucket);
            }
            bucket.images.add(image);
        }
    }

    private ArrayList<DiskImage> getActiveBucketImages() {
        for (DiskImage.Bucket bucket : mBuckets)
            if (bucket.id.equals(mActiveBucketId))
                return bucket.images;
        return null;
    }

    public void showBuckets() {
        // set animation direction
        mXItemAnimator.setReverse(true);

        // remove bucket images and animate removal
        mNoItems = true;
        ArrayList<DiskImage> images = getActiveBucketImages();
        int imageSize = images != null ? images.size() : 0;
        notifyItemRangeRemoved(0, imageSize);

        // set span count
        final int albumsPerRow = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 3;
        mGridLayoutManager.setSpanCount(albumsPerRow);

        // add buckets & animate all additions
        mNoItems = false;
        mBucketMode = true;
        mActiveBucketId = "";
        notifyItemRangeInserted(0, mBuckets.size());
    }

    private void showBucketImages(String bucketId) {
        // set animation direction
        mXItemAnimator.setReverse(false);

        // remove all items & animate all removal
        mNoItems = true;
        notifyItemRangeRemoved(0, mBuckets.size());

        // set span count
        final int imagesPerRow = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 3 : 5;
        mGridLayoutManager.setSpanCount(imagesPerRow);

        // set new bucket images and animate addition
        mNoItems = false;
        mBucketMode = false;
        mActiveBucketId = bucketId;
        ArrayList<DiskImage> images = getActiveBucketImages();
        int imageSize = images != null ? images.size() : 0;
        notifyItemRangeInserted(0, imageSize);
    }

    @Override
    public int getItemCount() {
        if (mNoItems) {
            return 0;
        } else if (mBucketMode) {
            return mBuckets.size();
        } else {
            ArrayList<DiskImage> images = getActiveBucketImages();
            return images != null ? images.size() : 0;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mBucketMode ? TYPE_BUCKET : TYPE_IMAGE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_BUCKET: return new BucketHolder(inflater.inflate(R.layout.list_item_image_picker_bucket, parent, false));
            case TYPE_IMAGE: return new ImageHolder(inflater.inflate(R.layout.list_item_image_picker_image, parent, false));
            default: return null;
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof BucketHolder) {
            BucketHolder holder = (BucketHolder) viewHolder;
            final DiskImage.Bucket bucket = mBuckets.get(position);
            Glide.with(mContext).load(bucket.images.get(0).path)
                    .thumbnail(.2F)
                    .override(mImageSize, mImageSize)
                    .placeholder(R.drawable.placeholder_image_picker)
                    .centerCrop().dontAnimate().into(holder.mImage);
            holder.mTitle.setText(bucket.name);
            holder.mSubtitle.setText(mContext.getResources().getQuantityString(R.plurals.images, bucket.images.size(), bucket.images.size()));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showBucketImages(bucket.id);
                }
            });

        } else if (viewHolder instanceof ImageHolder) {
            final ImageHolder holder = (ImageHolder) viewHolder;
            final ArrayList<DiskImage> images = getActiveBucketImages();
            DiskImage image = images != null ? images.get(position) : null;
            if (images == null || image == null) return; // should never happen
            Glide.with(mContext).load(image.path).thumbnail(0.2F)
                    .placeholder(R.drawable.placeholder_image_picker)
                    .centerCrop().dontAnimate().into(holder.mImage);
            float scale = mSelectedImages.contains(image) ? 0.85F : 1F;
            float alpha = mSelectedImages.contains(image) ? 1F : 0F;
            holder.mImage.setScaleX(scale);
            holder.mImage.setScaleY(scale);
            holder.mOverlay.setScaleX(scale);
            holder.mOverlay.setScaleY(scale);
            holder.mOverlay.setAlpha(alpha);
            holder.mTick.setAlpha(alpha);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    boolean selected = !mSelectedImages.contains(images.get(holder.getAdapterPosition()));

                    // cannot select more than maxCount
                    if (selected && mMaxCount == mSelectedImages.size()) return;

                    if (selected) mSelectedImages.add(images.get(holder.getAdapterPosition()));
                    else mSelectedImages.remove(images.get(holder.getAdapterPosition()));

                    // call listener after array-list is updated
                    mOnImageSelectListener.onImageSelect();

                    ObjectAnimator imageXAnimator, imageYAnimator, overlayXAnimator, overlayYAnimator,
                            tickAnimator, overlayAnimator;
                    float startScale = selected ? 1F : 0.85F, endScale = selected ? 0.85F : 1F,
                            startAlpha = selected ? 0F : 1F, endAlpha = selected ? 1F : 0F;
                    imageXAnimator = ObjectAnimator.ofFloat(holder.mImage, "scaleX", startScale, endScale);
                    imageYAnimator = ObjectAnimator.ofFloat(holder.mImage, "scaleY", startScale, endScale);
                    overlayXAnimator = ObjectAnimator.ofFloat(holder.mOverlay, "scaleX", startScale, endScale);
                    overlayYAnimator = ObjectAnimator.ofFloat(holder.mOverlay, "scaleY", startScale, endScale);
                    tickAnimator = ObjectAnimator.ofFloat(holder.mTick, "alpha", startAlpha, endAlpha);
                    overlayAnimator = ObjectAnimator.ofFloat(holder.mOverlay, "alpha", startAlpha, endAlpha);

                    imageXAnimator.setDuration(200); imageYAnimator.setDuration(200);
                    overlayXAnimator.setDuration(200); overlayYAnimator.setDuration(200);
                    tickAnimator.setDuration(200); overlayAnimator.setDuration(200);

                    imageXAnimator.start(); imageYAnimator.start();
                    overlayXAnimator.start(); overlayYAnimator.start();
                    tickAnimator.start(); overlayAnimator.start();
                }
            });

            // show preview on long press
            holder.mIsLongPressed = false;
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    holder.mIsLongPressed = true;
                    mOnImagePreviewListener.onShowPreview(images.get(holder.getAdapterPosition()));
                    return true;
                }
            });

            // hide preview on action_up & action_cancel
            holder.itemView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (holder.mIsLongPressed && (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)) {
                        holder.mIsLongPressed = false;
                        mOnImagePreviewListener.onHidePreview(images.get(holder.getAdapterPosition()));
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    public interface OnImageSelectListener {
        void onImageSelect();
    }

    public interface OnImagePreviewListener {
        void onShowPreview(DiskImage image);
        void onHidePreview(DiskImage image);
    }

    public static class BucketHolder extends RecyclerView.ViewHolder {
        public ImageView mImage;
        public TextView mTitle;
        public TextView mSubtitle;
        public BucketHolder(View view) {
            super(view);
            mImage = (ImageView) view.findViewById(R.id.image);
            mTitle = (TextView) view.findViewById(R.id.title);
            mSubtitle = (TextView) view.findViewById(R.id.subtitle);
        }
    }

    public static class ImageHolder extends RecyclerView.ViewHolder {
        public boolean mIsLongPressed;
        public ImageView mImage;
        public View mOverlay;
        public ImageView mTick;
        public ImageHolder(View view) {
            super(view);
            mImage = (ImageView) view.findViewById(R.id.image);
            mOverlay = view.findViewById(R.id.overlay);
            mTick = (ImageView) view.findViewById(R.id.tick);
        }
    }

}

