package jp.co.efusion.aninstantreply;


import android.content.Intent;
import android.database.Cursor;
import android.os.CountDownTimer;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;

import jp.co.efusion.MediaManager.MediaPlayerManager;
import jp.co.efusion.database.ChunkTable;
import jp.co.efusion.database.FavoriteTable;
import jp.co.efusion.database.SentenceTable;
import jp.co.efusion.listhelper.Log;
import jp.co.efusion.utility.CustomPagerAdapter;
import jp.co.efusion.utility.Default;

public class ChunkPlayActivity extends PlayActivity {
    private static final String TAG = ChunkPlayActivity.class.getSimpleName();

    Cursor chunkCursor;
    volatile private int chunkStartPoint;

    private ProgressBar aProgressBar;
    TextView aFullJapaneseTextView, qFullJapaneseTextView;
    TextView sentenceNo, fullJapaneseTextView, japaneseTextView, englishTextView, detailsTextView, pageTextView;
    TextView fsentenceNo, ffullJapaneseTextView, fjapaneseTextView, fenglishTextView, fdetailsTextView, fpageTextView;
    ImageButton favoriteButton, ffavoriteButton;
    ScrollView ascrollView, fscrollView;

    @Override
    void initPagerAdapter() {
        super.initPagerAdapter();
        customPagerAdapter = new CustomPagerAdapter(getApplicationContext(), true);
    }

    @Override
    protected void onInitPageView(CustomPagerAdapter customPagerAdapter) {
        super.onInitPageView(customPagerAdapter);
        qFullJapaneseTextView = (TextView) customPagerAdapter.findViewById(1, R.id.qFullJapaneseTextView);

        ascrollView = (ScrollView) customPagerAdapter.findViewById(2, R.id.aScrollView);
        aProgressBar = (ProgressBar) customPagerAdapter.findViewById(2, R.id.aProgressBar);
        aProgressBar = (ProgressBar) customPagerAdapter.findViewById(2, R.id.aProgressBar);
        aFullJapaneseTextView = (TextView) customPagerAdapter.findViewById(2, R.id.aFullJapaneseTextView);
        ascrollView = (ScrollView) customPagerAdapter.findViewById(2, R.id.aScrollView);

        favoriteButton = (ImageButton) customPagerAdapter.findViewById(3, R.id.favoriteButton);
        sentenceNo = (TextView) customPagerAdapter.findViewById(3, R.id.sentenceNo);
        fullJapaneseTextView = (TextView) customPagerAdapter.findViewById(3, R.id.fullJapaneseTextView);
        japaneseTextView = (TextView) customPagerAdapter.findViewById(3, R.id.japaneseTextView);
        englishTextView = (TextView) customPagerAdapter.findViewById(3, R.id.englishTextView);
        scrollView = (ScrollView) customPagerAdapter.findViewById(3, R.id.fScrollView);
        detailsTextView = (TextView) customPagerAdapter.findViewById(3, R.id.detailsTextView);
        pageTextView = (TextView) customPagerAdapter.findViewById(3, R.id.pageTextView);

        ffavoriteButton = (ImageButton) customPagerAdapter.findViewById(4, R.id.favoriteButton);
        fsentenceNo = (TextView) customPagerAdapter.findViewById(4, R.id.sentenceNo);
        ffullJapaneseTextView = (TextView) customPagerAdapter.findViewById(4, R.id.fullJapaneseTextView);
        fjapaneseTextView = (TextView) customPagerAdapter.findViewById(4, R.id.japaneseTextView);
        fenglishTextView = (TextView) customPagerAdapter.findViewById(4, R.id.englishTextView);
        fdetailsTextView = (TextView) customPagerAdapter.findViewById(4, R.id.detailsTextView);
        fpageTextView = (TextView) customPagerAdapter.findViewById(4, R.id.finalPageTextView);
        fscrollView = (ScrollView) customPagerAdapter.findViewById(4, R.id.finalScrollView);


        if (free_Set.equals("true")) {
            favoriteButton.setVisibility(View.INVISIBLE);
            ffavoriteButton.setVisibility(View.INVISIBLE);
        }

        //set callback
        favoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoriteButtonPressed(favoriteButton);
            }
        });
        ffavoriteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoriteButtonPressed(ffavoriteButton);
            }
        });

        ascrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
        fscrollView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });
    }

    @Override
    protected void onStartNewSentenceSession() {
        super.onStartNewSentenceSession();
        chunkStartPoint = Default.ZERO;
        //get chunk cursor of that sentence
        chunkCursor = databaseHelper.getQueryResultData(ChunkTable.TABLE_NAME, null, ChunkTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null,
                null, null, ChunkTable.CHUNK_NO + " ASC", null);
    }

    @Override
    protected void onLoadNewSentence(Boolean animated, int originAnimation) {
        super.onLoadNewSentence(animated, originAnimation);
        Log.d(TAG, "onLoadNewSentence()");
        startNewChunkSession(animated, originAnimation);
    }

    @Override
    protected void onLoadAnswerData(Boolean animated, int originAnimation) {
        super.onLoadAnswerData(animated, originAnimation);
        loadChunkAnswerData(true, Default.ZERO);
    }

    @Override
    protected void onLoadNewSentence() {
        chunkStartPoint++;
        startNewChunkSession(true, Default.ZERO);
    }

    @Override
    void onAnswerAutoPlayTimer(long millisUntilFinished) {
        long progress = (millisUntilFinished * 100) / (sharedPreferences.getInt(Default.AUTO_PLAY_INTERVAL, Default.AUTO_PLAY_INTERVAL_VALUES[Default.AUTO_PLAY_INTERVAL_VALUES_DEFAULT_INDEX]) * 1000);
        //change color
        if (progress >= 50) {
            aProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar_blue));
        } else if (progress >= 20) {
            aProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar_yellow));
        } else {
            aProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar_red));
        }
        //Log.e("Progress",millisUntilFinished+""+progress);
        aProgressBar.setProgress((int) progress);
    }

    @Override
    protected void onMediaCompletely() {
        if (PLAY_STATE == Default.SENTENCE_QUESTION_STATE && AUTO_PLAY_ON) {
            long countDownTime;
            if (IS_TIMER_RUNNING && TIMER_REMAINING_TIME > Default.ZERO) {
                countDownTime = TIMER_REMAINING_TIME;
            } else {
                countDownTime = sharedPreferences.getInt(Default.CHUNK_PLAY_TIMER, Default.CHUNK_PLAY_TIMER_VALUES[Default.CHUNK_PLAY_TIMER_VALUES_DEFAULT_INDEX]) * 1000;
            }
            IS_TIMER_RUNNING = true;
            autoPlayTimer = new CountDownTimer(countDownTime, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    IS_TIMER_RUNNING = true;
                    TIMER_REMAINING_TIME = millisUntilFinished;

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
                    loadSentenceAnswerData(true, Default.ZERO);
                }
            }.start();

        } else if (PLAY_STATE == Default.SENTENCE_ANSWER_STATE && AUTO_PLAY_ON) {
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
                    if (IS_ALERT_SHOWING) {
                        autoPlayTimer.cancel();
                        autoPlayTimer = null;
                    }
                }

                @Override
                public void onFinish() {
                    IS_TIMER_RUNNING = false;
                    TIMER_REMAINING_TIME = Default.ZERO;
                    startPoint++;
                    startNewSentenceSession(true, Default.ZERO);
                }
            }.start();
        }
    }

    @Override
    void updateProgressBarVisibility() {
        super.updateProgressBarVisibility();
        if (AUTO_PLAY_ON) {
            aProgressBar.setVisibility(View.INVISIBLE);
            aProgressBar.setProgress(100);
        } else {
            aProgressBar.setVisibility(View.INVISIBLE);
            aProgressBar.setProgress(100);
        }
        aProgressBar.setProgressDrawable(getResources().getDrawable(R.drawable.progress_bar_blue));
    }

    @Override
    void clearViewData() {
        //set progress 0
        qProgressBar.setProgress(Default.ZERO);
        aProgressBar.setProgress(Default.ZERO);
        //clear all textview
        fsentenceNo.setText("");
        qSentenceNo.setText("");
        aSentenceNo.setText("");
        sentenceNo.setText("");

        ffullJapaneseTextView.setText("");
        qFullJapaneseTextView.setText("");
        aFullJapaneseTextView.setText("");
        fullJapaneseTextView.setText("");

        fjapaneseTextView.setText("");
        qJapaneseTextView.setText("");
        aJapaneseTextView.setText("");
        japaneseTextView.setText("");

        fenglishTextView.setText("");
        aEnglishTextView.setText("");
        englishTextView.setText("");
        detailsTextView.setText("");

        fpageTextView.setText("");
        qPageTextView.setText("");
        aPageTextView.setText("");
        pageTextView.setText("");
        //set image button null
        ffavoriteButton.setBackgroundResource(0);
        qFavoriteButton.setBackgroundResource(0);
        aFavoriteButton.setBackgroundResource(0);
        favoriteButton.setBackgroundResource(0);
    }

    @Override
    protected Intent createServiceIntent() {
        return new Intent(ChunkPlayActivity.this, ChunkPlayService.class);
    }

    @Override
    void onGetDataFromService() {
        super.onGetDataFromService();
        chunkStartPoint = mService.getChunkStartPoint();
    }

    @Override
    void onResumeFromBackground() {
        super.onResumeFromBackground();

        //get chunk cursor of that sentence
        chunkCursor = databaseHelper.getQueryResultData(ChunkTable.TABLE_NAME, null, ChunkTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null,
                null, null, ChunkTable.CHUNK_NO + " ASC", null);
        chunkCursor.moveToPosition(chunkStartPoint);

        //check state & call that method
        if (PLAY_STATE == Default.QUESTION_STATE) {
            //load chunk japanese question data
            loadChunkQuestionData(false, Default.ZERO);
        } else if (PLAY_STATE == Default.ANSWER_STATE) {
            //load chunk english answer data
            loadChunkAnswerData(false, Default.ZERO);
        } else if (PLAY_STATE == Default.SENTENCE_QUESTION_STATE) {
            //load sentence answer data
            loadSentenceQuestionData(false, Default.ZERO);
        } else if (PLAY_STATE == Default.SENTENCE_ANSWER_STATE) {
            //load sentence answer data
            loadSentenceAnswerData(false, Default.ZERO);
        }
    }

    @Override
    void onSwipeToNextPage() {
        super.onSwipeToNextPage();
        if (currentPage == 1) {
            //previousPage = currentPage;
            //load chunk answer data
            loadChunkAnswerData(true, Default.ZERO);

        } else if (currentPage == 2) {

            //previousPage = currentPage;
            //load new chunk session
            chunkStartPoint++;
            startNewChunkSession(true, Default.ZERO);

        } else if (currentPage == 3) {
            //start new sentence session
            //swipeViewPager.setCurrentItem(Default.SENTENCE_ANSWER_STATE, false);
            loadSentenceAnswerData(true, Default.SENTENCE_QUESTION_STATE);
        } else if (currentPage == 4) {
            //start new sentence session
            //swipeViewPager.setCurrentItem(Default.SENTENCE_ANSWER_STATE, false);
            startPoint++;
            startNewSentenceSession(true, Default.ZERO);
        }
    }

    @Override
    void onSwipeToPreviousPage() {
        super.onSwipeToPreviousPage();
        if (currentPage == 1) {
            //start previous chunk session
            //swipeViewPager.setCurrentItem(Default.QUESTION_STATE, false);
            chunkStartPoint--;
            startNewChunkSession(true, 5);

        } else if (currentPage == 2) {
            //previousPage = currentPage;
            //load chunk question data
            loadChunkQuestionData(true, 5);

        } else if (currentPage == 3) {
            // previousPage = currentPage;
            //load new chunk session
            //chunkStartPoint++;
            startNewChunkSession(true, 5);
        } else if (currentPage == 4) {
            loadSentenceQuestionData(true, 5);
        }
    }

    /**
     * start new chunk session
     *
     * @param animated        -view pager will be animated or not
     * @param originAnimation -if animated , initial view position before animate
     */
    private void startNewChunkSession(Boolean animated, int originAnimation) {
        Log.d(TAG, "startNewChunkSession() chunkStartPoint " + chunkStartPoint +
                "; chunk size " + chunkCursor.getCount());

        //check chunk limitation
        if (chunkStartPoint < 0) {
            chunkStartPoint = 0;
            //start previous sentence set
            startPoint--;
            startNewSentenceSession(animated, originAnimation);
            return;
        } else if (chunkStartPoint >= chunkCursor.getCount()) {
            chunkStartPoint = chunkCursor.getCount() - 1;
            //load sentence answer data
            loadSentenceQuestionData(animated, originAnimation);
            return;
        }
        chunkCursor.moveToPosition(chunkStartPoint);
        //load chunk question data
        loadChunkQuestionData(animated, originAnimation);
    }


    /**
     * load chunk question data
     *
     * @param animated        -view pager will be animated or not
     * @param originAnimation -if animated , initial view position before animate
     */
    private void loadChunkQuestionData(Boolean animated, int originAnimation) {
        Log.d(TAG, "loadChunkQuestionData()");
        //clear all
        clear();
        clearViewData();

        //update state
        PLAY_STATE = Default.QUESTION_STATE;
        //move view pager
        //move view pager
        if (animated) {
            swipeViewPager.setCurrentItem(originAnimation, false);
        }
        swipeViewPager.setCurrentItem(Default.QUESTION_STATE, animated);

        //update progress bar visibility
        //Check auto play on or off
        updateProgressBarVisibility();

        //load favorite
        //get favorite state of sentence
        favoriteCursor = databaseHelper.getQueryResultData(FavoriteTable.TABLE_NAME, new String[]{FavoriteTable.FAVORITE_ID}, FavoriteTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null, null, null, null, null);
        if (favoriteCursor.getCount() > 0) {
            //already in favorite list
            qFavoriteButton.setBackgroundResource(R.drawable.favorite_on);
        } else {
            //Not add in favorite list yet
            qFavoriteButton.setBackgroundResource(R.drawable.favorite_off);
        }
        //set text view data
        qSentenceNo.setText(getResources().getString(R.string.sentence_no) + " " + String.format("%03d", cursor.getInt(cursor.getColumnIndex(SentenceTable.SENTENCE_NO))));

        qFullJapaneseTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_TEXT)));
        qJapaneseTextView.setText(chunkCursor.getString(chunkCursor.getColumnIndex(ChunkTable.CHUNK_QUES_TEXT)));
        qPageTextView.setText((startPoint + 1) + " / " + sentenceList.size());

        //start audio
        try {
            mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) :
                            cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) + "/" + chunkCursor.getString(chunkCursor.getColumnIndex(ChunkTable.CHUNK_QUES_AUDIO)));
            mediaPlayerManager.playAudio();
            //check came from background then pause audio
            if (IS_FROM_BACKGROUND_SERVICE) {
                mediaPlayerManager.pauseAudio();
                IS_FROM_BACKGROUND_SERVICE = false;
                //check for auto play on & audio current position && Timer remaining time 0
                if (AUTO_PLAY_ON && AUDIO_CURRENT_POSITION_SERVICE == Default.ZERO && TIMER_REMAINING_TIME_SERVICE == Default.ZERO) {
                    if (mediaPlayerManager.getDuration() != Default.ZERO) {
                        AUDIO_CURRENT_POSITION_SERVICE = mediaPlayerManager.getDuration() - 100;
                    }
                }
            }
            //check audio resuming or not
            if (AUDIO_CURRENT_POSITION_SERVICE > Default.ZERO) {
                mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION_SERVICE);
                AUDIO_CURRENT_POSITION_SERVICE = Default.ZERO;

            } else if (TIMER_REMAINING_TIME_SERVICE > Default.ZERO) {
                if (mediaPlayerManager.isPlaying()) {
                    mediaPlayerManager.stopAudio();
                }
                //update flag
                IS_TIMER_RUNNING = true;
                TIMER_REMAINING_TIME = TIMER_REMAINING_TIME_SERVICE;
                TIMER_REMAINING_TIME_SERVICE = Default.ZERO;
                onMediaCompletion();
            }
            //set audio complete listener
            mediaPlayerManager.setMediaCompletetionListener(this);
        } catch (IOException e) {
            //show audio not found alert
            audioNotFoundAlert();
        }
    }

    /**
     * load chunk answer data
     *
     * @param animated        -view pager will be animated or not
     * @param originAnimation -if animated , initial view position before animate
     */
    private void loadChunkAnswerData(Boolean animated, int originAnimation) {
        //clear all
        clear();
        clearViewData();
        //update state
        PLAY_STATE = Default.ANSWER_STATE;
        //move view pager
        //move view pager
        if (animated) {
            swipeViewPager.setCurrentItem(originAnimation, false);
        }
        swipeViewPager.setCurrentItem(Default.ANSWER_STATE, animated);

        //update progress bar visibility
        //Check auto play on or off
        updateProgressBarVisibility();

        //load favorite
        //get favorite state of sentence
        favoriteCursor = databaseHelper.getQueryResultData(FavoriteTable.TABLE_NAME, new String[]{FavoriteTable.FAVORITE_ID}, FavoriteTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null, null, null, null, null);
        if (favoriteCursor.getCount() > 0) {
            //already in favorite list
            aFavoriteButton.setBackgroundResource(R.drawable.favorite_on);
        } else {
            //Not add in favorite list yet
            aFavoriteButton.setBackgroundResource(R.drawable.favorite_off);
        }

        aSentenceNo.setText(getResources().getString(R.string.sentence_no) + " " + String.format("%03d", cursor.getInt(cursor.getColumnIndex(SentenceTable.SENTENCE_NO))));

        aFullJapaneseTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_TEXT)));
        aJapaneseTextView.setText(chunkCursor.getString(chunkCursor.getColumnIndex(ChunkTable.CHUNK_QUES_TEXT)));
        aEnglishTextView.setText(chunkCursor.getString(chunkCursor.getColumnIndex(ChunkTable.CHUNK_ANS_TEXT)));
        aPageTextView.setText((startPoint + 1) + " / " + sentenceList.size());
        //start audio
        try {
            mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) :
                            cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) + "/" + chunkCursor.getString(chunkCursor.getColumnIndex(ChunkTable.CHUNK_ANS_AUDIO)));
            mediaPlayerManager.playAudio();
            //check came from background then pause audio
            if (IS_FROM_BACKGROUND_SERVICE) {
                mediaPlayerManager.pauseAudio();
                IS_FROM_BACKGROUND_SERVICE = false;
                //check for auto play on & audio current position && Timer remaining time 0
                if (AUTO_PLAY_ON && AUDIO_CURRENT_POSITION_SERVICE == Default.ZERO && TIMER_REMAINING_TIME_SERVICE == Default.ZERO) {
                    if (mediaPlayerManager.getDuration() != Default.ZERO) {
                        AUDIO_CURRENT_POSITION_SERVICE = mediaPlayerManager.getDuration() - 100;
                    }
                }
            }
            //check audio resuming or not
            if (AUDIO_CURRENT_POSITION_SERVICE > Default.ZERO) {
                mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION_SERVICE);
                AUDIO_CURRENT_POSITION_SERVICE = Default.ZERO;

            } else if (TIMER_REMAINING_TIME_SERVICE > Default.ZERO) {
                if (mediaPlayerManager.isPlaying()) {
                    mediaPlayerManager.stopAudio();
                }
                //update flag
                IS_TIMER_RUNNING = true;
                TIMER_REMAINING_TIME = TIMER_REMAINING_TIME_SERVICE;
                TIMER_REMAINING_TIME_SERVICE = Default.ZERO;
                onMediaCompletion();
            }
            //set audio complete listener
            mediaPlayerManager.setMediaCompletetionListener(this);
        } catch (IOException e) {
            //show audio not found alert
            audioNotFoundAlert();
        }

    }

    /**
     * Load Sentence question data
     *
     * @param animated        -view pager will be animated or not
     * @param originAnimation -if animated , initial view position before animate
     */
    private void loadSentenceQuestionData(Boolean animated, int originAnimation) {
        //28-12-2015
        //clear all
        clear();
        clearViewData();
        //update state
        PLAY_STATE = Default.SENTENCE_QUESTION_STATE;
        //move view pager
        //move view pager
        if (animated) {
            swipeViewPager.setCurrentItem(originAnimation, false);
        }
        swipeViewPager.setCurrentItem(Default.SENTENCE_QUESTION_STATE, animated);

        //load favorite
        //get favorite state of sentence
        favoriteCursor = databaseHelper.getQueryResultData(FavoriteTable.TABLE_NAME, new String[]{FavoriteTable.FAVORITE_ID}, FavoriteTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null, null, null, null, null);
        if (favoriteCursor.getCount() > 0) {
            //already in favorite list
            favoriteButton.setBackgroundResource(R.drawable.favorite_on);
        } else {
            //Not add in favorite list yet
            favoriteButton.setBackgroundResource(R.drawable.favorite_off);
        }

        //set text view data
        sentenceNo.setText(getResources().getString(R.string.sentence_no) + " " + String.format("%03d", cursor.getInt(cursor.getColumnIndex(SentenceTable.SENTENCE_NO))));
        fullJapaneseTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_TEXT)));
        japaneseTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_TEXT)));
        // englishTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_ANSWER_TEXT)));
        // detailsTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_DETAILS)));
        pageTextView.setText((startPoint + 1) + " / " + sentenceList.size());
        //start audio
        try {

            mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) :
                            cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) + "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_AUDIO)));
            mediaPlayerManager.playAudio();

            //check came from background then pause audio
            if (IS_FROM_BACKGROUND_SERVICE) {
                mediaPlayerManager.pauseAudio();
                IS_FROM_BACKGROUND_SERVICE = false;
                //check for auto play on & audio current position && Timer remaining time 0
                if (AUTO_PLAY_ON && AUDIO_CURRENT_POSITION_SERVICE == Default.ZERO && TIMER_REMAINING_TIME_SERVICE == Default.ZERO) {
                    if (mediaPlayerManager.getDuration() != Default.ZERO) {
                        AUDIO_CURRENT_POSITION_SERVICE = mediaPlayerManager.getDuration() - 100;
                    }
                }
            }
            //check audio resuming or not
            if (AUDIO_CURRENT_POSITION_SERVICE > Default.ZERO) {
                mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION_SERVICE);
                AUDIO_CURRENT_POSITION_SERVICE = Default.ZERO;

            } else if (TIMER_REMAINING_TIME_SERVICE > Default.ZERO) {
                if (mediaPlayerManager.isPlaying()) {
                    mediaPlayerManager.stopAudio();
                }
                //update flag
                IS_TIMER_RUNNING = true;
                TIMER_REMAINING_TIME = TIMER_REMAINING_TIME_SERVICE;
                TIMER_REMAINING_TIME_SERVICE = Default.ZERO;
                onMediaCompletion();
            }
            //set audio complete listener
            mediaPlayerManager.setMediaCompletetionListener(this);

        } catch (IOException e) {
            //show audio not found alert
            audioNotFoundAlert();
        }

    }


    /**
     * Load Sentence answer data
     *
     * @param animated        -view pager will be animated or not
     * @param originAnimation -if animated , initial view position before animate
     */
    private void loadSentenceAnswerData(Boolean animated, int originAnimation) {
        //28-12-2015
        //clear all
        clear();
        clearViewData();
        //update state
        PLAY_STATE = Default.SENTENCE_ANSWER_STATE;
        //move view pager
        //move view pager
        if (animated) {
            swipeViewPager.setCurrentItem(originAnimation, false);
        }
        swipeViewPager.setCurrentItem(Default.SENTENCE_ANSWER_STATE, animated);

        //load favorite
        //get favorite state of sentence
        favoriteCursor = databaseHelper.getQueryResultData(FavoriteTable.TABLE_NAME, new String[]{FavoriteTable.FAVORITE_ID}, FavoriteTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null, null, null, null, null);
        if (favoriteCursor.getCount() > 0) {
            //already in favorite list
            ffavoriteButton.setBackgroundResource(R.drawable.favorite_on);
        } else {
            //Not add in favorite list yet
            ffavoriteButton.setBackgroundResource(R.drawable.favorite_off);
        }

        //set text view data
        fsentenceNo.setText(getResources().getString(R.string.sentence_no) + " " + String.format("%03d", cursor.getInt(cursor.getColumnIndex(SentenceTable.SENTENCE_NO))));
        ffullJapaneseTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_TEXT)));
        fjapaneseTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_TEXT)));
        fenglishTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_ANSWER_TEXT)));
        fdetailsTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_DETAILS)));
        fpageTextView.setText((startPoint + 1) + " / " + sentenceList.size());
        //start audio
        try {

            //mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + ((IS_FAVORITE_SET)?getContentID(sentenceList.get(startPoint)):contentID) + "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_ANSWER_AUDIO)));

//            if(!questionPlayed) {
//                mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : contentID) + "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_AUDIO)));
//
//            }else {
            mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + ((IS_FAVORITE_SET) ?
                    getContentID(sentenceList.get(startPoint)) :
                    cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) + "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_ANSWER_AUDIO)));
            //}
            mediaPlayerManager.playAudio();
            //check came from background then pause audio


            if (IS_FROM_BACKGROUND_SERVICE) {
                mediaPlayerManager.pauseAudio();
                IS_FROM_BACKGROUND_SERVICE = false;
                //check for auto play on & audio current position && Timer remaining time 0
                if (AUTO_PLAY_ON && AUDIO_CURRENT_POSITION_SERVICE == Default.ZERO && TIMER_REMAINING_TIME_SERVICE == Default.ZERO) {
                    if (mediaPlayerManager.getDuration() != Default.ZERO) {
                        AUDIO_CURRENT_POSITION_SERVICE = mediaPlayerManager.getDuration() - 100;
                    }
                }
            }
            //check audio resuming or not
            if (AUDIO_CURRENT_POSITION_SERVICE > Default.ZERO) {
                mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION_SERVICE);
                AUDIO_CURRENT_POSITION_SERVICE = Default.ZERO;

            } else if (TIMER_REMAINING_TIME_SERVICE > Default.ZERO) {
                if (mediaPlayerManager.isPlaying()) {
                    mediaPlayerManager.stopAudio();
                }
                //update flag
                IS_TIMER_RUNNING = true;
                TIMER_REMAINING_TIME = TIMER_REMAINING_TIME_SERVICE;
                TIMER_REMAINING_TIME_SERVICE = Default.ZERO;
                onMediaCompletion();
            }
            //set audio complete listener
            mediaPlayerManager.setMediaCompletetionListener(this);

        } catch (IOException e) {
            //show audio not found alert
            audioNotFoundAlert();
        }

    }
}
