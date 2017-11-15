package com.bolyndevelopment.owner.runlogger2;

import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

//Created 1/19/2016.

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
        formatter = new SimpleDateFormat(format, Locale.US);
        try {
            outDate = formatter.parse(inDate);
        } catch (ParseException pe) {
            Log.e(TAG, pe.getMessage());
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

    static long convertToMillis(int hours, int minutes, int seconds) {
        long millis = 0;
        millis += (hours * 60 * 60 * 1000);
        millis += (minutes * 60 * 1000);
        millis += (seconds * 1000);
        return millis;
    }

    static String convertMillisToHms(long millis) {
        long hour = TimeUnit.MILLISECONDS.toHours(millis);
        long min = TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        long sec = TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        if (hour > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hour, min, sec);
        } else if (min > 0){
            return String.format(Locale.US, "%02d:%02d", min, sec);
        } else if (sec > 0) {
            return String.format(Locale.US, "0:%02d", sec);
        } else {
            return "No timerTime";
        }
    }

    static String convertSecondsToHms(long seconds) {
        seconds *= 1000;
        long hour = TimeUnit.MILLISECONDS.toHours(seconds);
        long min = TimeUnit.MILLISECONDS.toMinutes(seconds) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(seconds));
        long sec = TimeUnit.MILLISECONDS.toSeconds(seconds) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(seconds));
        if (hour > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hour, min, sec);
        } else if (min > 0){
            return String.format(Locale.US, "%02d:%02d", min, sec);
        } else {
            return String.format(Locale.US, "0:%02d", sec);
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
                    Log.e(TAG, ioe.getMessage());
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

        static int darkenColor(int color) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] *= 1.2;
            return Color.HSVToColor(hsv);
        }

        static int changeAlpha(int color, int alphaValue) {
            final float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            int tempColor = Color.HSVToColor(hsv);
            return Color.argb(alphaValue, Color.red(tempColor), Color.green(tempColor), Color.blue(tempColor));
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
