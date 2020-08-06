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

import androidx.core.util.Consumer;

/**
 * Can run multiple {@link AsyncTask}s in parallel.
 *
 * <p>To run in parallel, call the method {@link #executeInParallel(Object[])}.</p>
 */
public abstract class AsyncTaskParallel<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

	protected Exception lastException;
	private Consumer<Exception> errorCallback;
	private Runnable finishCallback;

	protected AsyncTaskParallel() {
		errorCallback = null;
		finishCallback = null;
	}

	protected AsyncTaskParallel(Consumer<Exception> errorCallback, Runnable finishCallback) {
		this.errorCallback = errorCallback;
		this.finishCallback = finishCallback;
	}

	public AsyncTaskParallel<Params, Progress, Result> setFinishCallback(Runnable finishCallback) {
		this.finishCallback = finishCallback;
		return this;
	}

	public AsyncTaskParallel<Params, Progress, Result> setErrorCallback(Consumer<Exception> errorCallback) {
		this.errorCallback = errorCallback;
		return this;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		lastException = null;
	}

	/**
	 * Child classes should call as the super implementation as the last statement.
	 * @param result
	 */
	@Override
	protected void onPostExecute(Result result) {
		if (lastException != null) {
			if (errorCallback != null) {
				errorCallback.accept(lastException);
			}
			showErrorToUi();
		}
		if (finishCallback != null) {
			finishCallback.run();
		}
	}

	/**
	 * Override, if a custom error handling should happen.
	 */
	protected void showErrorToUi() {
	}

	/**
	 * @see #executeInParallel(Object[])
	 */
	public void executeInParallel() {
		executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Params[]) null);
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
