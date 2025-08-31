/*
 * SkyTube
 * Copyright (C) 2025
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
import android.text.InputType;
import android.widget.Toast;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;

public class PinUtils {
    public static void promptForPin(Context context, Runnable onSuccess, Runnable onFailure) {
        new SkyTubeMaterialDialog(context)
            .title(R.string.pref_title_enter_security_pin)
            .content(R.string.pref_summary_enter_security_pin)
            .inputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD)
            .input("", "", false, (dialog, input) -> {
                if (input != null && SkyTubeApp.getSettings().isCorrectSecurityPin(input.toString())) {
                    onSuccess.run();
                } else {
                    Toast.makeText(context, R.string.incorrect_pin, Toast.LENGTH_LONG).show();
                    if (onFailure != null) {
                        onFailure.run();
                    }
                }
            })
            .cancelListener((materialDialog) -> {
                if (onFailure != null) {
                    onFailure.run();
                }
            })
            .show();
    }
}
