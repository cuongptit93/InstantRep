package jp.co.efusion.aninstantreply;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.example.android.trivialdrivesample.util.IabResult;
import com.example.android.trivialdrivesample.util.Purchase;
import com.pushwoosh.BasePushMessageReceiver;
import com.pushwoosh.BaseRegistrationReceiver;
import com.pushwoosh.PushManager;
import com.pushwoosh.fragment.PushEventListener;
import com.pushwoosh.fragment.PushFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import jp.co.efusion.database.DatabaseHelper;
import jp.co.efusion.fragments.BlogFragment;
import jp.co.efusion.fragments.ContactFragment;
import jp.co.efusion.fragments.FavoriteFragment;
import jp.co.efusion.fragments.HelpFragment;
import jp.co.efusion.fragments.SettingFragment;
import jp.co.efusion.fragments.ThemeFragment;
import jp.co.efusion.listhelper.CacheLastSentence;
import jp.co.efusion.listhelper.NavDrawerItem;
import jp.co.efusion.listhelper.NavDrawerListAdapter;
import jp.co.efusion.listhelper.ThreadManager;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.SettingUtils;
import jp.co.efusion.utility.Utils;

public class HomeActivity extends ActionBarActivity implements InAppBillingSupporterListener, PushEventListener {
    private static final String TAG = HomeActivity.class.getSimpleName();
    private DrawerLayout mDrawerLayout;
    private FrameLayout container;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    // nav drawer title
    private CharSequence mDrawerTitle;

    // used to store app title
    private CharSequence mTitle;

    // slide menu items
    private String[] navMenuTitles;
    private TypedArray navMenuIcons;

    private ArrayList<NavDrawerItem> navDrawerItems;

    private NavDrawerListAdapter adapter;

    SharedPreferences sharedPreferences;
    DatabaseHelper databaseHelper;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.theme_color_green)));
        setContentView(R.layout.activity_home);

        mContext = getApplicationContext();

        //initialize database helper
        databaseHelper = new DatabaseHelper(this);
        //open database
        databaseHelper.openDatabase();

        //initialize sharepreference
        sharedPreferences = getSharedPreferences(Default.SHARE_PREFERENCE_NAME,
                Context.MODE_PRIVATE);

        //add new
        // Inflate the drawer_layout
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDrawerLayout = (DrawerLayout) inflater.inflate(R.layout.drawer_layout, null);

        ViewGroup decor = (ViewGroup) getWindow().getDecorView();
        View child = decor.getChildAt(0);
        decor.removeView(child);

        // This is the container we defined just now.
        container = (FrameLayout) mDrawerLayout.findViewById(R.id.frame_container1);
        container.addView(child);

        // Make the drawer replace the first child
        decor.addView(mDrawerLayout);

        mTitle = mDrawerTitle = getTitle();

        // load slide menu items
        navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);

        // nav drawer icons from resources
        navMenuIcons = getResources().obtainTypedArray(R.array.nav_drawer_icons);

        //mDrawerLayout = (DrawerLayout)drawer.findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) mDrawerLayout.findViewById(R.id.drawer_list);

        navDrawerItems = new ArrayList<NavDrawerItem>();

        // adding nav drawer items to array
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1)));

        navDrawerItems.add(new NavDrawerItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1)));

        navDrawerItems.add(new NavDrawerItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1)));

        navDrawerItems.add(new NavDrawerItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1)));

        navDrawerItems.add(new NavDrawerItem(navMenuTitles[4], navMenuIcons.getResourceId(4, -1)));

        navDrawerItems.add(new NavDrawerItem(navMenuTitles[5], navMenuIcons.getResourceId(5, -1)));

        navDrawerItems.add(new NavDrawerItem(navMenuTitles[6], navMenuIcons.getResourceId(0, -1)));

        navDrawerItems.add(new NavDrawerItem(navMenuTitles[7], navMenuIcons.getResourceId(1, -1)));


        // Recycle the typed array
        navMenuIcons.recycle();

        // setting the nav drawer list adapter
        adapter = new NavDrawerListAdapter(getApplicationContext(), navDrawerItems);
        mDrawerList.setAdapter(adapter);

        // enabling action bar app icon and behaving it as toggle button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.app_name, R.string.app_name) {

            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                // calling onPrepareOptionsMenu() to show action bar icons
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mTitle);
                // calling onPrepareOptionsMenu() to hide action bar icons
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0);

            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        if (savedInstanceState == null) {
            // on first time display view for first nav item
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                displayView(0);
            } else {
                boolean checkFragmentSetting = extras.getBoolean("check");
                if (checkFragmentSetting){
                    displayView(2);
                }
            }

        }

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onMenuItemClicked(position);
            }
        });

        //setup in app purchase
        App.inapp_billing_supporter.setup(this, this, new InAppBillingSupporter.OnFinishInventoryConsumedListener() {
            @Override
            public void onFinishInventoryConsumed(Purchase purchase, IabResult result) {
            }
        });

        //Init Pushwoosh fragment
        PushFragment.init(this);

        //Register receivers for push notifications
        registerReceivers();

        //Create and start push manager
        PushManager pushManager = PushManager.getInstance(this);
        //  pushManager.setNotificationFactory(new NotificationFactorySample());
        //Start push manager, this will count app open for Pushwoosh stats as well
        try {
            pushManager.onStartup(this);
        } catch (Exception e) {
            //push notifications are not available or AndroidManifest.xml is not configured properly
        }

        //Register for push!
        pushManager.registerForPushNotifications();

        checkMessage(getIntent());

    }

    @Override
    protected void onStart() {
        super.onStart();
        SettingUtils.showRattingDialog(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    private void onMenuItemClicked(final int position) {
        if (position >= 0 && position < 6) {
            displayView(position);

        } else if (position == 6) {//go to last sentence
            ThreadManager.getInstance().execTask(new Runnable() {
                @Override
                public void run() {
                    Intent i = CacheLastSentence.getPlayIntent(HomeActivity.this, CacheLastSentence.PLAY_MODE.NO_FORCE);
                    if (i != null) {
                        startActivity(i);
                        updateMenuDrawer(position);
                    }
                }
            });
        } else if (position == 7) {//rating
            Utils.openPlayStoreAppDetails(getApplicationContext(), getApplicationContext().getPackageName());
            updateMenuDrawer(position);
        }
    }

    /**
     * Diplaying fragment view for selected nav drawer list item
     */
    private void displayView(int position) {

        // update the main content by replacing fragments
        Fragment fragment = null;

        switch (position) {
            case 0:
                fragment = new ThemeFragment();
                break;
            case 1:
                fragment = new FavoriteFragment();
                break;
            case 2:
                fragment = new SettingFragment();
                break;
            case 3:
                fragment = new HelpFragment();
                break;
            case 4:
                fragment = new BlogFragment();
                break;
            case 5:
                fragment = new ContactFragment();
                break;
            default:
                break;
        }

        if (fragment != null) {
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.frame_container, fragment).commit();
            updateMenuDrawer(position);

        } else {
            // error in creating fragment
            Log.e("MainActivity", "Error in creating fragment");
        }
    }

    private void updateMenuDrawer(final int position) {
        // update selected item and title, then close the drawer
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                setTitle(navMenuTitles[position]);
                mDrawerLayout.closeDrawer(mDrawerList);
                mDrawerList.setItemChecked(position, true);
                mDrawerList.setSelection(position);

            }
        });
    }


    @Override
    public void inventoryChecked(String sku_id) {

//        if (sharedPreferences.getString(sku_id,null)!=null){
//
//            //update theme content package price & state into database
//            //update last played
//            int contentID=Default.ZERO;
//            ContentValues values = new ContentValues();
//            values.put(ThemeContentTable.THEME_CONTENT_PRICE,sharedPreferences.getString(sku_id,null));
//            values.put(ThemeContentTable.THEME_CONTENT_STATE,Default.STATE_PURCHASE);
//
//            if (sku_id.matches(Default.SKU_THEME1_PACKAGE1)){
//                contentID=2;
//            }
//            if (sku_id.matches(Default.SKU_THEME1_PACKAGE2)){
//                contentID=3;
//            }
//            if (sku_id.matches(Default.SKU_THEME1_PACKAGE3)){
//                contentID=4;
//            }
//            if (sku_id.matches(Default.SKU_THEME1_PACKAGE4)){
//                contentID=5;
//            }
//            if (sku_id.matches(Default.SKU_THEME1_PACKAGE5)){
//                contentID=6;
//            }
//            if (sku_id.matches(Default.SKU_THEME1_PACKAGE1_TO_2)){
//                contentID=7;
//                //return;
//            }
//            if (sku_id.matches(Default.SKU_THEME1_PACKAGE3_TO_5)){
//                contentID=8;
//                //return;
//            }
//            if (sku_id.matches(Default.SKU_THEME1_PACKAGE_ALL)){
//                contentID=9;
//                //return;
//            }
//            if (sku_id.matches(Default.SKU_THEME2_PACKAGE1)){
//                contentID=11;
//            }
//            if (sku_id.matches(Default.SKU_THEME2_PACKAGE2)){
//                contentID=12;
//            }
//            if (sku_id.matches(Default.SKU_THEME2_PACKAGE3)){
//                contentID=13;
//            }
//            if (sku_id.matches(Default.SKU_THEME2_PACKAGE4)){
//                contentID=14;
//            }
//            if (sku_id.matches(Default.SKU_THEME2_PACKAGE5)){
//                contentID=15;
//            }
//            if (sku_id.matches(Default.SKU_THEME2_PACKAGE6)){
//                contentID=16;
//            }
//            if (sku_id.matches(Default.SKU_THEME2_PACKAGE7)){
//                contentID=17;
//            }
//            if (sku_id.matches(Default.SKU_THEME2_PACKAGE8)){
//                contentID=18;
//            }
//            if (sku_id.matches(Default.SKU_THEME2_PACKAGE1_TO_4)){
//                contentID=19;
//                //return;
//            }
//            if (sku_id.matches(Default.SKU_THEME2_PACKAGE5_TO_8)){
//                contentID=20;
//                //return;
//            }
//            if (sku_id.matches(Default.SKU_THEME2_PACKAGE_ALL)){
//                contentID=21;
//                //return;
//            }
//            //update where theme content id & state <= 1
//            if (databaseHelper.updateSQL(ThemeContentTable.TABLE_NAME,values,
//                    ThemeContentTable.THEME_CONTENT_ID+ " = '" + contentID + "' AND "+ThemeContentTable.THEME_CONTENT_STATE+ "  <= '" + Default.STATE_PURCHASE + "'",null)){
//                Log.d("InappPurchase","Price & state update");
//            }
//        }
    }

    @Override
    public void purchased(String sku_id, boolean isTest) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!App.inapp_billing_supporter.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        databaseHelper.closeDataBase();
        App.inapp_billing_supporter.destroy();
        unregisterReceivers();
    }

//    @Override
//    public void onPause(){
//        super.onPause();
//        Log.e("Home Activity", "On Pause");
//        updateLearningTime(Default.PAUSE_STATE);
//    }
//    @Override
//    public void onResume(){
//        super.onResume();
//        Log.e("Home Activity", "On resume");
//        updateLearningTime(Default.RESUME_STATE);
//    }
//
//    /*
//    Update & Restore learning time based on activity state
//    @param int state- Possible value is RESUME_STATE & PAUSE_STATE
//     */
//    private void updateLearningTime(int state){
//        if (state==Default.RESUME_STATE){
//            //start each tme session
//            sharedPreferences.edit().putLong(Default.LEARNING_SESSION_KEY,System.currentTimeMillis()).commit();
//
//        }else if (state==Default.PAUSE_STATE){
//
//            long learningTime=System.currentTimeMillis()-sharedPreferences.getLong(Default.LEARNING_SESSION_KEY, System.currentTimeMillis());
//
//            sharedPreferences.edit().putLong(Default.LEARNING_HOUR_KEY,(sharedPreferences.getLong(Default.LEARNING_HOUR_KEY,Default.STATISTICS_DEFAULT_VALUE)+learningTime)).commit();
//
//        }
//    }


    //Registration receiver
    BroadcastReceiver mBroadcastReceiver = new BaseRegistrationReceiver() {
        @Override
        public void onRegisterActionReceive(Context context, Intent intent) {
            checkMessage(intent);
        }
    };


    //Push message receiver
    private BroadcastReceiver mReceiver = new BasePushMessageReceiver() {
        @Override
        protected void onMessageReceive(Intent intent) {

            try {
                JSONObject jsonObject = new JSONObject(intent.getExtras().getString(JSON_DATA_KEY));
                String title = jsonObject.getString("title");
                showNotification(title);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //JSON_DATA_KEY contains JSON payload of push notification.
            //showMessage("push message is " + intent.getExtras().getString(JSON_DATA_KEY));

        }
    };

    //Registration of the receivers
    public void registerReceivers() {
        IntentFilter intentFilter = new IntentFilter(getPackageName() + ".action.PUSH_MESSAGE_RECEIVE");

        registerReceiver(mReceiver, intentFilter, getPackageName() + ".permission.C2D_MESSAGE", null);

        registerReceiver(mBroadcastReceiver, new IntentFilter(getPackageName() + "." + PushManager.REGISTER_BROAD_CAST_ACTION));
    }

    public void unregisterReceivers() {
        //Unregister receivers on pause
        try {
            unregisterReceiver(mReceiver);
        } catch (Exception e) {
            // pass.
        }

        try {
            unregisterReceiver(mBroadcastReceiver);
        } catch (Exception e) {
            //pass through
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getIntent().hasExtra(Default.MENU_POSITION_EXTRA)) {
            displayView(getIntent().getIntExtra(Default.MENU_POSITION_EXTRA, 0));
        }

        //Re-register receivers on resume - unnecessary
//        registerReceivers();
    }

    @Override
    public void onPause() {
        super.onPause();

        //Unregister receivers on pause
        //unregisterReceivers();
    }


    private void checkMessage(Intent intent) {
        if (null != intent) {
            if (intent.hasExtra(PushManager.PUSH_RECEIVE_EVENT)) {
                try {
                    JSONObject jsonObject = new JSONObject(intent.getExtras().getString(PushManager.PUSH_RECEIVE_EVENT));
                    String title = jsonObject.getString("title");
                    showNotification(title);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // showMessage("push message is " + intent.getExtras().getString(PushManager.PUSH_RECEIVE_EVENT));
            } else if (intent.hasExtra(PushManager.REGISTER_EVENT)) {
                showMessage("register");
            } else if (intent.hasExtra(PushManager.UNREGISTER_EVENT)) {
                showMessage("unregister");
            } else if (intent.hasExtra(PushManager.REGISTER_ERROR_EVENT)) {
                showMessage("register error");
            } else if (intent.hasExtra(PushManager.UNREGISTER_ERROR_EVENT)) {
                showMessage("unregister error");
            }

            resetIntentValues();
        }
    }

    /**
     * Will check main Activity intent and if it contains any PushWoosh data, will clear it
     */
    private void resetIntentValues() {
        Intent mainAppIntent = getIntent();

        if (mainAppIntent.hasExtra(PushManager.PUSH_RECEIVE_EVENT)) {
            mainAppIntent.removeExtra(PushManager.PUSH_RECEIVE_EVENT);
        } else if (mainAppIntent.hasExtra(PushManager.REGISTER_EVENT)) {
            mainAppIntent.removeExtra(PushManager.REGISTER_EVENT);
        } else if (mainAppIntent.hasExtra(PushManager.UNREGISTER_EVENT)) {
            mainAppIntent.removeExtra(PushManager.UNREGISTER_EVENT);
        } else if (mainAppIntent.hasExtra(PushManager.REGISTER_ERROR_EVENT)) {
            mainAppIntent.removeExtra(PushManager.REGISTER_ERROR_EVENT);
        } else if (mainAppIntent.hasExtra(PushManager.UNREGISTER_ERROR_EVENT)) {
            mainAppIntent.removeExtra(PushManager.UNREGISTER_ERROR_EVENT);
        }

        setIntent(mainAppIntent);
    }

    private void showMessage(String message) {
        Log.e("push notification: ", message);
    }

    private void showNotification(String message) {
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("An Instant Reply")
                        .setContentText(message)
                        .setSound(notificationSound);


        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // builder.build().defaults = Notification.DEFAULT_ALL;
        manager.notify(12, builder.build());
    }

//    private int getNotificationIcon() {
//        boolean useWhiteIcon = (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP);
//        return useWhiteIcon ? R.mipmap.ic_launcher : R.mipmap.ic_launcher;
//    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //Check if we've got new intent with a push notification
        PushFragment.onNewIntent(this, intent);
        setIntent(intent);

        checkMessage(intent);
    }

    public void doOnRegistered(String registrationId) {
        Log.e("registrationId: ", registrationId);
    }

    public void doOnRegisteredError(String errorId) {
        Log.e("errorId: ", errorId);
    }

    public void doOnUnregistered(final String message) {
        Log.e("message: ", message);
    }

    public void doOnUnregisteredError(String errorId) {
        Log.e("errorId: ", errorId);
    }

    public void doOnMessageReceive(String message) {
        Log.e("message: ", message);
    }


//    @Override
//    public void onStart(){
//
//        super.onStart();
//
//        if (viewPager_tutorial.getVisibility() == View.INVISIBLE) {
//
//            getActionBar().show();
//
//        }
//
//    }
}

