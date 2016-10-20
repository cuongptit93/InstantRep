package jp.co.efusion.aninstantreply;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.widget.ScrollView;
import android.widget.Toast;

import java.io.IOException;

import jp.co.efusion.MediaManager.MediaPlayerManager;
import jp.co.efusion.MediaManager.SoundManager;
import jp.co.efusion.database.FavoriteTable;
import jp.co.efusion.database.SentenceTable;
import jp.co.efusion.utility.CustomPagerAdapter;
import jp.co.efusion.utility.Default;


public class NormalPlayActivity extends PlayActivity  {
    private static final String TAG = NormalPlayActivity.class.getSimpleName();

    @Override
    void initPagerAdapter() {
        super.initPagerAdapter();
        customPagerAdapter = new CustomPagerAdapter(getApplicationContext(), false);
    }

    @Override
    protected void onInitPageView(CustomPagerAdapter customPagerAdapter) {
        super.onInitPageView(customPagerAdapter);
        scrollView = (ScrollView) customPagerAdapter.findViewById(2, R.id.aScrollView);
    }

    @Override
    protected void onLoadNewSentence(Boolean animated, int originAnimation) {
        super.onLoadNewSentence(animated, originAnimation);
        loadJPQuestionData(animated, originAnimation);
    }

    @Override
    protected void onLoadAnswerData(Boolean animated, int originAnimation) {
        super.onLoadAnswerData(animated, originAnimation);
        //load english answer data
        loadENAnswerData(true, Default.ZERO);
    }

    @Override
    protected void onLoadNewSentence() {
        startPoint++;
        startNewSentenceSession(true, Default.ZERO);
    }

    @Override
    protected Intent createServiceIntent() {
        return new Intent(NormalPlayActivity.this, NormalPlayService.class);
    }

    @Override
    void onResumeFromBackground() {
        super.onResumeFromBackground();
        if (PLAY_STATE == Default.QUESTION_STATE) {
            //load japanese question data
            loadJPQuestionData(false, Default.ZERO);
        } else if (PLAY_STATE == Default.ANSWER_STATE) {
            //load english answer data
            loadENAnswerData(false, Default.ZERO);
        }
    }

    @Override
    void onSwipeToNextPage() {
        super.onSwipeToNextPage();
        //Next Page
        if (currentPage == 1) {
            loadENAnswerData(true, Default.ZERO);

        } else if (currentPage == 2) {
            //start new sentence session
            startPoint++;
            startNewSentenceSession(true, Default.ZERO);
        }

    }

    @Override
    void onSwipeToPreviousPage() {
        super.onSwipeToPreviousPage();
        //left to right Previous Page
        if (currentPage == 1) {
            //start previous sentence session
            startPoint--;
            startNewSentenceSession(true, 3);

        } else if (currentPage == 2) {
            loadJPQuestionData(true, 3);
        }
    }

    /**
     * Load Japanese question data
     *
     * @param animated        -view pager will be animated or not
     * @param originAnimation -if animated , initial view position before animate
     */
    private void loadJPQuestionData(Boolean animated, int originAnimation) {
        clear();
        clearViewData();

        //update state
        PLAY_STATE = Default.QUESTION_STATE;
        //move view pager
        if (animated) {
            swipeViewPager.setCurrentItem(originAnimation, false);
        }
        swipeViewPager.setCurrentItem(Default.QUESTION_STATE, animated);

        updateProgressBarVisibility();

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
        qJapaneseTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_TEXT)));
        qPageTextView.setText((startPoint + 1) + " / " + sentenceList.size());
        //start audio
        try {
            pathPlayAudio = Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + ((IS_FAVORITE_SET) ?
                    getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_AUDIO));

            mediaPlayerManager = new MediaPlayerManager(pathPlayAudio);

            soundManager = new SoundManager(pathPlayAudio);
            soundManager.playAudio(audioSpeed);

            mediaPlayerManager.prepare();
            float seconds = (((mediaPlayerManager.getDuration() % (1000 * 60 * 60)) % (1000 * 60)) / 1000)*(1/audioSpeed)*1000;
            //mediaPlayerManager.playAudio();
            CountDownTimePlayAudio((long)seconds, TIME_COUNT);

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
     * Load English answer data
     *
     * @param animated        -view pager will be animated or not
     * @param originAnimation -if animated , initial view position before animate
     */
    private void loadENAnswerData(Boolean animated, int originAnimation) {

        //clear all
        checkNullAudio = true;
        clear();
        clearViewData();
        //update state
        PLAY_STATE = Default.ANSWER_STATE;
        //move view pager
        if (animated) {
            swipeViewPager.setCurrentItem(originAnimation, false);
        }
        swipeViewPager.setCurrentItem(Default.ANSWER_STATE, animated);

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
        //set text view data
        aSentenceNo.setText(getResources().getString(R.string.sentence_no) + " " + String.format("%03d", cursor.getInt(cursor.getColumnIndex(SentenceTable.SENTENCE_NO))));
        aJapaneseTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_TEXT)));
        aEnglishTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_ANSWER_TEXT)));
        aDetailsTextView.setText(cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_DETAILS)));
        aPageTextView.setText((startPoint + 1) + " / " + sentenceList.size());
        //start audio
        try {
            pathPlayAudio = Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + ((IS_FAVORITE_SET) ?
                    getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_ANSWER_AUDIO));

            mediaPlayerManager = new MediaPlayerManager(pathPlayAudio);

            soundManager = new SoundManager(pathPlayAudio);
            soundManager.playAudio(audioSpeed);

            mediaPlayerManager.prepare();
            float seconds = (((mediaPlayerManager.getDuration() % (1000 * 60 * 60)) % (1000 * 60)) / 1000)*(1/audioSpeed)*1000;
            //mediaPlayerManager.playAudio();
            CountDownTimePlayAudio((long)seconds, TIME_COUNT);

            //mediaPlayerManager.playAudio();
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