package com.db;

import com.db.constants.CONSTANTS;
import com.db.utils.ByteArrayUtil;
import com.db.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件管理器
 * @author Roy
 * @date 2022/11/6 17:58
 */
public class FileManager {
    private static final Logger logger = LoggerFactory.getLogger(FileManager.class);
    /**
     * 读取指定的二进制文件
     *
     * @param filePath 文件的完全限定名
     * @return 文件中的数据
     */
    public static byte[] readFile(String filePath) {
        File file = new File(filePath);
        if (!file.isFile()) {
            return null;
        }
        if (file.length()==0) {
            return null;
        }
        try (InputStream in = new FileInputStream(filePath);
             ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            byte[] buf = new byte[(int) file.length()];
            int length = 0;
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
            }
            return out.toByteArray();
        } catch (Exception e) {
            System.out.println("读取" + filePath + "失败！");
        }
        return new byte[]{};
    }

    /**
     * 删除文件
     * @return
     */
    public static boolean removeFile(String filePath){
        File file = new File(filePath);
        if (file.isFile()) {
            return file.delete();
        }
        System.out.println("文件不存在!");
        return false;
    }

    /**
     * 将byte[]添加到文件内
     * 注：判断文件是否满了
     * @return
     */
    public static void insertDataToFile(List data, String filePath) {
        if (CollectionUtils.isEmpty(data)) {
            return;
        }
        String newFilePath = FileUtil.getMaxSameNamePath(new File(filePath));
//        List fileDataList = readFileToList(newFilePath);
//        if (fileDataList == null) {
//            fileDataList = new ArrayList();
//        }
//        fileDataList.addAll(data);
//        List<byte[]> newData = ByteArrayUtil.changeListToByte(fileDataList);
//        writeToFile(newFilePath,newData);
    }


    /**
     * 将索引信息写入文件
     * todo 结果数据量过大的问题
     * @param tree 存储索引信息的树
     * @param filePath 存储信息的文件路径
     * @return 数据写入结果
     */
//    public static boolean writeBPlusTreeToFile(BPlusTree tree, String filePath) {
//        byte[] bytes = ByteArrayUtil.objectToBytes(tree);
//        if (bytes != null) {
//            //todo 需检查数据大小
//            return writeBytesToFile(bytes,new File(filePath));
//        }
//        return true;
//    }

    /**
     * 将二进制数据写入文件
     * 当文件超长时，会自动创建新的文件，将页数据写入新文件内
     * @return 写入结果
     */
    public static boolean writeToFile(String filePath, List<byte[]> data) {
        File file = new File(filePath);
        boolean fileFlag = true;
        if (!file.isFile()) {
            fileFlag = FileUtil.createFile(filePath);
        }
        //需要写入的数据为空
        if (CollectionUtils.isEmpty(data)) {
            return true;
        }
        if (fileFlag) {
            byte[] fileBytes = new byte[]{};
            //key:文件路径 value：数据
            Map<String,byte[]> fileDataMap = new HashMap<>();
            boolean isNeedCreateNew = false;
            for (byte[] bytes : data) {
                //将数据插入当前文件
                if (fileBytes.length +bytes.length <= CONSTANTS.MAX_FILE_SIZE) {
                    fileBytes = ByteArrayUtil.byteMergerAll(fileBytes,bytes);
                } else {
                    //超过文件最大值，切换文件
                    fileDataMap.put(filePath, fileBytes);
                    writeBytesToFile(fileBytes, file);
                    //数据重置
                    fileBytes = new byte[]{};
                    filePath = FileUtil.generateNewFilePath(file);
                    file = new File(filePath);
                    isNeedCreateNew = true;
                }
            }
            if (!isNeedCreateNew) {
                writeBytesToFile(fileBytes,file);
            }
            return true;
        }
        return false;
    }

    /**
     * 将二进制数据写入文件
     * @param data 二进制数据
     * @param file 文件对象
     */
    public static boolean writeBytesToFile(byte[] data,File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fos.flush();
            return true;
        } catch (Exception e) {
            System.out.println("文件写入失败！请检查！");
            return false;
        }
    }

}
