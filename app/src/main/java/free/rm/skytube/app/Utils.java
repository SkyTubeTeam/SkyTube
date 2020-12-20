/*
 * SkyTube
 * Copyright (C) 2019  Zsombor Gegesy
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
package free.rm.skytube.app;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;

public class Utils {

    private static final NumberFormat speedFormatter = new DecimalFormat("0.0x");

    public static String formatSpeed(double speed) {
        return speedFormatter.format(speed);
    }

    public static void isTrue(boolean flag, String message) {
        if (!flag) {
            throw new IllegalArgumentException("Error: "+message);
        }
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static Integer min(Integer a, Integer b) {
        if (a == null) {
            return b;
        } else {
            if (b != null) {
                return Math.min(a, b);
            } else {
                return a;
            }
        }
    }

    public static int hash(Object... obj) {
        return Arrays.hashCode(obj);
    }

    /**
     * Some channelIds have prefix in them.
     * We need to remove them otherwise channel ids might not match in different places and cause problems.
     * @param channelId to check for prefix
     * @return channelId without prefix
     */
    public static String removeChannelIdPrefix(String channelId){
        if (channelId.contains("channel/")){
            channelId = channelId.split("channel/")[1];
        }
        return  channelId;
    }

    /**
     * Creates len amount of "?" for using in WHERE clause
     * @param len length of list
     * @return len amount of "?"
     */
    public static String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query anyway ..
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }

}
