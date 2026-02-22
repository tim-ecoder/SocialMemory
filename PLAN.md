# ПЛАН РЕАЛИЗАЦИИ — Who's Who (Android MVP)

## Сводка

**Приложение:** Тренажёр для запоминания людей перед мероприятиями
**Платформа:** Android 8.1+ (API 27), только Java
**Хранение:** SQLite (локальная БД, без сервера)
**UI:** Material Design (google-material 1.2.1 + AndroidX)
**Сборка:** aapt + javac + dx + zipalign + apksigner (Debian Android SDK)

---

## Окружение сборки

- `maven.google.com` **недоступен** — но AndroidX/Material доступны через
  GitHub-репозитории **dandar3** (`raw.githubusercontent.com` — разрешён прокси)
- Доступен `android.jar` API 23 + build-tools 29.0.3
- Сборка через shell-скрипт `build.sh` (по аналогии с KeyoneKB `build_ci.sh`)

### Источник зависимостей

Все AndroidX и Material JAR-файлы скачиваются с:
```
https://raw.githubusercontent.com/dandar3/android-{name}/HEAD/libs/{name}.jar
```

### Зависимости (35 JAR-файлов)

**Основные:**
| Библиотека | Версия | Репозиторий dandar3 |
|---|---|---|
| google-material | 1.2.1 | android-google-material |
| androidx-appcompat | 1.2.0 | android-androidx-appcompat |
| androidx-recyclerview | 1.1.0 | android-androidx-recyclerview |
| androidx-cardview | 1.0.0 | android-androidx-cardview |
| androidx-core | 1.8.0 | android-androidx-core |
| androidx-fragment | 1.3.1 | android-androidx-fragment |
| androidx-coordinatorlayout | — | android-androidx-coordinatorlayout |
| androidx-viewpager2 | — | android-androidx-viewpager2 |
| androidx-transition | — | android-androidx-transition |
| gson | 2.10.1 | Maven Central (repo1.maven.org) |

**Транзитивные (AndroidX):**
androidx-activity, androidx-annotation, androidx-annotation-experimental,
androidx-appcompat-resources, androidx-arch-core-common, androidx-arch-core-runtime,
androidx-collection, androidx-concurrent-futures, androidx-cursoradapter,
androidx-customview, androidx-drawerlayout, androidx-interpolator,
androidx-lifecycle-common, androidx-lifecycle-livedata, androidx-lifecycle-livedata-core,
androidx-lifecycle-runtime, androidx-lifecycle-viewmodel, androidx-lifecycle-viewmodel-savedstate,
androidx-loader, androidx-savedstate, androidx-tracing,
androidx-vectordrawable, androidx-vectordrawable-animated, androidx-versionedparcelable,
androidx-viewpager, google-guava-listenablefuture

---

## Доступные компоненты Material Design

Благодаря `google-material:1.2.1` мы можем использовать:
- **MaterialButton**, **MaterialCardView**, **MaterialToolbar**
- **FloatingActionButton** (настоящий FAB)
- **TextInputLayout** + **TextInputEditText** (Material text fields)
- **TabLayout**, **BottomNavigationView**
- **Snackbar**, **MaterialAlertDialogBuilder**
- **ChipGroup** + **Chip** (для тегов: увлечения, интересы)
- **LinearProgressIndicator**, **CircularProgressIndicator**
- **MaterialColors** — цветовая система

Благодаря `androidx-recyclerview:1.1.0`:
- **RecyclerView** + **LinearLayoutManager** / **GridLayoutManager**

Благодаря `androidx-fragment:1.3.1`:
- **FragmentContainerView**, **FragmentTransaction** с анимациями

---

## Архитектура

```
com.whoswho.app/
├── MainActivity.java              # AppCompatActivity + FragmentContainerView
├── db/
│   ├── DatabaseHelper.java        # SQLiteOpenHelper — схема + миграции
│   ├── PersonDao.java             # CRUD для людей
│   ├── EventDao.java              # CRUD для событий
│   └── QuizStatsDao.java          # Статистика для адаптивной тренировки
├── model/
│   ├── Person.java                # Модель «Человек»
│   ├── Event.java                 # Модель «Событие»
│   └── QuizResult.java            # Результат викторины
├── ui/
│   ├── splash/
│   │   └── SplashActivity.java    # Splash-экран
│   ├── events/
│   │   ├── EventListFragment.java # RecyclerView + MaterialCardView
│   │   └── EventAdapter.java      # RecyclerView.Adapter
│   ├── event/
│   │   ├── EventDetailFragment.java # Экран события + список людей
│   │   └── PersonAdapter.java       # RecyclerView.Adapter
│   ├── person/
│   │   ├── PersonEditFragment.java  # TextInputLayout поля
│   │   └── PhotoHelper.java         # Камера / галерея
│   ├── quiz/
│   │   ├── QuizFragment.java        # Основной экран викторины
│   │   ├── QuizEngine.java          # Логика вопросов + адаптивный алгоритм
│   │   └── QuizResultFragment.java  # Экран результата
│   └── settings/
│       └── ExportImportFragment.java # Экспорт/импорт базы
├── util/
│   ├── ImageCache.java            # LruCache для фото
│   ├── ImageUtils.java            # Ресайз, кроп, сохранение в internal storage
│   └── JsonExporter.java          # Экспорт/импорт JSON (Gson)
└── res/
    ├── layout/                    # XML-разметки с Material виджетами
    ├── drawable/                  # Shape/ripple для Material-стиля
    ├── values/                    # Цвета, строки, стили, размеры
    └── anim/                      # Анимации переходов
```

---

## Схема базы данных (SQLite)

### Таблица `persons`
| Поле | Тип | Описание |
|------|------|----------|
| id | INTEGER PK | Автоинкремент |
| first_name | TEXT NOT NULL | Имя |
| last_name | TEXT | Фамилия |
| photo_path | TEXT NOT NULL | Путь к файлу фото (internal storage) |
| company | TEXT | Компания |
| position | TEXT | Должность |
| context | TEXT | Контекст знакомства (до 200 символов) |
| note | TEXT | Заметка |
| hobbies | TEXT | Увлечения |
| interests | TEXT | Интересы |
| family_status | TEXT | Семейный статус |
| partner_name | TEXT | Имя партнёра |
| children_names | TEXT | Имена детей |
| pet_names | TEXT | Имена животных |
| religion | TEXT | Вероисповедание |
| political_views | TEXT | Политические взгляды |
| created_at | INTEGER | Unix timestamp |
| photo_updated_at | INTEGER | Unix timestamp |

### Таблица `events`
| Поле | Тип | Описание |
|------|------|----------|
| id | INTEGER PK | Автоинкремент |
| title | TEXT NOT NULL | Название |
| date | INTEGER | Unix timestamp (опционально) |
| description | TEXT | Описание |
| created_at | INTEGER | Unix timestamp |

### Таблица `event_persons` (связь M:N)
| Поле | Тип | Описание |
|------|------|----------|
| event_id | INTEGER FK | → events.id |
| person_id | INTEGER FK | → persons.id |

### Таблица `quiz_stats` (адаптивная тренировка)
| Поле | Тип | Описание |
|------|------|----------|
| id | INTEGER PK | Автоинкремент |
| person_id | INTEGER FK | → persons.id |
| event_id | INTEGER FK | → events.id |
| correct_count | INTEGER | Кол-во правильных ответов |
| wrong_count | INTEGER | Кол-во ошибок |
| last_shown_at | INTEGER | Unix timestamp последнего показа |
| weight | REAL | Вес (чем выше — тем чаще показывать) |

---

## Пошаговый план реализации

### Фаза 0 — Каркас проекта + скачивание зависимостей

1. Создать полную структуру каталогов `com.whoswho.app`
2. Написать `build.sh`:
   - Скачивание всех 35 JAR-зависимостей (dandar3 + Maven Central)
   - Компиляция Java → .class
   - dx → classes.dex
   - aapt → ресурсы → APK
   - Подпись debug keystore
3. Создать AndroidManifest.xml:
   - Permissions: CAMERA, READ/WRITE_EXTERNAL_STORAGE
   - SplashActivity (launcher), MainActivity
   - Theme: Theme.MaterialComponents.Light.NoActionBar
4. Создать ресурсы: colors.xml, styles.xml, strings.xml, dimens.xml
5. Создать минимальную MainActivity (AppCompatActivity)
6. Собрать APK — проверить что собирается с AndroidX

### Фаза 1 — База данных
1. Person.java, Event.java, QuizResult.java (POJO)
2. DatabaseHelper.java (SQLiteOpenHelper, все таблицы)
3. PersonDao.java (CRUD + getByEvent + поиск)
4. EventDao.java (CRUD + управление связями event_persons)
5. QuizStatsDao.java (статистика + обновление весов)
6. Собрать APK — проверить инициализацию БД

### Фаза 2 — Экран списка событий
1. MainActivity с FragmentContainerView + Toolbar
2. EventListFragment: RecyclerView + FAB
3. EventAdapter: MaterialCardView (название, дата, кол-во людей)
4. Диалог создания/редактирования события (MaterialAlertDialog + TextInputLayout)
5. Собрать APK

### Фаза 3 — Экран события + управление людьми
1. EventDetailFragment: заголовок + RecyclerView людей + FAB
2. PersonAdapter: круглое фото, имя, компания
3. Кнопка «Начать тренировку» (MaterialButton, активна >= 5 людей)
4. Собрать APK

### Фаза 4 — Добавление/редактирование человека
1. PersonEditFragment: TextInputLayout для всех полей
2. Фото: камера/галерея Intent, ресайз, сохранение в internal storage
3. Секция «Дополнительно» (expandable) — ChipGroup для тегов
4. ImageUtils + ImageCache (LruCache)
5. Собрать APK

### Фаза 5 — Викторина (5 режимов)
1. QuizEngine: генерация вопросов, дистракторы, адаптивный алгоритм
2. QuizFragment: 5 режимов UI (фото→имя, описание→фото, имя→должность, факт→фото, компания→фото)
3. Анимации ответов (цветовая индикация), прогресс-бар
4. QuizResultFragment: процент, список ошибок, повтор
5. Собрать APK

### Фаза 6 — Экспорт/импорт
1. JsonExporter: Gson → JSON с Base64 фото
2. ExportImportFragment: кнопки + диалоги подтверждения
3. Собрать APK

### Фаза 7 — Splash + онбординг
1. SplashActivity + ViewPager2 для онбординга
2. Собрать APK

### Фаза 8 — Полировка UI
1. Material Design цвета, ripple, elevation
2. Анимации переходов между фрагментами
3. Empty states
4. Одноручное управление

### Фаза 9 — Финальная сборка и тестирование
1. Прогон всех сценариев из ТЗ
2. Финальная подпись APK
3. Коммит и пуш

---

## Ключевые решения

1. **AndroidX + Material Design** через dandar3 GitHub JAR-файлы
2. **AppCompatActivity** + **androidx.fragment** для навигации
3. **RecyclerView** + **MaterialCardView** для списков
4. **TextInputLayout** для полей ввода
5. **FloatingActionButton** для основных действий
6. **SQLite напрямую** через SQLiteOpenHelper (не Room)
7. **Gson** для JSON экспорта/импорта
8. **BitmapFactory + LruCache** для фото (без Glide/Picasso)
9. **Сборка** по модели KeyoneKB build_ci.sh
