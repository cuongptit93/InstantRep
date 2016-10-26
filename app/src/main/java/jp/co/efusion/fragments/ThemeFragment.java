package jp.co.efusion.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.datetimepickercustom.SlideDateTimeListener;
import com.example.datetimepickercustom.SlideDateTimePicker;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
    private TextView learningObjectiveTextView,learningDayTextview,learningHourTextView,
            questionTriedTextView, defaultContentTextview, timerTargetTextView,
            dayChooseTextView, dayEndTextView ;

    private ListView themeListView;
    AdView adView;

    private ThemeListAdapter themeListAdapter;
    private ArrayList<ThemeItem> themeItems;

    SharedPreferences sharedPreferences;

    final CharSequence[] itemsChooseSelect = {"Day Choose", "Default"};

    private DatePicker mDatePicker;
    private Calendar mCalendar;
    private int mYear, mMonth, mDay, getDay, getMonth, getYear;
    DatePickerDialog datePickerDialog;
    private FragmentActivity myContext;

    boolean showLimit = true;

    private SimpleDateFormat mFormatter = new SimpleDateFormat("MMMM dd yyyy");

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

        defaultContentTextview = (TextView) rootView.findViewById(R.id.txtDefaultContent);
        timerTargetTextView = (TextView) rootView.findViewById(R.id.txtTimerTarget);
        dayChooseTextView = (TextView) rootView.findViewById(R.id.txtDayChoose);
        dayEndTextView = (TextView) rootView.findViewById(R.id.txtDayEnd);


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

        defaultContentTextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!sharedPreferences.getBoolean(Default.DEFAULT_COUNTDOWN_KEY, Default.DEFAULT_TEXT_COUNTDOWN)){
                    showDialogCountDown();
                }
                else{
                    showDialogTitleCountDown();
                }
            }
        });

        dayChooseTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //showDialogSelectItem();
                Date date;
                if(getDay !=0 && getYear !=0){
                    date = new Date(getYear - 1900, getMonth, getDay);
                }
                else{
                    date = new Date();
                }
                new SlideDateTimePicker.Builder(myContext.getSupportFragmentManager())
                        .setListener(dateTimeListener)
                        .setInitialDate(date)
                        //.setMinDate(minDate)
                        //.setMaxDate(maxDate)
                        //.setIs24HourTime(true)
                        //.setTheme(SlideDateTimePicker.HOLO_LIGHT)
                        .setIndicatorColor(Color.parseColor("#990000"))
                        .build()
                        .show();
            }
        });

        Log.e("Fragment", "Created View");

        return rootView;
    }


    @Override
    public void onStart(){
        super.onStart();

        /*sharedPreferences.edit().remove(Default.DEFAULT_COUNTDOWN_KEY).commit();
        sharedPreferences.edit().remove(Default.GOAL_CONTENT).commit();
        sharedPreferences.edit().remove(Default.GOAL_DAY_END).commit();
        sharedPreferences.edit().remove(Default.GET_DAY_GOAL).commit();
        sharedPreferences.edit().remove(Default.GET_MONTH_GOAL).commit();
        sharedPreferences.edit().remove(Default.GET_YEAR_GOAL).commit();*/
        //sharedPreferences.edit().remove(Default.LIMIT_GOAL).commit();

        ///////get date CountDown ///////////
        mCalendar = Calendar.getInstance();
        mYear = mCalendar.get(Calendar.YEAR);
        mMonth = mCalendar.get(Calendar.MONTH);
        mDay = mCalendar.get(Calendar.DAY_OF_MONTH);

        getYear = sharedPreferences.getInt(Default.GET_YEAR_GOAL, mCalendar.get(Calendar.YEAR));
        getMonth = sharedPreferences.getInt(Default.GET_MONTH_GOAL, mCalendar.get(Calendar.MONTH));
        getDay = sharedPreferences.getInt(Default.GET_DAY_GOAL, mCalendar.get(Calendar.DAY_OF_MONTH));


        if(!sharedPreferences.getBoolean(Default.DEFAULT_COUNTDOWN_KEY, Default.DEFAULT_TEXT_COUNTDOWN)){
            timerTargetTextView.setVisibility(View.INVISIBLE);
            dayChooseTextView.setVisibility(View.INVISIBLE);
            dayEndTextView.setVisibility(View.INVISIBLE);
        }
        else {
            defaultContentTextview.setTextSize(18f);
            timerTargetTextView.setVisibility(View.VISIBLE);
            dayChooseTextView.setVisibility(View.VISIBLE);
            dayEndTextView.setVisibility(View.VISIBLE);
            defaultContentTextview.setText(sharedPreferences.getString(Default.GOAL_CONTENT, getResources().getString(R.string.goalContent)));
            dayChooseTextView.setText(sharedPreferences.getString(Default.GOAL_DAY_END, getResources().getString(R.string.dayChoose)));

            try {
                if(getDate(getYear, getMonth, getDay) < Integer.parseInt(dayChooseTextView.getText().toString())){
                    dayChooseTextView.setText(String.valueOf(getDate(getYear, getMonth, getDay)));
                }
            }
            catch (Exception e){

            }

        }

        if(dayChooseTextView.getText().toString().equals("0")){
            showLimitGoalDialog();
            sharedPreferences.edit().remove(Default.DEFAULT_COUNTDOWN_KEY).commit();
            sharedPreferences.edit().remove(Default.GOAL_CONTENT).commit();
            sharedPreferences.edit().remove(Default.GOAL_DAY_END).commit();
            sharedPreferences.edit().remove(Default.GET_DAY_GOAL).commit();
            sharedPreferences.edit().remove(Default.GET_MONTH_GOAL).commit();
            sharedPreferences.edit().remove(Default.GET_YEAR_GOAL).commit();
            timerTargetTextView.setVisibility(View.INVISIBLE);
            dayChooseTextView.setVisibility(View.INVISIBLE);
            dayEndTextView.setVisibility(View.INVISIBLE);
            defaultContentTextview.setText(R.string.defaultGoal);
            defaultContentTextview.setTextSize(15f);
        }


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

    @Override
    public void onAttach(Activity activity) {
        myContext=(FragmentActivity) activity;
        super.onAttach(activity);
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
        /*final TextView dialogTitle = (TextView) dialog.findViewById(R.id.txtDialogTitle);
        final TextView dialogQuestion = (TextView) dialog.findViewById(R.id.txtDialogTitle);*/
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

    //showDialog countDown

    private void showDialogCountDown(){
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.custom_goal_dialog);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = (int) (metrics.widthPixels * 0.8);
        lp.width = screenWidth;//WindowManager.LayoutParams.MATCH_PARENT;
        dialog.show();
        dialog.getWindow().setAttributes(lp);

        Button setGoal =(Button)dialog.findViewById(R.id.btnSetContent);
        Button setDefault =(Button)dialog.findViewById(R.id.btnSetDefaul);

        setGoal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                showDialogTitleCountDown();
            }
        });

        setDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                defaultContentTextview.setTextSize(18f);
                timerTargetTextView.setVisibility(View.VISIBLE);
                dayChooseTextView.setVisibility(View.VISIBLE);
                dayEndTextView.setVisibility(View.VISIBLE);
                sharedPreferences.edit().putBoolean(Default.DEFAULT_COUNTDOWN_KEY, true).commit();
                defaultContentTextview.setText(sharedPreferences.getString(Default.GOAL_CONTENT, getResources().getString(R.string.goalContent)));
            }
        });
    }

    private void showLimitGoalDialog(){
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.custom_limit_goal_dialog);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = (int) (metrics.widthPixels * 0.8);
        lp.width = screenWidth;//WindowManager.LayoutParams.MATCH_PARENT;
        dialog.show();
        dialog.getWindow().setAttributes(lp);

        final Button LimitOK =(Button)dialog.findViewById(R.id.btnLimitOK);
        LimitOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

    }


    private void showDialogTitleCountDown(){
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setContentView(R.layout.objective_dialog_layout);
        WindowManager.LayoutParams lp = new        WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = (int) (metrics.widthPixels * 0.9);
        lp.width = screenWidth;//WindowManager.LayoutParams.MATCH_PARENT;
        dialog.show();
        dialog.getWindow().setAttributes(lp);

        final EditText dialogEditText=(EditText) dialog.findViewById(R.id.dialogEditText);
        final TextView dialogQuestion = (TextView) dialog.findViewById(R.id.txtDialogQuestion);
        final TextView dialogTitle = (TextView) dialog.findViewById(R.id.txtDialogTitle);
        dialogQuestion.setWidth(0);
        dialogQuestion.setHeight(0);
        dialogTitle.setText(R.string.titleGoalDialog);

        //dialog click event
        Button dialogOkButton=(Button)dialog.findViewById(R.id.dialogOkButton);
        dialogOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                if (dialogEditText.getText().length() > 0) {
                    sharedPreferences.edit().putString(Default.GOAL_CONTENT, dialogEditText.getText().toString()).commit();
                }

                if(!sharedPreferences.getBoolean(Default.DEFAULT_COUNTDOWN_KEY, Default.DEFAULT_TEXT_COUNTDOWN)){
                    new SlideDateTimePicker.Builder(myContext.getSupportFragmentManager())
                            .setListener(dateTimeListener)
                            .setInitialDate(new Date(/*2016-1900, 9, 30*/))
                            //.setMinDate(minDate)
                            //.setMaxDate(maxDate)
                            //.setIs24HourTime(true)
                            //.setTheme(SlideDateTimePicker.HOLO_LIGHT)
                            .setIndicatorColor(Color.parseColor("#990000"))
                            .build()
                            .show();
                    sharedPreferences.edit().putBoolean(Default.DEFAULT_COUNTDOWN_KEY, true).commit();
                }
                else{
                    defaultContentTextview.setText(sharedPreferences.getString(Default.GOAL_CONTENT, getResources().getString(R.string.goalContent)));
                }

            }
        });
        Button dialogCancelButton=(Button)dialog.findViewById(R.id.dialogCancelButton);
        dialogCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if(!sharedPreferences.getBoolean(Default.DEFAULT_COUNTDOWN_KEY, Default.DEFAULT_TEXT_COUNTDOWN)){
                    new SlideDateTimePicker.Builder(myContext.getSupportFragmentManager())
                            .setListener(dateTimeListener)
                            .setInitialDate(new Date())
                            //.setMinDate(minDate)
                            //.setMaxDate(maxDate)
                            //.setIs24HourTime(true)
                            //.setTheme(SlideDateTimePicker.HOLO_LIGHT)
                            .setIndicatorColor(Color.parseColor("#990000"))
                            .build()
                            .show();
                    sharedPreferences.edit().putBoolean(Default.DEFAULT_COUNTDOWN_KEY, true).commit();
                }
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

    private SlideDateTimeListener dateTimeListener = new SlideDateTimeListener() {

        @Override
        public void onDateTimeSet(Date date, int year, int month, int day) {

            int getDayGoal = getDate(year, month, day);
            if(getDayGoal > 0){
                sharedPreferences.edit().putString(Default.GOAL_DAY_END, String.valueOf(getDayGoal)).commit();
                dayChooseTextView.setText(sharedPreferences.getString(Default.GOAL_DAY_END, getResources().getString(R.string.dayChoose)));

                sharedPreferences.edit().putInt(Default.GET_DAY_GOAL, day).commit();
                sharedPreferences.edit().putInt(Default.GET_MONTH_GOAL, month).commit();
                sharedPreferences.edit().putInt(Default.GET_YEAR_GOAL, year).commit();

                getYear = sharedPreferences.getInt(Default.GET_YEAR_GOAL, mCalendar.get(Calendar.YEAR));
                getMonth = sharedPreferences.getInt(Default.GET_MONTH_GOAL, mCalendar.get(Calendar.MONTH));
                getDay = sharedPreferences.getInt(Default.GET_DAY_GOAL, mCalendar.get(Calendar.DAY_OF_MONTH));
            }
            else{
                Toast.makeText(getActivity(), "Please choose day larger than current day!", Toast.LENGTH_SHORT).show();
            }

            defaultContentTextview.setTextSize(18f);
            timerTargetTextView.setVisibility(View.VISIBLE);
            dayChooseTextView.setVisibility(View.VISIBLE);
            dayEndTextView.setVisibility(View.VISIBLE);
            defaultContentTextview.setText(sharedPreferences.getString(Default.GOAL_CONTENT, getResources().getString(R.string.goalContent)));
        }

        // Optional cancel listener
        @Override
        public void onDateTimeCancel() {
            defaultContentTextview.setTextSize(18f);
            timerTargetTextView.setVisibility(View.VISIBLE);
            dayChooseTextView.setVisibility(View.VISIBLE);
            dayEndTextView.setVisibility(View.VISIBLE);
            defaultContentTextview.setText(sharedPreferences.getString(Default.GOAL_CONTENT, getResources().getString(R.string.goalContent)));
        }
    };

    private int getDate(int year, int month, int day){

        int setMonth = 0,  setYear = 0, setDay = 0;
        setYear = year - mYear;
        if(month - mMonth >= 0)
            setMonth = month - mMonth + setYear*12;
        else
            setMonth = setYear*12 - (mMonth - month);

        if(setMonth == 0)
            setDay = day - mDay;
        else if(day - mDay >= 0)
            setDay = setMonth*30 + (day - mDay);
        else
            setDay = setMonth*30 - (mDay - day);

        return setDay;
    }

}
