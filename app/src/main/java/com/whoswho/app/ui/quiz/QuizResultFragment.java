package com.whoswho.app.ui.quiz;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.whoswho.app.R;
import com.whoswho.app.db.DatabaseHelper;
import com.whoswho.app.db.PersonDao;
import com.whoswho.app.model.Person;
import com.whoswho.app.util.ImageUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the result of a quiz session.
 *
 * <p>Argument keys:
 * <ul>
 *   <li>{@link #ARG_EVENT_ID}         – event the quiz was for.</li>
 *   <li>{@link #ARG_TOTAL_QUESTIONS}  – total number of questions shown.</li>
 *   <li>{@link #ARG_CORRECT_ANSWERS}  – number of correct answers.</li>
 *   <li>{@link #ARG_MISTAKE_IDS}      – {@code long[]} of person IDs answered wrong.</li>
 * </ul>
 */
public class QuizResultFragment extends Fragment {

    // -------------------------------------------------------------------------
    // Argument keys
    // -------------------------------------------------------------------------

    public static final String ARG_EVENT_ID        = "event_id";
    public static final String ARG_TOTAL_QUESTIONS = "total_questions";
    public static final String ARG_CORRECT_ANSWERS = "correct_answers";
    public static final String ARG_MISTAKE_IDS     = "mistake_ids";

    // Thumbnail size for mistake list
    private static final int THUMB_SIZE = 80;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private long   mEventId        = -1;
    private int    mTotalQuestions = 0;
    private int    mCorrectAnswers = 0;
    private long[] mMistakeIds     = new long[0];

    private List<Person> mMistakePeople;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * @param eventId        ID of the event the quiz was for.
     * @param totalQuestions Total questions shown in the session.
     * @param correctAnswers Number of questions answered correctly.
     * @param mistakeIds     Person IDs that were answered incorrectly (may be empty).
     */
    public static QuizResultFragment newInstance(long eventId,
                                                 int totalQuestions,
                                                 int correctAnswers,
                                                 long[] mistakeIds) {
        QuizResultFragment f = new QuizResultFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, eventId);
        args.putInt(ARG_TOTAL_QUESTIONS, totalQuestions);
        args.putInt(ARG_CORRECT_ANSWERS, correctAnswers);
        args.putLongArray(ARG_MISTAKE_IDS, mistakeIds != null ? mistakeIds : new long[0]);
        f.setArguments(args);
        return f;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mEventId        = args.getLong(ARG_EVENT_ID, -1L);
            mTotalQuestions = args.getInt(ARG_TOTAL_QUESTIONS, 0);
            mCorrectAnswers = args.getInt(ARG_CORRECT_ANSWERS, 0);
            long[] ids      = args.getLongArray(ARG_MISTAKE_IDS);
            mMistakeIds     = ids != null ? ids : new long[0];
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_result, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load mistake people from DB
        mMistakePeople = loadMistakePeople();

        // Percentage
        int pct = mTotalQuestions > 0
                ? (int) Math.round(100.0 * mCorrectAnswers / mTotalQuestions)
                : 0;

        TextView tvPct   = (TextView) view.findViewById(R.id.tv_percentage);
        TextView tvScore = (TextView) view.findViewById(R.id.tv_score);
        tvPct.setText(getString(R.string.result_score, pct));
        tvScore.setText(getString(R.string.result_correct, mCorrectAnswers, mTotalQuestions));

        // Mistakes list
        final TextView     tvReviewHeader = (TextView) view.findViewById(R.id.tv_review_header);
        final ListView     listMistakes   = (ListView) view.findViewById(R.id.list_mistakes);
        final Button       btnRetryMistakes = (Button) view.findViewById(R.id.btn_retry_mistakes);

        if (!mMistakePeople.isEmpty()) {
            tvReviewHeader.setVisibility(View.VISIBLE);
            listMistakes.setVisibility(View.VISIBLE);
            btnRetryMistakes.setVisibility(View.VISIBLE);

            MistakeAdapter adapter = new MistakeAdapter(getActivity(), mMistakePeople);
            listMistakes.setAdapter(adapter);
        } else {
            tvReviewHeader.setVisibility(View.GONE);
            listMistakes.setVisibility(View.GONE);
            btnRetryMistakes.setVisibility(View.GONE);
        }

        // Buttons
        Button btnRetryAll   = (Button) view.findViewById(R.id.btn_retry_all);
        Button btnBackToEvent = (Button) view.findViewById(R.id.btn_back_to_event);

        btnRetryAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryAll();
            }
        });

        btnRetryMistakes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryMistakes();
            }
        });

        btnBackToEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToEvent();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    /** Start a new quiz session over all event people. */
    private void retryAll() {
        QuizFragment quizFragment = QuizFragment.newInstance(mEventId);
        replaceWithFragment(quizFragment);
    }

    /** Start a new quiz session restricted to people we got wrong. */
    private void retryMistakes() {
        QuizFragment quizFragment = QuizFragment.newInstanceForPeople(mEventId, mMistakeIds);
        replaceWithFragment(quizFragment);
    }

    /**
     * Pops the back stack twice: once to remove this result fragment, and once
     * more to pop the quiz fragment, returning to EventDetailFragment.
     */
    private void backToEvent() {
        FragmentManager fm = getFragmentManager();
        if (fm == null) return;
        // Pop back to EventDetail by clearing the quiz-session entries
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    private void replaceWithFragment(Fragment fragment) {
        FragmentManager fm = getFragmentManager();
        if (fm == null) return;
        fm.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private List<Person> loadMistakePeople() {
        List<Person> people = new ArrayList<>();
        if (mMistakeIds.length == 0) return people;

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getActivity());
        PersonDao dao = new PersonDao(dbHelper);
        for (long id : mMistakeIds) {
            Person p = dao.getById(id);
            if (p != null) people.add(p);
        }
        return people;
    }

    // -------------------------------------------------------------------------
    // Inner adapter
    // -------------------------------------------------------------------------

    /**
     * Simple adapter that shows a small photo + full name for each mistake person.
     */
    private static class MistakeAdapter extends ArrayAdapter<Person> {

        private final List<Person> mItems;

        MistakeAdapter(Context ctx, List<Person> items) {
            super(ctx, 0, items);
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Person getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                // Build a simple row: ImageView + TextView side by side
                android.widget.LinearLayout row = new android.widget.LinearLayout(getContext());
                row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                row.setPadding(16, 12, 16, 12);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                ImageView iv = new ImageView(getContext());
                iv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                        dpToPx(getContext(), THUMB_SIZE),
                        dpToPx(getContext(), THUMB_SIZE)));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                row.addView(iv);

                TextView tv = new TextView(getContext());
                android.widget.LinearLayout.LayoutParams tvParams =
                        new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                tvParams.setMargins(dpToPx(getContext(), 16), 0, 0, 0);
                tv.setLayoutParams(tvParams);
                tv.setTextSize(16);
                row.addView(tv);

                holder = new ViewHolder();
                holder.photo = iv;
                holder.name  = tv;
                row.setTag(holder);
                convertView = row;
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Person person = mItems.get(position);
            holder.name.setText(person.getFullName());

            Bitmap bm = ImageUtils.loadScaled(person.getPhotoPath(), THUMB_SIZE);
            if (bm != null) {
                Bitmap circular = ImageUtils.getCircularBitmap(bm);
                holder.photo.setImageBitmap(circular != null ? circular : bm);
            } else {
                holder.photo.setImageBitmap(null);
            }

            return convertView;
        }

        private static int dpToPx(Context ctx, int dp) {
            float density = ctx.getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }

        private static class ViewHolder {
            ImageView photo;
            TextView  name;
        }
    }
}
