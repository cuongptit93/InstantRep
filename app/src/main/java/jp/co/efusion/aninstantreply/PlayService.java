package jp.co.efusion.aninstantreply;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

import jp.co.efusion.MediaManager.MediaCompletionListener;
import jp.co.efusion.MediaManager.MediaPlayerManager;
import jp.co.efusion.MediaManager.SoundManager;
import jp.co.efusion.database.DatabaseHelper;
import jp.co.efusion.database.SentenceSetTable;
import jp.co.efusion.database.SentenceTable;
import jp.co.efusion.listhelper.CacheLastSentence;
import jp.co.efusion.listhelper.Log;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.SentenceUtils;
import jp.co.efusion.utility.SettingUtils;

/**
 * Created by anhdt on 9/12/16.
 */
public abstract class PlayService extends Service implements MediaCompletionListener {
    private static final String TAG = PlayService.class.getSimpleName();

    MediaPlayerManager mediaPlayerManager = null;

    SoundManager soundManager = null;
    float audioSpeed;

    final IBinder mBinder = new ServiceBinder();

    SharedPreferences sharedPreferences;

    CountDownTimer autoPlayTimer;

    DatabaseHelper databaseHelper;
    Cursor cursor;

    ArrayList<Integer> sentenceList;
    int sentenceSetId, startPoint, contentID, sentenceSetAuto;

    //list contentId and sentenceSetId for all purchase content
    volatile List<Integer> purchaseList;
    volatile int purchaseIndicator;

    //flag for autoplay mode
    Boolean AUTO_PLAY_ON = false;
    //flag for play state
    int PLAY_STATE;

    //flag for tracking audio player pause mode
    int AUDIO_CURRENT_POSITION = Default.ZERO;

    Boolean IS_FAVORITE_SET = false;

    long TIMER_REMAINING_TIME = 0;

    boolean isShuffled;

    protected abstract void initAudio();

    protected abstract void loadNewSentence();

    protected abstract void onLoadAnswerData();

    protected abstract void onLoadNewSentence();

    protected abstract long getChunkInterval(SharedPreferences sharedPreferences);

    @Override
    public void onCreate() {
        super.onCreate();
        //initialize database helper
        databaseHelper = new DatabaseHelper(this);
        databaseHelper.openDatabase();

        //initialize sharepreference
        sharedPreferences = getSharedPreferences(Default.SHARE_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        //get and send value audioSpeed to Activity.
        audioSpeed = (float) sharedPreferences.getInt(Default.SPEED_SETTING, Default.DEFAULT_SPEED_SETTING)/10;
        Log.d(TAG, "onStartCommand");
        try {
            initParams(intent);
            initCursor();
            initAudio();
        } catch (Exception e) {
        }
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mediaPlayerManager != null)
            mediaPlayerManager.stopAudio();
        clear();
        super.onDestroy();
    }

    /**
     * Init intent params
     */
    protected void initParams(Intent intent) {
        startPoint = intent.getIntExtra(Default.START_POINT, Default.ZERO);
        sentenceList = intent.getIntegerArrayListExtra(Default.PLAYABLE_SENTENCE_LIST);
        AUTO_PLAY_ON = intent.getBooleanExtra("AUTO_PLAY_ON", false);
        PLAY_STATE = intent.getIntExtra("PLAY_STATE", Default.ZERO);
        AUDIO_CURRENT_POSITION = intent.getIntExtra("AUDIO_CURRENT_POSITION", Default.ZERO);
        TIMER_REMAINING_TIME = intent.getLongExtra("TIMER_REMAINING_TIME", Default.ZERO);
        IS_FAVORITE_SET = intent.getBooleanExtra("IS_FAVORITE_SET", false);
        sentenceSetAuto = intent.getIntExtra("sentenceSetAuto", Default.ZERO);
        contentID = intent.getIntExtra("contentID", Default.ZERO);
        sentenceSetId = intent.getIntExtra(SentenceSetTable.SET_ID, Default.ZERO);
        purchaseList = intent.getIntegerArrayListExtra("purchase_list");
        purchaseIndicator = intent.getIntExtra("purchase_indicator", 0);
        isShuffled = intent.getBooleanExtra(Default.IS_SHUFFLE_MODE, false);
    }

    protected void initCursor() {
        //update cursor
        cursor = databaseHelper.getQueryResultData(SentenceTable.TABLE_NAME, null,
                SentenceTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null, null, null, null, null);
        cursor.moveToFirst();
    }

    private boolean isAutoNextSentences(SharedPreferences sharedPreferences, int contentID) {
        return SettingUtils.isAutoNextSentence(sharedPreferences) && contentID != Default.ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID;
    }

    private boolean getNextSentences(DatabaseHelper databaseHelper, int sentenceSetIdPosition, boolean isReversed) {
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
                return true;
            }
        }
        return false;
    }

    protected void onStartNewSentenceSession() {

    }

    protected void startNewSentenceSession() {
        try {
            //check start point value limit
            if (startPoint < 0) {
                if (isAutoNextSentences(sharedPreferences, contentID)) {
                    purchaseIndicator--;
                    Log.d(TAG, "getNextSentences " + getNextSentences(databaseHelper, purchaseIndicator, true));
                    if (!getNextSentences(databaseHelper, purchaseIndicator, true)) {
                        purchaseIndicator++;
                        startPoint = 0;
                        return;
                    } else {
                        CacheLastSentence.updatePlaySentence(PlayService.this, sentenceList, startPoint, sentenceSetId, null);
                    }
                } else {
                    startPoint = 0;
                    return;
                }

            } else if (startPoint >= sentenceList.size()) {
                if (isAutoNextSentences(sharedPreferences, contentID)) {
                    purchaseIndicator++;
                    Log.d(TAG, "getNextSentences " + getNextSentences(databaseHelper, purchaseIndicator, false));
                    if (!getNextSentences(databaseHelper, purchaseIndicator, false)) {
                        purchaseIndicator--;
                        startPoint = sentenceList.size() - 1;
                        return;
                    } else {
                        CacheLastSentence.updatePlaySentence(PlayService.this, sentenceList, startPoint, sentenceSetId, null);
                    }
                } else {
                    startPoint = sentenceList.size() - 1;
                    return;
                }

            }
            CacheLastSentence.updateStartPointSentence(this, startPoint);
            clear();
            Log.d(TAG, "startNewSentenceSession() sentenceId " + sentenceList.get(startPoint));
            //load all data in cursor
            cursor = databaseHelper.getQueryResultData(SentenceTable.TABLE_NAME, null, SentenceTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToFirst();

                onStartNewSentenceSession();

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
                loadNewSentence();
            }
        } catch (Exception e) {
            Log.e(TAG, "startNewSentenceSession() error " + e);
        }
    }

    /**
     * Find the sentence content id
     *
     * @param sentenceID
     * @return themeContentID
     */
    protected int getContentID(int sentenceID) {
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
     *Clear all timers, media player etc
    */
    protected void clear() {
        //clear media player
        if (mediaPlayerManager != null) {
            if (mediaPlayerManager.isPlaying()) {
                mediaPlayerManager.stopAudio();
            }
            mediaPlayerManager = null;
        }
        //clear timer
        if (autoPlayTimer != null) {
            autoPlayTimer.cancel();
            autoPlayTimer = null;
        }
    }

    protected void updateLearningTime(int state) {
        if (state == Default.RESUME_STATE) {
            //start each tme session
            sharedPreferences.edit().putLong(Default.LEARNING_SESSION_KEY, System.currentTimeMillis()).commit();

        } else if (state == Default.PAUSE_STATE) {
            SettingUtils.setStudyTime(this);
        }
    }

    protected void onPlayMediaCompletely() {

    }

    public int getAudioCurrentPosition() {
        if (mediaPlayerManager != null) {
            return mediaPlayerManager.getCurrentPosition();
        }
        return Default.ZERO;
    }

    public long getTimerRemainingTime() {
        return TIMER_REMAINING_TIME;
    }

    public int getStartPoint() {
        return startPoint;
    }

    public int getChunkStartPoint() {
        return 0;
    }

    public int getPlayState() {
        return PLAY_STATE;
    }

    public int getSentenceSetId() {
        return sentenceSetId;
    }

    public int getPurchasedSentenceIndicator() {
        return purchaseIndicator;
    }

    public ArrayList<Integer> getSentencesList() {
        return sentenceList;
    }

    @Override
    public void onMediaCompletion() {
        updateLearningTime(Default.PAUSE_STATE);

        if (PLAY_STATE == Default.QUESTION_STATE && AUTO_PLAY_ON) {
            long countDownTime;
            if (TIMER_REMAINING_TIME > Default.ZERO) {
                countDownTime = TIMER_REMAINING_TIME;
            } else {
                countDownTime = getChunkInterval(sharedPreferences);
            }
            autoPlayTimer = new CountDownTimer(countDownTime, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    TIMER_REMAINING_TIME = millisUntilFinished;
                }

                @Override
                public void onFinish() {
                    TIMER_REMAINING_TIME = Default.ZERO;
                    onLoadAnswerData();
                }
            }.start();

        } else if (PLAY_STATE == Default.ANSWER_STATE && AUTO_PLAY_ON) {
            long countDownTime;
            if (TIMER_REMAINING_TIME > Default.ZERO) {
                countDownTime = TIMER_REMAINING_TIME;
            } else {
                countDownTime = sharedPreferences.getInt(Default.AUTO_PLAY_INTERVAL, Default.AUTO_PLAY_INTERVAL_VALUES[Default.AUTO_PLAY_INTERVAL_VALUES_DEFAULT_INDEX]) * 1000;
            }
            autoPlayTimer = new CountDownTimer(countDownTime, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    TIMER_REMAINING_TIME = millisUntilFinished;
                }

                @Override
                public void onFinish() {
                    TIMER_REMAINING_TIME = Default.ZERO;
                    onLoadNewSentence();

                }
            }.start();
        }

        onPlayMediaCompletely();

        updateLearningTime(Default.RESUME_STATE);
    }

    public class ServiceBinder extends Binder {
        PlayService getService() {
            return PlayService.this;
        }
    }
}