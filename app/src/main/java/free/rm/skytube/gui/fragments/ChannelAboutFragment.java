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

package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import free.rm.skytube.R;
import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.gui.businessobjects.fragments.TabFragment;

/**
 * A fragment that displays the channel's description (about section).
 */
public class ChannelAboutFragment extends TabFragment {

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_channel_about, container, false);

		// set the about text
		YouTubeChannel channel = (YouTubeChannel)getArguments().getSerializable(ChannelBrowserFragment.CHANNEL_OBJ);
		TextView aboutTextView = view.findViewById(R.id.about_text_view);
		aboutTextView.setText(channel.getDescription());

		return view;
	}


	@Override
	public String getFragmentName() {
		return SkyTubeApp.getStr(R.string.about);
	}

}
