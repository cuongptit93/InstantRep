package jp.co.efusion.fragments;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jp.co.efusion.aninstantreply.R;
import jp.co.efusion.aninstantreply.SentenceActivity;
import jp.co.efusion.database.DatabaseHelper;
import jp.co.efusion.database.SentenceSetTable;
import jp.co.efusion.database.ThemeContentTable;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.SettingUtils;

/**
 * A simple {@link Fragment} subclass.
 */
public class FavoriteFragment extends Fragment {

    AdView adView;

    SharedPreferences sharedPreferences;

    private ListView playModeListView;
    private List<HashMap<String,String>> playModeList;

    public FavoriteFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.e("Fragment", "Created");
        //initialize sharepreference
        sharedPreferences = getActivity().getSharedPreferences(Default.SHARE_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_favorite, container, false);
        //initialize UIVIEW
        playModeListView=(ListView)rootView.findViewById(R.id.playModeListView);
        //configure the adView Here
        adView = (AdView) rootView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("83379D0B53764804B8F94258363B28E2")
                .build();
        adView.loadAd(adRequest);
        //load list data
        loadPlayModeListData();

        playModeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent = new Intent(getActivity(),SentenceActivity.class);
                intent.putExtra(Default.FAVORITE_SET,true);
                //intent.putExtra(ThemeContentTable.THEME_CONTENT_ID, getIntent().getIntExtra(ThemeContentTable.THEME_CONTENT_ID, Default.ZERO));
                //intent.putExtra(SentenceSetTable.SET_AUTO, getIntent().getIntExtra(SentenceSetTable.SET_AUTO, Default.ZERO));
                //intent.putExtra(SentenceSetTable.SET_ID, getIntent().getIntExtra(SentenceSetTable.SET_ID, Default.ZERO));
                intent.putExtra(SentenceSetTable.SET_TITLE, getResources().getStringArray(R.array.nav_drawer_items)[1]);
                intent.putExtra(Default.PLAY_MODE, position);

                startActivity(intent);
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        //configure adview
        updateAdView();
    }

    /**
     * Define visibility based on purchase status
     */
    private void updateAdView() {
        //configure adview
        if (sharedPreferences.getBoolean(Default.IN_APP_PURCHASE, false)) {
            //invisible the adview
            adView.setVisibility(View.GONE);
        }
    }

    /**
     *  Load play mode lisview
     */
    private void loadPlayModeListData(){
        playModeList=new ArrayList<HashMap<String, String>>();
        String[] play_mode_list=getResources().getStringArray(R.array.play_mode_list);
        for (int i=0;i<play_mode_list.length;i++){
            HashMap<String,String> map=new HashMap<String,String>();
            map.put(Default.MODE_HASMAP_KEY,play_mode_list[i]);
            playModeList.add(map);
        }

        String[] from = new String[] {Default.MODE_HASMAP_KEY};
        int[] to = new int[] { R.id.setTitleTextView };

        SimpleAdapter simpleAdapter=new SimpleAdapter(getActivity(),playModeList,R.layout.set_list_item,from,to);
        playModeListView.setAdapter(simpleAdapter);
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.e("Favorite Fragment", "On Pause");
        //updateLearningTime(Default.PAUSE_STATE);
    }
    @Override
    public void onResume(){
        super.onResume();
        Log.e("Favorite Fragment", "On resume");
        //updateLearningTime(Default.RESUME_STATE);
    }

    /*
    Update & Restore learning time based on activity state
    @param int state- Possible value is RESUME_STATE & PAUSE_STATE
     */
    private void updateLearningTime(int state){
        if (state==Default.RESUME_STATE){
            //start each tme session
            sharedPreferences.edit().putLong(Default.LEARNING_SESSION_KEY,System.currentTimeMillis()).commit();

        }else if (state==Default.PAUSE_STATE){
//            long learningTime=System.currentTimeMillis()-sharedPreferences.getLong(Default.LEARNING_SESSION_KEY, System.currentTimeMillis());
//            SettingUtils.setStudyTime(sharedPreferences, SettingUtils.getStudyTime(getActivity().getApplicationContext()) + learningTime);
            SettingUtils.setStudyTime(getActivity().getApplicationContext());
        }
    }
}
