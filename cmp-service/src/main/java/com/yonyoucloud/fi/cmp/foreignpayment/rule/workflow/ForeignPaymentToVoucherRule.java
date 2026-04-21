package com.yonyoucloud.fi.cmp.foreignpayment.rule.workflow;

import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.constant.CtmConstants;
import com.yonyou.yonbip.ctm.workbench.ICtmApplicationService;
import com.yonyoucloud.fi.cmp.cmpentity.VoucherStatus;
import com.yonyoucloud.fi.cmp.foreignpayment.ForeignPayment;
import com.yonyoucloud.fi.cmp.foreignpayment.service.ForeignPaymentService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 外汇付款 生成凭证规则，需要在审批事务完成之后 生成凭证
 * *
 *
 * @author xuxbo
 * @date 2023/8/3 14:31
 */

@Slf4j
@Component
public class ForeignPaymentToVoucherRule extends AbstractCommonRule {



    @Autowired
    private ForeignPaymentService foreignPaymentService;

    @Resource
    private IApplicationService appService;


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        // TODO: 2024/1/22  判断审批事务是否已提交
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @SneakyThrows
            @Override
            public void afterCommit() {
                try {
                    //当审批事务已经提交了  然后才能生成凭证
                    for (BizObject bizObject : bills) {
                        String id = bizObject.getId().toString();
                        log.info("ForeignPaymentToVoucherRule bizObject, id = {}, pubTs = {}", bizObject.getId(), bizObject.getPubts());
                        ForeignPayment currentBill = MetaDaoHelper.findById(ForeignPayment.ENTITY_NAME, bizObject.getId());
                        log.info("ForeignPaymentToVoucherRule currentBill, id = {}, pubTs = {}", currentBill.getId(), currentBill.getPubts());
                        boolean enableEVNT = AppContext.getBean(ICtmApplicationService.class).isEnableApp(CtmConstants.YONBIP_FI_EEAC_EVNT_APP_CODE);
                        if (!enableEVNT) {
                            log.error("客户环境未安装事项中台服务");
                            currentBill.setVoucherstatus(VoucherStatus.NONCreate.getValue());
                        } else {
                            foreignPaymentService.toVourcher(currentBill);
                            currentBill.setVoucherstatus(VoucherStatus.POSTING.getValue());
                        }
                        currentBill.setEntityStatus(EntityStatus.Update);
                        MetaDaoHelper.update(ForeignPayment.ENTITY_NAME, currentBill);
                    }
                } catch (Exception e) {
                    log.error("生成凭证失败！",e);
//                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100354"),InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805CC","生成凭证失败！请检查数据！"));
                }

            }

        });

        return new RuleExecuteResult();
    }
}
