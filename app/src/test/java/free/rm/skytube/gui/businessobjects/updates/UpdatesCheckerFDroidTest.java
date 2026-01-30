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

package free.rm.skytube.gui.businessobjects.updates;

import static org.junit.jupiter.api.Assertions.*;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.junit.jupiter.api.Test;

class UpdatesCheckerFDroidTest {

    private static final String FDROID_RESPONSE =
            "{\"packageName\":\"free.rm.skytube.oss\"," +
            "\"suggestedVersionCode\":59," +
            "\"packages\":" +
            "[{\"versionName\":\"2.999\",\"versionCode\":59}," +
             "{\"versionName\":\"2.998\",\"versionCode\":58}," +
             "{\"versionName\":\"2.997\",\"versionCode\":57}]}";

    private JsonObject parseJson(String json) throws JsonParserException {
        return JsonParser.object().from(json);
    }

    @Test
    void testGetVersionReturnsCorrectVersionNameForMatchingVersionCode() throws JsonParserException {
        JsonObject json = parseJson(FDROID_RESPONSE);
        UpdatesChecker checker = new UpdatesChecker(false, "2.998");

        String version = checker.getVersion(json, 59);

        assertEquals("2.999", version);
    }

    @Test
    void testGetVersionReturnsMiddleVersion() throws JsonParserException {
        JsonObject json = parseJson(FDROID_RESPONSE);
        UpdatesChecker checker = new UpdatesChecker(false, "2.999");

        String version = checker.getVersion(json, 58);

        assertEquals("2.998", version);
    }

    @Test
    void testGetVersionReturnsFirstVersion() throws JsonParserException {
        JsonObject json = parseJson(FDROID_RESPONSE);
        UpdatesChecker checker = new UpdatesChecker(false, "2.997");

        String version = checker.getVersion(json, 57);

        assertEquals("2.997", version);
    }

    @Test
    void testGetVersionReturnsNullForNonExistentVersionCode() throws JsonParserException {
        JsonObject json = parseJson(FDROID_RESPONSE);
        UpdatesChecker checker = new UpdatesChecker(false, "2.999");

        String version = checker.getVersion(json, 999);

        assertNull(version);
    }

    @Test
    void testGetVersionWithSinglePackage() throws JsonParserException {
        JsonObject json = parseJson(
                "{\"packageName\":\"free.rm.skytube.oss\"," +
                "\"suggestedVersionCode\":1," +
                "\"packages\":[{\"versionName\":\"1.0.0\",\"versionCode\":1}]}");
        UpdatesChecker checker = new UpdatesChecker(false, "0.9");

        String version = checker.getVersion(json, 1);

        assertEquals("1.0.0", version);
    }

    @Test
    void testGetVersionWithEmptyPackagesArray() throws JsonParserException {
        JsonObject json = parseJson(
                "{\"packageName\":\"free.rm.skytube.oss\"," +
                "\"suggestedVersionCode\":1," +
                "\"packages\":[]}");
        UpdatesChecker checker = new UpdatesChecker(false, "0.9");

        String version = checker.getVersion(json, 1);

        assertNull(version);
    }
}
