//
// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license.
//
// Microsoft Cognitive Services (formerly Project Oxford): https://www.microsoft.com/cognitive-services
//
// Microsoft Cognitive Services (formerly Project Oxford) GitHub:
// https://github.com/Microsoft/Cognitive-Speech-TTS
//
// Copyright (c) Microsoft Corporation
// All rights reserved.
//
// MIT License:
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//

package com.guillermonegrete.tts.main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.importtext.ImportTextFragment;
import com.guillermonegrete.tts.savedwords.SavedWordsFragment;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends AppCompatActivity {
    // Note: Sign up at http://www.projectoxford.ai for the client credentials.

    private ActionBar actionbar;
    private BottomNavigationView navView;

    private String barTitle;
    private int currentTab;

    Fragment activeFragment;
    FragmentManager fragmentManager = getSupportFragmentManager();

    private static String CURRENT_SELECTED_FRAGMENT = "current fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);
        setActionBar();

        final Fragment ttsFrag;
        final Fragment savedWordsFrag;
        final Fragment importTextFrag;

        navView = findViewById(R.id.bottom_nav_view);

        if(savedInstanceState == null){
            ttsFrag = new TextToSpeechFragment();
            savedWordsFrag = new SavedWordsFragment();
            importTextFrag = new ImportTextFragment();

            fragmentManager.beginTransaction().add(R.id.main_tts_fragment_container, importTextFrag, "3").hide(importTextFrag).commit();
            fragmentManager.beginTransaction().add(R.id.main_tts_fragment_container, savedWordsFrag, "2").hide(savedWordsFrag).commit();
            fragmentManager.beginTransaction().add(R.id.main_tts_fragment_container, ttsFrag, "1").commit();

            activeFragment = ttsFrag;
        } else {
            ttsFrag = fragmentManager.findFragmentByTag("1");
            savedWordsFrag = fragmentManager.findFragmentByTag("2");
            importTextFrag = fragmentManager.findFragmentByTag("3");

            String item;
            currentTab = savedInstanceState.getInt(CURRENT_SELECTED_FRAGMENT);
            switch (currentTab){
                case R.id.nav_item_import:
                    activeFragment = importTextFrag;
                    break;
                case R.id.nav_item_saved:
                    activeFragment = savedWordsFrag;
                    break;
                case R.id.nav_item_main:
                default:
                    activeFragment = ttsFrag;
                    break;
            }

            Fragment settingsFrag = fragmentManager.findFragmentById(R.id.main_tts_fragment_container);
            if(settingsFrag instanceof SettingsFragment){
                setHiddenNavView();
            }
        }

        navView.setOnNavigationItemSelectedListener(menuItem -> {
            menuItem.setChecked(true);

            int tabId = menuItem.getItemId();
            currentTab = tabId;

            switch (tabId){
                case R.id.nav_item_saved:
                    barTitle = getString(R.string.saved);
                    changeFragment(savedWordsFrag);
                    return true;
                case R.id.nav_item_import:
                    barTitle = getString(R.string.import_text);
                    changeFragment(importTextFrag);
                    return true;
                case R.id.nav_item_main:
                    barTitle = getString(R.string.main);
                    changeFragment(ttsFrag);
                    return true;
            }

            return false;
        });

        navView.setOnNavigationItemReselectedListener(item -> {});
    }

    private void setActionBar(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionbar = getSupportActionBar();
        if(actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            barTitle = getString(R.string.main);
            actionbar.setTitle(barTitle);
            actionbar.setHomeButtonEnabled(false);
            actionbar.setDisplayHomeAsUpEnabled(false);
        }
    }

    private void changeFragment(Fragment newFragment){
        actionbar.setTitle(barTitle);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.hide(activeFragment).show(newFragment).commit();
        activeFragment = newFragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.settings_menu_item: {
                Fragment newFragment  = new SettingsFragment();

                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.hide(activeFragment);
                fragmentTransaction.add(R.id.main_tts_fragment_container, newFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();

                setHiddenNavView();
                return true;
            }

            case android.R.id.home:{
                onBackPressed();
                return true;
            }


        }
        return super.onOptionsItemSelected(item);
    }

    private void setHiddenNavView(){
        // Config bars
        actionbar.setTitle("");
        actionbar.setHomeButtonEnabled(true);
        actionbar.setDisplayHomeAsUpEnabled(true);
        navView.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();

        if (fm.getBackStackEntryCount() > 0){
            fm.popBackStackImmediate();
            actionbar.setTitle(barTitle);
            navView.setVisibility(View.VISIBLE);
            fm.beginTransaction().show(activeFragment).commit();

            actionbar.setHomeButtonEnabled(false);
            actionbar.setDisplayHomeAsUpEnabled(false);
        } else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_SELECTED_FRAGMENT, currentTab);
    }
}
