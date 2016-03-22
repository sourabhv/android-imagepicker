package me.psycoder.imagepicker;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;

public class ImagePickerActivity extends AppCompatActivity {

    public static final String EXTRA_MAX_IMAGES = "extra_max_images";
    public static final String EXTRA_IMAGES = "extra_images";
    private static final String BUNDLE_BUCKETS = "bundle_images";
    private static final String BUNDLE_SELECTED_IMAGES = "bundle_selected_images";

    public static final int REQUEST_CODE_ASK_READ_EXTERNAL_STORAGE_PERMISSION = 11;

    private boolean mPreviewLoading;
    private int mMaxImages;
    private Context mContext;
    private ActionBar mActionBar;
    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private ImageView mPreviewImage;
    private ImagePickerAdapter mAdapter;
    private LayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_picker);
        mContext = ImagePickerActivity.this;
        mMaxImages = getIntent().getIntExtra(EXTRA_MAX_IMAGES, 1);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mPreviewImage = (ImageView) findViewById(R.id.preview_image);

        setSupportActionBar(mToolbar);
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setTitle(R.string.select_images);
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }

        mPreviewImage.setScaleX(.2F);
        mPreviewImage.setScaleY(.2F);
        mPreviewImage.setAlpha(0F);

        final int albumsPerRow = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 3;
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        mLayoutManager = new LayoutManager(mContext, albumsPerRow, displaymetrics.heightPixels / 2);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        ItemAnimator animator = new ItemAnimator();
        mRecyclerView.setItemAnimator(animator);
        mAdapter = new ImagePickerAdapter(mContext, mMaxImages, mLayoutManager, animator, mSelectListener, mPreviewListener);
        mRecyclerView.setAdapter(mAdapter);

        if (savedInstanceState == null) {
            boolean hasPermission = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 && hasPermission) {
                new LoadImageTask().execute();
            } else {
                ActivityCompat.requestPermissions(ImagePickerActivity.this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                        REQUEST_CODE_ASK_READ_EXTERNAL_STORAGE_PERMISSION);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_READ_EXTERNAL_STORAGE_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new LoadImageTask().execute();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter.getBuckets() != null)
            outState.putParcelableArrayList(BUNDLE_BUCKETS, mAdapter.getBuckets());
        if (mAdapter.getSelectedImages() != null)
            outState.putParcelableArrayList(BUNDLE_SELECTED_IMAGES, mAdapter.getSelectedImages());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            ArrayList<DiskImage.Bucket> buckets = savedInstanceState.getParcelableArrayList(BUNDLE_BUCKETS);
            ArrayList<DiskImage> selectedImages = savedInstanceState.getParcelableArrayList(BUNDLE_SELECTED_IMAGES);
            if (buckets != null) mAdapter.setBuckets(buckets);
            if (selectedImages != null) mAdapter.setSelectedImages(selectedImages);
            mAdapter.notifyDataSetChanged();
            mSelectListener.onImageSelect();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_picker, menu);
        MenuItem reset = menu.findItem(R.id.action_reset);
        if (reset != null && mAdapter != null  && mAdapter.getSelectedImages().size() == 0)
            reset.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (mAdapter != null && mAdapter.getSelectedImages().size() > 0) {
                Intent data = new Intent();
                data.putParcelableArrayListExtra(EXTRA_IMAGES, mAdapter.getSelectedImages());
                setResult(Activity.RESULT_OK, data);
                finish();
            } else {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
            return true;
        } else if (item.getItemId() == R.id.action_reset) {
            if (mAdapter != null){
                mAdapter.clearSelectedImages();
                mAdapter.notifyDataSetChanged();
                mSelectListener.onImageSelect();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (mAdapter.isBucketMode())
            super.onBackPressed();
        else
            mAdapter.showBuckets();
    }

    private ImagePickerAdapter.OnImageSelectListener mSelectListener = new ImagePickerAdapter.OnImageSelectListener() {
        @Override
        public void onImageSelect() {
            int size = mAdapter.getSelectedImages().size();
            if (size == 0) {
                mActionBar.setTitle(R.string.select_images);
            } else {
                mActionBar.setTitle(getString(R.string.image_selection_message, size, mMaxImages));
            }
            mActionBar.setHomeAsUpIndicator(size == 0 ? 0 : R.drawable.ic_tick);
            invalidateOptionsMenu();
        }
    };

    ImagePickerAdapter.OnImagePreviewListener mPreviewListener = new ImagePickerAdapter.OnImagePreviewListener() {
        @Override
        public void onShowPreview(DiskImage image) {
            mLayoutManager.setCanScrollVertically(false);
            mPreviewLoading = true;
            Glide.with(mContext)
                    .load(image.path).fitCenter().dontAnimate()
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            return false;
                        }
                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            mPreviewLoading = false;
                            ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(mPreviewImage, "scaleX", .2F, 1F);
                            ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(mPreviewImage, "scaleY", .2F, 1F);
                            ObjectAnimator alphaXAnimator = ObjectAnimator.ofFloat(mPreviewImage, "alpha", 0F, 1F);
                            scaleXAnimator.setInterpolator(new OvershootInterpolator());
                            scaleYAnimator.setInterpolator(new OvershootInterpolator());
                            scaleXAnimator.setDuration(400); scaleYAnimator.setDuration(400); alphaXAnimator.setDuration(400);
                            scaleXAnimator.start(); scaleYAnimator.start(); alphaXAnimator.start();
                            return false;
                        }
                    })
                    .into(mPreviewImage);
        }

        @Override
        public void onHidePreview(DiskImage image) {
            mLayoutManager.setCanScrollVertically(true);
            if (mPreviewLoading) Glide.clear(mPreviewImage);
            mPreviewLoading = false;
            ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(mPreviewImage, "scaleX", 1F, .2F);
            ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(mPreviewImage, "scaleY", 1F, .2F);
            ObjectAnimator alphaXAnimator = ObjectAnimator.ofFloat(mPreviewImage, "alpha", 1F, 0F);
            scaleXAnimator.setInterpolator(new AnticipateInterpolator());
            scaleYAnimator.setInterpolator(new AnticipateInterpolator());
            scaleXAnimator.setDuration(400); scaleYAnimator.setDuration(400); alphaXAnimator.setDuration(400);
            scaleXAnimator.start(); scaleYAnimator.start(); alphaXAnimator.start();
        }
    };

    private class LoadImageTask extends AsyncTask<Void, Void, ArrayList<DiskImage>> {
        @Override
        protected ArrayList<DiskImage> doInBackground(Void... voids) {
            ArrayList<DiskImage> images = new ArrayList<>();
            String[] columns = {
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.BUCKET_ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.MIME_TYPE,
                    MediaStore.Images.ImageColumns.SIZE
            };
            String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC";
            Cursor imageCursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    columns, null, null, orderBy);
            if (imageCursor != null) {
                try {
                    while (imageCursor.moveToNext()) {
                        images.add(new DiskImage(imageCursor));
                    }
                } finally {
                    imageCursor.close();
                }
            }
            return images;
        }

        @Override
        protected void onPostExecute(ArrayList<DiskImage> images) {
            super.onPostExecute(images);
            mAdapter.setImages(images);
            mAdapter.notifyDataSetChanged();
        }
    }

}
