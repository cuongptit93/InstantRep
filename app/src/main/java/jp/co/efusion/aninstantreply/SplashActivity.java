package jp.co.efusion.aninstantreply;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

import java.util.ArrayList;
import java.util.List;

import jp.co.efusion.utility.Default;


public class SplashActivity extends ActionBarActivity {

    ViewPager viewPager_tutorial;
    ImageView imgView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Fabric.with(this, new Crashlytics());

        getSupportActionBar().hide();
        setContentView(R.layout.activity_splash);

        imgView = (ImageView) findViewById(R.id.splashImage);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                //setting tutorial viewPager
                //SharedPreferences lưu trạng thái.
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                //PREFERENCE_KEY_TUTORIAL_FINISHED: Tên tệp (key) lưu trạng thái. false: kiểu dữ liệu cần lưu
                boolean tutorial_finished = prefs.getBoolean(Default.PREFERENCE_KEY_TUTORIAL_FINISHED, false);

                if (!tutorial_finished) {
                    imgView.setVisibility(View.INVISIBLE);
                    viewPager_tutorial = (ViewPager) findViewById(R.id.viewpager_tutorial);
                    viewPager_tutorial.setVisibility(View.VISIBLE);
                    List<Integer> list_tutorial_data = new ArrayList<Integer>();
                    list_tutorial_data.add(R.drawable.tutorial1);
                    list_tutorial_data.add(R.drawable.tutorial2);
                    list_tutorial_data.add(R.drawable.tutorial3);
                    list_tutorial_data.add(R.drawable.tutorial4);
                    TutorialPagerAdapter pagerAdapter_tutorial = new TutorialPagerAdapter(getApplicationContext(), list_tutorial_data);
                    viewPager_tutorial.setAdapter(pagerAdapter_tutorial);
                    pagerAdapter_tutorial.setFinishedListener(new TutorialPagerListener() {
                        @Override
                        public void finished() {
                            viewPager_tutorial.setVisibility(View.INVISIBLE);
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            prefs.edit().putBoolean(Default.PREFERENCE_KEY_TUTORIAL_FINISHED, true).commit();
                            //   mDrawerLayout.setVisibility(View.VISIBLE);
                            finish();
                            Intent i = new Intent(SplashActivity.this, HomeActivity.class);
                            startActivity(i);
                        }
                    });
                } else {
                    //start main screen
                    Intent i = new Intent(SplashActivity.this, HomeActivity.class);
                    startActivity(i);
                }

            }
        }, Default.SPLASH_SCREEN_TIME);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
