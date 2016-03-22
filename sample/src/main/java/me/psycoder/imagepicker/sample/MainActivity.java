package me.psycoder.imagepicker.sample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import me.psycoder.imagepicker.DiskImage;
import me.psycoder.imagepicker.ImagePickerActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_IMAGES = 1;

    private Button mPickImages;
    private TextView mImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPickImages = (Button) findViewById(R.id.pick_images);
        mImages = (TextView) findViewById(R.id.images);

        mPickImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ImagePickerActivity.class);
                intent.putExtra(ImagePickerActivity.EXTRA_MAX_IMAGES, 5);
                startActivityForResult(intent, REQUEST_CODE_PICK_IMAGES);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_IMAGES && resultCode == RESULT_OK) {
            ArrayList<DiskImage> images = data.getParcelableArrayListExtra(ImagePickerActivity.EXTRA_IMAGES);
            mImages.setText(TextUtils.join("\n", images));
        }
    }
}
