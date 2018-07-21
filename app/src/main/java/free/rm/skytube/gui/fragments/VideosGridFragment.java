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

package free.rm.skytube.gui.fragments;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.bumptech.glide.Glide;
import com.mopub.nativeads.MoPubNativeAdPositioning;
import com.mopub.nativeads.MoPubRecyclerAdapter;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.RequestParameters;
import com.mopub.nativeads.ViewBinder;

import java.util.EnumSet;

import free.rm.skytube.R;
import free.rm.skytube.businessobjects.VideoCategory;
import free.rm.skytube.gui.businessobjects.MainActivityListener;
import free.rm.skytube.gui.businessobjects.adapters.VideoGridAdapter;
import free.rm.skytube.gui.businessobjects.fragments.BaseVideosGridFragment;

/**
 * A fragment that will hold a {@link GridView} full of YouTube videos.
 */
public abstract class VideosGridFragment extends BaseVideosGridFragment {

	protected RecyclerView	gridView;
	private MoPubRecyclerAdapter mRecyclerAdapter;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// inflate the layout for this fragment
		View view = super.onCreateView(inflater, container, savedInstanceState);

		// setup the video grid view
		gridView = view.findViewById(R.id.grid_view);
		if (videoGridAdapter == null) {
			videoGridAdapter = new VideoGridAdapter(getActivity());
		} else {
			videoGridAdapter.setContext(getActivity());
		}
		videoGridAdapter.setSwipeRefreshLayout(swipeRefreshLayout);

		if (getVideoCategory() != null)
			videoGridAdapter.setVideoCategory(getVideoCategory(), getSearchString());

		videoGridAdapter.setListener((MainActivityListener)getActivity());

		gridView.setHasFixedSize(true);
		gridView.setLayoutManager(new GridLayoutManager(getActivity(), getResources().getInteger(R.integer.video_grid_num_columns)));

		/*mRecyclerAdapter = new MoPubRecyclerAdapter(getActivity(), videoGridAdapter,
				MoPubNativeAdPositioning.clientPositioning()
						.addFixedPosition(0)
						.addFixedPosition(4)
						.enableRepeatingPositions(5));*/
		mRecyclerAdapter = new MoPubRecyclerAdapter(getActivity(), videoGridAdapter,
				new MoPubNativeAdPositioning.MoPubServerPositioning());

		final Location location = null;
		final String keywords = "";
		final String userDataKeywords = "";

		// Setting desired assets on your request helps native ad networks and bidders
		// provide higher-quality ads.
		final EnumSet<RequestParameters.NativeAdAsset> desiredAssets = EnumSet.of(
				RequestParameters.NativeAdAsset.TITLE,
				RequestParameters.NativeAdAsset.TEXT,
				RequestParameters.NativeAdAsset.ICON_IMAGE,
				RequestParameters.NativeAdAsset.MAIN_IMAGE,
				RequestParameters.NativeAdAsset.CALL_TO_ACTION_TEXT);

		RequestParameters mRequestParameters = new RequestParameters.Builder()
				.location(location)
				.keywords(keywords)
				.userDataKeywords(userDataKeywords)
				.desiredAssets(desiredAssets)
				.build();


		MoPubStaticNativeAdRenderer moPubStaticNativeAdRenderer = new MoPubStaticNativeAdRenderer(
				new ViewBinder.Builder(R.layout.native_ad_layout)
						.titleId(R.id.native_title)
						.textId(R.id.native_text)
						.mainImageId(R.id.native_main_image)
						.iconImageId(R.id.native_icon_image)
						.callToActionId(R.id.native_cta)
						.privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
						.build()
		);

		// Set up a renderer for a video native ad.
		/*MoPubVideoNativeAdRenderer moPubVideoNativeAdRenderer = new MoPubVideoNativeAdRenderer(
				new MediaViewBinder.Builder(R.layout.video_ad_list_item)
						.titleId(R.id.native_title)
						.textId(R.id.native_text)
						.mediaLayoutId(R.id.native_media_layout)
						.iconImageId(R.id.native_icon_image)
						.callToActionId(R.id.native_cta)
						.privacyInformationIconImageId(R.id.native_privacy_information_icon_image)
						.build());
*/

		mRecyclerAdapter.registerAdRenderer(moPubStaticNativeAdRenderer);
		//mRecyclerAdapter.registerAdRenderer(moPubVideoNativeAdRenderer);



		gridView.setAdapter(mRecyclerAdapter);
		if (mRecyclerAdapter != null) {
			mRecyclerAdapter.loadAds("11a17b188668469fb0412708c3d16813", mRequestParameters);
		}

		//mRecyclerAdapter.loadAds("1de632f25a504ab4ae481009a650627e");
		//gridView.setAdapter(videoGridAdapter);

		return view;
	}


	@Override
	public void onDestroy() {
		mRecyclerAdapter.destroy();
		super.onDestroy();
		Glide.get(getActivity()).clearMemory();
	}


	@Override
	protected int getLayoutResource() {
		return R.layout.videos_gridview;
	}


	/**
	 * @return Returns the category of videos being displayed by this fragment.
	 */
	protected abstract VideoCategory getVideoCategory();


	/**
	 * @return Returns the search string used when setting the video category.  (Can be used to
	 * set the channel ID in case of VideoCategory.CHANNEL_VIDEOS).
	 */
	protected String getSearchString() {
		return null;
	}

	/**
	 * @return The fragment/tab name/title.
	 */
	public abstract String getFragmentName();

}
