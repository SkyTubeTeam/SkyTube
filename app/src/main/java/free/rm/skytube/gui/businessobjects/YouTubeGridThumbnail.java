/*
 * SkyTube
 * Copyright (C) 2015  Ramon Mifsud
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

package free.rm.skytube.gui.businessobjects;

import android.content.Context;
import android.util.AttributeSet;

import free.rm.skytube.R;

/**
 * YouTube thumbnail that is going to be displayed in {@link GridAdapter}.
 */
public class YouTubeGridThumbnail extends InternetImageView {

	public YouTubeGridThumbnail(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	public YouTubeGridThumbnail(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public YouTubeGridThumbnail(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public YouTubeGridThumbnail(Context context) {
		super(context);
	}

	@Override
	protected int getDefaultImageResource() {
		return R.drawable.thumbnail_default;
	}

}
