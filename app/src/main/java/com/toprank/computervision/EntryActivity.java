package com.toprank.computervision;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class EntryActivity extends Activity {
    private Button btnTrain;
    private Button btnRecognize;
    private Button btnProcess;
    //btnCleanUp, btnProcess
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        btnTrain = (Button) findViewById(R.id.btnTrain);
        btnRecognize = (Button) findViewById(R.id.btnRecognize);
        btnProcess = (Button) findViewById(R.id.btnProcess);
        //btnCleanUp, btnProcess

        btnTrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                train();
            }
        });
        btnRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recognize();
            }
        });
        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                process();
            }
        });


        btnTrain.setEnabled(false);
        btnRecognize.setEnabled(false);
        d("Copying assets");
        new Util(this).copyAssets(new Util.ResultListener() {
            @Override
            public void successHook() {
                d("Assets copied");
                btnTrain.setEnabled(true);
                btnRecognize.setEnabled(true);
            }
        });
    }

    private void disableButtons(){
        btnTrain.setEnabled(false);
        btnRecognize.setEnabled(false);
        btnProcess.setEnabled(false);
    }

    private void enableButtons(){
        btnTrain.setEnabled(true);
        btnRecognize.setEnabled(true);
        btnProcess.setEnabled(true);
    }

    private void process(){
        disableButtons();
        new AsyncTask(){

            @Override
            protected Object doInBackground(Object[] objects) {
                new Util(EntryActivity.this).learn();
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                enableButtons();
            }
        }.execute();
    }

    private void cleanUp(){
        disableButtons();
        new AsyncTask(){

            @Override
            protected Object doInBackground(Object[] objects) {
                new Util(EntryActivity.this).cleanUp();
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                enableButtons();
            }
        }.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_entry, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_purge_database) {
            cleanUp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void train(){
        Intent intent = new Intent(EntryActivity.this, TrainActivity.class);
        startActivity(intent);
    }

    private void recognize(){
        Intent intent = new Intent(EntryActivity.this, RecognizeActivity.class);
        startActivity(intent);
    }

    private void d(String s){
        Log.d("EntryActivity", s);
    }
}
