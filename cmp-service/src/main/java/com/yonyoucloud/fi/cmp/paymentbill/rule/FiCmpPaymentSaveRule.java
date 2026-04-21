package com.yonyoucloud.fi.cmp.paymentbill.rule;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.billmake.vo.PushAndPullVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.mdd.ext.trans.itf.ISagaRule;
import com.yonyou.ucf.mdd.ext.util.JsonUtils;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.pushAndPull.PushAndPullService;
import com.yonyoucloud.fi.cmp.common.service.ArapBillServiceImpl;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.paybill.PayBill;
import com.yonyoucloud.fi.cmp.paybill.PayBillCharacterDef;
import com.yonyoucloud.fi.cmp.paybill.PayBillCharacterDefb;
import com.yonyoucloud.fi.cmp.paybill.PayBillb;
import com.yonyoucloud.fi.cmp.pushAndPull.PushAndPullModel;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.CmpWriteBankaccUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 付款单远程推送保存规则
 *
 * @author liuttm
 * @version V1.0
 * @date 2021/4/20 16:00
 * @Copyright yonyou
 */
@Slf4j
@Component
public class FiCmpPaymentSaveRule extends FiCmpPaymentBaseRule implements ISagaRule {

    private static final String PAYMAKEBILLCODE = "arapToCmpPaybill";

    @Autowired
    PushAndPullService pushAndPullService;
    @Autowired
    private ArapBillServiceImpl arapBillServiceImpl;
    @Autowired
    BankAccountSettingService bankaccountSettingService;
    private static final String ENTERPRISEBANKACCOUNT = "enterprisebankaccount";
    private static final String ENTERPRISEBANKACCOUNTS = "enterpriseBankAccount";

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(CmpCommonUtil.getNewFiFlag()){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-103014"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_20C255F404A00006", "在财务新架构环境下，不允许保存付款单。") /* "在财务新架构环境下，不允许保存付款单。" */);
        }
        if(log.isInfoEnabled()) {
            log.info("FiCmpPaymentBillSaveRule , request paramMap = {}", JsonUtils.toJson(paramMap));
        }

        //校验付款工作台有没有开通，没开通则抛异常提示
//        arapBillServiceImpl.checkService(IBillNumConstant.PAYMENT);
        //获取上游表单内容
        List<BizObject> bills = getBills(billContext, paramMap);
        // 收付推送过来的单据为空，直接结束流程
        if (bills.size() == 0) {
            return new RuleExecuteResult();
        }
        // 根据结算方式判断是否保存
        boolean serviceFlag = arapBillServiceImpl.checkSettleMode(bills.get(0).get("settlemode"));
        PayBill bill = getPayBill(bills.get(0).getId());
        // 修改操作，先将原单据删除，再保存新单据
        if (bill != null) {
            log.debug("executing   deleterule !!!!!");
            YtsContext.setYtsContext("oldbill", bill);
            CmpWriteBankaccUtils.delAccountBook(bill.getId().toString());
            MetaDaoHelper.delete(PayBill.ENTITY_NAME, bill);
        }
        //查询lisence是否有效 true-有效
        boolean lisenceValidFlag = arapBillServiceImpl.checkLisenceValid();
        // 应付的单据类型的结算方式不是现金可处理单据或者lisence过期，则结束流程
        if (!serviceFlag || !lisenceValidFlag) {
            return new RuleExecuteResult();
        }

        boolean newArch = true;//新架构标识
        BizObject bizObject = null;
        if (newArch) {
            // 新增操作调用生单规则，直接将数据插入
            PushAndPullModel pushAndPullModel = new PushAndPullModel();
            pushAndPullModel.setNeedDivide(false);//是否需要分单
            pushAndPullModel.setCode(PAYMAKEBILLCODE);
            bizObject = pushAndPullService.transformBillByMakeBillCode(bills, pushAndPullModel);//转单
            // 转换之后的单据进行保存前的校验
            arapBillServiceImpl.beforeSaveBillToCmp(bizObject);
        } else {
            PushAndPullVO vo = new PushAndPullVO();
            vo.setCode(PAYMAKEBILLCODE);
            vo.setIsMainSelect(0);
            vo.setSourceData(bills);
            BillDataDto billDataDto = getTargetDataDto(vo);
            billDataDto.setBillnum(IBillNumConstant.PAYMENT);
            // 转换之后的单据进行保存前的校验
            bizObject = arapBillServiceImpl.beforeSaveBillToCmp(billDataDto);
        }
        //是否直联
        String enterpriseBankAccount = bizObject.get(ENTERPRISEBANKACCOUNT);
        if (!StringUtils.isEmpty(enterpriseBankAccount)) {
            String data = bankaccountSettingService.getOpenFlag(enterpriseBankAccount);
            CtmJSONObject jsonObject = CtmJSONObject.parseObject(data);
            if (null != jsonObject) {
                CtmJSONObject jsonData = jsonObject.getJSONObject("data");
                if (null != jsonData) {
                    bizObject.set("isdirectconn", jsonData.get("openFlag"));
                }
            }
        }
        // 新增数据直接插入
        bizObject.setEntityStatus(EntityStatus.Insert);
        List<PayBillb> payBillbs = bizObject.getBizObjects("PayBillb", PayBillb.class);
        if (payBillbs != null && payBillbs.size() > 0) {
            for (PayBillb payBillb : payBillbs) {
                PayBillCharacterDefb payBillCharacterDefb = payBillb.getBizObject("payBillCharacterDefb", PayBillCharacterDefb.class);
                if (payBillCharacterDefb != null) {
                    payBillCharacterDefb.setId(payBillb.getId());
                    payBillCharacterDefb.setEntityStatus(EntityStatus.Insert);
                    payBillb.set("payBillCharacterDefb", payBillCharacterDefb);
                }
            }
        }
        PayBillCharacterDef payBillCharacterDef = bizObject.getBizObject("payBillCharacterDef", PayBillCharacterDef.class);
        if (payBillCharacterDef != null) {
            payBillCharacterDef.setId(bizObject.getId());
            payBillCharacterDef.setEntityStatus(EntityStatus.Insert);
            bizObject.set("payBillCharacterDef", payBillCharacterDef);
        }

        CmpMetaDaoHelper.insert(PayBill.ENTITY_NAME, bizObject);
        YtsContext.setYtsContext("newBizObjectId", bizObject.getId());
        //保存后的单据进行记账处理
        BillContext newBillContext = new BillContext();
        newBillContext.setFullname(PayBill.ENTITY_NAME);
        newBillContext.setBillnum(IBillNumConstant.PAYMENT);
        arapBillServiceImpl.afterSaveBillToCmp(newBillContext, bizObject);
        return new RuleExecuteResult();
    }


    /**
     * 回滚操作，先删除新单据，再保存旧单据。
     *
     * @param billContext
     * @param paramMap
     * @return
     * @throws Exception
     */
    @Override
    public RuleExecuteResult cancel(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        if(log.isInfoEnabled()) {
            log.info("FiCmpPaymentBillSaveRule cancel, request paramMap = {}", JsonUtils.toJson(paramMap));
        }
        // 若单据已保存成功，则删除新增单据
        Long newBizObjectId = (Long) YtsContext.getYtsContext("newBizObjectId");
        if (newBizObjectId != null) {
            //删除日记账，更新账面余额
            PayBill newBill = MetaDaoHelper.getById(PayBill.ENTITY_NAME, newBizObjectId);
            CmpWriteBankaccUtils.delAccountBook(newBizObjectId.toString());
            MetaDaoHelper.delete(PayBill.ENTITY_NAME, newBill);
        }
        // 如果存在旧单据，则将删除的旧单据恢复，执行保存逻辑，并将原code保存回原单据
        PayBill oldbill = (PayBill) YtsContext.getYtsContext("oldbill");
        if (oldbill == null) {
            return new RuleExecuteResult();
        }
        oldbill.setEntityStatus(EntityStatus.Insert);
        CmpMetaDaoHelper.insert(PayBill.ENTITY_NAME, oldbill);
        //保存后的单据进行记账处理
        BillContext newBillContext = new BillContext();
        newBillContext.setFullname(PayBill.ENTITY_NAME);
        newBillContext.setBillnum(IBillNumConstant.PAYMENT);
        arapBillServiceImpl.afterSaveBillToCmp(newBillContext, oldbill);
        return new RuleExecuteResult();
    }

}