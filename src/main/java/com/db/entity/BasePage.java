package com.db.entity;

import com.db.constants.CONSTANTS;

import java.util.List;

/**
 * 一页数据
 * @author Roy
 * @date 2022/10/22 8:11
 */
public class BasePage {
    /** 打开的文件id */
    private int fileId;

    /** 页id */
    private Integer pageId;

    /** 页内槽中存储的数据 */
    private BaseSocket[] socketList;

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public BaseSocket[] getSocketList() {
        return socketList;
    }

    public void setSocketList(BaseSocket[] socketList) {
        this.socketList = socketList;
    }

    public Integer getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public BasePage(int fileId, int pageId, List<List<Object[]>> socketData,List<List<byte[]>> socketByteData) {
        this.fileId = fileId;
        this.pageId = pageId;
        socketList = new BaseSocket[socketData.size()];
        for (int i = 0; i < socketData.size(); i++) {
            if (socketData.get(i) == null || socketData.get(i).size()==0) {
                socketList[i] = null;
                continue;
            }
            BaseSocket socket = new BaseSocket();
            socket.setObjectData(socketData.get(i));
            socket.setData(socketByteData.get(i));
            socketList[i] = socket;
        }
    }

    /**
     * 将数据追加到当前socket
     * @param socketData 对象信息
     * @param socketByteData byte[]信息
     * @return 添加结果
     */
    public boolean addSocketInfo(List<List<Object[]>> socketData,List<List<byte[]>> socketByteData) {
        for (int i = 0; i < socketList.length; i++) {
            if (socketList[i] == null && socketData.size()!=0) {
                List<Object[]> socketInfo = socketData.remove(0);
                List<byte[]> byteInfo = socketByteData.remove(0);
                BaseSocket socket = new BaseSocket();
                socket.setObjectData(socketInfo);
                socket.setData(byteInfo);
                socketList[i] = socket;
            }
        }
        return true;
    }

    /**
     * 获取空的socket数量
     * @return
     */
    public int getEmptySocketCount() {
        int socketLength = 0;
        int notEmptySocketCount = 0;
        for (BaseSocket socket : socketList) {
            if (socket != null) {
                socketLength = socket.getSocketData().length;
                notEmptySocketCount += 1;
            }
        }
        int pageSocketCount = CONSTANTS.PAGE_SIZE/(socketLength+1);
        return pageSocketCount-notEmptySocketCount;
    }
}
