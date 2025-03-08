package com.db.entity;

import java.util.List;

/**
 * 数据页：存储某一段数据
 * @author Roy
 * @date 2022/10/18 22:10
 */
public class DBFile {
    /** 当前查询文件所在路径 */
    private String filePath;

    /** 文件名称 */
    private String fileName;

    /** 对应的页信息 */
    private List<BasePage> pageList;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<BasePage> getPageList() {
        return pageList;
    }

    public void setPageList(List<BasePage> pageList) {
        this.pageList = pageList;
    }
}
