package com.db.utils;

import com.db.constants.CONSTANTS;

/**
 * byte[]处理的工具类
 * @author Roy
 * @date 2022/10/23 9:55
 */
public class ByteArrayUtil {
    /**
     * byte[]数组合并
     * @param values 需要合并的byte[]
     * @return 合并之后的byte[]
     */
    public static byte[] byteMergerAll(byte[]... values) {
        int lengthByte = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                lengthByte += values[i].length;
            }
        }
        byte[] allByte = new byte[lengthByte];
        int countLength = 0;
        for (int i = 0; i < values.length; i++) {
            byte[] b = values[i];
            if (b != null) {
                System.arraycopy(b, 0, allByte, countLength, b.length);
                countLength += b.length;
            }
        }
        return allByte;
    }
    /**
     * 截取byte数组   不改变原数组
     * @param b 原数组
     * @param off 偏差值（索引）
     * @param length 长度
     * @return 截取后的数组
     */
    public static byte[] subByte(byte[] b,int off,int length){
        if (length==0) {
            return new byte[0];
        }
        if (length == b.length) {
            return b;
        }
        byte[] b1 = new byte[length];
        System.arraycopy(b, off, b1, 0, length);
        return b1;
    }

    /**
     * 将byte[]转换为对象
     * @param bytes
     * @return
     */
    public static Object byteToObject(byte[] bytes,String type) {
        if (CONSTANTS.VARCHAR.equals(type)) {
            return new String(bytes);
        } else if (CONSTANTS.INT.equals(type)) {
            return toInt(bytes);
        } else if (CONSTANTS.FLOAT.equals(type)) {
            return byteToFloat(bytes);
        }
        return null;
    }

    public static byte[] floatToByte(float data) {
        // 把float转换为byte[]
        int fbit = Float.floatToIntBits(data);
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) (fbit >> (24 - i * 8));
        }
        return bytes;
    }

    /**
     * 将对象转换成byte数组集合
     * @return
     */
    public static byte[] objectToBytes(Object data,String type) {
        if (data == null) {
            return null;
        }
        if (strEquals(CONSTANTS.VARCHAR,type)) {
            return ((String) data).getBytes();
        } else if (strEquals(CONSTANTS.FLOAT,type)) {
            return floatToByte((float)data);
        } else if (strEquals(CONSTANTS.INT,type)) {
            return intToByte((int)data);
        }
        return null;
    }

    /**
     * 根据类型判断两个值是否相等
     * @param originData 第一个数据
     * @param equaledValue 第二个数据
     * @param type 数据类型
     * @return 判断的结果
     */
    private static boolean isEqualed(Object originData,Object equaledValue,String type) {
        if (strEquals(CONSTANTS.INT,type)) {
            int origin = (int) originData;
            return origin == (int)equaledValue;
        } else if (strEquals(CONSTANTS.VARCHAR,type)) {
            return originData.equals(equaledValue);
        } else if (strEquals(CONSTANTS.FLOAT,type)) {
            float origin = (float) originData;
            return origin == (float)equaledValue;
        }
        return false;
    }

    /**
     * 将文件中记录的元数据与所输入的数据对比
     * @param originData 文件中记录的元数据
     * @param equalValue 未添加补全位的数据
     * @return 比较的结果
     */
    public static boolean isEqualed(Object[] originData,Object equalValue) {
        String type = (String) originData[0];
        Object splitData = originData[4];
        if (strEquals(type,CONSTANTS.VARCHAR)) {
            byte[] bytes = objectToBytes(splitData, type);
            int realLength = (int) originData[3];
            byte[] newByte = subByte(bytes, 0, realLength);
            splitData = new String(newByte);
        }
        return isEqualed(splitData,equalValue,type);
    }

    public static boolean isEqualed(Object[] originData,Object[] equaledValue) {
        String type = (String) equaledValue[0];
        Object originObj = originData[4];
        Object equaledObj = equaledValue[4];
        if (strEquals(type,CONSTANTS.VARCHAR)) {
            int originRealLength = (int) originData[3];
            int equaledRealLength = (int) equaledValue[3];
            originObj = new String(subByte(objectToBytes(originObj,type),0,originRealLength));
            equaledObj = new String(subByte(objectToBytes(equaledObj,type),0,equaledRealLength));
        }
        return isEqualed(originObj,equaledObj,type);
    }

    /**
     * 解决因为字符串补全，导致equal方法执行结果非预期的问题
     * @param str1
     * @param str2
     * @return
     */
    public static boolean strEquals(String str1,String str2) {
        byte[] byte1 = str1.getBytes();
        byte[] byte2 = str2.getBytes();
        if (byte1.length==byte2.length) {
            return str1.equals(str2);
        } else {
            if (byte1.length > byte2.length) {
                byte[] tempByte = subByte(byte1,0,byte1.length);
                return str2.equals(new String(tempByte));
            } else {
                byte[] tempByte = subByte(byte2,0,byte1.length);
                return str1.equals(new String(tempByte));
            }
        }
    }

    /**
     * 将byte数组bRefArr转为一个整数,字节数组的低位是整型的低字节位
     * @param bRefArr
     * @return
     */
    public static int toInt(byte[] bRefArr) {
        int res = 0;
        for(int i=0;i<bRefArr.length;i++){
            res += (bRefArr[i] & 0xff) << ((3-i)*8);
        }
        return res;
    }

    /**
     * 将int转换为byte[]
     * @param num 数字
     * @return 转换之后的byte[]
     */
    public static byte[] intToByte(int num) {
        byte[] b = new byte[4];
        b[3] = (byte) (num & 0xff);
        b[2] = (byte) (num >> 8 & 0xff);
        b[1] = (byte) (num >> 16 & 0xff);
        b[0] = (byte) (num >> 24 & 0xff);
        return b;
    }

    /**
     * 字节数组转 float，小端
     */
    public static float byteToFloat(byte[] array) {
        // 数组长度有误
        if (array.length != 4) {
            return 0;
        }
        return Float.intBitsToFloat(toInt(array));
    }

    /**
     * 获取字符串补全后的byte[],总长度为100
     * @param name 字符串value
     * @return 补全后的byte[]
     */
    public static byte[] getStrCompleteByte(String name,int length) {
        byte[] strByte = name.getBytes();
        byte[] completeByte = new byte[length-strByte.length];
        return ByteArrayUtil.byteMergerAll(strByte,completeByte);
    }
}
