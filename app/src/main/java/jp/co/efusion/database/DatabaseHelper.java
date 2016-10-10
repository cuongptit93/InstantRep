package jp.co.efusion.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by xor2 on 12/7/15.
 * SQLiteDatabse for all kinds of database operation
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private final Context myContext;
    private static final String DATABASE_NAME = "an_instant_reply_db.sqlite";
    public static String DATABASE_PATH = "";
    public static final int DATABASE_VERSION = 1;
    public SQLiteDatabase sqLiteDatabase;

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            //here do anything while update dsatabase
            //deleteDatabase();
        }
    }

    //delete database
    public void deleteDatabase() {
        File file = new File(DATABASE_PATH);
        if (file.exists()) {
            file.delete();
            System.out.println("delete database file.");
        }
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.myContext = context;
        DATABASE_PATH = myContext.getDatabasePath(DATABASE_NAME).toString();
        //try to create if not create
        try {
            createDatabase();
        } catch (IOException e) {

        }
    }

    /*
    Create a empty database on the system if not create
     */
    private void createDatabase() throws IOException {

        if (checkDataBaseExist()) {
            //database exist try to update if new version available
            //onUpgrade(myDataBase, DATABASE_VERSION_old, DATABASE_VERSION);
        } else {
            //database file not exist, So copy this from application asset folder
            this.getReadableDatabase();
            try {
                this.close();

                OutputStream myOutput = new FileOutputStream(DATABASE_PATH);
                InputStream myInput = myContext.getAssets().open(DATABASE_NAME);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = myInput.read(buffer)) > 0) {
                    myOutput.write(buffer, 0, length);
                }
                myInput.close();
                myOutput.flush();
                myOutput.close();

            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    /*
    Check database already exist or not
    @return boolean true if exist file ,otherwise false
     */
    private boolean checkDataBaseExist() {
        return new File(DATABASE_PATH).exists();
    }

    /*
    Open database for SQL Operation
     */
    public void openDatabase() throws SQLException {
        //try to create if not create
        try {
            createDatabase();
        } catch (IOException e) {

        }
        sqLiteDatabase = SQLiteDatabase.openDatabase(DATABASE_PATH, null, SQLiteDatabase.OPEN_READWRITE);
    }

    /*
    *Close Database while not use & need
     */
    public synchronized void closeDataBase() throws SQLException {
        if (sqLiteDatabase != null) {
            sqLiteDatabase.close();
        }
        super.close();
    }

    /**
     * Execute SELECT query in to database
     *
     * @param all sql param
     * @return Cursor query result containg data set
     */
    public Cursor getQueryResultData(String tableName, String[] collumn, String selection, String[] sectionArgs,
                                     String groupBy, String having, String orderBy, String limit) {
        if (sqLiteDatabase != null && sqLiteDatabase.isOpen())
            return sqLiteDatabase.query(tableName, collumn, selection, sectionArgs, groupBy, having, orderBy, limit);

        return null;
    }

    /**
     * Execute raw query
     *
     * @param rawQuery
     * @return
     */
    public Cursor getQueryResultData(String rawQuery) {
        if (sqLiteDatabase != null && sqLiteDatabase.isOpen())
            return sqLiteDatabase.rawQuery(rawQuery, null);

        return null;
    }

    /**
     * @param tableName
     * @param nullColumnHack
     * @param values
     * @return
     */
    public Boolean insertSQL(String tableName, String nullColumnHack, ContentValues values) {
        if (sqLiteDatabase == null || !sqLiteDatabase.isOpen())
            return false;

        long i = sqLiteDatabase.insert(tableName, nullColumnHack, values);
        if (i != -1) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean updateSQL(String tableName, ContentValues values, String whereClause, String[] whereArgs) {
        if (sqLiteDatabase == null || !sqLiteDatabase.isOpen())
            return false;

        int i = sqLiteDatabase.update(tableName, values, whereClause, whereArgs);
        if (i > 0) {
            return true;
        } else {
            return false;
        }
    }

    public Boolean deleteSQL(String tableName, String whereClause, String[] whereArgs) {
        if (sqLiteDatabase == null || !sqLiteDatabase.isOpen())
            return false;
        
        int i = sqLiteDatabase.delete(tableName, whereClause, whereArgs);
        if (i != 0) {
            return true;
        } else {
            return false;
        }
    }
}
