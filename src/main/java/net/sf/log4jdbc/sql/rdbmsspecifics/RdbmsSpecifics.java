/*
 * Copyright 2007-2012 Arthur Blake
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.log4jdbc.sql.rdbmsspecifics;

import net.sf.log4jdbc.Properties;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Encapsulate sql formatting details about a particular relational database management system so that
 * accurate, useable SQL can be composed for that RDMBS.
 *
 * @author Arthur Blake
 */
public class RdbmsSpecifics {

    /**
     * Date format string for use in formatting dates.
     */
    protected static final String dateFormat = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * Default constructor.
     */
    public RdbmsSpecifics() {
    }

    /**
     * Format an Object that is being bound to a PreparedStatement parameter, for display. The goal is to reformat the
     * object in a format that can be re-run against the native SQL client of the particular Rdbms being used.  This
     * class should be extended to provide formatting instances that format objects correctly for different RDBMS
     * types.
     *
     * @param object jdbc object to be formatted.
     * @return formatted dump of the object.
     */
    public String formatParameterObject(Object object) {
        if (object == null) {
            return "NULL";
        }

        if (object instanceof String) {
            return "'" + escapeString((String) object) + "'";
        } else if (object instanceof Date) {
            return "'" + new SimpleDateFormat(dateFormat).format(object) + "'";
        } else if (object instanceof Boolean) {
            return Properties.isDumpBooleanAsTrueFalse() ?
                    (Boolean) object ? "true" : "false"
                    : (Boolean) object ? "1" : "0";
        } else {
            return object.toString();
        }
    }

    /**
     * Make sure string is escaped properly so that it will run in a SQL query analyzer tool.
     * At this time all we do is double any single tick marks.
     * Do not call this with a null string or else an exception will occur.
     *
     * @return the input String, escaped.
     */
    String escapeString(String in) {
        StringBuilder out = new StringBuilder();
        for (int i = 0, j = in.length(); i < j; i++) {
            char c = in.charAt(i);
            if (c == '\'') {
                out.append(c);
            }
            out.append(c);
        }
        return out.toString();
    }

}
