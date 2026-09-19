# DuckDB 学习项目(DuckDB Learn)

通过 5 个独立 demo,从零上手 **DuckDB** —— 一种进程内、列式的"分析型 SQLite"。

## 环境

- JDK 1.8+
- Maven 3.5+
- 已测试:`Apache Maven 3.5.2 + Temurin JDK 1.8.0_502 + Windows 11`

## 快速开始

```bash
# 编译
mvn compile

# 运行全部 demo
mvn exec:java

# 运行单个 demo(可选)
mvn exec:java -Dexec.mainClass=com.example.duckdb.Demo02Csv
mvn exec:java -Dexec.mainClass=com.example.duckdb.Demo03Analytics
```

完整运行输出参见 `target/run.log`,里面包含每个 demo 的 SQL 与结果。

## 项目结构

```
.
├── pom.xml                                 # 仅一个依赖:duckdb_jdbc 0.9.2 + slf4j-simple
└── src/main/
    ├── java/com/example/duckdb/
    │   ├── DuckDbUtils.java                # 共用工具:打开连接 + 格式化打印
    │   ├── DuckDbShowcase.java             # 入口,顺序执行全部 demo
    │   ├── Demo01Basics.java               # 1. 基础特性
    │   ├── Demo02Csv.java                  # 2. CSV 直接查询
    │   ├── Demo03Analytics.java            # 3. 分析查询(聚合 / 窗口函数 / LIST / PIVOT)
    │   ├── Demo04Formats.java              # 4. Parquet / JSON / Arrow
    │   └── Demo05PersistenceAndRemote.java # 5. 持久化 + httpfs + glob
    └── resources/data/
        ├── employees.csv
        ├── orders.csv
        └── events.json
```

## DuckDB 主要特性一览

### Demo 1 — 基础特性

- **进程内嵌入式**:无服务进程,JDBC URL 直接连接(`jdbc:duckdb:` 即内存数据库,`jdbc:duckdb:path.db` 即持久化单文件)
- **标准 SQL**:支持完整 DDL/DML/事务/类型丰富
- **使用体验**:与 SQLite 类似,但面向 **OLAP**(分析)而不是 OLTP(事务)

### Demo 2 — CSV / 文件直查

- `read_csv_auto('xxx.csv')` 直接读 CSV,**无需建表**
- 多 CSV 之间的 `JOIN` 也是一行 SQL
- `COPY (...) TO 'xxx.csv'` 反向导出
- 整个体验类似 Pandas 的 `read_csv`,但用 SQL 表达

### Demo 3 — 分析查询

- **列式存储 + 向量化执行**:20 万行聚合亚秒级
- **窗口函数**(WINDOW OVER ...):累计、求 Top-N、排名
- **CTE / WITH** + **PIVOT / UNPIVOT**
- **复合类型**:LIST / STRUCT / MAP,以及 `LIST_AGG`、`LIST_SORT`、`LIST_AVG` 等聚合

### Demo 4 — 多种数据格式

| 格式      | 读                          | 写                                |
|-----------|------------------------------|-------------------------------------|
| CSV       | `read_csv_auto()`            | `COPY ... TO 'x.csv'`               |
| Parquet   | `read_parquet()`             | `COPY ... TO 'x.parquet' (FORMAT PARQUET)` |
| JSON      | `read_json_auto()`           | `COPY ... TO 'x.json'`              |
| Arrow     | `read_arrow()`               | (需更高版本内置或用 Arrow SDK 写)   |

- Parquet 自带统计信息,可直接 `SELECT ... FROM parquet_metadata(...)` 查询
- JSON 支持嵌套结构 + `json_extract` / `json_extract_string` 路径表达式

### Demo 5 — 持久化 & 远端数据

- **持久化**:`jdbc:duckdb:path.db` 把整个数据库存成单文件(类似 SQLite)
- **httpfs 扩展**:`INSTALL httpfs; LOAD httpfs;` 然后直接 `SELECT FROM read_parquet('https://...')`,无需下载文件
- **glob**:用通配符一次读整个目录的 Parquet/CSV,常用作 Data Lake 入口

## 你应该从这里学到的

| 场景                      | 用 DuckDB 的理由                                                    |
|---------------------------|----------------------------------------------------------------------|
| 在 JVM 应用里做本地分析    | 比嵌入 SQLite/HSQLDB 性能高一个数量级,而且 SQL 更"现代"             |
| 处理 Parquet/CSV/JSON     | 不用先建表、不用 ETL,**SQL 即 ETL**                                 |
| 与 Pandas/Polars 配合     | DuckDB 可 0 拷贝消费 Arrow,与 Python 数据栈天然打通                 |
| 数据科学 / ETL 探索       | 直接对一个文件夹的 Parquet 做 glob 聚合                             |

## 一些坑(我替你踩过了)

1. **Java 8 不支持 text block**:`"""..."""` 是 Java 13+ 的语法,本项目所有 demo 都用 `+` 拼接保持 Java 8 兼容。
2. **DuckDB 版本特性差异**:
   - `parquet_metadata()` 0.9.2 字段是 `num_values`,不是 `num_rows`
   - `read_parquet()` 0.9.2 的虚拟列是 `name`,不是 `file_name`
   - `COPY ... TO ... (FORMAT ARROW)` 0.9.2 没有内置,需 Arrow SDK 或升级 DuckDB
3. **JDBC URL 写法**:空字符串 `jdbc:duckdb:` 是内存模式,带路径才是持久化模式。
4. **PowerShell 下的 Maven**:命令行参数里不要混 `-D` 和非 `-D`,会被当成 phase。

## 参考资料

- 官方文档:<https://duckdb.org/docs/>
- GitHub:<https://github.com/duckdb/duckdb>
- JDBC 用法:<https://duckdb.org/docs/api/java.html>
