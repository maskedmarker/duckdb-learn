package com.example.duckdb;

import java.sql.Connection;

/**
 * 演示 3:分析查询 — DuckDB 真正的强项:
 *  - 列式存储 + 向量化执行 -> 聚合/扫描性能极佳
 *  - 完整支持窗口函数(WINDOW)、CTE(WITH)、PIVOT、UNPIVOT 等高级 SQL
 *  - 内置 LIST/STRUCT/MAP 等组合类型
 */
public class Demo03Analytics {

    public static void run() throws Exception {
        System.out.println("\n========== Demo 3: 分析查询(聚合 + 窗口函数) ==========");

        try (Connection conn = DuckDbUtils.openMemory()) {
            // 造一份大一点的数据(20 万行,展示向量化执行的优势)
            String createSql =
                "CREATE TABLE sales AS " +
                "SELECT " +
                "    (i % 50)                                  AS region_id, " +
                "    (i % 17)                                  AS product_id, " +
                "    '2024-' || LPAD(((i % 12) + 1)::VARCHAR, 2, '0') AS month, " +
                "    (random() * 1000)::DECIMAL(10, 2)         AS revenue " +
                "FROM range(0, 200000) t(i)";
            DuckDbUtils.execute(conn, createSql);

            // 总行数
            DuckDbUtils.queryAndPrint(conn, "SELECT COUNT(*) AS rows FROM sales");

            // 聚合:按 region_id + month 求和
            DuckDbUtils.queryAndPrint(conn,
                "SELECT region_id, month, ROUND(SUM(revenue), 2) AS revenue " +
                "FROM sales " +
                "GROUP BY region_id, month " +
                "ORDER BY region_id, month " +
                "LIMIT 6");

            // 窗口函数:每个 region 内的累计收入 + 月度排名
            DuckDbUtils.queryAndPrint(conn,
                "WITH monthly AS (" +
                "    SELECT region_id, month, SUM(revenue) AS revenue " +
                "    FROM sales GROUP BY region_id, month" +
                ") " +
                "SELECT " +
                "    region_id, " +
                "    month, " +
                "    ROUND(revenue, 2) AS revenue, " +
                "    ROUND(SUM(revenue) OVER (PARTITION BY region_id ORDER BY month " +
                "                              ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW), 2) AS cum_revenue, " +
                "    RANK() OVER (PARTITION BY region_id ORDER BY revenue DESC) AS rnk " +
                "FROM monthly " +
                "ORDER BY region_id, month " +
                "LIMIT 10");

            // 列表/组合类型:LIST 列 + list_aggregate
            DuckDbUtils.queryAndPrint(conn,
                "SELECT " +
                "    region_id, " +
                "    LIST(ROUND(revenue, 2)) AS revenues, " +
                "    ROUND(LIST_AVG(LIST(ROUND(revenue, 2))), 2) AS avg_rev, " +
                "    LIST_SORT(LIST(DISTINCT month)) AS months " +
                "FROM sales " +
                "WHERE region_id < 3 " +
                "GROUP BY region_id " +
                "ORDER BY region_id");

            // PIVOT:一行代码把 long 格式变宽表
            DuckDbUtils.queryAndPrint(conn,
                "PIVOT sales " +
                "ON month " +
                "USING SUM(revenue) " +
                "GROUP BY region_id " +
                "ORDER BY region_id " +
                "LIMIT 3");
        }
    }
}
