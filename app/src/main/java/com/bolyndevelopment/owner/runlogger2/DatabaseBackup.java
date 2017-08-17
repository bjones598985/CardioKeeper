package com.bolyndevelopment.owner.runlogger2;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Bobby Jones on 8/17/2017.
 */

public class DatabaseBackup {
    private static final String BACKUP_FILE = "data_backup.txt";
    private static final String TAG = DatabaseBackup.class.getSimpleName();

    Context context;
    public DatabaseBackup(Context context){
        this.context = context;
    }

    int writeWorkoutsToFile(ContentValues values) {
        FileOutputStream outputStream = null;
        final List<String> fileList = Arrays.asList(context.fileList());
        try {

            if (fileList.contains(BACKUP_FILE)) {
                outputStream = context.openFileOutput(BACKUP_FILE, Context.MODE_APPEND);
            } else {
                outputStream = context.openFileOutput(BACKUP_FILE, Context.MODE_PRIVATE);
            }

            //outputStream = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), fileName));
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < list.size() - 1; x++) {
                sb.append(list.get(x));
                sb.append(",");
            }
            sb.append(list.get(list.size() - 1));
            sb.append('\n');
            outputStream.write(sb.toString().getBytes());
            outputStream.close();
            return 1;
        } catch (IOException ioe) {
            Log.e(TAG, ioe.toString());
            Log.e(TAG, "The file did not write");
            return -1;
        }
    }
}
