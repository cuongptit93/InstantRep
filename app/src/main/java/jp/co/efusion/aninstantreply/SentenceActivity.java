package jp.co.efusion.aninstantreply;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.actionsheet.ActionSheet;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import jp.co.efusion.database.DatabaseHelper;
import jp.co.efusion.database.FavoriteTable;
import jp.co.efusion.database.SentenceSetTable;
import jp.co.efusion.database.SentenceTable;
import jp.co.efusion.database.ThemeContentTable;
import jp.co.efusion.listhelper.Log;
import jp.co.efusion.listhelper.ThreadManager;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.SentenceUtils;
import jp.co.efusion.utility.SettingUtils;


public class SentenceActivity extends ActionBarActivity implements ActionSheet.ActionSheetListener {
    private static final String TAG = SentenceActivity.class.getSimpleName();

    DatabaseHelper databaseHelper;
    Cursor cursor;
    String[] columns;
    SimpleCursorAdapter simpleCursorAdapter;

    SharedPreferences sharedPreferences;

    private ListView sentenceListView;
    int playMode, sentenceSetID, contentID, sentenceSetAuto, lastPlayed = 0;
    String themeId;

    private String[] actionSheetItems;
    private ArrayList<Integer> sentenceList;
    String setTitle, free_Set;
    //flag for identify favorite data set
    private Boolean IS_FAVORITE_SET = false, FREE_SET;

    //declare admob adview
    private AdView adView;

    Handler uiHandler;
    boolean goToPlayScreeen;
    MenuItem optionMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.theme_color_green)));
        setContentView(R.layout.activity_sentence);


        uiHandler = new Handler(getMainLooper());

        //show home back button
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        free_Set = preferences.getString("Free_Set", "");

        //add title from set title
        setTitle = getIntent().getStringExtra(SentenceSetTable.SET_TITLE);

        //initialize database helper
        databaseHelper = new DatabaseHelper(this);
        //open database
        databaseHelper.openDatabase();

        //initialize sharepreference
        sharedPreferences = getSharedPreferences(Default.SHARE_PREFERENCE_NAME,
                Context.MODE_PRIVATE);

        //extra intent data
        IS_FAVORITE_SET = getIntent().getBooleanExtra(Default.FAVORITE_SET, false);
        FREE_SET = getIntent().getBooleanExtra(Default.FREE_SET, false);
        sentenceSetID = getIntent().getIntExtra(SentenceSetTable.SET_ID, Default.ZERO);
        contentID = getIntent().getIntExtra(ThemeContentTable.THEME_CONTENT_ID, Default.ZERO);

        sentenceSetAuto = getIntent().getIntExtra(SentenceSetTable.SET_AUTO, Default.ZERO);
        goToPlayScreeen = getIntent().getBooleanExtra(Default.GO_TO_PLAY_SCREEN, false);
        themeId = getIntent().getStringExtra("Theme_Id_No");
        playMode = getIntent().getIntExtra(Default.PLAY_MODE, Default.ZERO);

        Log.d(TAG, String.format("isFavoriteSet %b; freeSet %b; sentenceSetId %d; contentId %d; " +
                        "sentenceSetAuto %d; playMode %d; goToPlayScreeen %b",
                IS_FAVORITE_SET, FREE_SET, sentenceSetID, contentID, sentenceSetAuto, playMode, goToPlayScreeen));

        sentenceList = new ArrayList<Integer>();

        //initialize uiview
        sentenceListView = (ListView) findViewById(R.id.sentenceListView);

//        //load sentence data
//        loadSentenceData();
        //configure the adView Here
        adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("83379D0B53764804B8F94258363B28E2")
                .build();
        adView.loadAd(adRequest);
        //update add view
        updateAdView();

        //list view click event implementation
        sentenceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                navigateToPlaying(position, false);
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();

        //get sentenceSetID and Title to PlayActivity (CallBack)
        if(sharedPreferences.getInt("sentenceSetIDCallBack", Default.ZERO)!=0){
            sentenceSetID = sharedPreferences.getInt("sentenceSetIDCallBack", Default.ZERO);
            contentID = sharedPreferences.getInt("contentIDCallBack", Default.ZERO);
            setTitle = sharedPreferences.getString("titleCallBack", "");
        }

        getSupportActionBar().setTitle(setTitle);
        loadSentenceData();

        //load last played
        Cursor cr = databaseHelper.getQueryResultData(SentenceSetTable.TABLE_NAME,
                new String[]{SentenceSetTable.LAST_PLAYED_CHUNK, SentenceSetTable.LAST_PLAYED_NORMAL},
                SentenceSetTable.SET_AUTO + " = '" + sentenceSetAuto + "'", null, null, null, null, null);
        try {
            cr.moveToFirst();
            if (playMode == Default.CHUNK_PLAY_MODE) {
                try {
                    lastPlayed = cr.getInt(cr.getColumnIndex(SentenceSetTable.LAST_PLAYED_CHUNK));
                } catch (Exception e) {

                }

            } else if (playMode == Default.NORMAL_PLAY_MODE) {
                try {
                    lastPlayed = cr.getInt(cr.getColumnIndex(SentenceSetTable.LAST_PLAYED_NORMAL));
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {

        }

        //define action sheet item
        if (lastPlayed == Default.ZERO) {
            actionSheetItems = getResources().getStringArray(R.array.action_sheet_initial_item);
        } else {
            actionSheetItems = getResources().getStringArray(R.array.action_sheet_item);
        }
        if (playMode == Default.NORMAL_PLAY_MODE) {
            List<String> list = new ArrayList<String>(Arrays.asList(actionSheetItems));
            list.remove(0);
            actionSheetItems = list.toArray(new String[0]);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sentence, menu);
        optionMenu = menu.findItem(R.id.action_option);
        if (IS_FAVORITE_SET) {
            //hide option menu
            optionMenu.setVisible(false);
            this.invalidateOptionsMenu();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
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
    Load sentence list from database
     */
    private void loadSentenceData() {
        ThreadManager.getInstance().execTask(new Runnable() {
            @Override
            public void run() {
                //check shuffle all purchase
                if (contentID == Default.ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID) {
                    String contentIds = SentenceUtils.getPurchaseContentIds(databaseHelper, themeId);
                    if (contentIds != null) {
                        String selectionStr = SentenceTable.CONTENT_ID + " in " + contentIds;
                        cursor = databaseHelper.getQueryResultData(SentenceTable.TABLE_NAME, columns,
                                selectionStr,
                                null, null, null, SentenceTable.SENTENCE_NO + " ASC ", null);
                    }

                } else {
                    //check IS FAVORITE_SET then load data
                    if (IS_FAVORITE_SET) {
                        if (playMode == Default.NORMAL_PLAY_MODE) {

                            cursor = databaseHelper.getQueryResultData("SELECT " + SentenceTable.TABLE_NAME + "." + SentenceTable.SENTENCE_ID + " as _id, " + SentenceTable.TABLE_NAME + "." + SentenceTable.SENTENCE_ID
                                    + "," + SentenceTable.SENTENCE_NO + "," + SentenceTable.SENTENCE_TITLE + "," + SentenceTable.SENTENCE_QUESTION_TEXT
                                    + " FROM " + FavoriteTable.TABLE_NAME + "," + SentenceTable.TABLE_NAME
                                    + " WHERE " + SentenceTable.TABLE_NAME + "." + SentenceTable.SENTENCE_ID + "=" + FavoriteTable.TABLE_NAME + "." + FavoriteTable.SENTENCE_ID
                                    + " AND " + FavoriteTable.NORMAL_PLAY_MODE + "=" + 1
                                    + " ORDER BY " + FavoriteTable.TABLE_NAME + "." + FavoriteTable.FAVORITE_ID + " DESC");
                        } else {
                            cursor = databaseHelper.getQueryResultData("SELECT " + SentenceTable.TABLE_NAME + "." + SentenceTable.SENTENCE_ID + " as _id, " + SentenceTable.TABLE_NAME + "." + SentenceTable.SENTENCE_ID
                                    + "," + SentenceTable.SENTENCE_NO + "," + SentenceTable.SENTENCE_TITLE + "," + SentenceTable.SENTENCE_QUESTION_TEXT
                                    + " FROM " + FavoriteTable.TABLE_NAME + "," + SentenceTable.TABLE_NAME
                                    + " WHERE " + SentenceTable.TABLE_NAME + "." + SentenceTable.SENTENCE_ID + "=" + FavoriteTable.TABLE_NAME + "." + FavoriteTable.SENTENCE_ID
                                    + " AND " + FavoriteTable.CHUNK_PLAY_MODE + "=" + 1
                                    + " ORDER BY " + FavoriteTable.TABLE_NAME + "." + FavoriteTable.FAVORITE_ID + " DESC");
                        }

                    } else {
                        columns = new String[]{SentenceTable.SENTENCE_ID + " as _id", SentenceTable.SENTENCE_ID, SentenceTable.SENTENCE_NO,
                                SentenceTable.SENTENCE_TITLE, SentenceTable.SENTENCE_QUESTION_TEXT};

                        if (free_Set.equals("true")) {
                            cursor = databaseHelper.getQueryResultData(SentenceTable.TABLE_NAME, columns, SentenceTable.CONTENT_ID + " = '" + contentID + "'",
                                    null, null, null, SentenceTable.SENTENCE_NO + " ASC ", null);
                        } else {
                            /*cursor = databaseHelper.getQueryResultData(SentenceTable.TABLE_NAME, columns, SentenceTable.SET_ID + " = '" + sentenceSetID + "'",
                                    null, null, null, SentenceTable.SENTENCE_NO + " ASC ", null);*/

                            cursor = databaseHelper.getQueryResultData(SentenceTable.TABLE_NAME, columns, SentenceTable.SET_ID + " = '" + sentenceSetID + "' AND " + SentenceTable.CONTENT_ID + " = '" + contentID + "'",
                                    null, null, null, SentenceTable.SENTENCE_NO + " ASC ", null);
                        }
                    }
                }

                final String[] col = new String[]{SentenceTable.SENTENCE_NO, SentenceTable.SENTENCE_QUESTION_TEXT};
                final int[] to = new int[]{R.id.sentenceNoTextView, R.id.sentenceTitleTextView};

                if (!goToPlayScreeen) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            simpleCursorAdapter = new SimpleCursorAdapter(SentenceActivity.this, R.layout.sentence_list_item, cursor, col, to) {
                                @Override
                                public void setViewText(TextView v, String text) {
                                    if (v.getId() == R.id.sentenceNoTextView) {
                                        try {
                                            int i = Integer.parseInt(text);
                                            super.setViewText(v, String.format("%04d", i));
                                        } catch (NumberFormatException nfe) {
                                            super.setViewText(v, text);
                                        }

                                    } else {
                                        super.setViewText(v, text);
                                    }
                                }
                            };
                            sentenceListView.setAdapter(simpleCursorAdapter);
                        }
                    });
                }

                if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                    //clear lis
                    if (sentenceList != null) {
                        sentenceList.clear();
                    }
                    do {
                        try {
                            sentenceList.add(cursor.getInt(cursor.getColumnIndex(SentenceTable.SENTENCE_ID)));
                        } catch (Exception e) {

                        }
                    } while (cursor.moveToNext());
                }

                if (goToPlayScreeen) {
                    if (contentID == Default.ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID) {
                        navigateToPlaying(0, true);
                    } else {
                        navigateToPlaying(0, false);
                    }
                    finish();
                }
                android.util.Log.e("Sentence List", sentenceList + "");
            }
        });
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
    Show Action Sheet
     */
    private void showActionSheet() {
        ActionSheet.createBuilder(this, getSupportFragmentManager())
                .setCancelButtonTitle(R.string.action_sheet_cancel)
                .setOtherButtonTitles(actionSheetItems)
                .setCancelableOnTouchOutside(true)
                .setListener(this).show();
    }

    @Override
    public void onOtherButtonClick(ActionSheet actionSheet, int index) {

        if (playMode == Default.NORMAL_PLAY_MODE) {
            //normal play
            if (actionSheetItems.length == 1 && index == 0) {
                //start after shuffle
                navigateToPlaying(0, true);
            } else if (actionSheetItems.length == 2 && index == 0) {
                //find the indext of last played sentence
                int i = sentenceList.indexOf(lastPlayed);
                navigateToPlaying(i, false);

            } else if (actionSheetItems.length == 2 && index == 1) {
                //start after shuffle
                navigateToPlaying(0, true);
            }
            switch (index){
                case 0:
                    Intent i = new Intent(SentenceActivity.this, HomeActivity.class);
                    startActivity(i);
                    finish();
                    break;
                case 1:
                    Intent intent = new Intent(SentenceActivity.this, HomeActivity.class);
                    intent.putExtra("check", true);
                    startActivity(intent);
                    break;
                case 2:
                    //find the indext of last played sentence
                    int b = sentenceList.indexOf(lastPlayed);
                    navigateToPlaying(b, false);
                    break;
                case 3:
                    navigateToPlaying(0, true);
                    break;
                default:
                    break;
            }
        } else if (playMode == Default.CHUNK_PLAY_MODE) {
            /*//chunk play
            if (actionSheetItems.length == 2 && index == 0) {
                //normal play
                navigateToPlaying(0, false);
                Toast.makeText(getApplicationContext(), "2.1", Toast.LENGTH_LONG).show();

            } else if (actionSheetItems.length == 2 && index == 1) {
                //start after shuffle
                navigateToPlaying(0, true);
                Toast.makeText(getApplicationContext(), "2.2", Toast.LENGTH_LONG).show();

            } else if (actionSheetItems.length == 3 && index == 0) {
                //normal play
                Toast.makeText(getApplicationContext(), "3.1", Toast.LENGTH_LONG).show();
                navigateToPlaying(0, false);

            } else if (actionSheetItems.length == 3 && index == 1) {
                //find the indext of last played sentence
                Toast.makeText(getApplicationContext(), "3.2", Toast.LENGTH_LONG).show();
                int i = sentenceList.indexOf(lastPlayed);
                navigateToPlaying(i, false);

            } else if (actionSheetItems.length == 3 && index == 2) {
                //start after shuffle
                Toast.makeText(getApplicationContext(), "3.3", Toast.LENGTH_LONG).show();
                navigateToPlaying(0, true);

            }*/
            switch (index){
                case 0:
                    Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    break;
                case 1:
                    Intent i = new Intent(SentenceActivity.this, HomeActivity.class);
                    i.putExtra("check", true);
                    startActivity(i);
                    break;
                case 2:
                    //normal play
                    navigateToPlaying(0, false);
                    break;
                case 3:
                    //start after shuffle
                    navigateToPlaying(0, true);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onDismiss(ActionSheet actionSheet, boolean isCancle) {
    }

    /*
    navigate to playing activity based on starting point
    @param int startPoint indicate where start from
           Boolean isShuffle indicate array will shuffle or not
     */
    private void navigateToPlaying(final int startPoint, final Boolean isShuffled) {
        ThreadManager.getInstance().execTask(new Runnable() {
            @Override
            public void run() {
                Intent intent = null;
                //check play mode
                if (playMode == Default.CHUNK_PLAY_MODE) {
                    intent = new Intent(SentenceActivity.this, ChunkPlayActivity.class);
                } else if (playMode == Default.NORMAL_PLAY_MODE) {
                    intent = new Intent(SentenceActivity.this, NormalPlayActivity.class);
                }
                if (intent != null && sentenceList.size() > 0) {
                    intent.putExtra(Default.START_POINT, startPoint);
                    if (isShuffled) {
                        intent.putIntegerArrayListExtra(Default.PLAYABLE_SENTENCE_LIST, shuffleSentenceArray(sentenceList));
                    } else {
                        intent.putIntegerArrayListExtra(Default.PLAYABLE_SENTENCE_LIST, sentenceList);
                    }
                    intent.putExtra(Default.IS_SHUFFLE_MODE, isShuffled);
                    intent.putExtra(Default.FAVORITE_SET, IS_FAVORITE_SET);
                    intent.putExtra(Default.FREE_SET, FREE_SET);
                    intent.putExtra(Default.THEME_ID_EXTRA, themeId);
                    intent.putExtra(ThemeContentTable.THEME_CONTENT_ID, contentID);
                    intent.putExtra(SentenceSetTable.SET_AUTO, sentenceSetAuto);
                    intent.putExtra(SentenceSetTable.SET_ID, sentenceSetID);
                    intent.putExtra(SentenceSetTable.SET_TITLE, setTitle);

                    startActivity(intent);
                }
            }
        });
    }

    /*
    Shuffle array list
    @param arraylist which will be shuffled
    @return arraylist -shuffled arraylist
     */
    private ArrayList<Integer> shuffleSentenceArray(ArrayList<Integer> mysentenceList) {
        ArrayList<Integer> shuffledArrayList = new ArrayList<Integer>();
        shuffledArrayList.addAll(mysentenceList);
        Random rnd = new Random();
        for (int i = shuffledArrayList.size() - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            int a = shuffledArrayList.get(index);
            shuffledArrayList.set(index, shuffledArrayList.get(i));
            shuffledArrayList.set(i, a);
        }
        return shuffledArrayList;
    }


    @Override
    public void onPause() {
        super.onPause();
        android.util.Log.e("sentence list", "On Pause");
        // updateLearningTime(Default.PAUSE_STATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.e("sentence list", "On resume");
        // updateLearningTime(Default.RESUME_STATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //all connection close here
        android.util.Log.e("sentence list", "Destroy ");
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


}
