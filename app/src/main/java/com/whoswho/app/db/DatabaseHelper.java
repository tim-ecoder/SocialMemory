package com.whoswho.app.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "whoswho.db";
    private static final int DB_VERSION = 1;

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE persons (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "first_name TEXT NOT NULL," +
                "last_name TEXT," +
                "photo_path TEXT NOT NULL," +
                "company TEXT," +
                "position TEXT," +
                "context TEXT," +
                "note TEXT," +
                "hobbies TEXT," +
                "interests TEXT," +
                "family_status TEXT," +
                "partner_name TEXT," +
                "children_names TEXT," +
                "pet_names TEXT," +
                "religion TEXT," +
                "political_views TEXT," +
                "created_at INTEGER," +
                "photo_updated_at INTEGER" +
                ")");

        db.execSQL("CREATE TABLE events (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "date INTEGER," +
                "description TEXT," +
                "created_at INTEGER" +
                ")");

        db.execSQL("CREATE TABLE event_persons (" +
                "event_id INTEGER NOT NULL," +
                "person_id INTEGER NOT NULL," +
                "PRIMARY KEY (event_id, person_id)," +
                "FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE," +
                "FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE quiz_stats (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "person_id INTEGER NOT NULL," +
                "event_id INTEGER NOT NULL," +
                "correct_count INTEGER DEFAULT 0," +
                "wrong_count INTEGER DEFAULT 0," +
                "last_shown_at INTEGER," +
                "weight REAL DEFAULT 1.0," +
                "FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE," +
                "FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE," +
                "UNIQUE (person_id, event_id)" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future migrations go here
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.execSQL("PRAGMA foreign_keys = ON");
    }
}
