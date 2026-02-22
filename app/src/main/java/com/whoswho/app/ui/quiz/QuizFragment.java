package com.whoswho.app.ui.quiz;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.whoswho.app.R;
import com.whoswho.app.db.DatabaseHelper;
import com.whoswho.app.db.PersonDao;
import com.whoswho.app.db.QuizStatsDao;
import com.whoswho.app.model.Person;
import com.whoswho.app.model.QuizResult;
import com.whoswho.app.util.ImageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Drives the interactive quiz session for a single event.
 *
 * <p>Argument keys:
 * <ul>
 *   <li>{@link #ARG_EVENT_ID}     – the event being quizzed (required).</li>
 *   <li>{@link #ARG_PERSON_IDS}   – optional {@code long[]} to restrict the
 *       people pool (used by "Retry Mistakes"). When absent all event people
 *       are used.</li>
 * </ul>
 */
public class QuizFragment extends Fragment {

    // -------------------------------------------------------------------------
    // Argument keys
    // -------------------------------------------------------------------------

    public static final String ARG_EVENT_ID   = "event_id";
    public static final String ARG_PERSON_IDS = "person_ids";

    // Feedback delay before advancing to next question (milliseconds)
    private static final long FEEDBACK_DELAY_MS = 1500L;

    // Max questions per quiz session
    private static final int MAX_QUESTIONS = 20;

    // Photo display size
    private static final int PHOTO_SIZE = 400;
    private static final int THUMB_SIZE = 200;

    // -------------------------------------------------------------------------
    // Views
    // -------------------------------------------------------------------------

    private ProgressBar mProgressBar;
    private TextView    mTvCounter;

    // Question display
    private ImageView   mQuizPhoto;
    private TextView    mQuizText;

    // Answer areas
    private LinearLayout mAnswerButtonsContainer;
    private GridLayout   mAnswerPhotosContainer;

    // Text answer buttons
    private Button[] mAnswerButtons;

    // Photo answer images
    private ImageView[] mAnswerPhotos;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private long mEventId = -1;

    private List<QuizEngine.Question> mQuestions;
    private int mCurrentIndex = 0;
    private boolean mAnswered = false; // true while feedback is showing

    private QuizResult mResult;
    private QuizStatsDao mStatsDao;

    private final Handler mHandler = new Handler();

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /** Creates a quiz over all people in the event. */
    public static QuizFragment newInstance(long eventId) {
        QuizFragment f = new QuizFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, eventId);
        f.setArguments(args);
        return f;
    }

    /** Creates a quiz restricted to the supplied person IDs (Retry Mistakes). */
    public static QuizFragment newInstanceForPeople(long eventId, long[] personIds) {
        QuizFragment f = new QuizFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, eventId);
        args.putLongArray(ARG_PERSON_IDS, personIds);
        f.setArguments(args);
        return f;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mEventId = getArguments().getLong(ARG_EVENT_ID, -1L);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Progress header
        mProgressBar = (ProgressBar) view.findViewById(R.id.quiz_progress);
        mTvCounter   = (TextView)    view.findViewById(R.id.quiz_counter);

        // Question display
        mQuizPhoto = (ImageView) view.findViewById(R.id.quiz_photo);
        mQuizText  = (TextView)  view.findViewById(R.id.quiz_text);

        // Answer areas
        mAnswerButtonsContainer = (LinearLayout) view.findViewById(R.id.answer_buttons_container);
        mAnswerPhotosContainer  = (GridLayout)   view.findViewById(R.id.answer_photos_container);

        // Text buttons
        mAnswerButtons = new Button[4];
        mAnswerButtons[0] = (Button) view.findViewById(R.id.answer_btn_0);
        mAnswerButtons[1] = (Button) view.findViewById(R.id.answer_btn_1);
        mAnswerButtons[2] = (Button) view.findViewById(R.id.answer_btn_2);
        mAnswerButtons[3] = (Button) view.findViewById(R.id.answer_btn_3);

        // Photo images
        mAnswerPhotos = new ImageView[4];
        mAnswerPhotos[0] = (ImageView) view.findViewById(R.id.answer_photo_0);
        mAnswerPhotos[1] = (ImageView) view.findViewById(R.id.answer_photo_1);
        mAnswerPhotos[2] = (ImageView) view.findViewById(R.id.answer_photo_2);
        mAnswerPhotos[3] = (ImageView) view.findViewById(R.id.answer_photo_3);

        initQuiz();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHandler.removeCallbacksAndMessages(null);
    }

    // -------------------------------------------------------------------------
    // Quiz initialisation
    // -------------------------------------------------------------------------

    private void initQuiz() {
        if (mEventId < 0) return;

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getActivity());
        PersonDao personDao = new PersonDao(dbHelper);
        mStatsDao = new QuizStatsDao(dbHelper);

        // Determine the people pool
        List<Person> people;
        long[] restrictedIds = getArguments() != null
                ? getArguments().getLongArray(ARG_PERSON_IDS) : null;

        if (restrictedIds != null && restrictedIds.length > 0) {
            people = new ArrayList<>();
            for (long id : restrictedIds) {
                Person p = personDao.getById(id);
                if (p != null) people.add(p);
            }
            // If the restricted pool is too small, fall back to the full event pool
            if (people.size() < 4) {
                people = personDao.getByEvent(mEventId);
            }
        } else {
            people = personDao.getByEvent(mEventId);
        }

        Map<Long, Double> weights = mStatsDao.getWeightsForEvent(mEventId);

        int count = Math.min(people.size(), MAX_QUESTIONS);

        QuizEngine engine = new QuizEngine(people, weights);
        mQuestions = engine.generateQuestions(count);

        mResult = new QuizResult();
        mResult.setTotalQuestions(mQuestions.size());
        mResult.setCorrectAnswers(0);

        if (mQuestions.isEmpty()) {
            // Nothing to show – navigate back
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
            return;
        }

        mCurrentIndex = 0;
        showQuestion(mCurrentIndex);
    }

    // -------------------------------------------------------------------------
    // Question display
    // -------------------------------------------------------------------------

    private void showQuestion(int index) {
        mAnswered = false;
        QuizEngine.Question q = mQuestions.get(index);

        // Update progress
        int progressPct = (int) (100.0 * index / mQuestions.size());
        mProgressBar.setProgress(progressPct);
        mTvCounter.setText(getString(R.string.question_progress,
                index + 1, mQuestions.size()));

        // Reset answer area colours / enabled state
        resetAnswerViews();

        // Route to the correct display layout based on mode
        switch (q.mode) {
            case QuizEngine.MODE_PHOTO_TO_NAME:
                showPhotoQuestion(q);
                showTextAnswers(q);
                break;

            case QuizEngine.MODE_NAME_TO_POSITION:
                showTextQuestion(q.questionText);
                showPositionAnswers(q);
                break;

            case QuizEngine.MODE_DESC_TO_PHOTO:
            case QuizEngine.MODE_FACT_TO_PHOTO:
            case QuizEngine.MODE_COMPANY_TO_PHOTO:
                showTextQuestion(q.questionText);
                showPhotoAnswers(q);
                break;

            default:
                showPhotoQuestion(q);
                showTextAnswers(q);
                break;
        }
    }

    // ---- Question area helpers -----------------------------------------------

    private void showPhotoQuestion(QuizEngine.Question q) {
        mQuizPhoto.setVisibility(View.VISIBLE);
        mQuizText.setVisibility(View.GONE);

        Bitmap bm = loadBitmap(q.correctPerson.getPhotoPath(), PHOTO_SIZE);
        if (bm != null) {
            mQuizPhoto.setImageBitmap(bm);
        } else {
            mQuizPhoto.setImageBitmap(null);
        }
    }

    private void showTextQuestion(String text) {
        mQuizPhoto.setVisibility(View.GONE);
        mQuizText.setVisibility(View.VISIBLE);
        mQuizText.setText(text);
    }

    // ---- Answer area helpers -------------------------------------------------

    /**
     * Shows four name buttons where clicking selects the answer.
     */
    private void showTextAnswers(final QuizEngine.Question q) {
        mAnswerButtonsContainer.setVisibility(View.VISIBLE);
        mAnswerPhotosContainer.setVisibility(View.GONE);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            final Person option = q.options.get(i);
            mAnswerButtons[i].setText(option.getFullName());
            mAnswerButtons[i].setEnabled(true);
            mAnswerButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onTextAnswerSelected(q, idx);
                }
            });
        }
    }

    /**
     * Shows four position-text buttons (MODE_NAME_TO_POSITION).
     */
    private void showPositionAnswers(final QuizEngine.Question q) {
        mAnswerButtonsContainer.setVisibility(View.VISIBLE);
        mAnswerPhotosContainer.setVisibility(View.GONE);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            final Person option = q.options.get(i);
            String label = notEmpty(option.getPosition())
                    ? option.getPosition() : option.getFullName();
            mAnswerButtons[i].setText(label);
            mAnswerButtons[i].setEnabled(true);
            mAnswerButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onTextAnswerSelected(q, idx);
                }
            });
        }
    }

    /**
     * Shows four photo thumbnails (photo-answer modes).
     */
    private void showPhotoAnswers(final QuizEngine.Question q) {
        mAnswerButtonsContainer.setVisibility(View.GONE);
        mAnswerPhotosContainer.setVisibility(View.VISIBLE);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            final Person option = q.options.get(i);
            Bitmap bm = loadBitmap(option.getPhotoPath(), THUMB_SIZE);
            mAnswerPhotos[i].setImageBitmap(bm);
            mAnswerPhotos[i].setEnabled(true);
            mAnswerPhotos[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onPhotoAnswerSelected(q, idx);
                }
            });
        }
    }

    // -------------------------------------------------------------------------
    // Answer handling
    // -------------------------------------------------------------------------

    private void onTextAnswerSelected(QuizEngine.Question q, int selectedIndex) {
        if (mAnswered) return;
        mAnswered = true;

        Person chosen  = q.options.get(selectedIndex);
        boolean correct = chosen.getId() == q.correctPerson.getId();

        // Record in DB
        mStatsDao.recordAnswer(q.correctPerson.getId(), mEventId, correct);

        // Update result
        if (correct) {
            mResult.setCorrectAnswers(mResult.getCorrectAnswers() + 1);
        } else {
            mResult.addMistake(q.correctPerson);
        }

        // Visual feedback on all buttons
        for (int i = 0; i < 4; i++) {
            mAnswerButtons[i].setEnabled(false);
            Person opt = q.options.get(i);
            if (opt.getId() == q.correctPerson.getId()) {
                mAnswerButtons[i].setBackgroundColor(
                        getResources().getColor(R.color.correct_green_light));
            } else if (i == selectedIndex && !correct) {
                mAnswerButtons[i].setBackgroundColor(
                        getResources().getColor(R.color.wrong_red_light));
            }
        }

        scheduleNextQuestion();
    }

    private void onPhotoAnswerSelected(QuizEngine.Question q, int selectedIndex) {
        if (mAnswered) return;
        mAnswered = true;

        Person chosen  = q.options.get(selectedIndex);
        boolean correct = chosen.getId() == q.correctPerson.getId();

        // Record in DB
        mStatsDao.recordAnswer(q.correctPerson.getId(), mEventId, correct);

        // Update result
        if (correct) {
            mResult.setCorrectAnswers(mResult.getCorrectAnswers() + 1);
        } else {
            mResult.addMistake(q.correctPerson);
        }

        // Visual feedback on photo thumbnails via a coloured overlay
        for (int i = 0; i < 4; i++) {
            mAnswerPhotos[i].setEnabled(false);
            Person opt = q.options.get(i);
            if (opt.getId() == q.correctPerson.getId()) {
                mAnswerPhotos[i].setColorFilter(
                        getResources().getColor(R.color.correct_green_light),
                        android.graphics.PorterDuff.Mode.SRC_ATOP);
            } else if (i == selectedIndex && !correct) {
                mAnswerPhotos[i].setColorFilter(
                        getResources().getColor(R.color.wrong_red_light),
                        android.graphics.PorterDuff.Mode.SRC_ATOP);
            }
        }

        scheduleNextQuestion();
    }

    // -------------------------------------------------------------------------
    // Question flow
    // -------------------------------------------------------------------------

    private void scheduleNextQuestion() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                mCurrentIndex++;
                if (mCurrentIndex < mQuestions.size()) {
                    showQuestion(mCurrentIndex);
                } else {
                    navigateToResult();
                }
            }
        }, FEEDBACK_DELAY_MS);
    }

    private void navigateToResult() {
        // Build mistake person ID array
        List<Person> mistakes = mResult.getMistakePeople();
        long[] mistakeIds = new long[mistakes.size()];
        for (int i = 0; i < mistakes.size(); i++) {
            mistakeIds[i] = mistakes.get(i).getId();
        }

        QuizResultFragment resultFragment = QuizResultFragment.newInstance(
                mEventId,
                mResult.getTotalQuestions(),
                mResult.getCorrectAnswers(),
                mistakeIds);

        FragmentManager fm = getFragmentManager();
        if (fm == null) return;

        fm.beginTransaction()
                .replace(R.id.fragment_container, resultFragment)
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    // -------------------------------------------------------------------------
    // View reset helpers
    // -------------------------------------------------------------------------

    private void resetAnswerViews() {
        int defaultBg = getResources().getColor(R.color.surface_variant);

        for (Button btn : mAnswerButtons) {
            btn.setBackgroundColor(defaultBg);
            btn.setEnabled(true);
            btn.setTextColor(getResources().getColor(R.color.on_surface));
        }

        for (ImageView iv : mAnswerPhotos) {
            iv.clearColorFilter();
            iv.setEnabled(true);
        }
    }

    // -------------------------------------------------------------------------
    // Bitmap helper
    // -------------------------------------------------------------------------

    private Bitmap loadBitmap(String path, int maxSize) {
        if (path == null || path.isEmpty()) return null;
        return ImageUtils.loadScaled(path, maxSize);
    }

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
