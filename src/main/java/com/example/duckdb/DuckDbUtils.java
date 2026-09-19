package com.example.duckdb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 共用工具方法:获取 DuckDB 连接、执行 SQL 并格式化打印结果。
 * DuckDB 通过 JDBC URL 区分模式:
 *   - "jdbc:duckdb:"             内存数据库(进程退出即销毁)
 *   - "jdbc:duckdb:path/to/db"   持久化数据库(SQLite 风格单文件)
 *   - "jdbc:duckdb:"(空)         也是内存模式
 */
public final class DuckDbUtils {

    private DuckDbUtils() {}

    /** 打开内存数据库连接。 */
    public static Connection openMemory() throws SQLException {
        return DriverManager.getConnection("jdbc:duckdb:");
    }

    /** 打开持久化数据库连接(文件不存在则创建)。 */
    public static Connection openFile(String path) throws SQLException {
        return DriverManager.getConnection("jdbc:duckdb:" + path);
    }

    /** 执行任意 SQL(DDL/DML/查询均可)。 */
    public static void execute(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /** 打印 ResultSet 为对齐表格。 */
    public static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();

        String[] headers = new String[cols];
        int[] widths = new int[cols];
        for (int i = 0; i < cols; i++) {
            headers[i] = md.getColumnLabel(i + 1);
            widths[i] = headers[i].length();
        }

        java.util.List<String[]> rows = new java.util.ArrayList<>();
        while (rs.next()) {
            String[] row = new String[cols];
            for (int i = 0; i < cols; i++) {
                Object v = rs.getObject(i + 1);
                row[i] = v == null ? "NULL" : v.toString();
                widths[i] = Math.max(widths[i], row[i].length());
            }
            rows.add(row);
        }

        StringBuilder sb = new StringBuilder();
        appendSeparator(sb, widths);
        sb.append('\n');
        appendRow(sb, headers, widths);
        appendSeparator(sb, widths);
        sb.append('\n');
        for (String[] row : rows) {
            appendRow(sb, row, widths);
            sb.append('\n');
        }
        appendSeparator(sb, widths);
        sb.append('\n');
        sb.append(rows.size()).append(" row(s)\n");
        System.out.print(sb);
    }

    private static void appendSeparator(StringBuilder sb, int[] widths) {
        sb.append('+');
        for (int w : widths) {
            for (int i = 0; i < w + 2; i++) sb.append('-');
            sb.append('+');
        }
    }

    private static void appendRow(StringBuilder sb, String[] row, int[] widths) {
        sb.append('|');
        for (int i = 0; i < row.length; i++) {
            sb.append(' ').append(padRight(row[i], widths[i])).append(" |");
        }
    }

    private static String padRight(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }

    /** 简化调用:执行 SELECT 并打印。 */
    public static void queryAndPrint(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\nSQL> " + sql);
            printResultSet(rs);
        }
    }
}
