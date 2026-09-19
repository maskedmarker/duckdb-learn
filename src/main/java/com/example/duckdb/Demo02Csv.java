package com.example.duckdb;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 演示 2:CSV 直接查询 — DuckDB 可以把 CSV/Parquet/JSON 当成表直接查,
 * 无需先建表 + 导入。这是它最受欢迎的特性之一。
 */
public class Demo02Csv {

    public static void run() throws Exception {
        System.out.println("\n========== Demo 2: CSV 文件直接查询 ==========");

        File csv = new File("src/main/resources/data/employees.csv");
        if (!csv.exists()) {
            System.out.println("[跳过] 找不到 " + csv.getAbsolutePath());
            return;
        }

        try (Connection conn = DuckDbUtils.openMemory()) {
            String path = csv.getAbsolutePath().replace('\\', '/');

            // 1) read_csv_auto:自动推断 schema + 直接读
            DuckDbUtils.queryAndPrint(conn,
                "SELECT * FROM read_csv_auto('" + path + "') LIMIT 3");

            // 2) 给外部 CSV 创建一个"视图",之后像普通表一样用
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE VIEW employees AS SELECT * FROM read_csv_auto('" + path + "')");
            }

            DuckDbUtils.queryAndPrint(conn,
                "SELECT department, COUNT(*) AS cnt, ROUND(AVG(salary), 2) AS avg_salary " +
                "FROM employees GROUP BY department ORDER BY avg_salary DESC");

            // 3) 多 CSV JOIN:两个文件直接做关联查询
            File ordersCsv = new File("src/main/resources/data/orders.csv");
            String ordersPath = ordersCsv.getAbsolutePath().replace('\\', '/');
            String sql =
                "SELECT e.name, COUNT(o.id) AS orders, ROUND(SUM(o.quantity * o.price), 2) AS spent " +
                "FROM read_csv_auto('" + ordersPath + "') o " +
                "JOIN read_csv_auto('" + path + "') e ON o.customer_id = e.id " +
                "GROUP BY e.name ORDER BY spent DESC";
            DuckDbUtils.queryAndPrint(conn, sql);

            // 4) 反向操作:把查询结果导出为 CSV
            DuckDbUtils.execute(conn,
                "COPY (" +
                "  SELECT department, ROUND(AVG(salary), 2) AS avg_salary " +
                "  FROM employees GROUP BY department ORDER BY avg_salary DESC" +
                ") TO 'target/dept_summary.csv' (HEADER, DELIMITER ',')");
            System.out.println("\nSQL> COPY (...) TO 'target/dept_summary.csv' (HEADER, DELIMITER ',')");

            File out = new File("target/dept_summary.csv");
            if (out.exists()) {
                System.out.println("\n[导出成功] " + out.getAbsolutePath() + " 内容:");
                Files.lines(out.toPath()).forEach(System.out::println);
            }
        }
    }
}
