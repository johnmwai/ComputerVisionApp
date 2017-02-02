package com.toprank.computervision.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.toprank.computervision.Person;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static android.provider.BaseColumns._ID;
import static com.toprank.computervision.database.ComputerVisionDbContract.Person.*;

/**
 * Created by john on 7/25/15.
 */
public class PersonController {

    private static class QueryComponents{
        String[] projection = {
                _ID,
                COLUMN_NAME_NAME

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

    public int countPeople(Context context){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor mCount= db.rawQuery("select count(*) from " + TABLE_NAME , null);
        mCount.moveToFirst();
        int count= mCount.getInt(0);
        mCount.close();
        return count;
    }

    private Person createPerson(Cursor c){
        long id = c.getLong(c.getColumnIndex(_ID));
        String name = c.getString(c.getColumnIndex(COLUMN_NAME_NAME));
        Person p = new Person();
        p.setId(id);
        p.setName(name);
        return p;
    }

    public List<Person> getPeople(Context context, int offset, int limit){
        QueryComponents qc = new QueryComponents();

        Cursor c = createCursor(context, qc);
        List<Person> res = new LinkedList<>();
        c.moveToPosition(offset);
        int i = 0;
        while (!c.isAfterLast() && i < limit){
            Person p = createPerson(c);
            res.add(p);
            c.moveToNext();
            i ++;
        }
        return res;
    }

    public Person getPerson(Context context, String name){
        QueryComponents qc = new QueryComponents();
        qc.whereCols = COLUMN_NAME_NAME + " = ?";
        qc.whereVals = new String[]{
                name
        };
        Cursor c = createCursor(context, qc);
        c.moveToPosition(0);
        int i = 0;
        Person p = null;
        if(c.getCount() >= 1){
            p = createPerson(c);
        }
        return p;
    }

    public void deletePeople(Context context){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.execSQL(SQL_DELETE_ALL_RECORDS);
    }

    public void deletePeople(Context context, String name){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_NAME_NAME + "=" + name, null);
    }

    public void insertPeople(Context context, List<Person> people){
        ComputerVisionDbHelper helper = new ComputerVisionDbHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        for(Person p: people){
            ContentValues values = new ContentValues();
            values.put(COLUMN_NAME_NAME, p.getName());
            db.insert(
                    TABLE_NAME,
                    null,
                    values);
        }
    }

    public void refreshMats(Context context, List<Person> people){
        deletePeople(context);
        insertPeople(context, people);
    }
}
