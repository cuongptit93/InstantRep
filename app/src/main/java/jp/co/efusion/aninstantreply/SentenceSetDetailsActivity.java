package jp.co.efusion.aninstantreply;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.baoyz.actionsheet.ActionSheet;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jp.co.efusion.database.SentenceSetTable;
import jp.co.efusion.database.ThemeContentTable;
import jp.co.efusion.database.ThemeTable;
import jp.co.efusion.fragments.SettingFragment;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.SettingUtils;


public class SentenceSetDetailsActivity extends ActionBarActivity implements ActionSheet.ActionSheetListener {
    SharedPreferences sharedPreferences;

    private ListView playModeListView;
    private TextView detailsTexView;
    private List<HashMap<String, String>> playModeList;

    private Boolean FREE_SET;

    private String[] actionSheetItems;

    //declare admob adview
    private AdView adView;
    String free_Set, theme_no, setTitleActionBar;
    MenuItem optionMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.theme_color_green)));
        setContentView(R.layout.activity_sentence_set_details);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        free_Set = preferences.getString("Free_Set", "");
        theme_no = preferences.getString("Theme_No", "");
        if (getIntent().getBooleanExtra(Default.GO_TO_PLAY_SCREEN, false)) {
            gotoSentenceScreen(getResources().getStringArray(R.array.play_mode_list)[1]);
            finish();
        }

        //load item Home And Setting to Action sheet.
        final String[] itemAction = getResources().getStringArray(R.array.action_sheet_initial_item);
        actionSheetItems = Arrays.copyOf(itemAction, 2);

        //show home back button
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //add title from SENTENCE SET
        if (free_Set.equals("true"))
            setTitleActionBar = getIntent().getStringExtra(ThemeTable.THEME_TITLE);
        else
            setTitleActionBar = getIntent().getStringExtra(SentenceSetTable.SET_TITLE);


        //initialize sharepreference
        sharedPreferences = getSharedPreferences(Default.SHARE_PREFERENCE_NAME,
                Context.MODE_PRIVATE);

        //initialize UIVIEW
        playModeListView = (ListView) findViewById(R.id.playModeListView);
        detailsTexView = (TextView) findViewById(R.id.detailsTexView);

        //load list data
        loadPlayModeListData();

        //load details data
        detailsTexView.setText(getIntent().getStringExtra(SentenceSetTable.SET_DETAILS));

        FREE_SET = getIntent().getBooleanExtra(Default.FREE_SET, false);

        //configure the adView Here
        adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("83379D0B53764804B8F94258363B28E2")
                .build();
        adView.loadAd(adRequest);
        //update add view
        updateAdView();

        playModeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> obj = (HashMap<String, Object>) parent.getItemAtPosition(position);
                String listItem = (String) obj.get(Default.MODE_HASMAP_KEY);
                gotoSentenceScreen(listItem);
            }
        });

    }

    @Override
    protected void onStart() {
        if(sharedPreferences.getInt("sentenceSetIDCallBack", Default.ZERO)!=0){
            setTitleActionBar = sharedPreferences.getString("titleCallBack", "");
        }
        getSupportActionBar().setTitle(setTitleActionBar);
        super.onStart();
    }

    private void gotoSentenceScreen(String playMode) {
        Intent intent = new Intent(SentenceSetDetailsActivity.this, SentenceActivity.class);
        intent.putExtra(Default.FAVORITE_SET, false);
        intent.putExtra(Default.FREE_SET, FREE_SET);

        int contentID = getIntent().getIntExtra(ThemeContentTable.THEME_CONTENT_ID, Default.ZERO);
        boolean goToPlayScreen;
        if (contentID == Default.ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID) {
            goToPlayScreen = true;
        } else {
            goToPlayScreen = false;
        }

        intent.putExtra(Default.GO_TO_PLAY_SCREEN, goToPlayScreen);
        intent.putExtra(ThemeContentTable.THEME_CONTENT_ID, contentID);
        intent.putExtra(SentenceSetTable.SET_AUTO, getIntent().getIntExtra(SentenceSetTable.SET_AUTO, Default.ZERO));
        intent.putExtra(SentenceSetTable.SET_ID, getIntent().getIntExtra(SentenceSetTable.SET_ID, Default.ZERO));
        intent.putExtra("Theme_Id_No", getIntent().getStringExtra("Theme_Id_No"));
        if (free_Set.equals("true"))
            intent.putExtra(SentenceSetTable.SET_TITLE, getIntent().getStringExtra(ThemeTable.THEME_TITLE));
        else
            intent.putExtra(SentenceSetTable.SET_TITLE, getIntent().getStringExtra(SentenceSetTable.SET_TITLE));

        String[] play_mode_list = getResources().getStringArray(R.array.play_mode_list);

        if (playMode.equals(play_mode_list[0]))
            intent.putExtra(Default.PLAY_MODE, 0);
        else
            intent.putExtra(Default.PLAY_MODE, 1);

        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sentence, menu);
        optionMenu = menu.findItem(R.id.action_option);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_option:
                //Open setting fragment
                showActionSheet();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    Load play mode lisview
     */
    private void loadPlayModeListData() {
        playModeList = new ArrayList<HashMap<String, String>>();
        String[] play_mode_list = getResources().getStringArray(R.array.play_mode_list);

        if (theme_no.equals("Theme_1")) {
            if (free_Set.equals("true")) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put(Default.MODE_HASMAP_KEY, play_mode_list[0]);
                playModeList.add(map);
            } else {
                for (int i = 0; i < play_mode_list.length; i++) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put(Default.MODE_HASMAP_KEY, play_mode_list[i]);
                    playModeList.add(map);
                }
            }
        } else {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(Default.MODE_HASMAP_KEY, play_mode_list[1]);
            playModeList.add(map);
        }

        String[] from = new String[]{Default.MODE_HASMAP_KEY};
        int[] to = new int[]{R.id.setTitleTextView};

        SimpleAdapter simpleAdapter = new SimpleAdapter(SentenceSetDetailsActivity.this, playModeList, R.layout.set_list_item, from, to);
        playModeListView.setAdapter(simpleAdapter);
    }

    /*
    Define visibility based on purchase status
     */
    private void updateAdView() {
        //configure adview
        if (sharedPreferences.getBoolean(Default.IN_APP_PURCHASE, false)) {
            //in visible the adview
            adView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("sentence set details", "On Pause");
        //  updateLearningTime(Default.PAUSE_STATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("sentence set details", "On resume");
        //   updateLearningTime(Default.RESUME_STATE);
    }

    /*
    Update & Restore learning time based on activity state
    @param int state- Possible value is RESUME_STATE & PAUSE_STATE
     */
    private void updateLearningTime(int state) {
        if (state == Default.RESUME_STATE) {
            //start each tme session
            sharedPreferences.edit().putLong(Default.LEARNING_SESSION_KEY, System.currentTimeMillis()).commit();

        } else if (state == Default.PAUSE_STATE) {
//            long learningTime = System.currentTimeMillis() - sharedPreferences.getLong(Default.LEARNING_SESSION_KEY, System.currentTimeMillis());
//            SettingUtils.setStudyTime(sharedPreferences, SettingUtils.getStudyTime(this) + learningTime);
            SettingUtils.setStudyTime(this);
        }
    }

    @Override
    public void onDismiss(ActionSheet actionSheet, boolean isCancel) {

    }

    @Override
    public void onOtherButtonClick(ActionSheet actionSheet, int index) {
        //get Positon showActionSheet
        switch (index) {
            case 0:
                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            case 1:
                Intent i = new Intent(SentenceSetDetailsActivity.this, HomeActivity.class);
                i.putExtra("check", true);
                startActivity(i);
                break;
            default:
                break;
        }
    }

    private void showActionSheet() {

        ActionSheet.createBuilder(this, getSupportFragmentManager())
                .setCancelButtonTitle(R.string.action_sheet_cancel)
                .setOtherButtonTitles(actionSheetItems)
                .setCancelableOnTouchOutside(true)
                .setListener(this).show();
    }
}
