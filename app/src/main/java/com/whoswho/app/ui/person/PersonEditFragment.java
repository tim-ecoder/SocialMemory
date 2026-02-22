package com.whoswho.app.ui.person;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.whoswho.app.R;
import com.whoswho.app.db.DatabaseHelper;
import com.whoswho.app.db.EventDao;
import com.whoswho.app.db.PersonDao;
import com.whoswho.app.model.Person;
import com.whoswho.app.util.ImageUtils;

import java.io.File;

public class PersonEditFragment extends Fragment {

    private static final String ARG_PERSON_ID = "person_id";
    private static final String ARG_EVENT_ID  = "event_id";

    private static final int REQUEST_CAMERA  = 1001;
    private static final int REQUEST_GALLERY = 1002;

    private static final int PHOTO_MAX_SIZE = 800;

    private long personId = -1;
    private long eventId  = -1;

    private ImageView photoImage;
    private EditText etFirstName;
    private EditText etLastName;
    private EditText etCompany;
    private EditText etPosition;
    private EditText etContextText;
    private EditText etNote;
    private EditText etHobbies;
    private EditText etInterests;
    private EditText etFamilyStatus;
    private EditText etPartnerName;
    private EditText etChildrenNames;
    private EditText etPetNames;
    private EditText etReligion;
    private EditText etPoliticalViews;

    /** Saved photo path (set after successful photo capture/pick) */
    private String currentPhotoPath;

    // -------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------

    public static PersonEditFragment newInstance(long personId, long eventId) {
        PersonEditFragment f = new PersonEditFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PERSON_ID, personId);
        args.putLong(ARG_EVENT_ID, eventId);
        f.setArguments(args);
        return f;
    }

    // -------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            personId = getArguments().getLong(ARG_PERSON_ID, -1);
            eventId  = getArguments().getLong(ARG_EVENT_ID,  -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_person_edit, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        photoImage       = (ImageView) view.findViewById(R.id.photo_image);
        etFirstName      = (EditText)  view.findViewById(R.id.et_first_name);
        etLastName       = (EditText)  view.findViewById(R.id.et_last_name);
        etCompany        = (EditText)  view.findViewById(R.id.et_company);
        etPosition       = (EditText)  view.findViewById(R.id.et_position);
        etContextText    = (EditText)  view.findViewById(R.id.et_context_text);
        etNote           = (EditText)  view.findViewById(R.id.et_note);
        etHobbies        = (EditText)  view.findViewById(R.id.et_hobbies);
        etInterests      = (EditText)  view.findViewById(R.id.et_interests);
        etFamilyStatus   = (EditText)  view.findViewById(R.id.et_family_status);
        etPartnerName    = (EditText)  view.findViewById(R.id.et_partner_name);
        etChildrenNames  = (EditText)  view.findViewById(R.id.et_children_names);
        etPetNames       = (EditText)  view.findViewById(R.id.et_pet_names);
        etReligion       = (EditText)  view.findViewById(R.id.et_religion);
        etPoliticalViews = (EditText)  view.findViewById(R.id.et_political_views);
        Button btnSave   = (Button)    view.findViewById(R.id.btn_save);

        photoImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPhotoDialog();
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePerson();
            }
        });

        if (personId != -1) {
            loadPerson();
        }
    }

    // -------------------------------------------------------------------
    // Load existing person
    // -------------------------------------------------------------------

    private void loadPerson() {
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getActivity());
        PersonDao dao = new PersonDao(dbHelper);
        Person person = dao.getById(personId);
        if (person == null) return;

        etFirstName.setText(person.getFirstName());
        etLastName.setText(nullToEmpty(person.getLastName()));
        etCompany.setText(nullToEmpty(person.getCompany()));
        etPosition.setText(nullToEmpty(person.getPosition()));
        etContextText.setText(nullToEmpty(person.getContext()));
        etNote.setText(nullToEmpty(person.getNote()));
        etHobbies.setText(nullToEmpty(person.getHobbies()));
        etInterests.setText(nullToEmpty(person.getInterests()));
        etFamilyStatus.setText(nullToEmpty(person.getFamilyStatus()));
        etPartnerName.setText(nullToEmpty(person.getPartnerName()));
        etChildrenNames.setText(nullToEmpty(person.getChildrenNames()));
        etPetNames.setText(nullToEmpty(person.getPetNames()));
        etReligion.setText(nullToEmpty(person.getReligion()));
        etPoliticalViews.setText(nullToEmpty(person.getPoliticalViews()));

        currentPhotoPath = person.getPhotoPath();
        if (currentPhotoPath != null) {
            Bitmap bm = ImageUtils.loadScaled(currentPhotoPath, PHOTO_MAX_SIZE);
            if (bm != null) {
                photoImage.setImageBitmap(bm);
            }
        }
    }

    // -------------------------------------------------------------------
    // Photo dialog
    // -------------------------------------------------------------------

    private void showPhotoDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_photo)
                .setItems(new CharSequence[]{
                        getString(R.string.take_photo),
                        getString(R.string.choose_from_gallery)
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            launchCamera();
                        } else {
                            launchGallery();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void launchCamera() {
        Intent intent = PhotoHelper.createCameraIntent(getActivity());
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Toast.makeText(getActivity(), "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        Intent intent = PhotoHelper.createGalleryIntent();
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    // -------------------------------------------------------------------
    // Activity result
    // -------------------------------------------------------------------

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) return;

        Uri photoUri = null;

        if (requestCode == REQUEST_CAMERA) {
            // Camera stores to the uri we provided; data may be null for full-res
            photoUri = PhotoHelper.getCameraOutputUri();
            if (photoUri == null && data != null && data.getData() != null) {
                photoUri = data.getData();
            }
            // Some devices return a thumbnail in extras
            if (photoUri == null && data != null && data.getExtras() != null) {
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                if (thumbnail != null) {
                    handleBitmap(thumbnail);
                    return;
                }
            }
        } else if (requestCode == REQUEST_GALLERY) {
            if (data != null) {
                photoUri = data.getData();
            }
        }

        if (photoUri != null) {
            Bitmap bm = PhotoHelper.processPhoto(getActivity(), photoUri, PHOTO_MAX_SIZE);
            if (bm != null) {
                handleBitmap(bm);
            } else {
                Toast.makeText(getActivity(), "Could not load photo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleBitmap(Bitmap bm) {
        String filename = "person_" + System.currentTimeMillis() + ".jpg";
        String savedPath = ImageUtils.saveBitmap(getActivity(), bm, filename);
        if (savedPath != null) {
            currentPhotoPath = savedPath;
            Bitmap circular = ImageUtils.getCircularBitmap(bm);
            photoImage.setImageBitmap(circular != null ? circular : bm);
        } else {
            Toast.makeText(getActivity(), "Could not save photo", Toast.LENGTH_SHORT).show();
        }
    }

    // -------------------------------------------------------------------
    // Save
    // -------------------------------------------------------------------

    private void savePerson() {
        String firstName = etFirstName.getText().toString().trim();

        if (TextUtils.isEmpty(firstName)) {
            Toast.makeText(getActivity(), R.string.name_required, Toast.LENGTH_SHORT).show();
            etFirstName.requestFocus();
            return;
        }

        if (currentPhotoPath == null) {
            Toast.makeText(getActivity(), R.string.photo_required, Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(getActivity());
        PersonDao personDao = new PersonDao(dbHelper);
        EventDao  eventDao  = new EventDao(dbHelper);

        Person person;
        if (personId != -1) {
            person = personDao.getById(personId);
            if (person == null) person = new Person();
        } else {
            person = new Person();
        }

        person.setFirstName(firstName);
        person.setLastName(etLastName.getText().toString().trim());
        person.setCompany(etCompany.getText().toString().trim());
        person.setPosition(etPosition.getText().toString().trim());
        person.setContext(etContextText.getText().toString().trim());
        person.setNote(etNote.getText().toString().trim());
        person.setHobbies(etHobbies.getText().toString().trim());
        person.setInterests(etInterests.getText().toString().trim());
        person.setFamilyStatus(etFamilyStatus.getText().toString().trim());
        person.setPartnerName(etPartnerName.getText().toString().trim());
        person.setChildrenNames(etChildrenNames.getText().toString().trim());
        person.setPetNames(etPetNames.getText().toString().trim());
        person.setReligion(etReligion.getText().toString().trim());
        person.setPoliticalViews(etPoliticalViews.getText().toString().trim());
        person.setPhotoPath(currentPhotoPath);
        person.setPhotoUpdatedAt(System.currentTimeMillis());

        long savedPersonId;
        if (personId != -1) {
            personDao.update(person);
            savedPersonId = personId;
        } else {
            savedPersonId = personDao.insert(person);
        }

        if (eventId != -1 && savedPersonId != -1) {
            eventDao.addPerson(eventId, savedPersonId);
        }

        getFragmentManager().popBackStack();
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
