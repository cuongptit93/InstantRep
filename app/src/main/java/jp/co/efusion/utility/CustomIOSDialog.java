package jp.co.efusion.utility;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import jp.co.efusion.aninstantreply.R;

/**
 * Created by xor2 on 12/23/15.
 */

public class CustomIOSDialog {
    private Context context;
    private Dialog dialog;

    private IOSDialogListener iosDialogListener = null;
    private IOSOptionDialogListener iosOptionDialogListener = null;

    public CustomIOSDialog(Context mContext) {
        context = mContext;
    }

    public void createConfirmationDialog(String title, String message) {

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.custom_ios_confirmation_dialog_layout);
        dialog.setCancelable(false);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int screenWidth = (int) (metrics.widthPixels * 0.75);
        lp.width = screenWidth;//WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);

        TextView titleView = (TextView) dialog.findViewById(R.id.iosDialogTitle);
        TextView messageView = (TextView) dialog.findViewById(R.id.iosDialogMessage);
        titleView.setText(title);
        messageView.setText(message);
        //dialog click event
        Button dialogOkButton = (Button) dialog.findViewById(R.id.iosDialogOkButton);
        dialogOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                //save the input value
                if (iosDialogListener != null) {
                    iosDialogListener.onOk();
                }

            }
        });
        Button dialogCancelButton = (Button) dialog.findViewById(R.id.iosDialogCancelButton);
        dialogCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (iosDialogListener != null) {
                    iosDialogListener.onCancel();
                }
            }
        });

        dialog.show();
    }

    public void createAlertDialog(String title, String message) {

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.custom_ios_alert_dialog_layout);
        dialog.setCancelable(false);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int screenWidth = (int) (metrics.widthPixels * 0.75);
        lp.width = screenWidth;//WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);

        TextView titleView = (TextView) dialog.findViewById(R.id.iosDialogTitle);

        if (title == null) {
            titleView.setVisibility(View.GONE);
        } else {
            titleView.setText(title);
        }

        TextView messageView = (TextView) dialog.findViewById(R.id.iosDialogMessage);
        if (message == null) {
            messageView.setVisibility(View.GONE);
        } else {
            messageView.setText(message);
        }
        //dialog click event
        Button dialogOkButton = (Button) dialog.findViewById(R.id.iosDialogOkButton);
        dialogOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                //save the input value
                if (iosDialogListener != null) {
                    iosDialogListener.onOk();
                }
            }
        });


        dialog.show();
    }

    public void createOptiontDialog(String title, String message, List<String> optionText) {

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.custom_ios_option_dialog_layout);
        dialog.setCancelable(false);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int screenWidth = (int) (metrics.widthPixels * 0.75);
        lp.width = screenWidth;//WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(lp);

        TextView titleView = (TextView) dialog.findViewById(R.id.iosDialogTitle);
        if (title == null) {
            titleView.setVisibility(View.GONE);
        } else {
            titleView.setText(title);
        }

        TextView messageView = (TextView) dialog.findViewById(R.id.iosDialogMessage);
        if (message == null) {
            messageView.setVisibility(View.GONE);

        } else {
            messageView.setText(message);

        }

        View.OnClickListener buttonClickedListner = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (iosOptionDialogListener != null) {
                    iosOptionDialogListener.onClicked((Integer) v.getTag());
                }
            }
        };
        ViewGroup btnHolder = (ViewGroup) dialog.findViewById(R.id.btnHolder);
        for (int i = 0, size = optionText.size(); i < size; i++) {
            String text = optionText.get(i);
            View buttonHolder = LayoutInflater.from(context).
                    inflate(R.layout.layout_dialog_button, btnHolder, false);
            if (i == size - 1) {
                buttonHolder.setBackgroundResource(R.drawable.dialog_button_shape);
            }

            Button button = (Button) buttonHolder.findViewById(R.id.buttonId);
            button.setText(text);
            button.setTag(i);
            button.setOnClickListener(buttonClickedListner);

            btnHolder.addView(buttonHolder);
        }

        dialog.show();
    }

    public void setIOSDialogListener(IOSDialogListener listener) {
        iosDialogListener = listener;
    }

    public void setIOSDialogListener(IOSOptionDialogListener listener) {
        iosOptionDialogListener = listener;
    }
}
