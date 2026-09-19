package com.example.duckdb;

import java.sql.Connection;

/**
 * 演示 1:基础特性 — 内存数据库、DDL/DML、基础 SQL。
 *
 * DuckDB 是"分析型 SQLite":
 *  - 嵌入式,无独立服务进程
 *  - 默认内存模式,也可持久化到单文件(类似 SQLite)
 *  - 列式存储,但同时支持完整的 DDL/DML 与事务
 */
public class Demo01Basics {

    public static void run() throws Exception {
        System.out.println("\n========== Demo 1: 基础特性(内存数据库 + 标准 SQL) ==========");

        try (Connection conn = DuckDbUtils.openMemory()) {
            // ---- DDL:建表,支持丰富的数据类型 ----
            DuckDbUtils.execute(conn,
                "CREATE TABLE products (" +
                "    id          INTEGER PRIMARY KEY," +
                "    name        VARCHAR NOT NULL," +
                "    category    VARCHAR," +
                "    price       DECIMAL(10, 2)," +
                "    in_stock    BOOLEAN," +
                "    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

            // ---- DML:批量插入 ----
            DuckDbUtils.execute(conn,
                "INSERT INTO products (id, name, category, price, in_stock) VALUES " +
                "    (1, 'Laptop',   'Electronics', 999.99, TRUE)," +
                "    (2, 'Headset',  'Electronics',  79.50, TRUE)," +
                "    (3, 'Notebook', 'Stationery',    5.99, TRUE)," +
                "    (4, 'Pen Set',  'Stationery',   12.00, FALSE)," +
                "    (5, 'Mug',      'Kitchen',      15.00, TRUE)");

            // ---- 基础查询 ----
            DuckDbUtils.queryAndPrint(conn, "SELECT * FROM products ORDER BY id");
            DuckDbUtils.queryAndPrint(conn,
                "SELECT category, COUNT(*) AS cnt, AVG(price) AS avg_price " +
                "FROM products GROUP BY category ORDER BY avg_price DESC");

            // ---- UPDATE / DELETE ----
            DuckDbUtils.execute(conn, "UPDATE products SET price = price * 0.9 WHERE category = 'Electronics'");
            DuckDbUtils.queryAndPrint(conn, "SELECT name, price FROM products ORDER BY id");

            DuckDbUtils.execute(conn, "DELETE FROM products WHERE in_stock = FALSE");
            DuckDbUtils.queryAndPrint(conn, "SELECT COUNT(*) AS remaining FROM products");
        }
    }
}
