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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import free.rm.skytube.businessobjects.Sponsorblock.SBSegment;
import free.rm.skytube.businessobjects.Sponsorblock.SBVideoInfo;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;

public class JsonSerializer {
    private final Gson gson = new Gson();

    public YouTubeVideo fromPersistedVideoJson(String videoJson) {
        if (videoJson == null) {
            return null;
        }
        JsonElement element = JsonParser.parseString(videoJson);
        if (element.isJsonNull()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();

        YouTubeVideo video = new YouTubeVideo();

        // channel handling (current format)
        if (obj.has("channel") && !obj.get("channel").isJsonNull()) {
            video.setChannel(fromJsonObjectChannel(obj.getAsJsonObject("channel")));
        } else if (obj.has("channelId") || obj.has("channelName")) {
            // archaic format (version 2.82 -> 2.90)
            String channelId = getStringOrNull(obj, "channelId");
            String channelName = getStringOrNull(obj, "channelName");
            if (channelId != null && channelName != null) {
                video.setChannel(new YouTubeChannel(channelId, channelName));
            }
        }

        Long likeCount = getLongOrNull(obj, "likeCountNumber");
        Long dislikeCount = getLongOrNull(obj, "dislikeCountNumber");
        if (likeCount != null || dislikeCount != null) {
            video.setLikeDislikeCount(likeCount, dislikeCount);
        }

        if (obj.has("thumbsUpPercentage") && !obj.get("thumbsUpPercentage").isJsonNull()) {
            video.setThumbsUpPercentage(obj.get("thumbsUpPercentage").getAsInt());
        }

        if (obj.has("durationInSeconds") && !obj.get("durationInSeconds").isJsonNull()) {
            video.setDurationInSeconds(obj.get("durationInSeconds").getAsInt());
        }

        if (obj.has("viewsCountInt") && !obj.get("viewsCountInt").isJsonNull()) {
            video.setViewCount(getLongOrNull(obj, "viewsCountInt"));
        }

        if (obj.has("thumbnailMaxResUrl") && !obj.get("thumbnailMaxResUrl").isJsonNull()) {
            video.setThumbnailMaxResUrl(obj.get("thumbnailMaxResUrl").getAsString());
        }

        if (obj.has("language") && !obj.get("language").isJsonNull()) {
            video.setLanguage(obj.get("language").getAsString());
        }

        if (obj.has("isLiveStream") && !obj.get("isLiveStream").isJsonNull()) {
            video.setLiveStream(obj.get("isLiveStream").getAsBoolean());
        }

        if (obj.has("categoryId") && !obj.get("categoryId").isJsonNull()) {
            video.setCategoryId(obj.get("categoryId").getAsInt());
        }

        // CardData fields
        if (obj.has("id") && !obj.get("id").isJsonNull()) {
            video.setId(obj.get("id").getAsString());
        }
        if (obj.has("title") && !obj.get("title").isJsonNull()) {
            video.setTitle(obj.get("title").getAsString());
        }
        if (obj.has("description") && !obj.get("description").isJsonNull()) {
            video.setDescription(obj.get("description").getAsString());
        }
        if (obj.has("publishTimestamp") && !obj.get("publishTimestamp").isJsonNull()) {
            video.setPublishTimestamp(getLongOrNull(obj, "publishTimestamp"));
        }
        if (obj.has("publishTimestampExact") && !obj.get("publishTimestampExact").isJsonNull()) {
            video.setPublishTimestampExact(obj.get("publishTimestampExact").getAsBoolean());
        }
        if (obj.has("thumbnailUrl") && !obj.get("thumbnailUrl").isJsonNull()) {
            video.setThumbnailUrl(obj.get("thumbnailUrl").getAsString());
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
        return gson.toJson(toVideoJsonObject(video));
    }

    private JsonObject toVideoJsonObject(YouTubeVideo video) {
        JsonObject o = new JsonObject();
        if (video.getChannel() != null) {
            o.add("channel", toChannelJsonObject(video.getChannel()));
        }
        if (video.getLikeCountNumber() != null) {
            o.addProperty("likeCountNumber", video.getLikeCountNumber());
        }
        if (video.getDislikeCountNumber() != null) {
            o.addProperty("dislikeCountNumber", video.getDislikeCountNumber());
        }
        o.addProperty("thumbsUpPercentage", video.getThumbsUpPercentage());
        if (video.getDuration() != null) {
            o.addProperty("duration", video.getDuration());
        }
        o.addProperty("durationInSeconds", video.getDurationInSeconds());
        if (video.getViewsCountInt() != null) {
            o.addProperty("viewsCountInt", video.getViewsCountInt());
        }
        if (video.getThumbnailMaxResUrl() != null) {
            o.addProperty("thumbnailMaxResUrl", video.getThumbnailMaxResUrl());
        }
        if (video.getLanguage() != null) {
            o.addProperty("language", video.getLanguage());
        }
        o.addProperty("isLiveStream", video.isLiveStream());
        if (video.getCategoryId() != null) {
            o.addProperty("categoryId", video.getCategoryId());
        }
        if (video.getId() != null) {
            o.addProperty("id", video.getId());
        }
        if (video.getTitle() != null) {
            o.addProperty("title", video.getTitle());
        }
        if (video.getDescription() != null) {
            o.addProperty("description", video.getDescription());
        }
        if (video.getPublishTimestamp() != null) {
            o.addProperty("publishTimestamp", video.getPublishTimestamp());
        }
        o.addProperty("publishTimestampExact", video.getPublishTimestampExact());
        if (video.getThumbnailUrl() != null) {
            o.addProperty("thumbnailUrl", video.getThumbnailUrl());
        }
        return o;
    }

    private JsonObject toChannelJsonObject(YouTubeChannel channel) {
        JsonObject o = new JsonObject();
        if (channel.getBannerUrl() != null) {
            o.addProperty("bannerUrl", channel.getBannerUrl());
        }
        if (channel.getTotalSubscribers() != null) {
            o.addProperty("totalSubscribers", channel.getTotalSubscribers());
        }
        o.addProperty("subscriberCount", channel.getSubscriberCount());
        o.addProperty("isUserSubscribed", channel.isUserSubscribed());
        o.addProperty("lastVisitTime", channel.getLastVisitTime());
        o.addProperty("lastCheckTime", channel.getLastCheckTime());
        o.addProperty("lastVideoTime", channel.getLastVideoTime());
        if (channel.getCategoryId() != null) {
            o.addProperty("categoryId", channel.getCategoryId());
        }
        JsonArray videos = new JsonArray();
        for (YouTubeVideo v : channel.getYouTubeVideos()) {
            videos.add(toVideoJsonObject(v));
        }
        o.add("youTubeVideos", videos);
        JsonArray tags = new JsonArray();
        for (String t : channel.getTags()) {
            tags.add(t);
        }
        o.add("tags", tags);
        if (channel.getId() != null) {
            o.addProperty("id", channel.getId());
        }
        if (channel.getTitle() != null) {
            o.addProperty("title", channel.getTitle());
        }
        if (channel.getDescription() != null) {
            o.addProperty("description", channel.getDescription());
        }
        if (channel.getPublishTimestamp() != null) {
            o.addProperty("publishTimestamp", channel.getPublishTimestamp());
        }
        o.addProperty("publishTimestampExact", channel.getPublishTimestampExact());
        if (channel.getThumbnailUrl() != null) {
            o.addProperty("thumbnailUrl", channel.getThumbnailUrl());
        }
        return o;
    }

    private YouTubeChannel fromJsonObjectChannel(JsonObject obj) {
        YouTubeChannel channel = new YouTubeChannel();
        if (obj.has("bannerUrl") && !obj.get("bannerUrl").isJsonNull()) {
            channel.setBannerUrl(obj.get("bannerUrl").getAsString());
        }
        if (obj.has("totalSubscribers") && !obj.get("totalSubscribers").isJsonNull()) {
            channel.setTotalSubscribers(obj.get("totalSubscribers").getAsString());
        }
        if (obj.has("subscriberCount") && !obj.get("subscriberCount").isJsonNull()) {
            channel.setSubscriberCount(obj.get("subscriberCount").getAsLong());
        }
        if (obj.has("isUserSubscribed") && !obj.get("isUserSubscribed").isJsonNull()) {
            channel.setUserSubscribed(obj.get("isUserSubscribed").getAsBoolean());
        }
        if (obj.has("lastVisitTime") && !obj.get("lastVisitTime").isJsonNull()) {
            channel.setLastVisitTime(obj.get("lastVisitTime").getAsLong());
        }
        if (obj.has("lastCheckTime") && !obj.get("lastCheckTime").isJsonNull()) {
            channel.setLastCheckTime(obj.get("lastCheckTime").getAsLong());
        }
        if (obj.has("lastVideoTime") && !obj.get("lastVideoTime").isJsonNull()) {
            channel.setLastVideoTime(obj.get("lastVideoTime").getAsLong());
        }
        if (obj.has("categoryId") && !obj.get("categoryId").isJsonNull()) {
            channel.setCategoryId(obj.get("categoryId").getAsInt());
        }
        if (obj.has("youTubeVideos") && obj.get("youTubeVideos").isJsonArray()) {
            channel.getYouTubeVideos().clear();
            for (JsonElement e : obj.getAsJsonArray("youTubeVideos")) {
                if (e.isJsonObject()) {
                    channel.getYouTubeVideos().add(fromPersistedVideoJson(e.toString()));
                }
            }
        }
        if (obj.has("tags") && obj.get("tags").isJsonArray()) {
            channel.getTags().clear();
            for (JsonElement e : obj.getAsJsonArray("tags")) {
                if (!e.isJsonNull()) {
                    channel.getTags().add(e.getAsString());
                }
            }
        }
        if (obj.has("id") && !obj.get("id").isJsonNull()) {
            channel.setId(obj.get("id").getAsString());
        }
        if (obj.has("title") && !obj.get("title").isJsonNull()) {
            channel.setTitle(obj.get("title").getAsString());
        }
        if (obj.has("description") && !obj.get("description").isJsonNull()) {
            channel.setDescription(obj.get("description").getAsString());
        }
        if (obj.has("publishTimestamp") && !obj.get("publishTimestamp").isJsonNull()) {
            channel.setPublishTimestamp(getLongOrNull(obj, "publishTimestamp"));
        }
        if (obj.has("publishTimestampExact") && !obj.get("publishTimestampExact").isJsonNull()) {
            channel.setPublishTimestampExact(obj.get("publishTimestampExact").getAsBoolean());
        }
        if (obj.has("thumbnailUrl") && !obj.get("thumbnailUrl").isJsonNull()) {
            channel.setThumbnailUrl(obj.get("thumbnailUrl").getAsString());
        }
        return channel;
    }

    public String toPersistedSponsorBlockJson(SBVideoInfo sbVideoInfo) {
        if (sbVideoInfo == null) {
            return "null";
        }
        JsonObject root = new JsonObject();
        root.addProperty("videoDuration", sbVideoInfo.getVideoDuration());
        JsonArray segments = new JsonArray();
        for (SBSegment seg : sbVideoInfo.getSegments()) {
            JsonObject so = new JsonObject();
            so.addProperty("category", seg.getCategory());
            so.addProperty("startPos", seg.getStartPos());
            so.addProperty("endPos", seg.getEndPos());
            segments.add(so);
        }
        root.add("segments", segments);
        return gson.toJson(root);
    }

    public SBVideoInfo fromSponsorBlockJson(String sponsorBlockJson) {
        if (sponsorBlockJson == null) {
            return null;
        }
        JsonElement element = JsonParser.parseString(sponsorBlockJson);
        if (element.isJsonNull()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();
        double duration = 0.0;
        if (obj.has("videoDuration") && !obj.get("videoDuration").isJsonNull()) {
            duration = obj.get("videoDuration").getAsDouble();
        }
        SBVideoInfo info = new SBVideoInfo(duration);
        if (obj.has("segments") && obj.get("segments").isJsonArray()) {
            for (JsonElement e : obj.getAsJsonArray("segments")) {
                if (e.isJsonObject()) {
                    JsonObject so = e.getAsJsonObject();
                    String category = so.has("category") && !so.get("category").isJsonNull()
                            ? so.get("category").getAsString() : null;
                    double startPos = so.has("startPos") && !so.get("startPos").isJsonNull()
                            ? so.get("startPos").getAsDouble() : 0.0;
                    double endPos = so.has("endPos") && !so.get("endPos").isJsonNull()
                            ? so.get("endPos").getAsDouble() : 0.0;
                    info.getSegments().add(new SBSegment(category, startPos, endPos));
                }
            }
        }
        return info;
    }

    private Long getLongOrNull(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return obj.get(key).getAsLong();
    }

    private String getStringOrNull(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }
        return obj.get(key).getAsString();
    }
}
