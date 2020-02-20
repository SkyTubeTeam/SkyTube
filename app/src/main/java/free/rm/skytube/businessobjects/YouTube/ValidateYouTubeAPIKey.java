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

package free.rm.skytube.businessobjects.YouTube;

import java.io.IOException;

/**
 * Validates the given YouTube API key by performing a test call to YouTube servers.
 */
public class ValidateYouTubeAPIKey extends GetFeaturedVideos {

	private String youTubeAPIKey;


	/**
	 * Constructor.
	 *
	 * @param youTubeAPIKey The YouTube API key we are going to test/validate.
	 */
	public ValidateYouTubeAPIKey(String youTubeAPIKey) {
		this.youTubeAPIKey = youTubeAPIKey;
	}


	@Override
	public void init() throws IOException {
		super.init();
		videosList.setFields("items(id)");
		videosList.setKey(youTubeAPIKey);
	}


	/**
	 * Validate/Test the key.
	 *
	 * @return True if {@link #youTubeAPIKey} is a valid API key; false otherwise.
	 */
	public boolean isKeyValid() {
		boolean isKeyValid = false;

		try {
			init();

			if (!getNextVideos().isEmpty()) {
				isKeyValid = true;
			}
		} catch (IOException e){
			setLastException(e);
		}

		return isKeyValid;
	}


	@Override
	protected Long getMaxResults() {
		return 1L;
	}

}
