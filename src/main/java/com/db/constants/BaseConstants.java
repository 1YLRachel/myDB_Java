package com.db.constants;

/**
 * 基本信息的常量数据
 * @author Roy
 * @date 2022/11/9 21:24
 */
public class BaseConstants {
    /**-------------表基本信息------------------*/
    /** 表基本信息所存储的列名 */
    public static String[] TABLE_COLUMNS = new String[]{"id","name","type","length","ableNull","defaultValue","isPkey","isIndex","isFKey","fKTb","fKColumnName"};
    /** 各字段类型 */
    public static String[] TABLE_COLUMN_TYPES = new String[]{CONSTANTS.INT,
            CONSTANTS.VARCHAR,CONSTANTS.VARCHAR,CONSTANTS.INT,CONSTANTS.INT,
            CONSTANTS.VARCHAR,CONSTANTS.INT,CONSTANTS.INT,CONSTANTS.INT,
            CONSTANTS.VARCHAR,CONSTANTS.VARCHAR};
    /** 表基本信息每个字段的长度(单位：字节) */
    public static int[] COLUMN_LENGTH = new int[]{4,100,14,4,4,100,4,4,4,100,100};
    /** 表基本信息是否变长 */
    public static byte[] VARIABLE_COLUMN_FLAG = new byte[]{0,1,1,0,0,1,0,0,0,1,1};

    /**---------索引的基本信息-------------*/
    /** 索引基本信息所存储的列名 */
    public static String[] INDEX_COLUMNS = new String[]{"id","name","columnId","isPkey","isIndex","isComposite","isFkey","relTbId"};
    /** 各字段类型 */
    public static String[] INDEX_COLUMN_TYPES = new String[]{CONSTANTS.INT,
            CONSTANTS.VARCHAR,CONSTANTS.INT,CONSTANTS.INT,CONSTANTS.INT,
            CONSTANTS.INT,CONSTANTS.INT,CONSTANTS.INT};
    /** 索引基本信息每个字段的长度（单位字节） */
    public static int[] INDEX_COLUMN_LENGTH = new int[]{4,100,4,4,4,4,4,4};
    /** 索引的基本信息是否变长 */
    public static byte[] VARIABLE_INDEX_FLAG = new byte[]{0,1,0,0,0,0,0,0};

}
