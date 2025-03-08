package com.db.manager;


import com.db.constants.CONSTANTS;
import com.db.entity.BaseLinkedNode;
import com.db.entity.BasePage;
import com.db.entity.BaseSocket;
import com.db.file.page.FileManager;
import com.db.utils.ByteArrayUtil;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 环形链表（缓存）
 * todo 注意并发问题
 * @author Roy
 * @date 2022/10/18 22:31
 */
public class CacheManager {
    /** 缓存中页面个数上限 */
    private static int MAX_CACHE_PAGE = 60000;
    /** 链表 */
    private static LinkedList<BaseLinkedNode> cachePageList = new LinkedList<>();
    /** 用于标记页是否是dirty的数组 */
    private static LinkedList<Boolean> dirtyList = new LinkedList<>();

    /**
     * 根据fileId和pageId获取缓存中的结点信息
     * 若没有，则分配一个结点
     * @return 分配的结点中的结点信息
     */
    public static BaseLinkedNode getNodeByFileAndPage(int fileId,int pageId) {
        //1.如果缓存为空，则放到第一位
        if (CollectionUtils.isEmpty(cachePageList)) {
            BaseLinkedNode node = new BaseLinkedNode();
            node.setPrevData(node);
            node.setNextData(node);
            cachePageList.addFirst(node);
            dirtyList.addFirst(false);
            return node;
        }
        //2.在缓存中查找本页数据所在节点
        BaseLinkedNode fetchNode = null;
        int fetchIndex = -1;
        for (int i = 0; i < cachePageList.size(); i++) {
            BaseLinkedNode node = cachePageList.get(i);
            BasePage page = node.getData();
            if (page.getFileId() == fileId && page.getPageId() == pageId) {
                fetchNode = node;
                fetchIndex = i;
                break;
            }
        }
        //3.若找到了，则将其放到第一位
        if (fetchNode != null) {
            //(1)将其放到第一位
            cachePageList.remove(fetchIndex);
            cachePageList.addFirst(fetchNode);
            //(2)修改dirtyList
            Boolean dirtyFlag = dirtyList.get(fetchIndex);
            dirtyList.remove(fetchIndex);
            dirtyList.addFirst(dirtyFlag);
            return fetchNode;
        }
        //4.没有则创建新的节点，且将当前访问节点放到第一位
        if (cachePageList.size()==MAX_CACHE_PAGE) {
            removeLastNode();
        }
        BaseLinkedNode firstNode = cachePageList.get(0);
        BaseLinkedNode lastNode = firstNode.getNextData();
        BaseLinkedNode newNode = new BaseLinkedNode();
        newNode.setPrevData(firstNode);
        newNode.setNextData(firstNode.getNextData());
        firstNode.setNextData(newNode);
        lastNode.setPrevData(newNode);
        cachePageList.addFirst(newNode);
        dirtyList.addFirst(false);
        return newNode;
    }

    /**
     * 新增一条cache信息
     * 添加到当前节点的
     * @return 添加结果 true:添加成功
     */
    public static boolean addPage(int fileId,int pageId,List<List<Object[]>> data,List<List<byte[]>> byteData) {
        BaseLinkedNode node = getNodeByFileAndPage(fileId, pageId);
        //若缓存中已添加，则直接返回 todo 待检查
//        if (node.getData() != null && node.getData().getSocketList().length!=0) {
//            System.out.println("缓存中已存在！");
//            return false;
//        }
        BasePage page = new BasePage(fileId,pageId,data,byteData);
        node.setData(page);
        return true;
    }

    /**
     * 将一页的value值写入缓存（放到最后一页的空白socket中）
     * @param fileId 打开的文件id
     * @param dataList 数据
     * @return 添加结果
     */
    public static boolean addPagesByValue(int fileId,List<Object[]> dataList,
                                         String[] columnType,
                                         int[] columnLength,byte[] variableByte) {
        try {
            //1.计算需要分成多少页
            int socketLength = 0;
            for (int i = 0; i < columnLength.length; i++) {
                socketLength += columnLength[i] + 23;
            }
            int socketCount = CONSTANTS.PAGE_SIZE / (socketLength + 1);
            //1.获取当前新添加数据的页码
            int maxPageId = FileManager.getMaxPageId(fileId);
            BasePage basePage = FileManager.readPage(fileId, maxPageId, columnLength, columnType,null);
            //最大页中的空socket数量
            int emptySocketCount = basePage.getEmptySocketCount();
            //需要写入的pageId
            int pageId = maxPageId;
            //2.获取槽是否为空的记录
            int notEmptySocketCount = socketCount-emptySocketCount;
            List<List<Object[]>> baseData = new ArrayList<>(dataList.size());
            List<List<byte[]>> byteData = new ArrayList<>(dataList.size());
            for (int i = 0; i < dataList.size(); i++) {
                notEmptySocketCount++;
                Object[] data = dataList.get(i);
                List<Object[]> socketData = new ArrayList<>(data.length);
                baseData.add(socketData);
                List<byte[]> socketByteData = new ArrayList<>(data.length);
                byteData.add(socketByteData);
                for (int j = 0; j < data.length; j++) {
                    Object[] columnData = new Object[5];
                    columnData[0] = columnType[j];
                    columnData[1] = columnLength[j];
                    columnData[2] = variableByte[j];
                    //-1代表该值为null
                    byte[] valueByte = new byte[columnLength[j]];
                    columnData[3] = -1;
                    if (data[j] != null) {
                        valueByte = ByteArrayUtil.objectToBytes(data[j], columnType[j]);
                        columnData[3] = valueByte.length;
                    }
                    columnData[4] = data[j];
                    //byte[]数据
                    byte[] typeByte = ByteArrayUtil.getStrCompleteByte(columnType[j],14);
                    byte[] lengthByte = ByteArrayUtil.intToByte(columnLength[j]);
                    byte[] isVariableByte = new byte[]{variableByte[j]};
                    byte[] realLengthByte = ByteArrayUtil.intToByte((int)columnData[3]);
                    byte[] completeByte = new byte[columnLength[j] - valueByte.length];
                    socketByteData.add(ByteArrayUtil.byteMergerAll(typeByte, lengthByte, isVariableByte, realLengthByte, valueByte, completeByte));
                    socketData.add(columnData);
                }
                //写入之前的空白socket
                if (i == emptySocketCount - 1) {
                    basePage.addSocketInfo(baseData, byteData);
                    baseData = new ArrayList<>(dataList.size());
                    byteData = new ArrayList<>(dataList.size());
                    notEmptySocketCount = 0;
                    pageId++;
                } else if (i % socketCount == 0 && i != 0) {
                    //写入新页
                    addPage(fileId, pageId, baseData, byteData);
                    markDirty(fileId, pageId);
                    notEmptySocketCount = 0;
                    pageId++;
                    baseData = new ArrayList<>(dataList.size());
                    byteData = new ArrayList<>(dataList.size());
                }
            }
            //剩下的数据不够一页，写入新页
            if (notEmptySocketCount != 0) {
                basePage.addSocketInfo(baseData, byteData);
                markDirty(fileId, pageId);
            }
            return true;
        } catch (Exception e) {
            System.out.println("写入数据失败！");
            return false;
        }
    }

    /**
     * 删除最后一个结点
     * @return
     */
    private static boolean removeLastNode() {
        BaseLinkedNode lastNode = cachePageList.removeLast();
        Boolean dirtyFlag = dirtyList.removeLast();
        if (dirtyFlag) {
            //1.将data数据重新组织成一页
            BasePage page = lastNode.getData();
            BaseSocket[] socketList = page.getSocketList();
            byte[] data = null;
            for (int j = 0; j < socketList.length; j++) {
                byte[] byteData = socketList[j].getSocketData();
                data = ByteArrayUtil.byteMergerAll(data,byteData);
            }
            //表示数据被删除了，此时一定会有pageId
            int fileId = lastNode.getData().getFileId();
            if (data == null || data.length == 0) {
                FileManager.writePage(fileId,page.getPageId(),null);
            } else {
                byte[] completeByte = new byte[CONSTANTS.PAGE_SIZE - data.length];
                data = ByteArrayUtil.byteMergerAll(data, completeByte);
                FileManager.writePage(fileId,page.getPageId(),data);
            }
        }
        return true;
    }

    /**
     * 删除key中包含name的页
     * @param fileIds 打开的文件下标
     * @return 删除标志 若成功则为true
     */
    public static boolean removePages(List<Integer> fileIds) {
        List<Integer> removeIndexes = new ArrayList<>();
        for (int i = 0; i < cachePageList.size(); i++) {
            BaseLinkedNode node = cachePageList.get(i);
            BasePage page = node.getData();
            for (int fileId : fileIds) {
                if (page.getFileId() == fileId) {
                    //1.修改链表
                    BaseLinkedNode prevData = node.getPrevData();
                    BaseLinkedNode nextData = node.getNextData();
                    prevData.setNextData(nextData);
                    nextData.setPrevData(prevData);
                    //2.获取dirty标志，写回数据
                    if (dirtyList.get(i)) {
                        //1.将data数据重新组织成一页
                        BaseSocket[] socketList = page.getSocketList();
                        byte[] data = null;
                        int notEmptySocketCount = 0;
                        //计算一页中socket的数量
                        int socketLength = 0;
                        for (int j = 0; j < socketList.length; j++) {
                            if (socketList[j]!=null) {
                                byte[] byteData = socketList[j].getSocketData();
                                //记录socket的字节长度
                                socketLength = socketLength == 0 ?byteData.length:socketLength;
                                data = ByteArrayUtil.byteMergerAll(data,byteData);
                                notEmptySocketCount++;
                            }
                        }
                        //表示数据被删除了，此时一定会有pageId
                        if (data == null || data.length == 0) {
                            FileManager.writePage(fileId,page.getPageId(),null);
                        } else {
                            byte[] notEmptyByte = new byte[notEmptySocketCount];
                            int socketCount = CONSTANTS.PAGE_SIZE/(socketLength+1);
                            byte[] isEmptyByte = new byte[socketCount-notEmptySocketCount];
                            Arrays.fill(isEmptyByte,(byte) 1);
                            byte[] completeByte = new byte[CONSTANTS.PAGE_SIZE - socketCount-data.length];
                            data = ByteArrayUtil.byteMergerAll(notEmptyByte,isEmptyByte,data, completeByte);
                            FileManager.writePage(fileId,page.getPageId(),data);
                        }
                    }
                    //3.记录需要删除的缓存下标
                    removeIndexes.add(i);
                    break;
                }
            }
        }
        //4.删除缓存中的数据
        for (Integer index : removeIndexes) {
            cachePageList.remove((int)index);
            dirtyList.remove((int)index);
        }
        return true;
    }

    /**
     * 将页数据设为dirty
     */
    public static boolean markDirty(int fileId,int pageId) {
        BaseLinkedNode node = getNodeByFileAndPage(fileId, pageId);
        dirtyList.set(0,true);
        return true;
    }

    /**
     * 获取文件所存储的最大的pageId
     * @return
     */
    public static int getMaxCachePage(int fileId) {
        if (CollectionUtils.isEmpty(cachePageList)) {
            return -1;
        } else {
            int maxIndex = -1;
            for (int i = 0; i < cachePageList.size(); i++) {
                BaseLinkedNode node = cachePageList.get(i);
                BasePage page = node.getData();
                if (page.getFileId() == fileId) {
                    maxIndex = page.getPageId() > maxIndex ? page.getPageId() : maxIndex;
                    break;
                }
            }
            return maxIndex;
        }
    }
}
