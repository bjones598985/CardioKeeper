package com.q29ideas.cardiokeeper;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//Created 8/17/2017.

class DatabaseBackup {
    private static final String BACKUP_FILE = "data_backup.txt";
    private static final String TAG = DatabaseBackup.class.getSimpleName();

    private Context context;
    DatabaseBackup(Context context){
        this.context = context;
    }

    int updateDbLog(ArrayList<String> inserts) {
        FileOutputStream outputStream;
        final List<String> fileList = Arrays.asList(context.fileList());
        try {
            if (fileList.contains(BACKUP_FILE)) {
                outputStream = context.openFileOutput(BACKUP_FILE, Context.MODE_APPEND);
            } else {
                outputStream = context.openFileOutput(BACKUP_FILE, Context.MODE_PRIVATE);
            }
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < inserts.size(); x++) {
                sb.append('\n');
                sb.append(inserts.get(x));
            }
            outputStream.write(sb.toString().getBytes());
            outputStream.close();
            return 1;
        } catch (IOException ioe) {
            return -1;
        }
    }

    void dumpBackupFile() {
        File file = context.getFileStreamPath(BACKUP_FILE);
        BufferedInputStream bis;
        StringBuilder sb = new StringBuilder();
        char[] ch = new char[(int) file.length()];
        try {
            bis = new BufferedInputStream(context.openFileInput(BACKUP_FILE));
            int line;
            int count = 0;
            while ((line = bis.read()) != -1) {
                ch[count] = (char) line;
                count++;
            }
            for (char c : ch) {
                sb.append(c);
            }
        } catch (IOException ioe) {
            //do nothing
        }
    }

    static boolean exportDataLog() {
        final File sd = Environment.getExternalStorageDirectory();
        final File data = MyApplication.appContext.getFilesDir();

        final FileChannel source, destination;
        final File currentFile = new File(data, BACKUP_FILE);
        final File backupFile = new File(sd, BACKUP_FILE);

        try {
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
}