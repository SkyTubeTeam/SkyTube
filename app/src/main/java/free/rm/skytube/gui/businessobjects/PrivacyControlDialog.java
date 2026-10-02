/*
 * SkyTube
 * Copyright (C) 2026
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
package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import free.rm.skytube.R;
import free.rm.skytube.app.Settings;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.databinding.DialogPrivacyControlsBinding;

/**
 * Dialog shown on app startup that informs the user about the privacy-enhancing features
 * provided by SkyTube (SponsorBlock and the Return YouTube Dislike counter) and allows them
 * to enable or disable these features.
 */
public class PrivacyControlDialog {

	private final Context context;

	public PrivacyControlDialog(@NonNull Context context) {
		this.context = context;
	}

	public void show() {
		final DialogPrivacyControlsBinding binding =
				DialogPrivacyControlsBinding.inflate(LayoutInflater.from(context));
		final Settings settings = SkyTubeApp.getSettings();

		binding.privacyControlSbSwitch.setChecked(settings.isSponsorblockEnabled());
		binding.privacyControlRydSwitch.setChecked(settings.isUseDislikeApi());

		new AlertDialog.Builder(context)
				.setTitle(R.string.privacy_control_title)
				.setView(binding.getRoot())
				.setPositiveButton(R.string.ok, (dialog, which) -> {
					settings.setSponsorblockEnabled(binding.privacyControlSbSwitch.isChecked());
					settings.setUseDislikeApi(binding.privacyControlRydSwitch.isChecked());
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}
}