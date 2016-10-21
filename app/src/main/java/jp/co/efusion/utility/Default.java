package jp.co.efusion.utility;

import android.os.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by xor2 on 12/6/15.
 */
public class Default {

    //in app purchase
    public static final String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtrU+eB8WU9mSu27snVzDRIZjd3z7SRisE5IH82sG1NAmv1cbk6fHpPYzeMVkuOwJkIhow5ua4kcnSGmTeWGZhpU8fM7QdJ005J217HclhD0m5uz4y0P6jo9Q3KYnoMKSlJlLVGyC3eZ/BoB/Np0iTw4PRu9N+zIRcqDl9zOdQpahNH+NvpRfUlHYZhP1pjrZ9mnZbXnVJOxTOip1OLJdLzgbdY2w87GCmx/dwa5PWtfRrsnZzFhlwRRwMka9R5+ebbMlEKxnWLqKXZ5fHgjLfWO5MV2KzM4N50ZzyhfB9jEvVQh2KtCFCLmevgQ3wbxGeBgZ9Ea0XBaJntT8W4Dr3wIDAQAB";
    public static final String SKU_THEME1_PACKAGE1 = "jp.co.pdc.aninstantreply.theme1.package2";
    public static final String SKU_THEME1_PACKAGE2 = "jp.co.pdc.aninstantreply.theme1.package3";
    public static final String SKU_THEME1_PACKAGE3 = "jp.co.pdc.aninstantreply.theme1.package4";
    public static final String SKU_THEME1_PACKAGE4 = "jp.co.pdc.aninstantreply.theme1.package5";
    public static final String SKU_THEME1_PACKAGE5 = "jp.co.pdc.aninstantreply.theme1.package6";
    public static final String SKU_THEME1_PACKAGE1_TO_2 = "jp.co.pdc.aninstantreply.theme1.package7";
    public static final String SKU_THEME1_PACKAGE3_TO_5 = "jp.co.pdc.aninstantreply.theme1.package8";
    public static final String SKU_THEME1_PACKAGE_ALL = "jp.co.pdc.aninstantreply.theme1.package9";

    public static final String SKU_THEME2_PACKAGE1 = "jp.co.pdc.aninstantreply.theme2.package11";
    public static final String SKU_THEME2_PACKAGE2 = "jp.co.pdc.aninstantreply.theme2.package12";
    public static final String SKU_THEME2_PACKAGE3 = "jp.co.pdc.aninstantreply.theme2.package13";
    public static final String SKU_THEME2_PACKAGE4 = "jp.co.pdc.aninstantreply.theme2.package14";
    public static final String SKU_THEME2_PACKAGE5 = "jp.co.pdc.aninstantreply.theme2.package15";
    public static final String SKU_THEME2_PACKAGE6 = "jp.co.pdc.aninstantreply.theme2.package16";
    public static final String SKU_THEME2_PACKAGE7 = "jp.co.pdc.aninstantreply.theme2.package17";
    public static final String SKU_THEME2_PACKAGE8 = "jp.co.pdc.aninstantreply.theme2.package18";
    public static final String SKU_THEME2_PACKAGE1_TO_4 = "jp.co.pdc.aninstantreply.theme2.package19";
    public static final String SKU_THEME2_PACKAGE5_TO_8 = "jp.co.pdc.aninstantreply.theme2.package20";
    public static final String SKU_THEME2_PACKAGE_ALL = "jp.co.pdc.aninstantreply.theme2.package21";

    public static final boolean SKU_TYPE_CONSUMABLE = false;
    public static final boolean TEST_PURCHASE = false; //テスト購入を有効
    //end in app purchase

    public static final long SPLASH_SCREEN_TIME=1000; //IN MILI SECENDS

    public static final int RESUME_STATE=0;

    public static final int PAUSE_STATE=1;


    //    KEY FOR SHARE PREFERENCE VALUE
    public static final String SHARE_PREFERENCE_NAME="air_share_preference";

    public static final String IN_APP_PURCHASE="in_app_purchase";

    public static final String LEARNING_OBJECTIVE_KEY="learning_objective_key";

    public static final String DEFAULT_COUNTDOWN_KEY = "default_countdown_key";

    public static final String TARGET_CONTENT = "target_content";

    public static final String TARGET_TITLE = "target_title";

    public static final String TARGET_DAY_END = "target_day_end";

    public static final String LEARNING_INITIAL_DATE_KEY="learning_initial_date_key";

    public static final String LEARNING_HOUR_KEY="learning_hours_key";

    public static final String LEARNING_SESSION_KEY="learning_session_key";

    public static final String QUESTION_TRIED_KEY="question_tried_key";

    public static final String LOAD_PRICE_KEY="_load_price_key";

    public static final long STATISTICS_DEFAULT_VALUE=0;

    public static final int STATE_LOAD_PRICE= 0;

    public static final int STATE_PURCHASE= 1;

    public static final int STATE_DOWNLOAD= 2;

    public static final int STATE_UPDATE= 3;

    public static final int STATE_READY_TO_USE= 4;

    public static final String THEME1_ID="theme-1";

    public static final String THEME2_ID="theme-2";

    public static final List THEME1_SET1= new ArrayList<>(Arrays.asList("2","3"));

    public static final List THEME1_SET2= new ArrayList<>(Arrays.asList("4","5","6"));

    public static final List THEME1_ALL= new ArrayList<>(Arrays.asList("2","3","4","5","6"));

    public static final List THEME2_SET1= new ArrayList<>(Arrays.asList("11","12","13","14"));

    public static final List THEME2_SET2= new ArrayList<>(Arrays.asList("15","16","17","18"));

    public static final List THEME2_ALL= new ArrayList<>(Arrays.asList("11","12","13","14","15","16","17","18"));

    //set dummy id
    public static final int THEME1_SET1_ID=-1;
    public static final int THEME1_SET2_ID=-2;
    public static final int THEME1_ALL_ID=-3;
    public static final int THEME2_SET1_ID=-4;
    public static final int THEME2_SET2_ID=-5;
    public static final int THEME2_ALL_ID=-6;

    //directory of saved prescription image file
    public static final String RESOURCES_BASE_DIRECTORY= Environment.getExternalStorageDirectory() + "/.AnInstantReply/";

    //base url of resouces
//    public static final String RESOURCES_BASE_URL="http://xorgeek.com/medicinbox/AnInstantReply/";

    public static final String RESOURCES_BASE_URL="https://s3-ap-northeast-1.amazonaws.com/aninstantreply/audiofiles/";

    //resouces name
    public static final String RESOURCES_PREFIX="resources_content_";

    public static final String RESOURCES_SUFIX=".zip";

    public static final int ZERO=0;

    //request code
    public static final int HIDDENABLE_REQUEST_CODE=0;

    //map key for play mode has map
    public static final String MODE_HASMAP_KEY="mode_has_map_key";

    public static final String THEME_ID_EXTRA = "Theme_Id_No";

    public static final String MENU_POSITION_EXTRA = "menu_position_extra";

    //play mode
    public static final String PLAY_MODE="play_mode";

    public static final String FAVORITE_SET="favorite_set";

    public static final int CHUNK_PLAY_MODE=0;

    public static final int NORMAL_PLAY_MODE=1;

    //Flag go over sentence set screen
    public static final String GO_TO_PLAY_SCREEN = "go_to_play_screen";

    public static final String IS_SHUFFLE_MODE = "is_shuffle_mode";

    //Flag for free set
    public static final String FREE_SET="free_set";

    //flag for start point && array of sentence
    public static final String START_POINT ="start_point";

    public static final String PLAYABLE_SENTENCE_LIST="playable_sentence_list";

    //key for settings data
    public static final String AUTO_PLAY_ENABLE="auto_play_enable";
    public static final String AUTO_NEXT_SENTENCE="auto_next_sentence";

    public static final String ENABLE_RATING_POPUP = "enable_rating_popup";

    public static final Boolean AUTO_PLAY_ENABLE_DEFAULT=false;
    public static final Boolean AUTO_NEXT_SENTENCE_ENABLE_DEFAULT=false;

    public static final Boolean DEFAULT_TEXT_COUNTDOWN = false;

    public static final String AUTO_PLAY_INTERVAL="auto_play_interval";

    public static final int[] AUTO_PLAY_INTERVAL_VALUES={0,2,3,5};

    public static final int AUTO_PLAY_INTERVAL_VALUES_DEFAULT_INDEX=2;

    public static final String AUDIO_SPEED_SETTING = "audio_speed_setting";

    public static final float[] AUDIO_SPEED_SETTING_VALUES = {1f, 1.2f, 1.5f, 2};

    /*Audio Speed test begin */
    public static final String SPEED_SETTING = "speed_setting";
    public static final int DEFAULT_SPEED_SETTING = 10;
    /*Audio Speed test end */

    /*Audio Speed setting test begin */
    public static final String PATH_AUDIO_SPEED_SETTING = "audio_speed_setting";
    /*Audio Speed setting test end */

    public static final String CHUNK_PLAY_INTERVAL="chunk_play_interval";

    public static final int[] CHUNK_PLAY_INTERVAL_VALUES={3,5,8,10};

    public static final int CHUNK_PLAY_INTERVAL_VALUES_DEFAULT_INDEX=0;

    public static final String CHUNK_PLAY_TIMER="chunk_play_timer";

    public static final int[] CHUNK_PLAY_TIMER_VALUES={1,2,3};

    public static final int CHUNK_PLAY_TIMER_VALUES_DEFAULT_INDEX=0;

    //play state
    public static final int QUESTION_STATE=1;

    public static final int ANSWER_STATE=2;

    public static final int SENTENCE_QUESTION_STATE=3;

    public static final int SENTENCE_ANSWER_STATE=4;

    public static final int AUDIO_FILE_NAME_INDEX=12;

    //URL of help, blog
    public static final String HELP_URL="http://www.pdc.co.jp/an_instant_reply/help";

    public static final String BLOG_URL="http://www.pdc.co.jp/an_instant_reply";

    //email for contact us
    public static final String EMAIL_ADDRESS="an_instant_reply@pdc.jp";

    public static String PREFERENCE_KEY_TUTORIAL_FINISHED = "PREFERENCE_KEY_TUTORIAL_FINISHED";


    public static final String XML_URL="https://s3-ap-northeast-1.amazonaws.com/aninstantreply/update_status.xml";

    public static final String XML_DIRECTORY= Environment.getExternalStorageDirectory() + "/.AnInstantReply/XML/";

    public static final String XML_FILE_NAME= "update_status.xml";

    public static final String XML_FILE_PATH= XML_DIRECTORY+XML_FILE_NAME;

    //Define all purchase sentences
    public static final int ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID = -1;
    public static final int ALL_PURCHASE_SENTENCE_SET_ID = -1;

}