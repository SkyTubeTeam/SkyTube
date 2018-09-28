/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
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

package free.rm.skytube.businessobjects;

import android.os.AsyncTask;

/**
 * Can run multiple {@link AsyncTask}s in parallel.
 *
 * <p>To run in parallel, call the method {@link #executeInParallel(Object[])}.</p>
 */
public abstract class AsyncTaskParallel<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

	/**
	 * @see #executeInParallel(Object[])
	 */
	public void executeInParallel() {
		executeInParallel((Params[])null);
	}


	/**
	 * Execute this task.  If other tasks are running, try to run this task in parallel.
	 *
	 * @param params	Parameters.
	 */
	public void executeInParallel(Params... params) {
		this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
	}

}
