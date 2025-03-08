package com.db.utils;

import com.db.constants.BaseConstants;
import com.db.constants.CONSTANTS;
import com.db.constants.FileConstants;
import com.db.entity.BPlusTree;
import com.db.entity.BPlusTreeLeafNode;
import com.db.entity.BaseLinkedNode;
import com.db.entity.BasePage;
import com.db.entity.BaseSocket;
import com.db.entity.TableIndexDataInfo;
import com.db.file.page.FileManager;
import com.db.manager.CacheManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库解析相关的方法
 * @author Roy
 * @date 2022/10/23 17:55
 */
public class SqlParserUtil {
    /**
     * 判断数据库是否存在
     * @return 数据库是否存在
     */
    public static boolean isDbExist(String dbName) {
        List<String> dbNameList = Collections.singletonList(dbName);
        return FileUtil.isExistDir(CONSTANTS.DATABASE, dbNameList);
    }

    /**
     * 判断表是否存在
     */
    public static boolean isTbExist(String dbName,String tableName) {
        List<String> nameList = new ArrayList<>();
        nameList.add(dbName);
        nameList.add(tableName);
        return FileUtil.isExistDir(CONSTANTS.TABLE, nameList);
    }

    /**
     * 判断索引是否存在
     * @return
     */
    public static boolean isIndexDirExist(String dbName,String tableName,String indexName) {
        List<String> nameList = new ArrayList<>();
        nameList.add(dbName);
        nameList.add(tableName);
        nameList.add(indexName);
        return FileUtil.isExistDir(CONSTANTS.INDEX, nameList);
    }

    /**
     * 通过配置判断字段名是否存在
     */
    public static boolean isColumnExist(String dbName,String tableName,String column) {
        if (StringUtils.isEmpty(column)) {
            return false;
        }
        List<Object[]> list = getTableColumns(dbName, tableName);
        if (!CollectionUtils.isEmpty(list)) {
            for (Object[] data : list) {
                if (column.equals(data[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据表的路径获取表的信息（若缓存中有，则从缓存中获取）
     * @param dbName 数据库名
     * @param tableName 表名
     * @return 表的基础信息
     */
    public static List<Object[]> getTableColumns(String dbName,String tableName) {
        List<Object[]> tableBaseInfos = new ArrayList<>();
        String filePath = FileConstants.BASE_DIR_PATH+File.separator+dbName
                +File.separator+tableName+
                File.separator+FileConstants.TABLE_BASE_FILE_NAME;
        File file = new File(filePath);
        int fileIndex = FileManager.openFile(file);
        if (fileIndex == -1) {
            return null;
        }
        int pageCount = (int) Math.ceil((float)file.length()/CONSTANTS.PAGE_SIZE);
        for (int i = 0; i < pageCount; i++) {
            BaseLinkedNode node = CacheManager.getNodeByFileAndPage(fileIndex, i);
            if (node.getData()==null) {
                //当basePage为null，则为错误的情况
                BasePage basePage = FileManager.readPage(fileIndex, i, BaseConstants.COLUMN_LENGTH,BaseConstants.TABLE_COLUMN_TYPES,node);
                node.setData(basePage);
                //只取value object[4]
                List<Object[]> pageData = new ArrayList<>();
                for (BaseSocket socket : basePage.getSocketList()) {
                    if (socket != null) {
                        Object[] valueData = socket.getObjectValue();
                        pageData.add(valueData);
                    }
                }
                tableBaseInfos.addAll(pageData);
            } else {
                BasePage cachePageData = node.getData();
                BaseSocket[] socketList = cachePageData.getSocketList();
                //只取每个字段中的value部分
                for (BaseSocket socket : socketList) {
                    if (socket != null) {
                        Object[] valueData = socket.getObjectValue();
                        tableBaseInfos.add(valueData);
                    }
                }
            }
        }
        return tableBaseInfos;
    }

    /**
     * 根据索引文件路径获取索引字段信息
     * @return 索引详细信息
     */
    public static List<Object[]> getIndexInfoByPath(String dbName,String tableName,String indexName) {
        List<Object[]> indexColumnInfos = new ArrayList<>();
        String filePath = FileConstants.BASE_DIR_PATH+File.separator+dbName+
                File.separator+tableName+
                File.separator+FileConstants.INDEXES_DIR_NAME+
                File.separator+indexName+
                File.separator+FileConstants.INDEX_BASE_FILE_NAME;
        File indexFile = new File(filePath);
        int fileIndex = FileManager.openFile(indexFile);
        if (fileIndex == -1) {
            return null;
        }
        int pageCount = (int) Math.ceil((float)indexFile.length()/CONSTANTS.PAGE_SIZE);
        for (int i = 0; i < pageCount; i++) {
            BaseLinkedNode node = CacheManager.getNodeByFileAndPage(fileIndex, i);
            if (node.getData()==null) {
                //缓存中不存在，则读取文件
                BasePage basePage = FileManager.readPage(fileIndex, i, BaseConstants.INDEX_COLUMN_LENGTH, BaseConstants.INDEX_COLUMN_TYPES,node);
                node.setData(basePage);
                //只取value object[4]
                List<Object[]> pageData = new ArrayList<>();
                for (BaseSocket socket : basePage.getSocketList()) {
                    if (socket != null) {
                        Object[] valueData = socket.getObjectValue();
                        pageData.add(valueData);
                    }
                }
                indexColumnInfos.addAll(pageData);
            } else {
                BasePage basePage = node.getData();
                BaseSocket[] socketList = basePage.getSocketList();
                //只取每个字段中的value部分
                for (BaseSocket socket : socketList) {
                    if (socket != null) {
                        Object[] valueData = socket.getObjectValue();
                        indexColumnInfos.add(valueData);
                    }
                }
            }
        }
        return indexColumnInfos;
    }

    /**
     * select查询字段中等于某个值得数据
     * @param tableDataList 表中元数据
     * @param columnId 要过滤的列所在的下标
     * @param value 要等于的值
     * @return
     */
    public static List<List<Object[]>> getEqualedValue(List<List<Object[]>> tableDataList,
                                                           int columnId,
                                                           Object value) {
        List<List<Object[]>> result = new ArrayList<>();
        for (List<Object[]> columnData : tableDataList) {
            Object[] baseValue = columnData.get(columnId);
            if (ByteArrayUtil.isEqualed(baseValue,value)) {
                result.add(columnData);
            }
        }
        return result;
    }

    /**
     * select查询字段中等于表中等于某个值的数据
     * @param tbFileId 表文件id
     * @param columnIndex 列在表中数据存储的索引
     * @param value equal的值
     * @param columnLength 各字段长度
     * @param columnType 各字段类型
     * 获取满足条件的数据 key:表名，value: {key：pageId,value:满足条件的socketId}
     */
    public static void getEqualedValue(int tbFileId,
                                       String tableName,
                                       int columnIndex,
                                       Object value,
                                       int[] columnLength,
                                       String[] columnType,
                                       Map<String,Map<Integer,List<Integer>>> equaledValue) {
        boolean isTableMatched = true;
        if (equaledValue.get(tableName)==null) {
            //此表内容没有比较过
            isTableMatched = false;
        }
        //满足条件的数据，key：pageId,value：页中匹配的socket的下标
        Map<Integer,List<Integer>> matchedData = equaledValue.computeIfAbsent(tableName,k->new HashMap<>());
        //获取表中page的数量
        int maxPageId = FileManager.getMaxPageId(tbFileId);
        for (int i = 0; i <= maxPageId; i++) {
            BaseLinkedNode cacheNode = CacheManager.getNodeByFileAndPage(tbFileId, i);
            BasePage basePage = FileManager.readPage(tbFileId, i, columnLength, columnType, cacheNode);
            BaseSocket[] socketList = basePage.getSocketList();
            for (int j = 0; j < socketList.length; j++) {
                BaseSocket socket = socketList[j];
                if (socket==null) {
                    continue;
                }
                List<Object[]> columnData = socket.getObjectData();
                Object[] data = columnData.get(columnIndex);
                if (data == null) {
                    continue;
                }

                //值的对比，相同则添加记录
                List<Integer> socketIndexList = new ArrayList<>();
                if (ByteArrayUtil.isEqualed(data,value)) {
                    if (!isTableMatched) {
                        socketIndexList.add(j);
                    } else {
                        //查看之前的数据是否符合，若不符合则从结果中删除
                        List<Integer> socketIds = matchedData.get(i);
                        for (Integer socketId : socketIds) {
                            Object[] objects = columnData.get(socketId);
                            if (ByteArrayUtil.isEqualed(objects,value)) {
                                socketIndexList.add(socketId);
                            }
                        }
                    }
                }
                matchedData.put(i,socketIndexList);
            }
        }
    }

    /**
     * select两个表中对应字段相同的数据，同时将数据组合
     * @param tableDataMap 各表的数据
     * @param leftTableName 等号左边表的名称
     * @param leftColumnId 等号左边列对应的下标
     * @param rightColumnId 等号右边列对应的下标
     */
    public static void getTablesEqualedValue(Map<String,List<List<Object[]>>> tableDataMap,
                                             String leftTableName, int leftColumnId,
                                             String rightTableName,int rightColumnId) {
        //表中的数据
        List<List<Object[]>> leftTableData = tableDataMap.get(leftTableName);
        List<List<Object[]>> rightTableData = tableDataMap.get(rightTableName);
        //1.如果是一个表，直接将过滤结果返回，key值需要拼接表名
        if (leftTableName.equals(rightTableName)) {
            List<List<Object[]>> tableDataList = new ArrayList<>();
            for (List<Object[]> data : leftTableData) {
                if (ByteArrayUtil.isEqualed(data.get(leftColumnId),data.get(rightColumnId))) {
                    tableDataList.add(data);
                }
            }
            tableDataMap.put(leftTableName,tableDataList);
            return;
        }
        //2.不同的表筛选出符合条件的数据
        List<List<Object[]>> leftTableMatchedData = new ArrayList<>();
        List<List<Object[]>> rightTableMatchedData = new ArrayList<>();
        for (List<Object[]> leftColumnData : leftTableData) {
            Object[] leftValue = leftColumnData.get(leftColumnId);
            for (List<Object[]> rightColumnData : rightTableData) {
                Object[] rightValue = rightColumnData.get(rightColumnId);
                if (ByteArrayUtil.isEqualed(leftValue,rightValue)) {
                    leftTableMatchedData.add(leftColumnData);
                    rightTableMatchedData.add(rightColumnData);
                }
            }
        }
        tableDataMap.put(leftTableName,leftTableMatchedData);
        tableDataMap.put(rightTableName,rightTableMatchedData);
    }


    /**
     * 将不同表的数据进行组合，以第一个map为主
     * @return
     */
    public static List<Object[]> combineTableData(
            Map<String,List<Object[]>> tableDataMap) {
        if (CollectionUtils.isEmpty(tableDataMap)) {
            return null;
        }
        if (tableDataMap.size()==1) {
            for (String tableName : tableDataMap.keySet()) {
                return tableDataMap.get(tableName);
            }
        }
        List<Object[]> resultData = new ArrayList<>();
        for (String tableName : tableDataMap.keySet()) {
            //将第一张表的数据依次与后面的表中数据进行组合
            List<Object[]> dataList = tableDataMap.get(tableName);
            for (Object[] data : dataList) {
                if (data==null|| data.length==0) {
                    break;
                }
                for (String table : tableDataMap.keySet()) {
                    if (tableName.equals(table)) {
                        continue;
                    }
                    //其他表都与其进行组合
                    for (Object[] newData : tableDataMap.get(table)) {
                        List<Object> columnData = new ArrayList<>();
                        columnData.addAll(Arrays.asList(data));
                        columnData.addAll(Arrays.asList(newData));
                        resultData.add(columnData.toArray());
                    }
                }
            }
            break;
        }
        return resultData;
    }

    /**
     * 根据数据库名和表名获取索引信息
     * @param dbName
     * @param tableName
     * @return
     */
//    public static Map<String,List<PgClass>> getIndexInfosByTable(String dbName,String tableName) {
//        String filePath = FileConstants.BASE_DIR_PATH
//                +File.separator+dbName
//                +File.separator+tableName
//                +File.separator+FileConstants.INDEXES_DIR_NAME;
//        File indexDir  = new File(filePath);
//        File[] indexFiles = indexDir.listFiles(File::isDirectory);
//        if (indexFiles != null && indexFiles.length > 0) {
//            Map<String,List<PgClass>> indexInfos = new HashMap<>();
//            for (File indexFile : indexFiles) {
//                String indexName = indexFile.getName();
//                List<String> names = new ArrayList<>();
//                names.add(dbName);
//                names.add(tableName);
//                names.add(indexName);
//                indexInfos.put(indexName,getIndexInfoByPath(names));
//            }
//            return indexInfos;
//        }
//        return null;
//    }

    /**
     * 将索引信息写入文件
     * @param dbName 数据库名
     * @param tableName 表名
     * @param indexName 索引名
     * @param columnList 索引字段
     */
//    public static void addIndexToFile(String dbName,String tableName,String indexName,List<String> columnList) {
//        //(1)写入索引基本信息
//        String filePath = FileConstants.BASE_DIR_PATH
//                +File.separator+dbName
//                +File.separator+tableName
//                +File.separator+FileConstants.INDEXES_DIR_NAME
//                +File.separator+indexName
//                +File.separator+FileConstants.INDEX_BASE_FILE_NAME;
//        boolean createResult = FileUtil.createFile(filePath);
//        if (!createResult) {
//            //创建失败时，将已经创建的文件都删掉
//            System.out.println("创建文件失败！");
//            String dbPath = FileConstants.BASE_DIR_PATH+File.separator+dbName;
//            FileUtil.removeFile(dbPath);
//            return;
//        }
//        //获取列对应的基本信息
//        List<String> paths = new ArrayList<>();
//        paths.add(dbName);
//        paths.add(tableName);
//        List<PgClass> tableColumns = getTableByPath(paths);
//        List<PgClass> indexColumns = new ArrayList<>();
//        for (PgClass tableColumn : tableColumns) {
//            for (String column : columnList) {
//                if (tableColumn.getRelName().equals(column)) {
//                    indexColumns.add(tableColumn);
//                    break;
//                }
//            }
//        }
//        FileManager.writeDataToFile(indexColumns,filePath);
//        //（2）创建索引信息记录
//        //①创建数据记录文件
//        String dataFilePath = FileConstants.BASE_DIR_PATH
//                +File.separator+dbName
//                +File.separator+tableName
//                +File.separator+FileConstants.INDEXES_DIR_NAME
//                +File.separator+indexName
//                +File.separator+indexName+FileConstants.TABLE_DATA_FILE_EX;
//        boolean createDataFileResult = FileUtil.createFile(dataFilePath);
//        if (createDataFileResult) {
//            //todo 需要将数据也写入树中
//            BPlusTree bPlusTree = new BPlusTree();
//            FileManager.writeBPlusTreeToFile(bPlusTree,dataFilePath);
//        }
//    }

    /**
     * 获取表、索引或字段的基本信息
     * @return 基本信息数据
     */
    public static List<Object[]> getBaseData(String filePath,int[] columnLength,String[] columnTypes) {
        File file = new File(filePath);
        int fileIndex = FileManager.openFile(file);
        int pageCount = (int) Math.ceil((float)file.length()/CONSTANTS.PAGE_SIZE);
        List<Object[]> dataList = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            BaseLinkedNode node = CacheManager.getNodeByFileAndPage(fileIndex, i);
            if (node.getData() == null) {
                BasePage basePage = FileManager.readPage(fileIndex, i, columnLength,columnTypes,null);
                node.setData(basePage);
                BaseSocket[] socketList = basePage.getSocketList();
                for (int j = 0; j < socketList.length; j++) {
                    Object[] valueData = socketList[j].getObjectValue();
                    dataList.add(valueData);
                }
            } else {
                for (BaseSocket socket : node.getData().getSocketList()) {
                    dataList.add(socket.getObjectValue());
                }
            }
        }
        return dataList;
    }

    /**
     * 将索引信息记入缓存
     */
    public static void addIndexDataToCache(TableIndexDataInfo dataInfo) {
        if(!dataInfo.isDirty()) {
            return;
        }
        BPlusTree bPlusTree = dataInfo.getbPlusTree();
        int socketByteLength = dataInfo.getSockedByteLength();
        int indexDataFileId = dataInfo.getIndexDataFileId();
        //(1)计算一页可以放多少个socket（注：每一个socket都需要加上一个byte记录是否为空）
        int pageSocketCount = CONSTANTS.PAGE_SIZE/(socketByteLength+1);
        //(2)从bPlusTree中按照顺序获取所有叶子结点数据
        List<BPlusTreeLeafNode> leafDataList = bPlusTree.getAllLeafNodeData();
        //(3)将数据全部写入缓存
        List<List<Object[]>> pageObjectData = new ArrayList<>();
        List<List<byte[]>> pageByteData = new ArrayList<>();
        int pageId = 0;
        for (int i = 0; i < leafDataList.size(); i++) {
            BPlusTreeLeafNode node = leafDataList.get(i);
            pageObjectData.add(node.getObjectDataList());
            pageByteData.add(node.getByteData());
            if (i!=0 && i%pageSocketCount==0) {
                CacheManager.addPage(indexDataFileId,pageId,pageObjectData,pageByteData);
                CacheManager.markDirty(indexDataFileId,pageId);
                pageId++;
                pageByteData = new ArrayList<>();
                pageObjectData = new ArrayList<>();
            }
        }
        if (pageObjectData.size()!=0) {
            CacheManager.addPage(indexDataFileId,pageId,pageObjectData,pageByteData);
            CacheManager.markDirty(indexDataFileId,pageId);
        }
    }

}
