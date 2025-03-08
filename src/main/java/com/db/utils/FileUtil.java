package com.db.utils;

import com.db.constants.CONSTANTS;
import com.db.constants.FileConstants;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件相关的方法
 * @author Roy
 * @date 2022/10/23 9:57
 */
public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    /** 文件根目录 */
    private static File rootDir = new File(FileConstants.BASE_DIR_PATH);

    /**
     * 创建一个文件夹
     * @param fileType 文件类型
     * @param fileList 文件名数组 （多层级时，按照顺序进行添加）
     * @return 创建结果
     */
    public static boolean createDir(String fileType, List<String> fileList) {
        return createOrRemoveDir(true, fileType, fileList);
    }

    /**
     * 删除文件或文件夹
     * @return 删除结果
     */
    public static boolean removeFileOrDir(String fileType, List<String> fileList) {
        return createOrRemoveDir(false,fileType,fileList);
    }

    /**
     * 创建一个文件夹或删除文件
     * @param fileType 文件类型
     * @param fileList 文件名列表
     */
    private static boolean createOrRemoveDir(boolean createFlag,String fileType, List<String> fileList) {
        if (CollectionUtils.isEmpty(fileList)) {
            System.out.println("文件名为空！");
        }
        String filePath = FileConstants.BASE_DIR_PATH+File.separator+fileList.get(0);
        switch (fileType) {
            case CONSTANTS.DATABASE:
                if (fileList.size() == 1) {
                    if (createFlag) {
                        return createDir(filePath);
                    } else {
                        return removeFile(filePath);
                    }
                } else {
                    System.out.println("文件类型错误！");
                    return false;
                }
            case CONSTANTS.TABLE:
                if (fileList.size()==2) {
                    filePath += File.separator + fileList.get(1);
                    if (createFlag) {
                        return createDir(filePath);
                    } else {
                        return removeFile(filePath);
                    }
                } else {
                    System.out.println("文件类型错误！");
                    return false;
                }
            case CONSTANTS.INDEX:
                if (fileList.size()==3){
                    filePath += File.separator + fileList.get(1) + File.separator + fileList.get(2);
                    if (createFlag) {
                        return createDir(filePath);
                    } else {
                        return removeFile(filePath);
                    }
                } else{
                    System.out.println("文件类型错误！");
                    return false;
                }
            default:
                break;
        }
        return true;
    }

    /**
     * 创建文件
     * @return 是否创建成功标志
     */
    public static boolean createFile(String filePath) {
        File file = new File(filePath);
        File dir = file.getParentFile();
        if (!dir.exists()) {
            boolean mkdirFlag = dir.mkdirs();
            if (!mkdirFlag) {
                System.out.println("创建目录失败！");
                return false;
            }
        }
        try {
            return file.createNewFile();
        } catch (IOException e) {
            System.out.println("创建文件失败！");
            return false;
        }
    }

    /**
     * 创建一个文件
     * @param filePath 文件路径
     * @return 创建结果
     */
    public static boolean createDir(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return false;
        } else {
            return file.mkdirs();
        }
    }

    /**
     * 根据文件路径删除文件
     * @param filePath 文件路径
     * @return 删除结果
     */
    public static boolean removeFile(String filePath) {
        File file = new File(filePath);
        return removeFile(file);
    }

    /**
     * 删除：删除文件和文件夹（文件夹不为空时也会删除）
     * todo 使用观察者模式，在删除的同时做一些修改记录操作
     * @param file：文件对象
     * @return 删除文件标志
     */
    public static boolean removeFile(File file){
        if(file.exists()) {//判断路径是否存在
            if(file.isFile()){//boolean isFile():测试此抽象路径名表示的文件是否是一个标准文件。
                return file.delete();
            }else{//不是文件，对于文件夹的操作
                //保存 路径D:/1/新建文件夹2  下的所有的文件和文件夹到listFiles数组中
                File[] listFiles = file.listFiles();//listFiles方法：返回file路径下所有文件和文件夹的绝对路径
                for (File file2 : listFiles) {
                    /*
                     * 递归作用：由外到内先一层一层删除里面的文件 再从最内层 反过来删除文件夹
                     *    注意：此时的文件夹在上一步的操作之后，里面的文件内容已全部删除
                     *         所以每一层的文件夹都是空的  ==》最后就可以直接删除了
                     */
                    removeFile(file2);
                }
            }
            return file.delete();
        }else {
            System.out.println("文件不存在！");
        }
        return false;
    }

    /**
     * 当文件超长时，生成新的文件路径
     * @param file 文件对象
     * @return 生成新的文件路径
     */
    public static String generateNewFilePath(File file) {
        String filePath = getMaxSameNamePath(file);
        String fileNameNoEx = FileUtil.getFileNameNoEx(filePath);
        int beginIndex = fileNameNoEx.indexOf('(');
        int endIndex = fileNameNoEx.indexOf(')');
        //当已经存在名称相同添加序号的文件的时候，新的文件名将序号加一
        if(beginIndex != -1 && endIndex != -1) {
            String num = fileNameNoEx.substring(beginIndex+1, endIndex);
            String oldStr = "(" + num + ")";
            int newNum = Integer.parseInt(num) + 1;
            String newStr = "(" + newNum + ")";
            return filePath.replace(oldStr, newStr);
        } else {
            //新建文件名称添加"(1)"
            String newFileName = fileNameNoEx + "(1)";
            return filePath.replace(fileNameNoEx, newFileName);
        }
    }

    /**
     * 获取当前文件目录下的所有同名文件，找出含有最大的含有序号的文件路径
     * @param file 当前文件
     * @return 文件路径最大值
     */
    public static String getMaxSameNamePath(File file) {
        String maxFilePath = file.getAbsolutePath();
        if (!file.exists()) {
            FileUtil.createFile(maxFilePath);
            return maxFilePath;
        }
        File parentFile = file.getParentFile();
        File[] children = parentFile.listFiles();
        int maxNum = getNumFromFile(file);
        String fileName = getNoNumFileName(file.getName());
        //超过一个文件的时候才去判断
        if (children.length > 1) {
            for (File childFile : children) {
                if (fileName.equals(getNoNumFileName(childFile.getName()))) {
                    int num = getNumFromFile(childFile);
                    maxNum = Math.max(maxNum, num);
                    maxFilePath = maxNum > num ?maxFilePath:childFile.getAbsolutePath();
                }
            }
        }
        return maxFilePath;
    }
    /**
     * 找出文件中的序号
     * @param file 文件对象
     * @return 序号
     */
    private static int getNumFromFile(File file) {
        String fileName = file.getName();
        String fileNameNoEx = FileUtil.getFileNameNoEx(fileName);
        int beginIndex = fileNameNoEx.indexOf('(');
        int endIndex = fileNameNoEx.indexOf(')');
        int num = 0;
        if(beginIndex != -1 && endIndex != -1) {
            String numStr = fileNameNoEx.substring(beginIndex+1, endIndex);
            try {
                num = Integer.parseInt(numStr);
            } catch (Exception e) {
                System.out.println("文件"+fileName+"命名有误，请检查！");
            }
        }
        return num;
    }

    /**
     * 获取去掉小括号和序号之后的文件名称
     * @param fileName 文件名称
     * @return 去掉小括号和序号之后的文件名
     */
    public static String getNoNumFileName(String fileName) {
        String fileNameNoEx = FileUtil.getFileNameNoEx(fileName);
        int beginIndex = fileNameNoEx.indexOf('(');
        int endIndex = fileNameNoEx.indexOf(')');
        if(beginIndex != -1 && endIndex != -1) {
            String numStr = fileNameNoEx.substring(beginIndex,endIndex+1);
            return fileNameNoEx.replace(numStr,"");
        }
        return fileNameNoEx;
    }

    /**
     * 根据文件的绝对路径获取不带扩展名的文件名
     * @param filePath 文件的绝对路径
     * @return 不带扩展名的文件名
     */
    public static String getFileNameNoEx(String filePath) {
        if ((filePath != null) && (filePath.length() > 0)) {
            int dot = filePath.lastIndexOf('.');
            if ((dot >-1) && (dot < (filePath.length()))) {
                return filePath.substring(0, dot);
            }
        }
        return filePath;
    }

    /**
     * 判断数据库、表、索引、表数据文件是否存在
     * @param fileType 文件类型
     * @param name 名称 数据库名 表名 索引名
     * @return 是否存在
     */
    public static boolean isExistDir(String fileType,List<String> name) {
        File file = rootDir;
        String filePath = FileConstants.BASE_DIR_PATH;
        if(CONSTANTS.TABLE.equals(fileType)) {
            filePath += File.separator+name.get(0);
            file = new File(filePath);
        } else if (CONSTANTS.INDEX.equals(fileType)) {
            filePath += File.separator+name.get(0)+File.separator+name.get(1)
                    +File.separator+FileConstants.INDEXES_DIR_NAME;
            file = new File(filePath);
        } else if (CONSTANTS.DATA.equals(fileType)) {
            filePath += File.separator+name.get(0)+File.separator+name.get(1)
                    +File.separator+FileConstants.TABLE_DATA_DIR_NAME;
            file = new File(filePath);
        }
        String fileName = name.get(name.size()-1);
        File[] files = file.listFiles(pathname -> {
            if (pathname.getName().equals(fileName)) {
                return pathname.isDirectory();
            }
            return false;
        });
        if (files != null && files.length>0) {
            return true;
        }
        return false;
    }

    /**
     * 根据文件类型和文件名获取文件夹路径
     * @return 文件夹路径
     */
    public static String getDirPathByTypeAndName(String fileType,String[] names) {
        String filePath = FileConstants.BASE_DIR_PATH;
        if(CONSTANTS.DATABASE.equals(fileType)) {
            filePath += File.separator+names[0];
        } else if (CONSTANTS.TABLE.equals(fileType)) {
            filePath += File.separator+names[0]+File.separator+names[1];
        } else if (CONSTANTS.INDEX.equals(fileType)) {
            filePath += File.separator+names[0]+File.separator+names[1]
                    +File.separator+FileConstants.INDEXES_DIR_NAME
                    +File.separator+names[2];
        }
        return filePath;
    }

    /**
     * 根据数据库名和表名获取表字段基本信息文件路径
     * @param dbName 数据库名
     * @param tableName 表名
     * @return 表字段基本信息文件路径
     */
    public static String getTbColumnDataFilePath(String dbName,String tableName) {
        return FileConstants.BASE_DIR_PATH+
                File.separator+dbName+
                File.separator+tableName+
                File.separator+FileConstants.TABLE_BASE_FILE_NAME;
    }

}
