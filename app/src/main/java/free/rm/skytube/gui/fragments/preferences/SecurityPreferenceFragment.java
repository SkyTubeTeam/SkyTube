package free.rm.skytube.gui.fragments.preferences;

import android.os.Bundle;
import free.rm.skytube.R;

public class SecurityPreferenceFragment extends BasePreferenceFragment {
    @Override
    protected void showPreferencesInternal(String rootKey) {
        addPreferencesFromResource(R.xml.preference_security);
    }

    @Override
    public void onSharedPreferenceChanged(android.content.SharedPreferences sharedPreferences, String key) {
        // No-op
    }
}
