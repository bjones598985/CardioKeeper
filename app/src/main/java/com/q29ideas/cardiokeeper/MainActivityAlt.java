package com.q29ideas.cardiokeeper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
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

    static final float FAB_MULTI_FACTOR = 1.75f;

    static final int DIALOG_ENABLE_BACKUP = 1;
    static final int DIALOG_ABOUT = 2;
    static final int DIALOG_PERMISSION = 3;

    static final int MIN_DELAY_MILLIS = 200;
    static final int CODE_TIMER = 100;

    static final int WRITE_REQUEST_CODE = 1;

    static final int CREATE_FILE_CODE = 57;
    static final int SEARCH_FILE_CODE = 43;
    static final int SETTINGS_CODE = 36;

    static final String DATE = "date";
    static final String TIME = "timerTime";
    static final String DISTANCE = "distance";
    static final String CALORIES = "calories";
    static final String CARDIO_TYPE = "cardio_type";
    static final String DIALOG_TYPE = "dialogType";

    static final String DB_MIME_TYPE = "application/x-sqlite3";

    private int fabHeight;

    private ActionBarDrawerToggle drawerToggle;
    private Handler handler = new Handler();
    boolean isFirstBackup, isDualPane, areFabsExpanded = false;
    private CoordinatorLayout coord;
    private Toolbar toolbar;
    private DrawerLayout drawer;
    private NavigationView mainNavLeft;
    private FloatingActionButton plusFab, timerFab, filterFab, addFab;
    private BottomSheetBehavior bottomBehavior;

    String distUnit;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_FILE_CODE && resultCode == Activity.RESULT_OK) {
            final Uri backupUri = data.getData();
            Utils.backupDb(backupUri, handler, coord);
            saveUriPathToSharedPreferences(backupUri != null ? backupUri.toString() : null);
            checkIfAutoBackupEnabled();
        }
        if (requestCode == SEARCH_FILE_CODE && resultCode == Activity.RESULT_OK) {
            final Uri restoreUri = data.getData();
            Utils.restoreDb(restoreUri, handler);
            saveUriPathToSharedPreferences(restoreUri != null ? restoreUri.toString() : null);
        }
        if (requestCode == SETTINGS_CODE && resultCode == Activity.RESULT_OK) {
            boolean isDataChanged = data.getBooleanExtra("isDataChanged", false);
            if (isDataChanged) {
                setInitialPreferences();
                ((ListDisplayFragment)getSupportFragmentManager().findFragmentById(R.id.ListFrag)).notifyOfDataChange();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_alt);
        isDualPane = getResources().getBoolean(R.bool.dual_pane);

        //addRandomData(); testing only

        coord = findViewById(R.id.coord);
        toolbar = findViewById(R.id.toolbar);
        final ViewPager pager = findViewById(R.id.pager);
        pager.setAdapter(new PagerAdapter(getSupportFragmentManager()));

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
        initFabs();

        if (!isDualPane) {
            initDrawer();
            Glide.with(this).asBitmap()
                    .load(R.drawable.card_keep_finish_v5)
                    .into((ImageView) findViewById(R.id.app_icon_imageview));
        }
        setInitialPreferences();
        boolean recreate = getIntent().getBooleanExtra("recreate", false);
        if (recreate) {
            startActivityForResult(new Intent(MainActivityAlt.this, TimerActivity.class), CODE_TIMER);
        }

        bottomBehavior = BottomSheetBehavior.from(pager);
        bottomBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomBehavior.setPeekHeight(0);
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

    private void setInitialPreferences() {
        final SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String distPref = sPrefs.getString(getResources().getString(R.string.pref_distance), "-1");
        distUnit = distPref.equals("-1") ? getResources().getString(R.string.short_miles).toLowerCase() : getResources().getString(R.string.short_kilos).toLowerCase();
    }

    private void initFabs() {
        plusFab = findViewById(R.id.fab_menu);
        plusFab.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                plusFab.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                fabHeight = plusFab.getHeight();
            }
        });
        plusFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFabs();
            }
        });
        timerFab = findViewById(R.id.fab_time_record);
        timerFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateFabs();
                ((ListDisplayFragment)getSupportFragmentManager().findFragmentById(R.id.ListFrag)).onTimerFabClicked();
            }
        });
        filterFab = findViewById(R.id.fab_filter);
        filterFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateFabs();
                bottomBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        addFab = findViewById(R.id.fab_add_manual);
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateFabs();
                ((ListDisplayFragment)getSupportFragmentManager().findFragmentById(R.id.ListFrag)).initAddDialog(null);
            }
        });
    }

    private void animateFabs() {
        if (areFabsExpanded) {
            plusFab.animate().rotationBy(45f).scaleYBy(.25f).scaleXBy(.25f).start();
            timerFab.animate().rotationBy(360f).translationYBy(fabHeight * FAB_MULTI_FACTOR).start();
            filterFab.animate().rotationBy(360f).translationXBy(fabHeight * FAB_MULTI_FACTOR).start();
            addFab.animate().rotationBy(360f).translationXBy(fabHeight * FAB_MULTI_FACTOR * .75f).translationYBy(fabHeight * FAB_MULTI_FACTOR * .75f).start();
        } else {
            plusFab.animate().rotationBy(-45f).scaleYBy(-0.25f).scaleXBy(-0.25f).start();
            timerFab.animate().rotationBy(-360f).translationYBy(-fabHeight * FAB_MULTI_FACTOR).start();
            filterFab.animate().rotationBy(-360f).translationXBy(-fabHeight * FAB_MULTI_FACTOR).start();
            addFab.animate().rotationBy(-360f).translationXBy(-fabHeight * FAB_MULTI_FACTOR * .75f).translationYBy(-fabHeight * FAB_MULTI_FACTOR * .75f).start();
        }
        areFabsExpanded = !areFabsExpanded;
    }

    /*
    This is only called on smaller screens
     */
    private void initDrawer() {
        drawer = findViewById(R.id.main_drawer_layout);
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
        mainNavLeft = findViewById(R.id.main_nav_left);
        if (!isDualPane) {
            findViewById(R.id.first_child).setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void checkIfAutoBackupEnabled() {
        boolean syncEnabled = isAutoBackupEnabled();
        if (!syncEnabled) {
            Random rand = new Random();
            int r = rand.nextInt(10);
            if (r > 5) {
                showGeneralDialog(DIALOG_ENABLE_BACKUP);
            }
        }
    }

    private boolean isAutoBackupEnabled() {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getBoolean(getString(R.string.pref_sync), false);
    }

    //for testing purposes only
    private void addRandomData() {
        final List<String> cardioList = Arrays.asList(getResources().getStringArray(R.array.cardio_types));
        for (int y=1; y<13; y++) {
            for (int x = 1; x < 31; x += 3) {
                String date = String.format(Locale.US, "%04d-%02d-%02d", 2017, y, x);
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

    public void queryForRecords(final ArrayList<CheckBox> filter) {
        bottomBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        if (filter.size() > 0) {
            final String[] array = new String[filter.size()];
            for (int x = 0; x < filter.size(); x++) {
                array[x] = filter.get(x).getText().toString();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Cursor cursor = DataModel.getInstance().getSelectedRecords(array);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            ((ListDisplayFragment) getSupportFragmentManager().findFragmentById(R.id.ListFrag)).onRecordsQueried(cursor);
                        }
                    });
                }
            }).start();
        }
    }

    private void saveUriPathToSharedPreferences(String path) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getResources().getString(R.string.db_backup_key), path);
        editor.apply();
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

    private void onSetSortArgs(String... args) {
        bottomBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        ((ListDisplayFragment) getSupportFragmentManager().findFragmentById(R.id.ListFrag)).sortList(args);
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

    @Override
    public long saveEnteredData(final HashMap<String, String> map, ArrayList<String> lapData) {
        return DataModel.getInstance().addRecords(map, lapData);
    }

    @Override
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
            if (isAutoBackupEnabled()) {
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

    private class PagerAdapter extends FragmentPagerAdapter {

        PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Page page = new Page();
            Bundle bundle = new Bundle();
            bundle.putInt("pos", position);
            page.setArguments(bundle);
            return page;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    public static class Page extends Fragment implements View.OnClickListener {
        private int position;
        private View root;
        private ArrayList<CheckBox> filterList;
        private Spinner spinner1, spinner2, spinner3;

        private int[] ids = new int[]{R.id.checkBox_biking, R.id.checkBox_elliptical, R.id.checkBox_exercise_bike,
                R.id.checkBox_hiking, R.id.checkBox_jogging, R.id.checkBox_jump_rope, R.id.checkBox_rowing, R.id.checkBox_rowing_machine,
                R.id.checkBox_running, R.id.checkBox_stair_master, R.id.checkBox_swimming, R.id.checkBox_treadmill, R.id.checkBox_walking,
                R.id.checkBox_all, R.id.filter_button, R.id.clear_button};

        private WeakReference<MainActivityAlt> ref;

        public Page() {

        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            if (context instanceof MainActivityAlt) {
                ref = new WeakReference<>((MainActivityAlt) context);
            } else {
                throw new RuntimeException(context.toString()
                        + " must implement ListFragListener");
            }
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            position = getArguments().getInt("pos");
        }

        @SuppressLint("InflateParams")
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            root = position == 0 ? inflater.inflate(R.layout.filter_frag_layout, null) : inflater.inflate(R.layout.sort_frag_layout, null);
            return root;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            switch (position) {
                case 0:
                    filterList = new ArrayList<>();
                    for (int id : ids) {
                        view.findViewById(id).setOnClickListener(this);
                    }
                    break;
                case 1:
                    view.findViewById(R.id.sort_button).setOnClickListener(this);
                    view.findViewById(R.id.reset_button).setOnClickListener(this);
                    setUpSpinners(view);
                    break;
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.checkBox_biking:
                case R.id.checkBox_elliptical:
                case R.id.checkBox_exercise_bike:
                case R.id.checkBox_hiking:
                case R.id.checkBox_jogging:
                case R.id.checkBox_jump_rope:
                case R.id.checkBox_rowing:
                case R.id.checkBox_rowing_machine:
                case R.id.checkBox_running:
                case R.id.checkBox_stair_master:
                case R.id.checkBox_swimming:
                case R.id.checkBox_treadmill:
                case R.id.checkBox_walking:
                    if (filterList.contains(v)) {
                        filterList.remove(v);
                    } else {
                        filterList.add((CheckBox) v);
                    }
                    break;
                case R.id.checkBox_all:
                    addOrRemoveAllCheckboxes(((CheckBox)v).isChecked());
                    break;
                case R.id.filter_button:
                    ref.get().queryForRecords(filterList);
                    break;
                case R.id.clear_button:
                    clearAllChecks();
                    break;
                case R.id.sort_button:
                    setSortArgs();
                    break;
                case R.id.reset_button:
                    resetSpinners();
                    break;
            }
        }

        private void setUpSpinners(View view) {
            final ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(getContext(), R.array.sort_options, android.R.layout.simple_spinner_item);
            final ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(getContext(), R.array.sort_options, android.R.layout.simple_spinner_item);
            final ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(getContext(), R.array.sort_options, android.R.layout.simple_spinner_item);
            adapter1.setDropDownViewResource(android.R.layout.simple_list_item_1);
            adapter2.setDropDownViewResource(android.R.layout.simple_list_item_1);
            adapter3.setDropDownViewResource(android.R.layout.simple_list_item_1);
            spinner1 = view.findViewById(R.id.spinner1);
            spinner1.setAdapter(adapter1);
            spinner2 = view.findViewById(R.id.spinner2);
            spinner2.setAdapter(adapter2);
            spinner3 = view.findViewById(R.id.spinner3);
            spinner3.setAdapter(adapter3);
        }

        private void addOrRemoveAllCheckboxes(boolean isChecked) {
            if (isChecked) {
                filterList.clear();
                for (int x = 0; x < 13; x++) {
                    final CheckBox cBox = root.findViewById(ids[x]);
                    cBox.setChecked(true);
                    filterList.add(cBox);
                }
            } else {
                filterList.clear();
                clearAllChecks();
            }
        }

        private void clearAllChecks() {
            for (int x = 0; x < 14; x++) {
                final CheckBox cBox = root.findViewById(ids[x]);
                cBox.setChecked(false);
            }
        }

        private void setSortArgs() {
            String s1 = ((TextView) spinner1.getSelectedView()).getText().toString();
            String s2 = ((TextView) spinner2.getSelectedView()).getText().toString();
            String s3 = ((TextView) spinner3.getSelectedView()).getText().toString();
            ref.get().onSetSortArgs(s1, s2, s3);
        }

        private void resetSpinners() {
            spinner1.setSelection(0, true);
            spinner2.setSelection(0, true);
            spinner3.setSelection(0, true);
        }
    }
}