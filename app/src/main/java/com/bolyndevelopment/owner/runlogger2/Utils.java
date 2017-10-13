package com.bolyndevelopment.owner.runlogger2;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Owner on 1/19/2016.
 */
class Utils {
    private static final String TAG = "Utils";
    static final String DB_DATE_FORMAT = "MM/dd/yyyy";

    static int getCardioIcon(String exercise) {
        switch (exercise) {
            case "Biking":
                return R.drawable.bike_cardio;
            case "Elliptical":
                return R.drawable.elliptical_cardio;
            case "Exercise Bike":
                return R.drawable.exercise_bike_cardio;
            case "Hiking":
                return R.drawable.hike_cardio;
            case "Jogging":
                return R.drawable.jog_cardio;
            case "Jump Rope":
                return R.drawable.jump_rope_cardio;
            case "Rowing":
                return R.drawable.row_cardio;
            case "Rowing Machine":
                return R.drawable.row_machine;
            case "Running":
                return R.drawable.run_cardio;
            case "Stair Master":
                return R.drawable.swim_cardio;
            case "Swimming":
                return R.drawable.swim_cardio;
            case "Treadmill":
                return R.drawable.treadmill_cardio;
            case "Walking":
                return R.drawable.walk_cardio;
            default:
                return R.drawable.walk_cardio;
        }
    }

    static String getTimeStringMillis(String time) {
        String[] array = TextUtils.split(time, ":");
        int[] timeArray = new int[array.length];
        for (int x = 0; x<array.length;x++) {
            if (array[x].equals("")) {
                timeArray[x] = 0;
            } else {
                timeArray[x] = Integer.valueOf(array[x]);
            }
        }
        long millis = 0;
        switch (timeArray.length) {
            case 2:
                millis =  convertToMillis(0, timeArray[0], timeArray[1]);
                break;
            case 3:
                millis =  convertToMillis(timeArray[0], timeArray[1], timeArray[2]);
                break;
        }
        return String.valueOf(millis);
    }

    static long getTimeLongMillis(String time) {
        String[] array = TextUtils.split(time, ":");
        int[] timeArray = new int[array.length];
        for (int x = 0; x<array.length;x++) {
            if (array[x].equals("")) {
                timeArray[x] = 0;
            } else {
                timeArray[x] = Integer.valueOf(array[x]);
            }
        }
        long millis = 0;
        switch (timeArray.length) {
            case 2:
                millis =  convertToMillis(0, timeArray[0], timeArray[1]);
                break;
            case 3:
                millis =  convertToMillis(timeArray[0], timeArray[1], timeArray[2]);
                break;
        }
        return millis;
    }

    @Nullable
    static Date convertStringToDate(String inDate, String format) {
        DateFormat formatter;
        Date outDate = null;
        //String formattedDate = inDate.replace("/", "-");
        formatter = new SimpleDateFormat(format, Locale.US);
        try {
            outDate = formatter.parse(inDate);
        } catch (ParseException pe) {

        }
        return outDate;
    }

    static String convertDateToString(Date inDate, String format) {
        return new SimpleDateFormat(format, Locale.US).format(inDate);
    }

    static float convertDpToPixel(float dp) {
        DisplayMetrics metrics = MyApplication.appContext.getResources().getDisplayMetrics();
        return dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    static float convertPixelsToDp(float px){
        DisplayMetrics metrics = MyApplication.appContext.getResources().getDisplayMetrics();
        return px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    static String convertLongToDate(long time, String format) {
        Date d = new Date(time);
        return new SimpleDateFormat(format, Locale.US).format(d);
    }

    static long convertDateToMillis(Date date) {
        return date.getTime();
    }

    static Calendar[] comparisonDates(Calendar one, Calendar two) {
        Calendar[] calendarArray = new Calendar[2];
        one.set(Calendar.HOUR_OF_DAY, 12);
        one.set(Calendar.MINUTE, 0);
        one.set(Calendar.SECOND, 0);
        one.set(Calendar.MILLISECOND, 0);

        two.set(Calendar.HOUR_OF_DAY, 12);
        two.set(Calendar.MINUTE, 0);
        two.set(Calendar.SECOND, 0);
        two.set(Calendar.MILLISECOND, 0);
        calendarArray[0] = one;
        calendarArray[1] = two;
        return calendarArray;
    }

    static int getNumberOfDaysInMonth(Calendar cal) {
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);
        switch(month) {
            case 4:
            case 6:
            case 9:
            case 11:
                return 30;
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                return 31;
            case 2:
                if (year % 4 == 0) {
                    return 29;
                } else {
                    return 28;
                }
        }
        return 0;
    }

    static long convertToMillis(int hours, int minutes, int seconds) {
        long millis = 0;
        millis += (hours * 60 * 60 * 1000);
        millis += (minutes * 60 * 1000);
        millis += (seconds * 1000);
        return millis;
    }

    static int convertMillisToIntMinutes(long millis) {
        return (int) (millis / 60000);
    }

    static float convertMillisToFloatMinutes(long millis) {
        return (float) (millis / 60000);
    }

    static String convertMillisToHms(long millis) {
        return String.format(Locale.US, "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    static boolean readInternalFile() {
        final File sd = Environment.getExternalStorageDirectory();
        final File data = MyApplication.appContext.getFilesDir();

        final FileChannel source, destination;
        final File currentFile = new File(data, "01112017.txt");
        final File backupFile = new File(sd, "01112017.txt");

        try {
            Log.d(TAG, "inside try readinternalfile");
            source = new FileInputStream(currentFile).getChannel();
            destination = new FileOutputStream(backupFile).getChannel();
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
            return true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }

    static void backupDb(final Uri uri, final Handler handler, @Nullable final View coord) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File currentDB = new File(Environment.getDataDirectory(), "data/com.bolyndevelopment.owner.runlogger2/databases/log.db");
                try {
                    final ParcelFileDescriptor pfd = MyApplication.appContext.getContentResolver().openFileDescriptor(uri, "w");
                    if (pfd != null) {
                        final FileChannel destination = new FileOutputStream(pfd.getFileDescriptor()).getChannel();
                        final FileChannel source = new FileInputStream(currentDB).getChannel();
                        destination.transferFrom(source, 0, source.size());
                        source.close();
                        destination.close();
                        pfd.close();
                        if (coord != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Snackbar.make(coord, "Sweet, you records are successfully backed up!", Snackbar.LENGTH_SHORT).show();
                                }
                            });
                        }
                        String date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US).format(new Date());
                        handler.dispatchMessage(handler.obtainMessage(12, date));
                        Log.d(TAG, "db backed up successfully");
                    } else {
                        if (coord != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Snackbar.make(coord, "Oh no, I couldn't back up your records", Snackbar.LENGTH_SHORT).show();
                                }
                            });
                        }
                        Log.d(TAG, "db not backed up successfully");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    static void restoreDb(final Uri restoreUri, final Handler handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File applicationDb = new File(Environment.getDataDirectory(), "data/com.bolyndevelopment.owner.runlogger2/databases/log.db");
                try {
                    final FileChannel source = ((FileInputStream) MyApplication.appContext.getContentResolver().openInputStream(restoreUri)).getChannel();
                    if (source != null) {
                        final FileChannel destination = new FileOutputStream(applicationDb).getChannel();
                        destination.transferFrom(source, 0, source.size());
                        source.close();
                        destination.close();
                        handler.dispatchMessage(handler.obtainMessage(6));
                        //handler.post(new Runnable() {
                            //@Override
                            //public void run() {
                                //Snackbar.make(coord, "Yay! Your records have been successfully restored!", Snackbar.LENGTH_LONG).show();
                            //}
                        //});
                    } else {
                        handler.dispatchMessage(handler.obtainMessage(9));
                        //handler.post(new Runnable() {
                            //@Override
                            //public void run() {
                                //Snackbar.make(coord, "Couldn't restore database - the backup location is no good", Snackbar.LENGTH_LONG).show();
                            //}
                        //});
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Error writing DB " + e.toString());
                }
            }
        }).start();
    }

    static void exportData(final Handler handler) {
        Log.d(TAG, "export data");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File sd = Environment.getExternalStorageDirectory();
                final File data = Environment.getDataDirectory();

                final FileChannel source, destination;
                //final FileChannel destination;
                final String currentDBPath = "data/com.bolyndevelopment.owner.runlogger2/databases/log.db";
                final String backupDBPath = "log.db";
                final File currentDB = new File(data, currentDBPath);
                final File backupDB = new File(sd, backupDBPath);
                Log.d(TAG, "export data - inside thread");

                try {
                    Log.d(TAG, "export data");
                    source = new FileInputStream(currentDB).getChannel();
                    destination = new FileOutputStream(backupDB).getChannel();
                    destination.transferFrom(source, 0, source.size());
                    source.close();
                    destination.close();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MyApplication.appContext, "DB Exported", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    Log.d(TAG, "Error writing DB " + e.toString());
                    //Message msg = handler.obtainMessage(MainActivity.IMPORT_DB, "Uh oh - your export isn't working. Sorry.");
                    //handler.dispatchMessage(msg);
                }
            }
        }).start();
    }

    static void importDb(final Handler handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File externalStorageDirectory = Environment.getExternalStorageDirectory();
                final File dataDirectory = Environment.getDataDirectory();
                final FileChannel source, destination;
                final String applicationDbPath = "data/com.bolyndevelopment.owner.intelligym2/databases/workout_records.db";
                final String importDbPath = "workout_records.db";
                final File importDb = new File(externalStorageDirectory, importDbPath);
                final File applicationDb = new File(dataDirectory, applicationDbPath);
                try {
                    source = new FileInputStream(importDb).getChannel();
                    destination = new FileOutputStream(applicationDb).getChannel();
                    destination.transferFrom(source, 0, source.size());
                    source.close();
                    destination.close();
                    //Message msg = handler.obtainMessage(MainActivity.IMPORT_DB, "Data successfully imported to " + applicationDb.getAbsolutePath());
                    //handler.dispatchMessage(msg);
                    //Snackbar.make(mainLayout, "Database imported to " + applicationDb.getAbsolutePath(), Snackbar.LENGTH_LONG).show();
                } catch (IOException e) {
                    Log.d(TAG, "Error writing DB " + e.toString());
                    //Message msg = handler.obtainMessage(MainActivity.IMPORT_DB, "Uh oh - your import isn't working. Sorry.");
                    //handler.dispatchMessage(msg);
                }
            }
        }).start();
    }

    //we'll use this to delete the prior days text workout file
    static void cleanFileList() {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -1);
        String d = Utils.convertDateToString(cal.getTime(), "MMddyyyy");
        d += ".txt";
        final List<String> fileNameList = Arrays.asList(MyApplication.appContext.fileList());
        if (fileNameList.contains(d)) {
            MyApplication.appContext.deleteFile(d);
        }
    }

    static void writeDbToCsvFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = DataModel.getInstance().rawQuery("select Workout.date, " +
                        "Exercise.exercise, Set_Record.weight, Set_Record.reps, " +
                        "Set_Record.date_time, Set_Record.notes, Set_Record.set_id, " +
                        "Set_Record.ex_order_num from Set_Record, Workout, " +
                        "Exercise where Workout._id=Set_Record.workout_id " +
                        "and Exercise._id=Set_Record.exercise_id and Workout.date='07/03/2017' " +
                        "order by Set_Record.ex_order_num, Set_Record.set_id", null);
                File output = new File(Environment.getExternalStorageDirectory(), "workout.csv");
                BufferedOutputStream boss;
                cursor.moveToFirst();
                try {
                    boss = new BufferedOutputStream(new FileOutputStream(output));
                    String columnNames = "Date,Exercise,Weight,Reps,Date_Time,Notes,Set_Id,Exercise Order Number\n";
                    boss.write(columnNames.getBytes());
                    while (!cursor.isAfterLast()) {
                        String sb = cursor.getString(0) + "," +
                                cursor.getString(1) + "," +
                                cursor.getFloat(2) + "," +
                                cursor.getInt(3) + "," +
                                cursor.getInt(4) + "," +
                                cursor.getString(5) + "," +
                                cursor.getString(6) + "," +
                                cursor.getInt(7) +
                                '\n';
                        boss.write(sb.getBytes());
                        cursor.moveToNext();
                    }
                    boss.close();
                } catch (IOException ioe) {

                }
            }
        }).start();
    }

    static class ColorUtils {

        static List<Integer> makeNNumberOfColors(int color, int numOfShades) {
            List<Integer> colorList = new ArrayList<>();
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            float luminosity = hsv[2];
            float[] luminArray = new float[5];
            luminArray[0] = luminosity * .6f;
            luminArray[1] = luminosity * .8f;
            luminArray[2] = luminosity * 1.0f;
            luminArray[3] = luminosity * 1.2f;
            luminArray[4] = luminosity * 1.4f;
            for (float f : luminArray) {
                hsv[2] = f;
                colorList.add(Color.HSVToColor(hsv));
            }
            return colorList;
        }

        static int getCardioColor(String exercise) {
            switch (exercise) {
                case "Biking":
                    return Color.parseColor("#ff0000");
                case "Elliptical":
                    return Color.parseColor("#ff6680");
                case "Exercise Bike":
                    return Color.parseColor("#cc2100");
                case "Hiking":
                    return Color.parseColor("#008888");
                case "Jogging":
                    return Color.parseColor("#ff00ff");
                case "Jump Rope":
                    return Color.parseColor("#dd00a0");
                case "Rowing":
                    return Color.parseColor("#ffa500");
                case "Rowing Machine":
                    return Color.parseColor("#2aa900");
                case "Running":
                    return Color.parseColor("#999999");
                case "Stair Master":
                    return Color.parseColor("#666666");
                case "Swimming":
                    return Color.parseColor("#00cc00");
                case "Treadmill":
                    return Color.parseColor("#0088ff");
                case "Walking":
                    return Color.parseColor("#0000ff");
                default:
                    return Color.parseColor("#008888");
            }
        }
    }

}
