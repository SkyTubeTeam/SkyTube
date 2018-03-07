package free.rm.skytube.businessobjects.interfaces;

import android.net.Uri;

/**
 * Interface to be used when retrieving the desired stream (per the user's preferences) from a Video.
 */
public interface GetDesiredStreamListener {

	/**
	 * Called when the video's stream has been successfully retrieved.
	 *
	 * @param videoUri  The retrieved video's Uri.
	 */
	void onGetDesiredStream(Uri videoUri);

	/**
	 * Called if an error occurred while retrieving the video's Uri.
	 *
	 * @param errorMessage  Error message.
	 */
	void onGetDesiredStreamError(String errorMessage);

}
