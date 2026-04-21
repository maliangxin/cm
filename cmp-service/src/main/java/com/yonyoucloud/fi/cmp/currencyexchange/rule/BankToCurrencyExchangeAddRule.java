package com.yonyoucloud.fi.cmp.currencyexchange.rule;

import com.yonyou.iuap.ucf.common.i18n.InternationalUtils;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.billclaim.BillClaim;
import com.yonyoucloud.fi.cmp.cmpentity.AssociationStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.currencyexchange.CurrencyExchange;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.bankrecilication.CtmcmpReWriteBusRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.bankreconciliation.CorrDataEntity;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.billclaim.CorrDataEntityParam;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description: 认领单对账单生成外币兑换单，保存前规则，用来处理生单关联关系回写
 * @author: wanxbo@yonyou.com
 * @date: 2023/7/18 14:18
 */

@Slf4j
@Component("bankToCurrencyExchangeAddRule")
public class BankToCurrencyExchangeAddRule extends AbstractCommonRule {

    @Autowired
    CtmcmpReWriteBusRpcService ctmcmpReWriteBusRpcService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        //只处理外币兑换单
        String billNum = billContext.getBillnum();
        if (!"cmp_currencyexchange".equals(billNum)) {
            return new RuleExecuteResult();
        }
        //获取单据信息
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills == null || bills.size() == 0) {
            return new RuleExecuteResult();
        }
        BizObject bill = bills.get(0);
        CurrencyExchange currencyExchangeDB = MetaDaoHelper.findById(CurrencyExchange.ENTITY_NAME, bill.getId());
        String status = bill.get("_status") + "";
        //只过滤来源单据为对账单和认领单的数据
        String datasource = bill.getString("datasource");
        //16对账单，80认领单
        if (StringUtils.isEmpty(datasource) || (!"16".equals(datasource) && !"80".equals(datasource))) {
            return new RuleExecuteResult();
        }
        //买入关联对账单和买入关联认领单校验
        if (bill.get("paybankbill") == null && bill.get("paybillclaim") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100407"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F8BFC05C00035", "买入关联银行对账单和买入关联认领单不能同时为空。") /* "买入关联银行对账单和买入关联认领单不能同时为空。" */);
        }

        if (bill.get("paybankbill") != null && bill.get("paybillclaim") != null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100408"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F8BFC05C0002E", "买入关联银行对账单和买入关联认领单有且只能一个有值。") /* "买入关联银行对账单和买入关联认领单有且只能一个有值。" */);
        }

        //卖出关联对账单和卖出关联认领单校验
        if (bill.get("collectbankbill") == null && bill.get("collectbillclaim") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100409"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F8BFC05C00031", "卖出关联银行对账单和卖出关联认领单不能同时为空。") /* "卖出关联银行对账单和卖出关联认领单不能同时为空。" */);
        }

        if (bill.get("collectbankbill") != null && bill.get("collectbillclaim") != null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100410"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F8BFC05C00032", "卖出关联银行对账单和卖出关联认领单有且只能一个有值。") /* "卖出关联银行对账单和卖出关联认领单有且只能一个有值。" */);
        }

        List<CorrDataEntityParam> list = new ArrayList<>();
        CurrencyExchange currencyExchange = new CurrencyExchange();
        currencyExchange.init(bill);

        //买入关联银行对账单，封装关联数据
        if (currencyExchange.getPaybankbill() != null) {
            CorrDataEntityParam corrDataEntity = initCorrDataEntity(currencyExchange, status, billNum, currencyExchange.getBuysmartcheckno());
            //金额为买入金额
            corrDataEntity.setOriSum(currencyExchange.getPurchaseamount());
            corrDataEntity.setBankReconciliationId(currencyExchange.getPaybankbill());
            corrDataEntity.setMainid(currencyExchange.getId());
            if (!"Update".equals(status)) {
                //校验买入关联银行对账单是否为最新状态
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, currencyExchange.getPaybankbill());
                if (bankReconciliation == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100411"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F8BFC05C00034", "买入关联银行对账单单据不是最新状态，请刷新单据重试！") /* "买入关联银行对账单单据不是最新状态，请刷新单据重试！" */);
                }
            }
            list.add(corrDataEntity);
        }

        //卖出关联银行对账单，封装关联数据
        if (currencyExchange.getCollectbankbill() != null) {
            CorrDataEntityParam corrDataEntity = initCorrDataEntity(currencyExchange, status, billNum, currencyExchange.getSellsmartcheckno());
            //金额为卖出金额
            corrDataEntity.setOriSum(currencyExchange.getSellamount());
            corrDataEntity.setBankReconciliationId(currencyExchange.getCollectbankbill());
            corrDataEntity.setMainid(currencyExchange.getId());
            if (!"Update".equals(status)) {
                //校验买入关联银行对账单是否为最新状态
                BankReconciliation bankReconciliation = MetaDaoHelper.findById(BankReconciliation.ENTITY_NAME, currencyExchange.getCollectbankbill());
                if (bankReconciliation == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100412"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F8BFC05C0002F", "卖出关联银行对账单单据不是最新状态，请刷新单据重试！") /* "卖出关联银行对账单单据不是最新状态，请刷新单据重试！" */);
                }
            }
            list.add(corrDataEntity);
        }

        //买入关联认领单，封装关联数据
        if (currencyExchange.getPaybillclaim() != null) {
            CorrDataEntityParam corrDataEntity = initCorrDataEntity(currencyExchange, status, billNum, currencyExchange.getBuysmartcheckno());
            //金额为买入金额
            corrDataEntity.setOriSum(currencyExchange.getPurchaseamount());
            corrDataEntity.setBillClaimItemId(currencyExchange.getPaybillclaim());
            if (!"Update".equals(status)) {
                //校验买入关联认领单是否为最新状态
                BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, currencyExchange.getPaybillclaim());
                if (billClaim == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100413"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F8BFC05C00030", "买入关联认领单单据不是最新状态，请刷新单据重试！") /* "买入关联认领单单据不是最新状态，请刷新单据重试！" */);
                }
            }
            list.add(corrDataEntity);
        }

        //卖出关联认领单，封装关联数据
        if (currencyExchange.getCollectbillclaim() != null) {
            CorrDataEntityParam corrDataEntity = initCorrDataEntity(currencyExchange, status, billNum, currencyExchange.getSellsmartcheckno());
            //金额为买入金额
            corrDataEntity.setOriSum(currencyExchange.getSellamount());
            corrDataEntity.setBillClaimItemId(currencyExchange.getCollectbillclaim());
            if (!"Update".equals(status)) {
                //校验买入关联认领单是否为最新状态
                BillClaim billClaim = MetaDaoHelper.findById(BillClaim.ENTITY_NAME, currencyExchange.getCollectbillclaim());
                if (billClaim == null) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100414"), InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F8BFC05C00033", com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_189F8BFC05C00033", "卖出关联认领单单据不是最新状态，请刷新单据重试！") /* "卖出关联认领单单据不是最新状态，请刷新单据重试！" */) /* "卖出关联认领单单据不是最新状态，请刷新单据重试！" */);
                }
            }
            list.add(corrDataEntity);
        }
        // 只有在新增的情况下才可以回写关联关系
        if ("Insert".equals(status)) {
            ctmcmpReWriteBusRpcService.batchReWriteBankRecilicationForRpc(list);
        }else{
            // 如果是编辑保存，则需要校验关联的id是否相同，如果不相同则需要提示先删除再新增
            if(currencyExchange.getPaybillclaim() !=null && !currencyExchange.getPaybillclaim().equals(currencyExchangeDB.getPaybillclaim())){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2210D8B005600018", "卖出方向关联认领单发生变更，请删除单据后重新制单！") /* "卖出方向关联认领单发生变更，请删除单据后重新制单！" */);
            }
            if(currencyExchange.getPaybankbill() !=null && !currencyExchange.getPaybankbill().equals(currencyExchangeDB.getPaybankbill())){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2210D8B005600015", "卖出方向关联流水发生变更，请删除单据后重新制单！") /* "卖出方向关联流水发生变更，请删除单据后重新制单！" */);
            }
            if(currencyExchange.getCollectbillclaim() !=null && !currencyExchange.getCollectbillclaim().equals(currencyExchangeDB.getCollectbillclaim())){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2210D8B005600016", "买入方向关联认领单发生变更，请删除单据后重新制单！") /* "买入方向关联认领单发生变更，请删除单据后重新制单！" */);
            }
            if(currencyExchange.getCollectbankbill() !=null && !currencyExchange.getCollectbankbill().equals(currencyExchangeDB.getCollectbankbill())){
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_2210D8B005600017", "买入方向关联流水发生变更，请删除单据后重新制单！") /* "买入方向关联流水发生变更，请删除单据后重新制单！" */);
            }
        }
        return new RuleExecuteResult();
    }


    /**
     * 初始化关联回写实体数据
     *
     * @param currencyExchange
     * @param status
     * @param billNum
     * @return
     */
    private CorrDataEntityParam initCorrDataEntity(CurrencyExchange currencyExchange, String status, String billNum, String smartcheckno) {
        CorrDataEntityParam corrData = new CorrDataEntityParam();
        corrData.setVouchdate(currencyExchange.getVouchdate());
        corrData.setCode(currencyExchange.getCode());
        corrData.setStatus(status);
        corrData.setMainid(currencyExchange.getId());
        corrData.setGenerate(true);
        corrData.setAccentity(currencyExchange.getAccentity());
        corrData.setAuto(false);
        corrData.setBillNum(billNum);
        corrData.setDept(currencyExchange.getDept());
        corrData.setProject(currencyExchange.getProject());
        corrData.setBusid(currencyExchange.getId());
        corrData.setSmartcheckno(smartcheckno);
        corrData.setBillType(EventType.CurrencyExchangeBill.getValue() + "");
        return corrData;
    }
}
