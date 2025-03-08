package com.db.utils;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 页相关的方法
 * @author Roy
 * @date 2022/10/19 22:26
 */
public class PageUtil {
    private static final Logger logger = LoggerFactory.getLogger(PageUtil.class);
    /**
     * 将byte[]进行反序列化
     * todo 待优化
     * @param clazz 类型
     * @param data 需要进行序列化的byte数组
     * @return 序列化之后的对象
     */
    public static <T> T deserialize(Class<T> clazz, byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)){
            // 反序列化
            Object object = ois.readObject();
            if (clazz.isInstance(object)) {
                return clazz.cast(object);
            } else {
                System.out.println("指定数据类型不正确！"+object.toString());
                return null;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("数组解析失败");
            return null;
        }
    }

    /**
     * 将对象转换成byte数组
     * todo 待优化
     * @param data 数据对象
     * @return 二进制数组
     */
    public static <T> byte[] serialize(T data) {
        try (ByteArrayOutputStream baos=new ByteArrayOutputStream();
             ObjectOutputStream oos=new ObjectOutputStream(baos)){
            oos.writeObject(data);
            oos.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            System.out.println("反序列化失败！");
            return new byte[0];
        }
    }
}
