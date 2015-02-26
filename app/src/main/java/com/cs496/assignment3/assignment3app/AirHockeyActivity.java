package com.cs496.assignment3.assignment3app;

/* Code for this Activity was gathered largely while working through
** the first half of "OpenGL ES 2 for Android: A Quick-Start Guide" by
** Kevin Brothaler. Accompanying Renderer code, as well as the code in
** the "data", "objects", "programs", and "util" packages all fall under
** code produced with the aid of this tutorial for the intent of integrating
** acquired animation skills using OpenGL into a single app for this assignment
 */

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class AirHockeyActivity extends ActionBarActivity {
    private GLSurfaceView glSurfaceView;
    private boolean rendererSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glSurfaceView = new GLSurfaceView(this);
        final AirHockeyRenderer airHockeyRenderer = new AirHockeyRenderer(this);

        //Check if system supports OpenGL ES 2.0
        ActivityManager activityManager =
                (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();

        //Check the same on emulators
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                && (Build.FINGERPRINT.startsWith("generic") || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk") || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")));

        if(supportsEs2){
            //Request an OpenGL ES 2.0 compatible context
            glSurfaceView.setEGLContextClientVersion(2);

            //Assign renderer
            //glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
            glSurfaceView.setRenderer(airHockeyRenderer);
            rendererSet = true;
        } else{
            Toast.makeText(this, "This device does not support OpenGL ES 2.0",
                    Toast.LENGTH_LONG).show();
            return;
        }

        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if(event != null){
                    //Convert touch coords into normalized device coords, keeping in mind that
                    //Android's Y coords are inverted
                    final float normalizedX = (event.getX() / (float) view.getWidth()) * 2 - 1;
                    final float normalizedY = -((event.getY() / (float) view.getHeight()) * 2 - 1);

                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        glSurfaceView.queueEvent(new Runnable(){
                            @Override
                            public void run(){
                                airHockeyRenderer.handleTouchPress(normalizedX, normalizedY);
                            }
                        });
                    } else if(event.getAction() == MotionEvent.ACTION_MOVE){
                        glSurfaceView.queueEvent(new Runnable() {
                            @Override
                            public void run() {
                                airHockeyRenderer.handleTouchDrag(normalizedX, normalizedY);
                            }
                        });
                    }

                    return true;
                } else{
                    return false;
                }
            }
        });
        setContentView(glSurfaceView);
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(rendererSet){
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(rendererSet){
            glSurfaceView.onResume();
        }
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
}
