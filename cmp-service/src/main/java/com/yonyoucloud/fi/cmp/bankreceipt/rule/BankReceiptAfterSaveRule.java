package com.yonyoucloud.fi.cmp.bankreceipt.rule;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.precision.CheckPrecisionVo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreceipt.service.TaskBankReceiptService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.ReceiptassociationStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;


@Slf4j
@Component("bankReceiptAfterSaveRule")
public class BankReceiptAfterSaveRule extends AbstractCommonRule {

    @Autowired
    private TaskBankReceiptService taskBankReceiptService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);

        if (bills != null && bills.size() > 0) {
            for (BizObject bill : bills) {
                Long id = Long.parseLong(bill.getId().toString());
                //查最新的，否则pubts和库里的不同，更新时报错：当前单据不是最新状态，请刷新重试。
                List<BizObject> bizObjects = CmpMetaDaoHelper.queryColByOneEqualCondition(BankElectronicReceipt.ENTITY_NAME, ICmpConstant.ID, id, "*");
                BizObject bizObject = bizObjects.get(0);
                //根据流水号查询是否有匹配的银行对账单 给关联状态赋值
                //转换入参格式
                BankElectronicReceipt bankElectronicReceipt = new BankElectronicReceipt();
                bankElectronicReceipt.init(bizObject);
                Map<String, Object> inputMap = new HashMap<>();
                inputMap.put("enterpriseBankAccount", bankElectronicReceipt.getEnterpriseBankAccount());
                inputMap.put("receipts", Arrays.asList(bankElectronicReceipt));
                if (bankElectronicReceipt.getIsdown() == null) {
                    //数据库默认为false，这里给值是为了后面匹配流水时使用，否则报空指针
                    bankElectronicReceipt.setIsdown(false);
                }
                taskBankReceiptService.matchBankreceiptAndBankReconciliation(Arrays.asList(inputMap));
            }
        }
        return new RuleExecuteResult();
    }


}
