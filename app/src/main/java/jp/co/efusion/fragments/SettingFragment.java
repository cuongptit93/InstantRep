package jp.co.efusion.fragments;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import jp.co.efusion.MediaManager.SoundManager;
import jp.co.efusion.aninstantreply.R;
import jp.co.efusion.utility.CustomIOSDialog;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.IOSDialogListener;
import jp.co.efusion.utility.SettingUtils;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingFragment extends Fragment {

    //private static final int CHUNKPLAY_TIMER_POSITION = 4;
    //private static final int CHUNKPLAY_INTERVAL_POSITION = 6;
    //private static final int AUTOPLAY_INTERVAL_POSITION = 8;
    //private static final int CLEAR_SETTING_POSITION = 14;
    //private static final int AUDIO_SPEED_POSITION = 10;
    //private static final int AUDIO_VOLUME_POSITION = 12;

    private String[] arraySetting;

    boolean checkOpenAudioSetting = false;

    SoundManager mSoundManager;

    //position list SettingApp
    private int checkPositionSetting = 0, chunkplayTiemrPosition, chunkplayIntervalPosition, autoplayIntervalPosition, clearSettingPosition, audioVolumePosition, maxVolume, currentVolume;

    SharedPreferences sharedPreferences;

    //view declaration
    private ListView settingsListview;
    private SettingsAdapter settingsAdapter;
    private AudioManager audioManager = null;

    private CustomIOSDialog customIOSDialog;
    private  boolean isPlaySound;

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        View rootView = inflater.inflate(R.layout.fragment_setting, container, false);
        arraySetting = getResources().getStringArray(R.array.settingApp);

        //initial all fragment UIVIEW
        settingsListview = (ListView) rootView.findViewById(R.id.settingsListview);
        //adapter initialization
        settingsAdapter = new SettingsAdapter(getActivity());

        //set this adapter to settingsListview
        settingsListview.setAdapter(settingsAdapter);

        //set on item selecte listenser
        settingsListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedFromList = (settingsListview.getItemAtPosition(position).toString());
                if (position == clearSettingPosition) {
                    //show custom confirmation alert dialog
                    showCustomAlertDialog();
                } /*else if (position == audioSpeedPostion) {
                    showChoiceFloatValueDialog(position);
                }*/ else {
                    showChoiceDialogBox(position);
                }
            }
        });

        return rootView;
    }

    /**
     * show alert dialog to clear all setting
     */
    private void showCustomAlertDialog() {
        //show alert dialog for confirmation
        customIOSDialog = new CustomIOSDialog(getActivity());
        customIOSDialog.createConfirmationDialog(getResources().getString(R.string.settingclear_dialog_title), getResources().getString(R.string.settingclear_dialog_message));
        customIOSDialog.setIOSDialogListener(new IOSDialogListener() {
            @Override
            public void onCancel() {
            }

            @Override
            public void onOk() {
                //clear all share preference settings value && reload listview
//                sharedPreferences.edit().remove(Default.AUTO_PLAY_ENABLE).apply();
//                sharedPreferences.edit().remove(Default.CHUNK_PLAY_TIMER).apply();
//                sharedPreferences.edit().remove(Default.CHUNK_PLAY_INTERVAL).apply();
//                sharedPreferences.edit().remove(Default.AUTO_PLAY_INTERVAL).apply();

                //clear learning values of home screen
                sharedPreferences.edit().remove(Default.LEARNING_OBJECTIVE_KEY).apply();
                sharedPreferences.edit().remove(Default.LEARNING_INITIAL_DATE_KEY).apply();
                sharedPreferences.edit().remove(Default.QUESTION_TRIED_KEY).apply();
                sharedPreferences.edit().remove(Default.LEARNING_HOUR_KEY).apply();

                //reload listview
                //adapter initialization
                checkPositionSetting = 0;
                settingsAdapter = new SettingsAdapter(getActivity());
                //set this adapter to settingsListview
                settingsListview.setAdapter(settingsAdapter);
            }
        });
    }

    /**
     * show single choice Dialog Box
     *
     * @param position
     */
    private void showChoiceDialogBox(int position) {
        String title = "", key = "";
        int selected = -1, default_index = 0;
        int[] list = {};
        if (position == chunkplayTiemrPosition) {
            key = Default.CHUNK_PLAY_TIMER;
            list = Default.CHUNK_PLAY_TIMER_VALUES;
            default_index = Default.CHUNK_PLAY_TIMER_VALUES_DEFAULT_INDEX;
            title = getResources().getString(R.string.chunkplay_timer_title);
        } else if (position == chunkplayIntervalPosition) {
            key = Default.CHUNK_PLAY_INTERVAL;
            list = Default.CHUNK_PLAY_INTERVAL_VALUES;
            default_index = Default.CHUNK_PLAY_INTERVAL_VALUES_DEFAULT_INDEX;
            title = getResources().getString(R.string.chunkplay_interval_title);
        } else if (position == autoplayIntervalPosition) {
            key = Default.AUTO_PLAY_INTERVAL;
            list = Default.AUTO_PLAY_INTERVAL_VALUES;
            default_index = Default.AUTO_PLAY_INTERVAL_VALUES_DEFAULT_INDEX;
            title = getResources().getString(R.string.autoplay_interval_title);
        }

        final String KEY_CONSTANT = key;
        final int[] LIST_CONSTANT = list;
        final List<String> listItems = new ArrayList<String>();
        for (int i = 0; i < list.length; i++) {
            listItems.add(list[i] + getResources().getString(R.string.settings_time_unit));
        }
        selected = listItems.indexOf(sharedPreferences.getInt(KEY_CONSTANT, LIST_CONSTANT[default_index]) + getResources().getString(R.string.settings_time_unit));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        // does not select anything
        final CharSequence[] choiceList = listItems.toArray(new CharSequence[listItems.size()]);

        builder.setSingleChoiceItems(choiceList, selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //update share preference
                sharedPreferences.edit().putInt(KEY_CONSTANT, LIST_CONSTANT[which]).commit();
                //reload listview
                //adapter initializatio
                checkPositionSetting = 0; //set checkPositionSetting to zero and reload list view.
                settingsAdapter = new SettingsAdapter(getActivity());
                //set this adapter to settingsListview
                settingsListview.setAdapter(settingsAdapter);

                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    /*private void showChoiceFloatValueDialog(int position) {
        String title = "", key = "";
        int selected = -1, default_index = 0;
        float[] list = {};
        if (position == audioSpeedPostion) {
            key = Default.AUDIO_SPEED_SETTING;
            list = Default.AUDIO_SPEED_SETTING_VALUES;
            title = getResources().getString(R.string.audio_speed_setting);
        }

        final String KEY_CONSTANT = key;
        final float[] LIST_CONSTANT = list;

        final List<String> listItems = new ArrayList<String>();
        for (int i = 0; i < list.length; i++) {
            listItems.add(String.valueOf(list[i]));
        }
        selected = listItems.indexOf(String.valueOf(sharedPreferences.getFloat(KEY_CONSTANT, LIST_CONSTANT[default_index])));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);

        // does not select anything
        final CharSequence[] choiceList = listItems.toArray(new CharSequence[listItems.size()]);

        builder.setSingleChoiceItems(choiceList, selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //update share preference
                sharedPreferences.edit().putFloat(KEY_CONSTANT, LIST_CONSTANT[which]).commit();
                //reload listview
                //adapter initialization
                checkPositionSetting = 0; //set value checkPositionSetting to zero and reload listview
                settingsAdapter = new SettingsAdapter(getActivity());
                //set this adapter to settingsListview
                settingsListview.setAdapter(settingsAdapter);

                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }*/

    private class SettingsAdapter extends BaseAdapter {
        Context mContext;
        LayoutInflater mInflater;

        public SettingsAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            // return mVideo.size();
            return 17;
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public boolean isEnabled(int position) {
            int[] listPositon = {chunkplayTiemrPosition, chunkplayIntervalPosition, autoplayIntervalPosition, clearSettingPosition, audioVolumePosition};
            for (int i = 0; i < listPositon.length; i++) {
                if (position == listPositon[i]) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(final int position, View view, ViewGroup arg2) {
            final ViewHolder vh;
            final float scale = getResources().getDisplayMetrics().density;
            if (view == null) {
                vh = new ViewHolder();
                view = mInflater.inflate(R.layout.setting_list_each_item, null);
                vh.itemLayout = (RelativeLayout) view.findViewById(R.id.itemLayout);
                vh.topLayout = (RelativeLayout) view.findViewById(R.id.topLayout);
                vh.settingsTitleTextView = (TextView) view.findViewById(R.id.settingsTitleTextView);
                vh.settingsValueTextView = (TextView) view.findViewById(R.id.settingsValueTextView);
                vh.navigationImageView = (ImageView) view.findViewById(R.id.navigationImageView);
                vh.autoPlaySwitch = (Switch) view.findViewById(R.id.autoPlaySwitch);
                vh.seekBar = (SeekBar) view.findViewById(R.id.seekBar);
                view.setTag(vh);
            } else {
                vh = (ViewHolder) view.getTag();
            }

            //load list setting////
            if (checkPositionSetting < arraySetting.length) {

                ///position disable///
                if (arraySetting[checkPositionSetting].equals(getString(R.string.disable))) {
                    vh.itemLayout.setVisibility(View.INVISIBLE);
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) vh.itemLayout.getLayoutParams();
                    layoutParams.height = (int) (30 * scale + 0.5f);
                    vh.itemLayout.setLayoutParams(layoutParams);
                    vh.itemLayout.requestLayout();
                }

                //auto play
                if (arraySetting[checkPositionSetting].equals(getString(R.string.autoplay_title))) {
                    vh.settingsTitleTextView.setText(getResources().getString(R.string.autoplay_title));
                    vh.autoPlaySwitch.setVisibility(View.VISIBLE);
                    //set switch on off based on auto play status
                    vh.autoPlaySwitch.setChecked(sharedPreferences.getBoolean(Default.AUTO_PLAY_ENABLE, Default.AUTO_PLAY_ENABLE_DEFAULT));
                    vh.autoPlaySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            sharedPreferences.edit().putBoolean(Default.AUTO_PLAY_ENABLE, isChecked).commit();
                        }
                    });
                }

                //auto_next_sentence
                if (arraySetting[checkPositionSetting].equals(getString(R.string.auto_next_sentence))) {
                    vh.settingsTitleTextView.setText(getResources().getString(R.string.auto_next_sentence));
                    vh.autoPlaySwitch.setVisibility(View.VISIBLE);
                    vh.autoPlaySwitch.setChecked(sharedPreferences.getBoolean(Default.AUTO_NEXT_SENTENCE, Default.AUTO_NEXT_SENTENCE_ENABLE_DEFAULT));
                    vh.autoPlaySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            sharedPreferences.edit().putBoolean(Default.AUTO_NEXT_SENTENCE, isChecked).commit();
                        }
                    });
                }

                //Title chunk setting
                if (arraySetting[checkPositionSetting].equals(getString(R.string.chunkplay_header_title))) {
                    vh.itemLayout.setVisibility(View.VISIBLE);
                    vh.itemLayout.setPadding((int) (10 * scale + 0.5f), (int) (5 * scale + 0.5f), (int) (10 * scale + 0.5f), 0);
                    vh.itemLayout.setGravity(Gravity.BOTTOM);
                    vh.itemLayout.setBackgroundColor(Color.TRANSPARENT);
                    vh.settingsTitleTextView.setText(getResources().getString(R.string.chunkplay_header_title));
                    vh.settingsTitleTextView.setPadding(0, (int) (20 * scale + 0.5f), 0, 0);
                    vh.settingsTitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                }

                //ChunkPlay Timer
                if (arraySetting[checkPositionSetting].equals(getString(R.string.chunkplay_timer_title))) {
                    vh.settingsTitleTextView.setText(getResources().getString(R.string.chunkplay_timer_title));
                    vh.settingsValueTextView.setVisibility(View.VISIBLE);
                    vh.settingsValueTextView.setText(sharedPreferences.getInt(Default.CHUNK_PLAY_TIMER, Default.CHUNK_PLAY_TIMER_VALUES[Default.CHUNK_PLAY_TIMER_VALUES_DEFAULT_INDEX])
                            + getResources().getString(R.string.settings_time_unit));
                    vh.navigationImageView.setVisibility(View.VISIBLE);
                    chunkplayTiemrPosition = position;
                }


                if (arraySetting[checkPositionSetting].equals(getString(R.string.chunkplay_interval_title))) {
                    vh.settingsTitleTextView.setText(getResources().getString(R.string.chunkplay_interval_title));
                    vh.settingsValueTextView.setVisibility(View.VISIBLE);
                    vh.settingsValueTextView.setText(sharedPreferences.getInt(Default.CHUNK_PLAY_INTERVAL, Default.CHUNK_PLAY_INTERVAL_VALUES[Default.CHUNK_PLAY_INTERVAL_VALUES_DEFAULT_INDEX])
                            + getResources().getString(R.string.settings_time_unit));
                    vh.navigationImageView.setVisibility(View.VISIBLE);
                    chunkplayIntervalPosition = position;
                }

                if (arraySetting[checkPositionSetting].equals(getString(R.string.autoplay_interval_title))) {
                    vh.settingsTitleTextView.setText(getResources().getString(R.string.autoplay_interval_title));
                    vh.settingsValueTextView.setVisibility(View.VISIBLE);
                    vh.settingsValueTextView.setText(sharedPreferences.getInt(Default.AUTO_PLAY_INTERVAL, Default.AUTO_PLAY_INTERVAL_VALUES[Default.AUTO_PLAY_INTERVAL_VALUES_DEFAULT_INDEX])
                            + getResources().getString(R.string.settings_time_unit));
                    vh.navigationImageView.setVisibility(View.VISIBLE);
                    autoplayIntervalPosition = position;
                }

                /*if (arraySetting[checkPositionSetting].equals(getString(R.string.audio_speed_setting))) {
                    vh.settingsTitleTextView.setText(getResources().getString(R.string.audio_speed_setting));
                    vh.settingsValueTextView.setVisibility(View.VISIBLE);
                    vh.settingsValueTextView.setText(String.valueOf(SettingUtils.getAudioSpeed(sharedPreferences)));
                    vh.navigationImageView.setVisibility(View.VISIBLE);
                    audioSpeedPostion = position;
                }*/

                if (arraySetting[checkPositionSetting].equals(getString(R.string.audio_volumn_setting))) {
                    vh.settingsTitleTextView.setText(getResources().getString(R.string.audio_volumn_setting));
                    vh.settingsValueTextView.setVisibility(View.VISIBLE);
                    vh.navigationImageView.setVisibility(View.INVISIBLE);
                    vh.seekBar.setVisibility(View.VISIBLE);

                    audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    vh.seekBar.setMax(maxVolume);

                    if(currentVolume == maxVolume){
                        vh.settingsValueTextView.setText(String.valueOf(maxVolume));
                        vh.seekBar.setProgress(maxVolume);
                    }
                    else{
                        vh.settingsValueTextView.setText(String.valueOf(currentVolume));
                        vh.seekBar.setProgress(currentVolume);
                    }

                    vh.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                                    progress, 0);
                            vh.settingsValueTextView.setText(String.valueOf(progress));
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {

                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                        }
                    });
                    audioVolumePosition = position;
                }

                if (arraySetting[checkPositionSetting].equals(getString(R.string.speed_setting))) {
                    vh.settingsTitleTextView.setText(getResources().getString(R.string.speed_setting));
                    vh.settingsValueTextView.setVisibility(View.VISIBLE);
                    vh.navigationImageView.setVisibility(View.INVISIBLE);
                    vh.seekBar.setVisibility(View.VISIBLE);
                    vh.seekBar.setMax(20);
                    vh.settingsValueTextView.setText(String.valueOf((float)sharedPreferences.getInt(Default.SPEED_SETTING, Default.DEFAULT_SPEED_SETTING)/10));
                    vh.seekBar.setProgress(sharedPreferences.getInt(Default.SPEED_SETTING, Default.DEFAULT_SPEED_SETTING));
                    vh.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if(progress == 0){
                                sharedPreferences.edit().putInt(Default.SPEED_SETTING, 1).commit();
                                vh.settingsValueTextView.setText("0");
                            }
                            else{
                                sharedPreferences.edit().putInt(Default.SPEED_SETTING, progress).commit();
                                vh.settingsValueTextView.setText(String.valueOf((float)sharedPreferences.getInt(Default.SPEED_SETTING, Default.DEFAULT_SPEED_SETTING)/10));
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                            if(!sharedPreferences.getString(Default.PATH_AUDIO_SPEED_SETTING, "").equals("")){
                                if(checkOpenAudioSetting){
                                    mSoundManager.releaseAudio();
                                    checkOpenAudioSetting = false;
                                    isPlaySound = false;
                                }
                            }
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            if(!sharedPreferences.getString(Default.PATH_AUDIO_SPEED_SETTING, "").equals("")){
                                mSoundManager = new SoundManager(sharedPreferences.getString(Default.PATH_AUDIO_SPEED_SETTING, ""));
                                mSoundManager.playAudio((float) sharedPreferences.getInt(Default.SPEED_SETTING, Default.DEFAULT_SPEED_SETTING)/10);
                                checkOpenAudioSetting = true;
                                isPlaySound = true;
                            }
                        }
                    });
                }

                if (arraySetting[checkPositionSetting].equals(getString(R.string.clear_settings_title))) {
                    vh.settingsTitleTextView.setText(getResources().getString(R.string.clear_settings_title));
                    vh.settingsTitleTextView.setTextColor(getResources().getColor(R.color.blue_color));
                    clearSettingPosition = position;
                }

                if (arraySetting[checkPositionSetting].equals(getString(R.string.version_title))) {
                    vh.settingsTitleTextView.setText(getResources().getString(R.string.version_title));
                    vh.settingsValueTextView.setVisibility(View.VISIBLE);
                    vh.settingsValueTextView.setText(getResources().getString(R.string.version_value));
                    vh.navigationImageView.setVisibility(View.INVISIBLE);
                }

                checkPositionSetting++;
            }

            return view;
        }

    }

    class ViewHolder {
        RelativeLayout itemLayout, topLayout;
        TextView settingsTitleTextView, settingsValueTextView;
        ImageView navigationImageView;
        Switch autoPlaySwitch;
        SeekBar seekBar;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("Setting Fragment", "On Pause");
        if(isPlaySound){
            mSoundManager.pauseAudio();
        }
        //updateLearningTime(Default.PAUSE_STATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("Setting Fragment", "On resume");
        //updateLearningTime(Default.RESUME_STATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(isPlaySound){
            mSoundManager.releaseAudio();
            isPlaySound = false;
        }
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
//            SettingUtils.setStudyTime(sharedPreferences, SettingUtils.getStudyTime(getActivity().getApplicationContext()) + learningTime);
            SettingUtils.setStudyTime(getActivity().getApplicationContext());
        }
    }

}
