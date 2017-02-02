package com.toprank.computervision;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.IOException;

public class RecognizeActivity extends Activity {
    private FrameLayout layout;
    private FaceView faceView;
    private Preview mPreview;
    private TextView tvConfidence;
    private TextView tvPerson;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);
        layout = (FrameLayout) findViewById(R.id.previewContainer);

        tvConfidence = (TextView) findViewById(R.id.tvConfidence);
        tvPerson = (TextView) findViewById(R.id.tvPerson);
        try {
            faceView = new RecognitionFaceView(this);
            mPreview = new Preview(this, faceView);
            layout.addView(mPreview);
            layout.addView(faceView);
        } catch (IOException e) {
            e.printStackTrace();
            new AlertDialog.Builder(this).setMessage(e.getMessage()).create().show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recognize, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_front_facing) {
            useFrontCam();
            return true;
        }

        if (id == R.id.action_rear) {
            useBackCam();
            return true;
        }

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void useFrontCam(){
        faceView.setCameraDirection(CameraDirection.FRONT_FACING);
        mPreview.setCameraDirection(CameraDirection.FRONT_FACING);
    }

    private void useBackCam(){
        faceView.setCameraDirection(CameraDirection.REAR_FACING);
        mPreview.setCameraDirection(CameraDirection.REAR_FACING);
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        if(faceView != null){
            faceView.setMyRotation(degrees);
        }
    }

    public void showRecognitionInfo(RecognitionResult info){
        if(info == null){
            return;
        }
        tvPerson.setText(info.p == null ? "<<no match>>" : info.p.getName());
        tvConfidence.setText(info.p == null ? "<<no match>>" : String.valueOf(info.confidence));
    }
}
