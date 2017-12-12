package com.pedromr.apps.fridgelist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.content.pm.PackageManager;

@RequiresApi(api = Build.VERSION_CODES.M)
public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    private static final String PREF_FILEPATH = "filePath";
    private static final String PREF_DOODLES = "doodle";
    private static final String LOG_TAG = "FridgeList";
    public static final int REQUEST_CAMERA_CODE = 10001;
    String mCurrentPhotoPath;
    boolean imageDrawn = false;
    DoodleCanvas doodleCanvas;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        doodleCanvas = findViewById(R.id.doodle);
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        loadUserPreferences();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (!imageDrawn && hasFocus) {
            displayCurrentImage();
            imageDrawn = true;
        }
    }

    private void loadUserPreferences() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        if (sharedPref.contains(PREF_FILEPATH)) {
            mCurrentPhotoPath = sharedPref.getString(PREF_FILEPATH, null);
            Log.d(LOG_TAG, "Got sharedPref "+mCurrentPhotoPath);
        } else {
            Log.d(LOG_TAG, "No saved sharedPref");
        }

        if (sharedPref.contains(PREF_DOODLES)) {
            String doodleJSON = sharedPref.getString(PREF_DOODLES, null);
            Log.d(LOG_TAG, "got doodle JSON "+doodleJSON);
            doodleCanvas.loadJSON(doodleJSON);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_picture:
                takePicture(null);
                return true;
            case R.id.action_settings:
                saveDoodle();
                return true;
            case R.id.action_undo:
                undoDoodle();
                return true;
            case R.id.action_clear:
                clearDoodleCanvas();
                return true;
            case R.id.action_erase:
                doodleCanvas.setEraseMode(true);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void undoDoodle() {
        doodleCanvas.undo();
    }

    public void takePicture(View view) {
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_CODE);
            return; // once the request is granted we'll take the picture
        }
        dispatchTakePictureIntent();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_CODE:
                takePicture(null);
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    static final int REQUEST_TAKE_PHOTO = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(LOG_TAG, "Error creating image file", ex);
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.pedromr.apps.fridgelist",
                        photoFile);
                Log.d(LOG_TAG, "photoURI: "+photoURI);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            displayCurrentImage();
            clearDoodleCanvas();
        }
    }

    private void clearDoodleCanvas() {
        doodleCanvas.clear();
    }

    private void displayCurrentImage() {
        PackageManager pm = getPackageManager();

        if (mCurrentPhotoPath == null || mCurrentPhotoPath.isEmpty())
            return;

        ImageView mImageView = findViewById(R.id.imageView);
        if (mImageView == null)
            return;

        // Get the dimensions of the View
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoH = bmOptions.outWidth;
        int photoW = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap sourceBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);

//        mImageView.setImageBitmap(rotatedBitmap);
        doodleCanvas.setBitmap(rotatedBitmap);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        boolean res = sharedPref.edit().putString(PREF_FILEPATH, mCurrentPhotoPath).commit();
        Log.d(LOG_TAG, "Wrote sharedPref "+mCurrentPhotoPath+", result "+res);
    }

    public void saveDoodle() {
        String jsonData = doodleCanvas.saveAsJson();
        Log.d(LOG_TAG, "Saving data: "+jsonData);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        boolean res = sharedPref.edit().putString(PREF_DOODLES, jsonData).commit();
    }
}
