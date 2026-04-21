package com.yonyoucloud.fi.cmp.electronicstatementconfirm.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.electronicstatementconfirm.ElectronicStatementConfirm;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Date 2024年10月23日11:13:39
 **/
@Component
public class ElectronicStatementConfirmDelete extends AbstractCommonRule {

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        List<String> inputTypeErrorStatementnoList = new ArrayList<>();
        List<String> archiving_statusErrorStatementnoList = new ArrayList<>();
        for (BizObject bill : bills) {
            ElectronicStatementConfirm electronicStatementConfirm = MetaDaoHelper.findById(ElectronicStatementConfirm.ENTITY_NAME, bill.getId(), null);
            //如果当前单据是迁移单据，需要查询上游单据有没有 付款申请，有付款申请的 不能删除
            if(electronicStatementConfirm.getInputtype()!=null && electronicStatementConfirm.getInputtype() == DateOrigin.DownFromYQL.getValue()){
                inputTypeErrorStatementnoList.add("[" + electronicStatementConfirm.getStatementno() + "]");
                //// 数据来源：银企联下载，不能删除;提示语："电子对账单编号【XXX】、【YYY】数据来源为银企直联，不允许删除，请检查！"
                //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100696"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B0", "电子对账单编号") /* "电子对账单编号" */ + electronicStatementConfirm.getStatementno() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800AF", "数据来源为银企直联，不允许删除，请检查！") /* "数据来源为银企直联，不允许删除，请检查！" */);
            }
            ////判定归档状态，如为已归档时，则不允许删除，提示“电子对账单编号【XXX】、【YYY】已完成归档，不允许删除请检查！”
            //Boolean archiving_status = electronicStatementConfirm.getArchiving_status();
            //if (archiving_status) {
            //    archiving_statusErrorStatementnoList.add("[" + electronicStatementConfirm.getStatementno()+ "]");
            //    //throw new CtmException("电子对账单编号：" + electronicStatementConfirm.getStatementno() + "已完成归档，不允许删除请检查！");
            //}
        }
        String errorMessage = "";
        if (inputTypeErrorStatementnoList.size() > 0) {
            errorMessage = errorMessage + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800B0", "电子对账单编号") /* "电子对账单编号" */ + String.join("、", inputTypeErrorStatementnoList) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800AF", "数据来源为银企直联，不允许删除，请检查！") + "\n";
        }
        if (archiving_statusErrorStatementnoList.size() > 0) {
            errorMessage = errorMessage + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400577", "电子对账单编号：") /* "电子对账单编号：" */ + String.join("、", archiving_statusErrorStatementnoList) + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400578", "已完成归档，不允许删除请检查！") /* "已完成归档，不允许删除请检查！" */;//@notranslate
        }
        if (errorMessage.length() > 0) {
            throw new CtmException(errorMessage);
        }
        return new RuleExecuteResult();
    }
}
