package com.db.entity;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 实现B+树
 * @param <T> 指定值类型
 * @param <V> 使用泛型，指定索引类型,并且指定必须继承Comparable
 */
public class BPlusTree <T, V extends Comparable<V>> implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(BPlusTree.class);
    //B+树的阶
    private Integer bTreeOrder;
    //B+树的非叶子节点最小拥有的子节点数量（同时也是键的最小数量）
    //private Integer minNumber;
    //B+树的非叶子节点最大拥有的节点数量（同时也是键的最大数量）
    private Integer maxNumber;

    private Node<T, V> root;

    private LeafNode<T, V> left;

    //默认阶为3
    public BPlusTree() {
        this(3);
    }

    //有参构造方法，可以设定B+树的阶
    public BPlusTree(Integer bTreeOrder) {
        this.bTreeOrder = bTreeOrder;
        //this.minNumber = (int) Math.ceil(1.0 * bTreeOrder / 2.0);
        //因为插入节点过程中可能出现超过上限的情况,所以这里要加1
        this.maxNumber = bTreeOrder + 1;
        this.root = new LeafNode<T, V>();
        this.left = null;
    }

    //查询
    public T find(V key) {
        T t = this.root.find(key);
        if (t == null) {
            System.out.println("不存在！");
        }
        return t;
    }

    //插入
    public void insert(T value, V key) {
        if (key == null) {
            return;
        }
        Node<T, V> t = this.root.insert(value, key);
        if (t != null) {
            this.root = t;
        }
        this.left = (LeafNode<T, V>) this.root.refreshLeft();
    }

    //按照从左到右的顺序获取所有的叶子结点的value数据
    public List<Object[]> getAllLeafNodeData() {
        List<Object[]> dataList = new ArrayList<>();
        LeafNode node = (LeafNode) this.root;
        while (node.children==null && node.right==null) {
            dataList.add(node.values);
            if (node.right != null) {
                node = node.right;
            } else {
                break;
            }
        }
        return dataList;
    }


    /**
     * 节点父类，因为在B+树中，非叶子节点不用存储具体的数据，只需要把索引作为键就可以了
     * 所以叶子节点和非叶子节点的类不太一样，但是又会公用一些方法，所以用Node类作为父类,
     * 而且因为要互相调用一些公有方法，所以使用抽象类
     *
     * @param <T> 同BPlusTree
     * @param <V>
     */
    abstract class Node<T, V extends Comparable<V>> implements Serializable {
        //父节点
        protected Node<T, V> parent;
        //子节点
        protected Node<T, V>[] children;
        //键（子节点）数量
        protected Integer number;
        //键
        protected Object[] keys;

        //构造方法
        public Node() {
            this.keys = new Object[maxNumber];
            this.children = new Node[maxNumber];
            this.number = 0;
            this.parent = null;
        }

        //查找
        abstract T find(V key);

        //插入
        abstract Node<T, V> insert(T value, V key);

        abstract LeafNode<T, V> refreshLeft();
    }


    /**
     * 非叶节点类
     *
     * @param <T>
     * @param <V>
     */

    class BPlusNode<T, V extends Comparable<V>> extends Node<T, V> {

        public BPlusNode() {
            super();
        }

        /**
         * 递归查找,这里只是为了确定值究竟在哪一块,真正的查找到叶子节点才会查
         *
         * @param key
         * @return
         */
        @Override
        T find(V key) {
            int i = 0;
            while (i < this.number) {
                if (key.compareTo((V) this.keys[i]) <= 0) {
                    break;
                }
                i++;
            }
            if (this.number == i) {
                return null;
            }
            return this.children[i].find(key);
        }

        /**
         * 递归插入,先把值插入到对应的叶子节点,最终将调用叶子节点的插入类
         *
         * @param value
         * @param key
         */
        @Override
        Node<T, V> insert(T value, V key) {
            int i = 0;
            while (i < this.number) {
                if (key.compareTo((V) this.keys[i]) < 0) {
                    break;
                }
                i++;
            }
            if (key.compareTo((V) this.keys[this.number - 1]) >= 0) {
                i--;
//                if(this.children[i].number + 1 <= bTreeOrder) {
//                    this.keys[this.number - 1] = key;
//                }
            }
            return this.children[i].insert(value, key);
        }

        @Override
        LeafNode<T, V> refreshLeft() {
            return this.children[0].refreshLeft();
        }

        /**
         * 当叶子节点插入成功完成分解时,递归地向父节点插入新的节点以保持平衡
         *
         * @param node1
         * @param node2
         * @param key
         */
        Node<T, V> insertNode(Node<T, V> node1, Node<T, V> node2, V key) {

            V oldKey = null;
            if (this.number > 0) {
                oldKey = (V) this.keys[this.number - 1];
            }
            //如果原有key为null,说明这个非节点是空的,直接放入两个节点即可
            if (key == null || this.number <= 0) {
                this.keys[0] = node1.keys[node1.number - 1];
                this.keys[1] = node2.keys[node2.number - 1];
                this.children[0] = node1;
                this.children[1] = node2;
                this.number += 2;
                return this;
            }
            //原有节点不为空,则应该先寻找原有节点的位置,然后将新的节点插入到原有节点中
            int i = 0;
            while (key.compareTo((V) this.keys[i]) != 0) {
                i++;
            }
            //左边节点的最大值可以直接插入,右边的要挪一挪再进行插入
            this.keys[i] = node1.keys[node1.number - 1];
            this.children[i] = node1;

            Object[] tempKeys = new Object[maxNumber];
            Object[] tempChildren = new Node[maxNumber];

            System.arraycopy(this.keys, 0, tempKeys, 0, i + 1);
            System.arraycopy(this.children, 0, tempChildren, 0, i + 1);
            System.arraycopy(this.keys, i + 1, tempKeys, i + 2, this.number - i - 1);
            System.arraycopy(this.children, i + 1, tempChildren, i + 2, this.number - i - 1);
            tempKeys[i + 1] = node2.keys[node2.number - 1];
            tempChildren[i + 1] = node2;

            this.number++;

            //判断是否需要拆分
            //如果不需要拆分,把数组复制回去,直接返回
            if (this.number <= bTreeOrder) {
                System.arraycopy(tempKeys, 0, this.keys, 0, this.number);
                System.arraycopy(tempChildren, 0, this.children, 0, this.number);
                return null;
            }
            //如果需要拆分,和拆叶子节点时类似,从中间拆开
            Integer middle = this.number / 2;

            //新建非叶子节点,作为拆分的右半部分
            BPlusNode<T, V> tempNode = new BPlusNode<T, V>();
            //非叶节点拆分后应该将其子节点的父节点指针更新为正确的指针
            tempNode.number = this.number - middle;
            tempNode.parent = this.parent;
            //如果父节点为空,则新建一个非叶子节点作为父节点,并且让拆分成功的两个非叶子节点的指针指向父节点
            if (this.parent == null) {
                BPlusNode<T, V> tempBPlusNode = new BPlusNode<>();
                tempNode.parent = tempBPlusNode;
                this.parent = tempBPlusNode;
                oldKey = null;
            }
            System.arraycopy(tempKeys, middle, tempNode.keys, 0, tempNode.number);
            System.arraycopy(tempChildren, middle, tempNode.children, 0, tempNode.number);
            for (int j = 0; j < tempNode.number; j++) {
                tempNode.children[j].parent = tempNode;
            }
            //让原有非叶子节点作为左边节点
            this.number = middle;
            this.keys = new Object[maxNumber];
            this.children = new Node[maxNumber];
            System.arraycopy(tempKeys, 0, this.keys, 0, middle);
            System.arraycopy(tempChildren, 0, this.children, 0, middle);

            //叶子节点拆分成功后,需要把新生成的节点插入父节点
            BPlusNode<T, V> parentNode = (BPlusNode<T, V>) this.parent;
            return parentNode.insertNode(this, tempNode, oldKey);
        }

    }

    /**
     * 叶节点类
     *
     * @param <T>
     * @param <V>
     */
    class LeafNode<T, V extends Comparable<V>> extends Node<T, V> {
        protected Object[] values;
        protected LeafNode left;
        protected LeafNode right;

        public LeafNode() {
            super();
            this.values = new Object[maxNumber];
            this.left = null;
            this.right = null;
        }

        /**
         * 二分查找
         * @param key
         * @return
         */
        @Override
        T find(V key) {
            if (this.number <= 0) {
                return null;
            }
            int left = 0;
            int right = this.number;

            int middle = (left + right) / 2;

            while (left < right) {
                V middleKey = (V) this.keys[middle];
                if (key.compareTo(middleKey) == 0) {
                    return (T) this.values[middle];
                } else if (key.compareTo(middleKey) < 0) {
                    right = middle;
                } else {
                    left = middle;
                }
                middle = (left + right) / 2;
            }
            return null;
        }

        /**
         * @param value
         * @param key
         */
        @Override
        Node<T, V> insert(T value, V key) {
            //保存原始存在父节点的key值
            V oldKey = null;
            if (this.number > 0) {
                oldKey = (V) this.keys[this.number - 1];
            }
            //先插入数据
            int i = 0;
            while (i < this.number) {
                if (key.compareTo((V) this.keys[i]) < 0) {
                    break;
                }
                i++;
            }

            //复制数组,完成添加
            Object[] tempKeys = new Object[maxNumber];
            Object[] tempValues = new Object[maxNumber];
            System.arraycopy(this.keys, 0, tempKeys, 0, i);
            System.arraycopy(this.values, 0, tempValues, 0, i);
            System.arraycopy(this.keys, i, tempKeys, i + 1, this.number - i);
            System.arraycopy(this.values, i, tempValues, i + 1, this.number - i);
            tempKeys[i] = key;
            tempValues[i] = value;

            this.number++;

            //判断是否需要拆分
            //如果不需要拆分完成复制后直接返回
            if (this.number <= bTreeOrder) {
                System.arraycopy(tempKeys, 0, this.keys, 0, this.number);
                System.arraycopy(tempValues, 0, this.values, 0, this.number);

                //有可能虽然没有节点分裂，但是实际上插入的值大于了原来的最大值，所以所有父节点的边界值都要进行更新
                Node node = this;
                while (node.parent != null) {
                    V tempkey = (V) node.keys[node.number - 1];
                    if (tempkey.compareTo((V) node.parent.keys[node.parent.number - 1]) > 0) {
                        node.parent.keys[node.parent.number - 1] = tempkey;
                        node = node.parent;
                    } else {
                        break;
                    }
                }
                return null;
            }

            //如果需要拆分,则从中间把节点拆分差不多的两部分
            Integer middle = this.number / 2;

            //新建叶子节点,作为拆分的右半部分
            LeafNode<T, V> tempNode = new LeafNode<T, V>();
            tempNode.number = this.number - middle;
            tempNode.parent = this.parent;
            //如果父节点为空,则新建一个非叶子节点作为父节点,并且让拆分成功的两个叶子节点的指针指向父节点
            if (this.parent == null) {
                BPlusNode<T, V> tempBPlusNode = new BPlusNode<>();
                tempNode.parent = tempBPlusNode;
                this.parent = tempBPlusNode;
                oldKey = null;
            }
            System.arraycopy(tempKeys, middle, tempNode.keys, 0, tempNode.number);
            System.arraycopy(tempValues, middle, tempNode.values, 0, tempNode.number);

            //让原有叶子节点作为拆分的左半部分
            this.number = middle;
            this.keys = new Object[maxNumber];
            this.values = new Object[maxNumber];
            System.arraycopy(tempKeys, 0, this.keys, 0, middle);
            System.arraycopy(tempValues, 0, this.values, 0, middle);

            this.right = tempNode;
            tempNode.left = this;

            //叶子节点拆分成功后,需要把新生成的节点插入父节点
            BPlusNode<T, V> parentNode = (BPlusNode<T, V>) this.parent;
            return parentNode.insertNode(this, tempNode, oldKey);
        }

        @Override
        LeafNode<T, V> refreshLeft() {
            if (this.number <= 0) {
                return null;
            }
            return this;
        }
    }
}


