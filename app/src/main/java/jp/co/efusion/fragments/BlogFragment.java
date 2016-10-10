package jp.co.efusion.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import jp.co.efusion.aninstantreply.R;
import jp.co.efusion.database.DatabaseHelper;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.SettingUtils;

/**
 * A simple {@link Fragment} subclass.
 */
public class BlogFragment extends Fragment {

    SharedPreferences sharedPreferences;
    private WebView webView;
    private Button refreshButton,backButton,nextButton;

    public BlogFragment() {
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
        View rootView=inflater.inflate(R.layout.fragment_blog, container, false);

        webView = (WebView) rootView.findViewById(R.id.webView);
        webView.setWebViewClient(new MyBrowser());
        webView.getSettings().setJavaScriptEnabled(true);

        refreshButton=(Button)rootView.findViewById(R.id.refreshButton);
        backButton=(Button)rootView.findViewById(R.id.backButton);
        nextButton=(Button)rootView.findViewById(R.id.nextButton);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadURL(webView.getUrl());
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check has back history
                if (webView.canGoBack()) {
                    webView.goBack();
                }
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //check has forward history
                if (webView.canGoForward()) {
                    webView.goForward();
                }
            }
        });

        //load help url
        loadURL(Default.BLOG_URL);

        return rootView;
    }
    /**
     *load specific url
     * @param url
     */
    private void loadURL(String url){
        webView.loadUrl(url);
    }

    /**
     *
     * @param w
     */
    private void updateNavigation(WebView w){
        backButton.setEnabled(w.canGoBack());
        nextButton.setEnabled(w.canGoForward());
        backButton.setAlpha((w.canGoBack()) ? 1.0f : 0.3f);
        nextButton.setAlpha((w.canGoForward()) ? 1.0f : 0.3f);
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.e("Blog Fragment", "On Pause");
        //updateLearningTime(Default.PAUSE_STATE);
    }
    @Override
    public void onResume(){
        super.onResume();
        Log.e("Blog Fragment", "On resume");
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

    private class MyBrowser extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.e("WebView", "onPageFinished");
            updateNavigation(view);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.e("WebView", "onPageStarted");
            updateNavigation(view);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            Log.e("WebView", "onLoadResource");
            updateNavigation(view);
        }



    }
}
