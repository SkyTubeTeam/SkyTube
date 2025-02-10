/*
 * SkyTube
 * Copyright (C) 2025  Zsombor Gegesy
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

package free.rm.skytube.businessobjects;

import com.google.gson.Gson;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import free.rm.skytube.businessobjects.Sponsorblock.SBVideoInfo;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

public class JsonSerializer {
    private final Gson gson = new Gson();

    public YouTubeVideo fromPersistedVideoJson(String videoJson) {
        if (videoJson == null) {
            return null;
        }
        YouTubeVideo video = gson.fromJson(videoJson, YouTubeVideo.class);
        video.updatePublishTimestampFromDate();

        // due to upgrade to YouTubeVideo (by changing channel{Id,Name} to YouTubeChannel)
        // from version 2.82 to 2.90
        if (video.getChannel() == null) {
            try {
                JsonObject videoJsonObj = JsonParser.object().from(videoJson);
                final String channelId = videoJsonObj.getString("channelId");
                final String channelName = videoJsonObj.getString("channelName");
                if (channelId != null && channelName != null) {
                    video.setChannel(new YouTubeChannel(channelId, channelName));
                }
            } catch (JsonParserException e) {
                Logger.e(this, "Error occurred while extracting channel{Id,Name} from JSON", e);
            }
        }

        return video;
    }

    public YouTubeVideo fromPersistedVideoJson(byte[] jsonVideo) {
        if (jsonVideo != null) {
            return fromPersistedVideoJson(new String(jsonVideo));
        } else {
            return null;
        }
    }

    public String toPersistedVideoJson(YouTubeVideo video) {
        return gson.toJson(video);
    }

    public String toPersistedSponsorBlockJson(SBVideoInfo sbVideoInfo) {
        return gson.toJson(sbVideoInfo);
    }

    public SBVideoInfo fromSponsorBlockJson(String sponsorBlockJson) {
        return gson.fromJson(sponsorBlockJson, SBVideoInfo.class);
    }

}
