package com.example.duckdb;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * 演示 5:持久化数据库 + 远程/HTTP 文件直接查询。
 *
 *  - 持久化:URL 传文件路径,SQLite 风格单文件
 *  - 通过 httpfs 扩展直接读 S3 / HTTP 上的 Parquet(无需下载)
 *  - glob 模式直接读整个目录的 Parquet
 */
public class Demo05PersistenceAndRemote {

    public static void run() throws Exception {
        System.out.println("\n========== Demo 5: 持久化 + 远程/HTTP 文件 ==========");

        // ---- 持久化演示:写入文件 -> 重新打开 -> 数据还在 ----
        File dbFile = new File("target/persistent.duckdb");
        if (dbFile.exists()) dbFile.delete();
        dbFile.getParentFile().mkdirs();

        try (Connection conn = DuckDbUtils.openFile(dbFile.getAbsolutePath())) {
            DuckDbUtils.execute(conn,
                "CREATE TABLE kv (k VARCHAR PRIMARY KEY, v VARCHAR)");
            DuckDbUtils.execute(conn,
                "INSERT INTO kv VALUES ('hello', 'world'), ('duckdb', 'fast')");
        }
        System.out.println("[持久化] 已写入 " + dbFile.getAbsolutePath() +
                " (" + dbFile.length() + " bytes)");

        // 重新打开,数据仍在
        try (Connection conn = DuckDbUtils.openFile(dbFile.getAbsolutePath())) {
            DuckDbUtils.queryAndPrint(conn, "SELECT * FROM kv ORDER BY k");
        }

        // ---- 安装并使用 httpfs 扩展 ----
        try (Connection conn = DuckDbUtils.openMemory();
             Statement st = conn.createStatement()) {

            st.execute("INSTALL httpfs");
            st.execute("LOAD httpfs");

            // 直接查询 GitHub 上的公开 Parquet 文件(IEEE 论文常用数据集)
            // 这是 DuckDB 非常酷的能力:无需下载文件,SQL 即查询远端
            try {
                DuckDbUtils.queryAndPrint(conn,
                    "SELECT \"User ID\", \"Country\", \"Age\", \"Gender\" " +
                    "FROM read_parquet(" +
                    "  'https://raw.githubusercontent.com/duckdb/duckdb/main/data/parquet-testing/userdata1.parquet'" +
                    ") " +
                    "LIMIT 5");
            } catch (Exception e) {
                System.out.println("[远程查询失败,通常是因为网络问题] " + e.getMessage());
            }
        }

        // ---- glob:把整个目录当一张表 ----
        // 我们把之前 Demo04 生成的 parquet + 再多复制几份,演示 glob
        File dataDir = new File("target/glob_demo");
        deleteRecursively(dataDir);
        dataDir.mkdirs();

        File src = new File("target/from_duckdb.parquet");
        if (src.exists()) {
            for (int i = 0; i < 3; i++) {
                Files.copy(src.toPath(),
                    new File(dataDir, "part_" + i + ".parquet").toPath());
            }
            try (Connection conn = DuckDbUtils.openMemory()) {
                String pattern = dataDir.getAbsolutePath().replace('\\', '/') + "/*.parquet";
                DuckDbUtils.queryAndPrint(conn,
                    "SELECT name AS file_name, COUNT(*) AS rows " +
                    "FROM read_parquet('" + pattern + "') " +
                    "GROUP BY name ORDER BY name");
            }
        } else {
            System.out.println("[跳过 glob] 请先运行 Demo04 生成 target/from_duckdb.parquet");
        }
    }

    private static void deleteRecursively(File f) throws Exception {
        if (!f.exists()) return;
        try (Stream<Path> stream = Files.walk(f.toPath())) {
            stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
