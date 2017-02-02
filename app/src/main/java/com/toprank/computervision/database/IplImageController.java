package com.toprank.computervision.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.toprank.computervision.Person;

import static org.bytedeco.javacpp.opencv_core.*;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static android.provider.BaseColumns._ID;
import static com.toprank.computervision.database.ComputerVisionDbContract.IplImage.*;

/**
 * Created by john on 7/25/15.
 */
public class IplImageController {

    private static class QueryComponents{
        String[] projection = {
                _ID,
                COLUMN_NAME_NAME,
                COLUMN_NAME_WIDTH,
                COLUMN_NAME_HEIGHT,
                COLUMN_NAME_DEPTH,
                COLUMN_NAME_CHANNELS,
                COLUMN_NAME_PERSON,
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

    private IplImage createImage(Cursor c){
        int width = c.getInt(c.getColumnIndex(COLUMN_NAME_WIDTH));
        int height = c.getInt(c.getColumnIndex(COLUMN_NAME_HEIGHT));
        int depth = c.getInt(c.getColumnIndex(COLUMN_NAME_DEPTH));
        int channels = c.getInt(c.getColumnIndex(COLUMN_NAME_CHANNELS));
        byte[] data = c.getBlob(c.getColumnIndex(COLUMN_NAME_DATA));
        IplImage res = cvCreateImage(cvSize(width, height), depth, channels);
        res.getByteBuffer().put(data);
        return res;
    }

    public List<IplImage> getImages(Context context, String name, int offset, int limit){
        QueryComponents qc = new QueryComponents();
        qc.whereCols = COLUMN_NAME_NAME + " = ?";
        qc.whereVals = new String[]{
                name
        };
        Cursor c = createCursor(context, qc);
        List<IplImage> res = new LinkedList<>();
        c.moveToPosition(offset);
        int i = 0;
        while (!c.isAfterLast() && i < limit){
            IplImage img = createImage(c);
            res.add(img);
            c.moveToNext();
            i ++;
        }
        return res;
    }

    public IplImage getImage(Context context, String name){
        List<IplImage> images = getImages(context, name, 0, 1);
        if(images.size() == 0){
            return null;
        }
        return images.get(0);
    }

    public int countImages(Context context){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor mCount= db.rawQuery("select count(*) from " + TABLE_NAME, null);
        mCount.moveToFirst();
        int count= mCount.getInt(0);
        mCount.close();
        return count;
    }

    public int countFaceImages(Context context){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor mCount= db.rawQuery("select count(*) from " + TABLE_NAME + " where " +COLUMN_NAME_PERSON+ " is not null", null);
        mCount.moveToFirst();
        int count= mCount.getInt(0);
        mCount.close();
        return count;
    }

    public List<IplImage> getImages(Context context, Person p, int offset, int limit){
        QueryComponents qc = new QueryComponents();
        qc.whereCols = COLUMN_NAME_PERSON + " = ?";
        qc.whereVals = new String[]{
                String.valueOf(p.getId())
        };
        Cursor c = createCursor(context, qc);
        List<IplImage> res = new LinkedList<>();
        c.moveToPosition(offset);
        int i = 0;
        while (!c.isAfterLast() && i < limit){
            IplImage img = createImage(c);
            res.add(img);
            c.moveToNext();
            i ++;
        }
        return res;
    }

    public void deleteImages(Context context){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL(SQL_DELETE_ALL_RECORDS);
    }

    public void deleteImages(Context context, String name){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_NAME_NAME + "=" + name, null);
    }

    public void deleteImages(Context context, Person p){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_NAME_PERSON + "=" + p.getId(), null);
    }

    public void insertImages(Context context, HashMap<String, IplImage> imgs){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        for(String s: imgs.keySet()){
            IplImage img = imgs.get(s);
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_NAME, s);
            values.put(COLUMN_NAME_HEIGHT, img.height());
            values.put(COLUMN_NAME_WIDTH, img.width());
            values.put(COLUMN_NAME_DEPTH, img.depth());
            values.put(COLUMN_NAME_CHANNELS, img.nChannels());
            ByteBuffer bb = img.getByteBuffer();

            byte[] b = new byte[bb.remaining()];
            bb.get(b);

            values.put(COLUMN_NAME_DATA, b);

            db.insert(
                    TABLE_NAME,
                    null,
                    values);
        }
    }

    public void insertImages(Context context, Person p, HashMap<String, IplImage> imgs){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        for(String s: imgs.keySet()){
            IplImage img = imgs.get(s);
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_NAME, s);
            values.put(COLUMN_NAME_PERSON, p.getId());
            values.put(COLUMN_NAME_HEIGHT, img.height());
            values.put(COLUMN_NAME_WIDTH, img.width());
            values.put(COLUMN_NAME_DEPTH, img.depth());
            values.put(COLUMN_NAME_CHANNELS, img.nChannels());

            ByteBuffer bb = img.getByteBuffer();

            byte[] b = new byte[bb.remaining()];
            bb.get(b);

            values.put(COLUMN_NAME_DATA, b);
            db.insert(
                    TABLE_NAME,
                    null,
                    values);
        }
    }

    public void insertImage(Context context, String name, IplImage image){
        HashMap<String, IplImage> map = new HashMap<>();
        map.put(name, image);
        insertImages(context, map);
    }

    public void refreshImages(Context context, HashMap<String, IplImage> imgs){
        for(String name: imgs.keySet()){
            deleteImages(context, name);
        }
        insertImages(context, imgs);
    }

    public void refreshImages(Context context, Person p, HashMap<String, IplImage> imgs){
        deleteImages(context, p);
        insertImages(context, p, imgs);
    }
}
