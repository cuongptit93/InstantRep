package jp.co.efusion.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import jp.co.efusion.listhelper.DialogHelper;
import jp.co.efusion.listhelper.ThreadManager;

/**
 * Created by anhdt on 8/31/16.
 */
public class SettingUtils {

    private SettingUtils() {
    }

    /**
     * Check "go to next set sentences" is enable or not
     *
     * @param sharedPreferences
     * @return true - enable, false - disable
     */
    public static final boolean isAutoNextSentence(SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(Default.AUTO_NEXT_SENTENCE,
                Default.AUTO_NEXT_SENTENCE_ENABLE_DEFAULT);
    }

    /**
     * @param context
     * @param enable
     */
    public static void enableRatingPopup(Context context, boolean enable) {
        PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).
                edit().putBoolean(Default.ENABLE_RATING_POPUP, enable).commit();
    }

    public static boolean isEnableRatingPopup(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).
                getBoolean(Default.ENABLE_RATING_POPUP, true);
    }

    /**
     * Get studing time in mini second
     *
     * @param context
     * @return time in mini second
     */
    public static long getStudyTime(Context context) {
        return getStudyTime(context.getApplicationContext().getSharedPreferences(Default.SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE));
    }

    private static long getStudyTime(SharedPreferences sharedPreferences) {
        return sharedPreferences.getLong(Default.LEARNING_HOUR_KEY, Default.STATISTICS_DEFAULT_VALUE);
    }

    public static final long MINIMUM_RATTING_TIME_MS = 3 * 60 * 60 * 1000; //3 hours

    /**
     * Set studing time
     */
    public static void setStudyTime(final Context context) {

        ThreadManager.getInstance().execTask(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences sharedPreferences = context.getApplicationContext().
                            getSharedPreferences(Default.SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);

                    long learningTime = System.currentTimeMillis() -
                            sharedPreferences.getLong(Default.LEARNING_SESSION_KEY, System.currentTimeMillis());
                    learningTime = getStudyTime(context) + learningTime;

                    sharedPreferences.edit().putLong(Default.LEARNING_HOUR_KEY, learningTime).commit();
                } catch (Exception e) {
                }
            }
        });
    }

    /**
     * Show rating dialog
     *
     * @param context
     */
    public static void showRattingDialog(final Context context) {
        SharedPreferences sharedPreferences = context.getApplicationContext().
                getSharedPreferences(Default.SHARE_PREFERENCE_NAME, Context.MODE_PRIVATE);
        //Show ratting app
        if (isEnableRatingPopup(context) &&
                getStudyTime(sharedPreferences) >= MINIMUM_RATTING_TIME_MS &&
                sharedPreferences.getBoolean(Default.IN_APP_PURCHASE, false)) {
            DialogHelper.showRatingDialog(context);
        }
    }

    public static void setAudiSpeed(SharedPreferences sharedPreferences, float value) {
        sharedPreferences.edit().putFloat(Default.AUDIO_SPEED_SETTING, value).commit();
    }

    /*public static float getAudioSpeed(SharedPreferences sharedPreferences) {
        return sharedPreferences.getFloat(Default.AUDIO_SPEED_SETTING, Default.AUDIO_SPEED_SETTING_VALUES[0]);
    }*/
}
