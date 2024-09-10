/*
 * Copyright 2010 Tim Azzopardi
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

package net.sf.log4jdbc.sql.resultsetcollector;

import java.util.List;

/**
 * <p>A class which is used to print a <code>ResultSet</code> in a formatted table.</p>
 * Update : changed printResultSet into getResultSetToPrint
 *
 * @author Tim Azzopardi
 * @author Mathieu Seppey
 */
public class ResultSetCollectorPrinter {

    private static final String nl = System.lineSeparator();

    /**
     * Default constructor
     */
    public ResultSetCollectorPrinter() {
    }

    /**
     * Return a table which represents a <code>ResultSet</code>, to be printed by a logger,
     * based on the content of the provided <code>resultSetCollector</code>.
     *
     * <p>This method will be actually called by a <code>SpyLogDelegator</code>
     * when the <code>next()</code> method of the spied <code>ResultSet</code>
     * return <code>false</code> meaning that its end is reached.
     * It will be also called if the <code>ResultSet</code> is closed.</p>
     *
     * @param resultSetCollector the ResultSetCollector which has collected the data we want to print
     * @return A <code>String</code> which contains the formatted table to print
     *
     * @see net.sf.log4jdbc.sql.jdbcapi.ResultSetSpy
     * @see net.sf.log4jdbc.sql.resultsetcollector.DefaultResultSetCollector
     * @see net.sf.log4jdbc.log.SpyLogDelegator
     */
    public String getResultSetToPrint(ResultSetCollector resultSetCollector) {
        /*
         * A StringBuffer which is used to build the formatted table to print
         */
        final StringBuilder table = new StringBuilder();
        table.append(nl);

        int columnCount = resultSetCollector.getColumnCount();
        int[] maxLength = new int[columnCount];

        // 1. Calculate maximum width of each column (including column name)
        for (int column = 1; column <= columnCount; column++) {
            maxLength[column - 1] = calculateWidth(resultSetCollector.getColumnName(column));
        }

        // 2. Calculate maximum length of row data
        if (resultSetCollector.getRows() != null) {
            for (List<Object> printRow : resultSetCollector.getRows()) {
                int colIndex = 0;
                for (Object v : printRow) {
                    if (v != null) {
                        int length = calculateWidth(v.toString());
                        if (length > maxLength[colIndex]) {
                            maxLength[colIndex] = length;
                        }
                    }
                    colIndex++;
                }
            }
        }

        // 3. Add free space
        for (int i = 0; i < columnCount; i++) {
            maxLength[i] += 1;
        }

        // 4. Remove duplicates with border and header generation functions
        String boundaryLine = createBoundaryLine(maxLength);
        table.append(boundaryLine);
        table.append("|");

        for (int column = 1; column <= columnCount; column++) {
            table.append(padRight(resultSetCollector.getColumnName(column), maxLength[column - 1]))
                    .append("|");
        }

        table.append(nl);
        table.append(boundaryLine);

        // 5. Data output
        if (resultSetCollector.getRows() != null) {
            for (List<Object> printRow : resultSetCollector.getRows()) {
                table.append("|");
                int colIndex = 0;
                for (Object v : printRow) {
                    table.append(padRight(v == null ? "NULL" : v.toString(), maxLength[colIndex]))
                            .append("|");
                    colIndex++;
                }
                table.append(nl);
            }
        }

        table.append(boundaryLine);

        resultSetCollector.reset();
        return table.toString();
    }

    private static int calculateWidth(String value) {
        int width = 0;
        for (char c : value.toCharArray()) {
            Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(c);
            width += (
                    unicodeBlock == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                            unicodeBlock == Character.UnicodeBlock.HANGUL_JAMO ||
                            unicodeBlock == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
            ) ? 17 : 10;
        }
        return width / 10;
    }

    /**
     * Add space to the provided <code>String</code> to match the provided width
     * @param s the <code>String</code> we want to adjust
     * @param n the width of the returned <code>String</code>
     * @return a <code>String</code> matching the provided width
     */
    private static String padRight(String s, int n) {
        int padding = n - calculateWidth(s);
        StringBuilder padded = new StringBuilder(s);
        for (int i = 0; i < padding; i++) {
            padded.append(" ");
        }
        return padded.toString();
    }

    private String createBoundaryLine(int[] maxLength) {
        StringBuilder line = new StringBuilder("|");
        for (int length : maxLength) {
            for (int i = 0; i < length; i++) {
                line.append("-");
            }
            line.append("|");
        }
        return line.append(nl).toString();
    }
}
