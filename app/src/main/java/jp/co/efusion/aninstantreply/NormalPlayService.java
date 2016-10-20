package jp.co.efusion.aninstantreply;

import android.content.SharedPreferences;

import java.io.IOException;

import jp.co.efusion.MediaManager.MediaPlayerManager;
import jp.co.efusion.MediaManager.SoundManager;
import jp.co.efusion.database.SentenceTable;
import jp.co.efusion.listhelper.Log;
import jp.co.efusion.utility.Default;

public class NormalPlayService extends PlayService {
    private static final String TAG = NormalPlayService.class.getSimpleName();

    public NormalPlayService() {

    }

    @Override
    protected void initAudio() {
        //check audio resuming or not
        if (AUDIO_CURRENT_POSITION > Default.ZERO) {
            //check state  from service & call that method
            if (PLAY_STATE == Default.QUESTION_STATE) {
                //load japanese question data
                loadJPQuestionData();
            } else if (PLAY_STATE == Default.ANSWER_STATE) {
                //load english answer data
                loadENAnswerData();
            }
        } else if (TIMER_REMAINING_TIME > Default.ZERO) {
            //check timer running or not
            onMediaCompletion();
        }
    }

    @Override
    protected void loadNewSentence() {
        //load japanese question data
        loadJPQuestionData();
    }

    @Override
    protected void onLoadAnswerData() {
        loadENAnswerData();
    }

    @Override
    protected void onLoadNewSentence() {
        startPoint++;
        startNewSentenceSession();
    }

    @Override
    protected long getChunkInterval(SharedPreferences sharedPreferences) {
        return sharedPreferences.getInt(Default.CHUNK_PLAY_INTERVAL, Default.CHUNK_PLAY_INTERVAL_VALUES[Default.CHUNK_PLAY_INTERVAL_VALUES_DEFAULT_INDEX]) * 1000;
    }

    private void loadJPQuestionData() {
        //clear all
        clear();

        //update state
        PLAY_STATE = Default.QUESTION_STATE;
        //start audio
        try {
            mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID)))
                    + "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_AUDIO)));
            //mediaPlayerManager.playAudio();

            soundManager = new SoundManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID)))
                    + "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_QUESTION_AUDIO)));
            soundManager.playAudio(audioSpeed);

            if (AUDIO_CURRENT_POSITION > Default.ZERO && AUDIO_CURRENT_POSITION < mediaPlayerManager.getDuration()) {
                mediaPlayerManager.pauseAudio();
                mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION);
                AUDIO_CURRENT_POSITION = Default.ZERO;
            }
            //set audio complete listener
            mediaPlayerManager.setMediaCompletetionListener(this);
        } catch (IOException e) {
            Log.e(TAG, "loadJPQuestionData() error " + e);
        }

    }

    private void loadENAnswerData() {
        //clear all
        clear();

        //update state
        PLAY_STATE = Default.ANSWER_STATE;

        //start audio
        try {
            mediaPlayerManager = new MediaPlayerManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_ANSWER_AUDIO)));
            //mediaPlayerManager.playAudio();

            soundManager = new SoundManager(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX +
                    ((IS_FAVORITE_SET) ? getContentID(sentenceList.get(startPoint)) : cursor.getInt(cursor.getColumnIndex(SentenceTable.CONTENT_ID))) +
                    "/" + cursor.getString(cursor.getColumnIndex(SentenceTable.SENTENCE_ANSWER_AUDIO)));
            soundManager.playAudio(audioSpeed);

            if (AUDIO_CURRENT_POSITION > Default.ZERO && AUDIO_CURRENT_POSITION < mediaPlayerManager.getDuration()) {
                mediaPlayerManager.pauseAudio();
                mediaPlayerManager.resumeAudio(AUDIO_CURRENT_POSITION);
                AUDIO_CURRENT_POSITION = Default.ZERO;
            }
            //set audio complete listener
            mediaPlayerManager.setMediaCompletetionListener(this);
        } catch (IOException e) {
        }
    }
}
