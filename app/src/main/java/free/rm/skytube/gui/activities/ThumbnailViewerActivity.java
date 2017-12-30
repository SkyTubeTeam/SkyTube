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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.YouTubeVideo;

/**
 * An activity that allows the user to view the thumbnail of a YouTube video.
 */
public class ThumbnailViewerActivity extends AppCompatActivity {

	public static final String YOUTUBE_VIDEO = "ThumbnailViewerActivity.YouTubeVideo";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_thumbnail);

		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "TODO", Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();
			}
		});

		Intent          intent = getIntent();
		YouTubeVideo    youTubeVideo = (YouTubeVideo) intent.getExtras().getSerializable(YOUTUBE_VIDEO);
		ImageView       thumbnailImageView = findViewById(R.id.thumbnail_image_view);
		final View      loadingVideoView = findViewById(R.id.loadingVideoView);

		Glide.with(this)
				.load(youTubeVideo.getThumbnailMaxResUrl() != null  ?  youTubeVideo.getThumbnailMaxResUrl()  :  youTubeVideo.getThumbnailUrl())
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

}
