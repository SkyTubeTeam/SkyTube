#parse("Actual Header")

package ${PACKAGE_NAME};

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

#parse("File Header.java")
public class ${NAME} extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    }
}
