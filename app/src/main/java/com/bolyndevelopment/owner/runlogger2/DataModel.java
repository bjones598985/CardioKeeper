package com.bolyndevelopment.owner.runlogger2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Owner on 11/18/2015.
 */

class DataModel {
    public final static String TAG = "DataModel";

    private static volatile DataModel instance = null;

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
    static final String COL_SEQUENCE = "sequence";

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

    private DataModel() {
        helper = new SQLDatabaseHelper(MyApplication.appContext);
        helper.getWritableDatabase();
    }

    static DataModel getInstance() {
        if (instance == null) {
            synchronized(DataModel.class) {
                if (instance == null) {
                    instance = new DataModel();
                }
            }
        }
        return instance;
    }

    long addRecords(ArrayList<String> data, @Nullable ArrayList<String> lapData) {
        ArrayList<String> inserts = new ArrayList<>();
        int cals = 0;
        ContentValues values = new ContentValues();
        values.put(COL_DATE, data.get(0));
        values.put(COL_SEQUENCE, generateSequenceNumber(data.get(0)));
        values.put(COL_TIME, Long.parseLong(data.get(1)));
        values.put(COL_DISTANCE, Float.parseFloat(data.get(2)));
        if (!data.get(3).equals("")) {
            cals = Integer.parseInt(data.get(3));
            values.put(COL_CALORIES, cals);
        } else {
            values.put(COL_CALORIES, cals);
        }
        values.put(COL_CARDIO_TYPE, data.get(4));
        long dataId = helper.getWritableDatabase().insert(DATA_TABLE, null, values);
        String sqlInsert = "insert into " + DATA_TABLE + " values(" + data.get(0) + ", " + Long.parseLong(data.get(1)) + ", " +
                Float.parseFloat(data.get(2)) + ", " + cals + ", " + data.get(4) + ");";
        Log.d(TAG, "SQL: " + sqlInsert);
        if (dataId > -1) {
            inserts.add(sqlInsert + ":success");
        } else {
            inserts.add(sqlInsert + ":fail");
        }

        int ids = 0;
        if (lapData != null) {
            Collections.reverse(lapData);
            for (int l = 0; l < lapData.size(); l++) {
                ContentValues lapValues = new ContentValues();
                lapValues.put(COL_WORKOUT_ID, dataId);
                lapValues.put(COL_LAP_NUMBER, l + 1);
                lapValues.put(COL_TIME, Long.valueOf(lapData.get(l)));
                ids += helper.getWritableDatabase().insert(LAP_TABLE, null, lapValues);
                long ll = l + 1;
                String sql = "insert into " + LAP_TABLE + " values(" + dataId + ", " + ll + ", " + lapData.get(l) + ");";
                Log.d(TAG, "SQL: " + sql);
                if (ids > -1) {
                    inserts.add(sql + ":success");
                } else {
                    inserts.add(sql + ":fail");
                }
            }
        }
        new DatabaseBackup(MyApplication.appContext).updateDbLog(inserts);
        return dataId;
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

    int generateSequenceNumber(long currentTime) {
        Date d = new Date(currentTime);
        String formattedDate = new SimpleDateFormat("MM/dd/yyyy", Locale.US).format(d);
        Cursor c = rawQuery("Select max(" + COL_SEQUENCE + ") from "
                + DATA_TABLE + " where " + COL_DATE + " =?", new String[]{formattedDate});
        c.moveToFirst();
        int seq = c.getInt(0) + 1;
        c.close();
        return seq;
    }

    int generateSequenceNumber(String date) {
        Cursor c = rawQuery("Select max(" + COL_SEQUENCE + ") from "
                + DATA_TABLE + " where " + COL_DATE + " =?", new String[]{date});
        c.moveToFirst();
        int seq = c.getInt(0) + 1;
        c.close();
        return seq;
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
                    COL_SEQUENCE + " integer not null, " +
                    COL_TIME + " integer not null, " +
                    COL_DISTANCE + " real not null, " +
                    COL_CALORIES + " integer not null, " +
                    COL_CARDIO_TYPE + " text not null, " +
                    COL_NOTES + " text);");

            db.execSQL("create table " + LAP_TABLE + "(" +
                    COL_ID + " integer primary key autoincrement, " +
                    COL_WORKOUT_ID + " integer not null, " +
                    COL_LAP_NUMBER + " integer not null, " +
                    COL_TIME + " integer not null);");
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