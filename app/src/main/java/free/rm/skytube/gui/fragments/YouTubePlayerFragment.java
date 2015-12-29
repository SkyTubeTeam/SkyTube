package free.rm.skytube.gui.fragments;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoStream.ParseStreamMetaData;
import free.rm.skytube.businessobjects.VideoStream.StreamMetaData;
import free.rm.skytube.businessobjects.VideoStream.StreamMetaDataList;
import free.rm.skytube.gui.activities.YouTubePlayerActivity;
import free.rm.skytube.gui.businessobjects.MediaControllerEx;

/**
 * A fragment that holds a standalone YouTube player.
 */
public class YouTubePlayerFragment extends FragmentEx {

	private String			videoId = null;
	private VideoView		videoView = null;
	private MediaController	mediaController = null;
	private View			voidView = null;

	private static final int UI_VISIBILITY_TIMEOUT = 7000;
	private static final String TAG = YouTubePlayerFragment.class.getSimpleName();


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_youtube_player, container, false);

		if (videoId == null) {
			videoId = getActivity().getIntent().getExtras().getString(YouTubePlayerActivity.VIDEO_ID);

			videoView = (VideoView) view.findViewById(R.id.video_view);
			// play the video once its loaded
			videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				public void onPrepared(MediaPlayer mediaPlayer) {
					showHud();
					videoView.start();
				}
			});

			// setup the media controller (will control the video playing/pausing)
			mediaController = new MediaControllerEx(getActivity(), videoView);

			voidView = view.findViewById(R.id.void_view);
			voidView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					showOrHideHud();
				}
			});

			hideHud();

			new GetStreamTask(videoId).execute();
		}

		return view;
	}



	/**
	 * @return True if the HUD is visible (provided that this Fragment is also visible).
	 */
	private boolean isHudVisible() {
		return isVisible()  &&  (mediaController.isShowing()  ||  getActionBar().isShowing());
	}



	/**
	 * Hide or display the HUD depending if the HUD is currently visible or not.
	 */
	private void showOrHideHud() {
		if (isHudVisible())
			hideHud();
		else
			showHud();
	}



	/**
	 * Show the HUD (head-up display), i.e. the Action Bar and Media Controller.
	 */
	private void showHud() {
		if (!isHudVisible()) {
			getActionBar().show();
			getActionBar().setTitle("Xaxaxaxa");
			mediaController.show(0);

			// hide UI after a certain timeout (defined in UI_VISIBILITY_TIMEOUT)
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					hideHud();
				}
			}, UI_VISIBILITY_TIMEOUT);
		}
	}



	/**
	 * Hide the HUD.
	 */
	private void hideHud() {
		if (isHudVisible()) {
			getActionBar().hide();
			mediaController.hide();
		}
	}



	/**
	 * Given a video ID, it will asynchronously get a list of streams (supplied by YouTube) and then
	 * it asks the videoView to start playing a stream.
	 */
	private class GetStreamTask extends AsyncTask<Void, Exception, StreamMetaDataList> {

		/** Video ID */
		private String videoId;


		public GetStreamTask(String videoId) {
			this.videoId = videoId;
		}


		@Override
		protected StreamMetaDataList doInBackground(Void... param) {
			ParseStreamMetaData	ex = new ParseStreamMetaData(videoId);
			StreamMetaDataList 	streamMetaDataList = null;

			try {
				streamMetaDataList = ex.getStreamMetaDataList();
			} catch (Exception e) {
				// inform the user that an exception has been caught
				publishProgress(e);
				Log.e(TAG, "An error has occurred while getting video metadata/streams for video with id=" + videoId, e);
			}

			return streamMetaDataList;
		}


		@Override
		protected void onProgressUpdate(Exception... exception) {
			Toast.makeText(YouTubePlayerFragment.this.getActivity(),
					String.format(getActivity().getString(R.string.error_get_video_streams), videoId),
					Toast.LENGTH_LONG).show();
		}


		@Override
		protected void onPostExecute(StreamMetaDataList streamMetaDataList) {
			if (streamMetaDataList == null  ||  streamMetaDataList.size() <= 0) {
				String error = String.format(getActivity().getString(R.string.error_video_streams_empty), videoId);

				Toast.makeText(YouTubePlayerFragment.this.getActivity(),
						error,
						Toast.LENGTH_LONG).show();

			} else {
				Log.i(TAG, streamMetaDataList.toString());

				// TODO get stream based on user preferences!
				StreamMetaData desiredStream = streamMetaDataList.getDesiredStream();
				Log.i(TAG, ">> PLAYING: " + desiredStream);
				videoView.setVideoURI(desiredStream.getUri());
			}
		}
	}

}
