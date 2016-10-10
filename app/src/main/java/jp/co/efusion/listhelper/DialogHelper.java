package jp.co.efusion.listhelper;

import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import jp.co.efusion.aninstantreply.R;
import jp.co.efusion.utility.CustomIOSDialog;
import jp.co.efusion.utility.IOSOptionDialogListener;
import jp.co.efusion.utility.SettingUtils;
import jp.co.efusion.utility.Utils;

/**
 * Created by anhdt on 9/1/16.
 */
public class DialogHelper {
    /**
     * Show ratting dialog
     * @param context
     */
    public static void showRatingDialog(final Context context) {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                CustomIOSDialog customIOSDialog = new CustomIOSDialog(context);
                String title = context.getResources().getString(R.string.ratting_content);
                List<String> optionText = new ArrayList<>();
                optionText.add(context.getResources().getString(R.string.ratting_button_rate_app));
                optionText.add(context.getResources().getString(R.string.ratting_button_cancel));
                customIOSDialog.createOptiontDialog(null, title, optionText);
                customIOSDialog.setIOSDialogListener(new IOSOptionDialogListener() {
                    @Override
                    public void onClicked(int position) {
                        if (position == 0) {
                            Utils.openPlayStoreAppDetails(context.getApplicationContext(),
                                    context.getApplicationContext().getPackageName());
                        }
                        SettingUtils.enableRatingPopup(context, false);
                    }
                });
            }
        });
    }
}
