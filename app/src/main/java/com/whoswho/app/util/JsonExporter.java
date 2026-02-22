package com.whoswho.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;

import com.whoswho.app.db.DatabaseHelper;
import com.whoswho.app.db.EventDao;
import com.whoswho.app.db.PersonDao;
import com.whoswho.app.model.Event;
import com.whoswho.app.model.Person;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Exports and imports all app data (persons, events, event-person links)
 * as a JSON file with embedded Base64 photos.
 */
public class JsonExporter {

    private static final String EXPORT_DIR = "WhosWho";
    private static final int PHOTO_QUALITY = 80;

    // -------------------------------------------------------------------------
    // Export
    // -------------------------------------------------------------------------

    /**
     * Exports all data to a JSON file in the external storage directory.
     *
     * @return The absolute path of the exported file, or null on error.
     */
    public static String exportData(Context ctx) {
        try {
            DatabaseHelper dbHelper = DatabaseHelper.getInstance(ctx);
            PersonDao personDao = new PersonDao(dbHelper);
            EventDao eventDao = new EventDao(dbHelper);

            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("exported_at", System.currentTimeMillis());

            // Export persons
            List<Person> persons = personDao.getAll();
            JSONArray personsArray = new JSONArray();
            for (Person p : persons) {
                JSONObject pj = personToJson(p);
                personsArray.put(pj);
            }
            root.put("persons", personsArray);

            // Export events
            List<Event> events = eventDao.getAll();
            JSONArray eventsArray = new JSONArray();
            for (Event e : events) {
                JSONObject ej = eventToJson(e);

                // Export event-person links as person IDs
                List<Person> eventPersons = personDao.getByEvent(e.getId());
                JSONArray personIds = new JSONArray();
                for (Person ep : eventPersons) {
                    personIds.put(ep.getId());
                }
                ej.put("person_ids", personIds);

                eventsArray.put(ej);
            }
            root.put("events", eventsArray);

            // Write to file
            String json = root.toString(2);
            File exportFile = getExportFile(ctx);
            if (exportFile == null) return null;

            FileOutputStream fos = new FileOutputStream(exportFile);
            try {
                fos.write(json.getBytes("UTF-8"));
            } finally {
                fos.close();
            }

            return exportFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Import
    // -------------------------------------------------------------------------

    /**
     * Imports data from a JSON file, replacing all existing data.
     *
     * @param filePath Path to the JSON file.
     * @return true on success.
     */
    public static boolean importData(Context ctx, String filePath) {
        try {
            String json = readFileAsString(filePath);
            if (json == null) return false;

            JSONObject root = new JSONObject(json);

            DatabaseHelper dbHelper = DatabaseHelper.getInstance(ctx);
            PersonDao personDao = new PersonDao(dbHelper);
            EventDao eventDao = new EventDao(dbHelper);

            // Clear existing data
            for (Event e : eventDao.getAll()) {
                eventDao.delete(e.getId());
            }
            for (Person p : personDao.getAll()) {
                personDao.delete(p.getId());
            }

            // Import persons — track old ID -> new ID mapping
            Map<Long, Long> personIdMap = new HashMap<>();
            JSONArray personsArray = root.optJSONArray("persons");
            if (personsArray != null) {
                for (int i = 0; i < personsArray.length(); i++) {
                    JSONObject pj = personsArray.getJSONObject(i);
                    long oldId = pj.optLong("id", -1);
                    Person p = jsonToPerson(pj, ctx);
                    long newId = personDao.insert(p);
                    if (oldId > 0 && newId > 0) {
                        personIdMap.put(oldId, newId);
                    }
                }
            }

            // Import events and re-link persons
            JSONArray eventsArray = root.optJSONArray("events");
            if (eventsArray != null) {
                for (int i = 0; i < eventsArray.length(); i++) {
                    JSONObject ej = eventsArray.getJSONObject(i);
                    Event e = jsonToEvent(ej);
                    long newEventId = eventDao.insert(e);

                    // Re-link persons
                    JSONArray personIds = ej.optJSONArray("person_ids");
                    if (personIds != null && newEventId > 0) {
                        for (int j = 0; j < personIds.length(); j++) {
                            long oldPersonId = personIds.getLong(j);
                            Long newPersonId = personIdMap.get(oldPersonId);
                            if (newPersonId != null) {
                                eventDao.addPerson(newEventId, newPersonId);
                            }
                        }
                    }
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // JSON <-> Model conversion
    // -------------------------------------------------------------------------

    private static JSONObject personToJson(Person p) throws JSONException {
        JSONObject j = new JSONObject();
        j.put("id", p.getId());
        j.put("first_name", p.getFirstName());
        j.put("last_name", nullSafe(p.getLastName()));
        j.put("company", nullSafe(p.getCompany()));
        j.put("position", nullSafe(p.getPosition()));
        j.put("context", nullSafe(p.getContext()));
        j.put("note", nullSafe(p.getNote()));
        j.put("hobbies", nullSafe(p.getHobbies()));
        j.put("interests", nullSafe(p.getInterests()));
        j.put("family_status", nullSafe(p.getFamilyStatus()));
        j.put("partner_name", nullSafe(p.getPartnerName()));
        j.put("children_names", nullSafe(p.getChildrenNames()));
        j.put("pet_names", nullSafe(p.getPetNames()));
        j.put("religion", nullSafe(p.getReligion()));
        j.put("political_views", nullSafe(p.getPoliticalViews()));
        j.put("created_at", p.getCreatedAt());
        j.put("photo_updated_at", p.getPhotoUpdatedAt());

        // Embed photo as Base64
        String photoBase64 = encodePhotoBase64(p.getPhotoPath());
        if (photoBase64 != null) {
            j.put("photo_base64", photoBase64);
        }

        return j;
    }

    private static Person jsonToPerson(JSONObject j, Context ctx) throws JSONException {
        Person p = new Person();
        p.setFirstName(j.optString("first_name", ""));
        p.setLastName(j.optString("last_name", ""));
        p.setCompany(j.optString("company", ""));
        p.setPosition(j.optString("position", ""));
        p.setContext(j.optString("context", ""));
        p.setNote(j.optString("note", ""));
        p.setHobbies(j.optString("hobbies", ""));
        p.setInterests(j.optString("interests", ""));
        p.setFamilyStatus(j.optString("family_status", ""));
        p.setPartnerName(j.optString("partner_name", ""));
        p.setChildrenNames(j.optString("children_names", ""));
        p.setPetNames(j.optString("pet_names", ""));
        p.setReligion(j.optString("religion", ""));
        p.setPoliticalViews(j.optString("political_views", ""));
        p.setCreatedAt(j.optLong("created_at", System.currentTimeMillis()));
        p.setPhotoUpdatedAt(j.optLong("photo_updated_at", System.currentTimeMillis()));

        // Decode photo from Base64
        String photoBase64 = j.optString("photo_base64", null);
        if (photoBase64 != null && !photoBase64.isEmpty()) {
            String photoPath = decodePhotoBase64(ctx, photoBase64,
                    "import_" + System.currentTimeMillis() + "_" + p.getFirstName());
            p.setPhotoPath(photoPath);
        } else {
            p.setPhotoPath(j.optString("photo_path", ""));
        }

        return p;
    }

    private static JSONObject eventToJson(Event e) throws JSONException {
        JSONObject j = new JSONObject();
        j.put("id", e.getId());
        j.put("title", e.getTitle());
        j.put("date", e.getDate());
        j.put("description", nullSafe(e.getDescription()));
        j.put("created_at", e.getCreatedAt());
        return j;
    }

    private static Event jsonToEvent(JSONObject j) {
        Event e = new Event();
        e.setTitle(j.optString("title", ""));
        e.setDate(j.optLong("date", 0));
        e.setDescription(j.optString("description", ""));
        e.setCreatedAt(j.optLong("created_at", System.currentTimeMillis()));
        return e;
    }

    // -------------------------------------------------------------------------
    // Photo Base64 helpers
    // -------------------------------------------------------------------------

    private static String encodePhotoBase64(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) return null;
        File file = new File(photoPath);
        if (!file.exists()) return null;

        try {
            Bitmap bm = BitmapFactory.decodeFile(photoPath);
            if (bm == null) return null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, baos);
            bm.recycle();
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    private static String decodePhotoBase64(Context ctx, String base64, String nameHint) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bm == null) return null;

            String filename = nameHint.replaceAll("[^a-zA-Z0-9_]", "_") + ".jpg";
            return ImageUtils.saveBitmap(ctx, bm, filename);
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // File helpers
    // -------------------------------------------------------------------------

    private static File getExportFile(Context ctx) {
        File dir = new File(Environment.getExternalStorageDirectory(), EXPORT_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            // Fall back to app-specific external dir
            dir = ctx.getExternalFilesDir(null);
            if (dir == null) dir = ctx.getFilesDir();
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(dir, "whoswho_backup_" + timestamp + ".json");
    }

    /**
     * Returns the default export directory path for file picker.
     */
    public static String getExportDirPath(Context ctx) {
        File dir = new File(Environment.getExternalStorageDirectory(), EXPORT_DIR);
        if (!dir.exists()) {
            dir = ctx.getExternalFilesDir(null);
            if (dir == null) dir = ctx.getFilesDir();
        }
        return dir.getAbsolutePath();
    }

    private static String readFileAsString(String path) {
        try {
            File file = new File(path);
            byte[] bytes = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            try {
                fis.read(bytes);
            } finally {
                fis.close();
            }
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}
