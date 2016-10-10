package jp.co.efusion.utility;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Created by anhdt on 9/1/16.
 */
public class Utils {
    /**
     * Open Google PlayStore
     *
     * @param context
     * @param packageName
     */
    public static void openPlayStoreAppDetails(Context context, String packageName) {
        Intent localIntent = new Intent(Intent.ACTION_VIEW);
        localIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        );
        localIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));

        try {
            localIntent.setPackage("com.android.vending");
            context.startActivity(localIntent);

        } catch (ActivityNotFoundException e) {
            localIntent.setPackage(null);
            context.startActivity(localIntent);
        }
    }

    public static boolean hasOsVersion(int ver) {
        return Build.VERSION.SDK_INT >= ver;
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
}
