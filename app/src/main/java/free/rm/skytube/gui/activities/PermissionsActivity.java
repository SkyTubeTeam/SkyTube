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

package free.rm.skytube.gui.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import free.rm.skytube.R;

/**
 * A transparent activity that:
 * <ol>
 *     <li>Asks the user to grant us the WRITE_EXTERNAL_STORAGE permission.</li>
 *     <li>Once that permission is granted, it will execute the {@link #permissionsTask}.</li>
 * </ol>
 */
public class PermissionsActivity extends AppCompatActivity {

	private PermissionsTask     permissionsTask = null;

	public static final String  PERMISSIONS_TASK_OBJ  = "PermissionsActivity.PERMISSIONS_TASK_OBJ";
	private static final int    EXT_STORAGE_PERM_CODE = 1949;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_permissions);

		permissionsTask = (PermissionsTask) getIntent().getExtras().getSerializable(PERMISSIONS_TASK_OBJ);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasAccessToExtStorage(EXT_STORAGE_PERM_CODE)) {
				permissionsTask.onExternalStoragePermissionsGranted();
				finish();
		}
	}


	/**
	 * Check if the app has access to the external storage.  If not, ask the user whether he wants
	 * to give us access...
	 *
	 * @param permissionRequestCode The request code (either EXT_STORAGE_PERM_CODE_BACKUP or
	 *                              EXT_STORAGE_PERM_CODE_IMPORT) which is used by
	 *                              {onRequestPermissionsResult(int, String[], int[])} to
	 *                              determine whether we are going to backup (export) or to import.
	 *
	 * @return True if the user has given access to write to the external storage in the past;
	 * false otherwise.
	 */
	@TargetApi(Build.VERSION_CODES.M)
	private boolean hasAccessToExtStorage(int permissionRequestCode) {
		boolean hasAccessToExtStorage = true;

		// if the user has not yet granted us access to the external storage...
		if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			// We can request the permission (to the users).  If the user grants us access (or
			// otherwise), then the method #onRequestPermissionsResult() will be called.
			requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, permissionRequestCode);

			hasAccessToExtStorage = false;
		}

		return hasAccessToExtStorage;
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		// EXT_STORAGE_PERM_CODE_BACKUP is used to backup the databases
		if (requestCode == EXT_STORAGE_PERM_CODE) {
			if (grantResults.length > 0  &&  grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// permission was granted by the user
				permissionsTask.onExternalStoragePermissionsGranted();

			} else {
				// permission denied by the user
			}
			finish();
		}
	}



	////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * The task that {@link PermissionsActivity} will execute once the permissions are granted by
	 * the user.
	 */
	public interface PermissionsTask {

		/**
		 * Permissions have been granted -- execute the task.
		 */
		void onExternalStoragePermissionsGranted();

	}

}
