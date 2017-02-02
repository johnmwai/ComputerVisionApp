package com.toprank.computervision.database;

import android.provider.BaseColumns;

/**
 * Created by john on 7/24/15.
 */
public final class ComputerVisionDbContract {
    public static final String TEXT_TYPE = " TEXT";
    public static final String INTEGER_TYPE = " INTEGER";
    public static final String REAL_TYPE = " REAL";
    public static final String BLOB_TYPE = " BLOB";
    public static final String COMMA_SEP = ",";

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private ComputerVisionDbContract(){

    }

    /* Inner class that defines the table contents */
    public static abstract class Person implements BaseColumns {
        public static final String TABLE_NAME = "person";
        public static final String COLUMN_NAME_NAME = "name";



        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_NAME + TEXT_TYPE +
                        " )";

        public static final String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static final String SQL_DELETE_ALL_RECORDS =
                "DELETE FROM " + TABLE_NAME;
    }

    public static abstract class Mat implements BaseColumns {
        public static final String TABLE_NAME = "mat";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_ROWS = "rows";
        public static final String COLUMN_NAME_COLS = "cols";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_DATA = "data";



        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_ROWS + INTEGER_TYPE +COMMA_SEP +
                        COLUMN_NAME_COLS + INTEGER_TYPE +COMMA_SEP +
                        COLUMN_NAME_TYPE + INTEGER_TYPE +COMMA_SEP +
                        COLUMN_NAME_DATA + BLOB_TYPE +
                        " )";

        public static final String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static final String SQL_DELETE_ALL_RECORDS =
                "DELETE FROM " + TABLE_NAME;
    }

    public static abstract class IplImage implements BaseColumns {
        public static final String TABLE_NAME = "ipl_image";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_PERSON = "person";
        public static final String COLUMN_NAME_WIDTH = "width";
        public static final String COLUMN_NAME_HEIGHT = "height";
        public static final String COLUMN_NAME_DEPTH = "depth";
        public static final String COLUMN_NAME_CHANNELS = "channels";
        public static final String COLUMN_NAME_DATA = "data";

        public static final String SQL_CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                        COLUMN_NAME_PERSON + INTEGER_TYPE + COMMA_SEP +
                        COLUMN_NAME_HEIGHT + INTEGER_TYPE +COMMA_SEP +
                        COLUMN_NAME_WIDTH + INTEGER_TYPE +COMMA_SEP +
                        COLUMN_NAME_DEPTH + INTEGER_TYPE +COMMA_SEP +
                        COLUMN_NAME_CHANNELS + INTEGER_TYPE +COMMA_SEP +
                        COLUMN_NAME_DATA + BLOB_TYPE +
                        " )";

        public static final String SQL_DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        public static final String SQL_DELETE_ALL_RECORDS =
                "DELETE FROM " + TABLE_NAME;
    }


    //Person {String name, }
    //Mat {String name, int rows, int cols, int type, blob data}
    //IplImage {String name, int Person, int rows, int cols, int type, blob data}
}
