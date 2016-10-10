package jp.co.efusion.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import jp.co.efusion.aninstantreply.R;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.SettingUtils;

/**
 * A simple {@link Fragment} subclass.
 */
public class WebViewFragment extends Fragment {

    SharedPreferences sharedPreferences;

    public WebViewFragment() {
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_web_view, container, false);
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.e("WebView Fragment", "On Pause");
        //updateLearningTime(Default.PAUSE_STATE);
    }
    @Override
    public void onResume(){
        super.onResume();
        Log.e("WebView Fragment", "On resume");
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
