/*
 * NewTube
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
import android.content.DialogInterface;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.MaterialDialog;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.app.enums.Policy;

/**
 * Dialog that warns the user to enable the wi-fi if the device is connected to mobile network
 * (such 4G).
 */
public class MobileNetworkWarningDialog extends SkyTubeMaterialDialog {

	public MobileNetworkWarningDialog(@NonNull Context context) {
		super(context);
	}

	/**
	 * Display the dialog for video streaming.
	 *
	 * @param actionType Action Type.
	 * @return Policy which needs to be followed, either BLOCK or ALLOW - or ASK, when a warning dialog is displayed.
	 */
	public Policy showAndGetStatus(ActionType actionType) {
		final Policy displayWarning  = SkyTubeApp.getSettings().getWarningMeteredPolicy();

		if (SkyTubeApp.isActiveNetworkMetered()) {
			switch (displayWarning) {
				case BLOCK:
					Toast.makeText(getContext(), R.string.metered_network_blocked_by_policy, Toast.LENGTH_LONG).show();
					if (cancelListener != null) {
						cancelListener.onCancel(null);
					}
					return Policy.BLOCK;
				case ALLOW:
					return Policy.ALLOW;
				case ASK:
					title(R.string.metered_network);
					content(R.string.warning_metered_network_play);
					checkBoxPromptRes(R.string.warning_metered_network_disable, false, null);
					positiveText(R.string.play_video);
					show();
					return Policy.ASK;
			}
		}

		return Policy.ALLOW;
	}

	@Override
	public MobileNetworkWarningDialog onPositive(@NonNull MaterialDialog.SingleButtonCallback callback) {
		this.onPositiveCallback = (dialog, action) -> {
            if (dialog.isPromptCheckBoxChecked()) {
                SkyTubeApp.getSettings().setWarningMobilePolicy(Policy.ALLOW);
            }
            callback.onClick(dialog, action);
        };
		return this;
	}

	@Override
	public MobileNetworkWarningDialog onNegativeOrCancel(@NonNull DialogInterface.OnCancelListener callback) {
		this.onNegativeCallback = (dialog, action) -> {
		    if (dialog.isPromptCheckBoxChecked()) {
                SkyTubeApp.getSettings().setWarningMobilePolicy(Policy.BLOCK);
            }
            // no need to call the 'callback', as the cancelListener will be called.
        };
		this.cancelListener = callback;
		return this;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * The action the user wants to perform.
	 */
	public enum ActionType {
		/** User wants to stream/play video. */
		STREAM_VIDEO
	}

}
