package com.db;

import com.db.constants.BaseConstants;
import com.db.constants.CONSTANTS;
import com.db.constants.FileConstants;
import com.db.constants.GlobalConstants;
import com.db.entity.BPlusTree;
import com.db.entity.BPlusTreeLeafNode;
import com.db.entity.BaseLinkedNode;
import com.db.entity.BasePage;
import com.db.entity.BaseSocket;
import com.db.entity.TableIndexDataInfo;
import com.db.file.page.FileManager;
import com.db.manager.CacheManager;
import com.db.sql.parser.SQLBaseVisitor;
import com.db.sql.parser.SQLParser;
import com.db.utils.ByteArrayUtil;
import com.db.utils.FileUtil;
import com.db.utils.PrintTableUtil;
import com.db.utils.SqlParserUtil;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Roy
 * @date 2022/10/25 20:04
 */
public class SqlParseVisitor extends SQLBaseVisitor {
    /** 用户创建数据库所在的File */
    private File baseDir = new File(FileConstants.BASE_DIR_PATH);
    /** 缓存数据 */
    private Map<String,Object> memory = new HashMap<>();

    /**
     * 常量key
     */
    private static final String DB_NAME = "dbName";
    /** 等于某个值的条件过滤  key:tableName+"."+columnId value:value */
    private final Map<String,Object> equalValueMap = new HashMap<>();
    /** 等于另一个字段的条件过滤  key:tableName+"."+columnId,value:tableName+"."+columnId*/
    private final List<Map<String,String>> equalColumnMap = new ArrayList<>();
    /** 要查询的表的基本信息 key：tableName value: key:columnName,value:列的基本信息*/
    private Map<String,Map<String,Object[]>> tableBaseMap = new HashMap<>();
    /** 当前已经记入的表的索引数据信息 key:tableName value: key:indexName value:BPlusTree*/
    private Map<String,Map<String, TableIndexDataInfo>> tableIndexTree = new HashMap<>();
    /** 当前查询的表中的数据 */
    private Map<String,List<List<Object[]>>> tableDataMap;

    @Override
    public Object visitProgram(SQLParser.ProgramContext ctx) {
        return visitStatement(ctx.statement(ctx.getRuleIndex()));
    }


    @Override
    public Object visitStatement(SQLParser.StatementContext ctx) {
        try {
            if (ctx.table_statement() != null) {
                if (StringUtils.isEmpty(memory.get(DB_NAME))) {
                    System.out.println("请选择数据库！");
                    return null;
                }
                SQLParser.Table_statementContext table_statementContext = ctx.table_statement();
                if (table_statementContext instanceof SQLParser.Create_tableContext) {
                    try {
                        visitCreate_table((SQLParser.Create_tableContext) table_statementContext);
                    } catch (Exception e) {
                        System.out.println("创建表失败！");
                    }
                } else if (table_statementContext instanceof SQLParser.Drop_tableContext) {
                    try {
                        visitDrop_table((SQLParser.Drop_tableContext) table_statementContext);
                    } catch (Exception e) {
                        System.out.println("删除表失败！");
                    }
                } else if (table_statementContext instanceof SQLParser.Insert_into_tableContext) {
                    try {
                        visitInsert_into_table((SQLParser.Insert_into_tableContext) table_statementContext);
                    } catch (Exception e) {
                        System.out.println("插入表失败！");
                    }
                } else if (table_statementContext instanceof SQLParser.Delete_from_tableContext) {
                    try {
                        visitDelete_from_table((SQLParser.Delete_from_tableContext) table_statementContext);
                    } catch (Exception e) {
                        System.out.println("删除失败！");
                    }
                } else if (table_statementContext instanceof SQLParser.Update_tableContext) {
                    try {
                        visitUpdate_table((SQLParser.Update_tableContext) table_statementContext);
                    } catch (Exception e) {
                        System.out.println("修改失败！");
                    }
                } else if (table_statementContext instanceof SQLParser.Select_table_Context) {
                    try {
                        visitSelect_table_((SQLParser.Select_table_Context) table_statementContext);
                    } catch (Exception e) {
                        System.out.println("查询失败！");
                    }
                } else if (table_statementContext instanceof SQLParser.Describe_tableContext) {
                    try {
                        visitDescribe_table((SQLParser.Describe_tableContext) table_statementContext);
                    } catch (Exception e) {
                        System.out.println("展示表信息失败！");
                    }
                }
            } else if (ctx.db_statement() != null) {
                SQLParser.Db_statementContext dbContext = ctx.db_statement();
                if (dbContext instanceof SQLParser.Create_dbContext) {
                    try {
                        visitCreate_db((SQLParser.Create_dbContext) dbContext);
                    } catch (Exception e) {
                        System.out.println("创建表失败！");
                    }
                } else if (dbContext instanceof SQLParser.Drop_dbContext) {
                    try {
                        visitDrop_db((SQLParser.Drop_dbContext) dbContext);
                    } catch (Exception e) {
                        System.out.println("删除表失败！");
                    }
                } else if (dbContext instanceof SQLParser.Show_dbsContext) {
                    try {
                        visitShow_dbs((SQLParser.Show_dbsContext) dbContext);
                    } catch (Exception e) {
                        System.out.println("展示数据库信息失败！");
                    }
                } else if (dbContext instanceof SQLParser.Use_dbContext) {
                    try {
                        visitUse_db((SQLParser.Use_dbContext) dbContext);
                    } catch (Exception e) {
                        System.out.println("use 数据库指向失败！");
                    }
                } else if (dbContext instanceof SQLParser.Show_tablesContext) {
                    try {
                        visitShow_tables((SQLParser.Show_tablesContext) dbContext);
                    } catch (Exception e) {
                        System.out.println("展示表失败！");
                    }
                } else if (dbContext instanceof SQLParser.Show_indexesContext) {
                    try {
                        visitShow_indexes((SQLParser.Show_indexesContext) dbContext);
                    } catch (Exception e) {
                        System.out.println("展示索引信息失败！");
                    }
                }
            } else if (ctx.alter_statement() != null) {
                if (StringUtils.isEmpty(memory.get(DB_NAME))) {
                    System.out.println("请选择数据库！");
                    return null;
                }
                SQLParser.Alter_statementContext alterStatement = ctx.alter_statement();
                if (alterStatement instanceof SQLParser.Alter_add_indexContext) {
                    try {
                        visitAlter_add_index((SQLParser.Alter_add_indexContext) alterStatement);
                    } catch (Exception e) {
                        System.out.println("添加索引失败！");
                    }
                } else if (alterStatement instanceof SQLParser.Alter_drop_indexContext) {
                    try {
                        visitAlter_drop_index((SQLParser.Alter_drop_indexContext) alterStatement);
                    } catch (Exception e) {
                        System.out.println("删除索引失败！");
                    }
                } else if (alterStatement instanceof SQLParser.Alter_table_add_pkContext) {
                    try {
                        visitAlter_table_add_pk((SQLParser.Alter_table_add_pkContext) alterStatement);
                    } catch (Exception e) {
                        System.out.println("添加主键失败！");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return memory.get(DB_NAME);
    }

    /**
     * 创建数据库
     * @param ctx
     * @return
     */
    @Override
    public Object visitCreate_db(SQLParser.Create_dbContext ctx) {
        //检查命名是否符合规范
        String dbName = ctx.Identifier().getText().toUpperCase();
        if (!Pattern.matches(GlobalConstants.NAME_REGEX, dbName)) {
            System.out.println("必须以字母开头，只能输入字母数字和下划线！");
            return null;
        }
        if (FileConstants.GLOBAL_DB_NAME.equals(dbName)) {
            System.out.println("数据库名不能与系统数据库名重复！");
            return null;
        }
        //1.判断数据库是否存在
        boolean existDir = SqlParserUtil.isDbExist(dbName);
        if (existDir) {
            System.out.println("数据库已存在！");
            return null;
        }
        //2.不存在则进行创建
        FileUtil.createDir(CONSTANTS.DATABASE, Collections.singletonList(dbName));
        System.out.println(dbName+"数据库创建成功！");
        return null;
    }

    /**
     * 删除数据库
     * @param ctx
     * @return
     */
    @Override
    public Object visitDrop_db(SQLParser.Drop_dbContext ctx) {
        String dbName = ctx.Identifier().getText().toUpperCase();
        //1.判断数据库是否存在
        boolean existDir = SqlParserUtil.isDbExist(dbName);
        if (existDir) {
            //2、删除文件和缓存中的数据
            String[] names = new String[]{dbName};
            String dbPath = FileUtil.getDirPathByTypeAndName(CONSTANTS.DATABASE, names);
            FileManager.deleteFiles(dbPath);
        }
        System.out.println(dbName+"数据库删除成功");
        memory.put(DB_NAME,null);
        return true;
    }

    /**
     * 显示数据库
     * @param ctx
     * @return 数据库数量
     */
    @Override
    public Object visitShow_dbs(SQLParser.Show_dbsContext ctx) {
        File[] files = baseDir.listFiles(File::isDirectory);
        if (files == null) {
            System.out.println("暂无数据库！");
            return null;
        }
        String[] heads = new String[2];
        heads[0] = "序号";
        heads[1] = "名称";
        String[][] data = new String[files.length][2];
        if (files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                data[i][0] = String.valueOf(i);
                data[i][1] = files[i].getName();
            }
            PrintTableUtil.printTable(heads,data);
        } else {
            System.out.println("暂无数据库！");
        }
        return null;
    }

    @Override
    public Object visitUse_db(SQLParser.Use_dbContext ctx) {
        String dbName = ctx.Identifier().getText().toUpperCase();
        boolean existDb = FileUtil.isExistDir(CONSTANTS.DATABASE, Collections.singletonList(dbName));
        if (existDb) {
            memory.put(DB_NAME, dbName);
            //加载在global文件夹下定义的表和索引的基本信息
            //(1)表基本信息
            String tableBaseFilePath = FileConstants.GLOBAL_DIR_PATH+File.separator+FileConstants.GLOBAL_TB_NAME;
            File tableBaseFile = new File(tableBaseFilePath);
            int fileId = FileManager.openFile(tableBaseFile);
            //判断缓存中是否存在，若不存在，则将数据写入缓存
            BaseLinkedNode cacheNode = CacheManager.getNodeByFileAndPage(fileId, 0);
            int [] columnLength = new int[BaseConstants.TABLE_COLUMNS.length];
            Arrays.fill(columnLength,100);
            String[] columnType = new String[BaseConstants.TABLE_COLUMNS.length];
            Arrays.fill(columnType,CONSTANTS.VARCHAR);
            if (cacheNode.getData() == null) {
                cacheNode.setData(FileManager.readPage(fileId,0,columnLength,columnType,cacheNode));
            }
            //(2)索引的基本信息 索引基本信息的存储结构与表的基本信息存储结构一致
            String indexBaseFilePath = FileConstants.GLOBAL_DIR_PATH+File.separator+FileConstants.GLOBAL_INDEX_NAME;
            File indexBaseFile = new File(indexBaseFilePath);
            int indexFileId = FileManager.openFile(indexBaseFile);
            //判断缓存中是否存在，若不存在，则将数据写入缓存
            BaseLinkedNode indexCacheNode = CacheManager.getNodeByFileAndPage(indexFileId, 0);
            if (indexCacheNode.getData() == null) {
                int [] indexColumnLength = new int[BaseConstants.INDEX_COLUMNS.length];
                Arrays.fill(indexColumnLength,100);
                String[] indexColumnType = new String[BaseConstants.INDEX_COLUMNS.length];
                Arrays.fill(indexColumnType,CONSTANTS.VARCHAR);
                indexCacheNode.setData(FileManager.readPage(indexFileId,0,indexColumnLength,indexColumnType,indexCacheNode));
            }
        } else {
            System.out.println("数据库不存在！");
        }
        return dbName;
    }

    /**
     * 显示当前数据库中的表
     * @param ctx
     * @return
     */
    @Override
    public Object visitShow_tables(SQLParser.Show_tablesContext ctx) {
        String dbName = (String) memory.get(DB_NAME);
        if (StringUtils.isEmpty(dbName) || !SqlParserUtil.isDbExist(dbName)) {
            System.out.println("请选择数据库！");
        } else {
            String filePath = FileUtil.getDirPathByTypeAndName(CONSTANTS.DATABASE,new String[]{dbName});
            File dbDir = new File(filePath);
            File[] files = dbDir.listFiles(File::isDirectory);
            if (files != null && files.length > 0) {
                String[] heads = new String[2];
                heads[0] = "序号";
                heads[1] = "名称";
                String[][] data = new String[files.length][2];
                for (int i = 0; i < files.length; i++) {
                    data[i][0] = String.valueOf(i);
                    data[i][1] = files[i].getName();
                }
                PrintTableUtil.printTable(heads,data);
            } else {
                System.out.println("暂无表！");
            }
        }
        return null;
    }

    @Override
    public Object visitCreate_table(SQLParser.Create_tableContext ctx) {
        String dbName = (String) memory.get(DB_NAME);
        //1.检查命名是否符合规范
        String tableName = ctx.Identifier().getText().toUpperCase();
        if (!Pattern.matches(GlobalConstants.NAME_REGEX, tableName)) {
            System.out.println("必须以字母开头，只能输入字母数字和下划线！");
            return null;
        }
        //2.检查表是否已存在
        List<String> dirList = new ArrayList<>();
        dirList.add(dbName);
        dirList.add(tableName);
        boolean existTable = FileUtil.isExistDir(CONSTANTS.TABLE, dirList);
        if (existTable) {
            System.out.println("表已存在!");
            return null;
        }
        //3.获取需要记录的信息
        int childrenCount = ctx.field_list().children.size();
        //（1）字段信息
        List<Object[]> fieldList = new ArrayList<>();
        Map<String,Object[]> fieldMap = new HashMap<>();
        //主键信息
        List<Object[]> pKLists = new ArrayList<>();
        //列id
        int fieldId = 0;
        int pkFieldId =  0;
        for (int i = 0; i < childrenCount; i++) {
            SQLParser.FieldContext fieldContext = ctx.field_list().field(i);
            //字段信息
            if (fieldContext instanceof SQLParser.Normal_fieldContext) {
                SQLParser.Normal_fieldContext child = (SQLParser.Normal_fieldContext) fieldContext;
                Object[] fieldData = new Object[BaseConstants.TABLE_COLUMNS.length];
                fieldData[0] = fieldId;
                String fieldName = child.Identifier().getText().toUpperCase();
                fieldData[1] = fieldName;
                String type = child.type_().getText();
                fieldData[2] = type;
                if (type.contains(CONSTANTS.VARCHAR)) {
                    fieldData[2] = CONSTANTS.VARCHAR;
                    fieldData[3] = Integer.valueOf(child.type_().Integer().getText());
                } else {
                    fieldData[3] = BaseConstants.COLUMN_LENGTH[3];
                }
                fieldData[4] = child.getText().contains("NOTNULL")?CONSTANTS.FALSE:CONSTANTS.TRUE;
                //暂时用字符串表示 如果值为VARCHAR类型，则会添加‘’
                if (child.value() != null) {
                    fieldData[5] = child.value().getText();
                }
                fieldList.add(fieldData);
                if (fieldMap.get(fieldName) != null) {
                    System.out.println("列名重复");
                    return null;
                }
                fieldMap.put(fieldName,fieldData);
                fieldId ++;
            } else if (fieldContext instanceof SQLParser.Primary_key_fieldContext) {
                //主键信息
                SQLParser.Primary_key_fieldContext pKContext = (SQLParser.Primary_key_fieldContext) fieldContext;
                TerminalNode identifier = pKContext.Identifier();
                SQLParser.IdentifiersContext identifiers = pKContext.identifiers();
                if (identifier != null) {
                    //单个主键
                    Object[] fieldData = fieldMap.get(identifier.getText());
                    fieldData[6] = CONSTANTS.TRUE;
                    Object[] pKData = new Object[BaseConstants.INDEX_COLUMNS.length];
                    pKData[0] = pkFieldId;
                    pKData[1] = FileConstants.PK_DIR_NAME;
                    pKData[2] = fieldData[0];
                    pKData[3] = 1;
                    pKData[4] = 1;
                    pKLists.add(pKData);
                } else if (identifiers != null){
                    List<TerminalNode> identifierList = identifiers.Identifier();
                    for (TerminalNode terminalNode : identifierList) {
                        String columnName = terminalNode.getText().toUpperCase();
                        Object[] fieldData = fieldMap.get(columnName);
                        if (fieldData == null) {
                            System.out.println(columnName+"不存在！");
                            return null;
                        } else if (CONSTANTS.FALSE == (int) fieldData[4]){
                            fieldData[6] = CONSTANTS.TRUE;
                            Object[] pKData = new Object[BaseConstants.INDEX_COLUMNS.length];
                            pKData[0] = pkFieldId;
                            pKData[1] = FileConstants.PK_DIR_NAME;
                            pKData[2] = fieldData[0];
                            pKData[3] = 1;
                            pKData[4] = 1;
                            pKData[5] = 1;
                            pKLists.add(pKData);
                        } else {
                            System.out.println("主键信息错误！");
                            return null;
                        }
                    }
                }
                pkFieldId++;
            } else if (fieldContext instanceof SQLParser.Foreign_key_fieldContext) {
                //外键信息
                SQLParser.Foreign_key_fieldContext fKContext = (SQLParser.Foreign_key_fieldContext) fieldContext;
                //(1)获取外键所在表信息
                TerminalNode anotherTbNode = fKContext.Identifier(1);
                String anotherTableName = anotherTbNode.getText().toUpperCase();
                //获取另一个表的列基本信息
                String tbColumnDataFilePath = FileUtil.getTbColumnDataFilePath(dbName,anotherTableName);
                File anotherTbBaseFile = new File(tbColumnDataFilePath);
                int fileIndex = FileManager.openFile(anotherTbBaseFile);
                if (fileIndex == -1) {
                    System.out.println("关联表不存在");
                    return null;
                }
                List<Object[]> anotherTbColumns = SqlParserUtil.getBaseData(tbColumnDataFilePath,BaseConstants.COLUMN_LENGTH,BaseConstants.TABLE_COLUMN_TYPES);
                //外键
                TerminalNode tbNode = fKContext.Identifier(0);
                List<TerminalNode> tbFields = fKContext.identifiers(0).Identifier();
                List<TerminalNode> anotherTbFields = fKContext.identifiers(1).Identifier();
                //(1)外键对应其他表的fields  列必须是主键
                List<String> anotherTbFieldNames = new ArrayList<>();
                for (TerminalNode tbField : anotherTbFields) {
                    String fieldName = tbField.getText().toUpperCase();
                    //添加为外键是否有效
                    boolean isValid = false;
                    for (Object[] columns : anotherTbColumns) {
                        //暂时规定外键必须是主键
                        if (fieldName.equals(columns[1])
                                && CONSTANTS.TRUE == (int)columns[6]) {
                            isValid = true;
                            break;
                        }
                    }
                    if (isValid) {
                        anotherTbFieldNames.add(tbField.getText().toUpperCase());
                    } else {
                        System.out.println("外键无效，创建表失败！");
                        return null;
                    }
                }
                //(2)相应的列设置外键属性
                if (tbNode != null) {
                    String fieldName = tbNode.getText().toUpperCase();
                    Object[] fieldData = fieldMap.get(fieldName);
                    if (fieldData == null) {
                        System.out.println("外键指定列名不存在！");
                        return null;
                    }
                    fieldData[8] = CONSTANTS.TRUE;
                } else if (!CollectionUtils.isEmpty(tbFields)) {
                    int fKIndex = 0;
                    for (TerminalNode tbField : tbFields) {
                        String fieldName = tbField.getText().toUpperCase();
                        Object[] fieldData = fieldMap.get(fieldName);
                        if (fieldData == null) {
                            System.out.println("联合外键指定列名不存在！");
                            return null;
                        } else {
                            fieldData[8] = CONSTANTS.TRUE;
                            fieldData[9] = anotherTableName;
                            fieldData[10] = anotherTbFieldNames.get(fKIndex);
                        }
                        fKIndex++;
                    }
                } else {
                    System.out.println("外键创建有误，创建表失败！");
                    return null;
                }
            }
        }
        //4.创建文件夹，名称为表名
        boolean createFlag = FileUtil.createDir(CONSTANTS.TABLE, dirList);
        if (!createFlag) {
            System.out.println("表文件夹创建失败！");
            return null;
        }
        //5.创建记录表基本信息的文件
        File dbDir = new File(FileConstants.BASE_DIR_PATH+File.separator+memory.get(DB_NAME));
        String tableBaseFilePath = dbDir.getAbsolutePath()+File.separator+
                tableName+File.separator+FileConstants.TABLE_BASE_FILE_NAME;
        int fileId = FileManager.createFile(tableBaseFilePath);
        if (fileId == -1) {
            System.out.println("表基本信息文件创建失败！");
            return null;
        }
        //6.将数据写入文件
        boolean writeColumnFlag = FileManager.writeBaseDataToFile(BaseConstants.COLUMN_LENGTH,
                BaseConstants.TABLE_COLUMN_TYPES,
                BaseConstants.VARIABLE_COLUMN_FLAG,
                fileId, fieldList);
        if (!writeColumnFlag) {
            System.out.println("写入字段信息失败！");
            return null;
        }
        //7.主键会自动创建索引文件，存储索引信息
        if (!CollectionUtils.isEmpty(pKLists)) {
            //(1)写入索引基本信息
            String filePath = FileConstants.BASE_DIR_PATH
                    +File.separator+dbName
                    +File.separator+tableName
                    +File.separator+FileConstants.INDEXES_DIR_NAME
                    +File.separator+FileConstants.PK_DIR_NAME
                    +File.separator+FileConstants.INDEX_BASE_FILE_NAME;
            int indexFileId = FileManager.createFile(filePath);
            if (indexFileId == -1) {
                //创建失败时，将已经创建的文件都删掉
                System.out.println("创建文件失败！");
                String tablePath = FileConstants.BASE_DIR_PATH+File.separator+dbName
                        +File.separator+tableName;
                FileUtil.removeFile(tablePath);
                return null;
            }
            FileManager.writeBaseDataToFile(BaseConstants.INDEX_COLUMN_LENGTH,
                    BaseConstants.INDEX_COLUMN_TYPES,
                    BaseConstants.VARIABLE_INDEX_FLAG,
                    indexFileId,pKLists);
            //（2）创建索引信息记录
            //①创建数据记录文件
            String dataFilePath = FileConstants.BASE_DIR_PATH
                    +File.separator+dbName
                    +File.separator+tableName
                    +File.separator+FileConstants.INDEXES_DIR_NAME
                    +File.separator+FileConstants.PK_DIR_NAME
                    +File.separator+FileConstants.PK_DIR_NAME+FileConstants.TABLE_DATA_FILE_EX;
            int indexDataFileId = FileManager.createFile(dataFilePath);
            if (indexDataFileId == 0) {
                System.out.println("索引数据信息记录文件创建失败！");
                String tablePath = FileConstants.BASE_DIR_PATH+File.separator+dbName
                        +File.separator+tableName;
                FileUtil.removeFile(tablePath);
                return null;
            }
        }
        return super.visitCreate_table(ctx);
    }

    @Override
    public Object visitDrop_table(SQLParser.Drop_tableContext ctx) {
        String dbName = (String) memory.get(DB_NAME);
        String tableName = ctx.Identifier().getText().toUpperCase();
        List<String> fileList = new ArrayList<>();
        fileList.add(dbName);
        fileList.add(tableName);
        if (!FileUtil.isExistDir(CONSTANTS.TABLE,fileList)) {
            System.out.println("表不存在");
            return null;
        }
        String filePath = FileConstants.BASE_DIR_PATH+File.separator+dbName+File.separator+tableName;
        FileManager.deleteFiles(filePath);
        return super.visitDrop_table(ctx);
    }

    /**
     * 查询表的详细信息
     * @param ctx
     * @return
     */
    @Override
    public Object visitDescribe_table(SQLParser.Describe_tableContext ctx) {
        String dbName = (String) memory.get(DB_NAME);
        String tableName = ctx.Identifier().getText().toUpperCase();
        //判断表是否存在
        boolean tbExist = SqlParserUtil.isTbExist(dbName,tableName);
        if (!tbExist) {
            System.out.println(tableName+"表不存在！");
            return null;
        }
        String[] headers = BaseConstants.TABLE_COLUMNS;
        List<Object[]> tableColumns = SqlParserUtil.getTableColumns(dbName,tableName);
        String[][] data = new String[tableColumns.size()][headers.length];
        for (int i = 0; i < tableColumns.size(); i++) {
            Object[] column = tableColumns.get(i);
            for (int j = 0; j < column.length; j++) {
                if (j==4 || j==6 || j==7 || j==8) {
                    if (column[j] != null) {
                        data[i][j] = CONSTANTS.TRUE == (int) column[j] ? "是" : "否";
                    } else {
                        data[i][j] = "否";
                    }
                } else {
                    if (column[j]==null) {
                        data[i][j] = "<NULL>";
                    } else {
                        data[i][j] = String.valueOf(column[j]);
                    }
                }
            }
        }
        PrintTableUtil.printTable(headers,data);
        return null;
    }

    /**
     * 将数据导入表中
     * @param ctx
     * @return
     */
    @Override
    public Object visitLoad_data(SQLParser.Load_dataContext ctx) {
        return super.visitLoad_data(ctx);
    }

    /**
     * 获取当前数据库中的所有索引信息
     * @param ctx
     * @return
     */
    @Override
    public Object visitShow_indexes(SQLParser.Show_indexesContext ctx) {
        String dbName = (String) memory.get(DB_NAME);
        String dbDirPath = FileConstants.BASE_DIR_PATH+File.separator+dbName;
        File dbDirFile = new File(dbDirPath);
        String[] tables = dbDirFile.list((dir, name) -> dir.isDirectory());
        if (tables == null || tables.length==0) {
            return null;
        }
        String[] headers = new String[3];
        headers[0] = "tableName";
        headers[1] = "indexName";
        headers[2] = "columns";
        List<String[]> data = new ArrayList<>();
        for (String tableName : tables) {
            String indexDirPath = FileConstants.BASE_DIR_PATH + File.separator + dbName
                    + File.separator + tableName
                    + File.separator + FileConstants.INDEXES_DIR_NAME;
            File indexDir = new File(indexDirPath);
            if (indexDir.exists()) {
                String[] fileList = indexDir.list();
                if (fileList == null || fileList.length == 0) {
                    return null;
                }
                //查询表的信息
                List<Object[]> tableColumns = SqlParserUtil.getTableColumns(dbName, tableName);
                for (int i = 0; i < fileList.length; i++) {
                    String indexName = fileList[i];
                    String[] indexData = new String[3];
                    data.add(indexData);
                    indexData[0] = tableName;
                    indexData[1] = indexName;
                    StringBuilder columns = new StringBuilder();
                    List<Object[]> indexInfos = SqlParserUtil.getIndexInfoByPath(dbName,tableName,indexName);
                    if (!CollectionUtils.isEmpty(indexInfos)) {
                        for (int j = 0; j < indexInfos.size(); j++) {
                            Object[] field = indexInfos.get(j);
                            //将columnId转成columnName
                            int columnId = (int) field[2];
                            columns.append(tableColumns.get(columnId)[1]);
                            if (j != indexInfos.size() - 1) {
                                columns.append(",");
                            }
                        }
                        indexData[2] = "[" + columns + "]";
                    } else {
                        System.out.println("索引信息为空，请检查！");
                    }
                }
            }
        }
        String[][] arrayData = new String[data.size()][3];
        PrintTableUtil.printTable(headers, data.toArray(arrayData));
        return super.visitShow_indexes(ctx);
    }

    /**
     * 将表中数据导出
     * @param ctx
     * @return
     */
    @Override
    public Object visitDump_data(SQLParser.Dump_dataContext ctx) {
        return super.visitDump_data(ctx);
    }

    /**
     * 添加主键
     * @param ctx
     * @return
     */
    @Override
    public Object visitAlter_table_add_pk(SQLParser.Alter_table_add_pkContext ctx) {
        String dbName = (String) memory.get(DB_NAME);
        List<TerminalNode> identifiers = ctx.Identifier();
        String tableName = identifiers.get(0).getText().toUpperCase();
        //1.判断表是否存在
        boolean tbExist = SqlParserUtil.isTbExist(dbName,tableName);
        if (!tbExist) {
            System.out.println(tableName+"不存在！");
            return null;
        }
        //2.判断主键是否存在
        //(1)判断表内是否已经存在主键
        List<Object[]> tableBaseColumns = SqlParserUtil.getTableColumns(dbName, tableName);
        String[] columnTypes = new String[tableBaseColumns.size()];
        int[] columnLength = new int[tableBaseColumns.size()];
        //计算多少个socket可以组成一页数据
        int socketByteLength = 0;
        for (int i = 0; i < tableBaseColumns.size(); i++) {
            Object[] column = tableBaseColumns.get(i);
            if (column[6] != null && (int)column[6]==1) {
                System.out.println("主键已存在！");
                return null;
            }
            columnTypes[i] = (String) column[2];
            columnLength[i] = (int) column[3];
            //每一列数据都加上type 14,length 4,isVariable 1,realLength 4
            socketByteLength += columnLength[i]+23;
        }
        //(2)判断索引名是否存在
        String pkIndexName = FileConstants.PK_DIR_NAME;
        if (identifiers.size()==2) {
            pkIndexName = identifiers.get(1).getText().toUpperCase();
        }
        boolean pkExist = SqlParserUtil.isIndexDirExist(dbName, tableName, pkIndexName);
        if (pkExist) {
            System.out.println("索引已存在！");
            return null;
        }
        //3.获取要生成索引的列
        List<TerminalNode> columnList = ctx.identifiers().Identifier();
        //索引元数据记录
        List<Object[]> indexColumnList = new ArrayList<>();
        for (TerminalNode column : columnList) {
            String columnName = column.getText().toUpperCase();
            //检查字段是否存在，存在则将列基本信息写入
            boolean columnExist = false;
            for (Object[] columnInfo : tableBaseColumns) {
                if (columnName.equals(columnInfo[1])) {
                    columnExist = true;
                    indexColumnList.add(columnInfo);
                    break;
                }
            }
            if (!columnExist) {
                System.out.println(columnName+"不存在！");
                return null;
            }
        }
        //4.获取需要记录的索引信息
        //(1)整理数据,修改字段信息
        String tableBaseFilePath = FileConstants.BASE_DIR_PATH+File.separator+dbName+
                File.separator+tableName+
                File.separator+FileConstants.TABLE_BASE_FILE_NAME;
        File tableBaseFile = new File(tableBaseFilePath);
        int tableBaseFileId = FileManager.openFile(tableBaseFile);
        int pageCount = (int)Math.ceil((float)tableBaseFile.length()/CONSTANTS.PAGE_SIZE);
        //一页一页的去读取数据,修改字段信息
        for (int i = 0; i < pageCount; i++) {
            BasePage basePage = FileManager.readPage(tableBaseFileId, i, BaseConstants.COLUMN_LENGTH, BaseConstants.TABLE_COLUMN_TYPES,null);
            BaseSocket[] socketList = basePage.getSocketList();
            if (socketList != null && socketList.length!=0) {
                for (BaseSocket socket : socketList) {
                    if (socket==null) {
                        continue;
                    }
                    //包含字段的基本信息 type,length,isVariable,realLength,value
                    List<Object[]> objectData = socket.getObjectData();
                    String columnName = (String) objectData.get(1)[4];
                    for (Object[] objects : indexColumnList) {
                        if (columnName.equals(objects[1])) {
                            //修改数据
                            objectData.get(6)[4] = 1;
                            //修改byte数组
                            List<byte[]> data = socket.getData();
                            byte[] oriBytes = data.get(6);
                            byte[] originSplitBytes = ByteArrayUtil.subByte(oriBytes, 0, 23);
                            byte[] valueByte = ByteArrayUtil.intToByte(1);
                            data.set(6,ByteArrayUtil.byteMergerAll(originSplitBytes,valueByte));
                            //标记dirty
                            CacheManager.markDirty(tableBaseFileId,i);
                            break;
                        }
                    }
                }
            }
        }
        //(2)记录索引信息
        //创建文件
        String indexDirPath = FileConstants.BASE_DIR_PATH+File.separator+dbName+
                File.separator+tableName+
                File.separator+FileConstants.INDEXES_DIR_NAME+
                File.separator+pkIndexName;
        String indexBaseFilePath = indexDirPath+
                File.separator+FileConstants.INDEX_BASE_FILE_NAME;
        String indexDataFilePath = indexDirPath+File.separator+pkIndexName+FileConstants.TABLE_DATA_FILE_EX;
        FileUtil.createFile(indexBaseFilePath);
        FileUtil.createFile(indexDataFilePath);
        int baseFileIndex = FileManager.openFile(indexBaseFilePath);
        int indexDataFileId = FileManager.openFile(indexDataFilePath);
        //记录主键中列的信息
        List<Object[]> indexBaseInfo = new ArrayList<>();
        int[] indexColumnIds = new int[indexColumnList.size()];
        for (int i = 0; i < indexColumnList.size(); i++) {
            Object[] indexColumns = indexColumnList.get(i);
            Object[] baseInfo = new Object[BaseConstants.INDEX_COLUMNS.length];
            //列名
            baseInfo[1] = indexColumns[1];
            //columnId
            baseInfo[2] = indexColumns[0];
            //是否是主键
            baseInfo[3] = 1;
            //是否是索引
            baseInfo[4] = 0;
            //是否是复合约束
            baseInfo[5] = indexColumnList.size()>1 ? 1 : 0;
            //是否是外键
            baseInfo[6] = 0;
            indexBaseInfo.add(baseInfo);
            indexColumnIds[i] = (int) baseInfo[2];
        }
        //5.写入索引中各列的基本信息
        boolean insertDataFlag = CacheManager.addPagesByValue(baseFileIndex, indexBaseInfo, BaseConstants.INDEX_COLUMN_TYPES, BaseConstants.INDEX_COLUMN_LENGTH, BaseConstants.VARIABLE_INDEX_FLAG);
        if (!insertDataFlag) {
            System.out.println("写入数据失败!");
            //删除所创建的文件
            FileManager.closeFile(baseFileIndex);
            FileUtil.removeFile(indexDirPath);
            return null;
        }
        //todo 将表中的数据写入索引文件中
        //6.将表中的数据写入索引文件中，按照主键的顺序写入
//        String tableDataFile = FileConstants.BASE_DIR_PATH+File.separator+dbName+File.separator+tableName+
//                File.separator+FileConstants.TABLE_DATA_DIR_NAME+
//                File.separator+tableName+FileConstants.TABLE_DATA_FILE_EX;
//        File tbDataFile = new File(tableDataFile);
//        int tbDataFileId = FileManager.openFile(tbDataFile);
//        int maxPageId = FileManager.getMaxPageId(tbDataFileId);
        //7.定义B+树实现排序功能
//        BPlusTree bPlusTree = new BPlusTree();
//        Map<String,BPlusTreeLeafNode> pkValue = new HashMap<>();
//        for (int i = 0; i <= maxPageId; i++) {
//            BasePage basePage = FileManager.readPage(tbDataFileId, i, columnLength, columnTypes, null);
//            BaseSocket[] socketList = basePage.getSocketList();
//            if (socketList!=null && socketList.length!=0) {
//                for (int j = 0; j < socketList.length; j++) {
//                    BaseSocket socket = socketList[j];
//                    if (socket==null) {
//                        continue;
//                    }
//                    List<Object[]> objectData = socket.getObjectData();
//                    BPlusTreeLeafNode node = new BPlusTreeLeafNode(indexColumnIds,objectData,socket.getData());
//                    StringBuilder key = new StringBuilder();
//                    for (int indexColumnId : indexColumnIds) {
//                        key.append(objectData.get(indexColumnId)[4]).append("_");
//                    }
//                    if (pkValue.get(key.toString())!=null) {
//                        System.out.println("主键冲突，请检查！");
//                        return null;
//                    }
//                    pkValue.put(key.toString(),node);
//                    bPlusTree.insert(node,key.toString());
//                }
//            }
//        }
        //8.记录索引树信息
//        Map<String, TableIndexDataInfo> indexTreeMap = tableIndexTree.computeIfAbsent(tableName, v->new HashMap<>());
//        TableIndexDataInfo dataInfo = new TableIndexDataInfo();
//        dataInfo.setbPlusTree(bPlusTree);
//        dataInfo.setIndexDataFileId(indexDataFileId);
//        dataInfo.setSockedByteLength(socketByteLength);
//        dataInfo.setColumnIds(indexColumnIds);
//        dataInfo.setPrimaryKey(true);
//        dataInfo.setPkValue(pkValue);
//        dataInfo.markDirty();
//        indexTreeMap.put(pkIndexName,dataInfo);
        return super.visitAlter_table_add_pk(ctx);
    }

    /**
     * 对查询结果进行筛选 （目前只支持两个表查询）
     * （1）先处理字段等于某个值的情况；（2）再处理等于某个字段的情况
     * @param ctx
     * @return
     */
    @Override
    public Object visitWhere_operator_expression(SQLParser.Where_operator_expressionContext ctx) {
        //1.判断当前是查询、删除还是修改
        String operateType = CONSTANTS.SELECT;
        if (ctx.getParent().getParent() instanceof SQLParser.Delete_from_tableContext) {
            operateType = CONSTANTS.DELETE;
        } else if (ctx.getParent().getParent() instanceof SQLParser.Update_tableContext) {
            operateType = CONSTANTS.UPDATE;
        }
        List<TerminalNode> terminalNodes = ctx.column().Identifier();
        //验证条件是否正确
        Object[] leftColumn = null;
        String leftTableName = "";
        if (terminalNodes.size()==1) {
            String columnName = terminalNodes.get(0).getText().toUpperCase();
            //判断字段是否正确
            boolean columnExist = false;
            for (String tableName : tableBaseMap.keySet()) {
                Map<String, Object[]> columnMap = tableBaseMap.get(tableName);
                if (columnMap.get(columnName) != null) {
                    if (columnExist) {
                        System.out.println(columnName+"无法确定表名");
                        return null;
                    } else {
                        columnExist = true;
                        leftTableName = tableName;
                        leftColumn = columnMap.get(columnName);
                    }
                }
            }
            if(!columnExist) {
                System.out.println("未找到字段："+columnName);
                return null;
            }
        } else {
            String tableName = terminalNodes.get(0).getText().toUpperCase();
            String columnName = terminalNodes.get(1).getText().toUpperCase();
            //判断表和字段是否输入正确
            Map<String, Object[]> pgClassMap = tableBaseMap.get(tableName);
            if (CollectionUtils.isEmpty(pgClassMap)) {
                System.out.println(tableName+"未指定！");
                return null;
            }
            leftColumn = pgClassMap.get(columnName);
            if (leftColumn == null) {
                System.out.println(columnName+"列名不存在！");
                return null;
            }
            leftTableName = tableName;
        }
        SQLParser.ExpressionContext expression = ctx.expression();
        SQLParser.ColumnContext columnContext = expression.column();
        //(1)等于某个值
        if (columnContext == null || columnContext.isEmpty()) {
            Object value = null;
            try {
                String type = (String) leftColumn[2];
                if (type.contains(CONSTANTS.VARCHAR)) {
                    value = expression.value().getText().replace("'", "");
                    String str = (String) value;
                    if (str.getBytes().length > (int)leftColumn[3]) {
                        return null;
                    }
                } else if (type.equals(CONSTANTS.FLOAT)) {
                    value = Double.valueOf(expression.value().getText());
                } else if (type.equals(CONSTANTS.INT)) {
                    value = Integer.valueOf(expression.value().getText());
                }
            }catch (Exception e) {
                System.out.println("类型异常！查询出错！");
                return null;
            }
            //当为查询的时候，需要一步步对数据进行过滤
            if (CONSTANTS.SELECT.equals(operateType)) {
                List<List<Object[]>> newTableValue = SqlParserUtil.getEqualedValue(tableDataMap.get(leftTableName), (int) leftColumn[0], value);
                tableDataMap.put(leftTableName,newTableValue);
            }else {
                equalValueMap.put(leftTableName+"."+leftColumn[0],value);
            }
        } else {
            //(2)等于另一个字段
            //①判断字段的有效性
            String rightTable = "";
            Object[] rightColumn = null;
            terminalNodes = columnContext.Identifier();
            if (terminalNodes.size()==1) {
                String columnName = terminalNodes.get(0).getText().toUpperCase();
                //判断字段是否正确
                boolean columnExist = false;
                for (String tableName : tableBaseMap.keySet()) {
                    Map<String, Object[]> columnMap = tableBaseMap.get(tableName);
                    if (columnMap.get(columnName) != null) {
                        if (columnExist) {
                            System.out.println(columnName+"无法确定表名");
                            return null;
                        } else {
                            columnExist = true;
                            rightTable = tableName;
                            rightColumn = columnMap.get(columnName);
                        }
                    }
                }
            } else {
                String tableName = terminalNodes.get(0).getText().toUpperCase();
                String columnName = terminalNodes.get(1).getText().toUpperCase();
                //判断表和字段是否输入正确
                Map<String, Object[]> pgClassMap = tableBaseMap.get(tableName);
                if (CollectionUtils.isEmpty(pgClassMap)) {
                    System.out.println(tableName+"未指定！");
                    return null;
                }
                rightColumn = pgClassMap.get(columnName);
                if (leftColumn == null) {
                    System.out.println(columnName+"列名不存在！");
                    return null;
                }
                rightTable = tableName;
            }
            //②筛选数据
            if (CONSTANTS.SELECT.equals(operateType)) {
                SqlParserUtil.getTablesEqualedValue(tableDataMap,leftTableName,(int)leftColumn[0],rightTable,(int)rightColumn[0]);
            }
            Map<String,String> equalColumn = new HashMap<>();
            equalColumn.put(leftTableName+"."+leftColumn[0],rightTable+"."+rightColumn[0]);
            equalColumnMap.add(equalColumn);
        }
        return null;
    }

    @Override
    public Object visitWhere_operator_select(SQLParser.Where_operator_selectContext ctx) {
        return super.visitWhere_operator_select(ctx);
    }

    /**
     * 插入数据
     * todo 验证索引值重复
     * @param ctx
     * @return
     */
    @Override
    public Object visitInsert_into_table(SQLParser.Insert_into_tableContext ctx) {
        String dbName = (String) memory.get(DB_NAME);
        String tableName = ctx.Identifier().getText().toUpperCase();
        boolean tbExist = SqlParserUtil.isTbExist(dbName, tableName);
        if (!tbExist) {
            System.out.println("表不存在！");
            return null;
        }
        //1.检查数据类型是否正确
        List<Object[]> columns = SqlParserUtil.getTableColumns(dbName,tableName);
        SQLParser.Value_listsContext valueLists = ctx.value_lists();
        List<SQLParser.Value_listContext> values = valueLists.value_list();
        //-----------缓存写入需要记录的信息--------------

        //(1)列存储的数据类型
        String[] columnType = new String[columns.size()];
        //(2)字段长度
        int[] columnLength = new int[columns.size()];
        //(3)是否变长
        byte[] variableByte = new byte[columns.size()];
        //(4)主键中对应列的下标
        List<Integer> primaryKeyIds = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            Object[] objData = columns.get(i);
            columnType[i] = (String) objData[2];
            columnLength[i] = (int) objData[3];
            //目前只有VARCHAR是变长的
            if (ByteArrayUtil.strEquals(columnType[i],CONSTANTS.VARCHAR)) {
                variableByte[i] = 1;
            } else {
                variableByte[i] = 0;
            }
            if (objData[6] != null && (int)objData[6] == 1) {
                primaryKeyIds.add((Integer) objData[0]);
            }
        }
        //(5)将要写入数据库的数据
        List<Object[]> insertValues = new ArrayList<>();
        List<byte[]> insertByteValues = new ArrayList<>();
        for (SQLParser.Value_listContext value : values) {
            List<SQLParser.ValueContext> valueList = value.value();
            if (valueList.size() != columns.size()) {
                System.out.println("字段数量不对，请检查！");
                return null;
            }
            //一条数据记录
            Object[] columnData = new Object[columns.size()];
            byte[] byteData = null;
            for (int i = 0; i < valueList.size(); i++) {
                Object[] field = columns.get(i);
                String type = (String) field[2];
                byte[] typeByte = type.getBytes();
                //如果为NULL，判断是否可以为NULL
                SQLParser.ValueContext valueContext = valueList.get(i);
                if (CONSTANTS.NULL.equals(valueContext.getText().toUpperCase())) {
                    if (CONSTANTS.FALSE == (int)field[4]) {
                        System.out.println(field[1]+"不能为null");
                        return null;
                    } else {
                        columnData[i] = null;
                        continue;
                    }
                }
                byte[] lengthByte = ByteArrayUtil.intToByte((int)field[3]);
                byte[] isVariableByte = new byte[]{0};
                byte[] realLength = ByteArrayUtil.intToByte(4);
                int realByteLength = 4;
                //非NULL数据，类型转换
                if (type.contains(CONSTANTS.VARCHAR)) {
                    isVariableByte = new byte[]{1};
                    String fieldValue = valueContext.String().getText();
                    fieldValue = fieldValue.replace("'","");
                    int length = fieldValue.getBytes().length;
                    if (length > (int)field[3]) {
                        System.out.println(fieldValue + "字段过长！");
                        return null;
                    }
                    realLength = ByteArrayUtil.intToByte(length);
                    realByteLength = length;
                    columnData[i] = fieldValue;
                } else if (CONSTANTS.INT.equals(type)) {
                    int fieldValue = Integer.parseInt(valueContext.Integer().getText());
                    columnData[i] = fieldValue;
                } else {
                    float fieldValue = Float.parseFloat(valueContext.Float().getText());
                    columnData[i] = fieldValue;
                }
                byte[] valueByte = ByteArrayUtil.objectToBytes(columnData[i],type);
                byte[] completeByte = new byte[(int)field[3]-realByteLength];
                byteData = ByteArrayUtil.byteMergerAll(byteData,typeByte,lengthByte,isVariableByte,realLength,valueByte,completeByte);
            }
            insertByteValues.add(byteData);
            insertValues.add(columnData);
        }
        //2.获取即将填充数据的文件
        String dataFilePath = FileConstants.BASE_DIR_PATH + File.separator + dbName
                + File.separator + tableName + File.separator + FileConstants.TABLE_DATA_DIR_NAME
                + File.separator + tableName + FileConstants.TABLE_DATA_FILE_EX;
        File dataFile = new File(dataFilePath);
        if (!dataFile.exists()) {
            FileUtil.createFile(dataFilePath);
        }
        int fileId = FileManager.openFile(dataFilePath);
        //(1)todo 查询出所有的索引树
        if (primaryKeyIds.size()!=0) {
            //(2)将数据插入索引树
            //(3)标记dirty
            Map<String, TableIndexDataInfo> tableTree = tableIndexTree.get(tableName);
            if (tableTree!=null) {
                for (String indexName : tableTree.keySet()) {
                    TableIndexDataInfo dataInfo = tableTree.get(indexName);
                    if (dataInfo.isPrimaryKey()) {
                        BPlusTree bPlusTree = dataInfo.getbPlusTree();
                        BPlusTreeLeafNode node = new BPlusTreeLeafNode(dataInfo.getColumnIds(),insertValues,insertByteValues);
                        StringBuilder key = new StringBuilder();
                        for (int indexColumnId : dataInfo.getColumnIds()) {
                            key.append(insertValues.get(indexColumnId)[4]).append("_");
                        }
                        Map<String, BPlusTreeLeafNode> pkValue = dataInfo.getPkValue();
                        if (pkValue.get(key.toString())!=null) {
                            System.out.println("主键冲突，请检查！");
                            return null;
                        }
                        pkValue.put(key.toString(),node);
                        bPlusTree.insert(node,key.toString());
                        dataInfo.markDirty();
                    }
                }
            }
        }
        //插入表的数据文件数据
        CacheManager.addPagesByValue(fileId,insertValues,columnType,columnLength,variableByte);
        return super.visitInsert_into_table(ctx);
    }

    /**
     * 可以出现相同的值
     * @param ctx
     * @return
     */
    @Override
    public Object visitAlter_add_index(SQLParser.Alter_add_indexContext ctx) {
//        List<String> paths = new ArrayList<>();
//        String dbName = (String) memory.get(DB_NAME);
//        String tableName = ctx.Identifier().getText().toUpperCase();
//        //1.判断表是否存在
//        boolean tbExist = SqlParserUtil.isTbExist(dbName,tableName);
//        if (!tbExist) {
//            System.out.println(tableName+"不存在！");
//            return null;
//        }
//        paths.add(dbName);
//        paths.add(tableName);
//        //2.获取要生成索引的列
//        List<String> columns = new ArrayList<>();
//        List<TerminalNode> columnList = ctx.identifiers().Identifier();
//        //生成索引名
//        StringBuilder newIndexName = new StringBuilder();
//        for (TerminalNode column : columnList) {
//            String columnName = column.getText().toUpperCase();
//            //检查字段是否存在
//            boolean columnExist = SqlParserUtil.isColumnExist(dbName, tableName, columnName);
//            if (!columnExist) {
//                System.out.println(columnName+"不存在！");
//                return null;
//            }
//            columns.add(columnName);
//            newIndexName.append(columnName).append("_");
//        }
//        newIndexName.append("INDEX");
//        //当前表的所有索引信息
//        Map<String, List<PgClass>> indexesMap = SqlParserUtil.getIndexInfosByTable(dbName,tableName);
//        //3.判断是否存在重复创建索引的情况
//        if (!CollectionUtils.isEmpty(indexesMap)) {
//            for (String indexName : indexesMap.keySet()) {
//                List<PgClass> columnBaseList = indexesMap.get(indexName);
//                if(columnBaseList.size() == columns.size()) {
//                    //判断这些字段是否已经创建过索引了
//                    boolean isMatched = false;
//                    for (PgClass pgClass : columnBaseList) {
//                        for (String column : columns) {
//                            if (pgClass.getRelName().equals(column)) {
//                                isMatched = true;
//                                break;
//                            }
//                        }
//                        if (!isMatched) {
//                            break;
//                        }
//                    }
//                    if(isMatched) {
//                        System.out.println("当前字段已经创建了索引！");
//                        return null;
//                    }
//                }
//            }
//        }
//        //4.缓存写入
//        TablePage tablePage = new TablePage();
//        tablePage.setName(tableName);
//        tablePage.setDbName(dbName);
//        IndexPage indexPage = new IndexPage();
//        indexPage.setIndexName(newIndexName.toString());
//        indexPage.setTablePage(tablePage);
//        indexPage.setColumns(columns);
//        indexPage.setName(KeysUtil.createIndexKey(indexPage));
//
//        List<PgClass> tableBaseInfo = SqlParserUtil.getTableByPath(paths);
//        List<BaseSocket> sockets = new ArrayList<>();
//        for (PgClass pgClass : tableBaseInfo) {
//            for (String column : columns) {
//                if (pgClass.getRelName().equals(column)) {
//                    BaseSocket socket = new BaseSocket();
//                    socket.setObject(pgClass);
//                    sockets.add(socket);
//                }
//            }
//        }
//        indexPage.setSocketList(sockets);
//        CacheManager.addPage(indexPage);
//        //5.数据记录到文件
//        SqlParserUtil.addIndexToFile(dbName,tableName,newIndexName.toString(),columns);
//        return super.visitAlter_add_index(ctx);
        return null;
    }

    @Override
    public Object visitAlter_drop_index(SQLParser.Alter_drop_indexContext ctx) {
//        String dbName = (String) memory.get(DB_NAME);
//        //1.判断表是否存在
//        String tableName = ctx.Identifier().getText().toUpperCase();
//        boolean tbExist = SqlParserUtil.isTbExist(dbName, tableName);
//        if (!tbExist) {
//            System.out.println(tableName+"不存在！");
//            return null;
//        }
//        //2.判断索引是否存在
//        Set<String> indexNameList = new HashSet<>();
//        List<TerminalNode> terminalNodes = ctx.identifiers().Identifier();
//        for (TerminalNode terminalNode : terminalNodes) {
//            String indexName = terminalNode.getText().toUpperCase();
//            boolean indexExist = SqlParserUtil.isIndexExist(dbName, tableName, indexName);
//            if (!indexExist) {
//                System.out.println(indexName+"不存在！");
//                return null;
//            }
//            indexNameList.add(indexName);
//        }
//        //3.删除缓存中的索引信息 删除文件
//        Iterator<String> indexNames = indexNameList.iterator();
//        while(indexNames.hasNext()) {
//            List<String> paths = new ArrayList<>();
//            paths.add(dbName);
//            paths.add(tableName);
//            paths.add(indexNames.next());
//            CacheManager.removePages(CONSTANTS.INDEX,paths);
//            FileUtil.removeFileOrDir(CONSTANTS.INDEX,paths);
//        }
//        return super.visitAlter_drop_index(ctx);
        return null;
    }

    @Override
    public Object visitAlter_table_drop_pk(SQLParser.Alter_table_drop_pkContext ctx) {
        String dbName = (String) memory.get(DB_NAME);
        List<TerminalNode> terminalNodes = ctx.Identifier();
        String tableName = terminalNodes.get(0).getText().toUpperCase();
        //1.判断表名是否存在
        boolean tbExist = SqlParserUtil.isTbExist(dbName, tableName);
        if (!tbExist) {
            System.out.println("表名不存在！");
            return null;
        }
        //主键名默认为PK_INDEX
        String pkName = FileConstants.PK_DIR_NAME;
        if (terminalNodes.size()==2) {
            pkName = terminalNodes.get(1).getText().toUpperCase();
        }
        //2.判断主键是否存在
        boolean indexExist = SqlParserUtil.isIndexDirExist(dbName, tableName, pkName);
        if(!indexExist) {
            System.out.println(pkName+"不存在！");
            return null;
        }
        //3.修改表中的记录
        String tableBaseFilePath = FileConstants.BASE_DIR_PATH+File.separator+dbName+
                File.separator+tableName+
                File.separator+FileConstants.TABLE_BASE_FILE_NAME;
        File tableBaseFile = new File(tableBaseFilePath);
        int tableBaseFileId = FileManager.openFile(tableBaseFile);
        int pageCount = (int)Math.ceil((float)tableBaseFile.length()/CONSTANTS.PAGE_SIZE);
        //一页一页的去读取数据,修改字段信息
        for (int i = 0; i < pageCount; i++) {
            BasePage basePage = FileManager.readPage(tableBaseFileId, i, BaseConstants.COLUMN_LENGTH, BaseConstants.TABLE_COLUMN_TYPES,null);
            BaseSocket[] socketList = basePage.getSocketList();
            if (socketList != null && socketList.length!=0) {
                for (BaseSocket socket : socketList) {
                    if (socket==null) {
                        continue;
                    }
                    //包含字段的基本信息 type,length,isVariable,realLength,value
                    List<Object[]> objectData = socket.getObjectData();
                    //修改数据
                    objectData.get(6)[4] = 0;
                    //修改byte数组
                    List<byte[]> data = socket.getData();
                    byte[] oriBytes = data.get(6);
                    byte[] originSplitBytes = ByteArrayUtil.subByte(oriBytes, 0, 23);
                    byte[] valueByte = ByteArrayUtil.intToByte(0);
                    data.set(6,ByteArrayUtil.byteMergerAll(originSplitBytes,valueByte));
                    //标记dirty
                    CacheManager.markDirty(tableBaseFileId,i);
                    break;
                }
            }
        }
        //4.删除缓存中的索引信息;删除文件
        List<String> paths = new ArrayList<>();
        paths.add(dbName);
        paths.add(tableName);
        paths.add(pkName);
        FileUtil.removeFileOrDir(CONSTANTS.INDEX,paths);
        return null;
    }

    /**
     * 查询表中的数据
     * todo 暂不支持等于某个字段进行过滤的情况
     * @param ctx
     * @return
     */
    @Override
    public Object visitSelect_table_(SQLParser.Select_table_Context ctx) {
        String dbName = (String) memory.get(DB_NAME);
        SQLParser.Select_tableContext selectContext = ctx.select_table();
        //初始化缓存信息
        tableBaseMap = new HashMap<>();
        List<TerminalNode> identifier = selectContext.identifiers().Identifier();
        //1.获取from后面要查询的表名
        List<String> tableNames = new ArrayList<>();
        for (TerminalNode terminalNode : identifier) {
            String tableName = terminalNode.getText().toUpperCase();
            tableNames.add(tableName);
            //判断表是否存在
            boolean tbExist = SqlParserUtil.isTbExist(dbName, tableName);
            if (!tbExist) {
                System.out.println(tableName+"表不存在！");
                return null;
            }
        }
        //2.获取查询字段中要查询的表和字段
        Map<String,List<String>> tableColumnMap = new HashMap<>();
        //需要查询的字段下标
        Map<String,List<Integer>> tableColumnIdMap = new HashMap<>();
        Map<String,int[]> tableColumnLength = new HashMap<>();
        Map<String,String[]> tableColumnType = new HashMap<>();
        //表的基本信息
        for (String tableName : tableNames) {
            tableColumnMap.put(tableName,new ArrayList<>());
            //获取表的基本信息
            List<Object[]> fields = SqlParserUtil.getTableColumns(dbName,tableName);
            if (CollectionUtils.isEmpty(fields)) {
                System.out.println(tableName+"无基本信息，请检查！");
                return null;
            }
            //key:字段名  value:列中数据
            Map<String,Object[]> fieldNameMap = new HashMap<>();
            int[] columnLength = new int[fields.size()];
            String[] columnTypes = new String[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                Object[] field = fields.get(i);
                fieldNameMap.put((String)field[1],field);
                columnLength[i] = (int) field[3];
                columnTypes[i] = (String) field[2];
            }
            tableBaseMap.put(tableName,fieldNameMap);
            tableColumnLength.put(tableName,columnLength);
            tableColumnType.put(tableName,columnTypes);
        }
        //未指明表名的字段
        List<String> noTbColumn = new ArrayList<>();
        List<SQLParser.SelectorContext> selectors = selectContext.selectors().selector();
        for (SQLParser.SelectorContext selector : selectors) {
            SQLParser.ColumnContext columnContext = selector.column();
            List<TerminalNode> nodeList = columnContext.Identifier();
            //写了对应的表名
            if (nodeList.size()==2) {
                String tableName = nodeList.get(0).getText().toUpperCase();
                Map<String,Object[]> tableBaseInfoMap = tableBaseMap.get(tableName);
                String columnName = nodeList.get(1).getText().toUpperCase();
                List<String> columnList = tableColumnMap.get(tableName);
                List<Integer> columnIdList = tableColumnIdMap.computeIfAbsent(tableName, v -> new ArrayList<>());
                if (columnList == null) {
                    System.out.println("查询的表名"+tableName+"不存在!");
                    return null;
                } else {
                    //判断列名是否存在
                    Object[] columnData = tableBaseInfoMap.get(columnName);
                    if (columnData== null) {
                        System.out.println(columnName+"列名不存在！");
                        return null;
                    }
                    columnList.add(columnName);
                    columnIdList.add((Integer) columnData[0]);
                }
                tableColumnMap.put(tableName,columnList);
            } else if (nodeList.size()==1) {
                //没有写表名
                noTbColumn.add(nodeList.get(0).getText().toUpperCase());
            }
        }
        //3.判断没有指定表名的字段的有效性，同时将未指定表名的指定表名，添加到tableColumnMap
        if (!CollectionUtils.isEmpty(noTbColumn)) {
            for (String columnName : noTbColumn) {
                boolean columnExist = false;
                String tbName = null;
                for (String tableName : tableBaseMap.keySet()) {
                    Map<String, Object[]> columnMap = tableBaseMap.get(tableName);
                    if (columnMap.get(columnName) != null) {
                        if (columnExist) {
                            System.out.println(columnName+"无法确定表名");
                            return null;
                        } else {
                            columnExist = true;
                            tbName = tableName;
                        }
                    }
                }
                if(columnExist) {
                    List<String> columns = tableColumnMap.get(tbName);
                    columns.add(columnName);
                    tableColumnMap.put(tbName,columns);
                    List<Integer> columnIds = tableColumnIdMap.computeIfAbsent(tbName, v -> new ArrayList<>());
                    columnIds.add((Integer) tableBaseMap.get(tbName).get(columnName)[0]);
                } else {
                    System.out.println(columnName+"列名不存在！");
                    return null;
                }
            }
        }
        //4.查询的表中的所有数据
        tableDataMap = new HashMap<>();
        for (String tableName : tableColumnMap.keySet()) {
            List<List<Object[]>> columnDataList = new ArrayList<>();
            tableDataMap.put(tableName,columnDataList);
            String tbDataFilePath = FileConstants.BASE_DIR_PATH+File.separator+dbName+
                    File.separator+tableName+
                    File.separator+FileConstants.TABLE_DATA_DIR_NAME+
                    File.separator+tableName+FileConstants.TABLE_DATA_FILE_EX;
            File tbDataFile = new File(tbDataFilePath);
            int fileId = FileManager.openFile(tbDataFile);
            int maxPageId = FileManager.getMaxPageId(fileId);
            for (int i = 0; i <= maxPageId; i++) {
                BasePage basePage = FileManager.readPage(fileId, i, tableColumnLength.get(tableName),
                        tableColumnType.get(tableName), null);
                //只获取需要获取的字段
                for (BaseSocket socket : basePage.getSocketList()) {
                    if (socket==null) {
                        continue;
                    }
                    //原始数据
                    List<Object[]> objectValue = socket.getObjectData();
                    columnDataList.add(objectValue);
                }
            }
        }
        if (selectContext.where_and_clause() != null) {
            executeWhereClause(selectContext.where_and_clause());
        }
        //5.将查询结果进行整合
        List<Object[]> tableDataList = new ArrayList<>();
        //（1）将newDataMap按照展示的表的顺序去重组
        Map<String,List<List<Object[]>>> reOrgTableMap = new HashMap<>();
        for (String tableName : tableColumnIdMap.keySet()) {
            reOrgTableMap.put(tableName,tableDataMap.get(tableName));
        }
        if (CollectionUtils.isEmpty(equalColumnMap)) {
            //按照展示字段的顺序去获取数据
            Map<String,List<Object[]>> resultDataMap = new HashMap<>();
            for (String tableName : reOrgTableMap.keySet()) {
                List<List<Object[]>> objects = reOrgTableMap.get(tableName);
                //没有可以展示的数据
                if (CollectionUtils.isEmpty(objects)) {
                    return null;
                }
                //只取查询条件中的数据
                List<Object[]> data = new ArrayList<>();
                List<Integer> columnIds = tableColumnIdMap.get(tableName);
                for (List<Object[]> object : objects) {
                    Object[] objectData = new Object[columnIds.size()];
                    for (int i = 0; i < columnIds.size(); i++) {
                        objectData[i] = object.get(columnIds.get(i))[4];
                    }
                    data.add(objectData);
                }
                resultDataMap.put(tableName,data);
            }
            tableDataList = SqlParserUtil.combineTableData(resultDataMap);
        } else {
            //--------------将各个表的关联关系按照查询的顺序进行组合-----------------
            //key:tableName value:与前面所有的表的关联关系
            //                    key:tableName对应的columnId value:关联表的tableName+"."+关联表的columnId
            Map<String,List<Map<Integer,String>>> combinedTableMap = new HashMap<>();
            //记录当前循环到第几个表
            int tableIndex = 0;
            List<String> tableNameList = new ArrayList<>();
            for (String tableName : reOrgTableMap.keySet()) {
                tableNameList.add(tableName);
                List<Map<Integer,String>> equalConditions = new ArrayList<>();
                combinedTableMap.put(tableName,equalConditions);
                if (tableIndex==0) {
                    tableIndex++;
                    continue;
                }
                //记录已经匹配过的数据，然后将其从equalColumnMap中删除
                List<Map<String,String>> isMatchedCondition = new ArrayList<>();
                //获取当前表与前面的表的关联关系
                for (Map<String, String> equalColumn : equalColumnMap) {
                    //equalColumn只有一条数据，只是记录了对应关系
                    for (String tableAndColumn : equalColumn.keySet()) {
                        String[] split = tableAndColumn.split("\\.");
                        String leftTableName = split[0];
                        Integer leftColumnId = Integer.valueOf(split[1]);
                        String rightTableColumn = equalColumn.get(tableAndColumn);
                        String[] split1 = rightTableColumn.split("\\.");
                        String rightTableName = split1[0];
                        Integer rightColumnId = Integer.valueOf(split1[1]);
                        if (leftTableName.equals(tableName)) {
                            for (int i = 0; i < tableNameList.size()-1; i++) {
                                String beforeTableName = tableNameList.get(i);
                                if (rightTableName.equals(beforeTableName)) {
                                    isMatchedCondition.add(equalColumn);
                                    Map<Integer,String> equalMap = new HashMap<>();
                                    equalMap.put(leftColumnId,rightTableColumn);
                                    equalConditions.add(equalMap);
                                    break;
                                }
                            }
                        } else if (rightTableName.equals(tableName)){
                            for (int i = 0; i < tableNameList.size()-1; i++) {
                                String beforeTableName = tableNameList.get(i);
                                if (leftTableName.equals(beforeTableName)) {
                                    isMatchedCondition.add(equalColumn);
                                    Map<Integer,String> equalMap = new HashMap<>();
                                    equalMap.put(rightColumnId,tableAndColumn);
                                    equalConditions.add(equalMap);
                                    break;
                                }
                            }
                        }
                    }
                }
                equalColumnMap.removeAll(isMatchedCondition);
                tableIndex++;
            }
            //---------------根据combinedTableMap将数据进行组合--------------------
            List<Map<String,List<Object[]>>> tableDataCombine = new ArrayList<>();
            tableIndex = 0;
            for (String tableName : combinedTableMap.keySet()) {
                List<List<Object[]>> columnDataList = tableDataMap.get(tableName);
                if (tableIndex==0) {
                    //将第一个表中的数据全部插入
                    for (List<Object[]> columnData : columnDataList) {
                        Map<String, List<Object[]>> columnMap = new HashMap<>();
                        columnMap.put(tableName, columnData);
                        tableDataCombine.add(columnMap);
                    }
                    tableIndex++;
                    continue;
                }
                List<Map<Integer, String>> conditions = combinedTableMap.get(tableName);
                for (List<Object[]> columnData : columnDataList) {
                    //一个condition里面只存储了一条数据
                    for (Map<Integer, String> condition : conditions) {
                        for (Integer columnId : condition.keySet()) {
                            Object[] columnValue = columnData.get(columnId);
                            String tableAndColumn = condition.get(columnId);
                            String[] split = tableAndColumn.split("\\.");
                            String equalTableName = split[0];
                            int equalColumnId = Integer.parseInt(split[1]);
                            //最终组合之后的数据
                            List<Map<String,List<Object[]>>> combineColumnDataMap = new ArrayList<>();
//                          //用于将原本存在tableDataCombine中，但是重新添加匹配的数据找出来，用于将原来的数据删除
                            List<Map<String,List<Object[]>>> matchedData = new ArrayList<>();
                            for (Map<String, List<Object[]>> resultData : tableDataCombine) {
                                List<Object[]> beforeColumnData = resultData.get(equalTableName);
                                Object[] equalData = beforeColumnData.get(equalColumnId);
                                if (ByteArrayUtil.isEqualed(columnValue,equalData)) {
                                    matchedData.add(resultData);
                                    Map<String,List<Object[]>> dataMap = new HashMap<>();
                                    dataMap.putAll(resultData);
                                    dataMap.put(tableName,columnData);
                                    combineColumnDataMap.add(dataMap);
                                }
                            }
                            for (Map<String, List<Object[]>> matchedDatum : matchedData) {
                                tableDataCombine.remove(matchedDatum);
                            }
                            tableDataCombine.addAll(combineColumnDataMap);
                        }
                    }
                }
                tableIndex++;
            }
            //------------------------获取最终要展示的数据------------------------
            for (Map<String, List<Object[]>> columnData : tableDataCombine) {
                List<Object> showColumnData = new ArrayList<>();
                for (String tableName : columnData.keySet()) {
                    List<Object[]> tableData = columnData.get(tableName);
                    List<Integer> columnIds = tableColumnIdMap.get(tableName);
                    Object[] objectData = new Object[columnIds.size()];
                    for (int i = 0; i < columnIds.size(); i++) {
                        objectData[i] = tableData.get(columnIds.get(i))[4];
                    }
                    showColumnData.addAll(Arrays.asList(objectData));
                }
                tableDataList.add(showColumnData.toArray());
            }
        }
        //6.显示结果
        //(1)表头显示的列数据
        List<String> columnList = new ArrayList<>();
        for (String tableName : tableColumnMap.keySet()) {
            List<String> columns = tableColumnMap.get(tableName);
            for (String column : columns) {
                String columnName = tableName+"." +column;
                columnList.add(columnName);
            }
        }
        String[] headers = new String[columnList.size()];
        for (int i = 0; i < columnList.size(); i++) {
            headers[i] = columnList.get(i);
        }
        //(2)数据列表信息
        if (!CollectionUtils.isEmpty(tableDataList)) {
            String[][] data = new String[tableDataList.size()][columnList.size()];
            for (int i = 0; i < tableDataList.size(); i++) {
                Object[] columnData = tableDataList.get(i);
                for (int j = 0; j < columnData.length; j++) {
                    if (columnData[j]==null) {
                        data[i][j] = "<NULL>";
                    } else {
                        data[i][j] = String.valueOf(columnData[j]);
                    }
                }
            }
            PrintTableUtil.printTable(headers,data);
        }
        return null;
    }

    /**
     * 删除表中数据，todo 删除索引文件中的数据
     * @param ctx
     * @return
     */
    @Override
    public Object visitDelete_from_table(SQLParser.Delete_from_tableContext ctx) {
        String dbName = (String) memory.get(DB_NAME);
        String tableName = ctx.Identifier().getText().toUpperCase();
        //初始化要缓存信息
        tableBaseMap = new HashMap<>();
        //1.------------------获取表中列的基本信息-----------------
        List<Object[]> tableData = SqlParserUtil.getTableColumns(dbName,tableName);
        if (CollectionUtils.isEmpty(tableData)) {
            return null;
        }
        Map<String,Object[]> columnDataMap = new HashMap<>();
        for (Object[] columnData : tableData) {
            columnDataMap.put((String) columnData[1],columnData);
        }
        tableBaseMap.put(tableName,columnDataMap);
        //获取表中各字段长度和字段类型
        String[] columnTypes = new String[tableData.size()];
        int[] columnLengths = new int[tableData.size()];
        for (int i = 0; i < tableData.size(); i++) {
            Object[] baseData = tableData.get(i);
            columnLengths[i] = (int) baseData[3];
            columnTypes[i] = (String) baseData[2];
        }
        //2.------------------获取需要删除的条件------------------
        executeWhereClause(ctx.where_and_clause());
        //3.------------------过滤数据，记录缓存------------------
        String tbDataFilePath = FileConstants.BASE_DIR_PATH+File.separator+dbName+
                File.separator+tableName+
                File.separator+FileConstants.TABLE_DATA_DIR_NAME+
                File.separator+tableName+FileConstants.TABLE_DATA_FILE_EX;
        File tbDataFile = new File(tbDataFilePath);
        int fileId = FileManager.openFile(tbDataFile);
        int maxPageId = FileManager.getMaxPageId(fileId);
        for (int i = 0; i <= maxPageId; i++) {
            BasePage basePage = FileManager.readPage(fileId, i, columnLengths, columnTypes, null);
            BaseSocket[] socketList = basePage.getSocketList();
            for (int j = 0; j < socketList.length; j++) {
                BaseSocket socket = socketList[j];
                if (socket != null) {
                    List<Object[]> objectData = socket.getObjectData();
                    //若匹配了则修改当前数据，记录为dirty
                    if (isWhereMatched(objectData)) {
                        //删除socket
                        socketList[j] = null;
                        //标记dirty
                        CacheManager.markDirty(fileId, i);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 修改数据，todo 修改索引文件中的数据
     * @param ctx
     * @return
     */
    @Override
    public Object visitUpdate_table(SQLParser.Update_tableContext ctx) {
        String dbName = (String) memory.get(DB_NAME);
        String tableName = ctx.Identifier().getText().toUpperCase();
        //1.获取表的基本信息
        Map<String,Object[]> baseMap = new HashMap<>();
        tableBaseMap.put(tableName,baseMap);
        //表中各列的基本信息
        List<Object[]> tableBaseList = SqlParserUtil.getTableColumns(dbName,tableName);
        int[] columnLength = new int[tableBaseList.size()];
        String[] columnTypes = new String[tableBaseList.size()];
        for (int i = 0; i < tableBaseList.size(); i++) {
            Object[] columnBaseInfo = tableBaseList.get(i);
            baseMap.put((String) columnBaseInfo[1],columnBaseInfo);
            columnLength[i] = (int) columnBaseInfo[3];
            columnTypes[i] = (String) columnBaseInfo[2];
        }
        //2.判断表是否存在
        boolean tbExist = SqlParserUtil.isTbExist(dbName, tableName);
        if (!tbExist) {
            System.out.println(tableName+"表不存在！");
            return null;
        }
        //3.获取修改的字段
        List<TerminalNode> columns = ctx.set_clause().Identifier();
        List<SQLParser.ValueContext> values = ctx.set_clause().value();
        //set数据记录 key:columnId
        Map<Integer,Object> equalSetMap = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i).getText().toUpperCase();
            //3.判断字段是否存在
            boolean columnExist = SqlParserUtil.isColumnExist(dbName, tableName, column);
            if (!columnExist) {
                System.out.println(column+"不存在！");
                return null;
            }
            //修改类型
            Object[] tableColumns = baseMap.get(column);
            Object value = null;
            if (ByteArrayUtil.strEquals(CONSTANTS.INT, (String) tableColumns[2])) {
                value = Integer.valueOf(values.get(i).getText());
            } else if (ByteArrayUtil.strEquals(CONSTANTS.FLOAT, (String) tableColumns[2])) {
                value = Float.valueOf(values.get(i).getText());
            } else {
                value = values.get(i).getText().replace("'","");
            }
            equalSetMap.put((Integer) tableColumns[0],value);
        }
        //4.获取需要过滤的条件
        SQLParser.Where_and_clauseContext whereContext = ctx.where_and_clause();
        executeWhereClause(whereContext);
        //5.根据条件获取需要修改的数据信息 key：pageId,value:socketId
        String tableDataFilePath = FileConstants.BASE_DIR_PATH+File.separator+dbName+
                File.separator+tableName+
                File.separator+FileConstants.TABLE_DATA_DIR_NAME+
                File.separator+tableName+FileConstants.TABLE_DATA_FILE_EX;
        File tbDataFile = new File(tableDataFilePath);
        int fileId = FileManager.openFile(tbDataFile);
        int maxPageId = FileManager.getMaxPageId(fileId);
        for (int i = 0; i <= maxPageId; i++) {
            BasePage pageInfo = FileManager.readPage(fileId, i, columnLength, columnTypes, null);
            BaseSocket[] socketList = pageInfo.getSocketList();
            for (int j = 0; j < socketList.length; j++) {
                BaseSocket socket = socketList[j];
                if (socket != null) {
                    List<Object[]> objectBaseData = socket.getObjectData();
                    //若匹配了则修改当前数据，记录为dirty
                    if (isWhereMatched(objectBaseData)) {
                        //修改data和objectData
                        List<byte[]> dataList = socket.getData();
                        for (Integer columnId : equalSetMap.keySet()) {
                            //修改objectData
                            Object[] data = objectBaseData.get(columnId);
                            Object value = equalSetMap.get(columnId);
                            String type = columnTypes[columnId];
                            byte[] bytes = ByteArrayUtil.objectToBytes(value, type);
                            data[3] = bytes.length;
                            data[4] = value;
                            //修改data
                            byte[] bytesData = dataList.get(columnId);
                            byte[] originSplitData = ByteArrayUtil.subByte(bytesData, 0, 19);
                            byte[] realLengthByte = ByteArrayUtil.intToByte(bytes.length);
                            int length = (int) data[1];
                            byte[] completeByte = new byte[length - bytes.length];
                            dataList.set(columnId, ByteArrayUtil.byteMergerAll(originSplitData, realLengthByte, bytes, completeByte));
                        }
                        //标记dirty
                        CacheManager.markDirty(fileId, i);
                    }
                }
            }
        }
        return super.visitUpdate_table(ctx);
    }

    /**
     * 执行where过滤数据
     * todo 判断是否有用到索引列
     * @param where_and_clauseContext
     * @return
     */
    public Object executeWhereClause(SQLParser.Where_and_clauseContext where_and_clauseContext) {
        List<SQLParser.Where_clauseContext> whereContexts = where_and_clauseContext.where_clause();
        if (!CollectionUtils.isEmpty(whereContexts)) {
            List<SQLParser.Where_operator_expressionContext> equalContexts = new ArrayList<>();
            List<SQLParser.Where_operator_expressionContext> equalColumnContexts = new ArrayList<>();
            for (SQLParser.Where_clauseContext whereContext : whereContexts) {
                //（1）对等于某个值的条件进行筛选
                //(2)对等于某个字段进行筛选
                if (whereContext instanceof SQLParser.Where_operator_expressionContext) {
                    SQLParser.Where_operator_expressionContext context = (SQLParser.Where_operator_expressionContext) whereContext;
                    if (context.expression().value() == null) {
                        //等于某个字段
                        equalColumnContexts.add(context);
                    } else {
                        //等于某个值
                        equalContexts.add(context);
                    }
                } else if (whereContext instanceof SQLParser.Where_operator_selectContext) {
                    visitWhere_operator_select((SQLParser.Where_operator_selectContext) whereContext);
                }
            }
            //1.筛选等于某个值
            if (!CollectionUtils.isEmpty(equalContexts)) {
                for (SQLParser.Where_operator_expressionContext equalContext : equalContexts) {
                    visitWhere_operator_expression(equalContext);
                }
            }
            //2.筛选字段相等
            if (!CollectionUtils.isEmpty(equalColumnContexts)) {
                for (SQLParser.Where_operator_expressionContext equalColumnContext : equalColumnContexts) {
                    visitWhere_operator_expression(equalColumnContext);
                }
            }
        }
        return whereContexts;
    }

    /**
     * 判断socket是否满足where条件
     * @param objectBaseData socket的元数据
     * @return 是否满足
     */
    private boolean isWhereMatched(List<Object[]> objectBaseData) {
        //判断当前列是否匹配
        boolean matched = false;
        //(1)判断等于某个值的条件是否匹配当前列
        if (!CollectionUtils.isEmpty(equalValueMap)) {
            for (String tbAndColumn : equalValueMap.keySet()) {
                String[] split = tbAndColumn.split("\\.");
                int columnId = Integer.parseInt(split[1]);
                Object value = equalValueMap.get(tbAndColumn);
                if (ByteArrayUtil.isEqualed(objectBaseData.get(columnId),value)) {
                    matched = true;
                } else {
                    matched = false;
                }
            }
        }
        //(2)不匹配继续过滤下一条数据
        if (!matched) {
            return false;
        }
        //(3)判断字段相等的是否匹配当前列
        if (!CollectionUtils.isEmpty(equalColumnMap)) {
            for (Map<String, String> conditionMap : equalColumnMap) {
                for (String tbAndColumn : conditionMap.keySet()) {
                    String[] split = tbAndColumn.split("\\.");
                    int leftColumnId = Integer.parseInt(split[1]);
                    String rightTbAndColumn = conditionMap.get(tbAndColumn);
                    String[] split1 = rightTbAndColumn.split("\\.");
                    int rightColumnId = Integer.parseInt(split1[1]);
                    if (ByteArrayUtil.isEqualed(objectBaseData.get(leftColumnId),objectBaseData.get(rightColumnId))) {
                        matched = true;
                    } else {
                        matched = false;
                    }
                }
            }
        }
        return matched;
    }

    public void addDirtyIndexInfoToCache() {
        for (String tableName : tableIndexTree.keySet()) {
            Map<String, TableIndexDataInfo> indexDataMap = tableIndexTree.get(tableName);
            for (String columns : indexDataMap.keySet()) {
                SqlParserUtil.addIndexDataToCache(indexDataMap.get(columns));
            }
        }
    }
}
