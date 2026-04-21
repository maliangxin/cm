package com.yonyoucloud.fi.cmp.weekday;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

/**
 * @author shiqhs
 * @date 2022/05/03
 * @description 运算符集合类,包括算术和位运算符,约定每个运算符的优先级
 */
public class Operators implements Comparator<String> {

    /**
     * 运算符集合,包括算术和位运算符
     */
    private String[] operator = {"^", "~", "*", "/", "%", "+", "-", "&", "|"};

    /**
     * operator中相同下标运算符的优先级,值小的优先级高
     */
    private int[] priority = {2, 3, 4, 4, 4, 5, 5, 9, 10};

    /**
     * 使用顺序表存储运算符集合,调用查找算法
     */
    private SeqList<String> operatorList;

    public Operators() {
        this.operatorList = new SeqList<>(this.operator);
    }

    /**
     * 比较operator1、operator2运算符的优先级
     * @param operator1 运算符1
     * @param operator2 运算符2
     * @return 优先级
     */
    @Override
    public int compare(String operator1, String operator2) {
        int i = operatorList.search(operator1);
        int j = operatorList.search(operator2);
        return this.priority[i] - this.priority[j];
    }

    /**
     * 返回 x、y 操作数进行 operator 运算结果
     * @param x 操作数
     * @param y 操作数
     * @param operator 运算符
     * @return 运算结果
     */
    public int operate(int x, int y, String operator) {
        int value = 0;
        switch (operator) {
            case "+": value = x + y; break;
            case "-": value = x - y; break;
            case "*": value = x * y; break;
            case "/": value = x / y; break;
            case "%": value = x % y; break;
            // 注意: Java 中 ^ 表示逻辑异或操作,此处表示幂次方
            case "^": value = (int) Math.pow(x, y); break;
            case "&": value = x & y; break;
            case "|": value = x | y; break;
            default:
        }
        return value;
    }

    /**
     * 返回 x、y 操作数进行 operator 运算结果
     * @param x 操作数
     * @param y 操作数
     * @param operator 运算符
     * @return 运算结果
     */
    public BigDecimal operate(BigDecimal x, BigDecimal y, String operator) {
        BigDecimal value = BigDecimal.ZERO;
        switch (operator) {
            case "+": value = x.add(y); break;
            case "-": value = x.subtract(y); break;
            case "*": value = x.multiply(y); break;
            case "/": value = x.divide(y, 8, RoundingMode.HALF_UP); break;
            case "%": value = x.remainder(y); break;
            // 注意: Java 中 ^ 表示逻辑异或操作,此处表示幂次方
            case "^": value = BigDecimal.valueOf(Math.pow(x.doubleValue(), y.doubleValue())); break;
            case "&": value = BigDecimal.valueOf(x.longValue() & y.longValue()); break;
            case "|": value = new BigDecimal(String.valueOf(x.longValue() | y.longValue())); break;
            default:
        }
        return value;
    }
}
