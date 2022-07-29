package com.guillermonegrete.tts.main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.BottomNavigationViewKt;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.guillermonegrete.tts.R;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import dagger.hilt.android.AndroidEntryPoint;


@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements MenuProvider {

    NavController navController;
    AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);
        setActionBar();

        setupNavController();

        addMenuProvider(this);
    }

    private void setActionBar(){
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupNavController(){
        NavHostFragment navHostFragment  = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
        if(navHostFragment == null) return;
        navController = navHostFragment.getNavController();
        BottomNavigationView navView = findViewById(R.id.bottom_nav_view);
        BottomNavigationViewKt.setupWithNavController(navView, navController);

        navController.addOnDestinationChangedListener((nController, destination, arguments) -> {

            int destId = destination.getId();
            if (destId == R.id.settingsFragmentDest
                    || destId == R.id.webReaderFragment) {
                navView.setVisibility(View.GONE);
            } else {
                navView.setVisibility(View.VISIBLE);
            }
        });

        appBarConfiguration = new AppBarConfiguration.Builder(R.id.main, R.id.saved, R.id.importtext).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.menu_main_activity, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.settings_menu_item) {
            navController.navigate(R.id.action_global_settingsFragment);
            return true;
        }
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Necessary for making the back button in the action bar work
        return NavigationUI.navigateUp(navController, appBarConfiguration);
    }
}
