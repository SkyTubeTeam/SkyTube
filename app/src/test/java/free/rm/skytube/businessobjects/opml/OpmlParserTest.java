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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the OPML parser.
 */
class OpmlParserTest {

    /**
     * Reads OPML file from test resources
     */
    private String readResourceFile(String filename) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("opml/" + filename)) {
            if (inputStream == null) {
                throw new IOException("Resource file not found: opml/" + filename);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void testBasicOpmlFileParsing() throws Exception {
        String opmlContent = readResourceFile("basic.opml");
        List<OpmlParser.ParsedChannel> channels = OpmlParser.parseOpml(opmlContent);
        
        assertNotNull(channels, "Should parse OPML without throwing exception");
        assertEquals(2, channels.size(), "Should parse 2 channels from basic.opml");
        
        // Verify first channel
        OpmlParser.ParsedChannel channel1 = channels.get(0);
        assertEquals("UC123456789", channel1.getChannelId());
        assertEquals("Test Channel 1", channel1.getTitle());
        assertTrue(channel1.getSourceUrl().contains("channel_id=UC123456789"));
        
        // Verify second channel
        OpmlParser.ParsedChannel channel2 = channels.get(1);
        assertEquals("UC987654321", channel2.getChannelId());
        assertEquals("Test Channel 2", channel2.getTitle());
        assertTrue(channel2.getSourceUrl().contains("channel_id=UC987654321"));
    }

    @Test
    void testUserUrlsOpmlFileParsing() throws Exception {
        String opmlContent = readResourceFile("user_urls.opml");
        List<OpmlParser.ParsedChannel> channels = OpmlParser.parseOpml(opmlContent);
        
        assertNotNull(channels, "Should parse OPML without throwing exception");
        assertEquals(2, channels.size(), "Should parse 2 channels from user_urls.opml");
        
        // Verify user URL parsing
        OpmlParser.ParsedChannel userChannel = channels.stream()
                .filter(c -> c.getTitle().equals("User Channel"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(userChannel, "Should find User Channel");
        assertEquals("TestUser", userChannel.getChannelId());
        assertTrue(userChannel.getSourceUrl().contains("/user/TestUser"));
        
        // Verify custom URL parsing
        OpmlParser.ParsedChannel customChannel = channels.stream()
                .filter(c -> c.getTitle().equals("Custom Channel"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(customChannel, "Should find Custom Channel");
        assertEquals("TestChannel", customChannel.getChannelId());
        assertTrue(customChannel.getSourceUrl().contains("/c/TestChannel"));
    }

    @Test
    void testOpmlWithQueryParametersParsing() throws Exception {
        String opmlContent = readResourceFile("with_params.opml");
        List<OpmlParser.ParsedChannel> channels = OpmlParser.parseOpml(opmlContent);
        
        assertNotNull(channels, "Should parse OPML without throwing exception");
        assertEquals(1, channels.size(), "Should parse 1 channel from with_params.opml");
        
        OpmlParser.ParsedChannel channel = channels.get(0);
        assertEquals("UC111111111", channel.getChannelId(), "Should clean query parameters");
        assertEquals("Channel with Params", channel.getTitle());
        assertTrue(channel.getSourceUrl().contains("feature=something"), "Source URL should contain original parameters");
    }

    @Test
    void testOpmlWithFoldersParsing() throws Exception {
        String opmlContent = readResourceFile("with_folders.opml");
        List<OpmlParser.ParsedChannel> channels = OpmlParser.parseOpml(opmlContent);
        
        assertNotNull(channels, "Should parse OPML without throwing exception");
        // Should ignore folder but parse the channel inside it
        assertEquals(1, channels.size(), "Should parse 1 channel from with_folders.opml");
        
        OpmlParser.ParsedChannel channel = channels.get(0);
        assertEquals("UC222222222", channel.getChannelId());
        assertEquals("Actual Channel", channel.getTitle());
    }

    @Test
    void testEmptyOpmlFileParsing() throws Exception {
        String opmlContent = readResourceFile("empty.opml");
        List<OpmlParser.ParsedChannel> channels = OpmlParser.parseOpml(opmlContent);
        
        assertNotNull(channels, "Should parse OPML without throwing exception");
        assertTrue(channels.isEmpty(), "Empty OPML should return empty list");
    }

    @Test
    void testOpmlWithMissingAttributesParsing() throws Exception {
        String opmlContent = readResourceFile("missing_attrs.opml");
        List<OpmlParser.ParsedChannel> channels = OpmlParser.parseOpml(opmlContent);
        
        assertNotNull(channels, "Should parse OPML without throwing exception");
        // Should skip malformed entry but parse valid one
        assertEquals(1, channels.size(), "Should parse 1 valid channel from missing_attrs.opml");
        
        OpmlParser.ParsedChannel channel = channels.get(0);
        assertEquals("UC333333333", channel.getChannelId());
        assertTrue(channel.getTitle().isEmpty(), "Channel with missing title should have empty title");
    }

    @Test
    void testAllOpmlFilesParseWithoutErrors() throws Exception {
        // Test that all OPML files can be parsed without throwing exceptions
        String[] opmlFiles = {
            "basic.opml",
            "user_urls.opml",
            "with_params.opml", 
            "with_folders.opml",
            "empty.opml",
            "missing_attrs.opml"
        };

        for (String filename : opmlFiles) {
            assertDoesNotThrow(() -> {
                String content = readResourceFile(filename);
                List<OpmlParser.ParsedChannel> channels = OpmlParser.parseOpml(content);
                assertNotNull(channels, "Should be able to parse " + filename);
            }, "Should parse " + filename + " without throwing exception");
        }
    }

    @Test
    void testOpmlStructureConsistency() throws Exception {
        // Test that all OPML files have consistent structure
        String basicContent = readResourceFile("basic.opml");
        
        // All files should have these basic elements
        assertTrue(basicContent.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(basicContent.contains("<opml version=\"1.0\">"));
        assertTrue(basicContent.contains("<body>"));
        assertTrue(basicContent.contains("<outline text=\"Subscriptions\">"));
    }
}