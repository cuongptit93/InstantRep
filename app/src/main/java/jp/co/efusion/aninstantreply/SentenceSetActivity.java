package jp.co.efusion.aninstantreply;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.baoyz.actionsheet.ActionSheet;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.Arrays;

import jp.co.efusion.database.DatabaseHelper;
import jp.co.efusion.database.SentenceSetTable;
import jp.co.efusion.database.ThemeContentTable;
import jp.co.efusion.database.ThemeTable;
import jp.co.efusion.listhelper.Log;
import jp.co.efusion.listhelper.ThreadManager;
import jp.co.efusion.utility.SentenceUtils;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.SettingUtils;


public class SentenceSetActivity extends ActionBarActivity  implements ActionSheet.ActionSheetListener{
    private static final String TAG = SentenceSetActivity.class.getSimpleName();

    DatabaseHelper databaseHelper;
    volatile Cursor cursor;
    String[] columns;
    SimpleCursorAdapter simpleCursorAdapter;
    SharedPreferences sharedPreferences;
    private String[] actionSheetItems;

    private ListView sentenceSetListView;
    private int contentID;
    String free_Set;
    String theme_Id;

    MenuItem optionMenu;

    //declare admob adview
    private AdView adView;

    Handler uiHandler;
    boolean goToPlayScreen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiHandler = new Handler(getMainLooper());

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.theme_color_green)));
        setContentView(R.layout.activity_sentence_set);
        //show home back button
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        free_Set = preferences.getString("Free_Set", "");

        contentID = getIntent().getIntExtra(ThemeContentTable.THEME_CONTENT_ID, Default.ZERO);

        Bundle bundle = getIntent().getExtras();
        theme_Id = bundle.getString("Theme_Id_No");

        //add title from theme title
        if (theme_Id.matches(Default.THEME1_ID)) {
            getSupportActionBar().setTitle("ALL IN ONE Basic");
        }
        else if (theme_Id.matches(Default.THEME2_ID))
            getSupportActionBar().setTitle("ｳｨﾆﾝｸﾞﾌｨﾆｯｼｭ");

        if (contentID == Default.ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID  && !theme_Id.matches(Default.THEME1_ID)) {
            goToPlayScreen = true;
        } else {
            goToPlayScreen = false;
        }

        //load item Home And Setting to Action sheet.
        final String[] itemAction = getResources().getStringArray(R.array.action_sheet_initial_item);
        actionSheetItems = Arrays.copyOf(itemAction, 2);

        Log.d(TAG, String.format("free_Set %s; contentID %d; theme_Id %s", free_Set, contentID, theme_Id));

        //initialize database helper
        databaseHelper = new DatabaseHelper(this);
        //open database
        databaseHelper.openDatabase();

        //initialize sharepreference
        sharedPreferences = getSharedPreferences(Default.SHARE_PREFERENCE_NAME,
                Context.MODE_PRIVATE);

        //initialize view
        sentenceSetListView = (ListView) findViewById(R.id.sentenceSetListView);

        loadSentenceSetData();


        //configure the adView Here
        adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("83379D0B53764804B8F94258363B28E2")
                .build();
        adView.loadAd(adRequest);
        //update add view
        updateAdView();

        //set list view click litenser
        sentenceSetListView.setOnItemClickListener(

                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        //re set goToPlayScreen flag
                        if (contentID == Default.ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID  && theme_Id.matches(Default.THEME2_ID)) {
                            if (position == 0) {
                                goToPlayScreen = true;
                            } else {
                                goToPlayScreen = false;
                            }
                        }
                        //navigate to Set Details Activity
                        cursor.moveToPosition(position);
                        navigateToDetails(cursor, false);
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        if(contentID!=-1){
            //get sentenceSetID and Title to PlayActivity (CallBack)
            if(sharedPreferences.getInt("contentIDCallBack", Default.ZERO)!=0){
                if(contentID!=1){
                    contentID = sharedPreferences.getInt("contentIDCallBack", Default.ZERO);
                    loadSentenceSetData();
                }
                //clear sharedPreferences to Activity Play
                sharedPreferences.edit().remove("contentIDCallBack").commit();
                sharedPreferences.edit().remove("sentenceSetIDCallBack").commit();
                sharedPreferences.edit().remove("titleCallBack").commit();
                sharedPreferences.edit().remove("checkFreeSet").commit();
            }
        }
        else{
            //clear sharedPreferences to Activity Play
            sharedPreferences.edit().remove("contentIDCallBack").commit();
            sharedPreferences.edit().remove("sentenceSetIDCallBack").commit();
            sharedPreferences.edit().remove("titleCallBack").commit();
            sharedPreferences.edit().remove("checkFreeSet").commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sentence, menu);
        optionMenu = menu.findItem(R.id.action_option);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //Open setting fragment
                finish();
                return true;

            case R.id.action_option:
                showActionSheet();
                return true;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    Load sentence data of theme content
     */
    private void loadSentenceSetData() {
        ThreadManager.getInstance().execTask(new Runnable() {
            @Override
            public void run() {
                columns = new String[]{SentenceSetTable.SET_AUTO + " as _id", SentenceSetTable.SET_AUTO, SentenceSetTable.SET_ID,
                        SentenceSetTable.THEME_CONTENT_ID,
                        SentenceSetTable.SET_TITLE, SentenceSetTable.LAST_PLAYED_CHUNK, SentenceSetTable.LAST_PLAYED_NORMAL,
                        SentenceSetTable.SET_DETAILS};

                if (contentID == Default.ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID) {
                    //get all purchase theme_content_id
                    String contentIds = SentenceUtils.getPurchaseContentIds(databaseHelper, theme_Id);
                    if (contentIds != null) {
                        addAllPurchaseItem(contentIds);
                    } else {
                        return;
                    }
                } else {
                    cursor = databaseHelper.getQueryResultData(SentenceSetTable.TABLE_NAME, columns, SentenceSetTable.THEME_CONTENT_ID + " = '" + contentID + "'", null, null, null, null, null);
                }

                // THE DESIRED COLUMNS TO BE BOUND
                final String[] col = new String[]{SentenceSetTable.SET_TITLE};
                // THE XML DEFINED VIEWS WHICH THE DATA WILL BE BOUND TO
                final int[] to = new int[]{R.id.setTitleTextView};

                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        simpleCursorAdapter = new SimpleCursorAdapter(SentenceSetActivity.this, R.layout.set_list_item, cursor, col, to);

                        //check for free set
                        if (!getIntent().getBooleanExtra(Default.FREE_SET, false)) {
                            //set adapter
                            sentenceSetListView.setAdapter(simpleCursorAdapter);
                        }
                    }
                });

                if (getIntent().getBooleanExtra(Default.FREE_SET, false)) {
                    //navigate to Set Details Activity
                    cursor.moveToFirst();
                    navigateToDetails(cursor, true);
                }
            }
        });
    }

    private void addAllPurchaseItem(String contentIds) {
        String selectionStr = SentenceSetTable.THEME_CONTENT_ID + " in " + contentIds;
        cursor = databaseHelper.getQueryResultData(SentenceSetTable.TABLE_NAME, columns,
                selectionStr,
                null, null, null, null, null);
        if (cursor != null) {
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id",
                    SentenceSetTable.SET_AUTO, SentenceSetTable.SET_ID,
                    SentenceSetTable.THEME_CONTENT_ID,
                    SentenceSetTable.SET_TITLE, SentenceSetTable.LAST_PLAYED_CHUNK,
                    SentenceSetTable.LAST_PLAYED_NORMAL,
                    SentenceSetTable.SET_DETAILS});

            //add "all purchase sentences"
            MatrixCursor.RowBuilder rowBuilder = matrixCursor.newRow();
            rowBuilder.add(Default.ALL_PURCHASE_SENTENCE_SET_ID);
            rowBuilder.add(Default.ALL_PURCHASE_SENTENCE_SET_ID);
            rowBuilder.add(Default.ALL_PURCHASE_SENTENCE_SET_ID);
            rowBuilder.add(contentID);
            rowBuilder.add(getApplicationContext().getResources().getString(R.string.shuffle_all_purchase_sentences));
            rowBuilder.add(0);
            rowBuilder.add(0);
            rowBuilder.add("");

            while (cursor.moveToNext()) {
                addRowContent(cursor, matrixCursor);
            }

            cursor = matrixCursor;
        }
    }

    private void addRowContent(Cursor cursor, MatrixCursor matrixCursor) {
        try {
            MatrixCursor.RowBuilder rowBuilder = matrixCursor.newRow();
            rowBuilder.add(cursor.getInt(cursor.getColumnIndexOrThrow(SentenceSetTable.SET_AUTO)));
            rowBuilder.add(cursor.getInt(cursor.getColumnIndexOrThrow(SentenceSetTable.SET_AUTO)));
            rowBuilder.add(cursor.getInt(cursor.getColumnIndexOrThrow(SentenceSetTable.SET_ID)));
            rowBuilder.add(cursor.getInt(cursor.getColumnIndexOrThrow(SentenceSetTable.THEME_CONTENT_ID)));
            rowBuilder.add(cursor.getString(cursor.getColumnIndexOrThrow(SentenceSetTable.SET_TITLE)));
            rowBuilder.add(cursor.getInt(cursor.getColumnIndexOrThrow(SentenceSetTable.LAST_PLAYED_CHUNK)));
            rowBuilder.add(cursor.getInt(cursor.getColumnIndexOrThrow(SentenceSetTable.LAST_PLAYED_NORMAL)));
            rowBuilder.add(cursor.getString(cursor.getColumnIndexOrThrow(SentenceSetTable.SET_DETAILS)));
        } catch (Exception e) {
        }
    }

    /*
    Define visibility based on purchase status
     */
    private void updateAdView() {
        //configure adview
        if (sharedPreferences.getBoolean(Default.IN_APP_PURCHASE, false)) {
            //in visible the adview
            adView.setVisibility(View.GONE);
        }
    }

    /*
    Navigate to details activity
    @params Cursor cr -to put various data from cursor
             Boolean hiddenable- Represent where the activity will hide(case of FREE Set) or not
     */
    private void navigateToDetails(Cursor cr, Boolean hiddenable) {

        Intent intent = new Intent(SentenceSetActivity.this, SentenceSetDetailsActivity.class);
        intent.putExtra(ThemeContentTable.THEME_CONTENT_ID, cr.getInt(cr.getColumnIndex(SentenceSetTable.THEME_CONTENT_ID)));
        intent.putExtra(SentenceSetTable.SET_AUTO, cr.getInt(cr.getColumnIndex(SentenceSetTable.SET_AUTO)));
        intent.putExtra(SentenceSetTable.SET_ID, cr.getInt(cr.getColumnIndex(SentenceSetTable.SET_ID)));

        if (free_Set.equals("true"))
            intent.putExtra(ThemeTable.THEME_TITLE, getIntent().getStringExtra(ThemeTable.THEME_TITLE));

        else
            intent.putExtra(SentenceSetTable.SET_TITLE, cr.getString(cr.getColumnIndex(SentenceSetTable.SET_TITLE)));
//        intent.putExtra(SentenceSetTable.LAST_PLAYED_CHUNK,lastChunk);
//        intent.putExtra(SentenceSetTable.LAST_PLAYED_NORMAL,lastNormal);
        intent.putExtra(SentenceSetTable.SET_DETAILS, cr.getString(cr.getColumnIndex(SentenceSetTable.SET_DETAILS)));
        intent.putExtra(Default.FREE_SET, hiddenable);
        intent.putExtra("Theme_Id_No", theme_Id);
        intent.putExtra(Default.GO_TO_PLAY_SCREEN, goToPlayScreen);
        if (hiddenable) {
            startActivityForResult(intent, Default.HIDDENABLE_REQUEST_CODE);
        } else {
            startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == Default.HIDDENABLE_REQUEST_CODE) {
            //finish the activity
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("sentence set Activity", "On Pause");
        //updateLearningTime(Default.PAUSE_STATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("sentence set Activity", "On resume");
        //updateLearningTime(Default.RESUME_STATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //all connection close here
        Log.e("sentence set details ", "Destroy ");
        databaseHelper.closeDataBase();
    }

    /*
    Update & Restore learning time based on activity state
    @param int state- Possible value is RESUME_STATE & PAUSE_STATE
     */
    private void updateLearningTime(int state) {
        if (state == Default.RESUME_STATE) {
            //start each tme session
            sharedPreferences.edit().putLong(Default.LEARNING_SESSION_KEY, System.currentTimeMillis()).commit();

        } else if (state == Default.PAUSE_STATE) {
//            long learningTime = System.currentTimeMillis() - sharedPreferences.getLong(Default.LEARNING_SESSION_KEY, System.currentTimeMillis());
//            SettingUtils.setStudyTime(sharedPreferences, SettingUtils.getStudyTime(this) + learningTime);
            SettingUtils.setStudyTime(this);
        }
    }

    @Override
    public void onDismiss(ActionSheet actionSheet, boolean isCancel) {

    }

    @Override
    public void onOtherButtonClick(ActionSheet actionSheet, int index) {
        //get Positon showActionSheet
        switch (index) {
            case 0:
                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            case 1:
                Intent i = new Intent(SentenceSetActivity.this, HomeActivity.class);
                i.putExtra("check", true);
                startActivity(i);
                break;
            default:
                break;
        }

    }

    private void showActionSheet() {
        ActionSheet.createBuilder(this, getSupportFragmentManager())
                .setCancelButtonTitle(R.string.action_sheet_cancel)
                .setOtherButtonTitles(actionSheetItems)
                .setCancelableOnTouchOutside(true)
                .setListener(this).show();
    }
}
