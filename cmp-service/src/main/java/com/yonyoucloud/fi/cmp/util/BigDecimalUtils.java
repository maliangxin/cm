package com.yonyoucloud.fi.cmp.util;

import com.google.common.base.Optional;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;

import java.math.BigDecimal;
import java.util.Map;

public class BigDecimalUtils {


    /**
	 * BigDecimal的加法运算封装
	 * @author : sz
	 * 2017年3月23日下午4:53:21
	 * @param b1
	 * @param bn
	 * @return
	 */
   public static BigDecimal safeAdd(BigDecimal b1, BigDecimal... bn) {
       if (null == b1) {
           b1 = BigDecimal.ZERO;
       }
       if (null != bn) {
           for (BigDecimal b : bn) {
               b1 = b1.add(null == b ? BigDecimal.ZERO : b);
           }
       }
       return b1;
   }

    /**
     * BigDecimal的加法运算封装
     * @author : sz
     * 2017年3月23日下午4:53:21
     * @param bn
     * @return
     */
    public static BigDecimal safeAddObj(Object... bn) {
        BigDecimal[] values = new BigDecimal[bn.length];
        for(int i=0;i<bn.length;i++){
            try {
                values[i] = new BigDecimal(String.valueOf(bn[i]));
            } catch (Exception e) {
                values[i] = BigDecimal.ZERO;
            }
        }
        return BigDecimalUtils.safeAdd(BigDecimal.ZERO,values);
    }
 
   /**
    * Integer加法运算的封装
    * @author : sz
    * 2017年3月23日下午4:54:08
    * @param b1   第一个数
    * @param bn   需要加的加法数组
    * @注 ： Optional  是属于com.google.common.base.Optional<T> 下面的class
    * @return
    */
   public static Integer safeAdd(Integer b1, Integer... bn) {
       if (null == b1) {
           b1 = 0;
       }
       Integer r = b1;
       if (null != bn) {
           for (Integer b : bn) {
               r += Optional.fromNullable(b).or(0);
           }
       }
       return r > 0 ? r : 0;
   }
 
   /**
    * 计算金额方法
    * @author : sz
    * 2017年3月23日下午4:53:00
    * @param b1
    * @param bn
    * @return
    */
   public static BigDecimal safeSubtract(BigDecimal b1, BigDecimal... bn) {
       return safeSubtract(false, b1, bn);
   }

    /**
     * 计算金额方法
     * @author : sz
     * 2017年3月23日下午4:53:00
     * @param b1
     * @param bn
     * @return
     */
    public static BigDecimal safeSubtractObj(Object b1, Object... bn) {
        BigDecimal v1 = BigDecimal.ZERO;
        if (null == b1) {
            v1 = BigDecimal.ZERO;
        }else{
            try {
                v1 = new BigDecimal(String.valueOf(b1));
            } catch (Exception e) {
                v1 = BigDecimal.ZERO;
            }
        }
        BigDecimal[] values = new BigDecimal[bn.length];
        for(int i=0;i<bn.length;i++){
            try {
                values[i] = new BigDecimal(String.valueOf(bn[i]));
            } catch (Exception e) {
                values[i] = BigDecimal.ZERO;
            }
        }
        return BigDecimalUtils.safeSubtract(v1,values);
    }
 
   /**
    * BigDecimal的安全减法运算
    * @author : sz
    * 2017年3月23日下午4:50:45
    * @param isZero  减法结果为负数时是否返回0，true是返回0（金额计算时使用），false是返回负数结果
    * @param b1		   被减数
    * @param bn        需要减的减数数组
    * @return
    */
   public static BigDecimal safeSubtract(Boolean isZero, BigDecimal b1, BigDecimal... bn) {
       if (null == b1) {
           b1 = BigDecimal.ZERO;
       }
       BigDecimal r = b1;
       if (null != bn) {
           for (BigDecimal b : bn) {
               r = r.subtract((null == b ? BigDecimal.ZERO : b));
           }
       }
       return isZero ? (r.compareTo(BigDecimal.ZERO) == -1 ? BigDecimal.ZERO : r) : r;
   }
 
   /**
    * 整型的减法运算，小于0时返回0
    * @author : sz
    * 2017年3月23日下午4:58:16
    * @param b1
    * @param bn
    * @return
    */
   public static Integer safeSubtract(Integer b1, Integer... bn) {
       if (null == b1) {
           b1 = 0;
       }
       Integer r = b1;
       if (null != bn) {
           for (Integer b : bn) {
               r -= Optional.fromNullable(b).or(0);
           }
       }
       return null != r && r > 0 ? r : 0;
   }
 
   /**
    * 金额除法计算，返回2位小数（具体的返回多少位大家自己看着改吧）
    * @author : sz
    * 2017年3月23日下午5:02:17
    * @param b1
    * @param b2
    * @param scale 非空
    * @return
    */
   public static <T extends Number> BigDecimal safeDivide(T b1, T b2,int scale){
       return safeDivide(b1, b2, BigDecimal.ZERO,scale);
   }
 
   /**
    * BigDecimal的除法运算封装，如果除数或者被除数为0，返回默认值
    * 默认返回小数位后2位，用于金额计算
    * @author : sz
    * 2017年3月23日下午4:59:29
    * @param b1
    * @param b2
    * @param defaultValue
    * @param scale 非空
    * @return
    */
   public static <T extends Number> BigDecimal safeDivide(T b1, T b2, BigDecimal defaultValue,int scale) {
       if (null == b1 || null == b2) {
           return defaultValue;
       }
       try {
           return BigDecimal.valueOf(b1.doubleValue()).divide(BigDecimal.valueOf(b2.doubleValue()), scale, BigDecimal.ROUND_HALF_UP);
       } catch (Exception e) {
           return defaultValue;
       }
   }
 
   /**
    * BigDecimal的乘法运算封装
    * @author : sz
    * 2017年3月23日下午5:01:57
    * @param b1
    * @param b2
    * @param scale 非空
    * @return
    */
   public static <T extends Number> BigDecimal safeMultiply(T b1, T b2,int scale) {
       if (null == b1 || null == b2) {
           return BigDecimal.ZERO;
       }
       return BigDecimal.valueOf(b1.doubleValue()).multiply(BigDecimal.valueOf(b2.doubleValue())).setScale(scale, BigDecimal.ROUND_HALF_UP);
   }

    /**
     * BigDecimal的乘法运算封装
     * @author : sz
     * 2017年3月23日下午5:01:57
     * @param b1
     * @param b2
     * @return
     */
    public static <T extends Number> BigDecimal safeMultiply(T b1, T b2) {
        if (null == b1 || null == b2) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(b1.doubleValue()).multiply(BigDecimal.valueOf(b2.doubleValue()));
    }

    public static BigDecimal computeShouldPay(Map<String, Object> map) {
        BigDecimal shouldPay = BigDecimal.ZERO;
        BigDecimal totalInvoiceMoney = CommonHelper.getBigDecimal(map.get("totalInvoiceMoney"));
        BigDecimal qty = CommonHelper.getBigDecimal(map.get("qty"));
        BigDecimal oriTaxUnitPrice = CommonHelper.getBigDecimal(map.get("oriTaxUnitPrice"));
        BigDecimal totalInvoiceQty = CommonHelper.getBigDecimal(map.get("totalInvoiceQty"));
        BigDecimal invPriceExchRate = CommonHelper.getBigDecimal(map.get("invPriceExchRate"));
        BigDecimal oriSum = CommonHelper.getBigDecimal(map.get(IBussinessConstant.ORI_SUM));
        if (totalInvoiceMoney.compareTo(BigDecimal.ZERO) == 0) {
            shouldPay = oriSum;
        } else if (totalInvoiceQty.compareTo(qty) >= 0) {
            shouldPay = totalInvoiceMoney;
        } else if (totalInvoiceQty.compareTo(qty) < 0) {
            BigDecimal noPurinvoiceQty = qty.subtract(totalInvoiceQty);
            BigDecimal noPurinvoicePriceQty = noPurinvoiceQty.divide(invPriceExchRate);
            shouldPay = totalInvoiceMoney.add(oriTaxUnitPrice.multiply(noPurinvoicePriceQty));
        }
        return shouldPay;
    }

    /**
     * 判断两个BigDecimal是否相等
     * @param num1
     * @param num2
     * @return
     */
    public static boolean isEqual(BigDecimal num1, BigDecimal num2) {
        if(num1 == null && num2 != null){
            return false;
        }
        if(num1 != null && num2 == null){
            return false;
        }
        if(num1 != null && num2 != null){
            return num1.compareTo(num2) == 0;
        }
        return true;
    }
    /**
     * 针对BigDecimal进行千分位处理
     * @param num 传入的数值
     * @return
     */
    public static String handleThousandth(BigDecimal num) {
        if (num == null) {
            return null;
        }
        return num.toString().replaceAll("(?!^)(?=(\\d{3})+(\\.))", ",");
    }
}
