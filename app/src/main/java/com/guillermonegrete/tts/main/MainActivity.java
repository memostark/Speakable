package com.guillermonegrete.tts.main;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.BottomNavigationViewKt;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.guillermonegrete.tts.R;
import com.guillermonegrete.tts.common.views.NestedHideViewOnScrollBehavior;
import com.guillermonegrete.tts.databinding.ActivityMainBinding;

import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import dagger.hilt.android.AndroidEntryPoint;


@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements MenuProvider {

    NavController navController;
    AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this,
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)); // Using this removes the navigation scrim for some reason, even though it says light it works ok with light mode
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        PreferenceManager.setDefaultValues(this, R.xml.preferences_main, false);
        setActionBar();

        setupNavController();

        addMenuProvider(this);

        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar, (v, windowInsets) -> {
            var insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            var mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top;

            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void setActionBar(){
        setSupportActionBar(binding.toolbar);
    }

    private void setupNavController(){
        var navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
        if(navHostFragment == null) return;
        navController = navHostFragment.getNavController();
        var navView = binding.bottomNavView;
        BottomNavigationViewKt.setupWithNavController(navView, navController);

        navController.addOnDestinationChangedListener((nController, destination, arguments) -> {

            int destId = destination.getId();
            if (destId == R.id.settingsFragmentDest
                    || destId == R.id.webReaderFragment) {
                navView.setVisibility(View.GONE);
            } else {
                navView.setVisibility(View.VISIBLE);
                showBottomBar();
                binding.appBarLayout.setExpanded(true);
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

    public void showBottomBar() {
        var navView = binding.bottomNavView;
        var layoutParams = navView.getLayoutParams();
        if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
            var coordinatorLayoutBehavior =
                    ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
            if (coordinatorLayoutBehavior instanceof NestedHideViewOnScrollBehavior) {
                @SuppressWarnings("unchecked")
                var behavior =
                        (NestedHideViewOnScrollBehavior<BottomNavigationView>) coordinatorLayoutBehavior;
                behavior.slideUp(navView);
            }
        }
    }
}
