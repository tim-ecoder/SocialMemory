# Who's Who — Android App (Claude Code Build Guide)

## Quick Start

```bash
bash build.sh    # Downloads deps (cached), compiles, builds APK
# Output: whoswho-debug.apk (~1.7 MB)
```

## Critical Build Environment Info

### Network Restrictions
- **maven.google.com is BLOCKED** by proxy (403)
- **jitpack.io is BLOCKED** by proxy (403)
- **All Chinese Maven mirrors BLOCKED** (Aliyun, Tencent, Huawei)
- **ALLOWED:** `raw.githubusercontent.com`, `repo1.maven.org`, `*.googleapis.com`

### AndroidX/Material Source — dandar3 GitHub Repos
All AndroidX and Material JARs are downloaded from GitHub user **dandar3** who maintains
pre-built JARs of the entire AndroidX ecosystem:
```
https://raw.githubusercontent.com/dandar3/android-{repo-name}/{branch}/libs/{jar-name}.jar
```
This is the ONLY way to get AndroidX in this environment. See `build.sh` for all 36 URLs.

### Build Tools (Debian Android SDK)
```
SDK=/usr/lib/android-sdk
android.jar = API 23 (platforms/android-23/android.jar)
aapt, dx, zipalign, apksigner from build-tools/debian/
javac = system Java
```

### Important Build Quirks
1. **Java source uses `android.app.Fragment`** (not `androidx.fragment`), because we compile
   against API 23 `android.jar`. AndroidX JARs are only on classpath for Material widgets.
2. **No Gradle** — everything is built via shell script (`build.sh`)
3. **`${applicationId}` in AndroidManifest.xml** is patched by build.sh at build time
4. **META-INF/versions** must be stripped from JARs before dx (build.sh handles this)
5. **String resources**: Use positional format `%1$d / %2$d` (NOT `%d / %d`) for multiple substitutions
6. **"zip error: Nothing to do!"** warnings during build are harmless — ignore them
7. **Keystore** is auto-generated debug keystore, reused across builds

## Project Structure

```
com.whoswho.app/
├── MainActivity.java              # Activity + Fragment navigation (back stack)
├── db/
│   ├── DatabaseHelper.java        # SQLiteOpenHelper (4 tables, singleton)
│   ├── PersonDao.java             # CRUD + getByEvent, getNotInEvent, search
│   ├── EventDao.java              # CRUD + addPerson/removePerson
│   └── QuizStatsDao.java          # Adaptive quiz weights
├── model/
│   ├── Person.java                # 18 fields (name, photo, company, hobbies, etc.)
│   ├── Event.java                 # title, date, description, personCount
│   └── QuizResult.java            # totalQuestions, correctAnswers, mistakePeople
├── ui/
│   ├── splash/SplashActivity.java # 1.5s splash → MainActivity
│   ├── events/
│   │   ├── EventListFragment.java # ListView + create/edit/delete events
│   │   └── EventAdapter.java      # BaseAdapter with ViewHolder
│   ├── event/
│   │   ├── EventDetailFragment.java # Event header + people list + add/quiz buttons
│   │   └── PersonAdapter.java       # Person list items (photo + name + company)
│   ├── person/
│   │   ├── PersonEditFragment.java  # All person fields + camera/gallery photo
│   │   └── PhotoHelper.java         # Camera/gallery intents + bitmap processing
│   ├── quiz/
│   │   ├── QuizEngine.java          # 5 modes, weighted random, distractors
│   │   ├── QuizFragment.java        # Quiz UI (text answers + photo grid answers)
│   │   └── QuizResultFragment.java  # Score + mistake list + retry buttons
│   └── settings/
│       └── SettingsFragment.java    # Export/import buttons
└── util/
    ├── ImageUtils.java            # LruCache, loadScaled, getCircularBitmap, saveBitmap
    └── JsonExporter.java          # Export/import JSON with Base64 photos
```

## Database Schema (SQLite)

4 tables: `persons` (18 columns), `events` (5 columns), `event_persons` (M:N link),
`quiz_stats` (adaptive weights per person per event).

Foreign keys ON. Cascade deletes.

## Navigation Flow

```
SplashActivity (1.5s) → MainActivity
  ├── EventListFragment (home, gear → settings)
  │   ├── tap event → EventDetailFragment
  │   │   ├── tap person → PersonEditFragment (edit)
  │   │   ├── "Add Person" → dialog (create new / pick existing)
  │   │   │   └── "Create new" → PersonEditFragment (create)
  │   │   └── "Start Training" → QuizFragment
  │   │       └── done → QuizResultFragment
  │   │           ├── "Retry All" → QuizFragment
  │   │           ├── "Retry Mistakes" → QuizFragment (filtered)
  │   │           └── "Back to Event" → pop to EventDetailFragment
  │   └── gear → SettingsFragment (export/import)
```

## Quiz Modes (QuizEngine)
0. MODE_PHOTO_TO_NAME — show photo, pick name from 4 buttons
1. MODE_DESC_TO_PHOTO — show description text, pick photo from 4 images
2. MODE_NAME_TO_POSITION — show name, pick position from 4 buttons
3. MODE_FACT_TO_PHOTO — show hobby/interest, pick photo from 4 images
4. MODE_COMPANY_TO_PHOTO — show company name, pick photo from 4 images

Adaptive weight formula: `weight = wrong_count / (correct_count + 1) + 0.5`

## Colors (colors.xml)
- primary: #1565C0 (blue 800)
- primary_dark: #0D47A1
- secondary: #00897B (teal 600)
- background: #FAFAFA
- surface: #FFFFFF
- on_surface: #212121
- splash_background: #1565C0

## Key Implementation Notes

1. **PersonEditFragment** uses `newInstance(long personId, long eventId)` — personId=-1 for new
2. **QuizFragment** uses `newInstance(long eventId)` and `newInstanceForPeople(long eventId, long[] personIds)` for retry with specific people
3. **EventDetailFragment** requires minimum 5 people to enable "Start Training" button
4. **Export** saves to `/sdcard/WhosWho/whoswho_backup_TIMESTAMP.json` with Base64 photos
5. **Import** replaces all data, remaps person IDs for event links
6. **ImageUtils.saveBitmap()** saves to `{filesDir}/photos/` as JPEG
7. **PhotoHelper.processPhoto()** scales to maxSize with 2-pass decode (bounds first, then sample)

## Reference Project
This build approach was adapted from `tim-ecoder/KeyoneKB` branch `claude/build-signed-apk-FuujH`
which used the same dandar3 dependency pattern in its `build_ci.sh`.
