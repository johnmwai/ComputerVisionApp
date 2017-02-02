package com.toprank.computervision.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import static org.bytedeco.javacpp.opencv_core.*;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.provider.BaseColumns._ID;
import static com.toprank.computervision.database.ComputerVisionDbContract.Mat.*;

/**
 * Created by john on 7/25/15.
 */
public class MatController {

    private static class QueryComponents{
        String[] projection = {
                _ID,
                COLUMN_NAME_NAME,
                COLUMN_NAME_ROWS,
                COLUMN_NAME_COLS,
                COLUMN_NAME_TYPE,
                COLUMN_NAME_DATA,

        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder = _ID + " ASC";

        String whereCols = null;
        String[] whereVals = null;
    }

    private Cursor createCursor(Context context, QueryComponents qc){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.query(
                TABLE_NAME,  // The table to query
                qc.projection,                               // The columns to return
                qc.whereCols,                                // The columns for the WHERE clause
                qc.whereVals,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                qc.sortOrder                                 // The sort order
        );
    }

    private CvMat createMat(Cursor c){
        int rows = c.getInt(c.getColumnIndex(COLUMN_NAME_ROWS));
        int cols = c.getInt(c.getColumnIndex(COLUMN_NAME_COLS));
        int type = c.getInt(c.getColumnIndex(COLUMN_NAME_TYPE));
        byte[] data = c.getBlob(c.getColumnIndex(COLUMN_NAME_DATA));
        CvMat res = cvCreateMat(rows, cols, type);
        res.getByteBuffer().put(data);
        return res;
    }

    public List<CvMat> getMats(Context context, String name, int offset, int limit){
        QueryComponents qc = new QueryComponents();
        qc.whereCols = COLUMN_NAME_NAME + " = ?";
        qc.whereVals = new String[]{
                name
        };
        Cursor c = createCursor(context, qc);
        List<CvMat> res = new LinkedList<>();
        c.moveToPosition(offset);
        int i = 0;
        while (!c.isAfterLast() && i < limit){
            CvMat mat = createMat(c);
            res.add(mat);
            c.moveToNext();
            i ++;
        }
        return res;
    }

    public Map<String, CvMat> getMats(Context context, int offset, int limit){
        QueryComponents qc = new QueryComponents();
        Cursor c = createCursor(context, qc);
        Map<String, CvMat> res = new HashMap<>();
        c.moveToPosition(offset);
        int i = 0;
        while (!c.isAfterLast() && i < limit){
            CvMat mat = createMat(c);
            String name = c.getString(c.getColumnIndex(COLUMN_NAME_NAME));
            res.put(name, mat);
            c.moveToNext();
            i ++;
        }
        return res;
    }

    public void deleteMats(Context context){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL(SQL_DELETE_ALL_RECORDS);
    }

    public void deleteMats(Context context, String name){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_NAME_NAME + "=" + name, null);
    }

    public void insertMats(Context context, HashMap<String, CvMat> mats){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        for(String s: mats.keySet()){
            CvMat mat = mats.get(s);
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_NAME, s);
            values.put(COLUMN_NAME_ROWS, mat.rows());
            values.put(COLUMN_NAME_COLS, mat.cols());
            values.put(COLUMN_NAME_TYPE, mat.type());
            ByteBuffer bb = mat.getByteBuffer();

            byte[] b = new byte[bb.remaining()];
            bb.get(b);

            values.put(COLUMN_NAME_DATA, b);

            db.insert(
                    TABLE_NAME,
                    null,
                    values);
        }
    }

    public void insertMat(Context context, String name, CvMat mat){
        HashMap<String, CvMat> map = new HashMap<>();
        map.put(name, mat);
        insertMats(context, map);
    }

    public void refreshMats(Context context, HashMap<String, CvMat> mats){
        deleteMats(context);
        insertMats(context, mats);
    }
}
