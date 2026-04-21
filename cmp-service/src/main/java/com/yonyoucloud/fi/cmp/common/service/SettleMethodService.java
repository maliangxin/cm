package com.yonyoucloud.fi.cmp.common.service;

import com.yonyou.yonbip.ctm.json.CtmJSONObject;

import java.util.List;
import java.util.Map;

/**
 * @description: 结算方式 aa.settlemethod.SettleMethod
 * @author: wenyuhao 
 * @date: 2023/12/18 11:09
 * @param: 
 * @return: 
 **/
public interface SettleMethodService {

    /**
     * @description: 查找结算方式为银行转账且是否直连为是的数据
     * @author: wenyuhao
     * @date: 2023/12/29 14:51
     * @param: []
     * @return: java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     **/
    List<Map<String, Object>> listSettleMethodByBankTransSettlemode(String settlemode) throws Exception;

    List<Map<String, Object>> getSettleMethodByBankTransSettlemode(String settlemode) throws Exception;

    /**
     * @description: 结算方式为银行转账且是否直连为是时为true
     * @author: wenyuhao
     * @date: 2023/12/29 14:53
     * @param: []
     * @return: java.lang.Boolean
     **/
    Boolean checkSettleMethod(String settlemode) throws Exception;

    /**
     * @description: 判断结算方式是否由非直连的银行转账变为直连银行转账，是则返回true
     * @author: wenyuhao
     * @date: 2024/1/11 9:16
     * @param: [param]
     * @return: java.lang.Boolean
     **/
    Boolean checkSettleMethodCleanPayBankAccount(CtmJSONObject param) throws Exception;

    /**
     * 根据业务属性获取结算对象对应的值的id
     * @param service_attr
     * @return
     * @throws Exception
     */
    List<Object> listSettleMethodByService_attr(int service_attr) throws Exception;
}
