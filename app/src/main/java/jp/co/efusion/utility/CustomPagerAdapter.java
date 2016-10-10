package jp.co.efusion.utility;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import jp.co.efusion.aninstantreply.R;

/**
 * Created by xor2 on 12/21/15.
 */
public class CustomPagerAdapter extends PagerAdapter {

    List<View> viewList;
    public CustomPagerAdapter(Context context,Boolean isChunkPLay){

        viewList=new ArrayList<View>();
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        viewList.add(inflater.inflate(R.layout.view_pager_fake_page, null));
        viewList.add(inflater.inflate(R.layout.view_pager_question_page, null));
        viewList.add(inflater.inflate(R.layout.view_pager_answer_page, null));
        if (isChunkPLay){
            viewList.add(inflater.inflate(R.layout.view_pager_final_page_question, null));
            viewList.add(inflater.inflate(R.layout.view_pager_final_page, null));
        }
        viewList.add(inflater.inflate(R.layout.view_pager_fake_page, null));

    }

    @Override
    public int getCount() {

        return viewList.size();
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == ((View) arg1);

    }

    @Override
    public void destroyItem(View arg0, int arg1, Object arg2) {
        ((ViewPager) arg0).removeView((View) arg2);

    }

    @Override
    public Object instantiateItem(View collection, int position) {
//        LayoutInflater inflater = (LayoutInflater) collection.getContext()
//                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//
//        int resId = 0;
//        switch (position) {
//            case 0:
//                resId = R.layout.view_pager_fake_page;
//                break;
//            case 1:
//                resId = R.layout.view_pager_question_page;
//                break;
//            case 2:
//                resId = R.layout.view_pager_question_page;
//                break;
//            case 3:
//                resId = R.layout.view_pager_fake_page;
//                break;
//
//        }

        //View view = inflater.inflate(resId, null);
        View view = viewList.get(position);

        ((ViewPager) collection).addView(view, 0);

        return view;
    }

    public View findViewById(int position, int id) {
        return viewList.get(position).findViewById(id);
    }
}
