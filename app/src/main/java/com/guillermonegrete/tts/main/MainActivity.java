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
import androidx.annotation.Nullable;
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

    Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);
        setActionBar();

        final Fragment ttsFrag = new TextToSpeechFragment();
        final Fragment savedWordsFrag = new SavedWordsFragment();
        final Fragment importTextFrag = new ImportTextFragment();


        activeFragment = ttsFrag;

        navView = findViewById(R.id.bottom_nav_view);
        navView.setSelectedItemId(R.id.nav_item_main);
        navView.setOnNavigationItemSelectedListener(menuItem -> {
            menuItem.setChecked(true);

            switch (menuItem.getItemId()){
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

        if(savedInstanceState == null){
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction().add(R.id.main_tts_fragment_container, importTextFrag, "3").hide(importTextFrag).commit();
            fragmentManager.beginTransaction().add(R.id.main_tts_fragment_container, savedWordsFrag, "2").hide(savedWordsFrag).commit();
            fragmentManager.beginTransaction().add(R.id.main_tts_fragment_container, ttsFrag, "1").commit();
        }
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

        /*getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @NonNull View v, @Nullable Bundle savedInstanceState) {
//                TransitionManager.beginDelayedTransition(, Slide(Gravity.BOTTOM).excludeTarget(R.id.nav_host_fragment, true));
                boolean arrowBtn;
                System.out.println("Lifecycle callback registered called");

                if( f instanceof SettingsFragment){
                    navView.setVisibility(View.GONE);
                    arrowBtn = true;
                }else {

                    getSupportFragmentManager().beginTransaction().show(activeFragment).commit();

                    arrowBtn = false;
                }
                actionbar.setHomeButtonEnabled(arrowBtn);
                actionbar.setDisplayHomeAsUpEnabled(arrowBtn);
            }
        }, true);*/
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

                // Config bars
                actionbar.setTitle("");
                actionbar.setHomeButtonEnabled(true);
                actionbar.setDisplayHomeAsUpEnabled(true);
                navView.setVisibility(View.GONE);
                return true;
            }

            case android.R.id.home:{
                onBackPressed();
                return true;
            }


        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        System.out.println("Stack size " + fm.getBackStackEntryCount());

        if (fm.getBackStackEntryCount() > 0){
            fm.popBackStackImmediate();
            actionbar.setTitle(barTitle);
            navView.setVisibility(View.VISIBLE);
            fm.beginTransaction().show(activeFragment).commit();

            actionbar.setHomeButtonEnabled(false);
            actionbar.setDisplayHomeAsUpEnabled(false);
        } else super.onBackPressed();
    }
}
