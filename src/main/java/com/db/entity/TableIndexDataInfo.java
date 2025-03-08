package com.db.entity;

import java.util.Map;

/**
 * 表内索引信息
 * @author Roy
 * @date 2022/11/17 23:51
 */
public class TableIndexDataInfo {
    private BPlusTree bPlusTree;

    private int sockedByteLength;

    private int indexDataFileId;

    private boolean dirty;

    private int[] columnIds;

    private boolean isPrimaryKey;
    /** key nodeKey,value:leafNode */
    private Map<String,BPlusTreeLeafNode> pkValue;

    public BPlusTree getbPlusTree() {
        return bPlusTree;
    }

    public void setbPlusTree(BPlusTree bPlusTree) {
        this.bPlusTree = bPlusTree;
    }

    public int getSockedByteLength() {
        return sockedByteLength;
    }

    public void setSockedByteLength(int sockedByteLength) {
        this.sockedByteLength = sockedByteLength;
    }

    public int getIndexDataFileId() {
        return indexDataFileId;
    }

    public void setIndexDataFileId(int indexDataFileId) {
        this.indexDataFileId = indexDataFileId;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public int[] getColumnIds() {
        return columnIds;
    }

    public void setColumnIds(int[] columnIds) {
        this.columnIds = columnIds;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }

    public Map<String, BPlusTreeLeafNode> getPkValue() {
        return pkValue;
    }

    public void setPkValue(Map<String, BPlusTreeLeafNode> pkValue) {
        this.pkValue = pkValue;
    }
}
