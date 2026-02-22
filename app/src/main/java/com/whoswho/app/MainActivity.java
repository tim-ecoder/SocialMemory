package com.whoswho.app;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.whoswho.app.ui.event.EventDetailFragment;
import com.whoswho.app.ui.events.EventListFragment;

public class MainActivity extends Activity
        implements EventListFragment.OnEventSelectedListener,
                   EventDetailFragment.OnDetailActionListener {

    // Tag constants used when adding fragments to the back stack
    private static final String TAG_EVENT_LIST   = "event_list";
    private static final String TAG_EVENT_DETAIL = "event_detail";

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
     * Stub – implemented by PersonEditorFragment when that module is added.
     */
    public void showPersonEdit(long eventId, long personId) {
        // PersonEditorFragment will be wired here in a future sprint.
        // For now, fall through silently so the app does not crash.
    }

    /**
     * Navigates to the quiz screen for the given event.
     * Stub – implemented by QuizFragment when that module is added.
     */
    public void showQuiz(long eventId) {
        // QuizFragment will be wired here in a future sprint.
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
        // Person picker / editor flow – handled inside EventDetailFragment for now.
    }

    @Override
    public void onStartQuiz(long eventId) {
        showQuiz(eventId);
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
