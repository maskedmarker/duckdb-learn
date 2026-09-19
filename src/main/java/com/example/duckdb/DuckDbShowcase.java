package com.example.duckdb;

/**
 * 总入口:按顺序依次执行所有 demo。
 *
 * 运行方式(在项目根目录):
 *   mvn compile exec:java
 *
 * 或用 mvn 直接跑单个 demo:
 *   mvn exec:java -Dexec.mainClass=com.example.duckdb.Demo02Csv
 */
public class DuckDbShowcase {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();

        Demo01Basics.run();
        Demo02Csv.run();
        Demo03Analytics.run();
        Demo04Formats.run();
        Demo05PersistenceAndRemote.run();

        System.out.printf("%n========== 全部 demo 完成,耗时 %d ms ==========%n",
                System.currentTimeMillis() - start);
    }
}
