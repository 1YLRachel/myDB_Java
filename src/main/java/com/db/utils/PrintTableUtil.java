package com.db.utils;

import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 控制台打印表格
 * @author Roy
 * @date 2022/11/6 12:04
 */
public class PrintTableUtil {
    //按表格打印数据
    public static void printTable(String[] heads, String[][] data) {
        if (null == heads || heads.length <= 0) {
            return ;
        }

        if (null == data || data.length <=0 || null == data[0] || data[0].length <= 0) {
            return;
        }
        if (heads.length != data[0].length) {
            return;
        }
        //数据列数
        int rowNum = heads.length;
        //数据行数
        int lineNum = data.length;
        //每列数据宽度
        Map<Integer, Integer> rowWidthMap = getRowWidthMap(rowNum, lineNum, heads, data);
        //打印表头
        printHead(rowNum, rowWidthMap, heads);
        //打印数据
        printData(rowNum, lineNum, rowWidthMap, data);

    }

    //获取列宽度-每列取最大
    public static Map<Integer, Integer> getRowWidthMap(int rowNum, int lineNum, String[] heads, String[][] data) {
        Map<Integer, Integer> rowWidthMap = new HashMap<>();
        //先记入表头各列宽度
        for (int i = 0; i < rowNum; i++) {
            int len = length(heads[i]);
            if (null == rowWidthMap.get(i)) {
                rowWidthMap.put(i, len);
            } else if (rowWidthMap.get(i) < len) {
                rowWidthMap.put(i, len);
            }
        }
        //比较并记入数据的最大宽度
        for (int i = 0; i < lineNum; i++) {
            for (int j = 0; j < rowNum; j++) {
                int len = length(data[i][j]);
                if (null == rowWidthMap.get(j)) {
                    rowWidthMap.put(j, len);
                } else if (rowWidthMap.get(j) < len) {
                    rowWidthMap.put(j, len);
                }
            }
        }
        return rowWidthMap;
    }

    //打印表格中的横线
    public static void printLine(int rowNum, Map<Integer, Integer> map) {
        for (int i = 0; i < rowNum; i++) {
            int len = map.get(i);
            System.out.print("+");
            for (int k = 0; k < len; k++) {
                System.out.print("-");
            }
        }
        System.out.print("+");
        System.out.println();
    }

    public static int length(String str) {

        if (StringUtils.isEmpty(str)) {
            return 0;
        }
        return str.length();
    }

    //打印表头内容
    public static void printHead(int rowNum, Map<Integer, Integer> rowWidthMap, String[] heads) {
        printLine(rowNum, rowWidthMap);
        for (int h = 0; h < rowNum; h++) {
            System.out.print("|");
            int actLength = rowWidthMap.get(h);
            int dataLength = length(heads[h]);
            executePrintData(actLength, dataLength, heads[h]);
            if (h == rowNum - 1) {
                System.out.print("|");
            }
        }
        System.out.println();
        printLine(rowNum, rowWidthMap);
    }

    //打印表数据
    public static void printData(int rowNum, int lineNum, Map<Integer, Integer> rowWidthMap, String[][] data) {
        for (int j = 0; j < lineNum; j++) {
            for (int i = 0; i < rowNum; i++) {
                System.out.print("|");
                int dataLength = length(data[j][i]);
                int actLength = rowWidthMap.get(i);
                executePrintData(actLength, dataLength, data[j][i]);
                if (i == rowNum - 1) {
                    System.out.print("|");
                }
            }

            System.out.println();
            printLine(rowNum, rowWidthMap);
        }
        System.out.println();
    }

    //执行打印数据
    public static void executePrintData(int actLength, int dataLength, String data) {
        if (actLength > dataLength) {
            int num = actLength - dataLength;
            if (num == 1) {
                System.out.print(data);
                System.out.print(" ");
            } else {
                int beforeNum = num / 2;
                int afterNum = num - beforeNum;
                for (int m = 0; m < beforeNum; m++) {
                    System.out.print(" ");
                }
                System.out.print(data);
                for (int m = 0; m < afterNum; m++) {
                    System.out.print(" ");
                }
            }
        } else {
            System.out.print(data);
        }
    }

}
