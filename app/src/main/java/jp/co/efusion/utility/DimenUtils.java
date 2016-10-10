package jp.co.efusion.utility;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by anhdt on 4/22/16.
 */
public class DimenUtils {
    public static int getScreenWidth(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    public static int getScreenHeight(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    public static int getScreenDensity(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.densityDpi;
    }

    public static int getStatusBarHeight(Context c) {
        int result = 0;
        int resourceId = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = c.getResources().getDimensionPixelSize(resourceId);
        }
        //        }
        return result;
    }

    public static Point getRawScreenSize(Context context) {
        Point ret = new Point();
        WindowManager wm =
                (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();

        if (Utils.hasOsVersion(17)) {
            d.getRealSize(ret);
        }
        else {
            DisplayMetrics metrics = new DisplayMetrics();
            d.getMetrics(metrics);
            int w = metrics.widthPixels;
            int h = metrics.heightPixels + getStatusBarHeight(context);
            ret.set(w, h);
        }
        return ret;
    }

    public static Point getScreenSize(Context context) {
        Point ret = new Point();
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        int w = metrics.widthPixels;
        int h = metrics.heightPixels;
        ret.set(w, h);
        return ret;
    }

    public static void makeViewFullScreenImmersive(View v) {
        if (Build.VERSION.SDK_INT >= 11) {
            int flags = 0;
            if (Build.VERSION.SDK_INT >= 14) {
                flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
            if (Build.VERSION.SDK_INT >= 16) {
                flags |= //View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_FULLSCREEN;
            }
            if (Build.VERSION.SDK_INT >= 19) {
                flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            v.setSystemUiVisibility(flags);
        }
    }

    public static int convertPxToDp(Context context, float px) {
        final float scale = context.getResources().getDisplayMetrics().density;
        float ret = (px) / scale;
        return (int)ret;
    }
    public static int convertDpToPx(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        float ret = dp * scale;
        return (int)ret;
    }
    public static int convertSpToPx(Context context, float sp) {
        final float scale = context.getResources().getDisplayMetrics().scaledDensity;
        float ret = sp * scale;
        return (int)ret;
    }

}
