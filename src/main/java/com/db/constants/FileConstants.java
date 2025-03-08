package com.db.constants;

import java.io.File;

/**
 * 文件相关的常量
 * @author Roy
 * @date 2022/10/23 13:59
 */
public class FileConstants {
    /** 文件的根目录 */
    public static final String ROOT_PATH = System.getProperty("user.dir");
    /** 公共文件目录 */
    public static final String GLOBAL_DIR_PATH = ROOT_PATH + File.separator + "global";
    /** base数据所存储的目录 */
    public static final String BASE_DIR_PATH= ROOT_PATH + File.separator + "base";
    /** 数据库base文件名 */
    public static final String DB_BASE_FILE_NAME = "db.base";
    /** 表base文件名 */
    public static final String TABLE_BASE_FILE_NAME = "table.base";
    /** 默认的系统信息所在数据库名称 */
    public static final String GLOBAL_DB_NAME = "global_db";
    /** 表的global元数据信息所在文件名 */
    public static final String GLOBAL_TB_NAME = "global_tb.data";
    /** 索引的global元数据信息所在文件名 */
    public static final String GLOBAL_INDEX_NAME = "global_index.data";
    /** 索引的root文件夹 */
    public static final String INDEXES_DIR_NAME = "indexes";
    /** 主键索引默认文件夹名 */
    public static final String PK_DIR_NAME = "PK_INDEX";
    /** 索引默认base文件名 */
    public static final String INDEX_BASE_FILE_NAME = "index.base";
    /** 数据信息所在文件夹 */
    public static final String TABLE_DATA_DIR_NAME = "data";
    /** 表中数据存放文件后缀 */
    public static final String TABLE_DATA_FILE_EX = ".data";
}
