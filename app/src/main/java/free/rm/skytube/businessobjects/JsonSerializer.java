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

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonWriter;

import free.rm.skytube.businessobjects.Sponsorblock.SBSegment;
import free.rm.skytube.businessobjects.Sponsorblock.SBVideoInfo;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

public class JsonSerializer {

    public YouTubeVideo fromPersistedVideoJson(String videoJson) {
        if (videoJson == null) {
            return null;
        }
        String trimmed = videoJson.trim();
        if (trimmed.equals("null")) {
            return null;
        }
        try {
            JsonObject obj = JsonParser.object().from(trimmed);
            return fromJsonObject(obj);
        } catch (JsonParserException e) {
            return null;
        }
    }

    private YouTubeVideo fromJsonObject(JsonObject obj) {
        YouTubeVideo video = new YouTubeVideo();

        // channel handling (current format)
        if (obj.has("channel") && !obj.isNull("channel")) {
            video.setChannel(fromJsonObjectChannel(obj.getObject("channel")));
        } else if (obj.has("channelId") || obj.has("channelName")) {
            // archaic format (version 2.82 -> 2.90)
            String channelId = obj.has("channelId") && !obj.isNull("channelId") ? obj.getString("channelId") : null;
            String channelName = obj.has("channelName") && !obj.isNull("channelName") ? obj.getString("channelName") : null;
            if (channelId != null && channelName != null) {
                video.setChannel(new YouTubeChannel(channelId, channelName));
            }
        }

        if (obj.has("likeCountNumber") && !obj.isNull("likeCountNumber")) {
            Long like = obj.getLong("likeCountNumber");
            Long dislike = null;
            if (obj.has("dislikeCountNumber") && !obj.isNull("dislikeCountNumber")) {
                dislike = obj.getLong("dislikeCountNumber");
            }
            video.setLikeDislikeCount(like, dislike);
        } else if (obj.has("dislikeCountNumber") && !obj.isNull("dislikeCountNumber")) {
            video.setLikeDislikeCount(null, obj.getLong("dislikeCountNumber"));
        }

        if (obj.has("thumbsUpPercentage") && !obj.isNull("thumbsUpPercentage")) {
            video.setThumbsUpPercentage(obj.getInt("thumbsUpPercentage"));
        }

        if (obj.has("durationInSeconds") && !obj.isNull("durationInSeconds")) {
            video.setDurationInSeconds(obj.getInt("durationInSeconds"));
        }

        if (obj.has("viewsCountInt") && !obj.isNull("viewsCountInt")) {
            video.setViewCount(obj.getLong("viewsCountInt"));
        }

        if (obj.has("thumbnailMaxResUrl") && !obj.isNull("thumbnailMaxResUrl")) {
            video.setThumbnailMaxResUrl(obj.getString("thumbnailMaxResUrl"));
        }

        if (obj.has("language") && !obj.isNull("language")) {
            video.setLanguage(obj.getString("language"));
        }

        if (obj.has("isLiveStream") && !obj.isNull("isLiveStream")) {
            video.setLiveStream(obj.getBoolean("isLiveStream"));
        }

        if (obj.has("categoryId") && !obj.isNull("categoryId")) {
            video.setCategoryId(obj.getInt("categoryId"));
        }

        // CardData fields
        if (obj.has("id") && !obj.isNull("id")) {
            video.setId(obj.getString("id"));
        }
        if (obj.has("title") && !obj.isNull("title")) {
            video.setTitle(obj.getString("title"));
        }
        if (obj.has("description") && !obj.isNull("description")) {
            video.setDescription(obj.getString("description"));
        }
        if (obj.has("publishTimestamp") && !obj.isNull("publishTimestamp")) {
            video.setPublishTimestamp(obj.getLong("publishTimestamp"));
        }
        if (obj.has("publishTimestampExact") && !obj.isNull("publishTimestampExact")) {
            video.setPublishTimestampExact(obj.getBoolean("publishTimestampExact"));
        }
        if (obj.has("thumbnailUrl") && !obj.isNull("thumbnailUrl")) {
            video.setThumbnailUrl(obj.getString("thumbnailUrl"));
        }

        video.updatePublishTimestampFromDate();
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
        if (video == null) {
            return "null";
        }
        return JsonWriter.string(toVideoJsonObject(video));
    }

    private JsonObject toVideoJsonObject(YouTubeVideo video) {
        JsonObject o = new JsonObject();
        if (video.getChannel() != null) {
            o.put("channel", toChannelJsonObject(video.getChannel()));
        }
        if (video.getLikeCountNumber() != null) {
            o.put("likeCountNumber", video.getLikeCountNumber());
        }
        if (video.getDislikeCountNumber() != null) {
            o.put("dislikeCountNumber", video.getDislikeCountNumber());
        }
        o.put("thumbsUpPercentage", video.getThumbsUpPercentage());
        if (video.getDuration() != null) {
            o.put("duration", video.getDuration());
        }
        o.put("durationInSeconds", video.getDurationInSeconds());
        if (video.getViewsCountInt() != null) {
            o.put("viewsCountInt", video.getViewsCountInt());
        }
        if (video.getThumbnailMaxResUrl() != null) {
            o.put("thumbnailMaxResUrl", video.getThumbnailMaxResUrl());
        }
        if (video.getLanguage() != null) {
            o.put("language", video.getLanguage());
        }
        o.put("isLiveStream", video.isLiveStream());
        if (video.getCategoryId() != null) {
            o.put("categoryId", video.getCategoryId());
        }
        if (video.getId() != null) {
            o.put("id", video.getId());
        }
        if (video.getTitle() != null) {
            o.put("title", video.getTitle());
        }
        if (video.getDescription() != null) {
            o.put("description", video.getDescription());
        }
        if (video.getPublishTimestamp() != null) {
            o.put("publishTimestamp", video.getPublishTimestamp());
        }
        o.put("publishTimestampExact", video.getPublishTimestampExact());
        if (video.getThumbnailUrl() != null) {
            o.put("thumbnailUrl", video.getThumbnailUrl());
        }
        return o;
    }

    private JsonObject toChannelJsonObject(YouTubeChannel channel) {
        JsonObject o = new JsonObject();
        if (channel.getBannerUrl() != null) {
            o.put("bannerUrl", channel.getBannerUrl());
        }
        if (channel.getTotalSubscribers() != null) {
            o.put("totalSubscribers", channel.getTotalSubscribers());
        }
        o.put("subscriberCount", channel.getSubscriberCount());
        o.put("isUserSubscribed", channel.isUserSubscribed());
        o.put("lastVisitTime", channel.getLastVisitTime());
        o.put("lastCheckTime", channel.getLastCheckTime());
        o.put("lastVideoTime", channel.getLastVideoTime());
        if (channel.getCategoryId() != null) {
            o.put("categoryId", channel.getCategoryId());
        }
        JsonArray videos = new JsonArray();
        for (YouTubeVideo v : channel.getYouTubeVideos()) {
            videos.add(toVideoJsonObject(v));
        }
        o.put("youTubeVideos", videos);
        JsonArray tags = new JsonArray();
        for (String t : channel.getTags()) {
            tags.add(t);
        }
        o.put("tags", tags);
        if (channel.getId() != null) {
            o.put("id", channel.getId());
        }
        if (channel.getTitle() != null) {
            o.put("title", channel.getTitle());
        }
        if (channel.getDescription() != null) {
            o.put("description", channel.getDescription());
        }
        if (channel.getPublishTimestamp() != null) {
            o.put("publishTimestamp", channel.getPublishTimestamp());
        }
        o.put("publishTimestampExact", channel.getPublishTimestampExact());
        if (channel.getThumbnailUrl() != null) {
            o.put("thumbnailUrl", channel.getThumbnailUrl());
        }
        return o;
    }

    private YouTubeChannel fromJsonObjectChannel(JsonObject obj) {
        YouTubeChannel channel = new YouTubeChannel();
        if (obj.has("bannerUrl") && !obj.isNull("bannerUrl")) {
            channel.setBannerUrl(obj.getString("bannerUrl"));
        }
        if (obj.has("totalSubscribers") && !obj.isNull("totalSubscribers")) {
            channel.setTotalSubscribers(obj.getString("totalSubscribers"));
        }
        if (obj.has("subscriberCount") && !obj.isNull("subscriberCount")) {
            channel.setSubscriberCount(obj.getLong("subscriberCount"));
        }
        if (obj.has("isUserSubscribed") && !obj.isNull("isUserSubscribed")) {
            channel.setUserSubscribed(obj.getBoolean("isUserSubscribed"));
        }
        if (obj.has("lastVisitTime") && !obj.isNull("lastVisitTime")) {
            channel.setLastVisitTime(obj.getLong("lastVisitTime"));
        }
        if (obj.has("lastCheckTime") && !obj.isNull("lastCheckTime")) {
            channel.setLastCheckTime(obj.getLong("lastCheckTime"));
        }
        if (obj.has("lastVideoTime") && !obj.isNull("lastVideoTime")) {
            channel.setLastVideoTime(obj.getLong("lastVideoTime"));
        }
        if (obj.has("categoryId") && !obj.isNull("categoryId")) {
            channel.setCategoryId(obj.getInt("categoryId"));
        }
        if (obj.has("youTubeVideos") && !obj.isNull("youTubeVideos")) {
            channel.getYouTubeVideos().clear();
            JsonArray arr = obj.getArray("youTubeVideos");
            if (arr != null) {
                for (Object e : arr) {
                    if (e instanceof JsonObject) {
                        channel.getYouTubeVideos().add(fromJsonObject((JsonObject) e));
                    }
                }
            }
        }
        if (obj.has("tags") && !obj.isNull("tags")) {
            channel.getTags().clear();
            JsonArray arr = obj.getArray("tags");
            if (arr != null) {
                for (Object e : arr) {
                    if (e != null) {
                        channel.getTags().add(String.valueOf(e));
                    }
                }
            }
        }
        if (obj.has("id") && !obj.isNull("id")) {
            channel.setId(obj.getString("id"));
        }
        if (obj.has("title") && !obj.isNull("title")) {
            channel.setTitle(obj.getString("title"));
        }
        if (obj.has("description") && !obj.isNull("description")) {
            channel.setDescription(obj.getString("description"));
        }
        if (obj.has("publishTimestamp") && !obj.isNull("publishTimestamp")) {
            channel.setPublishTimestamp(obj.getLong("publishTimestamp"));
        }
        if (obj.has("publishTimestampExact") && !obj.isNull("publishTimestampExact")) {
            channel.setPublishTimestampExact(obj.getBoolean("publishTimestampExact"));
        }
        if (obj.has("thumbnailUrl") && !obj.isNull("thumbnailUrl")) {
            channel.setThumbnailUrl(obj.getString("thumbnailUrl"));
        }
        return channel;
    }

    public String toPersistedSponsorBlockJson(SBVideoInfo sbVideoInfo) {
        if (sbVideoInfo == null) {
            return "null";
        }
        JsonObject root = new JsonObject();
        root.put("videoDuration", sbVideoInfo.getVideoDuration());
        JsonArray segments = new JsonArray();
        for (SBSegment seg : sbVideoInfo.getSegments()) {
            JsonObject so = new JsonObject();
            so.put("category", seg.getCategory());
            so.put("startPos", seg.getStartPos());
            so.put("endPos", seg.getEndPos());
            segments.add(so);
        }
        root.put("segments", segments);
        return JsonWriter.string(root);
    }

    public SBVideoInfo fromSponsorBlockJson(String sponsorBlockJson) {
        if (sponsorBlockJson == null) {
            return null;
        }
        String trimmed = sponsorBlockJson.trim();
        if (trimmed.equals("null")) {
            return null;
        }
        try {
            JsonObject obj = JsonParser.object().from(trimmed);
            double duration = 0.0;
            if (obj.has("videoDuration") && !obj.isNull("videoDuration")) {
                duration = obj.getDouble("videoDuration");
            }
            SBVideoInfo info = new SBVideoInfo(duration);
            if (obj.has("segments") && !obj.isNull("segments")) {
                JsonArray arr = obj.getArray("segments");
                if (arr != null) {
                    for (Object e : arr) {
                        if (e instanceof JsonObject) {
                            JsonObject so = (JsonObject) e;
                            String category = so.has("category") && !so.isNull("category") ? so.getString("category") : null;
                            double startPos = so.has("startPos") && !so.isNull("startPos") ? so.getDouble("startPos") : 0.0;
                            double endPos = so.has("endPos") && !so.isNull("endPos") ? so.getDouble("endPos") : 0.0;
                            info.getSegments().add(new SBSegment(category, startPos, endPos));
                        }
                    }
                }
            }
            return info;
        } catch (JsonParserException e) {
            return null;
        }
    }
}
