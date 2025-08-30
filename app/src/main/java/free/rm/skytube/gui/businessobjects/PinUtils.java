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
