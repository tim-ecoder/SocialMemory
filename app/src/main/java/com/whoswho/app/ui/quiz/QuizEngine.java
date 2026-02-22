package com.whoswho.app.ui.quiz;

import com.whoswho.app.model.Person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates a sequence of quiz questions from a list of people and their
 * statistical weights (higher weight = person appears more often).
 *
 * <p>Five question modes are supported:
 * <ul>
 *   <li>{@link #MODE_PHOTO_TO_NAME}    – show photo, pick the correct name.</li>
 *   <li>{@link #MODE_DESC_TO_PHOTO}    – show description, pick the correct photo.</li>
 *   <li>{@link #MODE_NAME_TO_POSITION} – show full name, pick the correct position.</li>
 *   <li>{@link #MODE_FACT_TO_PHOTO}    – show a fact (hobby/interest), pick the photo.</li>
 *   <li>{@link #MODE_COMPANY_TO_PHOTO} – show company name, pick the photo.</li>
 * </ul>
 */
public class QuizEngine {

    // -------------------------------------------------------------------------
    // Mode constants
    // -------------------------------------------------------------------------

    /** Show photo → pick name from 4 options. */
    public static final int MODE_PHOTO_TO_NAME    = 0;
    /** Show description (position/company/context) → pick photo from 4 options. */
    public static final int MODE_DESC_TO_PHOTO    = 1;
    /** Show full name → pick position from 4 options. */
    public static final int MODE_NAME_TO_POSITION = 2;
    /** Show a fact (hobby or interest) → pick photo from 4 options. */
    public static final int MODE_FACT_TO_PHOTO    = 3;
    /** Show company name → pick photo from 4 options. */
    public static final int MODE_COMPANY_TO_PHOTO = 4;

    // -------------------------------------------------------------------------
    // Inner class: Question
    // -------------------------------------------------------------------------

    /**
     * A single quiz question.
     */
    public static class Question {
        /** One of the MODE_* constants defined on {@link QuizEngine}. */
        public final int mode;
        /** The person that is the correct answer. */
        public final Person correctPerson;
        /**
         * All four answer options in shuffled order. The correct person is
         * always one element of this list.
         */
        public final List<Person> options;
        /** The prompt text displayed to the user (may reference correctPerson fields). */
        public final String questionText;

        Question(int mode, Person correctPerson,
                 List<Person> options, String questionText) {
            this.mode = mode;
            this.correctPerson = correctPerson;
            this.options = options;
            this.questionText = questionText;
        }
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final List<Person> mPeople;
    private final Map<Long, Double> mWeights;
    private final Random mRandom;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * @param people  The pool of people that questions will be drawn from.
     *                Must contain at least 4 people for distractors to work.
     * @param weights Map from person-id to weight (higher = more likely to appear).
     *                People not present in this map receive a default weight of 1.0.
     */
    public QuizEngine(List<Person> people, Map<Long, Double> weights) {
        mPeople  = new ArrayList<>(people);
        mWeights = weights;
        mRandom  = new Random();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Generates up to {@code count} quiz questions using weighted random selection.
     *
     * <p>If there are fewer than 4 people in the pool no questions can be generated
     * (we need at least 3 distractors), in which case an empty list is returned.
     *
     * @param count Desired number of questions.
     * @return An ordered list of {@link Question} objects ready to display.
     */
    public List<Question> generateQuestions(int count) {
        List<Question> questions = new ArrayList<>();
        if (mPeople.size() < 4 || count <= 0) {
            return questions;
        }

        // Build a list of valid modes up-front; we'll filter per-person later
        int[] allModes = {
                MODE_PHOTO_TO_NAME,
                MODE_DESC_TO_PHOTO,
                MODE_NAME_TO_POSITION,
                MODE_FACT_TO_PHOTO,
                MODE_COMPANY_TO_PHOTO
        };

        // Weighted probability distribution over people
        double[] cumulativeWeights = buildCumulativeWeights();

        for (int i = 0; i < count; i++) {
            // Pick a correct person by weighted random
            Person correct = pickWeighted(cumulativeWeights);
            if (correct == null) break;

            // Find a valid mode for this person
            int mode = pickValidMode(correct, allModes);
            if (mode < 0) {
                // No valid mode – fall back to MODE_PHOTO_TO_NAME (always valid
                // as long as photo exists, which is required at person-save time)
                mode = MODE_PHOTO_TO_NAME;
            }

            // Build 3 distractors
            List<Person> distractors = pickDistractors(correct, 3);
            if (distractors.size() < 3) {
                // Not enough unique distractors – skip
                continue;
            }

            // Assemble options and shuffle
            List<Person> options = new ArrayList<>(distractors);
            options.add(correct);
            Collections.shuffle(options, mRandom);

            // Build question text
            String questionText = buildQuestionText(mode, correct);

            questions.add(new Question(mode, correct, options, questionText));
        }

        return questions;
    }

    // -------------------------------------------------------------------------
    // Private helpers – question building
    // -------------------------------------------------------------------------

    /**
     * Returns a MODE_* constant that is valid (i.e. has required data) for the
     * given person, chosen at random from the valid set. Returns -1 if none valid.
     */
    private int pickValidMode(Person person, int[] allModes) {
        List<Integer> valid = new ArrayList<>();
        for (int m : allModes) {
            if (isModeValid(m, person)) {
                valid.add(m);
            }
        }
        if (valid.isEmpty()) return -1;
        return valid.get(mRandom.nextInt(valid.size()));
    }

    /**
     * A mode is valid for a person when the data field it requires is non-empty.
     * MODE_PHOTO_TO_NAME and MODE_DESC_TO_PHOTO are always valid (photo is
     * required at save time and description is built from whatever fields are set).
     */
    private boolean isModeValid(int mode, Person person) {
        switch (mode) {
            case MODE_PHOTO_TO_NAME:
                // photo is required – always valid
                return true;
            case MODE_DESC_TO_PHOTO:
                // description string is always non-empty (at minimum it shows the name)
                return true;
            case MODE_NAME_TO_POSITION:
                return !isEmpty(person.getPosition());
            case MODE_FACT_TO_PHOTO:
                return !isEmpty(person.getHobbies()) || !isEmpty(person.getInterests());
            case MODE_COMPANY_TO_PHOTO:
                return !isEmpty(person.getCompany());
            default:
                return false;
        }
    }

    private String buildQuestionText(int mode, Person person) {
        switch (mode) {
            case MODE_PHOTO_TO_NAME:
                return "Who is this?";
            case MODE_DESC_TO_PHOTO:
                return "Find this person:\n" + person.getDescription();
            case MODE_NAME_TO_POSITION:
                return "What does " + person.getFullName() + " do?";
            case MODE_FACT_TO_PHOTO:
                return "Find this person:\n" + pickFact(person);
            case MODE_COMPANY_TO_PHOTO:
                return "Who works at " + person.getCompany() + "?";
            default:
                return "";
        }
    }

    /**
     * Returns a non-empty fact string (hobby or interest) for the person.
     * Prefers hobbies; falls back to interests; falls back to empty string.
     */
    private String pickFact(Person person) {
        if (!isEmpty(person.getHobbies()) && !isEmpty(person.getInterests())) {
            // Randomly choose one of the two
            return mRandom.nextBoolean() ? person.getHobbies() : person.getInterests();
        }
        if (!isEmpty(person.getHobbies())) return person.getHobbies();
        if (!isEmpty(person.getInterests())) return person.getInterests();
        return "";
    }

    // -------------------------------------------------------------------------
    // Private helpers – weighted sampling
    // -------------------------------------------------------------------------

    /**
     * Builds a cumulative-weight array aligned with {@link #mPeople}.
     * Each person's weight comes from {@link #mWeights}; defaults to 1.0.
     */
    private double[] buildCumulativeWeights() {
        double[] cum = new double[mPeople.size()];
        double total = 0.0;
        for (int i = 0; i < mPeople.size(); i++) {
            double w = mWeights.containsKey(mPeople.get(i).getId())
                    ? mWeights.get(mPeople.get(i).getId())
                    : 1.0;
            if (w <= 0) w = 0.1; // guard against non-positive weights
            total += w;
            cum[i] = total;
        }
        return cum;
    }

    /**
     * Picks one person by weighted random using a precomputed cumulative array.
     */
    private Person pickWeighted(double[] cumulativeWeights) {
        if (cumulativeWeights.length == 0) return null;
        double total = cumulativeWeights[cumulativeWeights.length - 1];
        double r = mRandom.nextDouble() * total;
        for (int i = 0; i < cumulativeWeights.length; i++) {
            if (r < cumulativeWeights[i]) {
                return mPeople.get(i);
            }
        }
        return mPeople.get(mPeople.size() - 1);
    }

    /**
     * Picks {@code count} distinct people from {@link #mPeople} that are NOT
     * {@code correct}. Selection is uniformly random (no weighting for distractors).
     */
    private List<Person> pickDistractors(Person correct, int count) {
        List<Person> pool = new ArrayList<>(mPeople);
        pool.remove(correct);
        Collections.shuffle(pool, mRandom);
        int take = Math.min(count, pool.size());
        return pool.subList(0, take);
    }

    // -------------------------------------------------------------------------
    // Misc
    // -------------------------------------------------------------------------

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
