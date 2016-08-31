package ca.itquality.patrol.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.atomic.AtomicInteger;

import ca.itquality.patrol.library.util.Util;

public class DatabaseManager {

    // Constants

    /* Database. */
    private static final String DATABASE_NAME = "Patrol";
    private static final int DATABASE_VERSION = 3;

    /* Heart rate table. */
    public static final String HEART_RATE_TABLE = "HeartRate";
    public static final String HEART_RATE_TIME_COLUMN = "Time";
    public static final String HEART_RATE_VALUE_COLUMN = "Value";
    public static final String HEART_RATE_IS_SENT_COLUMN = "IsSent";

    // Usual variables
    private static final String CREATE_TABLE_HEART_RATE = "CREATE TABLE "
            + HEART_RATE_TABLE + " ("
            + HEART_RATE_TIME_COLUMN + " INTEGER PRIMARY KEY, "
            + HEART_RATE_VALUE_COLUMN + " INTEGER,"
            + HEART_RATE_IS_SENT_COLUMN + " INTEGER)";

    private AtomicInteger mOpenCounter = new AtomicInteger();

    private static DatabaseManager instance;
    private SQLiteOpenHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    private DatabaseManager(SQLiteOpenHelper helper) {
        mDatabaseHelper = helper;
    }

    public static synchronized void initializeInstance(SQLiteOpenHelper helper) {
        if (instance == null) {
            instance = new DatabaseManager(helper);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(DatabaseManager.class.getSimpleName() +
                    " is not initialized, call initializeInstance(..) method first.");
        }

        return instance;
    }

    public synchronized SQLiteDatabase openDatabase() {
        if (mOpenCounter.incrementAndGet() == 1) {
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        if (mOpenCounter.decrementAndGet() == 0) {
            // Closing database
            mDatabase.close();

        }
    }

    public static class SitesDatabaseHelper extends SQLiteOpenHelper {

        public SitesDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null,
                    DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_HEART_RATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Logs that the database is being upgraded
            Util.Log("Upgrading database from version "
                    + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");

            // Kills the tables and existing data
            db.execSQL("DROP TABLE IF EXISTS " + HEART_RATE_TABLE);
            // Recreates the database with a new version
            onCreate(db);
        }
    }

}
