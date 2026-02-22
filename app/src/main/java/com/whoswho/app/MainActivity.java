package com.whoswho.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.whoswho.app.ui.event.EventDetailFragment;
import com.whoswho.app.ui.events.EventListFragment;
import com.whoswho.app.ui.person.PersonEditFragment;
import com.whoswho.app.ui.quiz.QuizFragment;
import com.whoswho.app.ui.settings.SettingsFragment;

public class MainActivity extends Activity
        implements EventListFragment.OnEventSelectedListener,
                   EventDetailFragment.OnDetailActionListener {

    // Tag constants used when adding fragments to the back stack
    private static final String TAG_EVENT_LIST   = "event_list";
    private static final String TAG_EVENT_DETAIL = "event_detail";
    private static final String TAG_PERSON_EDIT  = "person_edit";
    private static final String TAG_QUIZ         = "quiz";
    private static final String TAG_SETTINGS     = "settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Only add the root fragment on a fresh start (not on config change / restore)
        if (savedInstanceState == null) {
            showEventList();
        }
    }

    // -------------------------------------------------------------------------
    // Navigation helpers
    // -------------------------------------------------------------------------

    /**
     * Replaces the fragment container with the event list, clearing the back stack.
     */
    public void showEventList() {
        FragmentManager fm = getFragmentManager();
        // Clear the entire back stack so Back from the list exits the app
        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Fragment current = fm.findFragmentByTag(TAG_EVENT_LIST);
        if (current == null) {
            EventListFragment fragment = new EventListFragment();
            fm.beginTransaction()
              .replace(R.id.fragment_container, fragment, TAG_EVENT_LIST)
              .commit();
        }
    }

    /**
     * Navigates to the detail screen for the given event.
     */
    public void showEventDetail(long eventId) {
        EventDetailFragment fragment = EventDetailFragment.newInstance(eventId);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, TAG_EVENT_DETAIL)
                .addToBackStack(TAG_EVENT_DETAIL)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    /**
     * Navigates to the person editor for the given person in the given event context.
     */
    public void showPersonEdit(long eventId, long personId) {
        PersonEditFragment fragment = PersonEditFragment.newInstance(personId, eventId);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, TAG_PERSON_EDIT)
                .addToBackStack(TAG_PERSON_EDIT)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    /**
     * Navigates to the quiz screen for the given event.
     */
    public void showQuiz(long eventId) {
        QuizFragment fragment = QuizFragment.newInstance(eventId);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, TAG_QUIZ)
                .addToBackStack(TAG_QUIZ)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    // -------------------------------------------------------------------------
    // EventListFragment.OnEventSelectedListener
    // -------------------------------------------------------------------------

    @Override
    public void onEventSelected(long eventId) {
        showEventDetail(eventId);
    }

    // -------------------------------------------------------------------------
    // EventDetailFragment.OnDetailActionListener
    // -------------------------------------------------------------------------

    @Override
    public void onAddPersonToEvent(long eventId) {
        showPersonEdit(eventId, -1);
    }

    @Override
    public void onStartQuiz(long eventId) {
        showQuiz(eventId);
    }

    @Override
    public void onEditPerson(long eventId, long personId) {
        showPersonEdit(eventId, personId);
    }

    // -------------------------------------------------------------------------
    // EventListFragment.OnEventSelectedListener
    // -------------------------------------------------------------------------

    @Override
    public void onOpenSettings() {
        SettingsFragment fragment = SettingsFragment.newInstance();
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, TAG_SETTINGS)
                .addToBackStack(TAG_SETTINGS)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

    // -------------------------------------------------------------------------
    // Back navigation
    // -------------------------------------------------------------------------

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
