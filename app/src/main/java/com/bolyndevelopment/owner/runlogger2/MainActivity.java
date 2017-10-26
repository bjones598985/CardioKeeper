package com.bolyndevelopment.owner.runlogger2;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bolyndevelopment.owner.runlogger2.databinding.ActivityMainBinding;
import com.bumptech.glide.Glide;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity implements BackupRestoreDialog.ChoiceListener, GeneralDialog.GeneralDialogListener {
    public static final String TAG = "MainActivity";

    static final int DIALOG_ENABLE_BACKUP = 1;
    static final int DIALOG_ABOUT = 2;
    static final String DIALOG_TYPE = "dialogType";

    static final String DATE = "date";
    static final String TIME = "time";
    static final String DISTANCE = "distance";
    static final String CALORIES = "calories";
    static final String CARDIO_TYPE = "cardio_type";

    static final int CODE_TIMER = 100;

    static final int MIN_DELAY_MILLIS = 200;
    static final int ALPHA_25 = 63;

    public static final int WRITE_REQUEST_CODE = 1;

    static final int CREATE_FILE_CODE = 57;
    static final int SEARCH_FILE_CODE = 43;
    static final int SETTINGS_CODE = 36;

    static final String DB_MIME_TYPE = "application/x-sqlite3";

    private ActionBarDrawerToggle drawerToggle;

    List<ListItem> recordsList = new ArrayList<>();
    Handler handler;
    MyAdapter mAdapter;
    ArrayList<String> lapDataFromTimer;
    ActivityMainBinding binder;
    boolean isAddDialogOpen = false;
    boolean isFirstBackup;
    boolean isAutoBackupEnabled;
    String distUnit;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_TIMER && resultCode == Activity.RESULT_OK) {
            final String totalTime = data.getStringExtra("totalTime");
            lapDataFromTimer = data.getStringArrayListExtra("list");
            initAddDialog(totalTime);
        }
        if (requestCode == CREATE_FILE_CODE && resultCode == Activity.RESULT_OK) {
            final Uri backupUri = data.getData();
            Utils.backupDb(backupUri, handler, binder.coord);
            saveUriPathToSharedPreferences(backupUri.toString());
            if (isFirstBackup) {
                showGeneralDialog(DIALOG_ENABLE_BACKUP);
            }
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
                mAdapter.notifyDataSetChanged();
            }
        }
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
        binder = DataBindingUtil.setContentView(this, R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 6) {
                    queryForRecords();
                    Snackbar.make(binder.coord, "Yay! Your records have been successfully restored!", Snackbar.LENGTH_LONG).show();
                }
                if (msg.what == 9) {
                    Snackbar.make(binder.coord, "Couldn't restore database - the backup location is no good", Snackbar.LENGTH_LONG).show();
                }
                if (msg.what ==12) {
                    SharedPreferences sharedPref =  PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(getResources().getString(R.string.db_backup_date_time), msg.obj.toString());
                    editor.apply();
                }
            }
        };

        initRecyclerView();
        //addRandomData();
        queryForRecords();
        setSupportActionBar(binder.toolbar);
        setupDrawer();
        setupFabs();

        if (savedInstanceState != null) {
            isAddDialogOpen = savedInstanceState.getBoolean("isAddDialogOpen");
            if (isAddDialogOpen) {
                initAddDialog(null);
            }
        }
        setInitialPreferences();
    }

    private void setInitialPreferences() {
        final SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String distPref = sPrefs.getString(getResources().getString(R.string.pref_distance), "-1");
        distUnit = distPref.equals("-1") ? getResources().getString(R.string.short_miles).toLowerCase() : getResources().getString(R.string.short_kilos).toLowerCase();
        isAutoBackupEnabled = sPrefs.getBoolean(getResources().getString(R.string.pref_sync), false);
    }

    private void setupFabs() {
        binder.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isAddDialogOpen) {
                    initAddDialog(null);
                    isAddDialogOpen = true;
                }
            }
        });
        binder.fabTimeRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAddDialogOpen) {
                    recordsList.remove(0);
                    binder.mainRecyclerview.getAdapter().notifyItemRemoved(0);
                    isAddDialogOpen = false;
                }
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivityForResult(new Intent(getBaseContext(), TimerActivity.class), CODE_TIMER);
                    }
                }, MIN_DELAY_MILLIS);

            }
        });
    }

    private void setupDrawer() {
        binder.mainDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        drawerToggle = new ActionBarDrawerToggle(this,
                binder.mainDrawerLayout,
                binder.toolbar,
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
        binder.mainDrawerLayout.addDrawerListener(drawerToggle);
        //binder.mainDrawerLayout.setDrawerListener(drawerToggle);
        //drawerToggle.syncState();

    }


    public void onNavClick(final View v) {
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
                if (checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, WRITE_REQUEST_CODE)) {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(v.getContext());
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
            case R.id.nav_menu_adm:
                startActivity(new Intent(this, AndroidDatabaseManager.class));
                break;
        }
        binder.mainDrawerLayout.closeDrawer(binder.mainNavLeft);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Log.d(TAG, "home button touched");
            binder.mainDrawerLayout.openDrawer(GravityCompat.START);  // OPEN DRAWER
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isAddDialogOpen", isAddDialogOpen);
        super.onSaveInstanceState(outState);
    }

    private void initAddDialog(@Nullable String time) {
        AddDialog ad = new AddDialog();
        if (time != null) {
            ad.time = time;
            Cursor c = DataModel.getInstance().rawQuery("select date, cardio_type from Data limit 1", null);
            c.moveToFirst();
            String type = null;
            if (c.getCount() > 0) {
                type = c.getString(1);
                c.close();
            }
            if (type != null) {
                final List<String> list = Arrays.asList(getResources().getStringArray(R.array.cardio_types));
                ad.spinnerPosition = list.indexOf(type);
            }
        }
        ad.date = Utils.convertDateToString(new Date(), "MM/dd/yyyy");
        recordsList.add(0, ad);
        binder.mainRecyclerview.getAdapter().notifyItemInserted(0);
        binder.mainRecyclerview.scrollToPosition(0);
        isAddDialogOpen = true;
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
                ArrayList<String> list = new ArrayList<>();
                list.add(date);
                list.add(Utils.getTimeStringMillis(String.valueOf(min) + ":" + String.valueOf(sec)));
                list.add(String.valueOf(miles));
                list.add(String.valueOf(calories));
                int index = random.nextInt(13);
                if (index == 0) index++;
                list.add(cardioList.get(index));
                long l = DataModel.getInstance().addRecords(list, null);
            }
        }
    }

    private void queryForRecords() {
        recordsList.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Cursor cursor = DataModel.getInstance().getAllRecords();
                cursor.moveToFirst();
                ListItem item;
                while (!cursor.isAfterLast()) {
                    item = new ListItem();
                    item.cType = cursor.getString(4);
                    item.calories = cursor.getInt(3);
                    item.distance = cursor.getFloat(2);
                    item.date = cursor.getString(0);
                    item.time = Utils.convertMillisToHms(cursor.getLong(1));
                    recordsList.add(item);
                    cursor.moveToNext();
                }
                cursor.close();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAdapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();

    }

    private void initRecyclerView() {
        binder.mainRecyclerview.setHasFixedSize(true);
        mAdapter = new MyAdapter();
        binder.mainRecyclerview.setAdapter(mAdapter);


        /*
        used to add decoration onto recyclerview

        Drawable right = getResources().getDrawable(R.drawable.right_divider);
        Drawable left = getResources().getDrawable(R.drawable.left_divider);
        binder.mainRecyclerview.addItemDecoration(new DividerDecoration(left, right));
        */
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

    private void showGeneralDialog(int type) {
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
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }


    /*
    this is where we create the file and then get the Uri and write to it
     */
    private void createFile(String mimeType, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        // Filter to only show results that can be "opened", such as
        // a file (as opposed to a list of contacts or timezones).
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.putExtra(Intent.EXTRA_INITIAL_INTENTS, Intent.ACTION_GET_CONTENT);

        // Create a file with the requested MIME type.
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, CREATE_FILE_CODE);
    }

    private void searchForBackup() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType(DB_MIME_TYPE);
        startActivityForResult(intent, SEARCH_FILE_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (requestCode) {
            case WRITE_REQUEST_CODE:
                Log.d(TAG, "Write request");
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
                    //isThereExternalWriteAccess = true;
                    //Utils.exportData(handler);
                    //Toast.makeText(this, "You can now export the database.", Toast.LENGTH_SHORT).show();
                } else {
                    Toasty.error(this, "The database can be exported without permission", Toast.LENGTH_SHORT).show();
                    /*
                    this is where we should put in something to advise of the problem and give chance to redo it
                     */
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean checkForPermission(String permission, int requestCode) {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Toast.makeText(this, "Should show request permission: true", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                Toast.makeText(this, "Should show request permission: false", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
            return false;
        } else {
            Log.d(TAG, "permission already granted");
            return true;
        }
    }

    private void saveEnteredData(final ArrayList<String> list) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long id = DataModel.getInstance().addRecords(list, lapDataFromTimer);
                //long id = 0;
                if (id > -1) {
                    ListItem item = new ListItem();
                    //item.order = (int) id;
                    item.date = list.get(0);
                    //item.date = map.get(DATE);
                    item.time = Utils.convertMillisToHms(Long.parseLong(list.get(1)));
                    //item.time = Utils.convertMillisToHms(Long.parseLong(map.get(TIME)));
                    item.distance = list.get(2).equals("") ? 0 : Float.parseFloat(list.get(2));
                    Log.d(TAG, "distance: " + item.distance);
                    //item.distance = map.get(DISTANCE).equals("") ? 0 : Float.parseFloat(map.get(DISTANCE));
                    item.calories = list.get(3).equals("") ? 0 : Integer.parseInt(list.get(3));
                    Log.d(TAG, "calories: " + item.calories);
                    //item.calories = map.get(CALORIES).equals("") ? 0 : Integer.parseInt(map.get(CALORIES));
                    item.cType = list.get(4);
                    //item.cType = map.get(CARDIO_TYPE);
                    recordsList.add(0, item);
                    recordsList.remove(1);
                    //final int index = recordsList.indexOf(item);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyItemChanged(0);
                            binder.mainRecyclerview.scrollToPosition(0);
                            isAddDialogOpen = false;
                        }
                    });
                }
            }
        }).start();
        //new DatabaseBackup(this).dumpBackupFile();
    }

    private void saveEnteredData(final HashMap<String, String> map) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long id = DataModel.getInstance().addRecords(map, lapDataFromTimer);
                if (id > -1) {
                    ListItem item = new ListItem();
                    item.date = map.get(DATE);
                    item.time = Utils.convertMillisToHms(Long.parseLong(map.get(TIME)));
                    item.distance = map.get(DISTANCE).equals("") ? 0 : Float.parseFloat(map.get(DISTANCE));
                    item.calories = map.get(CALORIES).equals("") ? 0 : Integer.parseInt(map.get(CALORIES));
                    item.cType = map.get(CARDIO_TYPE);
                    recordsList.add(0, item);
                    recordsList.remove(1);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyItemChanged(0);
                            binder.mainRecyclerview.scrollToPosition(0);
                            isAddDialogOpen = false;
                        }
                    });
                }
            }
        }).start();
        //new DatabaseBackup(this).dumpBackupFile();
    }

    public void graphIt(String date, String cType) {
        Intent i = new Intent(this, HelloGraph.class);
        i.putExtra("date", date);
        i.putExtra("cType", cType);
        startActivity(i);
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
                Utils.backupDb(backupUri, handler, binder.coord);
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

    private class ListItem {
        int calories;
        float distance;
        String date = null, time = null, cType;
    }

    private class AddDialog extends ListItem {
        int spinnerPosition = 0;
    }

    private class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final int LIST_ITEM = 1;
        final int ADD_DIALOG = 2;

        final int CARDIO_SPINNER = 1;
        final int TIME_EDITTEXT = 2;
        final int DIST_EDITTEXT = 3;

        @Override
        public int getItemViewType(int position) {
            return recordsList.get(position) instanceof AddDialog ? ADD_DIALOG : LIST_ITEM;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return viewType == LIST_ITEM ? new BaseViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_layout_v3, parent, false)) :
                    new AddViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.button_dialog_frag_layout, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (recordsList.get(position) instanceof AddDialog) {
                AddViewHolder avh = (AddViewHolder) holder;
                final AddDialog ad = (AddDialog) recordsList.get(position);
                if (ad.spinnerPosition != 0) {
                    avh.cardioSpinner.setSelection(ad.spinnerPosition);
                }
                if (ad.date != null) {
                    avh.dateInput.setText(ad.date);
                }
                if (ad.time != null) {
                    avh.timeInput.setText(ad.time);
                }

            } else {
                final ListItem item = recordsList.get(position);
                BaseViewHolder bHolder = (BaseViewHolder) holder;
                String date = Utils.convertDateToString(Utils.convertStringToDate(item.date, "MM/dd/yyyy"), "MMM d");
                bHolder.date.setText(date);
                String cal = String.valueOf(item.calories) + " cals";
                bHolder.calories.setText(cal);
                bHolder.name.setText(item.cType);
                String distTime;
                if (bHolder.name.getText().equals(getResources().getString(R.string.jump_rope))) {
                    distTime = item.time;
                } else if (bHolder.name.getText().equals(getResources().getString(R.string.swimming))){
                    distTime = item.distance + " laps in " + item.time;
                } else {
                    distTime = item.distance + " " + distUnit + " in " + item.time;
                }
                bHolder.distance.setText(distTime);
                bHolder.icon.setImageResource(Utils.getCardioIcon(item.cType));
                int color = Utils.ColorUtils.getCardioColor(item.cType);

                Drawable circle = getResources().getDrawable(R.drawable.circle);
                Drawable semiCircleBanner = getResources().getDrawable(R.drawable.semi_circle_banner);

                semiCircleBanner.mutate();
                semiCircleBanner.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                bHolder.fl.setBackground(semiCircleBanner);

                circle.mutate();
                circle.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                bHolder.icon.setBackground(circle);
            }
        }

        @Override
        public int getItemCount() {
            return recordsList.size();
        }

        class BaseViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView date, time, distance, calories, name;
            ImageView icon;
            FrameLayout fl;

            BaseViewHolder(View itemView) {
                super(itemView);
                itemView.setOnClickListener(this);
                date = (TextView) itemView.findViewById(R.id.list_date_input);
                time = (TextView) itemView.findViewById(R.id.list_time_input);
                distance = (TextView) itemView.findViewById(R.id.list_miles_input);
                calories = (TextView) itemView.findViewById(R.id.list_calories_input);
                name = (TextView) itemView.findViewById(R.id.list_name_input);
                icon = (ImageView) itemView.findViewById(R.id.column_icon);
                fl = (FrameLayout) itemView.findViewById(R.id.frame_bg);
            }

            @Override
            public void onClick(View v) {
                graphIt(recordsList.get(getAdapterPosition()).date, recordsList.get(getAdapterPosition()).cType);
            }
        }

        class AddViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, Serializable{
            ViewGroup mainLayout;
            Spinner cardioSpinner;
            TextView dateInput;
            EditText timeInput, distInput, calsInput;
            TextInputLayout timeLayout;

            AddViewHolder(final View itemView) {
                super(itemView);
                mainLayout = (ViewGroup) itemView.findViewById(R.id.btn_dialog_frag_rel_layout);
                cardioSpinner = (Spinner) itemView.findViewById(R.id.cardio_type_spinner);
                dateInput = (TextView) itemView.findViewById(R.id.date_input);
                timeInput = (EditText) itemView.findViewById(R.id.time_input);
                distInput = (EditText) itemView.findViewById(R.id.miles_input);
                calsInput = (EditText) itemView.findViewById(R.id.calories_input);
                timeLayout = (TextInputLayout) itemView.findViewById(R.id.time_layout);
                itemView.findViewById(R.id.date_picker_button).setOnClickListener(this);
                itemView.findViewById(R.id.cancel_button).setOnClickListener(this);
                itemView.findViewById(R.id.confirm_button).setOnClickListener(this);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getBaseContext(),
                        R.array.cardio_types, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                cardioSpinner.setAdapter(adapter);
                cardioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        distInput.setEnabled(position != 6);
                        if (position == 11) {
                            ((TextView) itemView.findViewById(R.id.miles)).setText("Laps:");
                        } else {
                            ((TextView)itemView.findViewById(R.id.miles)).setText("Distance:");
                        }
                        GradientDrawable sd = (GradientDrawable)getResources().getDrawable(R.drawable.rounded_corner_background);
                        if (position != 0) {
                            sd.mutate();
                            int color = Utils.ColorUtils.getCardioColor(((TextView) view).getText().toString());
                            int chgColor = Utils.ColorUtils.changeAlpha(color, ALPHA_25);
                            sd.setColor(chgColor);
                            mainLayout.setBackground(sd);
                        } else {
                            sd.mutate();
                            sd.setColor(Color.parseColor("#e6e6e6"));
                            mainLayout.setBackground(sd);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }


            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.date_picker_button:
                        DialogFragment newFragment = new DatePickerFragment();
                        Bundle b = new Bundle();
                        b.putSerializable("date", this);
                        newFragment.setArguments(b);
                        newFragment.show(getFragmentManager(), "datePicker");
                        break;
                    case R.id.cancel_button:
                        recordsList.remove(0);
                        binder.mainRecyclerview.getAdapter().notifyItemRemoved(0);
                        isAddDialogOpen = false;
                        break;
                    case R.id.confirm_button:
                        int validation = validateFields();
                        if (validation > -1) {
                            highlightField(validation);
                        } else if (validateTimeFormattedProper()){
                            //saveEnteredData(addInfoToArray());
                            saveEnteredData(addInfoToMap());
                        }
                        break;
                }
            }

            private boolean validateTimeFormattedProper() {
                final String time = timeInput.getText().toString();
                String[] array = TextUtils.split(time, ":");
                int[] timeArray = new int[array.length];
                for (int x = 0; x<array.length;x++) {
                    if (array[x].equals("")) {
                        timeArray[x] = 0;
                    } else {
                        timeArray[x] = Integer.valueOf(array[x]);
                    }
                }
                switch (timeArray.length) {
                    case 2:
                        if (timeArray[1] >= 60) {
                            timeLayout.setError("Seconds >= 60");
                            return false;
                        } else {
                            timeLayout.setErrorEnabled(false);
                            return true;
                        }
                    case 3:
                        if (timeArray[2] >= 60) {
                            timeLayout.setError("Seconds >= 60");
                            return false;
                        } else if (timeArray[1] >= 60){
                            timeLayout.setError("Minutes >= 60");
                            return false;
                        } else {
                            timeLayout.setErrorEnabled(false);
                            return true;
                        }
                    default:
                        timeLayout.setError("Something ain't right...");
                        return false;
                }
            }

            private int validateFields() {
                if (cardioSpinner.getSelectedItemPosition() == 0) return CARDIO_SPINNER;
                if (timeInput.getText().toString().isEmpty()) return TIME_EDITTEXT;
                if (distInput.isEnabled() && distInput.getText().toString().isEmpty()) return DIST_EDITTEXT;
                return -1;
            }

            private void highlightField(int field) {
                final Drawable background = getResources().getDrawable(R.drawable.error_rectangle);
                switch (field) {
                    case CARDIO_SPINNER:
                        cardioSpinner.setBackground(background);
                        break;
                    case TIME_EDITTEXT:
                        timeInput.setBackground(background);
                        break;
                    case DIST_EDITTEXT:
                        distInput.setBackground(background);
                        break;
                }
            }

            private ArrayList<String> addInfoToArray() {
                //HashMap<String, String> cardioData = new HashMap<>();
                ArrayList<String> runData = new ArrayList<>();
                runData.add(dateInput.getText().toString());
                //cardioData.put(DATE, dateInput.getText().toString());
                runData.add(getTimeMillis());
                //cardioData.put(TIME, getTimeMillis());
                //if (distInput.isEnabled()) {
                runData.add(distInput.getText().toString());
                    //cardioData.put(DISTANCE, distInput.getText().toString());
                //}
                //runData.add(distInput.isEnabled() ? distInput.getText().toString() : "-1"); possibility
                runData.add(calsInput.getText().toString());
                //cardioData.put(CALORIES, calsInput.getText().toString());
                String cardio = (String)cardioSpinner.getSelectedItem();
                runData.add(cardio); //how we'll add in the cardio type
                //cardioData.put(CARDIO_TYPE, cardio);
                return runData;
            }

            private HashMap<String, String> addInfoToMap() {
                HashMap<String, String> cardioData = new HashMap<>();
                cardioData.put(DATE, dateInput.getText().toString());
                cardioData.put(TIME, getTimeMillis());
                cardioData.put(DISTANCE, distInput.getText().toString());
                cardioData.put(CALORIES, calsInput.getText().toString());
                String cardio = (String) cardioSpinner.getSelectedItem();
                cardioData.put(CARDIO_TYPE, cardio);
                return cardioData;
            }

            private String getTimeMillis() {
                final String time = timeInput.getText().toString();
                String[] array = TextUtils.split(time, ":");
                int[] timeArray = new int[array.length];
                for (int x = 0; x<array.length; x++) {
                    if (array[x].equals("")) {
                        timeArray[x] = 0;
                    } else {
                        timeArray[x] = Integer.valueOf(array[x]);
                    }
                }
                long millis = 0;
                switch (timeArray.length) {
                    case 2:
                        millis =  Utils.convertToMillis(0, timeArray[0], timeArray[1]);
                        break;
                    case 3:
                        millis =  Utils.convertToMillis(timeArray[0], timeArray[1], timeArray[2]);
                        break;
                }
                return String.valueOf(millis);
            }
        }
    }

    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        MyAdapter.AddViewHolder avh;
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            avh = (MyAdapter.AddViewHolder) getArguments().getSerializable("date");
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            month++;
            String formattedDate = String.format(Locale.US, "%02d/%02d/%04d", month, day, year);
            //String datePicked = month + "/" + day + "/" + year;
            // Do something with the date chosen by the user
            avh.dateInput.setText(formattedDate);
        }
    }

}
