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

package free.rm.skytube.gui.activities;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.Serializable;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.FileDownloader;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

import static free.rm.skytube.app.SkyTubeApp.getContext;

/**
 * An activity that allows the user to view the thumbnail of a YouTube video.
 */
public class ThumbnailViewerActivity extends AppCompatActivity {

	private YouTubeVideo youTubeVideo;

	public static final String YOUTUBE_VIDEO = "ThumbnailViewerActivity.YouTubeVideo";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_thumbnail);

		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(view -> {
			// download the thumbnail
			new ThumbnailDownloader()
					.setRemoteFileUrl(getThumbnailUrl())
					.setDirType(Environment.DIRECTORY_PICTURES)
					.setTitle(youTubeVideo.getTitle())
					.setDescription(getString(R.string.thumbnail) + " ― " + youTubeVideo.getChannelName())
					.setOutputFileName(youTubeVideo.getTitle())
					.setAllowedOverRoaming(true)
					.displayPermissionsActivity(ThumbnailViewerActivity.this);
		});

		Intent intent = getIntent();
		youTubeVideo = (YouTubeVideo) intent.getExtras().getSerializable(YOUTUBE_VIDEO);

		ImageView   thumbnailImageView = findViewById(R.id.thumbnail_image_view);
		final View  loadingVideoView = findViewById(R.id.loadingVideoView);

		thumbnailImageView.setOnClickListener(v -> finish());

		Glide.with(this)
				.load(getThumbnailUrl())
				.listener(new RequestListener<Drawable>() {
					@Override
					public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
						loadingVideoView.setVisibility(View.GONE);
						return false;
					}

					@Override
					public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
						loadingVideoView.setVisibility(View.GONE);
						return false;
					}
				})
				.into(thumbnailImageView);
	}


	private String getThumbnailUrl() {
		return youTubeVideo.getThumbnailMaxResUrl() != null  ?  youTubeVideo.getThumbnailMaxResUrl()  :  youTubeVideo.getThumbnailUrl();
	}



	////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Downloads a video thumbnail.
	 */
	private static class ThumbnailDownloader extends FileDownloader implements Serializable {

		@Override
		public void onFileDownloadStarted() {
		}

		@Override
		public void onFileDownloadCompleted(boolean success, Uri localFileUri) {
			Toast.makeText(getContext(),
					success  ?  R.string.thumbnail_downloaded  :  R.string.thumbnail_download_error,
					Toast.LENGTH_LONG)
					.show();
		}

		@Override
		public void onExternalStorageNotAvailable() {
			Toast.makeText(getContext(),
					R.string.external_storage_not_available,
					Toast.LENGTH_LONG).show();
		}

	}

}
