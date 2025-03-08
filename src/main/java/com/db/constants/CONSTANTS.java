package com.db.constants;

/**
 * 常量信息
 * @author Roy
 * @date 2022/10/19 20:42
 */
public class CONSTANTS {
    /** 索引 */
    public static final String INDEX = "IN";
    /** 数据库 */
    public static final String DATABASE = "DB";
    /** 表 */
    public static final String TABLE = "TB";
    /** 表中数据 */
    public static final String DATA = "DT";
    /** 文件 */
    public static final String FILE = "FL";
    /**  查询数据 */
    public static final String SEARCH_DATA = "DT";
    /** 一个页面的字节数 */
    public static final int PAGE_SIZE = 8192;
    /** 缓存中页面个数上限 */
    public static final int CAP = 60000;
    /** 一个表中列的上限 */
    public static final int MAX_COL_NUM = 31;
    /** 数据库中表的个数上限 */
    public static final int MAX_TB_NUM = 31;
    public static final int RELEASE = 1;
    /** 一个文件的最大容量设置为5M */
    public static final int MAX_FILE_SIZE = 5*1024*1024;
    /** 整形 */
    public static final String INT = "INT";
    /** 定长字符串 */
    public static final String VARCHAR = "VARCHAR";
    /** 浮点形 */
    public static final String FLOAT = "FLOAT";
    /** "NULL" */
    public static final String NULL = "NULL";
    /** 真 */
    public static final int TRUE = 1;
    /** 假 */
    public static final int FALSE = 0;
    /** = */
    public static final String EqualOrAssign = "=";
    /** 查询 */
    public static final String SELECT = "select";
    /** 删除 */
    public static final String DELETE = "delete";
    /** 修改 */
    public static final String UPDATE = "update";
    /** 结束的标志 */
    public static final String EXIT_FLAG = "exit";
}
