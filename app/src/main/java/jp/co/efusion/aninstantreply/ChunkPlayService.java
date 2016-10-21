package jp.co.efusion.aninstantreply;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.CountDownTimer;

import java.io.IOException;

import jp.co.efusion.MediaManager.MediaPlayerManager;
import jp.co.efusion.MediaManager.SoundManager;
import jp.co.efusion.database.ChunkTable;
import jp.co.efusion.database.SentenceTable;
import jp.co.efusion.utility.Default;

public class ChunkPlayService extends PlayService {

    Cursor chunkCursor;
    private int chunkStartPoint;

    @Override
    protected void initParams(Intent intent) {
        super.initParams(intent);
        chunkStartPoint = intent.getIntExtra("chunkStartPoint", Default.ZERO);
    }

    @Override
    protected void initCursor() {
        super.initCursor();
        //get chunk cursor of that sentence
        chunkCursor = databaseHelper.getQueryResultData(ChunkTable.TABLE_NAME, null, ChunkTable.SENTENCE_ID + " = '" + sentenceList.get(startPoint) + "'", null,
                null, null, ChunkTable.CHUNK_NO + " ASC", null);
        chunkCursor.moveToPosition(chunkStartPoint);
    }

    @Override
    protected void initAudio() {
        //check audio resuming or not
        if (AUDIO_CURRENT_POSITION > Default.ZERO) {
            //check state  from service & call that method
//          //check state & call that method
            if (PLAY_STATE == Default.QUESTION_STATE) {
                //load chunk japanese question data
                loadChunkQuestionData();
            } else if (PLAY_STATE == Default.ANSWER_STATE) {
                //load chunk english answer data
                loadChunkAnswerData();
            } else if (PLAY_STATE == Default.SENTENCE_QUESTION_STATE) {
                //load sentence answer data
                loadSentenceQuestionData();
            } else if (PLAY_STATE == Default.SENTENCE_ANSWER_STATE) {
                //load sentence answer data
                loadSentenceAnswerData();
            }

        } else if (TIMER_REMAINING_TIME > Default.ZERO) {
            //check timer running or not
            onMediaCompletion();
        }
    }

    @Override
    protected void loadNewSentence() {
        //start new chunk session
        startNewChunkSession();
    }

    @Override
    protected void onLoadAnswerData() {
        loadChunkAnswerData();

    }

    @Override
    protected void onLoadNewSentence() {
        chunkStartPoint++;
        startNewChunkSession();
    }

    @Override
    protected long getChunkInterval(SharedPreferences sharedPreferences) {
       return sharedPreferences.getInt(Default.CHUNK_PLAY_TIMER, Default.CHUNK_PLAY_TIMER_VALUES[Default.CHUNK_PLAY_TIMER_VALUES_DEFAULT_INDEX]) * 1000;
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
    protected void onPlayMediaCompletely() {
        super.onPlayMediaCompletely();

        if (PLAY_STATE == Default.SENTENCE_QUESTION_STATE && AUTO_PLAY_ON) {
            long countDownTime;
            if (TIMER_REMAINING_TIME > Default.ZERO) {
                countDownTime = TIMER_REMAINING_TIME;
            } else {
                countDownTime = sharedPreferences.getInt(Default.CHUNK_PLAY_TIMER, Default.CHUNK_PLAY_TIMER_VALUES[Default.CHUNK_PLAY_TIMER_VALUES_DEFAULT_INDEX]) * 1000;
            }
            autoPlayTimer = new CountDownTimer(countDownTime, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    TIMER_REMAINING_TIME = millisUntilFinished;
                }

                @Override
                public void onFinish() {
                    TIMER_REMAINING_TIME = Default.ZERO;
                    loadSentenceAnswerData();
                }
            }.start();

        } else if (PLAY_STATE == Default.SENTENCE_ANSWER_STATE && AUTO_PLAY_ON) {
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
                    startPoint++;
                    startNewSentenceSession();

                }
            }.start();
        }
    }

    @Override
    public int getChunkStartPoint(){
        return chunkStartPoint;
    }

    /**
     * start new chunk session
     */
    private void startNewChunkSession() {
        //check chunk limitation
        if (chunkStartPoint < 0) {
            chunkStartPoint = 0;
            //start previous sentence set
            startPoint--;
            startNewSentenceSession();

            return;
        } else if (chunkStartPoint >= chunkCursor.getCount()) {
            chunkStartPoint = chunkCursor.getCount() - 1;
            //load sentence answer data
            loadSentenceQuestionData();
            return;
        }
        chunkCursor.moveToPosition(chunkStartPoint);
        //load chunk question data
        loadChunkQuestionData();


    }

    /**
     * load chunk question data
     */
    private void loadChunkQuestionData() {
        //clear all
        clear();
        //update state
        PLAY_STATE = Default.QUESTION_STATE;

        //start audio
        try {
            mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + chunkCursor.getString(chunkCursor.getColumnIndex(ChunkTable.CHUNK_QUES_AUDIO)));

            soundManager = new SoundManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + chunkCursor.getString(chunkCursor.getColumnIndex(ChunkTable.CHUNK_QUES_AUDIO)));
            //soundManager.playAudio(audioSpeed);

            //mediaPlayerManager.playAudio();
            if (AUDIO_CURRENT_POSITION > Default.ZERO && AUDIO_CURRENT_POSITION < mediaPlayerManager.getDuration()) {
                mediaPlayerManager.pauseAudio();
                mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION);
                AUDIO_CURRENT_POSITION = Default.ZERO;
            }
            //set audio complete listener
            mediaPlayerManager.setMediaCompletetionListener(this);
        } catch (IOException e) {
            //show audio not found alert
            //audioNotFoundAlert();
        }
    }

    /**
     * load chunk answer data
     */
    private void loadChunkAnswerData() {

        //clear all
        clear();
        //update state
        PLAY_STATE = Default.ANSWER_STATE;

        //start audio
        try {
            mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + chunkCursor.getString(chunkCursor.getColumnIndex(ChunkTable.CHUNK_ANS_AUDIO)));

            soundManager = new SoundManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + chunkCursor.getString(chunkCursor.getColumnIndex(ChunkTable.CHUNK_ANS_AUDIO)));
            //soundManager.playAudio(audioSpeed);

            //mediaPlayerManager.playAudio();
            if (AUDIO_CURRENT_POSITION > Default.ZERO && AUDIO_CURRENT_POSITION < mediaPlayerManager.getDuration()) {
                mediaPlayerManager.pauseAudio();
                mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION);
                AUDIO_CURRENT_POSITION = Default.ZERO;
            }
            //set audio complete listener
            mediaPlayerManager.setMediaCompletetionListener(this);
        } catch (IOException e) {
            //show audio not found alert
            //audioNotFoundAlert();
        }

    }


    /**
     * Load Sentence question data
     */
    private void loadSentenceQuestionData() {
        //clear all
        clear();
        //update state
        PLAY_STATE = Default.SENTENCE_QUESTION_STATE;
        //start audio
        try {

            mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_AUDIO)));

            soundManager = new SoundManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_AUDIO)));
            //soundManager.playAudio(audioSpeed);

            //mediaPlayerManager.playAudio();
            if (AUDIO_CURRENT_POSITION > Default.ZERO && AUDIO_CURRENT_POSITION < mediaPlayerManager.getDuration()) {
                mediaPlayerManager.pauseAudio();
                mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION);
                AUDIO_CURRENT_POSITION = Default.ZERO;
            }
            //set audio complete listener
            mediaPlayerManager.setMediaCompletetionListener(this);
        } catch (IOException e) {
            //show audio not found alert
            //audioNotFoundAlert();
        }

    }


    /**
     * Load Sentence answer data
     */
    private void loadSentenceAnswerData() {
        //clear all
        clear();
        //update state
        PLAY_STATE = Default.SENTENCE_ANSWER_STATE;
        //start audio
        try {
            mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_ANSWER_AUDIO)));

            soundManager = new SoundManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_ANSWER_AUDIO)));
            //soundManager.playAudio(audioSpeed);

            //mediaPlayerManager.playAudio();
            if (AUDIO_CURRENT_POSITION > Default.ZERO && AUDIO_CURRENT_POSITION < mediaPlayerManager.getDuration()) {
                mediaPlayerManager.pauseAudio();
                mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION);
                AUDIO_CURRENT_POSITION = Default.ZERO;
            }
            //set audio complete listener
            mediaPlayerManager.setMediaCompletetionListener(this);
        } catch (IOException e) {
            //show audio not found alert
            //audioNotFoundAlert();
        }

    }
}
