package com.toprank.computervision;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import java.io.IOException;

public class TrainActivity extends Activity {
    private Button btnTakeSnapshot;
    private Button btnStartTraining;
    private FrameLayout layout;
    private FaceView faceView;
    private Preview mPreview;
    private EditText etPersonName;
    private String personName = "";
    private boolean training = false;

//    private static final String STATE_RETENTION_FRAGMENT = "STATE_FRAGMENT";
//    private StateRetentionFragment stateRetentionFragment;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);
        btnStartTraining = (Button) findViewById(R.id.btnStartTraining);
        btnTakeSnapshot = (Button) findViewById(R.id.btnTakeSnapshot);
        etPersonName = (EditText) findViewById(R.id.etPersonName);
        btnTakeSnapshot.setEnabled(false);
        btnStartTraining.setEnabled(false);
        btnStartTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                savedInstanceState.putBoolean("training", true);
                startTraining();
            }
        });
//        if(savedInstanceState.getBoolean("training", false)){
//            btnTakeSnapshot.setEnabled(true);
//        }

//        FragmentManager fm = getFragmentManager();
//        stateRetentionFragment = (StateRetentionFragment) fm.findFragmentByTag(STATE_RETENTION_FRAGMENT);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
//        if (stateRetentionFragment == null) {
//            stateRetentionFragment = new StateRetentionFragment();
//            fm.beginTransaction().add(stateRetentionFragment, STATE_RETENTION_FRAGMENT).commit();
//        }

        etPersonName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (etPersonName.getText().toString().trim().length() == 0){
                    btnStartTraining.setEnabled(false);
                }else{
                    if(!training){
                        btnStartTraining.setEnabled(true);
                    }
                }
            }
        });

        layout = (FrameLayout) findViewById(R.id.previewContainer);

        try {
            faceView = new FaceView(this);
            mPreview = new Preview(this, faceView);
            layout.addView(mPreview);
            layout.addView(faceView);

            btnTakeSnapshot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    faceView.takeSnapShot();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            new AlertDialog.Builder(this).setMessage(e.getMessage()).create().show();
        }
    }

    private void startTraining(){
        training = true;
        btnTakeSnapshot.setEnabled(true);
        personName = etPersonName.getText().toString();
        new Util(this).addPersonToDb(personName);
        btnStartTraining.setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_train, menu);
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
}
