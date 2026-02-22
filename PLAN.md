# ПЛАН РЕАЛИЗАЦИИ — Who's Who (Android MVP)

## Сводка

**Приложение:** Тренажёр для запоминания людей перед мероприятиями
**Платформа:** Android 8.1+ (API 27), только Java
**Хранение:** SQLite (локальная БД, без сервера)
**UI:** Material Design 3 (нативные Android-компоненты)
**Сборка:** aapt + javac + dx + zipalign + apksigner (Debian Android SDK)

---

## Ограничения окружения сборки

- `maven.google.com` и `dl.google.com` **недоступны** — невозможно скачать
  AndroidX, Material Components library, Room, AppCompat
- Доступен только `android.jar` API 23 (совместим с targetSdk 27)
- Сборка через shell-скрипт `build.sh`, не через Gradle Android Plugin

**Следствия:**
- UI строится на **нативных Android виджетах** (android.widget.*)
  с кастомными drawable для Material-подобного вида
- Вместо Room — **прямой SQLite** (SQLiteOpenHelper)
- Вместо RecyclerView — **ListView** с кастомными адаптерами
- Вместо Glide/Picasso — **BitmapFactory** + LruCache
- JSON через **org.json** (встроен в Android SDK)

---

## Архитектура

```
com.whoswho.app/
├── MainActivity.java              # Навигация между экранами (фрагменты)
├── db/
│   ├── DatabaseHelper.java        # SQLiteOpenHelper — схема + миграции
│   ├── PersonDao.java             # CRUD для людей
│   ├── EventDao.java              # CRUD для событий
│   └── QuizStatsDao.java          # Статистика ошибок для адаптивной тренировки
├── model/
│   ├── Person.java                # Модель «Человек»
│   ├── Event.java                 # Модель «Событие»
│   └── QuizResult.java            # Результат викторины
├── ui/
│   ├── splash/
│   │   └── SplashActivity.java    # Splash-экран
│   ├── events/
│   │   ├── EventListFragment.java # Список событий
│   │   └── EventListAdapter.java
│   ├── event/
│   │   ├── EventDetailFragment.java # Экран события + список людей
│   │   └── EventPersonAdapter.java
│   ├── person/
│   │   ├── PersonEditFragment.java  # Добавление/редактирование человека
│   │   └── PhotoHelper.java         # Камера / галерея + кроп
│   ├── quiz/
│   │   ├── QuizFragment.java        # Основной экран викторины
│   │   ├── QuizEngine.java          # Логика вопросов + адаптивный алгоритм
│   │   └── QuizResultFragment.java  # Экран результата
│   └── settings/
│       └── ExportImportFragment.java # Экспорт/импорт базы
├── util/
│   ├── ImageCache.java            # LruCache для фото
│   ├── ImageUtils.java            # Ресайз, кроп, сохранение в internal storage
│   └── JsonExporter.java          # Экспорт/импорт JSON
└── res/
    ├── layout/                    # XML-разметки всех экранов
    ├── drawable/                  # Кастомные shape/ripple для Material-стиля
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

### Фаза 0 — Каркас проекта
**Файлы:** AndroidManifest.xml, build.sh, базовая структура каталогов

1. Создать полную структуру каталогов `com.whoswho.app`
2. Создать AndroidManifest.xml (все Activity, permissions: CAMERA, READ/WRITE_EXTERNAL_STORAGE)
3. Создать ресурсы темы: colors.xml, styles.xml (Material Design 3-подобная тема)
4. Создать strings.xml (русский + английский)
5. Обновить build.sh для нового package name
6. Собрать пустой APK — проверить что собирается

### Фаза 1 — База данных
**Файлы:** db/*.java, model/*.java

1. Реализовать `Person.java` — POJO со всеми полями из ТЗ
2. Реализовать `Event.java` — POJO
3. Реализовать `QuizResult.java` — POJO для результата
4. Реализовать `DatabaseHelper.java`:
   - onCreate — создание всех таблиц
   - onUpgrade — миграции (пустые для v1)
5. Реализовать `PersonDao.java`:
   - insert, update, delete, getById, getAll
   - getByEvent(eventId)
   - поиск по имени
6. Реализовать `EventDao.java`:
   - insert, update, delete, getById, getAll
   - addPerson(eventId, personId)
   - removePerson(eventId, personId)
   - getPersonCount(eventId)
7. Реализовать `QuizStatsDao.java`:
   - getStatsForEvent(eventId)
   - updateWeight(personId, eventId, correct)
   - resetStats(eventId)
8. Собрать APK — проверить инициализацию БД

### Фаза 2 — Экран списка событий
**Файлы:** ui/events/*.java, res/layout/fragment_event_list.xml, item_event.xml

1. Создать `MainActivity.java` — контейнер для фрагментов
2. Создать `EventListFragment.java`:
   - ListView с кастомным адаптером
   - FAB «Создать событие»
   - Диалог создания события (название, дата, описание)
   - Длинное нажатие → удаление
   - Нажатие → переход к событию
3. Создать `EventListAdapter.java`:
   - Название, дата, кол-во людей
   - Material-подобные карточки (bg_card drawable)
4. Создать drawable: карточки, кнопки, FAB, ripple-эффекты
5. Собрать APK — проверить CRUD событий

### Фаза 3 — Экран события + управление людьми
**Файлы:** ui/event/*.java, res/layout/fragment_event_detail.xml, item_person.xml

1. Создать `EventDetailFragment.java`:
   - Заголовок события (название, дата)
   - Список людей события (ListView с фото-миниатюрами)
   - Кнопка «Добавить человека»
   - Кнопка «Начать тренировку» (активна при >= 5 людей)
   - Удаление человека из события (свайп или long press)
2. Создать `EventPersonAdapter.java`:
   - Круглое фото, имя, компания
   - Быстрый просмотр карточки
3. Собрать APK — проверить управление людьми в событии

### Фаза 4 — Добавление/редактирование человека
**Файлы:** ui/person/*.java, res/layout/fragment_person_edit.xml

1. Создать `PersonEditFragment.java`:
   - Фото (нажатие → камера или галерея через Intent)
   - Основные поля: имя, фамилия, компания, должность, контекст
   - Разворачиваемая секция «Дополнительно»:
     увлечения, интересы, семейный статус, партнёр, дети, животные,
     вероисповедание, политические взгляды
   - Кнопка «Сохранить»
   - Валидация: имя + фото обязательны
2. Создать `PhotoHelper.java`:
   - Intent для камеры (MediaStore.ACTION_IMAGE_CAPTURE)
   - Intent для галереи (ACTION_PICK)
   - Ресайз фото до 800x800 max
   - Сохранение в internal storage (getFilesDir)
   - Обновление фото без пересоздания записи
3. Создать `ImageUtils.java`:
   - decodeScaledBitmap — загрузка с ресайзом
   - getRoundedBitmap — круглая миниатюра
   - saveBitmap — сохранение в файл
4. Создать `ImageCache.java`:
   - LruCache<String, Bitmap> — кэш в памяти
   - get/put по пути к файлу
5. Собрать APK — проверить добавление человека с фото

### Фаза 5 — Викторина (5 режимов)
**Файлы:** ui/quiz/*.java, res/layout/fragment_quiz.xml, fragment_quiz_result.xml

1. Создать `QuizEngine.java` — ядро логики:
   - Генерация вопросов из людей события
   - Случайный порядок
   - 5 режимов:
     - MODE_PHOTO_TO_NAME: фото → выбери имя (4 варианта)
     - MODE_DESC_TO_PHOTO: описание → выбери фото (сетка 2x2)
     - MODE_NAME_TO_POSITION: имя → выбери должность (4 варианта)
     - MODE_FACT_TO_PHOTO: факт → выбери фото (сетка 2x2)
     - MODE_COMPANY_TO_PHOTO: компания → выбери фото (сетка 2x2)
   - Генерация дистракторов (неправильных вариантов) из того же события
   - Подсчёт правильных/неправильных
   - Адаптивный алгоритм:
     ```
     weight = wrong_count / (correct_count + 1) + time_decay
     probability(person) = weight(person) / sum(all_weights)
     ```
   - Микс режимов в одной сессии

2. Создать `QuizFragment.java`:
   - Режим «Фото → Имя»:
     - Большое фото по центру
     - 4 кнопки с именами
   - Режим «Описание → Фото»:
     - Текст описания сверху
     - Сетка 2×2 из фото
   - Режим «Имя → Должность»:
     - Имя крупно сверху
     - 4 кнопки с должностями
   - Режим «Факт → Фото»:
     - Факт (интерес/увлечение/контекст) сверху
     - Сетка 2×2 из фото
   - Режим «Компания → Фото»:
     - Название компании сверху
     - Сетка 2×2 из фото
   - Анимация при ответе:
     - Правильный → зелёная подсветка, delay 1 сек
     - Неправильный → красная подсветка + показ правильного, delay 2 сек
   - Прогресс-бар сверху (вопрос X из Y)
   - После ответа: краткая карточка человека (имя, компания, контекст)

3. Создать `QuizResultFragment.java`:
   - Итоговый процент (большая цифра, анимация)
   - Список: кого узнал / кого не узнал
   - Кнопка «Повторить тренировку»
   - Кнопка «Повторить ошибки» (только те, в ком ошибся)
   - Кнопка «Назад к событию»

4. Собрать APK — проверить все 5 режимов

### Фаза 6 — Экспорт/импорт базы
**Файлы:** util/JsonExporter.java, ui/settings/ExportImportFragment.java

1. Создать `JsonExporter.java`:
   - exportAll() → JSON-файл со всеми людьми + событиями + связями
   - Фото → Base64 внутри JSON (или отдельные файлы в zip)
   - importAll(file) → парсинг JSON, вставка в БД
   - Валидация формата при импорте
2. Создать `ExportImportFragment.java`:
   - Кнопка «Экспорт» → подтверждение → сохранение через Intent (ACTION_CREATE_DOCUMENT)
   - Кнопка «Импорт» → подтверждение → предупреждение о перезаписи → выбор файла
   - Share через Intent (ACTION_SEND)
3. Собрать APK — проверить экспорт/импорт

### Фаза 7 — Splash + Онбординг
**Файлы:** ui/splash/SplashActivity.java, res/layout/activity_splash.xml

1. Создать `SplashActivity.java`:
   - Логотип + название приложения
   - Задержка 1.5 сек → переход к MainActivity
   - Первый запуск → показать онбординг (3 слайда ViewFlipper)
2. Собрать APK — проверить запуск

### Фаза 8 — Полировка UI
1. Все drawable для Material Design 3-подобного вида:
   - Rounded corners (16dp) на карточках
   - Ripple-эффекты на всех кликабельных элементах
   - Цветовая схема Material You (Primary: #6750A4, Secondary: #625B71, Tertiary: #7D5260)
   - Elevation/shadow на карточках
   - Градиенты на header-экранах
2. Анимации переходов между фрагментами
3. Пустые состояния (empty state) — иконка + текст когда списки пустые
4. Плавная загрузка фото (placeholder → fade-in)
5. Одноручное управление: основные действия в нижней части экрана

### Фаза 9 — Финальная сборка и тестирование
1. Полный прогон всех сценариев из ТЗ (раздел 4)
2. Тест на минимум 5 людей в событии
3. Тест всех 5 режимов викторины
4. Тест экспорта/импорта
5. Тест офлайн-работы
6. Финальная подписка APK
7. Коммит и пуш

---

## Оценка объёма (файлы)

| Компонент | Файлов Java | Файлов XML |
|-----------|-------------|------------|
| Модели | 3 | — |
| База данных | 4 | — |
| UI: События | 3 | 3 |
| UI: Событие/Люди | 3 | 3 |
| UI: Редактор человека | 2 | 1 |
| UI: Викторина | 3 | 3 |
| UI: Настройки | 1 | 1 |
| UI: Splash | 1 | 2 |
| Утилиты | 3 | — |
| MainActivity | 1 | 1 |
| Ресурсы | — | ~15 |
| **Итого** | **~24** | **~29** |

---

## Порядок сборки каждой фазы

```bash
# После каждой фазы:
bash build.sh
# APK → socialmemory-debug.apk
# Установка: adb install -r socialmemory-debug.apk
```

---

## Ключевые решения

1. **Без AndroidX/Material library** — используем нативные виджеты +
   кастомные drawable для Material-подобного вида
2. **SQLite напрямую** — вместо Room, через SQLiteOpenHelper
3. **ListView** — вместо RecyclerView (доступен в базовом SDK)
4. **Fragment** — встроенный android.app.Fragment (не AndroidX)
5. **Фото** — хранятся в internal storage, путь в БД
6. **JSON экспорт** — через org.json (встроен в Android)
7. **Адаптивная тренировка** — weight-based алгоритм в таблице quiz_stats
