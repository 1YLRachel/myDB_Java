package com.db.entity;

/**
 * 存储可用的数据库信息
 * @author Roy
 * @date 2022/10/19 20:40
 */
public class PgDatabase {
    /** 序号 */
    private Integer id;
    /** 数据库名字 */
    private String db_name;
    /** 数据库编码格式 */
    private String encoding = "UTF-8";

    public String getDbName() {
        return db_name;
    }

    public void setDbName(String dbName) {
        this.db_name = dbName;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
