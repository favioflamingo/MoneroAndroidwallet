package com.example.root.monerotest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.root.monerotest.MenuFragments.ReceiveFragment;
import com.example.root.monerotest.MenuFragments.SendFragment;
import com.example.root.monerotest.MenuFragments.SettingsFragment;
import com.example.root.monerotest.MenuFragments.SignFragment;
import com.example.root.monerotest.QRGenerator.QRGeneratorActivity;
import com.example.root.monerotest.QRReader.QRReaderActivity;
import com.example.root.monerotest.Services.SyncWalletService;


public class MainActivity extends AppCompatActivity implements ServiceConnection, SyncWalletService.Callbacks {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private boolean mBound;
    private int mMaxSyncValue;
    private SyncWalletService mService;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private Boolean mIsSyncing;
    private int mCurrentFragmentID;
    private ProgressBar mSyncProgressBar;
    private TextView mHeightValue;
    private Handler mHandler;
    final ViewGroup nullParent = null;

    private BroadcastReceiver mBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SyncWalletService.ACTION_SYNC_DONE)){
                resumeActionBar();

                switch (mCurrentFragmentID){
                    case R.id.item_dashboard:
                        DashboardFragment fragment = (DashboardFragment) getFragmentManager().
                                                        findFragmentById(R.id.main_content);
                        if(fragment != null)
                            fragment.setData();
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_layout);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mIsSyncing = false;
        mHandler = new Handler();

        //Launch dashboard intent.
        DashboardFragment fragment = DashboardFragment.newInstance();
        getFragmentManager().beginTransaction().replace(R.id.main_content, fragment).commit();


        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //setup custom action bar
        View mainAB = inflater.inflate(R.layout.ab_main, null);
        setCustomActionBar(mainAB);

        //Set up left menu listeners to items.
        setNavigationDrawerLayoutListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mBound = false;
        IntentFilter filter = new IntentFilter();
        filter.addAction(SyncWalletService.ACTION_SYNC_DONE);
        registerReceiver(mBroadcast, filter);
        Intent serviceIntent = new Intent(this, SyncWalletService.class);
        bindService(serviceIntent, this, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBroadcast);
    }

    public void resumeActionBar(){
        mIsSyncing = false;
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customActionBar = null;

        //Switch to corresponding fragment depending
        // on which one is currently visible
        switch (mCurrentFragmentID){
            case R.id.item_dashboard:
                customActionBar = inflater.inflate(R.layout.ab_main, nullParent);
                break;
            case R.id.item_send:
                customActionBar = inflater.inflate(R.layout.ab_send, nullParent);
                break;
            case R.id.item_settings:
                customActionBar = inflater.inflate(R.layout.ab_main, nullParent);
                break;
            case R.id.item_receive:
                customActionBar = inflater.inflate(R.layout.ab_receive, nullParent);
                break;
        }

        if(customActionBar != null)
            setCustomActionBar(customActionBar);

        unbindService(this);
    }
    /**
     * Service Connection's required methods.
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        SyncWalletService.SyncServiceBinder binder = (SyncWalletService.SyncServiceBinder)
                service;
        mService = binder.getService();
        mService.registerClient(this);
        mBound = true;
    }
    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBound = false;
    }
    /**
     * Init and setup most UI and listeners.
     */
    private void setNavigationDrawerLayoutListener(){
        NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                switch (item.getItemId()){
                    case R.id.item_dashboard:
                        DashboardFragment dashboardFragment = DashboardFragment.newInstance();
                        setTitle(getString(R.string.nav_item_dashboard_title));
                        mCurrentFragmentID = R.id.item_dashboard;
                        getFragmentManager().beginTransaction().replace(R.id.main_content, dashboardFragment).commit();
                        break;
                    case R.id.item_send:
                        SendFragment fragmentSend = SendFragment.newInstance();
                        setTitle(getString(R.string.nav_item_send_title));
                        mCurrentFragmentID = R.id.item_send;
                        getFragmentManager().beginTransaction().replace(R.id.main_content, fragmentSend).commit();

                        break;
                    case R.id.item_sign:
                        SignFragment fragmentSign = SignFragment.newInstance();
                        setTitle(getString(R.string.nav_item_sign_title));
                        mCurrentFragmentID = R.id.item_sign;
                        getFragmentManager().beginTransaction().replace(R.id.main_content, fragmentSign).commit();
                        break;
                    case R.id.item_settings:
                        SettingsFragment settingsFragment = SettingsFragment.newInstance();
                        setTitle(getString(R.string.nav_item_settings_title));
                        mCurrentFragmentID = R.id.item_settings;
                        getFragmentManager().beginTransaction().replace(R.id.main_content, settingsFragment).commit();
                        break;

                    case R.id.item_receive:
                        ReceiveFragment fragment = ReceiveFragment.newInstance();
                        setTitle(getString(R.string.nav_item_receive_title));
                        mCurrentFragmentID = R.id.item_receive;
                        getFragmentManager().beginTransaction().replace(R.id.main_content, fragment).commit();
                        break;
                }
                mDrawerLayout.closeDrawer(Gravity.START);
                return true;
            }
        });


        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View customActionBar = inflater.inflate(R.layout.ab_main, nullParent);
        setCustomActionBar(customActionBar);
    }

    public void setCustomActionBar(View view){

        if(mIsSyncing!= null && mIsSyncing)
            return;

        ActionBar actionBar = getSupportActionBar();
        if(actionBar == null)
            return;

        View previousCustomView = actionBar.getCustomView();

        if(previousCustomView != null){
            actionBar.setCustomView(null);
        }


        (view.findViewById(R.id.action_open_menu)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDrawerLayout == null)
                    return;
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        });


        ImageButton qrGeneratorIcon = (ImageButton) view.findViewById(R.id.action_open_qr_generator);
        if(qrGeneratorIcon != null){

            qrGeneratorIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent startQRGenActivity = new Intent(MainActivity.this, QRGeneratorActivity.class);
                    startActivity(startQRGenActivity);
                }
            });

        }

        ImageButton qrGeneratorReader = (ImageButton) view.findViewById(R.id.action_read_qr);

        if(qrGeneratorReader != null){

            qrGeneratorReader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent startQRReaderActivity = new Intent(MainActivity.this, QRReaderActivity.class);
                    startActivity(startQRReaderActivity);
                }
            });

        }

        ImageButton actionSend = (ImageButton) view.findViewById(R.id.action_send);

        if(actionSend != null){
            actionSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO: handle sending action.
                }
            });

        }

        ImageButton actionSync = (ImageButton) view.findViewById(R.id.action_sync_wallet);

        if(actionSync != null){
            actionSync.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO: handle sync the wallet

                    Toast.makeText(MainActivity.this, "Action Sync", Toast.LENGTH_SHORT).show();
                    //if no connection. return

                    if(mService == null)
                        return;

                    mService.checkHeight();
                }
            });

        }

        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowHomeEnabled (false);

        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);

        actionBar.setCustomView(view);
    }

    /**
     * Callbacks so the service can report back to main activity.
     */
    @Override
    public void updateProgressBar(int current, int max) {

        //inflate progress bar custom view.
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customActionBar = inflater.inflate(R.layout.ab_sync_progress, nullParent);

        //Hold reference to both views in custom action bar view.
        mSyncProgressBar = (ProgressBar) customActionBar.findViewById(R.id.progress_bar_sync);
        mHeightValue = (TextView) customActionBar.findViewById(R.id.height_value);

        //load view.
        setCustomActionBar(customActionBar);

        if(mSyncProgressBar == null)
            return;

        mMaxSyncValue = max;
        mIsSyncing = true;
        mSyncProgressBar.setMax(max);
        runnableUpdateBar.run();
    }


    private void updateBar(){
        if(mSyncProgressBar == null)
            return;
        mSyncProgressBar.setProgress(WalletHeight());
        mHeightValue.setText(String.valueOf(WalletHeight()) + "/ " + mMaxSyncValue);
    }
    private Runnable  runnableUpdateBar = new  Runnable() {

         @Override
         public void run() {
             try {
                 updateBar();
             } catch (Exception ignored) {

             } finally {
                 mHandler.postDelayed(runnableUpdateBar, 1000);
             }
         }
     };




    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native int WalletHeight();
}
