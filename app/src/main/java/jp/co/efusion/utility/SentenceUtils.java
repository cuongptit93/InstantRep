package jp.co.efusion.utility;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jp.co.efusion.database.DatabaseHelper;
import jp.co.efusion.database.SentenceSetTable;
import jp.co.efusion.database.SentenceTable;
import jp.co.efusion.database.ThemeContentTable;
import jp.co.efusion.listhelper.Log;

/**
 * Created by anhdt on 8/31/16.
 */
public class SentenceUtils {

    public static String getPurchaseContentIds(DatabaseHelper databaseHelper, String themeContentId) {
        String contentIdsStr = null;
        List<Integer> contentIds = getListPurchaseContentIds(databaseHelper, themeContentId);
        if (contentIds != null && contentIds.size() > 0) {
            contentIdsStr = "";
            for (int contentId : contentIds) {
                contentIdsStr += String.valueOf(contentId) + ",";
            }
            if (contentIdsStr != null && !contentIdsStr.equals("")) {
                contentIdsStr = contentIdsStr.substring(0, contentIdsStr.length() - 1);
                contentIdsStr = "(" + contentIdsStr + ")";
            }
        }
        return contentIdsStr;
    }

    public static List<Integer> getListPurchaseContentIds(DatabaseHelper databaseHelper, String themeContentId) {
        String queryStr = "select * from " + ThemeContentTable.TABLE_NAME
                + " where " + ThemeContentTable.THEME_CONTENT_STATE + " = " + Default.STATE_READY_TO_USE
                + " and " + ThemeContentTable.THEME_ID + " = '" + themeContentId + "'"
                + " and " + ThemeContentTable.THEME_CONTENT_ID + " != 1"
                + " and " + ThemeContentTable.THEME_CONTENT_ID + " != 10;";

        Cursor tmpCursor = null;
        List<Integer> contentIds = null;
        try {
            tmpCursor = databaseHelper.getQueryResultData(queryStr);
            if (tmpCursor != null) {
                contentIds = new ArrayList<>();
                while (tmpCursor.moveToNext()) {
                    contentIds.add(tmpCursor.getInt(tmpCursor.getColumnIndex(ThemeContentTable.THEME_CONTENT_ID)));
                }
            }

        } catch (Exception e) {
        } finally {
            if (tmpCursor != null)
                tmpCursor.close();
        }

        return contentIds;
    }

    /**
     * Check purchased content is exist or not
     *
     * @param context
     * @return
     */
    public static boolean havePurchaseContent(Context context) {
        String queryStr = "select * from " + ThemeContentTable.TABLE_NAME
                + " where " + ThemeContentTable.THEME_CONTENT_STATE + " = " + Default.STATE_READY_TO_USE
                + " and " + ThemeContentTable.THEME_CONTENT_ID + " != 1"
                + " and " + ThemeContentTable.THEME_CONTENT_ID + " != 10;";

        Cursor tmpCursor = null;
        try {
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            //open database
            databaseHelper.openDatabase();

            tmpCursor = databaseHelper.getQueryResultData(queryStr);
            if (tmpCursor != null && tmpCursor.moveToFirst())
                return true;

        } catch (Exception e) {
        } finally {
            if (tmpCursor != null)
                tmpCursor.close();
        }

        return false;
    }

    public static List<Integer> getSentenceIDs(DatabaseHelper databaseHelper, int sentenceSetId,
                                               boolean isShuffled) {
        Cursor cursor = null;
        List<Integer> sentenceIds = null;

        try {
            cursor = databaseHelper.getQueryResultData(SentenceTable.TABLE_NAME,
                    null, SentenceTable.SET_ID + " = '" + sentenceSetId + "'",
                    null, null, null, SentenceTable.SENTENCE_NO + " ASC ", null);

            if (cursor != null) {
                sentenceIds = new ArrayList<>();
                while (cursor.moveToNext()) {
                    sentenceIds.add(cursor.getInt(cursor.getColumnIndex(SentenceTable.SENTENCE_ID)));
                }

                if (isShuffled) {
                    sentenceIds = shuffleSentenceArray(sentenceIds);
                }
            }
        } catch (Exception e) {
            Log.e("getSentenceIDs", "error " + e);
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return sentenceIds;
    }

    public static final List<Integer> shuffleSentenceArray(List<Integer> mysentenceList) {
        ArrayList<Integer> shuffledArrayList = new ArrayList<Integer>();
        shuffledArrayList.addAll(mysentenceList);
        Random rnd = new Random();
        for (int i = shuffledArrayList.size() - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            // Simple swap
            int a = shuffledArrayList.get(index);
            shuffledArrayList.set(index, shuffledArrayList.get(i));
            shuffledArrayList.set(i, a);
        }
        return shuffledArrayList;
    }

    public static String getSentenceSetTitle(DatabaseHelper databaseHelper, int sentenceSetId) {
        Cursor cursor = null;

        try {
            cursor = databaseHelper.getQueryResultData(SentenceSetTable.TABLE_NAME, null,
                    SentenceSetTable.SET_ID + " = " + sentenceSetId, null, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(SentenceSetTable.SET_TITLE));
                }
            }
        } catch (Exception e) {
            cursor = null;
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;

    }

    public static List<Integer> getPurchasedSetIds(DatabaseHelper databaseHelper, String themeId) {
        String contentIds = getPurchaseContentIds(databaseHelper, themeId);
        if (contentIds != null) {
            String selectionStr = SentenceSetTable.THEME_CONTENT_ID + " in " + contentIds;
            Cursor cursor = null;
            try {
                cursor = databaseHelper.getQueryResultData(SentenceSetTable.TABLE_NAME, null,
                        selectionStr,
                        null, null, null, null, null);
                if (cursor != null) {
                    List<Integer> results = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        results.add(cursor.getInt(cursor.getColumnIndexOrThrow(SentenceSetTable.SET_ID)));
                    }

                    return results;
                }
            } catch (Exception e) {

            } finally {
                if (cursor != null)
                    cursor.close();
            }
        }

        return null;
    }

    public static int getContenID(DatabaseHelper databaseHelper, int sentenceSetId) {
        Cursor cursor = null;

        try {
            cursor = databaseHelper.getQueryResultData(SentenceTable.TABLE_NAME, null,
                    SentenceTable.SET_ID + " = " + sentenceSetId, null, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(cursor.getColumnIndexOrThrow(SentenceTable.CONTENT_ID));
                }
            }
        } catch (Exception e) {
            cursor = null;
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return 0;

    }
}
