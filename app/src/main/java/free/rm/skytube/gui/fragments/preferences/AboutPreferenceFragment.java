/*
 * SkyTube
 * Copyright (C) 2017  Ramon Mifsud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.gui.fragments.preferences;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.text.SimpleDateFormat;
import java.util.Locale;

import free.rm.skytube.BuildConfig;
import free.rm.skytube.R;
import free.rm.skytube.gui.businessobjects.SkyTubeMaterialDialog;
import free.rm.skytube.gui.businessobjects.updates.UpdatesCheckerTask;

/**
 * Preference fragment for about (this app) related settings.
 */
public class AboutPreferenceFragment extends PreferenceFragmentCompat {
	private static final String TAG = AboutPreferenceFragment.class.getSimpleName();

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
		addPreferencesFromResource(R.xml.preference_about);

		// set the app's version number
		Preference versionPref = findPreference(getString(R.string.pref_key_version));
		versionPref.setSummary(getAppVersion());

		// check for updates option
		Preference updatesPref = findPreference(getString(R.string.pref_key_updates));
		if (BuildConfig.FLAVOR.equalsIgnoreCase("oss") || BuildConfig.BUILD_TYPE.equalsIgnoreCase("snapshot")) {
			// remove the updates option if the user is running the OSS flavor...
			getPreferenceScreen().removePreference(updatesPref);
		} else {
			updatesPref.setOnPreferenceClickListener(preference -> {
				new UpdatesCheckerTask(getActivity(), true).executeInParallel();
				return true;
			});
		}

		// if the user clicks on the website link, then open it using an external browser
		Preference websitePref = findPreference(getString(R.string.pref_key_website));
		websitePref.setSummary(BuildConfig.SKYTUBE_WEBSITE);
		websitePref.setOnPreferenceClickListener(preference -> {
			// view the app's website in a web browser
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.SKYTUBE_WEBSITE));
			startActivity(browserIntent);
			return true;
		});

		// credits
		Preference creditsPref = findPreference(getString(R.string.pref_key_credits));
		creditsPref.setOnPreferenceClickListener(preference -> {
			displayCredits();
			return true;
		});

		// if the user clicks on the license, then open the display the actual license
		Preference licensePref = findPreference(getString(R.string.pref_key_license));
		licensePref.setOnPreferenceClickListener(preference -> {
			displayAppLicense();
			return true;
		});
	}

	/**
	 * @return The app's version number.
	 */
	private String getAppVersion() {
		StringBuilder ver = new StringBuilder(BuildConfig.VERSION_NAME);

		if (BuildConfig.FLAVOR.equalsIgnoreCase("extra")) {
			ver.append(" Extra");
		}

		if (BuildConfig.BUILD_TYPE.equalsIgnoreCase("snapshot")) {
			ver.append(" Snapshot");
		}

		if (BuildConfig.DEBUG) {
			ver.append(" (Debug ").append(getAppBuildTimeStamp()).append(')');
		} else {
			ver.append(" (").append(getAppBuildTimeStamp()).append(')');
		}

		return ver.toString();
	}

	/**
	 * @return App's build timestamp.
	 */
	private static String getAppBuildTimeStamp() {
		String timeStamp = "???";

		try {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
			timeStamp = formatter.format(BuildConfig.BUILD_TIME);
		} catch (Throwable tr) {
			Log.d(TAG, "An error occurred while getting app's build timestamp", tr);
		}

		return timeStamp;
	}

	/**
	 * Displays the credits (i.e. contributors).
	 */
	private void displayCredits() {
		Context context = getContext();
		final WebView webView = new WebView(context);
		webView.setWebChromeClient(new WebChromeClient() {
			ProgressDialog progressDialog;

			@Override
			public void onProgressChanged(WebView view, int progress) {
				if (progressDialog == null) {
					progressDialog = new ProgressDialog(context);
					progressDialog.show();
					progressDialog.setMessage(getString(R.string.loading));
				}

				if (progress >= 100) {
					progressDialog.dismiss();
					progressDialog = null;
				}
			}
		});
		webView.getSettings().setJavaScriptEnabled(false);
		webView.loadUrl(BuildConfig.SKYTUBE_WEBSITE_CREDITS);

		new SkyTubeMaterialDialog(getActivity())
				.customView(webView, true)
				.negativeText("")
				.show();
	}

	/**
	 * Displays the app's license in an AlertDialog.
	 */
	private void displayAppLicense() {
		new AlertDialog.Builder(getActivity())
				.setMessage(R.string.app_license)
				.setNeutralButton(R.string.i_agree, null)
				.setCancelable(false)	// do not allow the user to click outside the dialog or press the back button
				.show();
	}
}
