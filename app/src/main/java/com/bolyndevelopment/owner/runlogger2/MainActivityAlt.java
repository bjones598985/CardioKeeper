package com.bolyndevelopment.owner.runlogger2;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityMainBinding;
import com.bumptech.glide.Glide;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivityAlt extends AppCompatActivity  implements
        BackupRestoreDialog.ChoiceListener, GeneralDialog.GeneralDialogListener,
        View.OnClickListener, ListDisplayFragment.ListFragListener,
        NavFragment.NavFragListener {
    public static final String TAG = "MainActivityAlt";

    static final int DIALOG_ENABLE_BACKUP = 1;
    static final int DIALOG_ABOUT = 2;
    static final int DIALOG_PERMISSION = 3;

    static final String DATE = "date";
    static final String TIME = "timerTime";
    static final String DISTANCE = "distance";
    static final String CALORIES = "calories";
    static final String CARDIO_TYPE = "cardio_type";

    static final String DIALOG_TYPE = "dialogType";

    static final int MIN_DELAY_MILLIS = 200;
    static final int CODE_TIMER = 100;

    public static final int WRITE_REQUEST_CODE = 1;

    static final int CREATE_FILE_CODE = 57;
    static final int SEARCH_FILE_CODE = 43;
    static final int SETTINGS_CODE = 36;

    static final String DB_MIME_TYPE = "application/x-sqlite3";

    private ActionBarDrawerToggle drawerToggle;
    Handler handler = new Handler();
    ArrayList<String> lapDataFromTimer;
    boolean isAddDialogOpen = false;
    boolean isFirstBackup;
    boolean isAutoBackupEnabled;
    boolean isDualPane;
    String distUnit;
    CoordinatorLayout coord;
    Toolbar toolbar;
    DrawerLayout drawer;
    NavigationView mainNavLeft;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        if (requestCode == CODE_TIMER && resultCode == Activity.RESULT_OK) {
            final String totalTime = data.getStringExtra("totalTime");
            lapDataFromTimer = data.getStringArrayListExtra("list");
            ((ListDisplayFragment)getSupportFragmentManager().findFragmentById(R.id.ListFrag)).initAddDialog(totalTime);
        }
        if (requestCode == CREATE_FILE_CODE && resultCode == Activity.RESULT_OK) {
            final Uri backupUri = data.getData();
            Utils.backupDb(backupUri, handler, coord);
            saveUriPathToSharedPreferences(backupUri.toString());
            checkIfAutoBackupEnabled();
        }
        if (requestCode == SEARCH_FILE_CODE && resultCode == Activity.RESULT_OK) {
            final Uri restoreUri = data.getData();
            Utils.restoreDb(restoreUri, handler);
            saveUriPathToSharedPreferences(restoreUri.toString());
        }
        if (requestCode == SETTINGS_CODE && resultCode == Activity.RESULT_OK) {
            boolean isDataChanged = data.getBooleanExtra("isDataChanged", false);
            if (isDataChanged) {
                setInitialPreferences();
                ((ListDisplayFragment)getSupportFragmentManager().findFragmentById(R.id.ListFrag)).notifyOfDataChange();
            }
        }
    }

    private void checkIfAutoBackupEnabled() {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean syncEnabled = sharedPref.getBoolean(getString(R.string.pref_sync), false);
        if (!syncEnabled) {
            Random rand = new Random();
            int r = rand.nextInt(10);
            if (r > 5) {
                showGeneralDialog(DIALOG_ENABLE_BACKUP);
            }
        }
    }

    private void addRandomData() {
        final List<String> cardioList = Arrays.asList(getResources().getStringArray(R.array.cardio_types));
        for (int y=1; y<8; y++) {
            for (int x = 1; x < 31; x += 3) {
                String date = String.format(Locale.US, "%02d/%02d/%04d", y, x, 2017);
                Random random = new Random();
                int min = random.nextInt((60 - 1) + 1) + 1;
                int sec = random.nextInt((60 - 1) + 1) + 1;
                int miles = random.nextInt((15 - 1) + 1) + 1;
                int calories = random.nextInt((1000 - 100) + 1) + 100;
                HashMap<String, String> map = new HashMap<>();
                map.put(DATE, date);
                map.put(TIME, Utils.getTimeStringMillis(String.valueOf(min) + ":" + String.valueOf(sec)));
                map.put(DISTANCE, String.valueOf(miles));
                map.put(CALORIES, String.valueOf(calories));
                int index = random.nextInt(13);
                if (index == 0) {
                    index++;
                }
                map.put(CARDIO_TYPE, cardioList.get(index));
                DataModel.getInstance().addRecords(map, null);
            }
        }
    }

    public void queryForRecords() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Cursor cursor = DataModel.getInstance().getAllRecords();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ((ListDisplayFragment)getSupportFragmentManager().findFragmentById(R.id.ListFrag)).onRecordsQueried(cursor);
                    }
                });
            }
        }).start();
    }

    private void saveUriPathToSharedPreferences(String path) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getResources().getString(R.string.db_backup_key), path);
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_alt);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindow().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        Log.i(TAG, "DensityDPI: " + metrics.densityDpi);
        Log.i(TAG, "Density: " + metrics.density);

        isDualPane = getResources().getBoolean(R.bool.dual_pane);

        coord = (CoordinatorLayout) findViewById(R.id.coord);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 6) {
                    queryForRecords();
                    Snackbar.make(coord, "Yay! Your records have been successfully restored!", Snackbar.LENGTH_LONG).show();
                }
                if (msg.what == 9) {
                    Snackbar.make(coord, "Couldn't restore database - the backup location is no good", Snackbar.LENGTH_LONG).show();
                }
                if (msg.what == 12) {
                    SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getResources().getString(R.string.db_backup_date_time), msg.obj.toString());
                    editor.apply();
                }
            }
        };
        setSupportActionBar(toolbar);
        setupFabs();
        if (savedInstanceState != null) {
            isAddDialogOpen = savedInstanceState.getBoolean("isAddDialogOpen");
            if (isAddDialogOpen) {
                ((ListDisplayFragment)getSupportFragmentManager().findFragmentById(R.id.ListFrag)).initAddDialog(null);
            }
        }
        if (!isDualPane) {
            setupDrawer();
            Glide.with(this).asBitmap()
                    .load(R.drawable.card_keep_finish_v5)
                    .into((ImageView) findViewById(R.id.app_icon_imageview));
        }
        setInitialPreferences();
        boolean recreate = getIntent().getBooleanExtra("recreate", false);
        if (recreate) {
            startActivityForResult(new Intent(MainActivityAlt.this, TimerActivity.class), CODE_TIMER);
        }
    }

    private void setInitialPreferences() {
        final SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String distPref = sPrefs.getString(getResources().getString(R.string.pref_distance), "-1");
        distUnit = distPref.equals("-1") ? getResources().getString(R.string.short_miles).toLowerCase() : getResources().getString(R.string.short_kilos).toLowerCase();
        isAutoBackupEnabled = sPrefs.getBoolean(getResources().getString(R.string.pref_sync), false);
    }

    private void setupFabs() {
        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isAddDialogOpen) {
                    ((ListDisplayFragment)getSupportFragmentManager().findFragmentById(R.id.ListFrag)).initAddDialog(null);
                    isAddDialogOpen = true;
                }

            }
        });
        findViewById(R.id.fab_time_record).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ListDisplayFragment)getSupportFragmentManager().findFragmentById(R.id.ListFrag)).onTimerFabClicked(isAddDialogOpen);
                isAddDialogOpen = false;
            }
        });
    }

    //only if screen is big
    private void setupDrawer() {
        drawer = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        drawer.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        drawerToggle = new ActionBarDrawerToggle(this,
                drawer,
                toolbar,
                R.string.drawer_open,
                R.string.drawer_close){

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        drawer.addDrawerListener(drawerToggle);
        mainNavLeft = (NavigationView) findViewById(R.id.main_nav_left);
        findViewById(R.id.nav_menu_graph).setOnClickListener(this);
        findViewById(R.id.nav_menu_backup).setOnClickListener(this);
        findViewById(R.id.nav_menu_settings).setOnClickListener(this);
        findViewById(R.id.nav_menu_about).setOnClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isAddDialogOpen", isAddDialogOpen);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    public void showGeneralDialog(int type) {
        GeneralDialog sd = new GeneralDialog();
        Bundle b = new Bundle();
        b.putInt(DIALOG_TYPE, type);
        sd.setArguments(b);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(sd, "general");
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!isDualPane) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (!isDualPane) {
            drawerToggle.syncState();
        }
    }

    /*
    this is where we create the file and then get the Uri and write to it
     */
    private void createFile(String mimeType, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Create a file with the requested MIME type.
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, CREATE_FILE_CODE);
    }

    private void searchForBackup() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(DB_MIME_TYPE);
        startActivityForResult(intent, SEARCH_FILE_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                    final String backupKey = sharedPref.getString(getResources().getString(R.string.db_backup_key), null);
                    Bundle b = new Bundle();
                    b.putString("backupKey", backupKey);
                    BackupRestoreDialog brd = new BackupRestoreDialog();
                    brd.setArguments(b);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.add(brd, "backup");
                    ft.commitAllowingStateLoss();
                } else {
                    showGeneralDialog(DIALOG_PERMISSION);
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean checkForPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public long saveEnteredData(final HashMap<String, String> map, ArrayList<String> lapData) {
        return DataModel.getInstance().addRecords(map, lapData);
        //new DatabaseBackup(this).dumpBackupFile();
    }

    @Override
    public void graphIt(String date, String cType) {
        Intent i = new Intent(this, HelloGraph.class);
        i.putExtra("date", date);
        i.putExtra("cType", cType);
        startActivity(i);
    }

    public void setInitDialogOpen(boolean isOpen) {
        isAddDialogOpen = isOpen;
    }

    @Override
    public void onChoiceSelected(int choice) {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final String backupKey = sharedPref.getString(getResources().getString(R.string.db_backup_key), null);
        isFirstBackup = backupKey == null;
        switch (choice) {
            case BackupRestoreDialog.BACKUP_TO_NEW:
                createFile(DB_MIME_TYPE, DataModel.DATABASE_NAME);
                break;
            case BackupRestoreDialog.BACKUP_TO_PREVIOUS:
                final Uri backupUri = Uri.parse(backupKey);
                Utils.backupDb(backupUri, handler, coord);
                checkIfAutoBackupEnabled();
                break;
            case BackupRestoreDialog.RESTORE_FROM_NEW:
                if (checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_REQUEST_CODE)) {
                    searchForBackup();
                }
                break;
            case BackupRestoreDialog.RESTORE_FROM_PREVIOUS:
                final Uri restoreUri = Uri.parse(backupKey);
                Utils.restoreDb(restoreUri, handler);
                break;
        }
    }

    @Override
    public void onGeneralDialogButtonClicked(int buttonId) {
        if (buttonId == DialogInterface.BUTTON_POSITIVE) {
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(getResources().getString(R.string.pref_sync), true);
            editor.apply();
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onDatabaseEvent(DataModel.DatabaseEvent event) {
        if (event.getEvent() == DataModel.DatabaseEvent.DATA_ADDED) {
            if (isAutoBackupEnabled) {
                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                final String backupKey = sharedPref.getString(getResources().getString(R.string.db_backup_key), null);
                final Uri backupUri = Uri.parse(backupKey);
                Utils.backupDb(backupUri, handler, null);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.nav_menu_graph:
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(getBaseContext(), HelloGraph.class));
                    }
                }, MIN_DELAY_MILLIS);
                break;
            case R.id.nav_menu_backup:
                onBackUpRestoreClicked();
                break;
            case R.id.nav_menu_settings:
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent i = new Intent(getBaseContext(), SettingsActivity.class);
                        i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
                        startActivityForResult(i, SETTINGS_CODE);
                    }
                }, MIN_DELAY_MILLIS);
                break;
            case R.id.nav_menu_about:
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showGeneralDialog(DIALOG_ABOUT);
                    }
                }, MIN_DELAY_MILLIS);
                break;
        }
        if (!isDualPane) {
            drawer.closeDrawer(mainNavLeft);
        }
    }

    @Override
    public void onBackUpRestoreClicked() {
        if (checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_REQUEST_CODE)) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivityAlt.this);
                    final String backupKey = sharedPref.getString(getResources().getString(R.string.db_backup_key), null);
                    Bundle b = new Bundle();
                    b.putString("backupKey", backupKey);
                    BackupRestoreDialog brd = new BackupRestoreDialog();
                    brd.setArguments(b);
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    ft.add(brd, "backup");
                    ft.commitAllowingStateLoss();
                }
            }, MIN_DELAY_MILLIS);
        }
    }

    @Override
    public void onMenuOptionClicked(View view) {
        onClick(view);
    }
}