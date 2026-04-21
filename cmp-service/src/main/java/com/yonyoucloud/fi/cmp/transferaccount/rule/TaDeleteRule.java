package com.yonyoucloud.fi.cmp.transferaccount.rule;

import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.voucher.enums.Status;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CtmCmpCheckRpcService;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.checkstock.CheckStock;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpCheckStatus;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpInputBillDir;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CmpLock;
import com.yonyoucloud.fi.cmp.cmpentity.CheckPurpose;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.enums.CheckDirection;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.CheckOperationType;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.SystemType;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.BillType;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.vo.checkstock.CheckDTO;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @ClassName TaDeleteRule
 * @Desc 转账单删除rule
 * @Author tongyd
 * @Date 2019/10/10
 * @Version 1.0
 */
@Component("taDeleteRule")
public class TaDeleteRule extends AbstractCommonRule {


    @Autowired
    private CtmCmpCheckRpcService ctmCmpCheckRpcService;

    @Autowired
    private CmCommonService cmCommonService;

    @Autowired
    private AutoConfigService autoConfigService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BizObject bizObject = getBills(billContext, paramMap).get(0);
        //pubts校验
        TransferAccount transferAccount = MetaDaoHelper.findById(TransferAccount.ENTITY_NAME, bizObject.getId());

        //影像相关
        Map<String, Object> autoConfigMap = cmCommonService.queryAutoConfigByAccentity(bizObject.get(ICmpConstant.ACCENTITY)) ;
        if(!Objects.isNull(autoConfigMap) && null != autoConfigMap.get("isShareVideo") && (Boolean) autoConfigMap.get("isShareVideo")){
            //走影像
            BillBiz.executeRule("shareDelete", billContext, paramMap);
        }

        if (bizObject.getPubts().compareTo(transferAccount.getPubts()) != 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100433"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180613","数据无效，请刷新后重试") /* "数据无效，请刷新后重试" */);
        }
        Short status = bizObject.get("status");
        BillDataDto bill = (BillDataDto)paramMap.get("param");
        if(status !=null && status== Status.confirming.getValue()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100434"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180615","该单据正在进行审批流程，无法删除") /* "该单据正在进行审批流程，无法删除" */);
        }
        //校验单据来源，如果不是现金自制单据，则不可删除
        Short srcItem = -1;
        if (bizObject.get("srcitem") != null) {
            srcItem = Short.valueOf(bizObject.get("srcitem").toString());
        }
        Short settleStatus = -1;
        if(bizObject.get("settlestatus")!=null){
            settleStatus = Short.valueOf(bizObject.get("settlestatus").toString());
        }
        if((bill.getPartParam() == null || bill.getPartParam().get("outsystem") == null)){
            if(!srcItem.equals(EventSource.Cmpchase.getValue())&&!srcItem.equals(EventSource.ManualImport.getValue())
                    &&!srcItem.equals(EventSource.ThreePartyReconciliation.getValue())){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100435"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180614","该单据不是现金自制单据，不能进行删除！") /* "该单据不是现金自制单据，不能进行删除！" */);
            }
        }

        if(srcItem.equals(EventSource.Drftchase.getValue())&&settleStatus.equals(SettleStatus.alreadySettled.getValue())){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100436"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180616","此商业汇票单据已结算，不能删除") /* "此商业汇票单据已结算，不能删除" */);
        }

        if (bizObject.size() > 0){
            BizObject currentBill = MetaDaoHelper.findById(billContext.getFullname(), bizObject.getId(), 3);
            if (currentBill.get("checkid")!= null) {
                Boolean checkFundTransfer = autoConfigService.getCheckFundTransfer();

                if (!checkFundTransfer) {
                    CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, transferAccount.getCheckid());

                    List<CheckDTO> checkDTOOriginal = this.setOriginValue(checkStock, transferAccount);
                    ctmCmpCheckRpcService.checkOperation(checkDTOOriginal);
                } else {
                    CheckStock checkStock = MetaDaoHelper.findById(CheckStock.ENTITY_NAME, transferAccount.getCheckid());
                    checkStock.setOccupy((short) 0);
                    checkStock.setEntityStatus(EntityStatus.Update);
                    MetaDaoHelper.update(CheckStock.ENTITY_NAME, checkStock);
                }
            }
        }

        //删除日记账
        CmpWriteBankaccUtils.delAccountBook(bizObject.getId().toString());
        return new RuleExecuteResult();
    }

    private List<CheckDTO> setOriginValue(CheckStock checkStock, TransferAccount transferAccount) {
        List<CheckDTO> checkDTOS = new ArrayList<>();

        CheckDTO checkDTO = new CheckDTO();

        //操作类型： 1.支票锁定/解锁接口、2.支票付票接口、3.支票兑付/背书接口、4.支票作废接口
        checkDTO.setOperationType(CheckOperationType.Unlock);
        //锁定类型  锁定状态 1锁定 0解锁
        checkDTO.setLock(CmpLock.NO);
        //支票编号ID
        checkDTO.setCheckBillNo(String.valueOf(transferAccount.getCheckid()));
        //业务单据明细ID
        checkDTO.setInputBillNo(transferAccount.getCode());
        //单据方向 2：付款 1：收款(按照结算的枚举来)
        checkDTO.setInputBillDir(CmpInputBillDir.Pay);
        //单据方向，字符串类型
        checkDTO.setInputBillDirString(CmpInputBillDir.Pay.getName());
        //支票状态:已入库
        checkDTO.setCheckBillStatus(CmpCheckStatus.InStock.getValue());
        //业务系统
        checkDTO.setSystem(SystemType.CashManager);
        //业务单据类型
        checkDTO.setBillType(BillType.TransferAccount);
        //支票方向
        checkDTO.setCheckBillDir(CheckDirection.Pay.getIndex());
        //支票用途 默认0提现，1转账
        checkDTO.setCheckPurpose(CheckPurpose.VirtualToBank.getValue());

        checkDTOS.add(checkDTO);
        return checkDTOS;
    }
}
