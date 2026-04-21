package com.yonyoucloud.fi.cmp.bankreceipt.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankRecepitSignService;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.SignStatus;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * desc:银行账户回单验签前规则
 * author:wangqiangac
 * date:2023/8/15 15:37
 */
@Component("bankRecepitSignRule")
public class BankRecepitSignRule extends AbstractCommonRule {
    @Autowired
    BankRecepitSignService bankRecepitSignService;
    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        String billnum = billDataDto.getBillnum();
        List<BizObject> bills = getBills(billContext, map);
        if (StringUtils.isEmpty(billnum)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101590"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418060F","传入的billnum为空，请检查") /* "传入的billnum为空，请检查" */);
        }
        BankElectronicReceipt bankElectronicReceipt = (BankElectronicReceipt) bills.get(0);
        SignStatus signStatus = bankElectronicReceipt.getSignStatus();
        String receiptno = bankElectronicReceipt.getReceiptno();
        DateOrigin dataOrigin = bankElectronicReceipt.getDataOrigin();
        Boolean isdown = bankElectronicReceipt.getIsdown();
        if(!isdown){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101591"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1910505E05800014", "选择的数据无对应的回单文件，请先下载相应的电子回单文件!") /* "选择的数据无对应的回单文件，请先下载相应的电子回单文件!" */);
        }
        String fileName = bankElectronicReceipt.getFilename();
        if(!fileName.endsWith(".ofd")){
            // 回单编号：XXX 非OFD格式文件，不支持进行验签，请检查!
            /* 多语-"回单编号" */
            String receiptNoLang = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CTM-CMP-MD_1808A9F004D007A4", "回单编号");
            /* 多语-"非OFD格式文件，不支持进行验签，请检查!" */
            String msg = com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D84016404300008", "非OFD格式文件，不支持进行验签，请检查!");
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101592"),receiptNoLang + "：" + receiptno + " " + msg);//@notranslate
        }
        if(DateOrigin.find(1).getValue() != dataOrigin.getValue()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101593"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1910505E05800015", "选择的数据非银企直联下载，不支持进行验签，请检查!") /* "选择的数据非银企直联下载，不支持进行验签，请检查!" */);
        }
        if(signStatus != null){
            if(SignStatus.UnSupported.getValue()==signStatus.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101594"),receiptno+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18E91B5C0550000D", "不支持验签") /* "不支持验签" */);
            }else if(SignStatus.SignSuccess.getValue()==signStatus.getValue()){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101595"),receiptno+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18E91B5C0550000E", "已经验签成功") /* "已经验签成功" */);
                //throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101596"),receiptno+com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18E91B5C0550000F", "验签") /* "验签" */);
            }
        }
        bankRecepitSignService.bankRecepitSign(bankElectronicReceipt);
        return new RuleExecuteResult();
    }
}
