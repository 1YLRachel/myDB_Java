package com.db.entity;

/**
 * 链表中的一个结点信息
 * @author Roy
 * @date 2022/10/22 8:08
 */
public class BaseLinkedNode {
    /** 前一条数据 */
    private BaseLinkedNode prevData;
    /** 下一条数据 */
    private BaseLinkedNode nextData;
    /** 每个结点存储一页数据 */
    private BasePage data;

    public BaseLinkedNode getPrevData() {
        return prevData;
    }

    public void setPrevData(BaseLinkedNode prevData) {
        this.prevData = prevData;
    }

    public BaseLinkedNode getNextData() {
        return nextData;
    }

    public void setNextData(BaseLinkedNode nextData) {
        this.nextData = nextData;
    }

    public BasePage getData() {
        return data;
    }

    public void setData(BasePage data) {
        this.data = data;
    }
}
