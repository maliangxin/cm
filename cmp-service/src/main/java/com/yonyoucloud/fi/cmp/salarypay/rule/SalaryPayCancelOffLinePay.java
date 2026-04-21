package com.yonyoucloud.fi.cmp.salarypay.rule;

import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.salarypay.SalaryPayService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 *  薪资支付工作台 -- 支付单取消线下支付
 *  JIRA -- CZFW-37436
 *  薪资支付工作台 结算 取消结算改为异步处理
 *  支付单取消线下支付接口适配平台异步处理 改为 支付单取消线下支付rule
 */
@Component
@Slf4j
public class SalaryPayCancelOffLinePay extends AbstractCommonRule {
    @Autowired
    private SalaryPayService salaryService;
    /**
     * 同 薪资支付工作台 -- 支付单取消线下支付 接口逻辑保持一致
     * 调用 salaryService -- cancelOffLinePay 方法进行支付单取消线下支付操作
     * @param billContext
     * @param map
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        CtmJSONObject params = new CtmJSONObject();
        List<BizObject> bills = getBills(billContext, map);
        List<CtmJSONObject> rows = new ArrayList<>();
        Long id = null;
        try{
            if (bills != null && bills.size() > 0) {
                BizObject bizobject = bills.get(0);
                id = bizobject.getId();
                if (!ValueUtils.isNotEmptyObj(id)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102007"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418034D","操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
                }
                rows.add(CtmJSONObject.parseObject(JsonUtils.toJson(bizobject)));
                params.put("rows", rows);
                salaryService.cancelOffLinePay(params);
            }
        } catch (Exception e){
            log.error("薪资结算支付单线下支付报错"+e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102008"),e.getMessage());
        }finally {
           List<YmsLock> ymsLockList = (List<YmsLock>) params.get("ymsLockList");
           if(!CollectionUtils.isEmpty(ymsLockList)){
               for(int i=0;i<ymsLockList.size();i++){
                   YmsLock ymsLock = ymsLockList.get(i);
                   JedisLockUtils.unlockBillWithOutTrace(ymsLock);
               }
           }
        }
        return new RuleExecuteResult();
    }
}
