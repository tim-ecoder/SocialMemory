package com.whoswho.app.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;

public class QuizStatsDao {

    private final DatabaseHelper dbHelper;

    public QuizStatsDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Returns weight map: personId -> weight (higher = show more often)
     */
    public Map<Long, Double> getWeightsForEvent(long eventId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Map<Long, Double> weights = new HashMap<>();
        Cursor c = db.query("quiz_stats", new String[]{"person_id", "weight"},
                "event_id=?", new String[]{String.valueOf(eventId)},
                null, null, null);
        try {
            while (c.moveToNext()) {
                weights.put(c.getLong(0), c.getDouble(1));
            }
        } finally {
            c.close();
        }
        return weights;
    }

    /**
     * Record a quiz answer. Updates weight based on correct/wrong.
     * Weight formula: wrong_count / (correct_count + 1) + base
     */
    public void recordAnswer(long personId, long eventId, boolean correct) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long now = System.currentTimeMillis();

        // Ensure row exists
        db.execSQL(
                "INSERT OR IGNORE INTO quiz_stats (person_id, event_id, correct_count, wrong_count, weight, last_shown_at) " +
                "VALUES (?, ?, 0, 0, 1.0, ?)",
                new Object[]{personId, eventId, now});

        if (correct) {
            db.execSQL(
                    "UPDATE quiz_stats SET correct_count = correct_count + 1, last_shown_at = ? " +
                    "WHERE person_id = ? AND event_id = ?",
                    new Object[]{now, personId, eventId});
        } else {
            db.execSQL(
                    "UPDATE quiz_stats SET wrong_count = wrong_count + 1, last_shown_at = ? " +
                    "WHERE person_id = ? AND event_id = ?",
                    new Object[]{now, personId, eventId});
        }

        // Recalculate weight
        db.execSQL(
                "UPDATE quiz_stats SET weight = (CAST(wrong_count AS REAL) / (correct_count + 1)) + 0.5 " +
                "WHERE person_id = ? AND event_id = ?",
                new Object[]{personId, eventId});
    }

    public void resetStats(long eventId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("quiz_stats", "event_id=?", new String[]{String.valueOf(eventId)});
    }

    public void resetStatsForPerson(long personId, long eventId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("quiz_stats", "person_id=? AND event_id=?",
                new String[]{String.valueOf(personId), String.valueOf(eventId)});
    }
}
