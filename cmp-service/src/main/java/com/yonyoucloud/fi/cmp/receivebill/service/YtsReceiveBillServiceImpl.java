package com.yonyoucloud.fi.cmp.receivebill.service;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyoucloud.fi.cmp.constant.IActionConstant;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.receivebill.ReceiveBill;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 自动生单推送应收接口
 * @author maliang
 * @version V1.0
 * @date 2021/5/14 13:39
 * @Copyright yonyou
 */
@Service
@Slf4j
public class YtsReceiveBillServiceImpl {
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Transactional
    public BizObject syncBill(BizObject dto) throws  Exception{
        if(log.isInfoEnabled()) {
            log.info("===============================   YtsReceiveBillServiceImpl syncBill ,request msg = {}", JsonUtils.toJson(dto));
        }
        Map paramMap = new HashMap();
        BillContext billContext = new BillContext();
        billContext.setBillnum(IBillNumConstant.RECEIVE_BILL);
        billContext.setAction(IActionConstant.AUTOBILLSAVE);
        paramMap.put("bizObject",dto);
        RuleExecuteResult result = BillBiz.executeRule(IActionConstant.AUTOBILLSAVE,billContext,paramMap);
        Map outParams = result.getOutParams();
        if(outParams !=null && outParams.get("paramMap") != null){
            dto.set("srcbillid",((Map)outParams.get("paramMap")).get("id"));
        }
        dto.setEntityStatus(EntityStatus.Insert);
        dto.setId(ymsOidGenerator.nextId());
        // 在现金库中插入单据
        CmpMetaDaoHelper.insert(ReceiveBill.ENTITY_NAME, dto);

        return dto;
    }
}
