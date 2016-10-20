package jp.co.efusion.aninstantreply;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.actionsheet.ActionSheet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.co.efusion.MediaManager.MediaCompletionListener;
import jp.co.efusion.MediaManager.MediaPlayerManager;
import jp.co.efusion.MediaManager.SoundManager;
import jp.co.efusion.database.DatabaseHelper;
import jp.co.efusion.database.FavoriteTable;
import jp.co.efusion.database.SentenceSetTable;
import jp.co.efusion.database.SentenceTable;
import jp.co.efusion.database.ThemeContentTable;
import jp.co.efusion.listhelper.CacheLastSentence;
import jp.co.efusion.listhelper.Log;
import jp.co.efusion.listhelper.ThreadManager;
import jp.co.efusion.utility.CustomIOSDialog;
import jp.co.efusion.utility.CustomPagerAdapter;
import jp.co.efusion.utility.CustomViewPager;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.IOSDialogListener;
import jp.co.efusion.utility.SentenceUtils;
import jp.co.efusion.utility.SettingUtils;

public class PlayActivity extends ActionBarActivity implements MediaCompletionListener, ActionSheet.ActionSheetListener {
    private static final String TAG = PlayActivity.class.getSimpleName();

    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_PATH = 250;
    private static final int SWIPE_THRESHOLD_VELOCITY = 100;

    static final long TIME_COUNT = 100, TIME_DELAY = 300;

    DatabaseHelper databaseHelper;
    volatile Cursor cursor, favoriteCursor;
    SharedPreferences sharedPreferences;
    String[] columns;
    MediaPlayerManager mediaPlayerManager= null, repeatPlayerManager = null;
    SoundManager soundManager = null,  repeatSoundManager = null;
    Handler uiHandler;
    GestureDetector gestureDetector = null;

    CountDownTimer autoPlayTimer, coutTimeMediaPlayer;
    long millisUntilFinishedCountTime;

    volatile ArrayList<Integer> sentenceList;
    volatile int startPoint;
    //list contentId and sentenceSetId for all purchase content
    volatile ArrayList<Integer> purchaseList;
    volatile int purchaseIndicator;
    int sentenceSetId, contentID, sentenceSetAuto;
    String themeId, setTitle, titleCallBack;
    boolean isShuffled, checkSetting = false, checkPause = false, checkNullAudio = false;
    ///, checkScreenoOff = false
    String free_Set, theme_no;
    //flag for identify favorite data set
    Boolean IS_FAVORITE_SET = false, FREE_SET;

    //flag for fast time pause
    Boolean FIRSTTIME_RESUME = true;

    //flag for tracking audio player pause mode
    int AUDIO_CURRENT_POSITION = Default.ZERO, AUDIO_CURRENT_POSITION_SERVICE = Default.ZERO;

    //flag for tracking timer running && Sentence limit Alert
    Boolean IS_TIMER_RUNNING = false, IS_ALERT_SHOWING = false, IS_FROM_BACKGROUND_SERVICE = false;
    long TIMER_REMAINING_TIME = 0, TIMER_REMAINING_TIME_SERVICE = Default.ZERO;

    float audioSpeed;

    //flag for autoplay mode
    Boolean AUTO_PLAY_ON = false;
    //flag for play state
    int PLAY_STATE;

    TextView titleTextView;
    //UIView declaration
    Button autoPLayButton;
    CustomViewPager swipeViewPager;
    CustomPagerAdapter customPagerAdapter;
    int currentPage = 1, previousPage = 1;
    private String[] actionSheetItems;
    String pathPlayAudio;

    //custom alert dialog
    CustomIOSDialog customIOSDialog;

    TextView qSentenceNo, qPageTextView, aSentenceNo, qJapaneseTextView, aJapaneseTextView,
            aEnglishTextView, aDetailsTextView, aPageTextView;
    ImageButton qFavoriteButton, aFavoriteButton;
    ProgressBar qProgressBar;
    ScrollView scrollView;

    int sentenceSetIDCallBack, contentIDCallBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_normal_play);

        //show home back button
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //hide action bar
        getSupportActionBar().hide();
        //add title from theme title
        getSupportActionBar().setTitle(getIntent().getStringExtra(SentenceSetTable.SET_TITLE));

        gestureDetector = new GestureDetector(new MyGestureDetector());

        //btnShowActionSheet = (Button) findViewById(R.id.btnShowActionSheet);

        initDatabase();
        if (this instanceof ChunkPlayActivity) {
            getIntentData(Default.CHUNK_PLAY_MODE, databaseHelper, sharedPreferences);
        } else {
            getIntentData(Default.NORMAL_PLAY_MODE, databaseHelper, sharedPreferences);
        }

        initMainView();
        initPageView(customPagerAdapter);

        configureAutoPlayView();

        startNewSentenceSession(false, Default.ZERO);

        //load item Home And Setting to Action sheet.
        final String[] itemAction = getResources().getStringArray(R.array.action_sheet_initial_item);
        actionSheetItems = Arrays.copyOf(itemAction, 2);

    }

    @Override
    protected void onStart() {
        titleCallBack = setTitle;
        super.onStart();

        //get and send value audioSpeed to Activity.
        audioSpeed = (float) sharedPreferences.getInt(Default.SPEED_SETTING, Default.DEFAULT_SPEED_SETTING)/10;
        //check pause Activity
        if (checkPause) {
            if (sharedPreferences.getBoolean(Default.AUTO_PLAY_ENABLE, Default.AUTO_PLAY_ENABLE_DEFAULT)) {
                AUTO_PLAY_ON = true;
            } else {
                AUTO_PLAY_ON = false;
            }
            IS_TIMER_RUNNING = true;
            resumeMediaPlay();
            configureAutoPlayView();
            updateProgressBarVisibility();
            CountDownTimePlayAudio(millisUntilFinishedCountTime + TIME_DELAY, TIME_COUNT);
            checkPause = false;
        } else {
            checkPause = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLearningTime(Default.RESUME_STATE);
        //check for first time or not
        if (!FIRSTTIME_RESUME) {
            bindDataFromService();
        } else {
            FIRSTTIME_RESUME = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateLearningTime(Default.PAUSE_STATE);
        if (IS_ALERT_SHOWING) {
            return;
        }
        startAudioService();
        //bind to service
        doBindService();

        /*//clear
        clear();
        clearViewData();*/
    }

    @Override
    public void onBackPressed() {
        backPressed();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop()");
        //update last sentence list
        CacheLastSentence.updatePlaySentence(this, sentenceList, startPoint, sentenceSetId, setTitle);

        super.onStop();

        if (checkSetting) {
            checkPause = true;
            checkSetting = false;
            coutTimeMediaPlayer.cancel();
        } /*else {
            if (mediaPlayerManager != null)
                mediaPlayerManager.stopAudio();
            if (repeatPlayerManager != null)
                repeatPlayerManager.stopAudio();
        }*/

    }

    @Override
    protected void onDestroy() {
        databaseHelper.closeDataBase();
        clear();
        clearViewData();

        doUnbindService();
        if (serviceIntent != null) {
            stopService(serviceIntent);
            serviceIntent = null;
        }
        super.onDestroy();
    }

    private void initDatabase() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        free_Set = preferences.getString("Free_Set", "");
        theme_no = preferences.getString("Theme_No", "");

        //initialize database helper
        databaseHelper = new DatabaseHelper(this);
        //open database
        databaseHelper.openDatabase();

        //initialize sharepreference
        sharedPreferences = getSharedPreferences(Default.SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);

        if (sharedPreferences.getBoolean(Default.AUTO_PLAY_ENABLE, Default.AUTO_PLAY_ENABLE_DEFAULT)) {
            AUTO_PLAY_ON = true;
        }
    }

    private void initMainView() {

        //initialize UI
        titleTextView = (TextView) findViewById(R.id.titleTextView);

        autoPLayButton = (Button) findViewById(R.id.autoPLayButton);

        initViewPager();

        if (setTitle == null || setTitle.equals("")) {
            setSentencesSetTitle(databaseHelper, sentenceSetId);
        } else {
            titleTextView.setText(setTitle);
        }
    }

    void initPagerAdapter() {
    }

    private void initViewPager() {
        initPagerAdapter();
        swipeViewPager = (CustomViewPager) findViewById(R.id.swipeViewPager);
        swipeViewPager.setAdapter(customPagerAdapter);

        //set callback
        swipeViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                previousPage = currentPage;
                currentPage = position;
                sentenceSetIDCallBack = sentenceSetId;
                contentIDCallBack = SentenceUtils.getContenID(databaseHelper, sentenceSetId);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        swipeViewPager.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return false;
                } else {
                    return true;
                }
            }
        });
    }

    protected void onInitPageView(CustomPagerAdapter customPagerAdapter) {

    }

    private void initPageView(CustomPagerAdapter customPagerAdapter) {
        if (customPagerAdapter == null)
            return;

        qProgressBar = (ProgressBar) customPagerAdapter.findViewById(1, R.id.qProgressBar);
        qFavoriteButton = (ImageButton) customPagerAdapter.findViewById(1, R.id.qFavoriteButton);
        qSentenceNo = (TextView) customPagerAdapter.findViewById(1, R.id.qSentenceNo);
        qJapaneseTextView = (TextView) customPagerAdapter.findViewById(1, R.id.qJapaneseTextView);
        qPageTextView = (TextView) customPagerAdapter.findViewById(1, R.id.qPageTextView);

        aFavoriteButton = (ImageButton) customPagerAdapter.findViewById(2, R.id.aFavoriteButton);
        aSentenceNo = (TextView) customPagerAdapter.findViewById(2, R.id.aSentenceNo);
        aJapaneseTextView = (TextView) customPagerAdapter.findViewById(2, R.id.aJapaneseTextView);
        aEnglishTextView = (TextView) customPagerAdapter.findViewById(2, R.id.aEnglishTextView);
        aDetailsTextView = (TextView) customPagerAdapter.findViewById(2, R.id.aDetailsTextView);
        aPageTextView = (TextView) customPagerAdapter.findViewById(2, R.id.aPageTextView);

        onInitPageView(customPagerAdapter);

        //set visibility
        if (free_Set.equals("true")) {
            //hide favorite image button
            qFavoriteButton.setVisibility(View.INVISIBLE);
            aFavoriteButton.setVisibility(View.INVISIBLE);
        }

        //set calback
        qFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoriteButtonPressed(qFavoriteButton);
            }
        });
        aFavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoriteButtonPressed(aFavoriteButton);
            }
        });

        scrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    /*
        *Configure auto play view
        */
    private void configureAutoPlayView() {
        autoPLayButton.setVisibility(View.VISIBLE);
        if (AUTO_PLAY_ON) {
            autoPLayButton.setText(R.string.auto_play_off_state_text);
        } else {
            autoPLayButton.setText(R.string.auto_play_on_state_text);
        }
    }

    protected void getIntentData(int playMode, DatabaseHelper databaseHelper, SharedPreferences sharedPreferences) {
        uiHandler = new Handler(getMainLooper());

        //extra intent value
        IS_FAVORITE_SET = getIntent().getBooleanExtra(Default.FAVORITE_SET, false);
        FREE_SET = getIntent().getBooleanExtra(Default.FREE_SET, false);
        startPoint = getIntent().getIntExtra(Default.START_POINT, Default.ZERO);
        sentenceSetAuto = getIntent().getIntExtra(SentenceSetTable.SET_AUTO, Default.ZERO);
        sentenceList = getIntent().getIntegerArrayListExtra(Default.PLAYABLE_SENTENCE_LIST);
        contentID = getIntent().getIntExtra(ThemeContentTable.THEME_CONTENT_ID, Default.ZERO);
        themeId = getIntent().getStringExtra(Default.THEME_ID_EXTRA);
        setTitle = getIntent().getStringExtra(SentenceSetTable.SET_TITLE);
        sentenceSetId = getIntent().getIntExtra(SentenceSetTable.SET_ID, Default.ZERO);
        isShuffled = getIntent().getBooleanExtra(Default.IS_SHUFFLE_MODE, false);

        initPurchaseList(databaseHelper, sharedPreferences, contentID, themeId);

        //save study info
        CacheLastSentence.savePlaySentence(this, sentenceList, playMode, startPoint,
                isShuffled, FREE_SET, IS_FAVORITE_SET, themeId, contentID, sentenceSetAuto, sentenceSetId,
                setTitle);
    }

    protected void initPurchaseList(final DatabaseHelper databaseHelper, SharedPreferences sharedPreferences, int contentID, final String themeId) {
        if (isAutoNextSentences(sharedPreferences, contentID)) {
            ThreadManager.getInstance().execTask(new Runnable() {
                @Override
                public void run() {
                    if (purchaseList == null) {
                        purchaseList = new ArrayList<>();
                    } else {
                        purchaseList.clear();
                    }
                    purchaseList.addAll(SentenceUtils.getPurchasedSetIds(databaseHelper, themeId));
                    if (purchaseList != null && purchaseList.size() > 0) {
                        for (int i = 0, size = purchaseList.size(); i < size; i++) {
                            if (sentenceSetId == purchaseList.get(i)) {
                                purchaseIndicator = i;
                                break;
                            }
                        }
                    }
                }
            });
        }
    }

    //Resume MediaPlay when open Setting, back backPressed.
    private void resumeMediaPlay() {
        /*//update flag
        IS_ALERT_SHOWING = false;

        //resume audio
        if (mediaPlayerManager != null && AUDIO_CURRENT_POSITION != Default.ZERO) {
            mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION);
            if(!mediaPlayerManager.isPlaying()){
                onMediaCompletion();
            }
        }
        else {
            //check timer && start timer
            if (IS_TIMER_RUNNING) {
                onMediaCompletion();
                IS_TIMER_RUNNING = false;
                TIMER_REMAINING_TIME = Default.ZERO;
            }
        }*/
        soundManager.resumeAudio();

    }

    protected void setSentencesSetTitle(final DatabaseHelper databaseHelper,
                                        final int sentenceSetId) {
        ThreadManager.getInstance().execTask(new Runnable() {
            @Override
            public void run() {
                setTitle = SentenceUtils.getSentenceSetTitle(databaseHelper, sentenceSetId);
                if (setTitle != null) {
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            titleTextView.setText(setTitle);
                            titleCallBack = setTitle;
                        }
                    });
                }
            }
        });
    }

    protected boolean getNextSentences(DatabaseHelper databaseHelper, int sentenceSetIdPosition, boolean isReversed) {
        if (purchaseList != null && purchaseList.size() > 0) {
            if (sentenceSetIdPosition < 0) {
                sentenceSetIdPosition = 0;
            } else if (sentenceSetIdPosition > purchaseList.size() - 1) {
                sentenceSetIdPosition = purchaseList.size() - 1;
            }
            List<Integer> sentenceIds = SentenceUtils.getSentenceIDs(databaseHelper, purchaseList.get(sentenceSetIdPosition), isShuffled);
            if (sentenceIds != null && sentenceIds.size() > 0) {
                sentenceSetId = purchaseList.get(sentenceSetIdPosition);
                sentenceList.clear();
                sentenceList.addAll(sentenceIds);

                if (isReversed) {
                    startPoint = sentenceList.size() - 1;
                } else {
                    startPoint = 0;
                }
                setSentencesSetTitle(databaseHelper, sentenceSetId);

                return true;
            }
        }

        return false;
    }

    protected boolean isAutoNextSentences(SharedPreferences sharedPreferences, int contentID) {
        return SettingUtils.isAutoNextSentence(sharedPreferences) &&
                contentID != Default.ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID;
    }

    protected void onStartNewSentenceSession() {
    }

    protected void onLoadNewSentence(Boolean animated, int originAnimation) {

    }

    /**
     * Start work for each sentence
     *
     * @param animated        -view pager will be animated or not
     * @param originAnimation -if animated , initial view position before animate
     */
    void startNewSentenceSession(final Boolean animated, final int originAnimation) {
        ThreadManager.getInstance().execTask(new Runnable() {
            @Override
            public void run() {
                //check start point value limit
                if (startPoint < 0) {
                    if (isAutoNextSentences(sharedPreferences, contentID)) {
                        if (purchaseIndicator == 0) {
                            sentenceLimitAlert();
                            startPoint = 0;
                            return;
                        } else {
                            purchaseIndicator--;
                            if (!getNextSentences(databaseHelper, purchaseIndicator, true)) {
                                purchaseIndicator++;
                                startPoint = 0;
                                sentenceLimitAlert();
                                return;
                            }
                        }

                    } else {
                        startPoint = 0;
                        sentenceLimitAlert();
                        return;
                    }
                } else if (startPoint >= sentenceList.size()) {
                    if (FREE_SET) {
                        purchaseIndicator = purchaseIndicator - 1;
                        FREE_SET = false;
                    }
                    if (isAutoNextSentences(sharedPreferences, contentID)) {
                        purchaseIndicator++;

                        if (purchaseIndicator > purchaseList.size() - 1) {
                            startPoint = startPoint - 1;
                            sentenceLimitAlert();
                            return;
                        } else {
                            if (!getNextSentences(databaseHelper, purchaseIndicator, false)) {
                                purchaseIndicator--;
                                startPoint = sentenceList.size() - 1;
                                sentenceLimitAlert();
                                return;
                            }
                        }

                    } else {
                        startPoint = sentenceList.size() - 1;
                        //show alert
                        sentenceLimitAlert();
                        return;
                    }
                }

                Log.d(TAG, "startNewSentenceSession() sentenceId " + sentenceList.get(startPoint));

                //load all data in cursor
                cursor = databaseHelper.getQueryResultData(SentenceTable.TABLE_NAME, null, SentenceTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null, null, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    onStartNewSentenceSession();
                    //get favorite state of sentence
                    favoriteCursor = databaseHelper.getQueryResultData(FavoriteTable.TABLE_NAME, new String[]{FavoriteTable.FAVORITE_ID}, FavoriteTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null, null, null, null, null);
                    //update question tried number
                    sharedPreferences.edit().putLong(Default.QUESTION_TRIED_KEY, sharedPreferences.getLong(Default.QUESTION_TRIED_KEY, Default.STATISTICS_DEFAULT_VALUE) + 1).commit();
                    //check free set or not
                    if (!IS_FAVORITE_SET) {
                        //update last played
                        ContentValues values = new ContentValues();
                        values.put(SentenceSetTable.LAST_PLAYED_NORMAL, sentenceList.get(startPoint));
                        if (databaseHelper.updateSQL(SentenceSetTable.TABLE_NAME, values, SentenceSetTable.SET_AUTO + " = '" + sentenceSetAuto + "'", null)) {

                        }
                    }

                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            clear();
                            clearViewData();
                            onLoadNewSentence(animated, originAnimation);
                        }
                    });
                }
            }
        });

    }

    private void sentenceLimitAlert() {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                /*//check alert showing
                if (IS_ALERT_SHOWING) {
                    return;
                }
                //update flag
                IS_ALERT_SHOWING = true;

                //UPDATE FLAG
                AUDIO_CURRENT_POSITION = Default.ZERO;
                if (mediaPlayerManager != null && mediaPlayerManager.isPlaying()) {
                    AUDIO_CURRENT_POSITION = mediaPlayerManager.pauseAudio();
                }
                //stop repeat player if exist
                if (repeatPlayerManager != null) {
                    if (repeatPlayerManager.isPlaying()) {
                        repeatPlayerManager.stopAudio();
                    }
                    repeatPlayerManager = null;
                }*/
                //check timer
                if (IS_TIMER_RUNNING && autoPlayTimer != null) {
                    //stop timer
                    autoPlayTimer.cancel();
                    autoPlayTimer = null;
                }
                soundManager.releaseAudio();
                showLimitDialog();
            }
        });
    }

    /*
      *Clear all timers, media player etc
       */
    void clear() {
        //clear media player
        if(checkNullAudio){
            soundManager.releaseAudio();
            coutTimeMediaPlayer.cancel();
        }
        /*if (mediaPlayerManager != null) {
            if (mediaPlayerManager.isPlaying()) {
                mediaPlayerManager.stopAudio();
            }
            mediaPlayerManager = null;
        }
        if (repeatPlayerManager != null) {
            if (repeatPlayerManager.isPlaying()) {
                repeatPlayerManager.stopAudio();
            }
            repeatPlayerManager = null;
        }*/
        //clear timer
        if (autoPlayTimer != null) {
            autoPlayTimer.cancel();
            autoPlayTimer = null;
            IS_TIMER_RUNNING = false;
            TIMER_REMAINING_TIME = Default.ZERO;
        }
    }

    void clearViewData() {
        //set progress 0
        qProgressBar.setProgress(Default.ZERO);
        qSentenceNo.setText("");
        aSentenceNo.setText("");
        qJapaneseTextView.setText("");
        aJapaneseTextView.setText("");
        aEnglishTextView.setText("");
        aDetailsTextView.setText("");
        qPageTextView.setText("");
        aPageTextView.setText("");
        qFavoriteButton.setBackgroundResource(0);
        aFavoriteButton.setBackgroundResource(0);
    }

    /**
     * Audio file not found alert
     */
    protected void audioNotFoundAlert() {
        //show Audio file not found alert
        customIOSDialog = new CustomIOSDialog(this);
        customIOSDialog.createAlertDialog(getResources().getString(R.string.audio_not_found_title), getResources().getString(R.string.audio_not_found_message));
        customIOSDialog.setIOSDialogListener(new IOSDialogListener() {
            @Override
            public void onCancel() {
            }

            @Override
            public void onOk() {
            }
        });
    }

    private void showLimitDialog() {
        //show limit alert
        customIOSDialog = new CustomIOSDialog(PlayActivity.this);
        customIOSDialog.createAlertDialog(getResources().getString(R.string.limit_cross_title), getResources().getString(R.string.limit_cross_text));
        customIOSDialog.setIOSDialogListener(new IOSDialogListener() {
            @Override
            public void onCancel() {
            }

            @Override
            public void onOk() {
                IS_ALERT_SHOWING = false;
                //resume audio
                /*if (mediaPlayerManager != null && AUDIO_CURRENT_POSITION != Default.ZERO) {
                    mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION);
                }*/
                soundManager.releaseAudio();
                //check timer && start timer
                if (IS_TIMER_RUNNING) {
                    onMediaCompletion();
                    IS_TIMER_RUNNING = false;
                    TIMER_REMAINING_TIME = Default.ZERO;
                }
            }
        });
    }

    private void showAutoPlayDialog() {
        //show alert dialog for confirmation
        customIOSDialog = new CustomIOSDialog(this);
        customIOSDialog.createConfirmationDialog(getResources().getString((AUTO_PLAY_ON) ? R.string.turn_off_auto_play_title : R.string.turn_on_auto_play_title),
                getResources().getString((AUTO_PLAY_ON) ? R.string.turn_off_auto_play_message : R.string.turn_on_auto_play_message));
        customIOSDialog.setIOSDialogListener(new IOSDialogListener() {
            @Override
            public void onCancel() {
                /*//update flag
                IS_ALERT_SHOWING = false;

                //resume audio
                if (mediaPlayerManager != null && AUDIO_CURRENT_POSITION != Default.ZERO) {
                    mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION);
                }
                //check timer && start timer
                if (IS_TIMER_RUNNING) {
                    onMediaCompletion();
                    IS_TIMER_RUNNING = false;
                    TIMER_REMAINING_TIME = Default.ZERO;
                }*/
                CountDownTimePlayAudio(millisUntilFinishedCountTime + TIME_DELAY, TIME_COUNT);
                soundManager.resumeAudio();
            }

            @Override
            public void onOk() {
                sharedPreferences.edit().putBoolean(Default.AUTO_PLAY_ENABLE, !AUTO_PLAY_ON).commit();
                /*//update flag
                IS_ALERT_SHOWING = false;
                //resume audio
                if (mediaPlayerManager != null && AUDIO_CURRENT_POSITION != Default.ZERO) {
                    mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION);
                }
                //check timer && start timer
                if (IS_TIMER_RUNNING) {
                    onMediaCompletion();
                    IS_TIMER_RUNNING = false;
                    TIMER_REMAINING_TIME = Default.ZERO;
                }*/
                soundManager.resumeAudio();
                CountDownTimePlayAudio(millisUntilFinishedCountTime + TIME_DELAY, TIME_COUNT);
                //update flag
                AUTO_PLAY_ON = !AUTO_PLAY_ON;
                configureAutoPlayView();
                updateProgressBarVisibility();
                if(!AUTO_PLAY_ON){
                    autoPlayTimer.cancel();
                }
                /*if (AUTO_PLAY_ON) {
                    if (mediaPlayerManager == null || !mediaPlayerManager.isPlaying()) {
                        onMediaCompletion();
                    }
                } else {
                    if (autoPlayTimer != null) {
                        autoPlayTimer.cancel();
                        autoPlayTimer = null;
                    }
                }*/
            }
        });
    }

    private void showExitDialog() {
        customIOSDialog = new CustomIOSDialog(this);
        customIOSDialog.createConfirmationDialog(getResources().getString(R.string.play_exit_dialog_title), getResources().getString(R.string.play_exit_dialog_message));
        customIOSDialog.setIOSDialogListener(new IOSDialogListener() {
            @Override
            public void onCancel() {
                resumeMediaPlay();
                CountDownTimePlayAudio(millisUntilFinishedCountTime + TIME_DELAY, TIME_COUNT);
            }

            @Override
            public void onOk() {
                IS_ALERT_SHOWING = false;
                soundManager.releaseAudio();
                clear();
                sharedPreferences.edit().putInt("sentenceSetIDCallBack", sentenceSetIDCallBack).commit();
                sharedPreferences.edit().putString("titleCallBack", titleCallBack).commit();
                sharedPreferences.edit().putInt("contentIDCallBack", contentIDCallBack).commit();
                sharedPreferences.edit().putString("checkFreeSet", String.valueOf(FREE_SET)).commit();
                finish();
            }
        });
    }

    /*
    * Favorite Image Button click implementation
     */
    void favoriteButtonPressed(ImageButton imageButton) {
        favoriteCursor = databaseHelper.getQueryResultData(FavoriteTable.TABLE_NAME, new String[]{FavoriteTable.FAVORITE_ID}, FavoriteTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null, null, null, null, null);
        if (favoriteCursor.getCount() > 0) {
            //already in favorite list && delete
            if (databaseHelper.deleteSQL(FavoriteTable.TABLE_NAME, FavoriteTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null)) {
                imageButton.setBackgroundResource(R.drawable.favorite_off);
            }
        } else {
            //Not add in favorite list yet && add
            ContentValues values = new ContentValues();
            values.put(FavoriteTable.SENTENCE_ID, sentenceList.get(startPoint));

            if (theme_no.equals("Theme_1")) {
                values.put(FavoriteTable.NORMAL_PLAY_MODE, true);
                values.put(FavoriteTable.CHUNK_PLAY_MODE, true);
            } else {
                values.put(FavoriteTable.NORMAL_PLAY_MODE, true);
                values.put(FavoriteTable.CHUNK_PLAY_MODE, false);
            }

            if (databaseHelper.insertSQL(FavoriteTable.TABLE_NAME, null, values)) {
                imageButton.setBackgroundResource(R.drawable.favorite_on);
            }
        }
    }

    private void backPressed() {
/*
        //update flag
        IS_ALERT_SHOWING = true;
        //UPDATE FLAG
        AUDIO_CURRENT_POSITION = Default.ZERO;
        if (mediaPlayerManager != null && mediaPlayerManager.isPlaying()) {
            AUDIO_CURRENT_POSITION = mediaPlayerManager.pauseAudio();

        }
        //stop repeat player if exist
        if (repeatPlayerManager != null) {
            if (repeatPlayerManager.isPlaying()) {
                repeatPlayerManager.stopAudio();
            }
            repeatPlayerManager = null;
        }
        //check timer
        if (IS_TIMER_RUNNING && autoPlayTimer != null) {
            //stop timer
            autoPlayTimer.cancel();
            autoPlayTimer = null;
        }*/
        if (IS_TIMER_RUNNING && autoPlayTimer != null) {
            //stop timer
            autoPlayTimer.cancel();
            autoPlayTimer = null;
        }
        coutTimeMediaPlayer.cancel();
        soundManager.pauseAudio();
        //show alert dialog for confirmation
        showExitDialog();
    }

    /*
    Exit button click implementation
     */
    public void exitButtonPressed(View v) {
        backPressed();
    }

    /*
    Autoplay button click implementation
     */
    public void autoPlayButtonPressed(View v) {
        /*IS_ALERT_SHOWING = true;
        AUDIO_CURRENT_POSITION = Default.ZERO;
        if (mediaPlayerManager != null && mediaPlayerManager.isPlaying()) {
            AUDIO_CURRENT_POSITION = mediaPlayerManager.pauseAudio();
        }
        if (repeatPlayerManager != null) {
            if (repeatPlayerManager.isPlaying()) {
                repeatPlayerManager.stopAudio();
            }
            repeatPlayerManager = null;
        }
        //check timer
        if (IS_TIMER_RUNNING && autoPlayTimer != null) {
            //stop timer
            autoPlayTimer.cancel();
            autoPlayTimer = null;
        }*/
        if (IS_TIMER_RUNNING && autoPlayTimer != null) {
            //stop timer
            autoPlayTimer.cancel();
            autoPlayTimer = null;
        }
        soundManager.pauseAudio();
        coutTimeMediaPlayer.cancel();
        showAutoPlayDialog();
    }

    /*Show Action Sheet Button onClick*/
    public void showActionSheetPressed(View view) {
        /*//update flag
        IS_ALERT_SHOWING = true;
        //UPDATE FLAG
        AUDIO_CURRENT_POSITION = Default.ZERO;
        if (mediaPlayerManager != null && mediaPlayerManager.isPlaying()) {
            AUDIO_CURRENT_POSITION = mediaPlayerManager.pauseAudio();

        }
        //stop repeat player if exist
        if (repeatPlayerManager != null) {
            if (repeatPlayerManager.isPlaying()) {
                repeatPlayerManager.stopAudio();
            }
            repeatPlayerManager = null;
        }
        //check timer
        if (IS_TIMER_RUNNING && autoPlayTimer != null) {
            //stop timer
            autoPlayTimer.cancel();
            autoPlayTimer = null;
        }*/
        if (IS_TIMER_RUNNING && autoPlayTimer != null) {
            //stop timer
            autoPlayTimer.cancel();
            autoPlayTimer = null;
            soundManager.releaseAudio();
        }
        soundManager.pauseAudio();
        coutTimeMediaPlayer.cancel();
        showActionSheet();
    }

    void updateProgressBarVisibility() {
        if (AUTO_PLAY_ON) {
            qProgressBar.setVisibility(View.VISIBLE);
            qProgressBar.setProgress(100);
        } else {
            qProgressBar.setVisibility(View.INVISIBLE);
            qProgressBar.setProgress(100);
        }

        qProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar_blue));
    }

    /**
     * Media
     */

    /**
     * Find the sentence content id
     *
     * @param sentenceID
     * @return themeContentID
     */
    int getContentID(int sentenceID) {
        int themeContentID = 0;
        Cursor contentCursor = databaseHelper.getQueryResultData(SentenceTable.TABLE_NAME, new String[]{SentenceTable.CONTENT_ID},
                SentenceTable.SENTENCE_ID + "='" + sentenceID + "'", null, null, null, null, null);
        if (contentCursor != null && contentCursor.getCount() > 0) {
            contentCursor.moveToFirst();
            themeContentID = contentCursor.getInt(contentCursor.getColumnIndex(SentenceTable.CONTENT_ID));
        }
        return themeContentID;
    }

    /*
    Update & Restore learning time based on activity state
    @param int state- Possible value is RESUME_STATE & PAUSE_STATE
     */
    private void updateLearningTime(int state) {
        if (state == Default.RESUME_STATE) {
            sharedPreferences.edit().putLong(Default.LEARNING_SESSION_KEY, System.currentTimeMillis()).commit();

        } else if (state == Default.PAUSE_STATE) {
            SettingUtils.setStudyTime(this);
        }
    }

    /**
     * While tap into screen try to repeat audio
     */
    private void repeatAudioPlay() {
        //check media player currently active or not
        /*if (mediaPlayerManager != null) {
            if (mediaPlayerManager.isPlaying()) {
                return;
            }
        }*/
        if (repeatPlayerManager != null) {
            if (repeatPlayerManager.isPlaying()) {
                return;
            }
        }
        try {
            repeatPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY +
                    Default.RESOURCES_PREFIX + ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) :
                    cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) + "/" +
                    cursor.getString(cursor.getColumnIndex(((PLAY_STATE == Default.QUESTION_STATE) ? SentenceTable.SENTENCE_QUESTION_AUDIO : SentenceTable.SENTENCE_ANSWER_AUDIO))));

            repeatSoundManager = new SoundManager(Default.RESOURCES_BASE_DIRECTORY +
                    Default.RESOURCES_PREFIX + ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) :
                    cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) + "/" +
                    cursor.getString(cursor.getColumnIndex(((PLAY_STATE == Default.QUESTION_STATE) ? SentenceTable.SENTENCE_QUESTION_AUDIO : SentenceTable.SENTENCE_ANSWER_AUDIO))));
            repeatSoundManager.playAudio(audioSpeed);

            //repeatPlayerManager.playAudio();
        } catch (IOException e) {
            audioNotFoundAlert();
        }
    }

    protected void onLoadAnswerData(Boolean animated, int originAnimation) {
    }

    protected void onLoadNewSentence() {
    }

    protected void onMediaCompletely() {
    }

    void onAnswerAutoPlayTimer(long millisUntilFinished) {
    }

    @Override
    public void onMediaCompletion() {
        Log.d(TAG, "onMediaCompletion() state " + PLAY_STATE);
        if (PLAY_STATE == Default.QUESTION_STATE && AUTO_PLAY_ON) {
            long countDownTime;
            if (IS_TIMER_RUNNING && TIMER_REMAINING_TIME > Default.ZERO) {
                countDownTime = TIMER_REMAINING_TIME;
            } else {
                countDownTime = sharedPreferences.getInt(Default.CHUNK_PLAY_INTERVAL, Default.CHUNK_PLAY_INTERVAL_VALUES[Default.CHUNK_PLAY_INTERVAL_VALUES_DEFAULT_INDEX]) * 1000;
            }
            IS_TIMER_RUNNING = true;
            autoPlayTimer = new CountDownTimer(countDownTime, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    IS_TIMER_RUNNING = true;
                    TIMER_REMAINING_TIME = millisUntilFinished;

                    long progress = (millisUntilFinished * 100) / (sharedPreferences.getInt(Default.CHUNK_PLAY_INTERVAL, Default.CHUNK_PLAY_INTERVAL_VALUES[Default.CHUNK_PLAY_INTERVAL_VALUES_DEFAULT_INDEX]) * 1000);
                    //change color
                    if (progress >= 50) {
                        qProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar_blue));
                    } else if (progress >= 20) {
                        qProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar_yellow));
                    } else {
                        qProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar_red));
                    }
                    qProgressBar.setProgress((int) progress);
                    if (IS_ALERT_SHOWING) {
                        try {
                            autoPlayTimer.cancel();
                            autoPlayTimer = null;
                        } catch (Exception e) {
                        }
                    }
                }

                @Override
                public void onFinish() {
                    IS_TIMER_RUNNING = false;
                    TIMER_REMAINING_TIME = Default.ZERO;
                    onLoadAnswerData(true, Default.ZERO);
                }
            }.start();

        } else if (PLAY_STATE == Default.ANSWER_STATE && AUTO_PLAY_ON) {
            long countDownTime;
            if (IS_TIMER_RUNNING && TIMER_REMAINING_TIME > Default.ZERO) {
                countDownTime = TIMER_REMAINING_TIME;
            } else {
                countDownTime = sharedPreferences.getInt(Default.AUTO_PLAY_INTERVAL, Default.AUTO_PLAY_INTERVAL_VALUES[Default.AUTO_PLAY_INTERVAL_VALUES_DEFAULT_INDEX]) * 1000;
            }
            IS_TIMER_RUNNING = true;
            autoPlayTimer = new CountDownTimer(countDownTime, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    IS_TIMER_RUNNING = true;
                    TIMER_REMAINING_TIME = millisUntilFinished;
                    onAnswerAutoPlayTimer(millisUntilFinished);
                    //check alert is showing or not
                    if (IS_ALERT_SHOWING) {
                        autoPlayTimer.cancel();
                        autoPlayTimer = null;
                    }
                }

                @Override
                public void onFinish() {
                    IS_TIMER_RUNNING = false;
                    TIMER_REMAINING_TIME = Default.ZERO;
                    onLoadNewSentence();
                }
            }.start();
        }

        onMediaCompletely();
    }

    /**
     * Service
     */
    boolean mIsBound = false;
    PlayService mService;
    Intent serviceIntent = null;
    ServiceConnection Scon = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder
                binder) {
            mService = ((PlayService.ServiceBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    private void doBindService() {
        bindService(new Intent(this, NormalPlayService.class),
                Scon, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void doUnbindService() {
        if (mIsBound) {
            unbindService(Scon);
            mIsBound = false;
        }
    }

    protected Intent createServiceIntent() {
        return null;
    }

    private void startAudioService() {
        serviceIntent = createServiceIntent();
        if (serviceIntent == null)
            return;

        serviceIntent.putExtra(Default.START_POINT, startPoint);
        serviceIntent.putIntegerArrayListExtra(Default.PLAYABLE_SENTENCE_LIST, sentenceList);
        serviceIntent.putExtra("AUTO_PLAY_ON", AUTO_PLAY_ON);
        serviceIntent.putExtra("PLAY_STATE", PLAY_STATE);
        serviceIntent.putExtra(Default.IS_SHUFFLE_MODE, isShuffled);
        if (mediaPlayerManager != null && mediaPlayerManager.isPlaying()) {
            serviceIntent.putExtra("AUDIO_CURRENT_POSITION", mediaPlayerManager.pauseAudio());
        } else {
            serviceIntent.putExtra("AUDIO_CURRENT_POSITION", Default.ZERO);
        }
        if (IS_TIMER_RUNNING && autoPlayTimer != null) {
            //stop timer
            autoPlayTimer.cancel();
            autoPlayTimer = null;
        }
        serviceIntent.putExtra("TIMER_REMAINING_TIME", TIMER_REMAINING_TIME);
        serviceIntent.putExtra("IS_FAVORITE_SET", IS_FAVORITE_SET);
        serviceIntent.putExtra("sentenceSetAuto", sentenceSetAuto);
        serviceIntent.putExtra("contentID", contentID);
        serviceIntent.putExtra(SentenceSetTable.SET_ID, sentenceSetId);
        serviceIntent.putIntegerArrayListExtra("purchase_list", purchaseList);
        serviceIntent.putExtra("purchase_indicator", purchaseIndicator);
        startService(serviceIntent);
    }

    void onGetDataFromService() {
        //assign variable value from background service
        startPoint = mService.getStartPoint();
        PLAY_STATE = mService.getPlayState();
        AUDIO_CURRENT_POSITION_SERVICE = mService.getAudioCurrentPosition();
        TIMER_REMAINING_TIME_SERVICE = mService.getTimerRemainingTime();
        IS_FROM_BACKGROUND_SERVICE = true;

        Log.d(TAG, "current sentenceSetId " + sentenceSetId + "; update sentencesId " + mService.getSentenceSetId());

        if (mService.getSentenceSetId() != sentenceSetId) {
            purchaseIndicator = mService.getPurchasedSentenceIndicator();
            sentenceSetId = mService.getSentenceSetId();
            sentenceList.clear();
            sentenceList.addAll(mService.getSentencesList());
            setSentencesSetTitle(databaseHelper, sentenceSetId);
        }
    }

    void onResumeFromBackground() {

    }

    private void bindDataFromService() {
        if (IS_ALERT_SHOWING) {
            doUnbindService();
            if (serviceIntent != null) {
                stopService(serviceIntent);
                serviceIntent = null;
            }
            return;
        }
        onGetDataFromService();
        //Log.d("From Background", String.format("startPoint %d \nPLAY_STATE %d \npurchaseIndicator %d \nsentenceSetId %d",
        //SstartPoint, PLAY_STATE, purchaseIndicator, sentenceSetId));

        cursor = databaseHelper.getQueryResultData(SentenceTable.TABLE_NAME, null, SentenceTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null, null, null, null, null);
        cursor.moveToFirst();

        onResumeFromBackground();

        //stop service
        doUnbindService();
        if (serviceIntent != null) {
            stopService(serviceIntent);
            serviceIntent = null;
        }
    }

    void onSwipeToNextPage() {

    }

    void onSwipeToPreviousPage() {

    }

    @Override
    public void onDismiss(ActionSheet actionSheet, boolean isCancel) {
        if (isCancel) {
            resumeMediaPlay();
            CountDownTimePlayAudio(millisUntilFinishedCountTime + TIME_DELAY, TIME_COUNT);
        }
    }

    @Override
    public void onOtherButtonClick(ActionSheet actionSheet, int index) {

        switch (index) {
            case 0:
                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                coutTimeMediaPlayer.cancel();
                startActivity(intent);
                break;
            case 1:
                Intent i = new Intent(PlayActivity.this, HomeActivity.class);
                i.putExtra("check", true);
                sharedPreferences.edit().putString(Default.PATH_AUDIO_SPEED_SETTING, pathPlayAudio).commit();
                AUTO_PLAY_ON = false;
                checkSetting = true;
                startActivity(i);
                break;
            default:
                break;
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

    class MyGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                }

                // right to left swipe
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    onSwipeToNextPage();

                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    onSwipeToPreviousPage();
                }

            } catch (Exception ex) {
            }

            return false;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            soundManager.stopAudio();
            soundManager.releaseAudio();
            repeatAudioPlay();
            return false;
        }
    }
    public void CountDownTimePlayAudio(long millisInFuture, long countDownInterval){
        coutTimeMediaPlayer = new CountDownTimer(millisInFuture, countDownInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisUntilFinishedCountTime = millisUntilFinished;
            }

            @Override
            public void onFinish() {
                onMediaCompletion();
            }
        }.start();
    }
}