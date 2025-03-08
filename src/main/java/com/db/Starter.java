package com.db;

import com.db.constants.BaseConstants;
import com.db.constants.CONSTANTS;
import com.db.constants.FileConstants;
import com.db.file.page.FileManager;
import com.db.manager.CacheManager;
import com.db.sql.parser.SQLLexer;
import com.db.sql.parser.SQLParser;
import com.db.sql.parser.listener.VerboseListener;
import com.db.utils.ByteArrayUtil;
import com.db.utils.FileUtil;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * 启动执行
 * @author Roy
 * @date 2022/10/25 21:25
 */
public class Starter {
    public static void main(String[] args) {
        System.out.println("**********************欢迎使用myDb*********************");
        //1.执行初始化操作
        boolean initFlag = init();
        if (!initFlag) {
            System.out.println("myDb初始化失败！");
            return;
        }
        SqlParseVisitor visitor = new SqlParseVisitor();
        Scanner scan = new Scanner(System.in);
        System.out.print("db> ");
        String sql = scan.nextLine();
        //当下一行的输入值为exit（不区分大小写）时，结束程序
        String dbName="";
        while (!CONSTANTS.EXIT_FLAG.equals(sql.toLowerCase())) {
            //当为空的时候，继续监听下一行
            if (!StringUtils.isEmpty(sql)) {
                //1.获取输入，将其视为char数组缓冲区
                ANTLRInputStream input = new ANTLRInputStream(sql);
                //2.将字符序列转换为单词(Token)
                SQLLexer lexer = new SQLLexer(input);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                SQLParser parser = new SQLParser(tokens);
                //3.去掉解析器中的ConsoleErrorListener，使用自定义的错误监听器
                parser.removeErrorListeners();
                VerboseListener verboseListener = new VerboseListener();
                parser.addErrorListener(verboseListener);
                SQLParser.ProgramContext tree = parser.program();
                //4.当没有解析错误的时候，就继续往下监听
                if (parser.getNumberOfSyntaxErrors()==0) {
                    Object result = visitor.visit(tree);
                    if (result instanceof String) {
                        dbName = (String) result;
                    }
                }
            }
            //5.继续监听下一行
            if (!StringUtils.isEmpty(dbName)) {
                System.out.print("db:"+dbName+" > ");
            } else {
                System.out.print("db> ");
            }
            sql= scan.nextLine();
        }
        //关闭文件，清除缓存
        visitor.addDirtyIndexInfoToCache();
        FileManager.closeAllFile();
        System.out.println("**********************退出成功*********************");
    }

    /**
     * 初始化基本数据
     * todo 记录缓存
     * @return 初始化结果 true:初始化成功
     */
    private static boolean init() {
        //1.创建base文件夹
        FileUtil.createDir(FileConstants.BASE_DIR_PATH);
        //2.创建记录表基本信息的文件
        boolean initTableFlag = initTableOrIndexInfo(FileConstants.GLOBAL_TB_NAME,
                BaseConstants.TABLE_COLUMNS,
                BaseConstants.TABLE_COLUMN_TYPES,
                BaseConstants.COLUMN_LENGTH,
                BaseConstants.VARIABLE_COLUMN_FLAG);
        if (!initTableFlag) {
            System.out.println("初始化global表基本信息失败!");
            return false;
        }
        //3.创建记录索引信息的文件
        boolean initIndexFlag = initTableOrIndexInfo(FileConstants.GLOBAL_INDEX_NAME,
                BaseConstants.INDEX_COLUMNS,
                BaseConstants.INDEX_COLUMN_TYPES,
                BaseConstants.INDEX_COLUMN_LENGTH,
                BaseConstants.VARIABLE_INDEX_FLAG);
        if (!initIndexFlag) {
            System.out.println("初始化global索引基本信息失败！");
            return false;
        }
        return true;
    }

    /**
     * 初始化表和索引的基本信息
     * @param fileName 文件名
     * @param columns 所要存储的列名
     * @param types 各列的字段类型
     * @param columnLength 列所占的字节长度
     * @param variableFlag 是否变长
     * @return 是否初始化成功
     */
    private static boolean initTableOrIndexInfo (String fileName,String[] columns,
                                               String[] types,int[] columnLength,
                                               byte[] variableFlag) {
        int socketLength = 124*columns.length;
        int socketCount = CONSTANTS.PAGE_SIZE / socketLength;
        byte[] completeByte = new byte[CONSTANTS.PAGE_SIZE - socketLength];
        String globalFilePath = FileConstants.GLOBAL_DIR_PATH
                + File.separator + fileName;
        File globalFile = new File(globalFilePath);
        if (!globalFile.exists()) {
            boolean globalFlag = FileUtil.createFile(globalFilePath);
            if (globalFlag) {
                //(1)添加基本信息 （一个列存储四个数据:名称 100，类型 14,长度 4，是否变长1，真实长度:4）
                //-----------------记录页信息--------------------
                int pageId = 0;
                int fileId = -1;
                //记录缓存的数据
                List<Object[]> dataList = new ArrayList<>();
                /** 表的基本信息文件中最终存储的byte[] */
                byte[] combinedByte = null;
                //一个socket
                List<byte[]> socketByteData = new ArrayList<>();
                for (int i = 0; i < columns.length; i++) {
                    //一列数据
                    byte[] columnByteData = null;
                    String columnName = columns[i];
                    byte[] nameByte = ByteArrayUtil.getStrCompleteByte(columnName,100);
                    String type = types[i];
                    byte[] typeByte = ByteArrayUtil.getStrCompleteByte(type,14);
                    int length = columnLength[i];
                    byte[] lengthByte = ByteArrayUtil.intToByte(length);
                    byte[] variableByte = new byte[]{variableFlag[i]};
                    int realLength = columnName.getBytes().length;
                    byte[] realLengthByte = ByteArrayUtil.intToByte(realLength);
                    columnByteData = ByteArrayUtil.byteMergerAll(typeByte,lengthByte,variableByte,realLengthByte,nameByte);
                    socketByteData.add(columnByteData);
                    combinedByte = ByteArrayUtil.byteMergerAll(combinedByte,columnByteData);
                    //缓存中存储 字段名，字段类型，长度，是否变长，真实长度
                    Object[] data = new Object[5];
                    data[0] = type;
                    data[1] = length;
                    data[2] = variableFlag[i];
                    data[3] = realLength;
                    data[4] = columnName;
                    dataList.add(data);
                }
                //如果剩下的数据不够一页，将剩下的数据记录
                int notEmptySocketCount = 1;
                byte[] isNotEmptySocket = new byte[notEmptySocketCount];
                byte[] isEmptySocket = new byte[socketCount-notEmptySocketCount];
                Arrays.fill(isEmptySocket, (byte) 1);
                //1.将数据添加到最终要写入的byte[],combinedByte作为一页
                combinedByte = ByteArrayUtil.byteMergerAll(isNotEmptySocket,isEmptySocket,combinedByte,completeByte);
                //2.将数据写入缓存
                fileId = FileManager.openFile(globalFilePath);
                if (fileId != -1) {
                    CacheManager.addPage(fileId,pageId, Collections.singletonList(dataList),
                            Collections.singletonList(socketByteData));
                }
                //(2)将global索引的基本信息写入文件
                try {
                    RandomAccessFile accessFile = new RandomAccessFile(globalFile,"rw");
                    accessFile.write(combinedByte);
                } catch (Exception e) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

}
