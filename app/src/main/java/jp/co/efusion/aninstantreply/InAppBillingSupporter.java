package jp.co.efusion.aninstantreply;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.android.trivialdrivesample.util.IabHelper;
import com.example.android.trivialdrivesample.util.IabResult;
import com.example.android.trivialdrivesample.util.Inventory;
import com.example.android.trivialdrivesample.util.Purchase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.co.efusion.database.DatabaseHelper;
import jp.co.efusion.database.ThemeContentTable;
import jp.co.efusion.utility.Default;

/*
 * IAB実装のサポートクラス
 * IABHelperより実装の想定範囲を狭くして、簡単に導入できるようにする
 * 
 * 実装方法
 * ０．Manifest追加　<uses-permission android:name="com.android.vending.BILLING" />  
 * １．base64EncodedPublicKey, INAPP_BILLING_REQUEST_CODEをセット
 * ２．購入処理のあるActivityの、onActivityResult内で、handleActivityResult()を呼ぶようにする
 * ３．アプリ終了処理のあるActivityのonDestroyで、destroy()を実行するようにする
 * ４．アプリ起動時にsetup()を実行する。実行後にすぐに購入アイテムのチェックが行われる。チェックは非同期で行われる。この際に設定したリスナーにより、購入済みかどうかを取得できる
 * ５．購入処理を行うにはpurhcase()を実行する。setPurchasedListenerを設定しておけば、アイテム購入時にpurchased()メソッドが呼ばれる
 *
 *
 * テスト上の問題
 *
 * 購入ボタンを押してからインジケータ付きのポップアップが表示されるが
 * この時にタップするとポップアップ表示が消えてエラーでアクティビティが落ちる
 * しかしこれはアプリローカルテスト購入時（android.test.purchased使用時）のみの現象で、
 * 本番購入（アルファ・ベータテスト含む）では、タップしてもポップアップが消えないようになっている
 *
 */


class InAppBillingSupporter {
    //InApp Billing
    String base64EncodedPublicKey = Default.base64EncodedPublicKey;
    final static int INAPP_BILLING_REQUEST_CODE = 987; //onActivityResultに渡されるコード。他のコードと重複しないように変更する
    final static boolean INAPP_BILLING_TEST_BUY = Default.TEST_PURCHASE; //テスト購入を有効
    final static boolean INAPP_BILLING_TEST_CLEAR = false; //テスト購入をクリア
    final static boolean MODE_BILLING_CONSUME = Default.SKU_TYPE_CONSUMABLE; //消費型モード
    static final String[] SKU_ITEMS = {
            Default.SKU_THEME1_PACKAGE1,
            Default.SKU_THEME1_PACKAGE2,
            Default.SKU_THEME1_PACKAGE3,
            Default.SKU_THEME1_PACKAGE4,
            Default.SKU_THEME1_PACKAGE5,
            Default.SKU_THEME1_PACKAGE1_TO_2,
            Default.SKU_THEME1_PACKAGE3_TO_5,
            Default.SKU_THEME1_PACKAGE_ALL,
            Default.SKU_THEME2_PACKAGE1,
            Default.SKU_THEME2_PACKAGE2,
            Default.SKU_THEME2_PACKAGE3,
            Default.SKU_THEME2_PACKAGE4,
            Default.SKU_THEME2_PACKAGE5,
            Default.SKU_THEME2_PACKAGE6,
            Default.SKU_THEME2_PACKAGE7,
            Default.SKU_THEME2_PACKAGE8,
            Default.SKU_THEME2_PACKAGE1_TO_4,
            Default.SKU_THEME2_PACKAGE5_TO_8,
            Default.SKU_THEME2_PACKAGE_ALL};
    static final String SKU_TEST = "android.test.purchased";


    IabHelper mHelper;

    SharedPreferences sharedPreferences;

    DatabaseHelper databaseHelper;

    private InAppBillingSupporterListener inventory_checked_listener;

    private InAppBillingSupporterListener purchased_listener;
    private OnFinishInventoryConsumedListener finish_inventory_consumed_listener;

    private SKUDetailsLoaderListener skuDetailsLoaderListener=null;

    public interface OnFinishInventoryConsumedListener {
        public void onFinishInventoryConsumed(Purchase purchase, IabResult result);
    }


    //購入処理のあるActivityのActivityResultにて呼び出す必要がある
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return mHelper.handleActivityResult(requestCode, resultCode, data);
    }

    //メインのアクティビティが終了する際（onDestroy()時）に以下を実行する必要がある
    public void destroy() {
        if (mHelper != null) mHelper.dispose();
        mHelper = null;
    }

    public void setPurchasedListener(InAppBillingSupporterListener listener) {
        this.purchased_listener = listener;
    }

    public void setup(Context context, InAppBillingSupporterListener listener, OnFinishInventoryConsumedListener finish_inventory_consumed_listener) {

        //initialize database helper
        databaseHelper = new DatabaseHelper(context);
        //open database
        databaseHelper.openDatabase();

        //initialize sharepreference
        sharedPreferences = context.getSharedPreferences(Default.SHARE_PREFERENCE_NAME,
                Context.MODE_PRIVATE);

        this.setup(context, mGotInventoryListener, listener);
        this.finish_inventory_consumed_listener = finish_inventory_consumed_listener;
    }

    /**
     * load SKU details like title,price,description from in app purchase server
     */
    public void loadSKUDetails(){
        //query for all sku product
        ArrayList<String> skuList = new ArrayList<String>();
        for (int i = 0; i < SKU_ITEMS.length; i++) {
            skuList.add(SKU_ITEMS[i]);
        }
        try {

            mHelper.queryInventoryAsync(true, skuList, new IabHelper.QueryInventoryFinishedListener() {
                @Override
                public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                    if (result.isFailure()) {
                        return;
                    }
                    for (int i = 0; i < SKU_ITEMS.length; i++) {
                        if (inv.getSkuDetails(SKU_ITEMS[i]) != null) {
                            sharedPreferences.edit().putString(SKU_ITEMS[i], inv.getSkuDetails(SKU_ITEMS[i]).getPrice()).commit();
                            Log.e("In App Purchase ID: "+ SKU_ITEMS[i], "Price is" + inv.getSkuDetails(SKU_ITEMS[i]).getPrice());
                        }
                        //check that already purchase
                        if (inv.hasPurchase(SKU_ITEMS[i])){
                            Log.e("Purchase Item",SKU_ITEMS[i]);
                            //restore purchase
                            restorePurchase(SKU_ITEMS[i], false);
                        }
                    }
                    if (skuDetailsLoaderListener != null) {
                        skuDetailsLoaderListener.onSKUDetailsLoad();
                    }
                }
            });
        }catch (Exception e){
            Log.e("queryInventoryAsync","error " + e);
        }
    }
    public void setSKUDetailsLoaderListener(SKUDetailsLoaderListener skudetailsLoaderListener){
        this.skuDetailsLoaderListener=skudetailsLoaderListener;
    }

    //セットアップ
    public void setup(Context context, final IabHelper.QueryInventoryFinishedListener inventory_finish_listener, InAppBillingSupporterListener _supporter_listner) {
        this.inventory_checked_listener = _supporter_listner;

        mHelper = new IabHelper(context, base64EncodedPublicKey);
//        App.mHelper.enableDebugLogging(true);

        try {
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {

                    if (!result.isSuccess()) {
                        // Oh noes, there was a problem.
//                    complain("Problem setting up in-app billing: " + result);
                        return;
                    }
                    //query for all sku product
                    ArrayList<String> skuList = new ArrayList<String>();
                    for (int i = 0; i < SKU_ITEMS.length; i++) {
                        skuList.add(SKU_ITEMS[i]);
                    }
                    mHelper.queryInventoryAsync(true,skuList,inventory_finish_listener);
                }
            });
        } catch (Exception e) {
//            Log.d("", "IabHelper Error");
        }
    }

    //購入済みアイテムの確認完了時
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {

            if (result.isFailure()) {
                return;
            }
            try {

                for (int i = 0; i < SKU_ITEMS.length; i++) {
                    if (inventory.getSkuDetails(SKU_ITEMS[i]) != null) {
                        sharedPreferences.edit().putString(SKU_ITEMS[i], inventory.getSkuDetails(SKU_ITEMS[i]).getPrice()).commit();
                        Log.e("In App Purchase", "Price is" + inventory.getSkuDetails(SKU_ITEMS[i]).getPrice());
                    }
                    //check that already purchase
                    if (inventory.hasPurchase(SKU_ITEMS[i])){
                        Log.e("Purchase Item",SKU_ITEMS[i]);
                        //restore purchase
                        restorePurchase(SKU_ITEMS[i],false);
                    }
                }
            }catch (Exception e){

            }
            //テスト削除
            if (INAPP_BILLING_TEST_CLEAR) {
                mHelper.consumeAsync(inventory.getPurchase("android.test.purchased"), null);
            } else if (INAPP_BILLING_TEST_BUY) {
                if (inventory.hasPurchase(SKU_TEST)) {
                    if (MODE_BILLING_CONSUME)
                        consume(inventory.getPurchase(SKU_TEST), true);
                    else
                        inventory_checked_listener.inventoryChecked(SKU_TEST);

                }
            } else {
                if (MODE_BILLING_CONSUME) {

                    for (int i = 0; i < SKU_ITEMS.length; i++) {
                        if (inventory.hasPurchase(SKU_ITEMS[i])) {
                            consume(inventory.getPurchase(SKU_ITEMS[i]), true);
                            break;
                        }
                    }
                } else {
                    for (int i = 0; i < SKU_ITEMS.length; i++) {
                        inventory_checked_listener.inventoryChecked(SKU_ITEMS[i]);
                    }
                }


            }


        }
    };

    //購入メソッド
    public void purchase(Activity context, String sku_id) {
        if (INAPP_BILLING_TEST_BUY) {
            sharedPreferences.edit().putString("TEST_PURCHASE_ID",sku_id);
            mHelper.launchPurchaseFlow(context, SKU_TEST, INAPP_BILLING_REQUEST_CODE, mPurchaseFinishedListener, sku_id);
        }else {
//			String sku_id = SKU_ID; //購入する商品のID
            int request_code = INAPP_BILLING_REQUEST_CODE; //onActivityResult呼び出し時に返されるコード
            IabHelper.OnIabPurchaseFinishedListener finished_listener = mPurchaseFinishedListener; //購入完了後に呼び出されるリスナーを設定
            //developer payload の文字列で、オーダーに対する追加情報（空の文字列でもよい）を送るために使用できる。購入後のレスポンスにこの値が入っていて、購入リクエストをユニークに識別する事ができる。
            mHelper.launchPurchaseFlow(context, sku_id, request_code, finished_listener, "");

            //定期購入は別メソッドを使用する
//			App.mHelper.launchSubscriptionPurchaseFlow(this, sku_id, request_code, finished_listener,extra_data);

        }

    }

    //購入完了時
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()) {
                Log.d("onIabError", "error code " + result.getResponse() + ":" + result.getMessage());
                return;
            } else {

                if (MODE_BILLING_CONSUME)
                    consume(purchase, false);
                else
                    callPurchaseFinishListener(purchase);


            }

        }
    };


    private void consume(Purchase purchase, final boolean isInventoryConsume) {

        mHelper.consumeAsync(purchase, new IabHelper.OnConsumeFinishedListener() {
            @Override
            public void onConsumeFinished(Purchase purchase, IabResult result) {

                if (result.isFailure()) {
                    Log.d("onIabError", "error code " + result.getResponse() + ":" + result.getMessage());
                    return;
                } else {
                    if (isInventoryConsume)
                        finish_inventory_consumed_listener.onFinishInventoryConsumed(purchase, result);
                    else
                        callPurchaseFinishListener(purchase);
                }
            }
        });

    }

    private void callPurchaseFinishListener(Purchase purchase) {
        String sku_id = purchase.getSku();
        boolean isTest = false;
        if (sku_id.equals(SKU_TEST)) {
            sku_id = purchase.getDeveloperPayload();
            isTest = true;
        }

        purchased_listener.purchased(sku_id, isTest);
    }

    public void restorePurchase(String sku_id, boolean isTest) {
        Log.e("purchased", "sku_id:" + sku_id);
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
            if (databaseHelper.updateSQL(ThemeContentTable.TABLE_NAME, values, ThemeContentTable.THEME_CONTENT_ID + " = '" + contentID.get(i) + "' AND "+ThemeContentTable.THEME_CONTENT_STATE+ " < "+Default.STATE_DOWNLOAD, null)) {

            }
        }
        sharedPreferences.edit().putBoolean(Default.IN_APP_PURCHASE, true).commit();

    }

}
