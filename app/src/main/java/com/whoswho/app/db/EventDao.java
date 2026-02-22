package com.whoswho.app.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.whoswho.app.model.Event;

import java.util.ArrayList;
import java.util.List;

public class EventDao {

    private final DatabaseHelper dbHelper;

    public EventDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insert(Event e) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", e.getTitle());
        cv.put("date", e.getDate());
        cv.put("description", e.getDescription());
        cv.put("created_at", e.getCreatedAt());
        return db.insert("events", null, cv);
    }

    public void update(Event e) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", e.getTitle());
        cv.put("date", e.getDate());
        cv.put("description", e.getDescription());
        db.update("events", cv, "id=?", new String[]{String.valueOf(e.getId())});
    }

    public void delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("events", "id=?", new String[]{String.valueOf(id)});
    }

    public Event getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT e.*, (SELECT COUNT(*) FROM event_persons WHERE event_id = e.id) AS person_count " +
                "FROM events e WHERE e.id = ?",
                new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) return fromCursor(c);
            return null;
        } finally {
            c.close();
        }
    }

    public List<Event> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT e.*, (SELECT COUNT(*) FROM event_persons WHERE event_id = e.id) AS person_count " +
                "FROM events e ORDER BY e.created_at DESC",
                null);
        List<Event> list = new ArrayList<>();
        try {
            while (c.moveToNext()) list.add(fromCursor(c));
        } finally {
            c.close();
        }
        return list;
    }

    public void addPerson(long eventId, long personId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("event_id", eventId);
        cv.put("person_id", personId);
        db.insertWithOnConflict("event_persons", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void removePerson(long eventId, long personId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("event_persons", "event_id=? AND person_id=?",
                new String[]{String.valueOf(eventId), String.valueOf(personId)});
    }

    public int getPersonCount(long eventId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM event_persons WHERE event_id=?",
                new String[]{String.valueOf(eventId)});
        try {
            if (c.moveToFirst()) return c.getInt(0);
            return 0;
        } finally {
            c.close();
        }
    }

    private Event fromCursor(Cursor c) {
        Event e = new Event();
        e.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        e.setTitle(c.getString(c.getColumnIndexOrThrow("title")));
        e.setDate(c.getLong(c.getColumnIndexOrThrow("date")));
        e.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
        e.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        int pcIdx = c.getColumnIndex("person_count");
        if (pcIdx >= 0) e.setPersonCount(c.getInt(pcIdx));
        return e;
    }
}
