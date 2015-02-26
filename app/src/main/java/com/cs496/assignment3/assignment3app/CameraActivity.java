package com.cs496.assignment3.assignment3app;

//Reference code acquired and adapted from various sources as noted per method

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraActivity extends ActionBarActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView showPic;
    private String curPhotoPath;
    private Uri mPhotoUri;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int THUMBNAIL_SIZE = 64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Get intent
        Intent intent = getIntent();
        setContentView(R.layout.activity_camera);
        showPic = (ImageView)findViewById(R.id.showPic);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startCamera(View view) {
        Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Check that a camera activity is available to handle the intent
        if (takePic.resolveActivity(getPackageManager()) != null) {
            //Create file for photo
            mPhotoUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
            takePic.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
            Toast.makeText(this, "File " + mPhotoUri.getPath().toString() + " created", Toast.LENGTH_LONG).show();
            startActivityForResult(takePic, REQUEST_IMAGE_CAPTURE);
        }
    }

    //reference code http://www.androidhive.info/2013/09/android-working-with-camera-api/
    //if doesn't work, try http://blog-emildesign.rhcloud.com/?p=590
    //or http://stackoverflow.com/questions/4916159/android-get-thumbnail-of-image-on-sd-card-given-uri-of-original-image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            if(data!= null){
                Toast.makeText(this, "Image saved to:\n" + data.getData(), Toast.LENGTH_LONG).show();
                Bundle extras = data.getExtras();
                Bitmap bmpimg = (Bitmap)extras.get("data");
                showPic.setImageBitmap(bmpimg);
            } else {
                try{
                    //Save to gallery
                    galleryAddPic();

                    Toast.makeText(this, "Image " + mPhotoUri.getPath().toString() + " saved", Toast.LENGTH_LONG).show();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    //Reduce image to avoid memory exceptions for large images
                    options.inSampleSize = 8;

                    final Bitmap bmp = BitmapFactory.decodeFile(mPhotoUri.getPath(), options);
                    showPic.setImageBitmap(bmp);
                } catch(NullPointerException e){
                    Toast.makeText(this, "NullPointerException on retrieving image", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }

        } else if(resultCode == RESULT_CANCELED){
            //User cancelled image capture
            Toast.makeText(this, "Image capture cancelled", Toast.LENGTH_LONG).show();
        } else{
            //Image capture failed, advise user
            Toast.makeText(this, "Image capture failed; unknown reason", Toast.LENGTH_LONG).show();
        }
    }

    //Store file url for use after camera
    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        //save file url in bundle as it will be null on screen orientation change
        outState.putParcelable("mPhotoUri", mPhotoUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        //get file url
        mPhotoUri = savedInstanceState.getParcelable("mPhotoUri");
    }

    //Create file for saving images
    private File getOutputMediaFile(int type){
        File mediaFile;
        //check that SDcard is mounted
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "AppDirectory");
            //create directory if it doesn't exist
            if(!mediaStorageDir.exists()){
                if(!mediaStorageDir.mkdirs()){
                    Toast.makeText(this, "Failed to create storage directory", Toast.LENGTH_LONG);
                    return null;
                }
            }
            //create a media file
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            if(type == MEDIA_TYPE_IMAGE){
                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"
                        + timeStamp + ".jpg");
            } else if(type == MEDIA_TYPE_VIDEO){
                mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_"
                        + timeStamp + ".mp4");
            } else{
                return null;
            }
        } else{
            Toast.makeText(this, "Cannot use storage media", Toast.LENGTH_LONG).show();
            return null;
        }
        return mediaFile;
    }

    private Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    //Saves the photo to the gallery
    private void galleryAddPic(){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = mPhotoUri;
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
}
