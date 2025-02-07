package com.guillermonegrete.tts.main;

import static com.guillermonegrete.tts.importtext.ImportTextFragment.MARGIN_OFFSET_NAME;

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
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
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
            var insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            var mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), insets.right, v.getPaddingBottom());
            var nv = binding.landscapeLayout;
            if (nv != null) nv.setPadding(insets.left, nv.getPaddingTop(), insets.right, nv.getPaddingBottom());

            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            return windowInsets;
        });
    }

    private void setActionBar(){
        setSupportActionBar(binding.toolbar);
    }

    private void setupNavController() {
        var navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
        if(navHostFragment == null) return;
        navController = navHostFragment.getNavController();
        var offset = getResources().getDimensionPixelSize(R.dimen.main_act_bars_height);
        var bundle = new Bundle();
        bundle.putInt(MARGIN_OFFSET_NAME, offset);

        var navView = binding.mainNavView;
        if (navView instanceof NavigationBarView nv) {
            NavigationUI.setupWithNavController(nv, navController);
            nv.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.importtext) {
                    navController.navigate(id, bundle);
                    return true;
                }
                navController.navigate(id);
                return true;
            });
        } else if (navView instanceof NavigationView nv) {
            NavigationUI.setupWithNavController(nv, navController);
            nv.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.importtext) {
                    navController.navigate(id, bundle);
                    return true;
                }
                navController.navigate(id);
                return true;
            });
        }

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
        var navView = binding.mainNavView;
        var layoutParams = navView.getLayoutParams();
        if (layoutParams instanceof CoordinatorLayout.LayoutParams) {
            var coordinatorLayoutBehavior =
                    ((CoordinatorLayout.LayoutParams) layoutParams).getBehavior();
            if (coordinatorLayoutBehavior instanceof NestedHideViewOnScrollBehavior && navView instanceof BottomNavigationView nv) {
                @SuppressWarnings("unchecked")
                var behavior =
                        (NestedHideViewOnScrollBehavior<BottomNavigationView>) coordinatorLayoutBehavior;
                behavior.slideUp(nv);
            }
        }
    }
}
