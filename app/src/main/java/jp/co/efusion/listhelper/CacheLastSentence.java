package jp.co.efusion.listhelper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.ArrayList;

import jp.co.efusion.aninstantreply.ChunkPlayActivity;
import jp.co.efusion.aninstantreply.NormalPlayActivity;
import jp.co.efusion.aninstantreply.PlayActivity;
import jp.co.efusion.database.SentenceSetTable;
import jp.co.efusion.database.ThemeContentTable;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.ObjectSerializer;

/**
 * Created by anhdt on 9/1/16.
 */
public class CacheLastSentence {
    private static final String PREFS_NAME = "cache_last_sentence.xml";
    private static final Object lockObj = new Object();

    public static void savePlaySentence(final Context context, final ArrayList<Integer> sentenceList,
                                        final int playMode, final int startPoint, final boolean isShuffled, final boolean freeSet,
                                        final boolean isFavorite, final String themeId,
                                        final int contentId, final int sentenceAuto, final int setId, final String setTitle) {

        ThreadManager.getInstance().execTask(new Runnable() {
            @Override
            public void run() {
                synchronized (lockObj) {
                    SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();

                    editor.putInt("play_mode", playMode);
                    editor.putInt("start_point", startPoint);
                    editor.putBoolean("is_shuffled", isShuffled);
                    editor.putBoolean("is_favorite", isFavorite);
                    editor.putBoolean("free_set", freeSet);
                    editor.putString("theme_id", themeId);
                    editor.putInt("content_id", contentId);
                    editor.putInt("sentence_auto", sentenceAuto);
                    editor.putInt("set_id", setId);
                    editor.putString("set_title", setTitle);
                    try {
                        editor.putString("sentence_list", ObjectSerializer.serialize(sentenceList));
                    } catch (Exception e) {
                    }

                    editor.commit();
                }
            }
        });
    }

    public static void updateStartPointSentence(final Context context, final int startPoint) {
        ThreadManager.getInstance().execTask(new Runnable() {
            @Override
            public void run() {
                synchronized (lockObj) {
                    Log.d("updateStartPointSentence", "start point " + startPoint);
                    context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putInt("start_point", startPoint).commit();
                }
            }
        });
    }

    public static void updatePlaySentence(final Context context, final ArrayList<Integer> sentenceList,
                                          final int startPoint, final int sentenceSetId, final String setTitle) {
        ThreadManager.getInstance().execTask(new Runnable() {
            @Override
            public void run() {
                synchronized (lockObj) {
                    updateSyncPlaySentence(context, sentenceList, startPoint, sentenceSetId, setTitle);
                }
            }
        });
    }

    public static void updateSyncPlaySentence(Context context, ArrayList<Integer> sentenceList,
                                              int startPoint, int sentenceSetId, String setTitle) {
        try {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putInt("start_point", startPoint);
            editor.putString("set_title", setTitle);
            editor.putInt("set_id", sentenceSetId);
            try {
                editor.putString("sentence_list", ObjectSerializer.serialize(sentenceList));
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d("updateSyncPlaySentence", "sentenceSetId " + sentenceSetId +
                    "\nsentence list " + sentenceList);

            editor.commit();
        } catch (Exception e) {
        }
    }

    /**
     * Get intent for play screen
     *
     * @param context
     * @param forcePlayMode
     * @return
     */
    public static Intent getPlayIntent(Context context, PLAY_MODE forcePlayMode) {
        synchronized (lockObj) {
            SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Intent intent = null;

            int playMode = prefs.getInt("play_mode", Default.NORMAL_PLAY_MODE);
            if (forcePlayMode == PLAY_MODE.NORMAL_MODE) {
                intent = new Intent(context, NormalPlayActivity.class);

            } else if (forcePlayMode == PLAY_MODE.CHUNK_MODE) {
                intent = new Intent(context, ChunkPlayActivity.class);

            } else {
                //check play mode
                if (playMode == Default.CHUNK_PLAY_MODE) {
                    intent = new Intent(context, ChunkPlayActivity.class);
                } else if (playMode == Default.NORMAL_PLAY_MODE) {
                    intent = new Intent(context, NormalPlayActivity.class);
                }
            }

            ArrayList<Integer> sentenceList = null;
            try {
                sentenceList = (ArrayList<Integer>) ObjectSerializer.deserialize(prefs.getString("sentence_list", null));
            } catch (Exception e) {
            }

            if (intent != null && sentenceList != null && sentenceList.size() > 0) {
                intent.putExtra(Default.START_POINT, prefs.getInt("start_point", 0));
                intent.putIntegerArrayListExtra(Default.PLAYABLE_SENTENCE_LIST, sentenceList);
                intent.putExtra(Default.IS_SHUFFLE_MODE, prefs.getBoolean("is_shuffled", false));
                intent.putExtra(Default.FAVORITE_SET, prefs.getBoolean("is_favorite", false));
                intent.putExtra(Default.FREE_SET, prefs.getBoolean("free_set", false));
                intent.putExtra(Default.THEME_ID_EXTRA, prefs.getString("theme_id", null));
                intent.putExtra(ThemeContentTable.THEME_CONTENT_ID, prefs.getInt("content_id", 0));
                intent.putExtra(SentenceSetTable.SET_AUTO, prefs.getInt("sentence_auto", 0));
                intent.putExtra(SentenceSetTable.SET_ID, prefs.getInt("set_id", 0));
                intent.putExtra(SentenceSetTable.SET_TITLE, prefs.getString("set_title", null));
            }

            return intent;
        }
    }

    public static enum PLAY_MODE {
        NO_FORCE,
        NORMAL_MODE,
        CHUNK_MODE
    }
}
