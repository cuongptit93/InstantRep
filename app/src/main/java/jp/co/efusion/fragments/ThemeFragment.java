package jp.co.efusion.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


import jp.co.efusion.aninstantreply.R;
import jp.co.efusion.aninstantreply.ThemeContentActivity;
import jp.co.efusion.database.DatabaseHelper;
import jp.co.efusion.database.ThemeTable;
import jp.co.efusion.listhelper.ThemeItem;
import jp.co.efusion.listhelper.ThemeListAdapter;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.SettingUtils;

public class ThemeFragment extends Fragment {

    DatabaseHelper databaseHelper;
    Cursor cursor;
    private TextView learningObjectiveTextView,learningDayTextview,learningHourTextView,questionTriedTextView;
    private ListView themeListView;
    AdView adView;

    private ThemeListAdapter themeListAdapter;
    private ArrayList<ThemeItem> themeItems;

    SharedPreferences sharedPreferences;

    public ThemeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Log.e("Fragment", "Created");
        //initialize database helper
        databaseHelper=new DatabaseHelper(getActivity());
        //open database
        databaseHelper.openDatabase();

        //initialize sharepreference
        sharedPreferences = getActivity().getSharedPreferences(Default.SHARE_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView=inflater.inflate(R.layout.fragment_theme, container, false);

        //initial all fragment UIVIEW
        learningObjectiveTextView=(TextView)rootView.findViewById(R.id.learningObjectiveTextView);
        learningDayTextview=(TextView)rootView.findViewById(R.id.learningDayTextview);
        learningHourTextView=(TextView)rootView.findViewById(R.id.learningHourTextView);
        questionTriedTextView=(TextView)rootView.findViewById(R.id.questionTriedTextView);

        themeListView=(ListView)rootView.findViewById(R.id.themeListView);

        //configure the adView Here
        adView = (AdView) rootView.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("83379D0B53764804B8F94258363B28E2")
                .build();
        adView.loadAd(adRequest);

        //initialize array list
        themeItems=new ArrayList<ThemeItem>();
        //load theme data
        cursor=databaseHelper.getQueryResultData(ThemeTable.TABLE_NAME, null, null, null, null, null, null, null);
        if (cursor!=null && cursor.getCount()!=0){
            cursor.moveToFirst();
            do{
                //add theme item to the themeItems ArrayList
                themeItems.add(new ThemeItem(cursor.getString(cursor.getColumnIndex(ThemeTable.THEME_TITLE)), cursor.getString(cursor.getColumnIndex(ThemeTable.THEME_IMAGE))));
            }while(cursor.moveToNext());
        }
        themeListAdapter=new ThemeListAdapter(getActivity(),themeItems);
        themeListView.setAdapter(themeListAdapter);

        //set onClick listerner to learning objective
        learningObjectiveTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show dialog box
                showDialogBox();
            }
        });

        //set click on item listener
        themeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Navigate to theme content package activity
                cursor.moveToPosition(position);
                Intent intent=new Intent(getActivity(), ThemeContentActivity.class);
                intent.putExtra(ThemeTable.THEME_ID,cursor.getString(cursor.getColumnIndex(ThemeTable.THEME_ID)));
                intent.putExtra(ThemeTable.THEME_TITLE,cursor.getString(cursor.getColumnIndex(ThemeTable.THEME_TITLE)));

                Log.e("Theme ID",cursor.getString(cursor.getColumnIndex(ThemeTable.THEME_ID)));
                startActivity(intent);

            }
        });
        Log.e("Fragment", "Created View");

        return rootView;
    }


    @Override
    public void onStart(){

        super.onStart();

        //add dynamic function here
        Log.e("Fragment", "Start ");
        //load save data
        learningObjectiveTextView.setText(sharedPreferences.getString(Default.LEARNING_OBJECTIVE_KEY, getResources().getString(R.string.learning_objective_default_text)));
        //check value set or not
        if (sharedPreferences.getLong(Default.LEARNING_INITIAL_DATE_KEY,Default.STATISTICS_DEFAULT_VALUE)!=Default.STATISTICS_DEFAULT_VALUE){

            String previousDate = sharedPreferences.getString("app_Launch_date", "");
            String dateToday = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Long daycount = sharedPreferences.getLong("app_Launch_Count", Default.STATISTICS_DEFAULT_VALUE);

            if(!previousDate.equals(dateToday)){

                sharedPreferences.edit().putString("app_Launch_date", dateToday).commit();
                daycount++;
                sharedPreferences.edit().putLong("app_Launch_Count", daycount).commit();
            }

        //    learningDayTextview.setText(String.format());

            learningDayTextview.setText(String.format("%d", daycount));

            //learningDayTextview.setText(daysBetween(System.currentTimeMillis(),sharedPreferences.getLong(Default.LEARNING_INITIAL_DATE_KEY,Default.STATISTICS_DEFAULT_VALUE))+"");
        }else{
            //set initial date value
            sharedPreferences.edit().putLong(Default.LEARNING_INITIAL_DATE_KEY,System.currentTimeMillis()).commit();
            learningDayTextview.setText(Default.STATISTICS_DEFAULT_VALUE + "");

            String dateToday = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            sharedPreferences.edit().putString("app_Launch_date", dateToday).commit();
            sharedPreferences.edit().putLong("app_Launch_Count", 0).commit();

          //  Log.e("Fragment", dateToday);

        }
       // float formatedHours=((float)sharedPreferences.getLong(Default.LEARNING_HOUR_KEY, Default.STATISTICS_DEFAULT_VALUE) / ((float)(1000*60*60)));
        //learningHourTextView.setText(String.format("%.2f", formatedHours));

        long totalSeconds = SettingUtils.getStudyTime(getActivity().getApplicationContext()) / 1000;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;
        learningHourTextView.setText(String.format("%02d:%02d", hours,minutes));

        questionTriedTextView.setText(sharedPreferences.getLong(Default.QUESTION_TRIED_KEY, Default.STATISTICS_DEFAULT_VALUE) + "");

        //configure adview
        if (sharedPreferences.getBoolean(Default.IN_APP_PURCHASE,false)){
            //in visible the adview
            adView.setVisibility(View.GONE);
        }

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //all connection close here
        Log.e("Fragment", "Destroy ");
        databaseHelper.closeDataBase();
    }

    /*
     *learningObjectiveTextView click implementation
     */
    private void showDialogBox(){

        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.objective_dialog_layout);
        WindowManager.LayoutParams lp = new        WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = (int) (metrics.widthPixels * 0.80);
        lp.width = screenWidth;//WindowManager.LayoutParams.MATCH_PARENT;
        dialog.show();
        dialog.getWindow().setAttributes(lp);

        final EditText dialogEditText=(EditText)dialog.findViewById(R.id.dialogEditText);
        //dialog click event
        Button dialogOkButton=(Button)dialog.findViewById(R.id.dialogOkButton);
        dialogOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                //save the input value
                if (dialogEditText.getText().length() > 0) {
                    sharedPreferences.edit().putString(Default.LEARNING_OBJECTIVE_KEY, dialogEditText.getText().toString()).commit();
                    learningObjectiveTextView.setText(dialogEditText.getText().toString());
                }
            }
        });
        Button dialogCancelButton=(Button)dialog.findViewById(R.id.dialogCancelButton);
        dialogCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

    }

    /*
    Find days between two date in miliseconds
     */
    public int daysBetween(long t1, long t2) {
        return (int) ((t1 - t2) / (1000 * 60 * 60 * 24));
    }


    @Override
    public void onPause(){
        super.onPause();
        Log.e("Theme Fragment", "On Pause");
        //updateLearningTime(Default.PAUSE_STATE);
    }
    @Override
    public void onResume(){
        super.onResume();
        Log.e("Theme Fragment", "On resume");
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