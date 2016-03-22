package me.psycoder.imagepicker;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by sourabh on 23/03/16.
 */
public class DiskImage implements Parcelable {

    public int size;
    public String path, mimeType;
    String bucketName, bucketId;

    public DiskImage(Cursor cursor) {
        bucketName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME));
        bucketId = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID));
        path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));
        size = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.ImageColumns.SIZE));
    }

    protected DiskImage(Parcel in) {
        size = in.readInt();
        bucketName = in.readString();
        bucketId = in.readString();
        path = in.readString();
        mimeType = in.readString();
    }

    public static final Creator<DiskImage> CREATOR = new Creator<DiskImage>() {
        @Override
        public DiskImage createFromParcel(Parcel in) {
            return new DiskImage(in);
        }

        @Override
        public DiskImage[] newArray(int size) {
            return new DiskImage[size];
        }
    };

    @Override
    public String toString() {
        return path.substring(path.lastIndexOf(File.separator) + 1, path.length());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(size);
        dest.writeString(bucketName);
        dest.writeString(bucketId);
        dest.writeString(path);
        dest.writeString(mimeType);
    }

    public static class Bucket implements Parcelable {

        public String id, name;
        public ArrayList<DiskImage> images;

        public Bucket(String id, String name) {
            this.id = id;
            this.name = name;
            images = new ArrayList<>();
        }

        protected Bucket(Parcel in) {
            id = in.readString();
            name = in.readString();
            images = in.createTypedArrayList(DiskImage.CREATOR);
        }

        public static final Creator<Bucket> CREATOR = new Creator<Bucket>() {
            @Override
            public Bucket createFromParcel(Parcel in) {
                return new Bucket(in);
            }

            @Override
            public Bucket[] newArray(int size) {
                return new Bucket[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(id);
            parcel.writeString(name);
            parcel.writeTypedList(images);
        }
    }

}

