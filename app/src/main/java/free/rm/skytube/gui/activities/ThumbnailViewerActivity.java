/*
 * NewTube
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
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.databinding.ActivityVideoThumbnailBinding;

/**
 * An activity that allows the user to view the thumbnail of a YouTube video.
 */
public class ThumbnailViewerActivity extends AppCompatActivity {
	public static final String YOUTUBE_VIDEO = "ThumbnailViewerActivity.YouTubeVideo";

	private YouTubeVideo youTubeVideo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ActivityVideoThumbnailBinding binding = ActivityVideoThumbnailBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		Intent intent = getIntent();
		youTubeVideo = (YouTubeVideo) intent.getExtras().getSerializable(YOUTUBE_VIDEO);

		binding.thumbnailImageView.setOnClickListener(v -> finish());

		Glide.with(this)
				.load(getThumbnailUrl())
				.listener(new RequestListener<Drawable>() {
					@Override
					public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
						binding.loadingVideoView.setVisibility(View.GONE);
						return false;
					}

					@Override
					public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
						binding.loadingVideoView.setVisibility(View.GONE);
						return false;
					}
				})
				.into(binding.thumbnailImageView);
	}

	private String getThumbnailUrl() {
		return youTubeVideo.getThumbnailMaxResUrl() != null  ?  youTubeVideo.getThumbnailMaxResUrl()  :  youTubeVideo.getThumbnailUrl();
	}
}
