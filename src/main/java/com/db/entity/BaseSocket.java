package com.db.entity;

import com.db.utils.ByteArrayUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 页面数据区:一个socket对应一条数据
 * @author Roy
 * @date 2022/10/19 22:19
 */
public class BaseSocket implements Serializable {
    /** 对应的数据信息:type 14,length 4,isVariable 1,realLength 4，value(组合)*/
    private List<byte[]> data;

    /** 序列化之后的数据 */
    private List<Object[]> objectData;

    public byte[] getSocketData() {
        byte[] newData = null;
        for (int i = 0; i < objectData.size(); i++) {
            byte[] columnData = data.get(i);
            int length = ByteArrayUtil.toInt(ByteArrayUtil.subByte(columnData,14,4));
            String type = new String(ByteArrayUtil.subByte(columnData,0,14));
            byte[] value = ByteArrayUtil.objectToBytes(objectData.get(i)[4],type);
            int valueLength = value==null?0:value.length;
            byte[] completeValue = new byte[length-valueLength];
            byte[] realLength = ByteArrayUtil.intToByte(value==null?-1:valueLength);
            //包含了字段类型14，字段长度4，是否变长1
            byte[] oriPartByte = ByteArrayUtil.subByte(columnData,0,19);
            newData = ByteArrayUtil.byteMergerAll(newData,oriPartByte,realLength,value,completeValue);
        }
        return newData;
    }

    public List<byte[]> getData() {
        return data;
    }

    /**
     * 存入的信息为file中记录的byte[]，每一列一个byte[]
     * @param data
     */
    public void setData(List<byte[]> data) {
        this.data = data;
    }

    public List<Object[]> getObjectData() {
        if (objectData == null) {
            objectData = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                Object[] baseData = new Object[4];
                byte[] bytes = data.get(i);
                int realLength = ByteArrayUtil.toInt(ByteArrayUtil.subByte(bytes,19,4));
                if (realLength != -1) {
                    byte[] typeByte = ByteArrayUtil.subByte(bytes,0,14);
                    String type = new String(typeByte);
                    byte[] value = ByteArrayUtil.subByte(bytes,23,realLength);
                    baseData[i] = ByteArrayUtil.byteToObject(value,type);
                } else {
                    baseData[i] = null;
                }
                objectData.add(baseData);
            }
        }
        return objectData;
    }

    /**
     * 只取value部分
     * @return
     */
    public Object[] getObjectValue() {
        Object[] valueData = new Object[data.size()];
        if (objectData == null) {
            getObjectData();
        }
        for (int i = 0; i < objectData.size(); i++) {
            Object[] baseData = objectData.get(i);
            valueData[i] = baseData[4];
        }
        return valueData;
    }

    public void setObjectData(List<Object[]> objectData) {
        this.objectData = objectData;
    }
}
