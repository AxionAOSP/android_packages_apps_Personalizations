/*
 * Copyright (C) 2023-2024 the risingOS Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.custom.settings.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.android.SystemRestartUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@SearchIndexable
public class Spoof extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    public static final String TAG = "Spoof";
    private static final String SYS_GPHOTOS_SPOOF = "persist.sys.pixelprops.gphotos";
    private static final String KEY_IMPORT_KEYBOX = "import_keybox";
    private static final String KEY_CLEAR_KEYBOX = "clear_keybox";
    private static final String KEYBOX_PATH = "/data/misc/keybox/keybox.xml";

    private Preference mGphotosSpoof;
    private Preference mImportKeybox;
    private Preference mClearKeybox;

    private final ActivityResultLauncher<String> mImportKeyboxLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleKeyboxImport(uri);
                }
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.spoof);

        mGphotosSpoof = findPreference(SYS_GPHOTOS_SPOOF);
        mGphotosSpoof.setOnPreferenceChangeListener(this);

        mClearKeybox = findPreference(KEY_CLEAR_KEYBOX);
        mClearKeybox.setOnPreferenceClickListener(preference -> {
            clearKeybox();
            return true;
        });

        mImportKeybox = findPreference(KEY_IMPORT_KEYBOX);
        mImportKeybox.setOnPreferenceClickListener(preference -> {
            mImportKeyboxLauncher.launch("text/xml");
            return true;
        });
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mGphotosSpoof) {
            SystemRestartUtils.showSystemRestartDialog(getContext());
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    private void handleKeyboxImport(Uri uri) {
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(KEYBOX_PATH)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            setPermissions(KEYBOX_PATH);
            showToast(R.string.import_success);
            SystemRestartUtils.showSystemRestartDialog(getContext());
        } catch (Exception e) {
            Log.e(TAG, "Keybox import failed", e);
            showToast(R.string.import_failed);
        }
    }

    private void setPermissions(String path) {
        try {
            File file = new File(path);
            file.setReadable(false, false);
            file.setWritable(false, false);
            file.setReadable(true, true);
            file.setWritable(true, true);
        } catch (Exception e) {
            Log.e(TAG, "Permission set failed", e);
        }
    }

    private void showToast(int resId) {
        getActivity().runOnUiThread(() -> 
            Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show()
        );
    }

    private void clearKeybox() {
        try {
            File file = new File(KEYBOX_PATH);
            if (file.exists() && file.delete()) {
                showToast(R.string.clear_success);
                SystemRestartUtils.showSystemRestartDialog(getContext());
            } else {
                showToast(R.string.clear_failed);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear keybox", e);
            showToast(R.string.clear_failed);
        }
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.spoof) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
