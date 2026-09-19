package com.example.duckdb;

import java.io.File;
import java.sql.Connection;

/**
 * 演示 4:Parquet / JSON / Arrow — DuckDB 原生读写现代数据格式。
 *
 *  - Parquet:列式磁盘格式,大数据生态的事实标准
 *  - JSON:支持 read_json_auto / json_extract 等
 *  - Arrow:Apache Arrow IPC 格式 — DuckDB 与 Python/R/Spark 等生态的通用"数据语言"
 *
 * 注:关于 Arrow — 本 demo 只演示 DuckDB 一侧的 read_arrow(),
 *   不包含 Arrow 文件生成。如需完整回路:
 *     1) 用 Python pyarrow / R arrow 生成 .arrow 文件
 *     2) 把文件放到 resources/data/sample.arrow
 *     3) 取消下方 readArrow() 的注释即可
 *   这样跨程序零拷贝消费的场景,在 Python 数据科学栈里是最常用的玩法。
 */
public class Demo04Formats {

    public static void run() throws Exception {
        System.out.println("\n========== Demo 4: Parquet / JSON / Arrow ==========");

        try (Connection conn = DuckDbUtils.openMemory()) {
            // 1) Parquet:落盘 + 重新读取(DuckDB 一行 SQL 即可)
            File parquet = new File("target/from_duckdb.parquet");
            parquet.getParentFile().mkdirs();
            String parquetPath = parquet.getAbsolutePath().replace('\\', '/');
            String copyParquet =
                "COPY ( " +
                "    SELECT i AS id, 'name_' || i AS name, (random() * 100)::INT AS score " +
                "    FROM range(1, 11) t(i)" +
                ") TO '" + parquetPath + "' (FORMAT PARQUET)";
            DuckDbUtils.execute(conn, copyParquet);

            System.out.println("\n[Parquet 已生成] " + parquet.getAbsolutePath() +
                    " (" + parquet.length() + " bytes)");

            DuckDbUtils.queryAndPrint(conn,
                "SELECT * FROM read_parquet('" + parquetPath + "') LIMIT 5");

            DuckDbUtils.queryAndPrint(conn,
                "SELECT file_name, num_values AS rows_in_chunk, row_group_id, row_group_num_rows " +
                "FROM parquet_metadata('" + parquetPath + "')");

            // 2) JSON:支持嵌套结构
            File json = new File("src/main/resources/data/events.json");
            if (json.exists()) {
                String path = json.getAbsolutePath().replace('\\', '/');
                DuckDbUtils.queryAndPrint(conn,
                    "SELECT * FROM read_json_auto('" + path + "') LIMIT 10");
            }

            DuckDbUtils.queryAndPrint(conn,
                "WITH t AS ( " +
                "  SELECT '{\"name\":\"Alice\",\"scores\":[90,85,78]}'::JSON AS j " +
                ") " +
                "SELECT " +
                "  json_extract_string(j, '$.name') AS name, " +
                "  json_extract(j, '$.scores') AS scores_json, " +
                "  CAST(json_extract(j, '$.scores[0]') AS INTEGER) AS first_score " +
                "FROM t");

            // 3) Arrow: 如果 resources/data/sample.arrow 存在,DuckDB 直接 read_arrow()
            File arrowFile = new File("src/main/resources/data/sample.arrow");
            if (arrowFile.exists()) {
                String arrowPath = arrowFile.getAbsolutePath().replace('\\', '/');
                System.out.println("\n[Arrow] 检测到 " + arrowPath);
                DuckDbUtils.queryAndPrint(conn,
                    "SELECT * FROM read_arrow('" + arrowPath + "') ORDER BY id");
            } else {
                System.out.println("\n[Arrow] 跳过:未发现 resources/data/sample.arrow");
                System.out.println("        若要演示 Arrow 互操作,可用以下方式生成文件后取消注释 Demo04 中的相关代码:");
                System.out.println("        Python: import pyarrow as pa; pa.table({...}).to_file('sample.arrow')");
                System.out.println("        或:        R: arrow::write_arrow(tibble::tibble(...), 'sample.arrow')");
            }
        }
    }
}
