package ca.itquality.patrol.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import ca.itquality.patrol.library.util.Util;

public class DatabaseManager {

    // Constants

    /* Database. */
    private static final String DATABASE_NAME = "Patrol";
    private static final int DATABASE_VERSION = 3;

    /* Steps table. */
    public static final String STEPS_TABLE = "Steps";
    public static final String STEPS_TIME_COLUMN = "Time";
    public static final String STEPS_VALUE_COLUMN = "Value";
    public static final String STEPS_IS_SENT_COLUMN = "IsSent";

    /* Activity table. */
    public static final String ACTIVITY_TABLE = "Activity";
    public static final String ACTIVITY_TIME_COLUMN = "Time";
    public static final String ACTIVITY_VALUE_COLUMN = "Value";
    public static final String ACTIVITY_IS_SENT_COLUMN = "IsSent";

    // Usual variables
    private static final String CREATE_TABLE_STEPS = "CREATE TABLE "
            + STEPS_TABLE + " ("
            + STEPS_TIME_COLUMN + " INTEGER PRIMARY KEY, "
            + STEPS_VALUE_COLUMN + " INTEGER,"
            + STEPS_IS_SENT_COLUMN + " INTEGER)";
    private static final String CREATE_TABLE_ACTIVITY = "CREATE TABLE "
            + ACTIVITY_TABLE + " ("
            + ACTIVITY_TIME_COLUMN + " INTEGER PRIMARY KEY, "
            + ACTIVITY_VALUE_COLUMN + " TEXT,"
            + ACTIVITY_IS_SENT_COLUMN + " INTEGER)";

    private int mOpenCounter;

    private static DatabaseManager instance;
    private static SQLiteOpenHelper mDatabaseHelper;
    private SQLiteDatabase mDatabase;

    public static synchronized void initializeInstance(SQLiteOpenHelper helper) {
        if (instance == null) {
            instance = new DatabaseManager();
            mDatabaseHelper = helper;
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
        mOpenCounter++;
        if(mOpenCounter == 1) {
            // Opening new database
            mDatabase = mDatabaseHelper.getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDatabase() {
        mOpenCounter--;
        if(mOpenCounter == 0) {
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
            db.execSQL(CREATE_TABLE_STEPS);
            db.execSQL(CREATE_TABLE_ACTIVITY);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Logs that the database is being upgraded
            Util.Log("Upgrading database from version "
                    + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");

            // Kills the tables and existing data
            db.execSQL("DROP TABLE IF EXISTS " + STEPS_TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + ACTIVITY_TABLE);
            // Recreates the database with a new version
            onCreate(db);
        }
    }

}
