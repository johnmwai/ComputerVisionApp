package com.toprank.computervision.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.toprank.computervision.database.ComputerVisionDbContract.IplImage;
import com.toprank.computervision.database.ComputerVisionDbContract.Mat;
import com.toprank.computervision.database.ComputerVisionDbContract.Person;

/**
 * Created by john on 7/24/15.
 */
public class ComputerVisionDbHelper extends SQLiteOpenHelper {


    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 8;
    public static final String DATABASE_NAME = "ComputerVision.db";

    public ComputerVisionDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Person.SQL_CREATE_TABLE);
        db.execSQL(Mat.SQL_CREATE_TABLE);
        db.execSQL(IplImage.SQL_CREATE_TABLE);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(Person.SQL_DELETE_TABLE);
        db.execSQL(Mat.SQL_DELETE_TABLE);
        db.execSQL(IplImage.SQL_DELETE_TABLE);

        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
