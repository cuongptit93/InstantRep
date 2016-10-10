package jp.co.efusion.listhelper;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import jp.co.efusion.aninstantreply.R;
import jp.co.efusion.database.ThemeContentTable;
import jp.co.efusion.utility.CustomButtonListener;
import jp.co.efusion.utility.Default;

/**
 * Created by xor2 on 12/9/15.
 */

public class ThemeContentListAdapter extends CursorAdapter {

    private LayoutInflater cursorInflater;
    CustomButtonListener customButtonListener;
    int contentID;

    public ThemeContentListAdapter(Context mContext,Cursor cr,int flags){
        super(mContext, cr, flags);
        cursorInflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        this.contentID=0;
    }

    public void setCustomButtonListner(CustomButtonListener listener) {
        this.customButtonListener = listener;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return cursorInflater.inflate(R.layout.content_list_item, parent, false);
    }

    @Override
    public void bindView(View view, Context context,Cursor cursor) {

        //initialize view & set value
        TextView titleTextView=(TextView)view.findViewById(R.id.titleTextView);
        final Button contentButton=(Button)view.findViewById(R.id.contentButton);
        ImageView navigationImageView=(ImageView)view.findViewById(R.id.navigationImageView);

        //load title & set
        //load state & price then , configure view & button title based on state
        titleTextView.setText(cursor.getString(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_TITLE)));
        try{
            if (cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_STATE))== Default.STATE_READY_TO_USE){
                //hide button & show navigation image
                contentButton.setVisibility(View.GONE);
                contentButton.setFocusable(false);
                navigationImageView.setVisibility(View.VISIBLE);
            }else{
                //hide navigation image & show button
                navigationImageView.setVisibility(View.GONE);
                contentButton.setVisibility(View.VISIBLE);
                contentButton.setFocusable(true);

                //button title based on state
                if (cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_STATE))== Default.STATE_LOAD_PRICE){
                    contentButton.setText(R.string.state_load_price_text);

                }else if (cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_STATE))== Default.STATE_PURCHASE){
                    contentButton.setText(cursor.getString(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_PRICE)));

                }else if (cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_STATE))== Default.STATE_DOWNLOAD){
                    contentButton.setText(R.string.state_download_text);

                }else if (cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_STATE))== Default.STATE_UPDATE){
                    contentButton.setText(R.string.state_update_text);
                }
            }


        }catch (Exception e){

        }
        //set tag
        contentButton.setTag(cursor.getPosition());
        //set click listener
        contentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (customButtonListener != null) {
                    customButtonListener.onButtonClickListner((Integer)v.getTag());
                }
            }
        });
    }
}
