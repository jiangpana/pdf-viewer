package com.avrapps.pdfviewer;

import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_IMAGE_TO_PDF;
import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_MERGE_PDF;
import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_UNLOCK_PDF;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.avrapps.pdfviewer.library_fragment.LibraryFragment;
import com.avrapps.pdfviewer.others_fragments.OthersFragment;
import com.avrapps.pdfviewer.results_fragment.FirebaseUtils;
import com.avrapps.pdfviewer.results_fragment.ResultsFragment;
import com.avrapps.pdfviewer.settings_fragment.SettingsFragment;
import com.avrapps.pdfviewer.settings_fragment.constants.AppConstants;
import com.avrapps.pdfviewer.tools_fragment.BrowseFilesToolsFragment;
import com.avrapps.pdfviewer.tools_fragment.ToolsFragment;
import com.avrapps.pdfviewer.utils.AppRater;
import com.avrapps.pdfviewer.utils.BillinUtils;
import com.avrapps.pdfviewer.utils.MessagingUtility;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.avrapps.pdfviewer.utils.PermissionUtils;
import com.avrapps.pdfviewer.utils.PreferenceUtil;
import com.avrapps.pdfviewer.viewer_fragment.DocumentFragment;
import com.google.android.play.core.splitcompat.SplitCompat;
import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    PermissionUtils permissionUtils;
    String fragmentOpen = "";
    DocumentFragment documentFragment;
    boolean shouldExitOnBackButton = false;
    LibraryFragment libraryFragment;
    int operation = 0;
    BrowseFilesToolsFragment browseFilesToolsFragment;
    ResultsFragment resultsFragment;
    LinearLayout buttonPanel;
    int primaryColor = R.color.black;
    boolean shouldShowListGridOption = false;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MiscUtils.setTheme(this);
        MiscUtils.setLocale(this);
        setContentView(R.layout.main_activity);
        buttonPanel = findViewById(R.id.buttonPanel);
        permissionUtils = new PermissionUtils();
        AppRater.app_launched(this);
        setSupportActionBar(findViewById(R.id.my_toolbar));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            primaryColor = PreferenceUtil.getPrimaryColor(this);
        }
        setButtonBackground(R.id.open_file_button);
        if (permissionUtils.checkRunTimePermission(this)) {
            restoreOpenFragmentOnReopen(savedInstanceState);
        }
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        SplitCompat.installActivity(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (BillinUtils.isApplicationBought(this)) {
//            menu.findItem(R.id.pro_option).setVisible(false);
        }
        MenuItem listGridMenuItem = menu.findItem(R.id.list_grid);
        if (shouldShowListGridOption) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isListViewPreferred = preferences.getBoolean("isListViewPreferred", false);
            listGridMenuItem.setIcon(isListViewPreferred ? R.drawable.ic_baseline_view_list_24 : R.drawable.ic_baseline_grid_view_24);
        }
        listGridMenuItem.setVisible(shouldShowListGridOption);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.pro_option) {
//            MessagingUtility.showBuyApplicationDailog(this);
//        } else if (item.getItemId() == R.id.support_option) {
//            MessagingUtility.showSupportDialog(this);
//        } else
            if (item.getItemId() == R.id.list_grid) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isListViewPreferred = preferences.getBoolean("isListViewPreferred", false);
            SharedPreferences.Editor edit = preferences.edit();
            edit.putBoolean("isListViewPreferred", !isListViewPreferred);
            edit.apply();
            invalidateOptionsMenu();
            refreshCurrentFragment();
        }
        return true;
    }

    private void refreshCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
        if (currentFragment != null) {
            if (currentFragment instanceof LibraryFragment) {
                ((LibraryFragment) currentFragment).onRefresh();
            }
        }
    }

    private void restoreOpenFragmentOnReopen(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String message = savedInstanceState.getString("fragmentOpen");
            if (message != null && message.equals("SettingsFragment")) {
                openSettingsFragment(null);
                setButtonBackground(R.id.open_settings_button);
                return;
            }
        }
        continueOperations();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionUtils.validatePermissions(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("fragmentOpen", fragmentOpen);
        super.onSaveInstanceState(outState);
    }

    public void openSettingsFragment(View view) {
        if (fragmentOpen.equals("SettingsFragment")) return;
        fragmentOpen = "SettingsFragment";
        showGridListViewOption(false);
        setButtonBackground(R.id.open_settings_button);
        openFragment(new SettingsFragment());
    }

    public void openToolsFragment(View view) {
        if (fragmentOpen.equals("ToolsFragment")) return;
        fragmentOpen = "ToolsFragment";
        showGridListViewOption(false);
        setButtonBackground(R.id.open_tools_button);
        openFragment(new ToolsFragment());
    }

    public void openLibraryFragment(View view) {
        if (fragmentOpen.equals("LibraryFragment")) return;
        fragmentOpen = "LibraryFragment";
        showGridListViewOption(true);
        setButtonBackground(R.id.open_file_button);
        libraryFragment = new LibraryFragment();
        openFragment(libraryFragment);
    }

    public void openOthersFragment(View view) {
        if (fragmentOpen.equals("OthersFragment")) return;
        fragmentOpen = "OthersFragment";
        showGridListViewOption(false);
        setButtonBackground(R.id.more_button);
        SplitInstallManager splitInstallManager = SplitInstallManagerFactory.create(this);
        if (!splitInstallManager.getInstalledModules().contains("DocumentScannerAndSync")) {
            openFragment(new OthersFragment());
        } else {
            openScannerFragment();
        }
    }

    public void openScannerFragment() {
        Fragment fragment;
        try {
            fragment = (Fragment) Class.forName("com.avrapps.documentscanner.ScanFragment").newInstance();
        } catch (Exception e) {
            Toast.makeText(this, R.string.cant_start_feature, Toast.LENGTH_SHORT).show();
            return;
        }
        openFragment(fragment);
    }

    private void openFragment(Fragment f) {
        Log.d("fragmentOpen", fragmentOpen);
//        buttonPanel.setVisibility(
//                (f instanceof BrowseFilesToolsFragment || f instanceof ResultsFragment) ? View.GONE : View.VISIBLE);
        FragmentManager manager = getSupportFragmentManager();
        manager.beginTransaction()
                .replace(R.id.frame_layout, f)
                .commit();
    }

    public void openDocumentFragment(String path, ArrayList<String> selectedFiles) {
        fragmentOpen = "DocumentFragment";
        documentFragment = new DocumentFragment();
        Bundle bundle = new Bundle();
        bundle.putString("path", path);
        bundle.putStringArrayList("additionalFiles", selectedFiles);
        bundle.putBoolean("shouldQuit", shouldExitOnBackButton);
        documentFragment.setArguments(bundle);
        openFragment(documentFragment);
    }

    public void continueOperations() {
        openLibraryFragment(null);
        Uri uri = getIntent().getData();
        if (uri != null) {
            shouldExitOnBackButton = true;
            MiscUtils.openDoc(uri, this, new ArrayList<>());
            FirebaseUtils.analyticsFileOpen(this, "FILE_OPEN_EXTERNAL", uri);
        }
    }

    @SuppressLint("ResourceType")
    void setButtonBackground(int id) {
        try {
            TypedValue typedValue = new TypedValue();
            getApplicationContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);

            Button mainButton = findViewById(id);
            findViewById(R.id.open_file_button).setBackgroundResource(typedValue.resourceId);
            findViewById(R.id.open_tools_button).setBackgroundResource(typedValue.resourceId);
            findViewById(R.id.open_settings_button).setBackgroundResource(typedValue.resourceId);
            findViewById(R.id.more_button).setBackgroundResource(typedValue.resourceId);
            mainButton.setBackgroundColor(ContextCompat.getColor(this, R.color.white));

            setButtonStyle(R.id.open_file_button, R.color.black);
            setButtonStyle(R.id.open_tools_button, R.color.black);
            setButtonStyle(R.id.open_settings_button, R.color.black);
            setButtonStyle(R.id.more_button, R.color.black);
            setButtonStyle(id, primaryColor);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setButtonStyle(int button, int primaryColor) {
        Button mainButton = findViewById(button);
        Drawable[] drawables = mainButton.getCompoundDrawables();
        for (Drawable drawable : drawables) {
            if (drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(ContextCompat.getColor(this, primaryColor), PorterDuff.Mode.SRC_ATOP);
            }
        }
        mainButton.setTextColor(ContextCompat.getColor(this, primaryColor));
    }


    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
        if (fragment instanceof LibraryFragment && libraryFragment != null) {
            libraryFragment.onBackPressed();
        } else if (fragment instanceof DocumentFragment) {//|| fragment instanceof DocFragment ) {
            if (documentFragment != null && documentFragment.onBackPressed()) {
                if (shouldExitOnBackButton) {
                    finish();
                } else {
                    openLibraryFragment(null);
                    findViewById(R.id.my_toolbar).setVisibility(View.VISIBLE);
                    findViewById(R.id.buttonPanel).setVisibility(View.GONE);
                }
            }
        } else if (fragment instanceof BrowseFilesToolsFragment) {
            if (browseFilesToolsFragment != null) {
                browseFilesToolsFragment.onBackPressed();
            }
        } else if (fragment instanceof ResultsFragment) {
            if (resultsFragment != null) {
                openLibraryFragment(null);
            }
        } else {
            this.finish();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        Log.e("Main", "Response Received on Activity" + resultCode);
        if (requestCode == 4010) {
            if (resultCode == RESULT_OK && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Uri treeUri = resultData.getData();
                DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
                Log.e("Main", "Doc Id : " + pickedDir.getUri());
                Set<String> removableStorages = PreferenceUtil.getStringSet(this, AppConstants.REMOVABLE_STORAGES);
                String sdcard = "";
                for (String storage : removableStorages) {
                    sdcard = new File(storage).getName();
                    if (new File(storage).getName().equals(pickedDir.getName())) {
                        Log.e("Main", "Doc Id matched: " + pickedDir.getUri());
                        grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
                        if (fragment instanceof DocumentFragment) {
                            documentFragment.checkSDCardPermission();
                        }
                        return;
                    }
                }
                MessagingUtility.showSdcardAccessDialog(this, getString(R.string.incorrct_path),
                        getString(R.string.select_proper_path_desc, sdcard),
                        getString(R.string.grant_permission), (String returnValue) -> PermissionUtils.takeCardUriPermission(this), false);
            }
        }

    }

    public void performOperation(int operation) {
        this.operation = operation;
        Bundle bundle = new Bundle();
        bundle.putInt("OPERATION_" + operation, operation);
        mFirebaseAnalytics.logEvent("TOOL_OPERATION_START", bundle);
        openBrowseFilesFragment();
    }

    public void openBrowseFilesFragment() {
        browseFilesToolsFragment = new BrowseFilesToolsFragment();
        Bundle bundle = new Bundle();
        String formats = ".pdf";
        if (operation == TOOL_IMAGE_TO_PDF) {
            formats = ".jpg,.jpeg,.png,.bmp,.tif.,tiff";
        }
        bundle.putBoolean("passwordCheck", !(operation == TOOL_UNLOCK_PDF || operation == TOOL_IMAGE_TO_PDF));
        bundle.putBoolean("multiSelect", operation == TOOL_IMAGE_TO_PDF || operation == TOOL_MERGE_PDF);
        bundle.putInt("minimumSelect", operation == TOOL_MERGE_PDF ? 2 : 1);
        bundle.putString("formats", formats);
        browseFilesToolsFragment.setArguments(bundle);
        openFragment(browseFilesToolsFragment);
    }

    public void continueOperationsOnFileSelect(LinkedHashMap<String, String> datum) {
        resultsFragment = new ResultsFragment(this);
        Bundle bundle = new Bundle();
        bundle.putInt("operationCode", operation);
        bundle.putString("openedFrom", "TOOLS"); // default is viewer
        bundle.putSerializable("filePaths", datum);
        resultsFragment.setArguments(bundle);
        openFragment(resultsFragment);
    }

    public void continueOperationsOnFileSelect(String datum, String password, int operationOverride, Bundle bundleExtra) {
        resultsFragment = new ResultsFragment(this);
        Bundle bundle = new Bundle();
        if (operationOverride > 0) {
            bundle.putInt("operationCode", operationOverride);
        } else {
            bundle.putInt("operationCode", operation);
        }
        bundle.putString("filePath", datum);
        bundle.putAll(bundleExtra);
        if (password != null) {
            bundle.putString("password", password);
        }
        resultsFragment.setArguments(bundle);
        openFragment(resultsFragment);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean(AppConstants.VOLUME_BUTTON_SCROLL, true)) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
            if (fragment instanceof DocumentFragment) {
                if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        ((DocumentFragment) fragment).goToNextPage();
                    }
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        ((DocumentFragment) fragment).goToPrevPage();
                    }
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    public void showGridListViewOption(boolean show) {
        shouldShowListGridOption = show;
        invalidateOptionsMenu();
    }
}
