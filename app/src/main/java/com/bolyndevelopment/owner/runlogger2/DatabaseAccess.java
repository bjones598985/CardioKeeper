package com.bolyndevelopment.owner.runlogger2;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Owner on 11/18/2015.
 */

class DatabaseAccess {
    public final static String TAG = "DatabaseAccess";

    private static volatile DatabaseAccess instance = null;

    private static final int DATABASE_VERSION = 1;
    static final String DATABASE_NAME = "log.db";

    static final String COL_ID = "_id";

    static final String COL_DATE = "date";
    static final String COL_TIME = "time";
    static final String COL_DISTANCE = "distance";
    static final String COL_CALORIES = "calories";
    static final String COL_CARDIO_TYPE = "cardio_type";
    static final String DATA_TABLE = "Data";
    static final String COL_NOTES = "notes";

    static final String LAP_TABLE = "Lap";
    static final String COL_WORKOUT_ID = "workout_id";
    static final String COL_LAP_NUMBER = "lap_num";

    SQLDatabaseHelper helper;
    private SQLiteDatabase database;

    public String[] from = new String[]{COL_DATE, //0
            COL_TIME, //1
            COL_DISTANCE, //2
            COL_CALORIES, //3
            COL_CARDIO_TYPE}; //4

    private DatabaseAccess() {
        helper = new SQLDatabaseHelper(MyApplication.appContext);
        helper.getWritableDatabase();
    }

    static DatabaseAccess getInstance() {
        if (instance == null) {
            synchronized(DatabaseAccess.class) {
                if (instance == null) {
                    instance = new DatabaseAccess();
                }
            }
        }
        return instance;
    }

    long addRecord(ArrayList<String> data) {
        ContentValues values = new ContentValues();
        values.put(COL_DATE, data.get(0));
        values.put(COL_TIME, Long.parseLong(data.get(1)));
        values.put(COL_DISTANCE, Float.parseFloat(data.get(2)));
        if (!data.get(3).equals("")) {
            values.put(COL_CALORIES, Integer.parseInt(data.get(3)));
        } else {
            values.put(COL_CALORIES, 0);
        }
        values.put(COL_CARDIO_TYPE, data.get(4));
        return helper.getWritableDatabase().insert(DATA_TABLE, null, values);
    }

    Cursor getAllRecords() {
        return helper.getReadableDatabase().query(DATA_TABLE, from, null, null, null, null, COL_DATE + " desc");
    }

    //will still work
    Cursor rawQuery(String query, String[] arguments) {
        return helper.getWritableDatabase().rawQuery(query, arguments);
    }

    Cursor getRecords(String[] columns) {
        return helper.getReadableDatabase().query(DATA_TABLE, columns, null,null,null,null,null);
    }

    static class SQLDatabaseHelper extends SQLiteOpenHelper implements BaseColumns {

        SQLDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table " + DATA_TABLE + "(" +
                    COL_ID + " integer primary key autoincrement, " +
                    COL_DATE + " text not null, " +
                    COL_TIME + " integer not null, " +
                    COL_DISTANCE + " real not null, " +
                    COL_CALORIES + " integer not null, " +
                    COL_CARDIO_TYPE + " text not null, " +
                    COL_NOTES + " text);");

            db.execSQL("create table " + LAP_TABLE + "(" +
                    COL_ID + " integer primary key autoincrement, " +
                    COL_WORKOUT_ID + "integer not null, " +
                    COL_LAP_NUMBER + " integer not null, " +
                    COL_TIME + " integer not null, " +
                    COL_DISTANCE + " real not null);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        //used only for AndroidDatabaseManager.java
        ArrayList<Cursor> getData(String Query){
            //get writable database
            SQLiteDatabase sqlDB = this.getWritableDatabase();
            String[] columns = new String[] { "mesage" };
            //an array list of cursor to save two cursors one has results from the query
            //other cursor stores error message if any errors are triggered
            ArrayList<Cursor> alc = new ArrayList<>(2);
            MatrixCursor Cursor2= new MatrixCursor(columns);
            alc.add(null);
            alc.add(null);

            try{
                String maxQuery = Query ;
                //execute the query results will be save in Cursor newLine
                Cursor c = sqlDB.rawQuery(maxQuery, null);


                //add value to cursor2
                Cursor2.addRow(new Object[] { "Success" });

                alc.set(1,Cursor2);
                if (null != c && c.getCount() > 0) {


                    alc.set(0,c);
                    c.moveToFirst();

                    return alc ;
                }
                return alc;
            } catch(SQLException sqlEx){
                Log.d("printing exception", sqlEx.getMessage());
                //if any exceptions are triggered save the error message to cursor an return the arraylist
                Cursor2.addRow(new Object[] { ""+sqlEx.getMessage() });
                alc.set(1,Cursor2);
                return alc;
            } catch(Exception ex){

                Log.d("printing exception", ex.getMessage());

                //if any exceptions are triggered save the error message to cursor an return the arraylist
                Cursor2.addRow(new Object[] { ""+ex.getMessage() });
                alc.set(1,Cursor2);
                return alc;
            }
        }
    }
}
