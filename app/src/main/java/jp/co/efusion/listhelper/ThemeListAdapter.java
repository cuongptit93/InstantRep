package jp.co.efusion.listhelper;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import jp.co.efusion.aninstantreply.R;
import jp.co.efusion.utility.DimenUtils;

/**
 * Created by xor2 on 12/8/15.
 */
public class ThemeListAdapter extends BaseAdapter{

    private Context context;
    private ArrayList<ThemeItem> themeItems;

    public ThemeListAdapter(Context context, ArrayList<ThemeItem> themeItems){
        this.context = context;
        this.themeItems = themeItems;
    }

    @Override
    public int getCount() {
        return themeItems.size();
    }

    @Override
    public Object getItem(int position) {
        return themeItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater)context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.theme_list_item, null);
        }
        ImageView imgIcon = (ImageView) convertView.findViewById(R.id.themeImageView);
        TextView txtTitle = (TextView) convertView.findViewById(R.id.themeTitleTextView);

        //create drawable resource id
        String mDrawableName = themeItems.get(position).getImage().substring(0,themeItems.get(position).getImage().indexOf("."));
        int resID = context.getResources().getIdentifier(mDrawableName.toLowerCase(), "drawable", context.getPackageName());

//        imgIcon.setBackgroundResource(resID);
        scaleImage(imgIcon, resID);

       // txtTitle.setText(themeItems.get(position).getTitle());

        return convertView;

    }

    private void scaleImage(ImageView view, int resId) throws NoSuchElementException  {
        // Get bitmap from the the ImageView.
        Bitmap bitmap = null;

        try {
            bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
//            Drawable drawing = view.getDrawable();
//            bitmap = ((BitmapDrawable) drawing).getBitmap();
        } catch (NullPointerException e) {
            e.printStackTrace();
            throw new NoSuchElementException("No drawable on given view");
        } catch (ClassCastException e) {
        }

        int width = 0;
        try {
            width = bitmap.getWidth();
        } catch (NullPointerException e) {
            throw new NoSuchElementException("Can't find bitmap on given view/drawable");
        }

        int height = bitmap.getHeight();
        int bounding = DimenUtils.getScreenWidth(context);

        float xScale = ((float) bounding) / width;
        float yScale = ((float) bounding) / height;
        float scale = (xScale <= yScale) ? xScale : yScale;

        // Create a matrix for the scaling and add the scaling data
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        // Create a new bitmap and convert it to a format understood by the ImageView
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        width = scaledBitmap.getWidth(); // re-use
        height = scaledBitmap.getHeight(); // re-use
        BitmapDrawable result = new BitmapDrawable(scaledBitmap);

        // Apply the scaled bitmap
        view.setImageDrawable(result);

        // Now change ImageView's dimensions to match the scaled image
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }

}
