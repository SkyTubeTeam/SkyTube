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

package free.rm.skytube.gui.businessobjects.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.mopub.mobileads.MoPubView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;
import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.db.Tasks.IsVideoBookmarkedTask;
import free.rm.skytube.businessobjects.db.Tasks.IsVideoWatchedTask;
import free.rm.skytube.gui.activities.ThumbnailViewerActivity;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.YouTubePlayer;

/**
 * A ViewHolder for the videos grid view.
 */
class GridViewHolder extends RecyclerView.ViewHolder {
	/** YouTube video */
	private YouTubeVideo            youTubeVideo = null;
	private Context                 context = null;
	private MainActivityListener    mainActivityListener;
	private boolean                 showChannelInfo;

	private TextView titleTextView;
	private TextView channelTextView;
	private TextView thumbsUpPercentageTextView;
	private TextView videoDurationTextView;
	private TextView publishDateTextView;
	private ImageView thumbnailImageView;
	private TextView viewsTextView;
	private ProgressBar videoPositionProgressBar;
	public static final String YOUTUBE_URL = "https://www.youtube.com/watch?v=";


	/**
	 * Constructor.
	 *
	 * @param view              Cell view (parent).
	 * @param listener          MainActivity listener.
	 * @param showChannelInfo   True to display channel information (e.g. channel name) and allows
	 *                          user to open and browse the channel; false to hide such information.
	 */
	GridViewHolder(View view, MainActivityListener listener, boolean showChannelInfo) {
		super(view);

		titleTextView = view.findViewById(R.id.title_text_view);
		channelTextView = view.findViewById(R.id.channel_text_view);
		thumbsUpPercentageTextView = view.findViewById(R.id.thumbs_up_text_view);
		videoDurationTextView = view.findViewById(R.id.video_duration_text_view);
		publishDateTextView = view.findViewById(R.id.publish_date_text_view);
		thumbnailImageView = view.findViewById(R.id.thumbnail_image_view);
		viewsTextView = view.findViewById(R.id.views_text_view);
		videoPositionProgressBar = view.findViewById(R.id.video_position_progress_bar);

		this.mainActivityListener = listener;
		this.showChannelInfo = showChannelInfo;

		thumbnailImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View thumbnailView) {
				if (youTubeVideo != null) {
					if(gridViewHolderListener != null)
						gridViewHolderListener.onClick();
					YouTubePlayer.launch(youTubeVideo, context);
				}
			}
		});

		View.OnClickListener channelOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mainActivityListener != null)
					mainActivityListener.onChannelClick(youTubeVideo.getChannelId());
			}
		};

		view.findViewById(R.id.channel_layout).setOnClickListener(showChannelInfo ? channelOnClickListener : null);

		view.findViewById(R.id.options_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onOptionsButtonClick(v);
			}
		});
	}



	/**
	 * Updates the contents of this ViewHold such that the data of these views is equal to the
	 * given youTubeVideo.
	 *
	 * @param youTubeVideo		{@link YouTubeVideo} instance.
	 */
	void updateInfo(YouTubeVideo youTubeVideo, Context context, MainActivityListener listener) {
		this.youTubeVideo = youTubeVideo;
		this.context = context;
		this.mainActivityListener = listener;
		updateViewsData();
	}


	public void updateViewsData() {
		updateViewsData(this.context);
	}

	/**
	 * This method will update the {@link View}s of this object reflecting this GridView's video.
	 *
	 * @param context			{@link Context} current context.
	 */
	public void updateViewsData(Context context) {
		this.context = context;
		titleTextView.setText(youTubeVideo.getTitle());
		channelTextView.setText(showChannelInfo ? youTubeVideo.getChannelName() : "");
		publishDateTextView.setText(youTubeVideo.getPublishDatePretty());
		videoDurationTextView.setText(youTubeVideo.getDuration());
		viewsTextView.setText(youTubeVideo.getViewsCount());
		Glide.with(context)
						.load(youTubeVideo.getThumbnailUrl())
						.apply(new RequestOptions().placeholder(R.drawable.thumbnail_default))
						.into(thumbnailImageView);

		if (youTubeVideo.getThumbsUpPercentageStr() != null) {
			thumbsUpPercentageTextView.setVisibility(View.VISIBLE);
			thumbsUpPercentageTextView.setText(youTubeVideo.getThumbsUpPercentageStr());
		} else {
			thumbsUpPercentageTextView.setVisibility(View.INVISIBLE);
		}

		if(SkyTubeApp.getPreferenceManager().getBoolean(context.getString(R.string.pref_key_disable_playback_status), false)) {
			videoPositionProgressBar.setVisibility(View.INVISIBLE);
		} else {
			PlaybackStatusDb.VideoWatchedStatus videoWatchedStatus = PlaybackStatusDb.getVideoDownloadsDb().getVideoWatchedStatus(youTubeVideo);
			if (videoWatchedStatus.position > 0) {
				videoPositionProgressBar.setVisibility(View.VISIBLE);
				videoPositionProgressBar.setMax(youTubeVideo.getDurationInSeconds() * 1000);
				videoPositionProgressBar.setProgress((int) videoWatchedStatus.position);
			} else {
				videoPositionProgressBar.setVisibility(View.INVISIBLE);
			}

			if (videoWatchedStatus.watched) {
				videoPositionProgressBar.setVisibility(View.VISIBLE);
				videoPositionProgressBar.setMax(youTubeVideo.getDurationInSeconds() * 1000);
				videoPositionProgressBar.setProgress(youTubeVideo.getDurationInSeconds() * 1000);
			}
		}
	}



 	private void onOptionsButtonClick(final View view) {
		final PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
		popupMenu.getMenuInflater().inflate(R.menu.video_options_menu, popupMenu.getMenu());
		Menu menu = popupMenu.getMenu();
		new IsVideoBookmarkedTask(youTubeVideo, menu).executeInParallel();

		// If playback history is not disabled, see if this video has been watched. Otherwise, hide the "mark watched" & "mark unwatched" options from the menu.
		if(!SkyTubeApp.getPreferenceManager().getBoolean(context.getString(R.string.pref_key_disable_playback_status), false)) {
			new IsVideoWatchedTask(youTubeVideo, menu).executeInParallel();
		} else {
			popupMenu.getMenu().findItem(R.id.mark_watched).setVisible(false);
			popupMenu.getMenu().findItem(R.id.mark_unwatched).setVisible(false);
		}

		if(youTubeVideo.isDownloaded()) {
			popupMenu.getMenu().findItem(R.id.delete_download).setVisible(true);
			popupMenu.getMenu().findItem(R.id.download_video).setVisible(false);
		} else {
			popupMenu.getMenu().findItem(R.id.delete_download).setVisible(false);
			boolean allowDownloadsOnMobile = SkyTubeApp.getPreferenceManager().getBoolean(SkyTubeApp.getStr(R.string.pref_key_allow_mobile_downloads), false);
			if(SkyTubeApp.isConnectedToWiFi() || (SkyTubeApp.isConnectedToMobile() && allowDownloadsOnMobile))
				popupMenu.getMenu().findItem(R.id.download_video).setVisible(true);
			else
				popupMenu.getMenu().findItem(R.id.download_video).setVisible(false);
		}
		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch(item.getItemId()) {
					case R.id.menu_open_video_with:
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(youTubeVideo.getVideoUrl()));
						context.startActivity(browserIntent);
						return true;
					case R.id.share:
						youTubeVideo.shareVideo(view.getContext());
						return true;
					case R.id.copyurl:
						youTubeVideo.copyUrl(context);
						return true;
					case R.id.mark_watched:
						PlaybackStatusDb.getVideoDownloadsDb().setVideoWatchedStatus(youTubeVideo, true);
						updateViewsData();
						return true;
					case R.id.mark_unwatched:
						PlaybackStatusDb.getVideoDownloadsDb().setVideoWatchedStatus(youTubeVideo, false);
						updateViewsData();
						return true;
					case R.id.bookmark_video:
						youTubeVideo.bookmarkVideo(context, popupMenu.getMenu());
						return true;
					case R.id.unbookmark_video:
						youTubeVideo.unbookmarkVideo(context, popupMenu.getMenu());
						return true;
					case R.id.view_thumbnail:
						Intent i = new Intent(context, ThumbnailViewerActivity.class);
						i.putExtra(ThumbnailViewerActivity.YOUTUBE_VIDEO, youTubeVideo);
						context.startActivity(i);
						return true;
					case R.id.delete_download:
						youTubeVideo.removeDownload();
						return true;
					case R.id.download_video:
						getYoutubeDownloadVideoList(YOUTUBE_URL + youTubeVideo.getId());
						//showDialog();
						return true;
					case R.id.block_channel:
						youTubeVideo.getChannel().blockChannel();
				}
				return false;
			}
		});
		popupMenu.show();
	}

	/**
	 * Interface to alert a listener that this GridViewHolder has been clicked.
	 */
	public interface GridViewHolderListener {
		void onClick();
	}

	private GridViewHolderListener gridViewHolderListener;

	public void setGridViewHolderListener(GridViewHolderListener gridViewHolderListener) {
		this.gridViewHolderListener = gridViewHolderListener;
	}

	private MoPubView mMoPubView;

	private void showDialog() {
		MaterialDialog md = new MaterialDialog.Builder(context)
				.title(R.string.download_video)
				.customView(R.layout.mrect_ad, true)
				.build();
		mMoPubView = (MoPubView) md.findViewById(R.id.banner_mopubview);
		LinearLayout.LayoutParams layoutParams =
				(LinearLayout.LayoutParams) mMoPubView.getLayoutParams();
		layoutParams.width = getWidth();
		layoutParams.height = getHeight();
		mMoPubView.setLayoutParams(layoutParams);
		mMoPubView.setAdUnitId("252412d5e9364a05ab77d9396346d73d");
		mMoPubView.loadAd();
		md.show();
	}

	public int getWidth() {
		return (int) context.getResources().getDimension(R.dimen.mrect_width);
	}

	public int getHeight() {
		return (int) context.getResources().getDimension(R.dimen.mrect_height);
	}

	private void showListDialog(final Map<String,YtFile> map) {
		new MaterialDialog.Builder(context)
				.title(R.string.download_video)
				.items(map.keySet())
				.itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
					@Override
					public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
						/**
						 * If you use alwaysCallSingleChoiceCallback(), which is discussed below,
						 * returning false here won't allow the newly selected radio button to actually be selected.
						 **/
						new DownloadFile().execute(map.get(dialog.getItems().get(which)).getUrl(),"");
						return true;
					}
				})
				.positiveText(R.string.ok).choiceWidgetColor(ColorStateList.valueOf(context.getResources().getColor(R.color.dialog_title)))
				.show();
	}
	private void getYoutubeDownloadVideoList(String youtubeLink) {
		new YouTubeExtractor(context) {

			@Override
			public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
				Map<String,YtFile> map = new HashMap<>();
				if (ytFiles == null) {
					// Something went wrong we got no urls. Always check this.
					//finish();
					return;
				}
				// Iterate over itags
				for (int i = 0, itag; i < ytFiles.size(); i++) {
					itag = ytFiles.keyAt(i);
					// ytFile represents one file with its url and meta data
					YtFile ytFile = ytFiles.get(itag);

					// Just add videos in a decent format => height -1 = audio
					if (ytFile.getFormat().getHeight() == -1 || ytFile.getFormat().getHeight() >= 360) {
						String str = (ytFile.getFormat().getHeight() == -1) ? "Audio " +
								ytFile.getFormat().getAudioBitrate() + " kbit/s" :
								ytFile.getFormat().getHeight() + "p";
						str += (ytFile.getFormat().isDashContainer()) ? " dash" : "";
						map.put(str,ytFile);
					}
				}
				showListDialog(map);
			}
		}.extract(youtubeLink, true, false);
	}

	// DownloadFile AsyncTask
	private class DownloadFile extends AsyncTask<String, Integer, String> {

		MaterialDialog md;
		private MoPubView mMoPubView;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			md = new MaterialDialog.Builder(context)
					.title(R.string.download_video)
					.customView(R.layout.mrect_ad, true)
					.build();
			mMoPubView = (MoPubView) md.findViewById(R.id.banner_mopubview);
			LinearLayout.LayoutParams layoutParams =
					(LinearLayout.LayoutParams) mMoPubView.getLayoutParams();
			layoutParams.width = getWidth();
			layoutParams.height = getHeight();
			mMoPubView.setLayoutParams(layoutParams);
			mMoPubView.setAdUnitId("252412d5e9364a05ab77d9396346d73d");
			mMoPubView.loadAd();
			md.show();
		}

		@Override
		protected String doInBackground(String... Url) {
			try {
				URL url = new URL(Url[0]);
				URLConnection connection = url.openConnection();
				connection.connect();

				// Detect the file lenghth
				int fileLength = connection.getContentLength();

				// Locate storage location
				String filepath = Environment.getExternalStorageDirectory()
						.getPath();

				// Download the file
				InputStream input = new BufferedInputStream(url.openStream());

				File f = new File(filepath, Url[1]);

				// Save the downloaded file
				OutputStream output = new FileOutputStream(f);

				byte data[] = new byte[1024];
				long total = 0;
				int count;
				while ((count = input.read(data)) != -1) {
					total += count;
					// Publish the progress
					publishProgress((int) (total * 100 / fileLength));
					output.write(data, 0, count);
				}

				// Close connection
				output.flush();
				output.close();
				input.close();
			} catch (Exception e) {
				// Error Log
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			super.onProgressUpdate(progress);

			//bnp.setProgress(progress[0]);

		}

		@Override
		protected void onPostExecute(String file_url) {


			//dialog.dismiss();


		}
	}

}
