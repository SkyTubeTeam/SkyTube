/*
 * SkyTube
 * Copyright (C) 2021  Zsombor Gegesy
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
package free.rm.skytube.businessobjects.YouTube.newpipe;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.schabi.newpipe.extractor.stream.Description;

public class NewPipeUtils {
    private NewPipeUtils() {}

    public static String filterHtml(String content) {
        return Jsoup.clean(content, "", Safelist.basic(), new Document.OutputSettings().prettyPrint(false));
    }

    public static String filterHtml(Description description) {
        String result;
        if (description.getType() == Description.HTML) {
            result = filterHtml(description.getContent());
        } else {
            result = description.getContent();
        }
        return result;
    }

}
