package com.yonyoucloud.fi.cmp.weekday;

import com.yonyoucloud.fi.basecom.util.BigDecimalUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * @author squirrelzt
 * @date 2022/05/03
 * @description 算数表达式
 */
public class ArithmeticExpression {


    /**
     * 运算符集合
     */
    private static Operators operators;

    static {
        operators = new Operators();
    }

    /**
     * int 型数据运算
     * @param infix 后缀表达式
     * @return 运算结果
     */
    public int calcInt(String infix) {
        StringBuilder postfix = toPostfix(infix);
        return toValue(postfix);
    }

    /**
     * BigDecimal 型数据运算
     * @param infix 后缀表达式
     * @return 运算结算
     */
    public BigDecimal calc(String infix) {
        StringBuilder postfix = toPostfix(infix);
        return toValueBigDecimal(postfix);
    }

    /**
     * 计算利率，保留小数点后 8 位
     * @param infix 后缀表达式
     * @return 计算结果
     */
    public BigDecimal calcInterestRate(String infix) {
        return calc(infix).setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * 计算金额,保留小数点后 2 位
     * @param infix 后缀表达式
     * @return 计算结果
     */
    public BigDecimal calcAmount(String infix) {
        return calc(infix).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 将中缀表达式转换为后缀表达式
     * @param infix 中缀表达式
     * @return 后缀表达式
     */
    public StringBuilder toPostfix(String infix) {
        // 运算符栈,顺序栈
        Stack<String> stack = new SeqStack<>(infix.length());
        // 后缀表达式字符串
        StringBuilder postfix = new StringBuilder(infix.length() * 2);
        int i = 0;
        while (i < infix.length()) {
            char ch = infix.charAt(i);
            if (ch == '.' || (ch >= '0' && ch <= '9')) {
                while (i < infix.length() && ((ch = infix.charAt(i)) == '.' || (ch >= '0' && ch <= '9'))) {
                    postfix.append(ch);
                    i++;
                }
                postfix.append(" ");
            } else {
                switch (ch) {
                    // 跳过空格
                    case ' ':
                        i++;
                        break;
                    case '(':
                    case '[':
                        stack.push(ch +"");
                        i++;
                        break;
                    case ')':
                        String out = "";
                        while ((out = stack.pop()) != null && !"(".equals(out)) {
                            postfix.append(out);
                        }
                        i++;
                        break;
                    case ']':
                        String out1 = "";
                        while ((out1 = stack.pop()) != null && !"[".equals(out1)) {
                            postfix.append(out1);
                        }
                        i++;
                        break;
                    default:
                        boolean minusFlag = false;
                        if (ch == '-') {
                            if (i == 0) {
                                minusFlag = true;
                            } else {
                                char ch1 = getChar(infix, i - 1);
                                if (ch1 == '+' || ch1 == '-' || ch1 == '*' || ch1 == '/' || ch1 == '^') {
                                    minusFlag = true;
                                } else if (ch1 == '(' || ch1 == ')' || ch1 == '[' || ch1 == ']') {
                                    minusFlag = checkMinus(i - 1, infix);
                                }
                            }
                        }
                        if (minusFlag) {
                            postfix.append("~");
                            i++;
                            break;
                        }
                        //遇到所有运算符{"^", "*", "/", "%", "+", "-", "&", "|"},将ch运算符的优先级与栈顶运算符的优先级比较
                        // 若栈顶运算符优先级高,则出栈
                        while (!stack.isEmpty() && !"(".equals(stack.peek()) && !"[".equals(stack.peek())
                                && operators.compare(ch + "", stack.peek()) >= 0) {
                            postfix.append(stack.pop());
                        }
                        stack.push(ch + "");
                        i++;
                }
            }
        }
        // 所有运算符出栈,添加到 postfix 串之后
        while (!stack.isEmpty()) {
            postfix.append(stack.pop());
        }
        return postfix;
    }

    /**
     * 获取 infix 第 index，如果为空格，则取上1位
     * @param infix 表达式字符串
     * @param index 索引
     * @return 字符
     */
    private char getChar(String infix, int index) {
        char ch = infix.charAt(index);
        int j = 1;
        while (ch == ' ' && index - j > 0) {
            ch = infix.charAt(index - j);
            j++;
        }
        return ch;
    }

    /**
     * 校验是否是负号
     * @param index 索引
     * @param infix 中缀表达式
     * @return 是否是负号
     */
    public boolean checkMinus(int index, String infix) {
        char ch = infix.charAt(index);
        if (ch == '(' || ch == ')' || ch == '[' || ch == ']' || ch == ' ') {
            return checkMinus(index - 1, infix);
        } else {
            return ch == '+' || ch == '-' || ch == '*' || ch == '/' || ch == '^';
        }
    }

    /**
     * 计算后缀表达式的值
     * @param postfix 后缀表达式
     * @return 结算结果
     */
    public int toValue(StringBuilder postfix) {
        // 操作数栈,链式栈
        Stack<Integer> stack = new LinkedStack<>();
        int value;
        for (int i = 0;i < postfix.length(); i++) {
            char ch = postfix.charAt(i);
            // 数字字符
            if (ch >= '0' && ch <= '9') {
                value = 0;
                // 将整数字符串转化为整数值,没有符号，以空格结束
                while (ch >= '0' && ch <= '9') {
                    value = value * 10 + ch - '0';
                    ch = postfix.charAt(++i);
                }
                stack.push(value);
            } else {
                // 约定操作数后有一个空格分割
                if (ch != ' ') {
                    // 出栈两个操作数,注意出栈顺序
                    int y = stack.pop();
                    int x = stack.pop();
                    value = operators.operate(x, y, ch + "");
                    stack.push(value);
                }
            }
        }
        return stack.pop();
    }

    /**
     * 计算后缀表达式的值
     * @param postfix 后缀表达式
     * @return 结算结果
     */
    public BigDecimal toValueBigDecimal(StringBuilder postfix) {
        // 操作数栈,链式栈
        Stack<BigDecimal> stack = new LinkedStack<>();
        BigDecimal value;
        for (int i = 0;i < postfix.length(); i++) {
            boolean hasRadix = false;
            char ch = postfix.charAt(i);
            if (ch == '~') {
                continue;
            }
            // 数字字符
            if (ch == '.' || (ch >= '0' && ch <= '9')) {
                BigDecimal value1 = BigDecimal.ZERO;
                // 将字符串转化为数值,没有符号，以空格结束
                while (ch >= '0' && ch <= '9') {
                    BigDecimal cur = BigDecimal.valueOf(ch - '0');
                    value1 = BigDecimalUtils.safeMultiply(value1 , 10).add(cur);
                    ch = postfix.charAt(++i);
                }
                if (ch == '.') {
                    hasRadix = true;
                    ch = postfix.charAt(++i);
                }
                BigDecimal value2 = BigDecimal.ZERO;
                int j = 0;
                while (hasRadix && ch >= '0' && ch <= '9') {
                    BigDecimal cur = BigDecimal.valueOf(ch - '0');
                    value2 = BigDecimalUtils.safeMultiply(value2 , 10).add(cur);
                    ch = postfix.charAt(++i);
                    j++;
                }
                value = value1.add(value2.divide(new BigDecimal(Double.toString(Math.pow(10, j))), 8, RoundingMode.HALF_UP));
                if (i > 1 && postfix.charAt(i - 2) == '~') {
                    value = value.multiply(BigDecimal.valueOf(-1));
                }
                stack.push(value);
            } else {
                // 约定操作数后有一个空格分割
                if (ch != ' ') {
                    // 出栈两个操作数,注意出栈顺序
                    BigDecimal y = stack.pop();
                    BigDecimal x = stack.pop();
                    BigDecimal result = operators.operate(x, y, ch + "");
                    stack.push(result);
                }
            }
        }
        return stack.pop();
    }
}
