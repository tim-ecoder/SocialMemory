package com.whoswho.app.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.whoswho.app.model.Person;

import java.util.ArrayList;
import java.util.List;

public class PersonDao {

    private final DatabaseHelper dbHelper;

    public PersonDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insert(Person p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = toContentValues(p);
        return db.insert("persons", null, cv);
    }

    public void update(Person p) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = toContentValues(p);
        db.update("persons", cv, "id=?", new String[]{String.valueOf(p.getId())});
    }

    public void delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("persons", "id=?", new String[]{String.valueOf(id)});
    }

    public Person getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("persons", null, "id=?", new String[]{String.valueOf(id)},
                null, null, null);
        try {
            if (c.moveToFirst()) return fromCursor(c);
            return null;
        } finally {
            c.close();
        }
    }

    public List<Person> getAll() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("persons", null, null, null, null, null, "first_name ASC");
        List<Person> list = new ArrayList<>();
        try {
            while (c.moveToNext()) list.add(fromCursor(c));
        } finally {
            c.close();
        }
        return list;
    }

    public List<Person> getByEvent(long eventId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT p.* FROM persons p " +
                "INNER JOIN event_persons ep ON p.id = ep.person_id " +
                "WHERE ep.event_id = ? ORDER BY p.first_name ASC",
                new String[]{String.valueOf(eventId)});
        List<Person> list = new ArrayList<>();
        try {
            while (c.moveToNext()) list.add(fromCursor(c));
        } finally {
            c.close();
        }
        return list;
    }

    public List<Person> getNotInEvent(long eventId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT p.* FROM persons p " +
                "WHERE p.id NOT IN (SELECT person_id FROM event_persons WHERE event_id = ?) " +
                "ORDER BY p.first_name ASC",
                new String[]{String.valueOf(eventId)});
        List<Person> list = new ArrayList<>();
        try {
            while (c.moveToNext()) list.add(fromCursor(c));
        } finally {
            c.close();
        }
        return list;
    }

    public List<Person> search(String query) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String like = "%" + query + "%";
        Cursor c = db.rawQuery(
                "SELECT * FROM persons WHERE first_name LIKE ? OR last_name LIKE ? OR company LIKE ? " +
                "ORDER BY first_name ASC",
                new String[]{like, like, like});
        List<Person> list = new ArrayList<>();
        try {
            while (c.moveToNext()) list.add(fromCursor(c));
        } finally {
            c.close();
        }
        return list;
    }

    private ContentValues toContentValues(Person p) {
        ContentValues cv = new ContentValues();
        cv.put("first_name", p.getFirstName());
        cv.put("last_name", p.getLastName());
        cv.put("photo_path", p.getPhotoPath());
        cv.put("company", p.getCompany());
        cv.put("position", p.getPosition());
        cv.put("context", p.getContext());
        cv.put("note", p.getNote());
        cv.put("hobbies", p.getHobbies());
        cv.put("interests", p.getInterests());
        cv.put("family_status", p.getFamilyStatus());
        cv.put("partner_name", p.getPartnerName());
        cv.put("children_names", p.getChildrenNames());
        cv.put("pet_names", p.getPetNames());
        cv.put("religion", p.getReligion());
        cv.put("political_views", p.getPoliticalViews());
        cv.put("created_at", p.getCreatedAt());
        cv.put("photo_updated_at", p.getPhotoUpdatedAt());
        return cv;
    }

    private Person fromCursor(Cursor c) {
        Person p = new Person();
        p.setId(c.getLong(c.getColumnIndexOrThrow("id")));
        p.setFirstName(c.getString(c.getColumnIndexOrThrow("first_name")));
        p.setLastName(c.getString(c.getColumnIndexOrThrow("last_name")));
        p.setPhotoPath(c.getString(c.getColumnIndexOrThrow("photo_path")));
        p.setCompany(c.getString(c.getColumnIndexOrThrow("company")));
        p.setPosition(c.getString(c.getColumnIndexOrThrow("position")));
        p.setContext(c.getString(c.getColumnIndexOrThrow("context")));
        p.setNote(c.getString(c.getColumnIndexOrThrow("note")));
        p.setHobbies(c.getString(c.getColumnIndexOrThrow("hobbies")));
        p.setInterests(c.getString(c.getColumnIndexOrThrow("interests")));
        p.setFamilyStatus(c.getString(c.getColumnIndexOrThrow("family_status")));
        p.setPartnerName(c.getString(c.getColumnIndexOrThrow("partner_name")));
        p.setChildrenNames(c.getString(c.getColumnIndexOrThrow("children_names")));
        p.setPetNames(c.getString(c.getColumnIndexOrThrow("pet_names")));
        p.setReligion(c.getString(c.getColumnIndexOrThrow("religion")));
        p.setPoliticalViews(c.getString(c.getColumnIndexOrThrow("political_views")));
        p.setCreatedAt(c.getLong(c.getColumnIndexOrThrow("created_at")));
        p.setPhotoUpdatedAt(c.getLong(c.getColumnIndexOrThrow("photo_updated_at")));
        return p;
    }
}
