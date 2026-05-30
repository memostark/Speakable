package com.guillermonegrete.tts.main;

import static com.guillermonegrete.tts.importtext.ImportTextFragment.MARGIN_OFFSET_NAME;
import static com.guillermonegrete.tts.importtext.tabs.FilesFragment.MARGIN_OFFSET_KEY;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.MenuProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavArgument;
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
import com.guillermonegrete.tts.main.domain.interactors.BackupManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;


@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements MenuProvider {

    NavController navController;
    AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Inject
    BackupManager backupManager;

    ActivityResultLauncher<Uri> getBackupFolder = registerForActivityResult(
        new ActivityResultContracts.OpenDocumentTree(),
        uri ->
            backupManager.createBackup(
                uri,
                (outputUri) -> Toast.makeText(this, "Backup at: " + outputUri, Toast.LENGTH_LONG).show(),
                t -> {
                    Timber.e(t, "Error creating backup");
                    Toast.makeText(this, "Error creating backup", Toast.LENGTH_SHORT).show();
                }
            )
    );

    ActivityResultLauncher<String[]> getBackupFile = registerForActivityResult(
        new ActivityResultContracts.OpenDocument(),
        uri ->
            backupManager.restoreDatabase(
                uri,
                () -> {
                    var dialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.backup_success_dialog_title)
                            .setMessage(R.string.backup_success_dialog_message)
                            .setNegativeButton(R.string.no, (dialog1, which) -> dialog1.dismiss())
                            .setPositiveButton(R.string.yes, (dialog1, which) -> restartApp())
                            .create();
                    dialog.show();
                },
                t -> {
                    Timber.e(t, "Error loading backup");
                    Toast.makeText(this, "Error loading backup", Toast.LENGTH_SHORT).show();
                }
            )
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this,
                SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
                SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)); // Using this removes the navigation scrim for some reason, even though it says light it works ok with light mode

        if (savedInstanceState != null) {
            // On configuration change, the fragments args may be saved. Update the args to avoid using the old ones.
            updateMarginArguments(savedInstanceState);
        }

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

            // Return CONSUMED if you don't want the window insets to keep passing
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
        var walletGraph = navController.getGraph().findNode(R.id.importtext);
        if (walletGraph != null) walletGraph.addArgument(MARGIN_OFFSET_NAME, new NavArgument.Builder().setDefaultValue(offset).build());

        var navView = binding.mainNavView;
        if (navView instanceof NavigationBarView nv) {
            NavigationUI.setupWithNavController(nv, navController);
        } else if (navView instanceof NavigationView nv) {
            NavigationUI.setupWithNavController(nv, navController);
        }

        navController.addOnDestinationChangedListener((nController, destination, arguments) -> {

            int destId = destination.getId();
            if (destId == R.id.settingsFragmentDest
                    || destId == R.id.webReaderFragment
                    || destId == R.id.notesListFragment) {
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
        var id = item.getItemId();
        if (id == R.id.settings_menu_item) {
            navController.navigate(R.id.action_global_settingsFragment);
            return true;
        } else if (id == R.id.create_backup_item) {
            getBackupFolder.launch(null);
            return true;
        } else if (id == R.id.restore_from_backup_item) {
            var dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.restore_backup_dialog_title)
                    .setMessage(R.string.restore_backup_dialog_message)
                    .setNegativeButton(android.R.string.cancel, (dialog1, which) -> dialog1.dismiss())
                    .setPositiveButton(android.R.string.ok, (dialog1, which) ->
                        getBackupFile.launch(new String[]{"application/zip", "application/x-zip-compressed"})
                    )
                    .create();
            dialog.show();
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

    private void restartApp() {
        var context = getApplicationContext();
        var pm = context.getPackageManager();
        var intent = pm.getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) return;
        var mainIntent = Intent.makeRestartActivityTask(intent.getComponent());
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    private void updateMarginArguments(Bundle savedInstanceState) {
        var bundlableSavedStateRegistry = savedInstanceState.getBundle("androidx.lifecycle.BundlableSavedStateRegistry.key");
        if (bundlableSavedStateRegistry == null) return;

        var fragments = bundlableSavedStateRegistry.getBundle("android:support:fragments");
        if (fragments == null) return;

        var importFragment = getImportFragmentBundle(fragments);
        if (importFragment == null) return;

        var args = importFragment.getBundle("arguments");
        if (args == null) return;

        var newMargin = getResources().getDimensionPixelSize(R.dimen.main_act_bars_height);
        var diff = newMargin - args.getInt(MARGIN_OFFSET_NAME);
        args.putInt(MARGIN_OFFSET_NAME, newMargin);

        // Update child fragments of import text
        var importFragmentManager = importFragment.getBundle("childFragmentManager");
        if (importFragmentManager == null) return;
        for (String key : importFragmentManager.keySet()) {
            if (!key.startsWith("fragment_")) continue;
            var fragment = importFragmentManager.getBundle(key);
            if (fragment == null) continue;
            args = fragment.getBundle("arguments");
            if (args != null && args.containsKey(MARGIN_OFFSET_KEY)) {
                args.putInt(MARGIN_OFFSET_KEY, args.getInt(MARGIN_OFFSET_KEY) + diff);
            }
        }
    }

    private @Nullable Bundle getImportFragmentBundle(Bundle fragments) {
        Bundle importFragment = null;
        for (String key: fragments.keySet()) {
            if (!key.startsWith("fragment_")) continue;
            var navHostFragment = fragments.getBundle(key);
            if (navHostFragment == null) continue;
            var childFragmentManager = navHostFragment.getBundle("childFragmentManager");
            if (childFragmentManager == null) continue;
            for (String childKey : childFragmentManager.keySet()) {
                if (!childKey.startsWith("fragment_")) continue;
                var frag = childFragmentManager.getBundle(childKey);
                if (frag == null) continue;
                var args = frag.getBundle("arguments");
                if (args != null && args.containsKey(MARGIN_OFFSET_NAME)) {
                    importFragment = frag;
                }
            }
        }

        return importFragment;
    }
}
