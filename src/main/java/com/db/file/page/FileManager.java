package com.db.file.page;

import com.db.constants.BaseConstants;
import com.db.constants.CONSTANTS;
import com.db.entity.BaseLinkedNode;
import com.db.entity.BasePage;
import com.db.manager.CacheManager;
import com.db.utils.ByteArrayUtil;
import com.db.utils.FileUtil;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 文件管理器
 * @author Roy
 * @date 2022/11/8 20:45
 */
public class FileManager {
    private static final int MAX_FILE_NUM = 128;
    private static final int MAX_TYPE_NUM = 256;
    /** 文件名和id的对应关系 */
    private static List<File> fileList = new ArrayList<>();
    /** fileList中对应的空文件下标 */
    private static ArrayList<Integer> emptyFileIndexes = new ArrayList<>();

    /**
     * 将buf+off开始的2048个四字节整数(8kb信息)写入fileID和pageID指定的文件页中
     * 无pageId为追加数据，有pageId无数据则为删除数据
     * @param fileId 文件id，用于区别已经打开的文件
     * @param pageId 文件的页号
     * @param pageData 需要存入的数据
     * @return 成功操作返回true
     */
    public static boolean writePage(int fileId, Integer pageId, byte[] pageData) {
        File file = fileList.get(fileId);
        if (file == null) {
            return false;
        }
        //1.读取file中要写入的pageId之后的数据
        RandomAccessFile accessFile = null;
        try {
            long beforeLength = file.length();
            //追加数据
            if (pageId != null){
                beforeLength = pageId * CONSTANTS.PAGE_SIZE;
            }
            accessFile = new RandomAccessFile(file,"rw");
            accessFile.seek(beforeLength);
            byte[] backUpData = new byte[0];
            //文件不为空，则读取当前页后面的数据
            if (file.length() != 0) {
                backUpData = new byte[(int) (file.length()-beforeLength-CONSTANTS.PAGE_SIZE)];
                accessFile.read(backUpData);
            }
            //2.数据写入
            if (pageData != null && pageData.length != 0) {
                accessFile.write(pageData);
            }
            accessFile.write(backUpData);
            return true;
        } catch (Exception e) {
            System.out.println("写入文件失败！");
        }
        return false;
    }

    /**
     * 将fileID和pageID指定的文件页中2048个四字节整数(8kb)读入到buf+off开始的内存中
     * @param fileId 文件id，用于区别已经打开的文件
     * @param pageId 文件的页号（从0开始） todo pageId不对
     * @param columnLength 槽中每一个字段上设置的长度（每个字段需要存储type 14,length 4,isVariable 1,realLength 4，value）
     * @return 读取的page对象
     */
    public static BasePage readPage(int fileId,int pageId,int[] columnLength,String[] valueType,BaseLinkedNode node) {
        File file = fileList.get(fileId);
        try {
            if (node==null) {
                node = CacheManager.getNodeByFileAndPage(fileId,pageId);
            }
            if (node.getData()==null) {
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                long pos = pageId * CONSTANTS.PAGE_SIZE;
                raf.seek(pos);
                //一个socket长度
                int socketLength = 0;
                for (int i = 0; i < columnLength.length; i++) {
                    socketLength += columnLength[i] + 23;
                }
                int socketCount = CONSTANTS.PAGE_SIZE / (socketLength + 1);
                //1.获取槽是否为空的记录
                byte[] emptyBitmap = new byte[socketCount];
                raf.read(emptyBitmap);
                pos += socketCount;
                if (file.length()==0) {
                    raf.seek(0);
                } else {
                    raf.seek(pos);
                }
                //2.读取页内槽信息
                List<List<Object[]>> pageData = new ArrayList<>();
                List<List<byte[]>> pageByteData = new ArrayList<>();
                for (int i = 0; i < socketCount; i++) {
                    if (file.length()==0) {
                        pageData.add(null);
                    } else {
                        //减去标记是否为空的标记位
                        byte[] bytes = new byte[socketLength];
                        raf.read(bytes);
                        if (emptyBitmap[i] == 0) {
                            pageData.add(getSocketInfo(columnLength, bytes, pageByteData, valueType));
                        } else {
                            pageData.add(null);
                        }
                        pos += bytes.length;
                        raf.seek(pos);
                    }
                }
                BasePage basePage = new BasePage(fileId, pageId, pageData, pageByteData);
                node.setData(basePage);
                //3.返回页信息
                return basePage;
            } else {
                return node.getData();
            }
        } catch (Exception e) {
            System.out.println("读取文件失败！");
        }
        return null;
    }

    /**
     * 将一个槽信息按照记录进行分解
     *   对于表中的一行数据来说，一个socket包含了两部分数据:
     *   1.存储字段信息;
     *   2.记录各列实际长度的int[]（一个int占4字节，若使用byte存储可能长度不够）
     *   若真实长度为-1，则表示为null
     * @param columnLength 每一列的长度 (value的长度)
     * @param socketData 从文件中读取到的byte[]信息（type 14,length 4,isVariable 1,realLength 4，value）
     * @param pageByteData 给 pageByteData赋值
     * @return 槽内的各列数据
     */
    private static List<Object[]> getSocketInfo(int[] columnLength,
                                                byte[] socketData,
                                                List<List<byte[]>> pageByteData,
                                                String[] valueType) {
        if (socketData == null || socketData.length == 0) {
            return null;
        }
        List<byte[]> socketByteData = new ArrayList<>();
        //当前socket所存储的数据，包含类型，长度，是否边长，真实长度和数据
        List<Object[]> columnData = new ArrayList<>();
        int off = 0;
        for (int i = 0; i < columnLength.length; i++) {
            //每列真实数据
            Object[] baseData = new Object[5];
            //当前socket的原始byte[]数据
            socketByteData.add(ByteArrayUtil.subByte(socketData,off,columnLength[i]+23));
            //字段类型
            byte[] typeByte = ByteArrayUtil.subByte(socketData,off,14);
            String type = new String(typeByte);
            //字段定长
            int length = columnLength[i];
            //是否变长
            byte[] isVariableByte = ByteArrayUtil.subByte(socketData,off+18,1);
            byte isVariable = isVariableByte[0];
            //真实长度
            byte[] realLengthByte = ByteArrayUtil.subByte(socketData,off+19,4);
            int realLength = ByteArrayUtil.toInt(realLengthByte);
            //真实数据
            Object value = null;
            byte[] valueByte = new byte[length];
            if (realLength!=-1) {
                valueByte = ByteArrayUtil.subByte(socketData, off+23, realLength);
                if (valueByte.length!=0) {
                    value = ByteArrayUtil.byteToObject(valueByte,valueType[i]);
                }
            }
            baseData[0] = type;
            baseData[1] = length;
            baseData[2] = isVariable;
            baseData[3] = realLength;
            baseData[4] = value;
            columnData.add(baseData);
            off += columnLength[i] + 23;
        }
        pageByteData.add(socketByteData);
        return columnData;
    }

    private static Object[] getGlobalSocketInfo(int[] columnLength,byte[] socketData,List<List<byte[]>> pageByteData) {
        if (socketData == null || socketData.length == 0) {
            return null;
        }
        Object[] resultData = new Object[5];
        List<byte[]> socketByteData = new ArrayList<>();
        byte[] typeByte = ByteArrayUtil.subByte(socketData, 0, columnLength[0]);
        byte[] lengthByte = ByteArrayUtil.subByte(socketData,14,columnLength[1]);
        byte[] isVariableByte = ByteArrayUtil.subByte(socketData,18,columnLength[2]);
        byte[] realLengthByte = ByteArrayUtil.subByte(socketData,19,columnLength[3]);
        byte[] valueByte = ByteArrayUtil.subByte(socketData,23,columnLength[4]);
        socketByteData.add(typeByte);
        socketByteData.add(lengthByte);
        socketByteData.add(isVariableByte);
        socketByteData.add(realLengthByte);
        socketByteData.add(valueByte);
        pageByteData.add(socketByteData);
        String type = new String(typeByte);
        int length = ByteArrayUtil.toInt(lengthByte);
        byte isVariable = isVariableByte[0];
        int realLength = ByteArrayUtil.toInt(realLengthByte);
        String value = new String(ByteArrayUtil.subByte(valueByte,0,realLength));
        resultData[0] = type;
        resultData[1] = length;
        resultData[2] = isVariable;
        resultData[3] = realLength;
        resultData[4] = value;
        return resultData;
    }

    /**
     * 当关闭文件的时候，将缓存中的脏数据写回文件
     * @param fileId 文件id
     * @return 操作成功返回true
     */
    public static boolean closeFile(int fileId) {
        CacheManager.removePages(Collections.singletonList(fileId));
        fileList.add(fileId,null);
        emptyFileIndexes.add(fileId);
        return true;
    }

    /**
     * 根据文件路径打开文件，如果成功打开，在fileID中存储为该文件分配的id
     * @param file 文件对象
     * @return 如果成功打开，返回下标；否则返回-1
     */
    public static int openFile(File file) {
        for (int i = 0; i < fileList.size(); i++) {
            if (fileList.get(i).equals(file)) {
                return i;
            }
        }
        //如果文件存在，且是一个文件，则打开文件
        if (file.exists() && file.isFile()) {
            Integer firstEmptyIndex = null;
            if (emptyFileIndexes.size() > 0) {
                firstEmptyIndex = emptyFileIndexes.get(0);
            }
            if (firstEmptyIndex == null) {
                //超过最大打开文件数量，也无法再打开新的文件
                if (fileList.size() >= MAX_FILE_NUM) {
                    System.out.println("超过了最大打开文件数量！");
                    return -1;
                }
                fileList.add(file);
                return fileList.size()-1;
            } else {
                fileList.add(firstEmptyIndex,file);
                emptyFileIndexes.remove(0);
                return firstEmptyIndex;
            }
        }
        return -1;
    }

    public static int openFile(String filePath) {
        File file = new File(filePath);
        return openFile(file);
    }

    /**
     * 根据文件路径删除文件，不需要关闭文件
     * @param filePath 文件路径
     * @return 删除的文件id
     */
    public static List<Integer> deleteFiles(String filePath) {
        List<Integer> fileIds = new ArrayList<>();
        //1.获取与要删除的文件路径前缀一样的文件
        for (int i = 0; i < fileList.size(); i++) {
            File file = fileList.get(i);
            if (file.getAbsolutePath().startsWith(filePath)) {
                fileIds.add(i);
                fileList.add(i,null);
                emptyFileIndexes.add(i);
            }
        }
        //2.删除缓存中的文件信息
        CacheManager.removePages(fileIds);
        //3.删除物理文件
        FileUtil.removeFile(filePath);
        return fileIds;
    }

    /**
     * 创建文件
     * @return 文件下标
     */
    public static int createFile(String filePath) {
        FileUtil.createFile(filePath);
        return openFile(filePath);
    }

    /**
     * 将基本信息写入文件
     * @param fileId 文件id
     * @param dataList 数据信息
     * @return 写入结果
     */
    public static boolean writeBaseDataToFile(int[] columnLength,String[] columnTypes,
                                                byte[] variableColumns,
                                                int fileId,List<Object[]> dataList) {
        File file = fileList.get(fileId);
        //将数据转换为文件中所记录的页式数据byte[]
        byte[] fileByte = null;
        //每一个槽的长度(每个字段需要存储type 14,length 4,isVariable 1,realLength 4，value)
        int socketLength = 0;
        for (int length : columnLength) {
            //+1是因为页首需要添加槽是否为空
            socketLength+=length+23;
        }
        //页中槽的数量
        int socketCount = CONSTANTS.PAGE_SIZE / (socketLength+1);
        byte[] pageCompleteByte = new byte[CONSTANTS.PAGE_SIZE - socketLength*dataList.size()-socketCount];
        //每页数据
        byte[] pageData = null;
        int pageId = 0;
        List<List<Object[]>> cachePageData = new ArrayList<>();
        List<List<byte[]>> pageByteData = new ArrayList<>();
        //循环每个槽的数据
        for (int i = 0; i < dataList.size(); i++) {
            List<byte[]> socketByteData = new ArrayList<>();
            Object[] valueData = dataList.get(i);
            //将每个槽的数据写入页中
            List<Object[]> socketData = new ArrayList<>();
            cachePageData.add(socketData);
            //循环每个槽中的value值
            for (int j = 0; j < valueData.length; j++) {
                int length = columnLength[j];
                Object obj = valueData[j];
                //字节数据
                String typeStr = columnTypes[j];
                byte[] type = ByteArrayUtil.getStrCompleteByte(typeStr,14);
                byte[] lengthByte = ByteArrayUtil.intToByte(length);
                byte isVariable = variableColumns[j];
                byte[] isVariableByte = new byte[]{isVariable};
                //真实数据
                Object[] baseData = new Object[5];
                baseData[0] = typeStr;
                baseData[1] = length;
                baseData[2] = isVariable;
                baseData[4] = obj;
                socketData.add(baseData);
                if (obj == null) {
                    //字节数据
                    byte[] bytes = new byte[length];
                    byte[] realLength = ByteArrayUtil.intToByte(-1);
                    byte[] columnByte = ByteArrayUtil.byteMergerAll(type,lengthByte,isVariableByte,realLength,bytes);
                    socketByteData.add(columnByte);
                    pageData = ByteArrayUtil.byteMergerAll(pageData,columnByte);
                    //真实数据
                    baseData[3] = -1;
                } else {
                    //字节数据
                    byte[] bytes = ByteArrayUtil.objectToBytes(obj,typeStr);
                    byte[] realLength = ByteArrayUtil.intToByte(bytes.length);
                    byte[] completeByte = new byte[length-bytes.length];
                    byte[] columnByte = ByteArrayUtil.byteMergerAll(type,lengthByte,isVariableByte,realLength,bytes,completeByte);
                    socketByteData.add(columnByte);
                    pageData = ByteArrayUtil.byteMergerAll(pageData,columnByte);
                    //转换后的数据
                    baseData[3] = bytes.length;
                }
                pageByteData.add(socketByteData);
            }
        }
        //将槽中的数据添加为页数据
        byte[] isNotEmptySocket = new byte[dataList.size()];
        byte[] isEmptySocket = new byte[socketCount-dataList.size()];
        Arrays.fill(isEmptySocket, (byte) 1);
        //1.将数据添加到最终要写入的byte[],combinedByte作为一页
        fileByte = ByteArrayUtil.byteMergerAll(fileByte,isNotEmptySocket,isEmptySocket,pageData,pageCompleteByte);
        //2.将数据写入缓存
        CacheManager.addPage(fileId,pageId,cachePageData,pageByteData);
        //(2)将表的基本信息写入文件
        try {
            RandomAccessFile accessFile = new RandomAccessFile(file,"rw");
            accessFile.write(fileByte);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 获取当前文件最大的pageId
     * @return
     */
    public static int getMaxPageId(int fileId) {
        File file = fileList.get(fileId);
        if (file.length()==0) {
            return 0;
        }
        long length = file.length();
        int filePageIndex = (int)Math.ceil((float)length/CONSTANTS.PAGE_SIZE)-1;
        int maxCachePage = CacheManager.getMaxCachePage(fileId);
        return Math.max(filePageIndex, maxCachePage);
    }

    /**
     * 结束的时候将所有数据写回
     * @return 关闭文件结果
     */
    public static boolean closeAllFile() {
        for (int i = 0; i < fileList.size(); i++) {
            CacheManager.removePages(Collections.singletonList(i));
        }
        return true;
    }
}
