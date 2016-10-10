package jp.co.efusion.listhelper;


public class Log {
    public static String LOG_TAG = "InstantRep";

    //Log Level
    private static final int LOG_VERBOSE = 5;
    private static final int LOG_DEBUG = 4;
    private static final int LOG_INFO = 3;
    private static final int LOG_WARN = 2;
    private static final int LOG_ERROR = 1;

    private static int LOG_LEVEL = LOG_DEBUG;

    public static int getLevelLog() {
        return LOG_LEVEL;
    }

    public static void i(String tag, String message) {
        if (getLevelLog() >= LOG_INFO)
            android.util.Log.i(LOG_TAG, tag + " " + message);
    }

    public static void d(String tag, String message) {
        if (getLevelLog() >= LOG_DEBUG)
            android.util.Log.d(LOG_TAG, tag + " " + message);
    }

    public static void e(String tag, String message) {
        if (getLevelLog() >= LOG_ERROR)
            android.util.Log.e(LOG_TAG, tag + " " + message);
    }

    public static void w(String tag, String message) {
        if (getLevelLog() >= LOG_WARN)
            android.util.Log.w(LOG_TAG, tag + " " + message);
    }

    public static void v(String tag, String message) {
        if (getLevelLog() >= LOG_VERBOSE)
            android.util.Log.v(LOG_TAG, tag + " " + message);
    }


    public static void i(String message) {
        if (getLevelLog() >= LOG_INFO)
            android.util.Log.i(LOG_TAG, message);
    }

    ///
    public static void d(String message) {
        if (getLevelLog() >= LOG_DEBUG)
            android.util.Log.d(LOG_TAG, message);
    }

    public static void e(String message) {
        if (getLevelLog() >= LOG_ERROR)
            android.util.Log.e(LOG_TAG, message);
    }

    public static void w(String message) {
        if (getLevelLog() >= LOG_WARN)
            android.util.Log.w(LOG_TAG, message);
    }

    public static void v(String message) {
        if (getLevelLog() >= LOG_VERBOSE)
            android.util.Log.v(LOG_TAG, message);
    }


}
