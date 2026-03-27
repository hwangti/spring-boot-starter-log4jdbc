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

import net.sf.log4jdbc.Properties;

import java.sql.Types;
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
    private static final String GRAY = "\033[90m";
    private static final String RESET = "\033[0m";

    /**
     * Default constructor
     */
    public ResultSetCollectorPrinter() {
    }

    /**
     * Return a table which represents a <code>ResultSet</code>, to be printed by a logger,
     * based on the content of the provided <code>resultSetCollector</code>.
     *
     * @param resultSetCollector the ResultSetCollector which has collected the data we want to print
     * @return A <code>String</code> which contains the formatted table to print
     */
    public String getResultSetToPrint(ResultSetCollector resultSetCollector) {
        final StringBuilder table = new StringBuilder();
        int totalRows = resultSetCollector.getRows() != null ? resultSetCollector.getRows().size() : 0;
        table.append(totalRows).append(" row(s) fetched").append(nl);

        int columnCount = resultSetCollector.getColumnCount();
        int[] maxLength = new int[columnCount];
        boolean[] numeric = new boolean[columnCount];

        // 1. Determine numeric columns
        for (int column = 1; column <= columnCount; column++) {
            numeric[column - 1] = isNumericType(resultSetCollector.getColumnType(column));
        }

        int maxColumnWidth = (int) Properties.getResultSetTableMaxColumnWidth();

        // 2. Calculate maximum width of each column (including column name)
        for (int column = 1; column <= columnCount; column++) {
            maxLength[column - 1] = calculateWidth(resultSetCollector.getColumnName(column));
        }

        // 3. Calculate maximum length of row data
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

        // 4. Apply max column width limit
        if (maxColumnWidth > 0) {
            for (int i = 0; i < columnCount; i++) {
                if (maxLength[i] > maxColumnWidth) {
                    maxLength[i] = maxColumnWidth;
                }
            }
        }

        // 5. Top border ┌───┬───┐
        table.append(createBorderLine(maxLength, '┌', '┬', '┐'));

        // 6. Header row │ col │
        String pipe = border("│");
        table.append(pipe);
        for (int column = 1; column <= columnCount; column++) {
            table.append(" ")
                    .append(padRight(truncate(resultSetCollector.getColumnName(column), maxLength[column - 1]), maxLength[column - 1]))
                    .append(" ").append(pipe);
        }
        table.append(nl);

        // 7. Header separator ├───┼───┤
        table.append(createBorderLine(maxLength, '├', '┼', '┤'));

        // 8. Data rows
        int maxRows = (int) Properties.getResultSetTableMaxRows();
        if (resultSetCollector.getRows() != null) {
            int rowIndex = 0;
            for (List<Object> printRow : resultSetCollector.getRows()) {
                if (maxRows > 0 && rowIndex >= maxRows) {
                    break;
                }
                table.append(pipe);
                int colIndex = 0;
                for (Object v : printRow) {
                    String raw = v == null ? DefaultResultSetCollector.NULL_RESULT_SET_VAL : v.toString();
                    boolean isNull = DefaultResultSetCollector.NULL_RESULT_SET_VAL.equals(raw);
                    String value = truncate(raw, maxLength[colIndex]);
                    String padded;
                    if (numeric[colIndex]) {
                        padded = padLeft(value, maxLength[colIndex]);
                    } else {
                        padded = padRight(value, maxLength[colIndex]);
                    }
                    table.append(" ")
                            .append(isNull ? gray(padded) : padded)
                            .append(" ").append(pipe);
                    colIndex++;
                }
                table.append(nl);
                rowIndex++;
            }
            // Ellipsis row + summary outside table
            if (maxRows > 0 && totalRows > maxRows) {
                int omitted = totalRows - maxRows;
                table.append(pipe);
                for (int i = 0; i < columnCount; i++) {
                    if (numeric[i]) {
                        table.append(" ").append(padLeft("...", maxLength[i])).append(" ").append(pipe);
                    } else {
                        table.append(" ").append(padRight("...", maxLength[i])).append(" ").append(pipe);
                    }
                }
                table.append(nl);
                table.append(createBorderLine(maxLength, '└', '┴', '┘'));
                table.append(" ... ").append(omitted).append(" row(s) omitted").append(nl);
                resultSetCollector.reset();
                return table.toString();
            }
        }

        // 9. Bottom border └───┴───┘
        table.append(createBorderLine(maxLength, '└', '┴', '┘'));

        resultSetCollector.reset();
        return table.toString();
    }

    private static boolean isNumericType(int sqlType) {
        switch (sqlType) {
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return true;
            default:
                return false;
        }
    }

    private static int charWidth(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return (block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
                block == Character.UnicodeBlock.HANGUL_JAMO ||
                block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
        ) ? 17 : 10;
    }

    private static int calculateWidth(String value) {
        int width = 0;
        for (char c : value.toCharArray()) {
            width += charWidth(c);
        }
        return width / 10;
    }

    private static String padRight(String s, int n) {
        int padding = n - calculateWidth(s);
        StringBuilder padded = new StringBuilder(s);
        for (int i = 0; i < padding; i++) {
            padded.append(" ");
        }
        return padded.toString();
    }

    private static String truncate(String s, int maxWidth) {
        if (maxWidth <= 0 || calculateWidth(s) <= maxWidth) {
            return s;
        }
        int ellipsisWidth = 3; // "..." = 3 columns
        StringBuilder sb = new StringBuilder();
        int widthAccum = 0;
        for (char c : s.toCharArray()) {
            int cw = charWidth(c);
            if ((widthAccum + cw) / 10 > maxWidth - ellipsisWidth) {
                break;
            }
            sb.append(c);
            widthAccum += cw;
        }
        sb.append("...");
        return sb.toString();
    }

    private static String padLeft(String s, int n) {
        int padding = n - calculateWidth(s);
        StringBuilder padded = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            padded.append(" ");
        }
        padded.append(s);
        return padded.toString();
    }

    private static String border(String s) {
        if (Properties.isHighlightSql()) {
            return GRAY + s + RESET;
        }
        return s;
    }

    private static String gray(String s) {
        if (Properties.isHighlightSql()) {
            return GRAY + s + RESET;
        }
        return s;
    }

    private String createBorderLine(int[] maxLength, char left, char middle, char right) {
        StringBuilder line = new StringBuilder();
        line.append(left);
        for (int i = 0; i < maxLength.length; i++) {
            // +2 for left/right padding spaces
            for (int j = 0; j < maxLength[i] + 2; j++) {
                line.append('─');
            }
            line.append(i < maxLength.length - 1 ? middle : right);
        }
        return border(line.toString()) + nl;
    }
}
