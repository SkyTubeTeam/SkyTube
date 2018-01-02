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

package free.rm.skytube.gui.businessobjects.updates;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.AsyncTaskParallel;

/**
 * A task that will check if any SkyTube updates have been published.  If there are, then the
 * user will be notified.
 */
public class UpdatesCheckerTask extends AsyncTaskParallel<Void, Void, UpdatesChecker> {

	/** If set to true, it will display the "SkyTube is up to date" message.  */
	private final boolean displayUpToDateMessage;
	private final Context context;

	/**
	 * Constructor.
	 *
	 * @param context                   Context.
	 * @param displayUpToDateMessage    If set to true, it will display the "SkyTube is up to date"
	 *                                  message; otherwise the message will not be displayed.
	 */
	public UpdatesCheckerTask(Context context, boolean displayUpToDateMessage) {
		this.context = context;
		this.displayUpToDateMessage = displayUpToDateMessage;
	}

	@Override
	protected UpdatesChecker doInBackground(Void... params) {
		UpdatesChecker updatesChecker = new UpdatesChecker();
		updatesChecker.checkForUpdates();
		return updatesChecker;
	}

	@Override
	protected void onPostExecute(final UpdatesChecker updatesChecker) {
		// if there is an update available...
		if (updatesChecker != null && updatesChecker.getLatestApkUrl() != null) {
			// ask the user whether he wants to update or not
			new AlertDialog.Builder(context)
					.setTitle(R.string.update_available)
					.setMessage( String.format(context.getString(R.string.update_dialog_msg), Float.toString(updatesChecker.getLatestApkVersion())) )
					.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							new UpgradeAppTask(updatesChecker.getLatestApkUrl(), context).executeInParallel();
						}
					})
					.setNegativeButton(R.string.later, null)
					.show();
		} else if (displayUpToDateMessage) {
			// inform the user that there is no update available (app is up-to-date)
			new AlertDialog.Builder(context)
					.setTitle(R.string.up_to_date)
					.setMessage(R.string.up_to_date_msg)
					.setNeutralButton(R.string.ok, null)
					.show();
		}
	}

}
