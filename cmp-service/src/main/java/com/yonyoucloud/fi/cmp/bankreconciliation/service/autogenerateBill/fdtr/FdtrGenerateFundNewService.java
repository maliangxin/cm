package com.yonyoucloud.fi.cmp.bankreconciliation.service.autogenerateBill.fdtr;

import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.ctm.stwb.paramsetting.pubitf.ISettleParamPubQueryService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.SendEventMessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 到账认领V2
 * 银行对账单 --> 自动生单 --> 资金收付款单
 * 新建service 避免侵入原BusinessGenerateFundService
 *
 * @author yp
 */
@Slf4j
@Service
@Transactional(rollbackFor = RuntimeException.class)
public class FdtrGenerateFundNewService {

    /**
     * 对账单到资金调度，事件源编码
     */
    private final String CMP_TO_FDTR_EVENT_SOURCE = "cmp_bankreconciliation";
    /**
     * 对账单到资金调度，事件类型
     */
    private final String CMP_TO_FDTR_EVENT_TYPE = "cmp_bankreconciliation_to_fdtr";

    @Autowired
    private ISettleParamPubQueryService settleParamPubQueryService;

    /**
     * 1,根据autoorderrule判断生成收款单或付款单 -- 枚举 EventType
     * 2,调用收付款单不同的生单Service
     *
     * @param bankReconciliations
     * @param eventType
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = RuntimeException.class)
    public String generateBill(List<BankReconciliation> bankReconciliations, EventType eventType ) throws Exception {
        StringBuffer retMsg = new StringBuffer();
        for (BankReconciliation bankReconciliation : bankReconciliations) {
            List<BankReconciliation> bankReconciliationList = new ArrayList<>();
            bankReconciliationList.add(bankReconciliation);
            //事件中心，发送的数据包装类
            BizObject userObject = new BizObject();
            // 流水ID
            userObject.put("bankreconciliationId", bankReconciliation.getId());
            userObject.put("bankSeqNo", bankReconciliation.getBank_seq_no());
            //业务单据类型
            userObject.put("billtype", eventType.getValue());
            //对账单生单数据
            userObject.put("datalist", bankReconciliationList);
            //发送消息到事件中心
            bankReconciliation.set("pushDownMark", "true");
            SendEventMessageUtils.sendEventMessageEos(userObject, CMP_TO_FDTR_EVENT_SOURCE, CMP_TO_FDTR_EVENT_TYPE);
        }
        //修改生单状态
        updateToFdtrIsautocreatebill(bankReconciliations);
        return retMsg.toString();
    }

    /**
     * 更新是否自动生单状态为是
     */
    private void updateToFdtrIsautocreatebill(List<BankReconciliation> datalist) throws Exception {
        //已匹配到的银行对账单id
        List<Long> matchedIds = new ArrayList<>();
        for (BankReconciliation bankReconciliation : datalist) {
            matchedIds.add(bankReconciliation.getId());
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("id").in(matchedIds));
        querySchema.addCondition(group);
        List<BankReconciliation> toUpdateDataList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema, null);
        toUpdateDataList.forEach(b -> {
            //设置更新状态，更改为已自动生单
            EntityTool.setUpdateStatus(b);
            b.setIsautocreatebill(true);
        });
        CommonSaveUtils.updateBankReconciliation(toUpdateDataList);
    }

}