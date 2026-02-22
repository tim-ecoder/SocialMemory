package com.whoswho.app.ui.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Toast;

import com.whoswho.app.R;
import com.whoswho.app.util.JsonExporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen with Export/Import functionality.
 */
public class SettingsFragment extends Fragment {

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnExport = (Button) view.findViewById(R.id.btn_export);
        Button btnImport = (Button) view.findViewById(R.id.btn_import);

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmExport();
            }
        });

        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmImport();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Export
    // -------------------------------------------------------------------------

    private void confirmExport() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.export_data)
                .setMessage(R.string.export_confirm)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doExport();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void doExport() {
        String path = JsonExporter.exportData(getActivity());
        if (path != null) {
            Toast.makeText(getActivity(),
                    getString(R.string.export_success) + "\n" + path,
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(),
                    R.string.export_error, Toast.LENGTH_SHORT).show();
        }
    }

    // -------------------------------------------------------------------------
    // Import
    // -------------------------------------------------------------------------

    private void confirmImport() {
        // Find available JSON files
        final List<File> jsonFiles = findJsonFiles();

        if (jsonFiles.isEmpty()) {
            Toast.makeText(getActivity(),
                    "No backup files found in " + JsonExporter.getExportDirPath(getActivity()),
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Show file picker dialog
        final String[] fileNames = new String[jsonFiles.size()];
        for (int i = 0; i < jsonFiles.size(); i++) {
            fileNames[i] = jsonFiles.get(i).getName();
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.import_data)
                .setItems(fileNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showImportConfirm(jsonFiles.get(which));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showImportConfirm(final File file) {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.import_data)
                .setMessage(R.string.import_confirm)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doImport(file.getAbsolutePath());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void doImport(String path) {
        boolean success = JsonExporter.importData(getActivity(), path);
        if (success) {
            Toast.makeText(getActivity(),
                    R.string.import_success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(),
                    R.string.import_error, Toast.LENGTH_SHORT).show();
        }
    }

    // -------------------------------------------------------------------------
    // File search
    // -------------------------------------------------------------------------

    /**
     * Finds JSON backup files in standard export locations.
     */
    private List<File> findJsonFiles() {
        List<File> results = new ArrayList<>();

        // Check Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        addJsonFiles(downloadsDir, results);

        // Check app-specific external dir
        File appExtDir = getActivity().getExternalFilesDir(null);
        if (appExtDir != null) {
            addJsonFiles(appExtDir, results);
        }

        // Check app internal dir
        addJsonFiles(getActivity().getFilesDir(), results);

        return results;
    }

    private static void addJsonFiles(File dir, List<File> results) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".json")) {
                results.add(f);
            }
        }
    }
}
