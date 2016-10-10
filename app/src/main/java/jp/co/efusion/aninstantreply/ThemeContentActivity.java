package jp.co.efusion.aninstantreply;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baoyz.actionsheet.ActionSheet;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jp.co.efusion.database.DatabaseHelper;
import jp.co.efusion.database.ThemeContentTable;
import jp.co.efusion.database.ThemeTable;
import jp.co.efusion.fragments.SettingFragment;
import jp.co.efusion.listhelper.Log;
import jp.co.efusion.listhelper.ThemeContentListAdapter;
import jp.co.efusion.listhelper.ThreadManager;
import jp.co.efusion.utility.ConnectionDetector;
import jp.co.efusion.utility.CustomButtonListener;
import jp.co.efusion.utility.CustomIOSDialog;
import jp.co.efusion.utility.Decompress;
import jp.co.efusion.utility.Default;
import jp.co.efusion.utility.SettingUtils;


//class Package
//{
//    public String PackageKey;
//    public String PackageDate;
//
//}


public class ThemeContentActivity extends ActionBarActivity implements CustomButtonListener, InAppBillingSupporterListener, ActionSheet.ActionSheetListener {
    private static final String TAG = ThemeContentActivity.class.getSimpleName();

    DatabaseHelper databaseHelper;
    volatile Cursor cursor;
    String[] columns;
    SharedPreferences sharedPreferences;

    private String themeID;
    private ListView themeContentListView;
    private View footerView;
    private Button purchaseSet1Button, purchaseSet2Button, purchaseAllButton;
    private ThemeContentListAdapter themeListAdapter;
    TextView purchaseSet1TV, purchaseSet2TV, purchaseAllTV;
    RelativeLayout RLset1, RLset2, RLbuyALL;

    int playMode;
    private String[] actionSheetItems;

    //for download content
    // Internet detector
    ConnectionDetector connectionDetector;

    // Progress Dialog
    private ProgressDialog pDialog;

    // Progress dialog type (0 - for Horizontal progress bar)
    public static final int progress_bar_type = 0;

    // File url to download
    DownloadFileFromURL downloadFileFromURLTask;

    //declare admob adview
    private AdView adView;

    private static final int REQUEST_WRITE_STORAGE = 112;

    NodeList nodesList;
    Map<String, String> xmlMapLocal, xmlMapServer;
    Document docLocal;
    boolean updateStatus = false;

    //Flag to indicator have purchase content or not
    boolean havePurchaseContent = false;
    Handler uiHandler;
    MenuItem optionMenu;

    //custom alert dialog
    CustomIOSDialog customIOSDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.theme_color_green)));
        setContentView(R.layout.activity_theme_content);
        android.util.Log.e("Oncreate", "themeContent Activity");
        //show home back button
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //add title from theme title
        themeID = getIntent().getStringExtra(ThemeTable.THEME_ID);

        //initialize database helper
        databaseHelper = new DatabaseHelper(this);
        //open database
        databaseHelper.openDatabase();

        // Internet detector
        connectionDetector = new ConnectionDetector(getApplicationContext());

        //initialize sharepreference
        sharedPreferences = getSharedPreferences(Default.SHARE_PREFERENCE_NAME,
                Context.MODE_PRIVATE);

        //check for resources directory
        if (!new File(Default.RESOURCES_BASE_DIRECTORY).exists()) {
            new File(Default.RESOURCES_BASE_DIRECTORY).mkdirs();
        }

        //initialize view
        themeContentListView = (ListView) findViewById(R.id.themeContentListView);
        footerView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.content_list_footer, null, false);
        purchaseSet1Button = (Button) footerView.findViewById(R.id.purchaseSet1Button);
        purchaseSet2Button = (Button) footerView.findViewById(R.id.purchaseSet2Button);
        purchaseAllButton = (Button) footerView.findViewById(R.id.purchaseAllButton);
        purchaseSet1TV = (TextView) footerView.findViewById(R.id.purchaseSet1TextView);
        purchaseSet2TV = (TextView) footerView.findViewById(R.id.purchaseSet2TextView);
        purchaseAllTV = (TextView) footerView.findViewById(R.id.purchaseAllTextView);

        RLset1 = (RelativeLayout) footerView.findViewById(R.id.layout_set1);
        RLset2 = (RelativeLayout) footerView.findViewById(R.id.layout_set2);
        RLbuyALL = (RelativeLayout) footerView.findViewById(R.id.layout_buyAll);

        //load item Home And Setting to Action sheet.
        final String[] itemAction = getResources().getStringArray(R.array.action_sheet_initial_item);
        actionSheetItems = Arrays.copyOf(itemAction, 2);

        //update footer button title
        if (themeID.matches(Default.THEME1_ID)) {
            purchaseSet1Button.setText(getResources().getString(R.string.state_load_price_text));
            purchaseSet2Button.setText(getResources().getString(R.string.state_load_price_text));
            purchaseAllButton.setText(getResources().getString(R.string.state_load_price_text));
            purchaseSet1TV.setText(getResources().getString(R.string.theme_1_set_1_purchase_text));
            purchaseSet2TV.setText(getResources().getString(R.string.theme_1_set_2_purchase_text));
            purchaseAllTV.setText(getResources().getString(R.string.purchase_all_text));
            getSupportActionBar().setTitle("ALL IN ONE Basic");

        } else if (themeID.matches(Default.THEME2_ID)) {
            purchaseSet1Button.setText(getResources().getString(R.string.state_load_price_text));
            purchaseSet2Button.setText(getResources().getString(R.string.state_load_price_text));
            purchaseAllButton.setText(getResources().getString(R.string.state_load_price_text));
            purchaseSet1TV.setText(getResources().getString(R.string.theme_2_set_1_purchase_text));
            purchaseSet2TV.setText(getResources().getString(R.string.theme_2_set_2_purchase_text));
            purchaseAllTV.setText(getResources().getString(R.string.purchase_all_text));
            getSupportActionBar().setTitle("ｳｨﾆﾝｸﾞﾌｨﾆｯｼｭ");

        }

        uiHandler = new Handler(getMainLooper());

        //load theme data
        loadThemeData();

        //configure footer view
        // configureFooterView();

        //configure the adView Here
        adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("83379D0B53764804B8F94258363B28E2")
                .build();
        adView.loadAd(adRequest);
        //update add view
        updateAdView();

        //in app purchase listener
        App.inapp_billing_supporter.setPurchasedListener(this);

        //load price from in app puchase server
        loadPriceFromInAppServer();
        //load sku price from google play server
        App.inapp_billing_supporter.setSKUDetailsLoaderListener(new SKUDetailsLoaderListener() {
            @Override
            public void onSKUDetailsLoad() {

                android.util.Log.e("onSKUDetailsLoad", "finish price loading");
                //load price from in app puchase server
                loadPriceFromInAppServer();
            }
        });
        App.inapp_billing_supporter.loadSKUDetails();


        themeContentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (cursor == null)
                    return;

                cursor.moveToPosition(position);
                if (cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID)) == Default.ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID) {
                    if (!havePurchaseContent) {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                customIOSDialog = new CustomIOSDialog(ThemeContentActivity.this);
                                customIOSDialog.createAlertDialog(null, getResources().getString(R.string.no_purchase_content_dialog_message));
                            }
                        });
                        return;
                    }
                }

                if (cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_STATE)) == Default.STATE_READY_TO_USE) {
                    //check for free or not
                    Intent intent = new Intent(ThemeContentActivity.this, SentenceSetActivity.class);
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ThemeContentActivity.this);
                    SharedPreferences.Editor editor = preferences.edit();
                    if (position == 0) {
                        intent.putExtra(Default.FREE_SET, true);

                        if (themeID.matches(Default.THEME1_ID)) {
                            playMode = 1;
                            intent.putExtra(ThemeTable.THEME_TITLE, "ALL IN ONE Basic");
                        } else if (themeID.matches(Default.THEME2_ID)) {
                            intent.putExtra(ThemeTable.THEME_TITLE, "ｳｨﾆﾝｸﾞﾌｨﾆｯｼｭ");
                            playMode = 0;
                        }


                        editor.putString("Free_Set", "true");
                        editor.apply();
                    } else {
                        intent.putExtra(Default.FREE_SET, false);
                        intent.putExtra(ThemeTable.THEME_TITLE, getIntent().getStringExtra(ThemeTable.THEME_TITLE));
                        editor.putString("Free_Set", "false");
                        editor.apply();
                    }

                    if (themeID.matches(Default.THEME1_ID)) {
                        editor.putString("Theme_No", "Theme_1");
                        editor.apply();
                    } else {
                        editor.putString("Theme_No", "Theme_2");
                        editor.apply();
                    }

                    intent.putExtra("Theme_Id_No", themeID);
                    intent.putExtra(ThemeContentTable.THEME_CONTENT_ID, cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID)));
                    startActivity(intent);

                }

            }
        });
        //check permission
        boolean hasPermission = (ContextCompat.checkSelfPermission(ThemeContentActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(ThemeContentActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
            return;
        }

        //copy free set resources
        copyFreeSetResources();

//        //test purchase
//        //update content state
//        ContentValues values = new ContentValues();
//        values.put(ThemeContentTable.THEME_CONTENT_STATE, Default.STATE_DOWNLOAD);
//        if (databaseHelper.updateSQL(ThemeContentTable.TABLE_NAME, values, ThemeContentTable.THEME_CONTENT_ID + " = '" + 11 + "'", null)) {
//            //reload theme data
//            reloadThemeData();
//            sharedPreferences.edit().putBoolean(Default.IN_APP_PURCHASE, true).commit();
//            updateAdView();
//        }


        File folder = new File(Default.XML_DIRECTORY);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
            try {
                createXml();
            } catch (IOException e) {
            }
        }
        // Do something on success
        else {

            File xmlFile = new File(Default.XML_FILE_PATH);
            if (!xmlFile.exists()) {
                try {
                    createXml();
                } catch (IOException e) {
                }
            }
            // Do something else on failure
        }

        parseXML();

        // Execute DownloadXML AsyncTask
        new DownloadXML().execute((Default.XML_URL));

    }


    private void createXml() throws IOException {

        if (checkXMLExist()) {
            //database exist try to update if new version available
            //onUpgrade(myDataBase, DATABASE_VERSION_old, DATABASE_VERSION);
        } else {
            //database file not exist, So copy this from application asset folder
            // this.getReadableDatabase();
            try {

                OutputStream myOutput = new FileOutputStream(Default.XML_FILE_PATH);
                InputStream myInput = getApplicationContext().getAssets().open(Default.XML_FILE_NAME);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = myInput.read(buffer)) > 0) {
                    myOutput.write(buffer, 0, length);
                }
                myInput.close();
                myOutput.flush();
                myOutput.close();

            } catch (IOException e) {
                throw new Error("Error copying xml");
            }
        }
    }

    private boolean checkXMLExist() {
        return new File(Default.XML_FILE_PATH).exists();
    }


    private void parseXML() {
        try {

            xmlMapLocal = new HashMap<String, String>();
            InputStream myInput = new FileInputStream(Default.XML_FILE_PATH);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            docLocal = builder.parse(myInput);

            nodesList = docLocal.getElementsByTagName("Package");
            for (int i = 0; i < nodesList.getLength(); i++) {
                Element element = (Element) nodesList.item(i);
                NodeList key = element.getElementsByTagName("PackageKey");
                Element line = (Element) key.item(0);
                NodeList Packagedate = element.getElementsByTagName("PackageDate");
                Element line2 = (Element) Packagedate.item(0);
                xmlMapLocal.put(line.getTextContent(), line2.getTextContent());
            }

            //   Log.e("eeer", "PackagesssKey:" + xmlMapLocal);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    // DownloadXML AsyncTask
    private class DownloadXML extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... Url) {

            try {

                ArrayList<String> phoneNumberList = new ArrayList<>();
                xmlMapServer = new HashMap<String, String>();

                URL url = new URL(Url[0]);
                URLConnection conn = url.openConnection();

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(conn.getInputStream());

                NodeList nodes = doc.getElementsByTagName("Package");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element element = (Element) nodes.item(i);
                    NodeList key = element.getElementsByTagName("PackageKey");
                    Element line = (Element) key.item(0);
                    NodeList dateList = element.getElementsByTagName("PackageDate");
                    Element line2 = (Element) dateList.item(0);
                    xmlMapServer.put(line.getTextContent(), line2.getTextContent());

                    // phoneNumberList.add(line.getTextContent());
                }

                // Log.e("purchased", "PackageKey:" + xmlMapServer);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute(Void args) {

            for (final String key : xmlMapServer.keySet()) {

                String value1 = xmlMapLocal.get(key);
                String value2 = xmlMapServer.get(key);
                String contentID = key.replace("package-", "");

                if (!value1.equals(value2)) {
                    android.util.Log.e("purchased", "not Equal");

                    if (themeID.matches(Default.THEME1_ID)) {

                        if (Integer.valueOf(contentID) < 7) {
                            ContentValues values = new ContentValues();
                            values.put(ThemeContentTable.THEME_CONTENT_STATE, Default.STATE_UPDATE);
                            if (databaseHelper.updateSQL(ThemeContentTable.TABLE_NAME, values, ThemeContentTable.THEME_CONTENT_ID + " = '" + contentID + "' AND " + ThemeContentTable.THEME_CONTENT_STATE + "  == '" + Default.STATE_READY_TO_USE + "'", null)) {
                                //reload theme data
                                reloadThemeData();

                                for (int i = 0; i < nodesList.getLength(); i++) {

                                    Element element = (Element) nodesList.item(i);
                                    NodeList key2 = element.getElementsByTagName("PackageKey");
                                    Element line = (Element) key2.item(0);
                                    NodeList Packagedate = element.getElementsByTagName("PackageDate");

                                    Node node = Packagedate.item(0);

                                    if (line.getTextContent().equals(key)) {
                                        node.setTextContent(value2);
                                        writeXML();

                                        android.util.Log.e("testest", "writeXML:" + contentID);
                                    }

                                }
                            }
                        }
                    } else {

                        if (Integer.valueOf(contentID) > 10) {
                            ContentValues values = new ContentValues();
                            values.put(ThemeContentTable.THEME_CONTENT_STATE, Default.STATE_UPDATE);
                            if (databaseHelper.updateSQL(ThemeContentTable.TABLE_NAME, values, ThemeContentTable.THEME_CONTENT_ID + " = '" + contentID + "' AND " + ThemeContentTable.THEME_CONTENT_STATE + "  == '" + Default.STATE_READY_TO_USE + "'", null)) {
                                //reload theme data
                                reloadThemeData();

                                for (int i = 0; i < nodesList.getLength(); i++) {

                                    Element element = (Element) nodesList.item(i);
                                    NodeList key2 = element.getElementsByTagName("PackageKey");
                                    Element line = (Element) key2.item(0);
                                    NodeList Packagedate = element.getElementsByTagName("PackageDate");

                                    Node node = Packagedate.item(0);

                                    if (line.getTextContent().equals(key)) {
                                        node.setTextContent(value2);
                                        writeXML();
                                        android.util.Log.e("testest", "writeXML2:" + contentID);
                                    }
                                }
                            }
                        }
                    }

                } else
                    android.util.Log.e("purchased", "PackageNo:" + contentID);

            }


//            File xmlFile = new File(Default.XML_FILE_PATH);
//            if(xmlFile.exists())
//                xmlFile.delete();
        }
    }


    public void writeXML() {

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        DOMSource source = new DOMSource(docLocal);
        StreamResult result = new StreamResult(new File(Default.XML_FILE_PATH));
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        //System.out.println("Done");
    }


    /*
    Load theme content data into listview
     */
    private void loadThemeData() {
        ThreadManager.getInstance().execTask(new Runnable() {
            @Override
            public void run() {
                columns = new String[]{ThemeContentTable.THEME_CONTENT_ID + " as _id",
                        ThemeContentTable.THEME_CONTENT_ID, ThemeContentTable.THEME_ID, ThemeContentTable.THEME_CONTENT_TITLE,
                        ThemeContentTable.THEME_CONTENT_PRICE, ThemeContentTable.THEME_CONTENT_STATE};
                cursor = databaseHelper.getQueryResultData(ThemeContentTable.TABLE_NAME, columns, ThemeContentTable.THEME_ID + " = '" + themeID + "'", null, null, null, null, null);
                cursor = addAllPurchase(cursor);

                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        themeListAdapter = new ThemeContentListAdapter(getApplicationContext(), cursor, 0);
                        //add header & footer view to listview
                        themeContentListView.addFooterView(footerView);

                        themeListAdapter.setCustomButtonListner(ThemeContentActivity.this);
                        themeContentListView.setAdapter(themeListAdapter);
                    }
                });
            }
        });
    }

    //add all purchase practice
    private Cursor addAllPurchase(Cursor cursor) {
        try {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    MatrixCursor matrixCursor = new MatrixCursor(new String[]{"_id",
                            ThemeContentTable.THEME_CONTENT_ID,
                            ThemeContentTable.THEME_ID, ThemeContentTable.THEME_CONTENT_TITLE,
                            ThemeContentTable.THEME_CONTENT_PRICE, ThemeContentTable.THEME_CONTENT_STATE});

                    //add trial sentences
                    addRowContent(cursor, matrixCursor);
                    cursor.moveToNext();

                    //add "all purchase sentences"
                    MatrixCursor.RowBuilder rowBuilder = matrixCursor.newRow();
                    rowBuilder.add(Default.ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID);
                    rowBuilder.add(Default.ALL_PURCHASE_THEME_CONTENT_PACKAGE_ID);
                    rowBuilder.add(themeID);
                    rowBuilder.add(getApplicationContext().getResources().getString(R.string.all_purchase_content_title));
                    rowBuilder.add("0");
                    rowBuilder.add(Default.STATE_READY_TO_USE);

                    do {
                        addRowContent(cursor, matrixCursor);
                    } while (cursor.moveToNext());

                    havePurchaseContent = havePurchaseContent();

                    return matrixCursor;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "addAllPurchase() error " + e);
        }
        return cursor;
    }

    /**
     * Create new cursor from sqlite data
     */
    private void addRowContent(Cursor cursor, MatrixCursor matrixCursor) {
        try {
            MatrixCursor.RowBuilder rowBuilder = matrixCursor.newRow();
            rowBuilder.add(cursor.getInt(cursor.getColumnIndexOrThrow(ThemeContentTable.THEME_CONTENT_ID)));
            rowBuilder.add(cursor.getInt(cursor.getColumnIndexOrThrow(ThemeContentTable.THEME_CONTENT_ID)));
            rowBuilder.add(cursor.getString(cursor.getColumnIndexOrThrow(ThemeContentTable.THEME_ID)));
            rowBuilder.add(cursor.getString(cursor.getColumnIndexOrThrow(ThemeContentTable.THEME_CONTENT_TITLE)));
            rowBuilder.add(cursor.getString(cursor.getColumnIndexOrThrow(ThemeContentTable.THEME_CONTENT_PRICE)));
            rowBuilder.add(cursor.getInt(cursor.getColumnIndexOrThrow(ThemeContentTable.THEME_CONTENT_STATE)));
        } catch (Exception e) {
            Log.e(TAG, "addRowContent() error " + e);
        }
    }

    /**
     * Check have content is purchased or not
     */
    private boolean havePurchaseContent() {
        String queryStr = "select * from " + ThemeContentTable.TABLE_NAME
                + " where " + ThemeContentTable.THEME_CONTENT_STATE + " = " + Default.STATE_READY_TO_USE
                + " and " + ThemeContentTable.THEME_ID + " = '" + themeID + "'"
                + " and " + ThemeContentTable.THEME_CONTENT_ID + " != 1"
                + " and " + ThemeContentTable.THEME_CONTENT_ID + " != 10;";
        Cursor cursor = null;
        try {
            cursor = databaseHelper.getQueryResultData(queryStr);

            if (cursor != null && cursor.moveToFirst()) {
                return true;
            }

        } catch (Exception e) {

        } finally {
            if (cursor != null)
                cursor.close();
        }

        return false;
    }

    /*
    Reload theme content data into listview
     */
    private void reloadThemeData() {
        ThreadManager.getInstance().execTask(new Runnable() {
            @Override
            public void run() {
                cursor = databaseHelper.getQueryResultData(ThemeContentTable.TABLE_NAME, columns, ThemeContentTable.THEME_ID + " = '" + themeID + "'", null, null, null, null, null);
                cursor = addAllPurchase(cursor);

                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        themeListAdapter.swapCursor(cursor);
                        //update footerview
                        configureFooterView();
                    }
                });
            }
        });
    }

    /*
    configure Foter View for free sentence set
    */
    private void configureFooterView() {
        //query to database for state greater than 0 interval of three row if exist then invisible set button
        Cursor cr = databaseHelper.getQueryResultData(ThemeContentTable.TABLE_NAME, new String[]{ThemeContentTable.THEME_CONTENT_ID}, ThemeContentTable.THEME_ID + " = '" +
                themeID + "' AND " + ThemeContentTable.THEME_CONTENT_STATE + " >1 ", null, null, null, null, null);
        if (cr != null && cr.getCount() > 0) {
            cr.moveToFirst();
            //check cursor count 2 or greater (by default retrieve 1 for free set)
            if (cr.getCount() >= 2) {
                //invisible purchase all button
                //purchaseAllButton.setVisibility(View.GONE);
                purchaseAllButton.setEnabled(false);
                RLbuyALL.setVisibility(View.GONE);
            }
            do {
                int i = cr.getInt(cr.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID));
                if (Default.THEME1_SET1.indexOf("" + i) >= 0 || Default.THEME2_SET1.indexOf("" + i) >= 0) {
                    //purchaseSet1Button.setVisibility(View.GONE);
                    purchaseSet1Button.setEnabled(false);
                    RLset1.setVisibility(View.GONE);
                }
                if (Default.THEME1_SET2.indexOf("" + i) >= 0 || Default.THEME2_SET2.indexOf("" + i) >= 0) {
                    //purchaseSet2Button.setVisibility(View.GONE);
                    purchaseSet2Button.setEnabled(false);
                    RLset2.setVisibility(View.GONE);
                }
            } while (cr.moveToNext());
        }

    }

    /*
    Load item price if not loaded previously
     */
    private void loadPriceFromInAppServer() {
        //check in share preference that price already loaded or not
        if (sharedPreferences.getString(Default.SKU_THEME1_PACKAGE1, null) != null) {
            updateThemeContent(2, sharedPreferences.getString(Default.SKU_THEME1_PACKAGE1, null));
        }

        if (sharedPreferences.getString(Default.SKU_THEME1_PACKAGE2, null) != null) {
            updateThemeContent(3, sharedPreferences.getString(Default.SKU_THEME1_PACKAGE2, null));
        }

        if (sharedPreferences.getString(Default.SKU_THEME1_PACKAGE3, null) != null) {
            updateThemeContent(4, sharedPreferences.getString(Default.SKU_THEME1_PACKAGE3, null));
        }

        if (sharedPreferences.getString(Default.SKU_THEME1_PACKAGE4, null) != null) {
            updateThemeContent(5, sharedPreferences.getString(Default.SKU_THEME1_PACKAGE4, null));
        }

        if (sharedPreferences.getString(Default.SKU_THEME1_PACKAGE5, null) != null) {
            updateThemeContent(6, sharedPreferences.getString(Default.SKU_THEME1_PACKAGE5, null));
        }


        if (sharedPreferences.getString(Default.SKU_THEME2_PACKAGE1, null) != null) {
            updateThemeContent(11, sharedPreferences.getString(Default.SKU_THEME2_PACKAGE1, null));
        }
        if (sharedPreferences.getString(Default.SKU_THEME2_PACKAGE2, null) != null) {
            updateThemeContent(12, sharedPreferences.getString(Default.SKU_THEME2_PACKAGE2, null));
        }
        if (sharedPreferences.getString(Default.SKU_THEME2_PACKAGE3, null) != null) {
            updateThemeContent(13, sharedPreferences.getString(Default.SKU_THEME2_PACKAGE3, null));
        }
        if (sharedPreferences.getString(Default.SKU_THEME2_PACKAGE4, null) != null) {
            updateThemeContent(14, sharedPreferences.getString(Default.SKU_THEME2_PACKAGE4, null));
        }
        if (sharedPreferences.getString(Default.SKU_THEME2_PACKAGE5, null) != null) {
            updateThemeContent(15, sharedPreferences.getString(Default.SKU_THEME2_PACKAGE5, null));
        }

        if (sharedPreferences.getString(Default.SKU_THEME2_PACKAGE6, null) != null) {
            updateThemeContent(16, sharedPreferences.getString(Default.SKU_THEME2_PACKAGE6, null));
        }

        if (sharedPreferences.getString(Default.SKU_THEME2_PACKAGE7, null) != null) {
            updateThemeContent(17, sharedPreferences.getString(Default.SKU_THEME2_PACKAGE7, null));
        }

        if (sharedPreferences.getString(Default.SKU_THEME2_PACKAGE8, null) != null) {
            updateThemeContent(18, sharedPreferences.getString(Default.SKU_THEME2_PACKAGE8, null));
        }

        //set button prices
        if (themeID.matches(Default.THEME1_ID)) {
            if (sharedPreferences.getString(Default.SKU_THEME1_PACKAGE1_TO_2, null) != null) {
                purchaseSet1Button.setText(sharedPreferences.getString(Default.SKU_THEME1_PACKAGE1_TO_2, null));
            }

            if (sharedPreferences.getString(Default.SKU_THEME1_PACKAGE3_TO_5, null) != null) {
                purchaseSet2Button.setText(sharedPreferences.getString(Default.SKU_THEME1_PACKAGE3_TO_5, null));
            }

            if (sharedPreferences.getString(Default.SKU_THEME1_PACKAGE_ALL, null) != null) {
                purchaseAllButton.setText(sharedPreferences.getString(Default.SKU_THEME1_PACKAGE_ALL, null));
            }
        } else if (themeID.matches(Default.THEME2_ID)) {
            if (sharedPreferences.getString(Default.SKU_THEME2_PACKAGE1_TO_4, null) != null) {
                purchaseSet1Button.setText(sharedPreferences.getString(Default.SKU_THEME2_PACKAGE1_TO_4, null));
            }

            if (sharedPreferences.getString(Default.SKU_THEME2_PACKAGE5_TO_8, null) != null) {
                purchaseSet2Button.setText(sharedPreferences.getString(Default.SKU_THEME2_PACKAGE5_TO_8, null));
            }

            if (sharedPreferences.getString(Default.SKU_THEME2_PACKAGE_ALL, null) != null) {
                purchaseAllButton.setText(sharedPreferences.getString(Default.SKU_THEME2_PACKAGE_ALL, null));
            }
        }
        //reload theme data
        reloadThemeData();
    }

    /**
     * update where theme content id & state <= 1
     *
     * @param contentId
     * @param price
     */
    private void updateThemeContent(int contentId, String price) {
        android.util.Log.e("Iap data of contentID:" + contentId, "New Price: " + price);
        ContentValues contentValues = new ContentValues();
        contentValues.put(ThemeContentTable.THEME_CONTENT_PRICE, price);
        contentValues.put(ThemeContentTable.THEME_CONTENT_STATE, Default.STATE_PURCHASE);
        if (databaseHelper.updateSQL(ThemeContentTable.TABLE_NAME, contentValues, ThemeContentTable.THEME_CONTENT_ID + " = '" + contentId + "' AND " + ThemeContentTable.THEME_CONTENT_STATE + "  <= '" + Default.STATE_PURCHASE + "'", null)) {
            android.util.Log.d("InappPurchase", "Price & state update");
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        //update dynamic content here
        updateAdView();
    }

    /*
    Button click implementation
     */
    @Override
    public void onButtonClickListner(int position) {
        cursor.moveToPosition(position);
        android.util.Log.e("Content ID", cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID)) + "");
        //based on state take different action
        if (cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_STATE)) == Default.STATE_LOAD_PRICE || cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_STATE)) == Default.STATE_PURCHASE) {

//            ContentValues values = new ContentValues();
//            values.put(ThemeContentTable.THEME_CONTENT_STATE, Default.STATE_DOWNLOAD);
//            if (databaseHelper.updateSQL(ThemeContentTable.TABLE_NAME, values, ThemeContentTable.THEME_CONTENT_ID + " = '" + cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID)) + "'", null)) {
//                //reload theme data
//                reloadThemeData();
//            }

            //launch in app purchase
            launchInAppPurchase(cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID)));
        } else if (cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_STATE)) == Default.STATE_DOWNLOAD) {
            //download from server & then change state
            //check internet connection
            updateStatus = false;
            if (!connectionDetector.isConnectingToInternet()) {
                // Internet Connection is not present
                // stop executing code by return
                return;
            }
            downloadFileFromURLTask = new DownloadFileFromURL();
            downloadFileFromURLTask.execute(new String[]{String.valueOf(cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID)))});
        } else if (cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_STATE)) == Default.STATE_UPDATE) {
            //download from server & then change state
            //check internet connection
            updateStatus = true;
            if (!connectionDetector.isConnectingToInternet()) {
                // Internet Connection is not present
                // stop executing code by return
                return;
            }
            downloadFileFromURLTask = new DownloadFileFromURL();
            downloadFileFromURLTask.execute(new String[]{String.valueOf(cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID)))});
        }
    }

    /**
     * Button click implementation
     */
    public void purchaseSet1Pressed(View v) {
        //launch in app purchase
        //check content set
        if (themeID.matches(Default.THEME1_ID)) {
            launchInAppPurchase(Default.THEME1_SET1_ID);
        } else if (themeID.matches(Default.THEME2_ID)) {
            launchInAppPurchase(Default.THEME2_SET1_ID);
        }
    }

    /*
    Button click implementation
     */
    public void purchaseSet2Pressed(View v) {
        //launch in app purchase
        //check content set
        if (themeID.matches(Default.THEME1_ID)) {
            launchInAppPurchase(Default.THEME1_SET2_ID);
        } else if (themeID.matches(Default.THEME2_ID)) {
            launchInAppPurchase(Default.THEME2_SET2_ID);
        }

    }

    /**
     * purchase all package button implementation
     *
     * @param v
     */
    public void purchaseAllPressed(View v) {
        //launch in app purchase
        //check content set
        if (themeID.matches(Default.THEME1_ID)) {
            launchInAppPurchase(Default.THEME1_ALL_ID);
        } else if (themeID.matches(Default.THEME2_ID)) {
            launchInAppPurchase(Default.THEME2_ALL_ID);
        }
    }

    public void launchInAppPurchase(final int contentID) {

        App.inapp_billing_supporter.setPurchasedListener(this);
        //theme 1
        if (contentID == 2) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME1_PACKAGE1);
        }
        if (contentID == 3) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME1_PACKAGE2);
        }
        if (contentID == 4) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME1_PACKAGE3);
        }
        if (contentID == 5) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME1_PACKAGE4);
        }
        if (contentID == 6) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME1_PACKAGE5);
        }
        if (contentID == Default.THEME1_SET1_ID) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME1_PACKAGE1_TO_2);
        }

        if (contentID == Default.THEME1_SET2_ID) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME1_PACKAGE3_TO_5);
        }

        if (contentID == Default.THEME1_ALL_ID) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME1_PACKAGE_ALL);
        }

        //theme 2
        if (contentID == 11) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME2_PACKAGE1);
        }
        if (contentID == 12) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME2_PACKAGE2);
        }
        if (contentID == 13) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME2_PACKAGE3);
        }
        if (contentID == 14) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME2_PACKAGE4);
        }
        if (contentID == 15) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME2_PACKAGE5);
        }
        if (contentID == 16) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME2_PACKAGE6);
        }
        if (contentID == 17) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME2_PACKAGE7);
        }
        if (contentID == 18) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME2_PACKAGE8);
        }

        if (contentID == Default.THEME2_SET1_ID) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME2_PACKAGE1_TO_4);
        }

        if (contentID == Default.THEME2_SET2_ID) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME2_PACKAGE5_TO_8);
        }

        if (contentID == Default.THEME2_ALL_ID) {
            App.inapp_billing_supporter.purchase(this, Default.SKU_THEME2_PACKAGE_ALL);
        }
    }

    @Override
    public void purchased(String sku_id, boolean isTest) {
        android.util.Log.e("purchased", "sku_id:" + sku_id);
        if (isTest) {
            //sku_id=sharedPreferences.getString("TEST_PURCHASE_ID","");
            return;
        }
        List contentID = null;

        if (sku_id.matches(Default.SKU_THEME1_PACKAGE1)) {
            contentID = new ArrayList<>(Arrays.asList("2"));
        }
        if (sku_id.matches(Default.SKU_THEME1_PACKAGE2)) {
            contentID = new ArrayList<>(Arrays.asList("3"));
        }
        if (sku_id.matches(Default.SKU_THEME1_PACKAGE3)) {
            contentID = new ArrayList<>(Arrays.asList("4"));
        }
        if (sku_id.matches(Default.SKU_THEME1_PACKAGE4)) {
            contentID = new ArrayList<>(Arrays.asList("5"));
        }
        if (sku_id.matches(Default.SKU_THEME1_PACKAGE5)) {
            contentID = new ArrayList<>(Arrays.asList("6"));
        }
        if (sku_id.matches(Default.SKU_THEME1_PACKAGE1_TO_2)) {
            contentID = Default.THEME1_SET1;
        }
        if (sku_id.matches(Default.SKU_THEME1_PACKAGE3_TO_5)) {
            contentID = Default.THEME1_SET2;
        }
        if (sku_id.matches(Default.SKU_THEME1_PACKAGE_ALL)) {
            contentID = Default.THEME1_ALL;
            //return;
        }
        if (sku_id.matches(Default.SKU_THEME2_PACKAGE1)) {
            contentID = new ArrayList<>(Arrays.asList("11"));
        }
        if (sku_id.matches(Default.SKU_THEME2_PACKAGE2)) {
            contentID = new ArrayList<>(Arrays.asList("12"));
        }
        if (sku_id.matches(Default.SKU_THEME2_PACKAGE3)) {
            contentID = new ArrayList<>(Arrays.asList("13"));
        }
        if (sku_id.matches(Default.SKU_THEME2_PACKAGE4)) {
            contentID = new ArrayList<>(Arrays.asList("14"));
        }
        if (sku_id.matches(Default.SKU_THEME2_PACKAGE5)) {
            contentID = new ArrayList<>(Arrays.asList("15"));
        }
        if (sku_id.matches(Default.SKU_THEME2_PACKAGE6)) {
            contentID = new ArrayList<>(Arrays.asList("16"));
        }
        if (sku_id.matches(Default.SKU_THEME2_PACKAGE7)) {
            contentID = new ArrayList<>(Arrays.asList("17"));
        }
        if (sku_id.matches(Default.SKU_THEME2_PACKAGE8)) {
            contentID = new ArrayList<>(Arrays.asList("18"));
        }
        if (sku_id.matches(Default.SKU_THEME2_PACKAGE1_TO_4)) {
            contentID = Default.THEME2_SET1;
        }
        if (sku_id.matches(Default.SKU_THEME2_PACKAGE5_TO_8)) {
            contentID = Default.THEME2_SET2;
        }
        if (sku_id.matches(Default.SKU_THEME2_PACKAGE_ALL)) {
            contentID = Default.THEME2_ALL;
        }
        if (contentID == null || contentID.size() <= Default.ZERO) {
            return;
        }
        //update content state
        for (int i = 0; i < contentID.size(); i++) {
            ContentValues values = new ContentValues();
            values.put(ThemeContentTable.THEME_CONTENT_STATE, Default.STATE_DOWNLOAD);
            if (databaseHelper.updateSQL(ThemeContentTable.TABLE_NAME, values, ThemeContentTable.THEME_CONTENT_ID + " = '" + contentID.get(i) + "'", null)) {

            }
        }
        //reload theme data
        reloadThemeData();
        sharedPreferences.edit().putBoolean(Default.IN_APP_PURCHASE, true).commit();
        updateAdView();
    }

    @Override
    public void inventoryChecked(String sku_id) {

        //ここにはsetup時しかこない
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

    /**
     * Copy Free Set data from asset to sd card
     */
    private void copyFreeSetResources() {
        new FreeSetResourcesConfiguration().execute();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (!App.inapp_billing_supporter.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //check for resources directory
                    if (!new File(Default.RESOURCES_BASE_DIRECTORY).exists()) {
                        new File(Default.RESOURCES_BASE_DIRECTORY).mkdirs();
                    }
                    copyFreeSetResources();
                } else {
                    finish();
                }
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        android.util.Log.e("Theme Content Activity", "On Pause");
        // updateLearningTime(Default.PAUSE_STATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        android.util.Log.e("Theme Content Activity", "On resume");
        // updateLearningTime(Default.RESUME_STATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //all connection close here
        android.util.Log.e("Fragment", "Destroy ");
        databaseHelper.closeDataBase();
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
                //Open setting fragment
                finish();
                return true;

            case R.id.action_option:
                showActionSheet();
                return true;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
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

    /**
     * Showing Dialog
     */
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type: // we set this to 0
                pDialog = new ProgressDialog(this);

                if (!updateStatus)
                    pDialog.setMessage(getResources().getString(R.string.download_progress_message));

                else
                    pDialog.setMessage(getResources().getString(R.string.update_progress_message));

                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (downloadFileFromURLTask.getStatus() == AsyncTask.Status.RUNNING) {
                            // My AsyncTask is currently doing work in doInBackground()
                            downloadFileFromURLTask.cancel(true);
                            android.util.Log.e("Asynktask ", "Running");
                        }
                    }
                });
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    /*
    *Alert dialog box
     */
    private void showAlert(String message) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        // alertDialogBuilder.setTitle(R.string.profile_setup_alert_title);

        // set dialog message
        alertDialogBuilder
                .setMessage(message)
                .setPositiveButton(getResources().getString(R.string.alert_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    class FreeSetResourcesConfiguration extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... param) {
            File freeResoucesZipFile = null;
            File freeResoucesFile = null;
            try {
                android.util.Log.e("FreeSetResources", "Start Background Copy");
                if (cursor == null || !cursor.moveToFirst()) {
                    return null;
                }
                if (cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID)) < 0)
                    return null;

                String freeResourcesZipName = Default.RESOURCES_PREFIX + cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID)) + Default.RESOURCES_SUFIX;
                String freeResourcesName = Default.RESOURCES_PREFIX + cursor.getInt(cursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID));
                freeResoucesZipFile = new File(Default.RESOURCES_BASE_DIRECTORY + freeResourcesZipName);
                freeResoucesFile = new File(Default.RESOURCES_BASE_DIRECTORY + freeResourcesName);

                if (freeResoucesFile.exists()) {
                    return null;
                }
                OutputStream myOutput = new FileOutputStream(Default.RESOURCES_BASE_DIRECTORY + freeResourcesZipName);
                InputStream myInput = getAssets().open(freeResourcesZipName);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = myInput.read(buffer)) > 0) {
                    myOutput.write(buffer, 0, length);
                }
                myInput.close();
                myOutput.flush();
                myOutput.close();
                //unzip now
                //unzip
                Decompress d = new Decompress(Default.RESOURCES_BASE_DIRECTORY + freeResourcesZipName, Default.RESOURCES_BASE_DIRECTORY);
                //Decompress d = new Decompress(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0] + Default.RESOURCES_SUFIX, Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0]+"/");
                if (!d.unzip()) {

                }
                //delete zip file
                if (freeResoucesZipFile.exists()) {
                    freeResoucesZipFile.delete();
                }
                //delete _MACOSX file
                if (new File(Default.RESOURCES_BASE_DIRECTORY + "__MACOSX").exists()) {
                    DeleteRecursive(new File(Default.RESOURCES_BASE_DIRECTORY + "__MACOSX"));
                }

            } catch (Exception e) {

            } finally {
                if (freeResoucesZipFile != null && freeResoucesZipFile.exists()) {
                    freeResoucesZipFile.delete();
                }
                if (freeResoucesFile != null && freeResoucesFile.exists()) {
                    freeResoucesFile.delete();
                }
            }

            android.util.Log.e("FreeSetResources", "End Background Copy");

            return null;
        }
    }

    private void DeleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                DeleteRecursive(child);

        fileOrDirectory.delete();
    }

    /**
     * Background Async Task to download file
     */
    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread
         * Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //pDialog.setProgress(0);
            //check for resources directory
            if (!new File(Default.RESOURCES_BASE_DIRECTORY).exists()) {
                new File(Default.RESOURCES_BASE_DIRECTORY).mkdirs();
            }

            showDialog(progress_bar_type);
            publishProgress("" + 0);
            //   pDialog.setMessage(getResources().getString(R.string.download_progress_message));

            if (!updateStatus)
                pDialog.setMessage(getResources().getString(R.string.download_progress_message));

            else
                pDialog.setMessage(getResources().getString(R.string.update_progress_message));

        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected String doInBackground(String... params) {
            int count;
            try {
                //delete zip file
                if (new File(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0] + Default.RESOURCES_SUFIX).exists()) {
                    new File(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0] + Default.RESOURCES_SUFIX).delete();
                }
                //delete folder file
                if (new File(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0]).exists()) {
                    new File(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0]).delete();
                }

                URL url = new URL(Default.RESOURCES_BASE_URL + Default.RESOURCES_PREFIX + params[0] + Default.RESOURCES_SUFIX);
                URLConnection conection = url.openConnection();
                conection.connect();
                // getting file length
                int lenghtOfFile = conection.getContentLength();

                // input stream to read file - with 8k buffer
                InputStream input = new BufferedInputStream(url.openStream(), 8192);

                // Output stream to write file
                OutputStream output = new FileOutputStream(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0] + Default.RESOURCES_SUFIX);

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

                //unzip
                Decompress d = new Decompress(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0] + Default.RESOURCES_SUFIX, Default.RESOURCES_BASE_DIRECTORY);
                //Decompress d = new Decompress(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0] + Default.RESOURCES_SUFIX, Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0]+"/");
                if (!d.unzip()) {

                }
                //delete zip file
                if (new File(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0] + Default.RESOURCES_SUFIX).exists()) {
                    new File(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + params[0] + Default.RESOURCES_SUFIX).delete();
                }
                //delete _MACOSX file
                if (new File(Default.RESOURCES_BASE_DIRECTORY + "__MACOSX").exists()) {
                    DeleteRecursive(new File(Default.RESOURCES_BASE_DIRECTORY + "__MACOSX"));
                }


                return params[0];

            } catch (Exception e) {
                android.util.Log.e("Error: ", e.getMessage());
                return null;
            }
        }

        /**
         * Updating progress bar
         */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
            if (Integer.parseInt(progress[0]) >= 99) {
                pDialog.setMessage(getResources().getString(R.string.unzip_progress_message));
            }
        }

        /**
         * After completing background task
         * Dismiss the progress dialog
         * *
         */
        @Override
        protected void onPostExecute(String contentID) {

            if (contentID == null) {
                // dismiss the dialog
                dismissDialog(progress_bar_type);

                //show alert to download again
                showAlert(getResources().getString(R.string.download_error_message));

                //stop furture execution
                return;
            }
            android.util.Log.e("Result", contentID);


//            //unzip
//            Decompress d = new Decompress(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + contentID + Default.RESOURCES_SUFIX, Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + contentID+"/");
//            if (!d.unzip()) {
//                // dismiss the dialog
//                dismissDialog(progress_bar_type);
//                //show alert
//                showAlert(getResources().getString(R.string.unzip_error_message));
//                //stop furture execution
//                return;
//            }
//            //delete zip file
//            if (new File(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + contentID + Default.RESOURCES_SUFIX).exists()){
//                new File(Default.RESOURCES_BASE_DIRECTORY + Default.RESOURCES_PREFIX + contentID + Default.RESOURCES_SUFIX).delete();
//            }
//            //update content state
            ContentValues values = new ContentValues();
            values.put(ThemeContentTable.THEME_CONTENT_STATE, Default.STATE_READY_TO_USE);
            if (databaseHelper.updateSQL(ThemeContentTable.TABLE_NAME, values, ThemeContentTable.THEME_CONTENT_ID + " = '" + contentID + "'", null)) {
                //reload theme data
                reloadThemeData();
            }
            // dismiss the dialog
            dismissDialog(progress_bar_type);

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
                Intent i = new Intent(ThemeContentActivity.this, HomeActivity.class);
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
