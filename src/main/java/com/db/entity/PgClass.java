package com.db.entity;

import com.db.constants.CONSTANTS;

import java.io.Serializable;
import java.util.List;

/**
 * 包括索引、序列、复合类型和一些特殊关系类型
 * (1)表空间和索引空间暂时不指定
 * (2)布尔标识是懒于维护的，当为false的时候不存储相应属性
 * @author Roy
 * @date 2022/10/19 20:43
 */
public class PgClass implements Serializable {
    /** 表、索引名字 */
    private String relName;
    /** 包含这个关系的名字空间(模式)的 OID */
    private String relId;
    /** 如果有，则为对应这个表的行类型的数据类型的OID(索引为零，它们没有pg_type记录) */
    private String relType;
    /** 如果该表在整个集群中由所有数据库共享则为真（即是否是系统表） */
    private int isShared = CONSTANTS.FALSE;
    /** 如果它是表中的字段而且至少有(或者最近有过)一个索引，则为真。 */
    private int isIndex = CONSTANTS.FALSE;
    /**
     * r = ordinary table（普通表）,
     * i = index（索引）,
     * S = sequence（序列）,
     * c = composite type（复合类型）,
     * t = TOAST table（TOAST 表）,
     * f = foreign table（外部表） */
    private char kind;
    /** 如果这个字段是主键，则为真 */
    private int isPKey = CONSTANTS.FALSE;
    /** 是否可以为NUll "0":不能为null "1":"可以为null*/
    private int ableNull = CONSTANTS.TRUE;
    /** 默认值 */
    private String defValue;
    /** 是否作为外键 */
    private int isFKey = CONSTANTS.FALSE;
    /** 外键关联表 */
    private String relTbName;
    /** 外键关联字段 */
    private List<String> relFKFields;
    /** 主键关联字段 */
    private List<String> pKeyFields;
    /** 外键联合字段 */
    private List<String> fKeyFields;
    /** 字段长度 */
    private Integer length;

    public String getRelName() {
        return relName;
    }

    public void setRelName(String relName) {
        this.relName = relName;
    }

    public String getRelType() {
        return relType;
    }

    public void setRelType(String relType) {
        this.relType = relType;
        if (CONSTANTS.INT.equals(relType)) {
            length = 4;
        } else if (CONSTANTS.FLOAT.equals(relType)) {
            length = 4;
        }
    }

    public Character getKind() {
        return kind;
    }

    public void setKind(Character kind) {
        this.kind = kind;
    }

    public Object getDefValue() {
        return defValue;
    }

    public String getRelId() {
        return relId;
    }

    public void setRelId(String relId) {
        this.relId = relId;
    }

    public void setKind(char kind) {
        this.kind = kind;
    }

    public void setDefValue(String defValue) {
        this.defValue = defValue;
    }

    public String getRelTbName() {
        return relTbName;
    }

    public void setRelTbName(String relTbName) {
        this.relTbName = relTbName;
    }

    public List<String> getfKeyFields() {
        return fKeyFields;
    }

    public void setfKeyFields(List<String> fKeyFields) {
        this.fKeyFields = fKeyFields;
    }

    public List<String> getpKeyFields() {
        return pKeyFields;
    }

    public void setpKeyFields(List<String> pKeyFields) {
        this.pKeyFields = pKeyFields;
    }

    public List<String> getRelFKFields() {
        return relFKFields;
    }

    public void setRelFKFields(List<String> relFKFields) {
        this.relFKFields = relFKFields;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public int getIsShared() {
        return isShared;
    }

    public void setIsShared(int isShared) {
        this.isShared = isShared;
    }

    public int getIsIndex() {
        return isIndex;
    }

    public void setIsIndex(int isIndex) {
        this.isIndex = isIndex;
    }

    public int getIsPKey() {
        return isPKey;
    }

    public void setIsPKey(int isPKey) {
        this.isPKey = isPKey;
    }

    public int getAbleNull() {
        return ableNull;
    }

    public void setAbleNull(int ableNull) {
        this.ableNull = ableNull;
    }

    public int getIsFKey() {
        return isFKey;
    }

    public void setIsFKey(int isFKey) {
        this.isFKey = isFKey;
    }
}
