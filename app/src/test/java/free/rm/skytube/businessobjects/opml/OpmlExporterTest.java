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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeChannel;

/**
 * Test class for OPML exporter functionality.
 */
public class OpmlExporterTest {
    /**
     * Mock YouTubeChannel for testing that doesn't require SkyTubeApp initialization.
     */
    static class MockYouTubeChannel extends YouTubeChannel {

        public MockYouTubeChannel(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }
    
    private List<YouTubeChannel> testChannels;
    
    @BeforeEach
    public void setUp() {
        testChannels = new ArrayList<>();
        testChannels.add(new MockYouTubeChannel("UC1234567890", "Test Channel 1"));
        testChannels.add(new MockYouTubeChannel("UC_abc-def_ghi", "Test & Channel 2"));
        testChannels.add(new MockYouTubeChannel("UC123", "Channel <with> tags"));
    }
    
    @Test
    public void testDefaultExportFileName() {
        String fileName = OpmlExporter.getDefaultExportFileName();
        
        Assertions.assertTrue(fileName.startsWith("skytube_subscriptions_"), 
                            "File name should start with skytube_subscriptions_");
        Assertions.assertTrue(fileName.endsWith(".opml"), 
                            "File name should end with .opml");
        Assertions.assertTrue(fileName.length() > "skytube_subscriptions_.opml".length(), 
                            "File name should contain datetime");
        
        // Verify ISO datetime format (yyyy-MM-dd_HH-mm-ss)
        String datetimePart = fileName.replace("skytube_subscriptions_", "").replace(".opml", "");
        Assertions.assertTrue(datetimePart.matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}"), 
                            "File name should contain ISO datetime format (yyyy-MM-dd_HH-mm-ss)");
    }

    @Test
    public void testGenerateOpmlCorrect() throws ParserConfigurationException, IOException, SAXException {
        String opmlContent = OpmlExporter.generateOpmlContent(testChannels);

        assertOpmlIsValidXml(opmlContent, 3);

        // Test that outline elements have correct structure
        Assertions.assertTrue(opmlContent.contains("type=\"rss\""),
                "Outline should have type='rss'");
        Assertions.assertTrue(opmlContent.contains("xmlUrl=\"https://www.youtube.com/feeds/videos.xml?channel_id="),
                "Outline should have correct xmlUrl format");
        Assertions.assertTrue(opmlContent.contains("htmlUrl=\"https://www.youtube.com/channel/"),
                "Outline should have correct htmlUrl format");

        // Test that all channels are included
        Assertions.assertTrue(opmlContent.contains("Test Channel 1"),
                            "OPML should contain first channel title");
        Assertions.assertTrue(opmlContent.contains("UC1234567890"),
                            "OPML should contain first channel ID");
        
        Assertions.assertTrue(opmlContent.contains("Test &amp; Channel 2"),
                            "OPML should contain second channel title");
        Assertions.assertTrue(opmlContent.contains("UC_abc-def_ghi"),
                            "OPML should contain second channel ID");

        Assertions.assertTrue(opmlContent.contains("Channel &lt;with&gt; tags"),
                "Angle brackets should be escaped as &lt; and &gt;");
    }

    @Test
    public void testChannelIdFiltering() {
        // Test valid channel IDs
        Assertions.assertEquals("UC1234567890", OpmlExporter.filterXmlSafeChannelId("UC1234567890"));
        Assertions.assertEquals("UC_abc-def_ghi", OpmlExporter.filterXmlSafeChannelId("UC_abc-def_ghi"));
        
        // Test filtering of unsafe characters
        Assertions.assertEquals("UC123", OpmlExporter.filterXmlSafeChannelId("UC123!@#$%"));
        Assertions.assertEquals("UC_abc", OpmlExporter.filterXmlSafeChannelId("UC_abc<>?"));
        Assertions.assertEquals("UC", OpmlExporter.filterXmlSafeChannelId("UC!@#$%^&*()"));
        
        // Test null and empty
        Assertions.assertEquals("", OpmlExporter.filterXmlSafeChannelId(null));
        Assertions.assertEquals("", OpmlExporter.filterXmlSafeChannelId(""));
        Assertions.assertEquals("", OpmlExporter.filterXmlSafeChannelId("   "));
    }
    
    @Test
    public void testXmlEscapingFunction() {
        // Test basic escaping
        Assertions.assertEquals("test &amp; test", OpmlExporter.escapeXml("test & test"));
        Assertions.assertEquals("test &lt;test&gt;", OpmlExporter.escapeXml("test <test>"));
        Assertions.assertEquals("test &quot;test&quot;", OpmlExporter.escapeXml("test \"test\""));
        Assertions.assertEquals("test &apos;test&apos;", OpmlExporter.escapeXml("test 'test'"));
        
        // Test combined escaping
        Assertions.assertEquals("test &amp; &lt;test&gt; &quot;test&quot; &apos;test&apos;", 
                            OpmlExporter.escapeXml("test & <test> \"test\" 'test'"));
        
        // Test null and empty
        Assertions.assertEquals("", OpmlExporter.escapeXml(null));
        Assertions.assertEquals("", OpmlExporter.escapeXml(""));
    }
    
    @Test
    public void testEmptyChannelList() throws ParserConfigurationException, IOException, SAXException {
        List<YouTubeChannel> emptyList = new ArrayList<>();
        String opmlContent = OpmlExporter.generateOpmlContent(emptyList);
        assertOpmlIsValidXml(opmlContent, 0);
        
        // Should still generate valid OPML structure even with no channels
        Assertions.assertTrue(opmlContent.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"),
                            "OPML should start with XML declaration even when empty");
        Assertions.assertTrue(opmlContent.contains("<opml version=\"1.0\">"),
                            "OPML should contain OPML version even when empty");
        Assertions.assertTrue(opmlContent.endsWith("  </body>\n</opml>"),
                            "OPML should end with proper closing tags even when empty");
    }
    
    @Test
    public void testChannelWithNullTitle() throws ParserConfigurationException, IOException, SAXException {
        List<YouTubeChannel> channelsWithNull = List.of(new MockYouTubeChannel("UC123", null));

        String opmlContent = OpmlExporter.generateOpmlContent(channelsWithNull);
        assertOpmlIsValidXml(opmlContent, 1);

        // Should use "" for null titles
        Assertions.assertTrue(opmlContent.contains("text=\"\""),
                            "Should use empty string for null channel titles");
    }

    private void assertOpmlIsValidXml(String opmlContent, int numberOfChannels) throws ParserConfigurationException, IOException, SAXException {
        // Validate that the generated OPML is valid XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        // Parse the OPML content as XML
        Document document = builder.parse(new InputSource(new StringReader(opmlContent)));
        
        // Verify basic structure
        Assertions.assertNotNull(document, "OPML content should be valid XML");
        Assertions.assertEquals("opml", document.getDocumentElement().getNodeName(), "Root element should be 'opml'");
        Assertions.assertEquals("1.0", document.getDocumentElement().getAttribute("version"), "OPML version should be 1.0");
        
        // Verify head section
        NodeList headElements = document.getElementsByTagName("head");
        Assertions.assertEquals(1, headElements.getLength(), "Should have exactly one head element");
        
        // Verify title in head
        NodeList titleElements = document.getElementsByTagName("title");
        Assertions.assertEquals(1, titleElements.getLength(), "Should have exactly one title element");
        Assertions.assertEquals("SkyTube Subscriptions Export", titleElements.item(0).getTextContent(), "Title should be 'SkyTube Subscriptions Export'");
        
        // Verify body section
        NodeList bodyElements = document.getElementsByTagName("body");
        Assertions.assertEquals(1, bodyElements.getLength(), "Should have exactly one body element");
        
        // Verify outline elements
        NodeList outlineElements = document.getElementsByTagName("outline");
        Assertions.assertEquals(numberOfChannels, outlineElements.getLength(), "Should have one outline elements for each channel");
        
        // Verify each outline has required attributes
        for (int i = 0; i < outlineElements.getLength(); i++) {
            Element outline = (Element) outlineElements.item(i);
            Assertions.assertTrue(outline.hasAttribute("text"), "Outline should have text attribute");
            Assertions.assertTrue(outline.hasAttribute("type"), "Outline should have type attribute");
            Assertions.assertTrue(outline.hasAttribute("xmlUrl"), "Outline should have xmlUrl attribute");
            Assertions.assertTrue(outline.hasAttribute("htmlUrl"), "Outline should have htmlUrl attribute");
            Assertions.assertEquals("rss", outline.getAttributes().getNamedItem("type").getNodeValue(), "Outline type should be 'rss'");
        }
    }
}