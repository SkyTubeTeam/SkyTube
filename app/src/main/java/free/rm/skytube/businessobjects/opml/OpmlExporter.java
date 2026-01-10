/*
 * SkyTube
 * Copyright (C) 2026  SkyTube Team
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

import android.content.Context;
import android.net.Uri;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import free.rm.skytube.businessobjects.Logger;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

/**
 * Utility class for exporting subscriptions to OPML format.
 */
public class OpmlExporter {

    private static final String OPML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<opml version=\"1.0\">\n" +
            "  <head>\n" +
            "    <title>SkyTube Subscriptions Export</title>\n" +
            "  </head>\n" +
            "  <body>\n";

    private static final String OPML_FOOTER = "  </body>\n" +
            "</opml>";

    private static final String OUTLINE_TEMPLATE = "    <outline text=\"%s\" type=\"rss\" xmlUrl=\"https://www.youtube.com/feeds/videos.xml?channel_id=%s\" htmlUrl=\"https://www.youtube.com/channel/%s\" />\n";

    /**
     * Export subscriptions to OPML file.
     *
     * @param outputFile The file to export to
     * @return true if export was successful, false otherwise
     */
    public static boolean exportSubscriptionsToOpml(File outputFile) throws IOException {
        List<YouTubeChannel> channels = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannels();

        if (channels.isEmpty()) {
            return false;
        }

        // Generate OPML content
        String opmlContent = generateOpmlContent(channels);

        // Ensure parent directory exists
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                return false;
            }
        }

        // Write to file
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(opmlContent);
        }
        return true;
    }

    /**
     * Export subscriptions to OPML using Storage Access Framework.
     *
     * @param context Android context
     * @param outputUri The URI to export to (from SAF)
     * @return true if export was successful, false otherwise
     */
    public static boolean exportSubscriptionsToOpmlWithSaf(Context context, Uri outputUri) throws IOException {

        List<YouTubeChannel> channels = SubscriptionsDb.getSubscriptionsDb().getSubscribedChannels();

        if (channels.isEmpty()) {
            return false;
        }

        String opmlContent = generateOpmlContent(channels);

        try (OutputStream outputStream = context.getContentResolver().openOutputStream(outputUri)) {
            if (outputStream != null) {
                byte[] bytes = opmlContent.getBytes("UTF-8");
                outputStream.write(bytes);
                outputStream.flush();
                return true;
            }
            return false;
        }
    }

    /**
     * Generate OPML content from a list of channels.
     * This method is testable without file I/O.
     *
     * @param channels List of YouTube channels to export
     * @return OPML content as a string
     */
    static String generateOpmlContent(List<YouTubeChannel> channels) {
        StringBuilder opmlBuilder = new StringBuilder();

        opmlBuilder.append(OPML_HEADER);

        // Add each subscription as an outline element
        for (YouTubeChannel channel : channels) {
            String channelId = filterXmlSafeChannelId(channel.getId());
            String channelTitle = channel.getTitle() != null ?
                    escapeXml(channel.getTitle()) : "";

            String outline = String.format(OUTLINE_TEMPLATE, channelTitle, channelId, channelId);
            opmlBuilder.append(outline);
        }

        opmlBuilder.append(OPML_FOOTER);

        return opmlBuilder.toString();
    }

    /**
     * Filter channel ID to ensure it's XML-safe.
     * Removes any characters that are not safe for XML attributes.
     *
     * @param channelId The raw channel ID
     * @return XML-safe channel ID
     */
    static String filterXmlSafeChannelId(String channelId) {
        if (channelId == null) {
            return "";
        }

        // Keep only alphanumeric characters, hyphens, and underscores
        // This matches YouTube's channel ID format
        return channelId.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    /**
     * Escape XML special characters in text.
     *
     * @param text The text to escape
     * @return XML-escaped text
     */
    static String escapeXml(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * Get the default export file name with ISO datetime format.
     *
     * @return Default file name for OPML export (e.g., skytube_subscriptions_2024-01-10_14-30-45.opml)
     */
    public static String getDefaultExportFileName() {
        SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
        String currentDatetime = datetimeFormat.format(new Date());
        return "skytube_subscriptions_" + currentDatetime + ".opml";
    }
}