package com.db.entity;

import com.db.constants.CONSTANTS;
import com.db.utils.ByteArrayUtil;
import sun.util.resources.cldr.ka.LocaleNames_ka;

import java.util.List;

/**
 * B+树的叶子结点
 * @author Roy
 * @date 2022/11/17 21:19
 */
public class BPlusTreeLeafNode implements Comparable{
    /** 索引字段id(即字段下标) */
    private int[] indexColumnIds;
    /** 一个socket信息 */
    private List<Object[]> objectDataList;
    /** 一个socket的byte[]信息 */
    private List<byte[]> byteData;


    @Override
    public int compareTo(Object o) {
        if (o instanceof BPlusTreeLeafNode) {
            BPlusTreeLeafNode node = (BPlusTreeLeafNode) o;
            List<Object[]> objectDataList = node.getObjectDataList();
            for (int indexColumnId : indexColumnIds) {
                Object[] data = this.objectDataList.get(indexColumnId);
                Object[] otherData = objectDataList.get(indexColumnId);
                if (data[4] instanceof String) {
                    //字符串类型比较
                    int i = ((String) data[4]).compareTo((String) otherData[4]);
                    if (i!=0) {
                        return i;
                    }
                } else if (ByteArrayUtil.strEquals((String) data[0], CONSTANTS.FLOAT)){
                    float compareValue = (float)data[4] - (float)otherData[4];
                    if (compareValue != 0) {
                        return (int)compareValue;
                    }
                } else {
                    int compareValue = (int)data[4] - (int)otherData[4];
                    if (compareValue != 0) {
                        return compareValue;
                    }
                }
            }
        }
        return 0;
    }

    public BPlusTreeLeafNode(int[] indexColumnIds, List<Object[]> objectDataList) {
        this.indexColumnIds = indexColumnIds;
        this.objectDataList = objectDataList;
        for (Object[] objects : objectDataList) {
            byte[] columnByte = null;
            byte[] typeByte = ((String)objects[0]).getBytes();
            int length = (int) objects[1];
            byte[] lengthByte = ByteArrayUtil.intToByte(length);
            byte[] isVariableByte = (byte[]) objects[2];
            byte[] realLength = ByteArrayUtil.intToByte((int)objects[3]);
            byte[] valueByte = ByteArrayUtil.objectToBytes(objects[4], (String) objects[0]);
            byte[] completeByte = new byte[length-valueByte.length];
            columnByte = ByteArrayUtil.byteMergerAll(typeByte,lengthByte,isVariableByte,realLength,valueByte,completeByte);
            byteData.add(columnByte);
        }
    }

    public BPlusTreeLeafNode(int[] indexColumnIds, List<Object[]> objectDataList, List<byte[]> byteData) {
        this.indexColumnIds = indexColumnIds;
        this.objectDataList = objectDataList;
        this.byteData = byteData;
    }

    public int[] getIndexColumnIds() {
        return indexColumnIds;
    }

    public void setIndexColumnIds(int[] indexColumnIds) {
        this.indexColumnIds = indexColumnIds;
    }

    public List<Object[]> getObjectDataList() {
        return objectDataList;
    }

    public void setObjectDataList(List<Object[]> objectDataList) {
        this.objectDataList = objectDataList;
    }

    public List<byte[]> getByteData() {
        return byteData;
    }

    public void setByteData(List<byte[]> byteData) {
        this.byteData = byteData;
    }
}
