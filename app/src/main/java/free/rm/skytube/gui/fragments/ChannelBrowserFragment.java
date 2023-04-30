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

package free.rm.skytube.gui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import free.rm.skytube.R;
import free.rm.skytube.app.EventBus;
import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.DatabaseTasks;
import free.rm.skytube.databinding.FragmentChannelBrowserBinding;
import free.rm.skytube.gui.businessobjects.adapters.SubsAdapter;
import free.rm.skytube.gui.businessobjects.fragments.FragmentEx;
import free.rm.skytube.gui.businessobjects.fragments.TabFragment;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * A Fragment that displays information about a channel.
 *
 * This fragment is made up of two other fragments:
 * <ul>
 *     <li>{@link ChannelVideosFragment}</li>
 *     <li>{@link ChannelPlaylistsFragment}.</li>
 * </ul>
 */
public class ChannelBrowserFragment extends FragmentEx {

	private YouTubeChannel		channel;
	private String 				channelId;

	public static final String FRAGMENT_CHANNEL_VIDEOS = "ChannelBrowserFragment.FRAGMENT_CHANNEL_VIDEOS";
	public static final String FRAGMENT_CHANNEL_PLAYLISTS = "ChannelBrowserFragment.FRAGMENT_CHANNEL_PLAYLISTS";

	private FragmentChannelBrowserBinding binding;
	private Disposable disposable;

	public static final String CHANNEL_OBJ = "ChannelBrowserFragment.ChannelObj";
	public static final String CHANNEL_ID  = "ChannelBrowserFragment.ChannelID";

	// The fragments that will be displayed
	private ChannelVideosFragment       channelVideosFragment;
	private ChannelPlaylistsFragment    channelPlaylistsFragment;
	private ChannelAboutFragment        channelAboutFragment;

	private ChannelPagerAdapter channelPagerAdapter;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if(savedInstanceState != null) {
			channelVideosFragment = (ChannelVideosFragment)getChildFragmentManager().getFragment(savedInstanceState, FRAGMENT_CHANNEL_VIDEOS);
			channelPlaylistsFragment = (ChannelPlaylistsFragment)getChildFragmentManager().getFragment(savedInstanceState, FRAGMENT_CHANNEL_PLAYLISTS);
		}

		// inflate the layout for this fragment
		binding = FragmentChannelBrowserBinding.inflate(inflater, container, false);

		binding.tabLayout.setupWithViewPager(binding.pager);
		binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				binding.pager.setCurrentItem(tab.getPosition());
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {
			}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //When current tab reselected scroll to the top of the video list
                TabFragment tabFragment = channelPagerAdapter.getItem(tab.getPosition());
                if (tabFragment instanceof VideosGridFragment) {
                    ((VideosGridFragment)tabFragment).scrollToTop();
                }
            }
		});

		binding.pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				if (channelPagerAdapter != null) {
					channelPagerAdapter.getItem(position).onFragmentSelected();
				}
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});

		// setup the toolbar/actionbar
		setSupportActionBar(binding.toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		binding.channelSubscribeButton.setOnClickListener(v -> {
			// If we're subscribing to the channel, save the list of videos we have into the channel (to be stored in the database by SubscribeToChannelTask)
			if(channel != null && !channel.isUserSubscribed()) {
				Iterator<CardData> iterator = channelVideosFragment.getVideoGridAdapter().getIterator();
				while (iterator.hasNext()) {
					CardData cd = iterator.next();
					if (cd instanceof YouTubeVideo) {
						channel.addYouTubeVideo((YouTubeVideo) cd);
					}
				}
			}
		});

		getChannelParameters();

		return binding.getRoot();
	}

	@Override
	public void onDestroy() {
		if (disposable != null) {
			disposable.dispose();
		}
		binding.channelSubscribeButton.clearBackgroundTasks();
		binding = null;
		super.onDestroy();
	}

	private void getChannelParameters() {
		// we need to create a YouTubeChannel object:  this can be done by either:
		//   (1) the YouTubeChannel object is passed to this Fragment
		//   (2) passing the channel ID... a task is then created to create a YouTubeChannel
		//       instance using the given channel ID
		final Bundle bundle = getArguments();
		final String oldChannelId = this.channelId;

		Logger.i(ChannelBrowserFragment.this, "getChannelParameters " + bundle);
		if (bundle != null  &&  bundle.getSerializable(CHANNEL_OBJ) != null) {
			this.channel = (YouTubeChannel) bundle.getSerializable(CHANNEL_OBJ);
			channelId = channel.getId();
		} else {
			channelId = bundle.getString(CHANNEL_ID);
			if (!Objects.equals(oldChannelId, channelId)) {
				this.channel = null;
			}
		}
		if (channel == null) {
			if (disposable == null) {
				disposable = DatabaseTasks.getChannelInfo(requireContext(), channelId, false)
						.subscribe(youTubeChannel -> {
							if (youTubeChannel == null) {
								return;
							}
							// In the event this fragment is passed a channel id and not a channel object, set the
							// channel the subscribe button is for since there wasn't a channel object to set when
							// the button was created.
							channel = youTubeChannel;
							initViews();
						});
			}
		} else {
			initViews();
		}
	}

	@Override
	public synchronized void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(CHANNEL_ID, channelId);
		// if channel is not null, the ChannelPagerAdapter is initialized, with all the sub-fragments
		if (channel != null) {
			outState.putSerializable(CHANNEL_OBJ, channel);
			if (channelVideosFragment != null) {
				getChildFragmentManager().putFragment(outState, FRAGMENT_CHANNEL_VIDEOS, channelVideosFragment);
			}
			if (channelPlaylistsFragment != null) {
				getChildFragmentManager().putFragment(outState, FRAGMENT_CHANNEL_PLAYLISTS, channelPlaylistsFragment);
			}
		}
	}

	private FragmentManager getChildFragmentManagerSafely() {
		try {
			return getChildFragmentManager();
		} catch (IllegalStateException e) {
			Logger.e(this, "Fragment mapper is not available, as the Fragment is not attached :"+e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Initialise views that are related to {@link #channel}.
	 */
	private synchronized void initViews() {
		if (channel != null) {
			FragmentManager fm = getChildFragmentManagerSafely();
			if (fm == null) {
				return;
			}
			channelPagerAdapter = new ChannelPagerAdapter(fm);
			binding.pager.setOffscreenPageLimit(2);
			binding.pager.setAdapter(channelPagerAdapter);

			this.channelVideosFragment.setYouTubeChannel(channel);

			this.channelVideosFragment.onFragmentSelected();

			Glide.with(requireContext())
					.load(channel.getThumbnailUrl())
					.apply(new RequestOptions().placeholder(R.drawable.channel_thumbnail_default))
					.into(binding.channelThumbnailImageView);

			Glide.with(requireContext())
					.load(channel.getBannerUrl())
					.apply(new RequestOptions().placeholder(R.drawable.banner_default))
					.into(binding.channelBannerImageView);

			if (channel.getSubscriberCount() >= 0) {
				binding.channelSubsTextView.setText(channel.getTotalSubscribers());
			} else {
				Logger.i(this, "Channel subscriber count for {} is {}", channel.getTitle(), channel.getSubscriberCount());
				binding.channelSubsTextView.setVisibility(View.GONE);
			}

			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.setTitle(channel.getTitle());
			}

			// if the user has subscribed to this channel, then change the state of the
			// subscribe button
			binding.channelSubscribeButton.setChannel(channel);

			if (channel.isUserSubscribed()) {
				// the user is visiting the channel, so we need to update the last visit time
				channel.updateLastVisitTime();

				// since we are visiting the channel, then we need to disable the new videos notification
				EventBus.getInstance().notifyChannelNewVideosStatus(channel.getId(), false);
			}
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////
	private class ChannelPagerAdapter extends FragmentPagerAdapter {
		/** List of fragments that will be displayed as tabs. */
		private final List<TabFragment> channelBrowserFragmentList = new ArrayList<>();

		public ChannelPagerAdapter(FragmentManager fm) {
			super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

			// Initialize fragments
			if (channelVideosFragment == null) {
				channelVideosFragment = new ChannelVideosFragment();
			}

			if (channelPlaylistsFragment == null) {
				channelPlaylistsFragment = new ChannelPlaylistsFragment();
			}

			if (channelAboutFragment == null) {
				channelAboutFragment = new ChannelAboutFragment();
			}

			Bundle bundle = new Bundle();
			bundle.putSerializable(CHANNEL_OBJ, channel);

			channelVideosFragment.setArguments(bundle);
			channelPlaylistsFragment.setArguments(bundle);
			channelAboutFragment.setArguments(bundle);

			channelBrowserFragmentList.add(channelVideosFragment);
			channelBrowserFragmentList.add(channelPlaylistsFragment);
			channelBrowserFragmentList.add(channelAboutFragment);
		}

		@Override
		public TabFragment getItem(int position) {
			return channelBrowserFragmentList.get(position);
		}

		@Override
		public int getCount() {
			return channelBrowserFragmentList.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return channelBrowserFragmentList.get(position).getFragmentName();
		}
	}

	/**
	 * Return the Channel Playlists Fragment. This is needed so that the fragment can have a reference to MainActivity
	 * @return {@link free.rm.skytube.gui.fragments.ChannelPlaylistsFragment}
	 */
	public ChannelPlaylistsFragment getChannelPlaylistsFragment() {
		if(channelPlaylistsFragment == null)
			channelPlaylistsFragment = new ChannelPlaylistsFragment();
		return channelPlaylistsFragment;
	}
}
