package com.whoswho.app.ui.event;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.whoswho.app.R;
import com.whoswho.app.db.DatabaseHelper;
import com.whoswho.app.db.EventDao;
import com.whoswho.app.db.PersonDao;
import com.whoswho.app.model.Event;
import com.whoswho.app.model.Person;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventDetailFragment extends Fragment {

    public static final String ARG_EVENT_ID = "event_id";

    private static final int MIN_PEOPLE_FOR_TRAINING = 4;

    /** Implemented by the host activity. */
    public interface OnDetailActionListener {
        void onAddPersonToEvent(long eventId);
        void onStartQuiz(long eventId);
        void onEditPerson(long eventId, long personId);
    }

    // Views
    private TextView mTvTitle;
    private TextView mTvDate;
    private TextView mTvDescription;
    private ListView mListPeople;
    private View mEmptyPeopleState;
    private Button mBtnAddPerson;
    private Button mBtnStartTraining;

    // Data
    private long mEventId;
    private Event mEvent;
    private PersonAdapter mPersonAdapter;
    private EventDao mEventDao;
    private PersonDao mPersonDao;

    private OnDetailActionListener mListener;

    private final SimpleDateFormat mDateFormat =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static EventDetailFragment newInstance(long eventId) {
        EventDetailFragment f = new EventDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_EVENT_ID, eventId);
        f.setArguments(args);
        return f;
    }

    // -------------------------------------------------------------------------
    // Fragment lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnDetailActionListener) {
            mListener = (OnDetailActionListener) activity;
        }
        // listener is optional – navigation may be handled internally
    }

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
        View root = inflater.inflate(R.layout.fragment_event_detail, container, false);

        mTvTitle = (TextView) root.findViewById(R.id.tv_detail_title);
        mTvDate = (TextView) root.findViewById(R.id.tv_detail_date);
        mTvDescription = (TextView) root.findViewById(R.id.tv_detail_description);
        mListPeople = (ListView) root.findViewById(R.id.list_people);
        mEmptyPeopleState = root.findViewById(R.id.empty_people_state);
        mBtnAddPerson = (Button) root.findViewById(R.id.btn_add_person);
        mBtnStartTraining = (Button) root.findViewById(R.id.btn_start_training);

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getActivity());
        mEventDao = new EventDao(dbHelper);
        mPersonDao = new PersonDao(dbHelper);

        mPersonAdapter = new PersonAdapter(getActivity());
        mListPeople.setAdapter(mPersonAdapter);

        mListPeople.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Person person = mPersonAdapter.getItem(position);
                if (mListener != null) {
                    mListener.onEditPerson(mEventId, person.getId());
                }
            }
        });

        mListPeople.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                Person person = mPersonAdapter.getItem(position);
                showRemovePersonDialog(person);
                return true;
            }
        });

        mBtnAddPerson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddPersonDialog();
            }
        });

        mBtnStartTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = mPersonAdapter.getCount();
                if (count < MIN_PEOPLE_FOR_TRAINING) {
                    Toast.makeText(getActivity(),
                            R.string.need_more_people, Toast.LENGTH_LONG).show();
                    return;
                }
                if (mListener != null) {
                    mListener.onStartQuiz(mEventId);
                }
            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    // -------------------------------------------------------------------------
    // Data
    // -------------------------------------------------------------------------

    private void loadData() {
        if (mEventId <= 0) return;

        mEvent = mEventDao.getById(mEventId);
        if (mEvent == null) return;

        // Header
        mTvTitle.setText(mEvent.getTitle());

        if (mEvent.getDate() > 0) {
            mTvDate.setText(mDateFormat.format(new Date(mEvent.getDate())));
            mTvDate.setVisibility(View.VISIBLE);
        } else {
            mTvDate.setVisibility(View.GONE);
        }

        String desc = mEvent.getDescription();
        if (desc != null && !desc.isEmpty()) {
            mTvDescription.setText(desc);
            mTvDescription.setVisibility(View.VISIBLE);
        } else {
            mTvDescription.setVisibility(View.GONE);
        }

        // People
        List<Person> people = mPersonDao.getByEvent(mEventId);
        mPersonAdapter.setPersons(people);
        updateEmptyState(people.isEmpty());
        updateTrainingButton(people.size());
    }

    private void updateEmptyState(boolean empty) {
        mListPeople.setVisibility(empty ? View.GONE : View.VISIBLE);
        mEmptyPeopleState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void updateTrainingButton(int count) {
        mBtnStartTraining.setEnabled(count >= MIN_PEOPLE_FOR_TRAINING);
        mBtnStartTraining.setAlpha(count >= MIN_PEOPLE_FOR_TRAINING ? 1.0f : 0.4f);
    }

    // -------------------------------------------------------------------------
    // Dialogs
    // -------------------------------------------------------------------------

    /**
     * Shows a picker to add an existing person to this event.
     * Falls back to a toast if no eligible persons exist.
     */
    private void showAddPersonDialog() {
        final List<Person> available = mPersonDao.getNotInEvent(mEventId);

        // First entry is always "Create new person"
        final String[] names = new String[available.size() + 1];
        names[0] = getString(R.string.new_person);
        for (int i = 0; i < available.size(); i++) {
            Person p = available.get(i);
            String name = p.getFullName();
            String company = p.getCompany();
            if (company != null && !company.isEmpty()) {
                name = name + " (" + company + ")";
            }
            names[i + 1] = name;
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_person)
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // Create new person
                            if (mListener != null) {
                                mListener.onEditPerson(mEventId, -1);
                            }
                        } else {
                            Person chosen = available.get(which - 1);
                            mEventDao.addPerson(mEventId, chosen.getId());
                            loadData();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showRemovePersonDialog(final Person person) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.remove_person)
                .setMessage(person.getFullName())
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mEventDao.removePerson(mEventId, person.getId());
                        loadData();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
