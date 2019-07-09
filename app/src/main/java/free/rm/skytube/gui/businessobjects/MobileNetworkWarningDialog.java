/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

import androidx.annotation.NonNull;
import android.widget.CompoundButton;

import com.afollestad.materialdialogs.MaterialDialog;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;

/**
 * Dialog that warns the user to enable the wi-fi if the device is connected to mobile network
 * (such 4G).  The dialog will ask the user to either cancel the download or proceed.
 *
 * <p>This dialog is available for (1) video downloads and (2) video streaming.</p>
 */
public class MobileNetworkWarningDialog extends SkyTubeMaterialDialog {

	public MobileNetworkWarningDialog(@NonNull Context context) {
		super(context);
	}


	/**
	 * Display the dialog.  The dialog message/contents will be determained by the {@link .actionType}.
	 *
	 * @param actionType    Action Type:  either video downloads or video streaming.
	 *
	 * @return True if the dialog was displayed; false otherwise.
	 */
	public boolean showAndGetStatus(ActionType actionType) {
		final boolean   displayWarning  = SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_warn_mobile_downloads), true);
		boolean         dialogDisplayed = false;

		if (SkyTubeApp.isConnectedToMobile() && displayWarning) {
			title(R.string.mobile_data);
			content(actionType == ActionType.STREAM_VIDEO ? R.string.warning_mobile_network_play : R.string.warning_mobile_network_download);
			checkBoxPromptRes(R.string.warning_mobile_network_disable, false, new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					SkyTubeApp.getPreferenceManager().edit().putBoolean(SkyTubeApp.getStr(R.string.pref_key_warn_mobile_downloads), !isChecked).apply();
				}
			});
			positiveText(actionType == ActionType.STREAM_VIDEO ?  R.string.play_video : R.string.download_video);
			show();
			dialogDisplayed = true;
		}

		return dialogDisplayed;
	}


	@Override
	public MobileNetworkWarningDialog onPositive(@NonNull MaterialDialog.SingleButtonCallback callback) {
		this.onPositiveCallback = callback;
		return this;
	}


	@Override
	public MobileNetworkWarningDialog onNegative(@NonNull MaterialDialog.SingleButtonCallback callback) {
		this.onNegativeCallback = callback;
		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * The action the user wants to perform.
	 */
	public enum ActionType {
		/** User wants to stream/play video. */
		STREAM_VIDEO,
		/** User wants to download the video and save it on the device. */
		DOWNLOAD_VIDEO
	}

}
