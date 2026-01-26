/*
 * SkyTube
 * Copyright (C) 2026  Zsombor Gegesy
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
package free.rm.skytube.businessobjects.opml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * OPML parser for importing YouTube subscriptions from OPML files.
 * Provides static methods for parsing OPML content without requiring Android context.
 */
public class OpmlParser {

    // Patterns for extracting YouTube channel IDs from various URL formats
    private static final Pattern YOUTUBE_CHANNEL_PATTERN = Pattern.compile(".*youtube\\.com/(?:user/|channel/|c/)?([^&]+)");
    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile(".*channel_id=([^&]+)");

    /**
     * Represents a parsed YouTube channel from OPML
     */
    public static class ParsedChannel {
        private final String channelId;
        private final String title;
        private final String sourceUrl;

        public ParsedChannel(String channelId, String title, String sourceUrl) {
            this.channelId = channelId;
            this.title = title;
            this.sourceUrl = sourceUrl;
        }

        public String getChannelId() {
            return channelId;
        }

        public String getTitle() {
            return title;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        @Override
        public String toString() {
            return title + " (" + channelId + ") from " + sourceUrl;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ParsedChannel that = (ParsedChannel) o;
            return channelId.equals(that.channelId) &&
                    title.equals(that.title) &&
                    sourceUrl.equals(that.sourceUrl);
        }

        @Override
        public int hashCode() {
            return channelId.hashCode() + title.hashCode() + sourceUrl.hashCode();
        }
    }

    /**
     * Parses OPML content from an InputStream and returns a list of parsed channels.
     *
     * @param inputStream InputStream containing OPML data
     * @return List of parsed channels
     * @throws IOException            If there's an error reading the input
     * @throws XmlPullParserException If there's an error parsing the XML
     */
    public static List<ParsedChannel> parseOpml(InputStream inputStream) throws IOException, XmlPullParserException {
        List<ParsedChannel> channels = new ArrayList<>();


        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(inputStream, null);

            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && "outline".equals(parser.getName())) {
                    ParsedChannel parsedChannel = parseOutlineTag(parser);
                    if (parsedChannel != null) {
                        channels.add(parsedChannel);
                    }
                }
                event = parser.next();
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Ignore close exceptions
                }
            }
        }

        return channels;
    }

    /**
     * Parses an outline tag and extracts channel information if it's a YouTube channel.
     *
     * @param parser the XML pull parser positioned at a start tag
     * @return a parsed channel or null
     */
    private static @Nullable ParsedChannel parseOutlineTag(XmlPullParser parser) {
        String xmlUrl = parser.getAttributeValue(null, "xmlUrl");
        String htmlUrl = parser.getAttributeValue(null, "htmlUrl");
        String title = parser.getAttributeValue(null, "text"); // Use "text" attribute, not "title"
        String type = parser.getAttributeValue(null, "type");

        // Skip if this is not a YouTube-related outline (e.g., folders)
        if (type != null && !"rss".equals(type)) {
            return null;
        }

        String channelId = null;
        String sourceUrl = null;

        // Try to extract from xmlUrl first
        if (xmlUrl != null) {
            channelId = tryExtractChannelId(xmlUrl, CHANNEL_ID_PATTERN, YOUTUBE_CHANNEL_PATTERN);
            if (channelId != null) {
                sourceUrl = xmlUrl;
            }
        }

        // Fallback to htmlUrl if xmlUrl didn't yield a channel ID
        if (channelId == null && htmlUrl != null) {
            channelId = tryExtractChannelId(htmlUrl, YOUTUBE_CHANNEL_PATTERN, null);
            if (channelId != null) {
                sourceUrl = htmlUrl;
            }
        }

        // Add channel if we found an ID (title can be empty but not null)
        if (channelId != null) {
            // Use empty string if title is null
            if (title == null) {
                title = "";
            }
            return new ParsedChannel(channelId, title, sourceUrl);
        }
        return null;
    }

    /**
     * Attempts to extract a channel ID from a URL using the provided patterns.
     */
    private static String tryExtractChannelId(String url, Pattern primaryPattern, Pattern fallbackPattern) {
        Matcher matcher = primaryPattern.matcher(url);
        if (matcher.find()) {
            String channelId = matcher.group(1);
            // Clean query parameters from channel ID
            int questionMark = channelId.indexOf('?');
            if (questionMark != -1) {
                channelId = channelId.substring(0, questionMark);
            }
            return channelId;
        }

        // Try fallback pattern if provided
        if (fallbackPattern != null) {
            matcher = fallbackPattern.matcher(url);
            if (matcher.find()) {
                String channelId = matcher.group(1);
                // Clean query parameters from channel ID
                int questionMark = channelId.indexOf('?');
                if (questionMark != -1) {
                    channelId = channelId.substring(0, questionMark);
                }
                return channelId;
            }
        }

        return null;
    }

    /**
     * Convenience method that parses OPML from a string.
     *
     * @param opmlString String containing OPML data
     * @return List of parsed channels
     * @throws IOException            If there's an error reading the input
     * @throws XmlPullParserException If there's an error parsing the XML
     */
    public static List<ParsedChannel> parseOpml(String opmlString) throws IOException, XmlPullParserException {
        // Using "UTF-8" is intentional, so we can keep the same code in SkytubeLegacy too.
        try (InputStream inputStream = new ByteArrayInputStream(opmlString.getBytes("UTF-8"))) {
            return parseOpml(inputStream);
        }
    }
}