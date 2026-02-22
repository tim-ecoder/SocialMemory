package com.whoswho.app.ui.events;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.whoswho.app.R;
import com.whoswho.app.db.DatabaseHelper;
import com.whoswho.app.db.EventDao;
import com.whoswho.app.model.Event;

import java.util.List;

public class EventListFragment extends Fragment {

    /** Host activity must implement this interface to receive navigation events. */
    public interface OnEventSelectedListener {
        void onEventSelected(long eventId);
    }

    private ListView mListView;
    private View mEmptyState;
    private Button mBtnAdd;

    private EventAdapter mAdapter;
    private EventDao mEventDao;
    private OnEventSelectedListener mListener;

    // -------------------------------------------------------------------------
    // Fragment lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnEventSelectedListener) {
            mListener = (OnEventSelectedListener) activity;
        } else {
            throw new ClassCastException(activity.getClass().getName()
                    + " must implement OnEventSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_event_list, container, false);

        mListView = (ListView) root.findViewById(R.id.list_events);
        mEmptyState = root.findViewById(R.id.empty_state);
        mBtnAdd = (Button) root.findViewById(R.id.btn_add_event);

        mEventDao = new EventDao(DatabaseHelper.getInstance(getActivity()));
        mAdapter = new EventAdapter(getActivity());
        mListView.setAdapter(mAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Event event = mAdapter.getItem(position);
                if (mListener != null) {
                    mListener.onEventSelected(event.getId());
                }
            }
        });

        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Event event = mAdapter.getItem(position);
                showDeleteConfirmDialog(event);
                return true;
            }
        });

        mBtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEventDialog(null);
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEvents();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    // -------------------------------------------------------------------------
    // Data
    // -------------------------------------------------------------------------

    private void loadEvents() {
        List<Event> events = mEventDao.getAll();
        mAdapter.setEvents(events);
        updateEmptyState(events.isEmpty());
    }

    private void updateEmptyState(boolean empty) {
        mListView.setVisibility(empty ? View.GONE : View.VISIBLE);
        mEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    // -------------------------------------------------------------------------
    // Dialogs
    // -------------------------------------------------------------------------

    /**
     * Shows the create/edit event dialog.
     * @param event pass null for creation, non-null for editing.
     */
    private void showEventDialog(final Event event) {
        View dialogView = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_event, null);

        final EditText etName = (EditText) dialogView.findViewById(R.id.et_event_name);
        final EditText etDate = (EditText) dialogView.findViewById(R.id.et_event_date);
        final EditText etDesc = (EditText) dialogView.findViewById(R.id.et_event_description);

        boolean isEdit = event != null;
        String title = isEdit
                ? getString(R.string.edit_event)
                : getString(R.string.create_event);

        if (isEdit) {
            etName.setText(event.getTitle());
            if (event.getDate() > 0) {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault());
                etDate.setText(sdf.format(new java.util.Date(event.getDate())));
            }
            if (event.getDescription() != null) {
                etDesc.setText(event.getDescription());
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(getActivity(), R.string.name_required, Toast.LENGTH_SHORT).show();
                    return;
                }

                String dateStr = etDate.getText().toString().trim();
                long dateMs = parseDateMillis(dateStr);

                String desc = etDesc.getText().toString().trim();

                if (event == null) {
                    // Create
                    Event newEvent = new Event();
                    newEvent.setTitle(name);
                    newEvent.setDate(dateMs);
                    newEvent.setDescription(desc.isEmpty() ? null : desc);
                    mEventDao.insert(newEvent);
                } else {
                    // Update
                    event.setTitle(name);
                    event.setDate(dateMs);
                    event.setDescription(desc.isEmpty() ? null : desc);
                    mEventDao.update(event);
                }
                loadEvents();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showDeleteConfirmDialog(final Event event) {
        String message = String.format(getString(R.string.delete_event_confirm), event.getTitle());
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.delete_event)
                .setMessage(message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mEventDao.delete(event.getId());
                        loadEvents();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Attempts to parse a human-readable date string into epoch milliseconds.
     * Returns 0 if parsing fails or input is blank.
     */
    private long parseDateMillis(String dateStr) {
        if (TextUtils.isEmpty(dateStr)) {
            return 0;
        }
        String[] formats = {
                "MMM d, yyyy",
                "MM/dd/yyyy",
                "dd.MM.yyyy",
                "yyyy-MM-dd"
        };
        for (String fmt : formats) {
            try {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat(fmt, java.util.Locale.getDefault());
                sdf.setLenient(false);
                java.util.Date d = sdf.parse(dateStr);
                if (d != null) {
                    return d.getTime();
                }
            } catch (java.text.ParseException ignored) {
            }
        }
        return 0;
    }
}
