package com.socialmemory.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView tabMemories;
    private TextView tabPeople;
    private TextView tabTimeline;
    private int selectedTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupStatusBar();
        setupTabs();
        setupButtons();
        setupFab();
    }

    private void setupStatusBar() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#FF3700B3"));
    }

    private void setupTabs() {
        tabMemories = (TextView) findViewById(R.id.tabMemories);
        tabPeople = (TextView) findViewById(R.id.tabPeople);
        tabTimeline = (TextView) findViewById(R.id.tabTimeline);

        View.OnClickListener tabListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.tabMemories) {
                    selectTab(0);
                } else if (v.getId() == R.id.tabPeople) {
                    selectTab(1);
                } else if (v.getId() == R.id.tabTimeline) {
                    selectTab(2);
                }
            }
        };

        tabMemories.setOnClickListener(tabListener);
        tabPeople.setOnClickListener(tabListener);
        tabTimeline.setOnClickListener(tabListener);
    }

    private void selectTab(int index) {
        selectedTab = index;

        int activeColor = Color.parseColor("#FF6200EE");
        int inactiveColor = Color.parseColor("#FF9E9E9E");

        tabMemories.setTextColor(index == 0 ? activeColor : inactiveColor);
        tabMemories.setBackgroundResource(index == 0 ? R.drawable.bg_tab_indicator : 0);
        tabMemories.setTextSize(14);
        if (index == 0) tabMemories.getPaint().setFakeBoldText(true);
        else tabMemories.getPaint().setFakeBoldText(false);

        tabPeople.setTextColor(index == 1 ? activeColor : inactiveColor);
        tabPeople.setBackgroundResource(index == 1 ? R.drawable.bg_tab_indicator : 0);
        tabPeople.setTextSize(14);
        if (index == 1) tabPeople.getPaint().setFakeBoldText(true);
        else tabPeople.getPaint().setFakeBoldText(false);

        tabTimeline.setTextColor(index == 2 ? activeColor : inactiveColor);
        tabTimeline.setBackgroundResource(index == 2 ? R.drawable.bg_tab_indicator : 0);
        tabTimeline.setTextSize(14);
        if (index == 2) tabTimeline.getPaint().setFakeBoldText(true);
        else tabTimeline.getPaint().setFakeBoldText(false);

        String[] tabNames = {"Memories", "People", "Timeline"};
        Toast.makeText(this, tabNames[index] + " selected", Toast.LENGTH_SHORT).show();
    }

    private void setupButtons() {
        TextView btnGetStarted = (TextView) findViewById(R.id.btnGetStarted);
        TextView btnSignIn = (TextView) findViewById(R.id.btnSignIn);

        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,
                    "Welcome! Let's create your first memory.",
                    Toast.LENGTH_LONG).show();
            }
        });

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,
                    "Sign in coming soon!",
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFab() {
        TextView fab = (TextView) findViewById(R.id.fabAdd);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,
                    "Create new memory",
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}
