package jp.co.efusion.aninstantreply;

/**
 * Created by xor3 on 3/13/16.
 */
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.util.List;

public class TutorialPagerAdapter extends PagerAdapter
{
    LayoutInflater _inflater = null;

    public List<Integer> list_drawable_id;

    public Button button_finish;
    private TutorialPagerListener listener;

    public TutorialPagerAdapter(Context context, List<Integer> list)
    {
        super();
        _inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.list_drawable_id = list;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        LinearLayout layout = (LinearLayout)_inflater.inflate(R.layout.card_pager_tutorial, null);

//        nameView = (TextView)layout.findViewById(R.id.word);
//        nameView.setText(list.get(position));

        ImageView imageView = (ImageView)layout.findViewById(R.id.imageView);
        imageView.setImageResource(list_drawable_id.get(position));



        //finish button
        if( position==list_drawable_id.size()-1 )
        {
            button_finish = (Button) layout.findViewById(R.id.button_finish);
            button_finish.setVisibility(View.VISIBLE);
            button_finish.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (listener != null)
                        listener.finished();
                }
            });
        }



        container.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        container.removeView((View) object);
    }

    //これが実装されていないと、データ変更時に正しくViewの更新がされない
    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }


    @Override
    public int getCount()
    {
        return list_drawable_id.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object obj)
    {
        return view.equals(obj);
    }


    public void setFinishedListener( TutorialPagerListener listener )
    {
        this.listener = listener;
    }

}
