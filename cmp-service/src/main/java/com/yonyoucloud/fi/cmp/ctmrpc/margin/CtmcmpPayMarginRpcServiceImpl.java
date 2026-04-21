package com.yonyoucloud.fi.cmp.ctmrpc.margin;


import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.org.dto.FinOrgDTO;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.ExchangeRateTypeVO;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.ucf.transtype.service.itf.ITransTypeService;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.util.JSONBuilderUtils;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.cmpentity.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.pushAndPull.PushAndPullService;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.margincommon.service.MarginCommonService;
import com.yonyoucloud.fi.cmp.margintype.MarginType;
import com.yonyoucloud.fi.cmp.marginworkbench.MarginWorkbench;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.margin.CtmcmpPayMarginRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonResponseDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.margin.PayMarginVO;
import com.yonyoucloud.fi.cmp.paymargin.PayMargin;
import com.yonyoucloud.fi.cmp.pushAndPull.PushAndPullModel;
import com.yonyoucloud.fi.cmp.util.AccentityUtil;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.biz.base.Objectlizer;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

@Service
public class CtmcmpPayMarginRpcServiceImpl implements CtmcmpPayMarginRpcService {
    private static final String SYSTEMCODE = "system_0001";

    /**
     * 单据转换code，详见单据转换表配置
     */
    private static final String AGREEMENTTOCMPPAYMARGIN = "agreementToCmpPaymargin";

    private static final String CREDITCHANGETOCMPPAYMARGIN = "creditchangeToCmpPaymargin";
    private static final String FINANCINGREGISTERTOCMPPAYMARGIN = "financingregisterToCmpPaymargin";
    private static final String FINANCEINTOCMPPAYMARGIN = "financeinToCmpPaymargin";
    private static final String REPAYMENTTOCMPPAYMARGIN = "repaymentToCmpPaymargin";
    private static final String FINANCINGROLLOVERTOCMPPAYMARGIN = "financingrolloverToCmpPaymargin";

    private static final String GUARANTEEREGISTERTOCMPPAYMARGIN = "guaranteeregisterToCmpPaymargin";
    private static final String GUARANTEECHANGETOCMPPAYMARGIN = "guaranteechangeToCmpPaymargin";

    //出票保证金
    private static final String PAYSECURITYTOPAYMARGIN = "paysecurityToPaymargin";
    //票据签发
    private static final String SIGNNOTETOPAYMARGIN = "signnoteToPaymargin";
    //应付票据初始化
    private static final String SIGNNOTEINITTOPAYMARGIN = "signnoteinitToPaymargin";

    //开征办理推保证金
    private static final String ISSUEREGISTERPUSHMARGINLEDGER = "issueregisterPushMarginLedger";

    //开证修改推保证金
    private static final String CHANGECERTIFICATEPUSHMARGINLEDGER = "changeCertificatePushMarginLedger";

    //开函申请推保证金
    private static final String LGM_GUARANTEEAPPLYTOPAYMARGINVO = "lgm_GuaranteeApplyTOPayMarginVO";


//    private static final String AGREEMENTTOCMPPAYMARGIN = "agreementToCmpPaymargin";
//    private static final String AGREEMENTTOCMPPAYMARGIN = "agreementToCmpPaymargin";
//    private static final String AGREEMENTTOCMPPAYMARGIN = "agreementToCmpPaymargin";

    @Autowired
    ITransTypeService iTransTypeService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    private IFIBillService fiBillService;

    @Autowired
    private PushAndPullService pushAndPullService;

    @Autowired
    MarginCommonService marginCommonService;

    //@Autowired
//    OrgRpcService orgRpcService;

    /**
     * 支付保证金存入接口实现*
     *
     * @param payMarginVO
     * @return
     * @throws Exception
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public PayMarginVO payMarginSave(PayMarginVO payMarginVO) throws Exception {
        PayMargin payMargin = new PayMargin();
        Short initflagvo = 0;
        if (ObjectUtils.isNotEmpty(payMarginVO.getInitflag())){
            if (payMarginVO.getInitflag()) {
                initflagvo = 1;
            }
        }
        Short settleflagvo = 0;
        if (ObjectUtils.isNotEmpty(payMarginVO.getSettleflag())){
            if (payMarginVO.getSettleflag()) {
                settleflagvo = 1;
            }
        }
        Short conversionmarginflagvo = 0;
        if (ObjectUtils.isNotEmpty(payMarginVO.getConversionmarginflag())){
            if (payMarginVO.getConversionmarginflag()) {
                conversionmarginflagvo = 1;
            }
        }
        Short samenametransferflagvo = 0;
        if (ObjectUtils.isNotEmpty(payMarginVO.getSamenametransferflag())){
            if (payMarginVO.getSamenametransferflag()) {
                samenametransferflagvo = 1;
            }
        }

        payMarginVO.setInitflag(null);
        payMarginVO.setSettleflag(null);
        payMarginVO.setConversionmarginflag(null);
        payMarginVO.setSamenametransferflag(null);
        payMargin =  Objectlizer.convert(JSONBuilderUtils.beanToMap(payMarginVO),PayMargin.ENTITY_NAME);

        payMargin.setSettleflag(settleflagvo);
        payMargin.setConversionmarginflag(conversionmarginflagvo);
        //资金支付保证金单据新增接口
        payMargin.setOurassociationstatus(payMarginVO.getOurassociationstatus());
        payMargin.setOurcheckno(payMarginVO.getOurcheckno());
        payMargin.setOurbankbillid(payMarginVO.getOurbankbillid());
        payMargin.setOurbillclaimid(payMarginVO.getOurbillclaimid());
        payMargin.setOppassociationstatus(payMarginVO.getOppassociationstatus());
        payMargin.setOppcheckno(payMarginVO.getOppcheckno());
        payMargin.setOppbankbillid(payMarginVO.getOppbankbillid());
        payMargin.setOppbillclaimid(payMarginVO.getOppbillclaimid());
        

        String accentity = payMargin.getAccentity();
        //校验会计主体期初
        CmpCommonUtil.checkPeriodFirstDate(accentity);
        Date vouchdate = payMargin.getVouchdate();
        String marginbusinessno = payMargin.getMarginbusinessno();
        Short tradetypeflag = payMargin.getTradetypeflag();
        String srcbillid = payMargin.getSrcbillid();
        baseCheck(payMargin);
        //根据单据类型的租户id和formId,查询单据类型的下的交易类型
        String tradetypeId = querytranstype(tradetypeflag);
        payMargin.setTradetype(tradetypeId);
        String currency = payMargin.getCurrency();
        //根据会计主体获取本币币种
        CurrencyTenantDTO natCurrencyTenantDTO = queryNatCurrency(accentity);
        Integer natMoneyDigit = null;
        if (natCurrencyTenantDTO != null) {
            payMargin.setNatCurrency(natCurrencyTenantDTO.getId());
            natMoneyDigit = natCurrencyTenantDTO.getMoneydigit();
        }
        //设置汇率类型和汇率
        CtmJSONObject ctmJSONObject = getExchangeRate(payMargin.getExchRate(), payMargin.getExchangeratetype(), currency, natCurrencyTenantDTO.getId(), vouchdate);
        if (ObjectUtils.isNotEmpty(ctmJSONObject)){
            payMargin.setExchangeratetype(ctmJSONObject.get("exchangeratetypeId").toString());
            payMargin.setExchRate((BigDecimal)ctmJSONObject.get("exchangerate"));
            payMargin.setExchRateOps((Short) ctmJSONObject.get("exchRateOp"));
        } else if (ObjectUtils.isEmpty(payMargin.getExchRateOps())) {
            // 当传入汇率时，但是汇率折算方式没有传入时，默认赋值乘法
            payMargin.setExchRateOps(EXCHANGE_RATE_OPS_MULTIPLY);
        }
        //保证金金额：交易类型为支付保证金时必填，交易类型为取回保证金时，保证金金额和转换金额不可以同时为空
        BigDecimal marginamount = payMargin.getMarginamount();
        BigDecimal conversionamount = payMargin.getConversionamount();
        if (tradetypeflag == MarginTradeTypeFlag.MarginPayment.getValue()) {
            if (ObjectUtils.isEmpty(marginamount)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100804"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F12C7405080004", "交易类型为支付保证金时,保证金金额不可为空！") /* "交易类型为支付保证金时,保证金金额不可为空！" */);
            }
        } else { //交易类型为取回保证金时，保证金金额和转换金额不可以同时为空
            if (ObjectUtils.isEmpty(marginamount) && ObjectUtils.isEmpty(conversionamount)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100805"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F12F3604F80007", "交易类型为取回保证金时，保证金金额和转换金额不可同时为空！") /* "交易类型为取回保证金时，保证金金额和转换金额不可同时为空！" */);
            }
        }

        if (ObjectUtils.isNotEmpty(marginamount)) {
            payMargin.setNatmarginamount(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(payMargin.getExchRateOps(), payMargin.getExchRate(), marginamount, natMoneyDigit));
        } else {
            payMargin.setMarginamount(BigDecimal.ZERO);
            payMargin.setNatmarginamount(BigDecimal.ZERO);
        }
        //对方类型 oppositetype
        Short oppositetype = payMargin.getOppositetype();
        if (ObjectUtils.isEmpty(oppositetype)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100806"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803F9", "对方类型不可为空！") /* "对方类型不可为空！" */);
        }
        //是否结算  保证金金额大于0时，必填
        Short settleflag = payMargin.getSettleflag();
        if (marginamount.compareTo(BigDecimal.ZERO) > 0 && ObjectUtils.isEmpty(settleflag)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100807"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F13F3C04F80007", "保证金金额大于0时,是否结算标识不可为空！") /* "是否结算标识不可为空！" */);
        }
        //当是否结算为是时，必传 枚举值为1-付款/0-收款
        Short paymenttype = payMargin.getPaymenttype();
        if (settleflag == 1) {
            //付款结算模式
            Short paymentsettlemode = payMargin.getPaymentsettlemode();
            if (ObjectUtils.isEmpty(paymentsettlemode)) {
                //如果付款结算模式为空，则赋默认值为 主动结算
                payMargin.setPaymentsettlemode(PaymentSettlemode.ActiveSettlement.getValue());
            }
//            if (ObjectUtils.isEmpty(paymenttype)) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100808"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180402","需结算时，收付类型不可为空！") /* "需结算时，收付类型不可为空！" */);
//            }
            Long settlemode = payMargin.getSettlemode();
            if (ObjectUtils.isEmpty(settlemode)) {
                //获取结算方式
                Long settlemodeId = querysettlemode();
                payMargin.setSettlemode(settlemodeId);
            }
            //结算状态
            Short settlestatus = payMargin.getSettlestatus();
            if (ObjectUtils.isEmpty(settlestatus)) {
                //不上送，是否结算为是时，系统默认赋值待结算
                payMargin.setSettlestatus(FundSettleStatus.WaitSettle.getValue());
            }
            if (payMargin.getSettlestatus().equals(FundSettleStatus.SettleSuccess.getValue())) {
                //上游传的结算状态如果是结算成功 咱们这边给赋值已结算补单
                payMargin.setSettlestatus(FundSettleStatus.SettlementSupplement.getValue());
            }
            //本方银行账户 enterprisebankaccount
            String enterprisebankaccount = payMargin.getEnterprisebankaccount();
            if (ObjectUtils.isEmpty(enterprisebankaccount)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100809"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803EE", "需结算时，本方银行账户不可为空！") /* "需结算时，本方银行账户不可为空！" */);
            }
            String oppositeid = payMarginVO.getOppositeid();
            String oppositeaccount = payMarginVO.getOppositeaccount();
            // 对方档案id 和 对方银行信息id
            checkOpposite(oppositeid,oppositeaccount,payMargin.getSettlestatus(),payMargin.getPaymentsettlemode(),payMargin.getOppositetype());
            //根据对方类型进行 对方档案id的映射
            switch (oppositetype) {
                case 1: //客户
                    payMargin.setCustomer(ValueUtils.isNotEmptyObj(oppositeid) ? Long.valueOf(oppositeid) : null);
                    payMargin.setCustomerbankaccount(ValueUtils.isNotEmptyObj(oppositeaccount) ? Long.valueOf(oppositeaccount) : null);
                    break;
                case 2: //供应商
                    payMargin.setSupplier(ValueUtils.isNotEmptyObj(oppositeid) ? Long.valueOf(oppositeid) : null);
                    payMargin.setSupplierbankaccount(ValueUtils.isNotEmptyObj(oppositeaccount) ? Long.valueOf(oppositeaccount) : null);
                    break;
                case 3: //其他
                    break;
                case 4: //内部单位
                    payMargin.setOurname(oppositeid);
                    payMargin.setOurbankaccount(oppositeaccount);
                    break;
                case 5: //资金业务对象
                    payMargin.setCapBizObj(oppositeid);
                    payMargin.setCapBizObjbankaccount(oppositeaccount);
                    break;
                default:
                    break;
            }
            //对方名称 对方银行账户名称 对方银行账号 对方开户行档案id
            String oppositename = payMargin.getOppositename();
            String oppositebankaccountname = payMargin.getOppositebankaccountname();
            String oppositebankaccount = payMargin.getOppositebankaccount();
            String oppositebankNumber = payMargin.getOppositebankNumber();
            checkOppositeaccount(oppositename,oppositebankaccountname,oppositebankaccount,oppositebankNumber,payMargin.getSettlestatus(),payMargin.getPaymentsettlemode(),payMargin.getOppositetype());
        } else {
            payMargin.setSettlemode(null);
            payMargin.setPaymentsettlemode(PaymentSettlemode.ActiveSettlement.getValue());

            if (ObjectUtils.isNotEmpty(payMargin.getSettlestatus())) {
                if (payMargin.getSettlestatus().equals(FundSettleStatus.SettleSuccess.getValue())) {
                    //上游传的结算状态如果是结算成功 咱们这边给赋值已结算补单
                    payMargin.setSettlestatus(FundSettleStatus.SettlementSupplement.getValue());
                }
            } else {
                //不传结算时：如果上游没有传结算状态 赋值为待结算
                payMargin.setSettlestatus(FundSettleStatus.WaitSettle.getValue());

            }
        }
        if (ObjectUtils.isEmpty(paymenttype)) {
            //收付类型 交易类型为取回保证金，对方类型不为本单位时，等于收款，否则等于付款
//                if(!oppositetype.equals(MarginOppositeType.OwnOrg.getValue()) && tradetypeflag.equals(1)) {
//                    payMargin.setPaymenttype(PaymentType.FundCollection.getValue());
//                } else {
//                    payMargin.setPaymenttype(PaymentType.FundPayment.getValue());
//                }

            //2024.10.29  支付保证金 收付类型赋值有优化
            //当交易类型为取回保证金，对方类型为：内部单位时，内部单位是本单位，且对方账户有值的赋值为：付款
            //对方类型为：内部单位时，内部单位是本单位，且对方账户没有值的赋值为：收款  内部单位不是本单位时，赋值为：收款
            if (tradetypeflag == MarginTradeTypeFlag.MarginWithdraw.getValue()) {
                if (ObjectUtils.isNotEmpty(payMargin.getOppositetype()) && payMargin.getOppositetype() == MarginOppositeType.OwnOrg.getValue()) {
//                        String accentity = payMargin.getAccentity();
                    String ourbankaccountname = payMargin.getOurname();
                    String ourbankaccount = payMargin.getOurbankaccount();
                    if (ObjectUtils.isNotEmpty(ourbankaccountname) && ourbankaccountname.equals(accentity)) {
                        if (ObjectUtils.isNotEmpty(ourbankaccount)) {
                            payMargin.setPaymenttype(PaymentType.FundPayment.getValue());
                        } else {
                            payMargin.setPaymenttype(PaymentType.FundCollection.getValue());
                        }
                    } else {
                        payMargin.setPaymenttype(PaymentType.FundCollection.getValue());
                    }
                } else {
                    payMargin.setPaymenttype(PaymentType.FundCollection.getValue());
                }
            } else {
                payMargin.setPaymenttype(PaymentType.FundPayment.getValue());
            }
        }
        //上游单据会把所有的对方信息都传递  对方单位如果不是其他，此时把 对方名称 对方银行账户名称 对方银行账号 对方开户行档案id 置空
        if(!payMargin.getOppositetype().equals(MarginOppositeType.Other.getValue())){
            payMargin.setOppositename(null);
            payMargin.setOppositebankaccountname(null);
            payMargin.setOppositebankaccount(null);
            payMargin.setOppositebankNumber(null);
        }
        //交易类型为取回保证金时，可以上送，否则，上送系统也不入库，默认赋值否
        checkConversion(tradetypeflag, conversionamount, natMoneyDigit, payMargin);
        //关联信息
        checkassociation(payMargin);

        //校验保证金虚拟户可用余额
        checkamount(tradetypeflag,marginamount,conversionamount,marginbusinessno,payMargin);

        String billnum = payMarginVO.getBillnum();
        BizObject bizObject = transBill(srcbillid,billnum);
        if (ObjectUtils.isNotEmpty(bizObject)) {
            for (Map.Entry<String,Object> entry : bizObject.entrySet()) {
                payMargin.put(entry.getKey(),entry.getValue());
            }
            if (ObjectUtils.isNotEmpty(payMargin.get("margintype"))) {
                Long margintype = payMargin.get("margintype") instanceof Long ? payMargin.get("margintype") : Long.valueOf(payMargin.get("margintype"));
                payMargin.setMargintype(margintype);
                checkMarginType(payMargin.getMargintype().toString());
            }
            if (ObjectUtils.isNotEmpty(payMargin.get("newmargintype"))) {
                Long newmargintype = payMargin.get("newmargintype") instanceof Long ? payMargin.get("newmargintype") : Long.valueOf(payMargin.get("newmargintype"));
                payMargin.setNewmargintype(newmargintype);
                checkMarginType(payMargin.getNewmargintype().toString());
            }

        }

        //设置期初为：否
        payMargin.setInitflag((short) 0);
        //设置推送为第一次推送
        payMargin.setPushtimes(ICmpConstant.FIRST);
        //设置同名账户划转为false
        payMargin.setSamenametransferflag((short) 0);
        BillDataDto billDataDto = new BillDataDto(IBillNumConstant.CMP_PAYMARGIN);
        payMargin.setEntityStatus(EntityStatus.Insert);
        billDataDto.setData(payMargin);
        //根据原始业务号查询支付保证金有几条  marginbusinessno
        Boolean deleteflag = marginCommonService.findPayMargin(marginbusinessno);
        YtsContext.setYtsContext("isdeleteMVA", deleteflag);
        fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), billDataDto);
        YtsContext.setYtsContext("billid", ((List<PayMargin>) billDataDto.getData()).get(0).getId());
        if (ObjectUtils.isNotEmpty(payMargin.getId())){
            payMarginVO.setId(payMargin.getId().toString());
        }
        return payMarginVO;
    }

    /**
     * 支付保证金存入接口回滚*
     *
     * @param payMarginVO
     * @return
     * @throws Exception
     */
    @Override
    public PayMarginVO payMarginSaveRollback(PayMarginVO payMarginVO) throws Exception {
        BillDataDto billDataDto = new BillDataDto(IBillNumConstant.CMP_PAYMARGIN);
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.PRIMARY_ID);
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name(ICmpConstant.PRIMARY_ID).eq(YtsContext.getYtsContext("billid")));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<PayMargin> payMarginList = MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema, null);
        if (payMarginList != null && payMarginList.size() > 0) {
            PayMargin payMargin = payMarginList.get(0);
            payMargin.setEntityStatus(EntityStatus.Delete);
            payMargin.set("isRPC",true);
            if(YtsContext.getYtsContext("isdeleteMVA").equals(true)){
                payMargin.set("isRPCDELETE",true);
            } else {
                payMargin.set("isRPCDELETE",false);
            }
            billDataDto.setData(payMargin);
            fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100810"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00055", "查询失败") /* "查询失败" */);
        }
        return payMarginVO;
    }

    /**
     * 根据保证金业务号查询保证金台账信息
     * @param params
     * @return
     * @throws Exception
     */
    @Override
    public CommonResponseDataVo balanceQueryPay(CommonRequestDataVo params) throws Exception {
        CommonResponseDataVo result = new CommonResponseDataVo();
        //获取保证金原始业务号
        String marginbusinessno = params.getMarginbusinessno();
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name(ICmpConstant.MARGINBUSINESSNO).eq(marginbusinessno));
        queryConditionGroup.addCondition(QueryCondition.name("marginFlag").eq(MarginFlag.PayMargin.getValue()));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<MarginWorkbench> marginWorkbenchList = MetaDaoHelper.queryObject(MarginWorkbench.ENTITY_NAME, querySchema, null);
        if (marginWorkbenchList != null && marginWorkbenchList.size() > 0) {
            MarginWorkbench marginWorkbench = marginWorkbenchList.get(0);
            //支付金额
            BigDecimal payAmount = marginWorkbench.getPayAmount();
            //取回金额
            BigDecimal retrieveAmount = marginWorkbench.getRetrieveAmount();
            //转换金额
            BigDecimal conversionAmount = marginWorkbench.getConversionAmount();
            //保证金余额
            BigDecimal marginBalance = marginWorkbench.getMarginBalance();
            //保证金可用余额
            BigDecimal marginAvailableBalance = marginWorkbench.getMarginAvailableBalance();
            result.setPayAmount(payAmount);
            result.setRetrieveAmount(retrieveAmount);
            result.setConversionAmount(conversionAmount);
            result.setMarginbalance(marginBalance);
            result.setMarginAvailableBalance(marginAvailableBalance);
            return  result;
        } else {
            return null;
        }
//        else {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100811"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803F2", "当前保证金原始业务号没有期初数据！") /* "当前保证金原始业务号没有期初数据！" */);
//        }

    }

    /**
     * 支付保证金删除*
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public String deletePayMarginByIds(CommonRequestDataVo param) throws Exception {
        List<PayMargin> payMarginList = new ArrayList<>();
        try {
            List<String> srcbillid = param.getSrcbillid();
            if (srcbillid == null || srcbillid.size() == 0) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100812"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803F7", "来源业务单据id为空！") /* "来源业务单据id为空！" */);
            }
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup conditionGroup = new QueryConditionGroup(ConditionOperator.and);
            conditionGroup.appendCondition(QueryCondition.name(SRCBILLID).in(srcbillid));
            if (ObjectUtils.isNotEmpty(param.getMarginbusinessno())) {
                conditionGroup.addCondition(QueryCondition.name(MARGINBUSINESSNO_X).eq(param.getMarginbusinessno()));
            }
            if (ObjectUtils.isNotEmpty(param.getTradetypeflag())) {
                Short tradetypeflag = Short.parseShort(param.getTradetypeflag());
                conditionGroup.appendCondition(QueryCondition.name(TRADETYPEFLAG).in(tradetypeflag));
            }
            querySchema.addCondition(conditionGroup);
            payMarginList = MetaDaoHelper.queryObject(PayMargin.ENTITY_NAME, querySchema, null);
            BillDataDto billDataDto = new BillDataDto(IBillNumConstant.CMP_PAYMARGIN);
            List<MarginWorkbench> marginWorkbenchList_rollback = new ArrayList<>();
            if (payMarginList != null && payMarginList.size() > 0) {
                for (PayMargin payMargin : payMarginList) {
                    if (!(ObjectUtils.isEmpty(payMargin.getVerifystate())) && (payMargin.getVerifystate() == VerifyState.SUBMITED.getValue() || payMargin.getVerifystate() == VerifyState.COMPLETED.getValue())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100813"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18FB28A205080002", "当前业务单据关联的支付保证金已进行业务处理，无法删除！") /* "当前业务单据关联的支付保证金已进行业务处理，无法删除！" */);
                    }
                    payMargin.set("isRPC",true);
                    billDataDto.setData(payMargin);
                    //如果虚拟户对应的数据只有一条 则回滚的时候需要插入一条
                    Boolean rollbackflag = marginCommonService.useByMulPayMargin(payMargin.getMarginvirtualaccount().toString());
                    if (rollbackflag) {
                        String marginBusinessNo  = payMargin.getMarginbusinessno();
                        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                        QueryConditionGroup conditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
                        conditionGroup1.appendCondition(QueryCondition.name("marginBusinessNo").in(marginBusinessNo));
                        conditionGroup1.addCondition(QueryCondition.name("marginFlag").eq(MarginFlag.PayMargin.getValue()));
                        querySchema1.addCondition(conditionGroup1);
                        List<MarginWorkbench> marginWorkbenchList = MetaDaoHelper.queryObject(MarginWorkbench.ENTITY_NAME, querySchema1, null);
                        if (marginWorkbenchList.size() > 0 && ObjectUtils.isNotEmpty(marginWorkbenchList)){
                            marginWorkbenchList_rollback.add(marginWorkbenchList.get(0));
                        }
                    }
                    fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto);
                }
                YtsContext.setYtsContext("payMarginList_fail", payMarginList);
                YtsContext.setYtsContext("marginWorkbenchList_fail", marginWorkbenchList_rollback);
            } else {
                return ResultMessage.success();
            }

        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100814"),e.getMessage());
        }
        return ResultMessage.success();
    }

    /**
     * 支付保证金删除回滚*
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public String deletePayMarginRollbackNew(CommonRequestDataVo param) throws Exception {

        List<PayMargin> payMarginList = (List<PayMargin>) YtsContext.getYtsContext("payMarginList_fail");
        if (payMarginList != null && payMarginList.size() > 0) {
            for (PayMargin payMargin : payMarginList) {
                payMargin.setEntityStatus(EntityStatus.Insert);
                CmpMetaDaoHelper.insert(PayMargin.ENTITY_NAME, payMargin);
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100815"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F8EFE205080000", "支付保证金删除接口，回滚失败！") /* "支付保证金删除接口，回滚失败！" */);
        }

        List<MarginWorkbench> marginWorkbenchList = (List<MarginWorkbench>) YtsContext.getYtsContext("marginWorkbenchList_fail");
        if (marginWorkbenchList != null && marginWorkbenchList.size() > 0) {
            for (MarginWorkbench marginWorkbench : marginWorkbenchList){
                marginWorkbench.setEntityStatus(EntityStatus.Insert);
                CmpMetaDaoHelper.insert(MarginWorkbench.ENTITY_NAME, marginWorkbench);
            }
        }

        return ResultMessage.success();
    }


    /**
     *  校验保证金类型是否存在 以及是否时启用状态
     * @param marginTypeId
     * @throws Exception
     */
    private static void checkMarginType(String marginTypeId) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name(ID).eq(marginTypeId));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<MarginType> marginTypeList = MetaDaoHelper.queryObject(MarginType.ENTITY_NAME, querySchema, null);
        if (marginTypeList.size() > 0) {
            MarginType marginType = marginTypeList.get(0);
            if (!marginType.getIsEnabledType()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100816"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19162EDA05200001", "单据转换而来的保证金类型为非启用状态！") /* "单据转换而来的保证金类型为非启用状态！" */);
            }
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100817"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_19162FE205500003", "未查询到通过单据转换而来的保证金类型！") /* "未查询到通过单据转换而来的保证金类型！" */);
        }

    }

    /**
     * 调用单据转换规则方法
     * @param srcbillid
     * @param billnum
     * @return
     */
    private BizObject transBill (String srcbillid, String billnum) {
        BizObject bizObject = null;
        try {
            //调用单据转换规则
            List<BizObject> bills = new ArrayList<>();
            PushAndPullModel pushAndPullModel = new PushAndPullModel();
            List<String> mainids = new ArrayList<>();
            mainids.add(srcbillid);
//            pushAndPullModel.setMainids(mainids);
            pushAndPullModel.setChilds(mainids);
            pushAndPullModel.setIsMainSelect(1);
            //根据billnum进行映射不同的code
            String pushCode = "";
            if (ObjectUtils.isNotEmpty(billnum)) {
                switch (billnum) {
//                case "cam_agreement": //授信合同
//                    pushCode = AGREEMENTTOCMPPAYMARGIN;
//                    break;
//                case "cam_creditchange": //授信变更
//                    pushCode = CREDITCHANGETOCMPPAYMARGIN;
//                    break;
                    case "tlm_financingregister": //融资登记
                        pushCode = FINANCINGREGISTERTOCMPPAYMARGIN;
                        break;
                    case "tlm_financein": //融入登记
                        pushCode = FINANCEINTOCMPPAYMARGIN;
                        break;
                    case "tlm_repayment": //融资还本
                        pushCode = REPAYMENTTOCMPPAYMARGIN;
                        break;
                    case "tlm_financingrollover": //融资展期
                        pushCode = FINANCINGROLLOVERTOCMPPAYMARGIN;
                        break;
                    case "lgm_guaranteeregister": //开函登记
                        pushCode = GUARANTEEREGISTERTOCMPPAYMARGIN;
                        break;
                    case "lgm_guaranteechange": //开函变更
                        pushCode = GUARANTEECHANGETOCMPPAYMARGIN;
                        break;
                    case "drft_paysecurity": //出票保证金
                        pushCode = PAYSECURITYTOPAYMARGIN;
                        break;
                    case "drft_signnote": //票据签发
                        pushCode = SIGNNOTETOPAYMARGIN;
                        break;
                    case "drft_signnoteinit": //应付票据初始化
                        pushCode = SIGNNOTEINITTOPAYMARGIN;
                        break;
                    case "lcm_issueregister": //开征办理
                        pushCode = ISSUEREGISTERPUSHMARGINLEDGER;
                        break;
                    case "lcm_changecertificate": //开证修改
                        pushCode = CHANGECERTIFICATEPUSHMARGINLEDGER;
                        break;
                    case "lgm_guaranteeapply": //开函申请
                        pushCode = LGM_GUARANTEEAPPLYTOPAYMARGINVO;
                    default:
                        break;

                }
            }

            if (ObjectUtils.isEmpty(pushCode)) {
                return bizObject;
            } else {
                pushAndPullModel.setCode(pushCode);
                bizObject = pushAndPullService.transformBillByMakeBillCode(bills, pushAndPullModel);
            }

        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100818"),e.getMessage());
        }
        return bizObject;
    }

    private static void baseCheck(PayMargin payMargin) {
        String accentity = payMargin.getAccentity();
        if (StringUtils.isEmpty(accentity)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100819"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803FC", "会计主体不可为空!") /* "会计主体不可为空!" */);
        }
        Date vouchdate = payMargin.getVouchdate();
        if (ObjectUtils.isEmpty(vouchdate)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100820"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00058", "单据业务日期不可为空！") /* "单据业务日期不可为空！" */);
        }
        String marginbusinessno = payMargin.getMarginbusinessno();
        if (StringUtils.isEmpty(marginbusinessno)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100821"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803FE", "保证金原始业务号不可为空!") /* "保证金原始业务号不可为空!" */);
        }

        Short tradetypeflag = payMargin.getTradetypeflag();
        if (ObjectUtils.isEmpty(tradetypeflag)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100822"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180401", "交易类型标识不可为空！") /* "交易类型标识不可为空！" */);
        }

        String currency = payMargin.getCurrency();
        if (StringUtils.isEmpty(currency)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100823"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00057", "币种不可为空!") /* "币种不可为空!" */);
        }
        //事项来源 srcitem
        Short srcitem = payMargin.getSrcitem();
        if (ObjectUtils.isEmpty(srcitem)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100824"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803ED", "事项来源不可为空！") /* "事项来源不可为空！" */);
        }
        //单据类型 billtype
        String billtype = payMargin.getBilltype();
        if (ObjectUtils.isEmpty(billtype)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100825"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F11D8604F80006", "单据类型不可为空！") /* "单据类型不可为空！" */);
        }
        //来源业务单据编号
        String srcbillno = payMargin.getSrcbillno();
        if (ObjectUtils.isEmpty(srcbillno)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100826"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803F1", "来源业务单据编号不可为空！") /* "来源业务单据编号不可为空！" */);
        }
        //来源业务单据id
        String srcbillid = payMargin.getSrcbillid();
        if (ObjectUtils.isEmpty(srcbillid)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100827"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803F4", "来源业务单据id不可为空！") /* "来源业务单据id不可为空！" */);
        }
    }


    private String querytranstype (Short tradetypeflag) throws Exception {
        List<BdTransType> bdTransTypes = iTransTypeService.getTransTypesByFormId(InvocationInfoProxy.getTenantid().toString(), ICmpConstant.CM_CMP_PAYMARGIN);
        String tradetypeId = "";
        //根据交易类型标识 0-支付保证金 1-取回保证金，获取交易类型的id
        if (tradetypeflag == MarginTradeTypeFlag.MarginPayment.getValue()) {
            //循环遍历bdTransTypes，取 已启用并且扩展字段中{"depositType_ext":"cmp_paymargin_payment"}depositType_ext为cmp_paymargin_payment的 第一条数据的id
            if (bdTransTypes.size() > 0) {
                //CtmJSONObject jsonObject = JSON.parseObject(bdTransType.getExtendAttrsJson()).get("depositType_ext")
                for (BdTransType bdTransType : bdTransTypes) {
                    if (ObjectUtils.isNotEmpty(bdTransType.getExtendAttrsJson())) {
                        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
                        if (bdTransType.getEnable() == 1 && (bdTransType.getCode().equals("cmp_paymargin_payment") || (ObjectUtils.isNotEmpty(jsonObject) && jsonObject.get("paymargin_ext").equals("cmp_paymargin_payment")))) {
                            //赋值tradetype
                            tradetypeId = bdTransType.getId();
                            break;
                        }
                    }

                }
            }
        } else if (tradetypeflag == MarginTradeTypeFlag.MarginWithdraw.getValue()) {
            //循环遍历bdTransTypes，取 已启用并且扩展字段中{"paymargin_ext":"cmp_paymargin_withdraw"}depositType_ext为cmp_paymargin_withdraw的 第一条数据的id
            for (BdTransType bdTransType : bdTransTypes) {
                if (ObjectUtils.isNotEmpty(bdTransType.getExtendAttrsJson())) {
                    CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
                    if (bdTransType.getEnable() == 1 && (bdTransType.getCode().equals("cmp_paymargin_withdraw") || (ObjectUtils.isNotEmpty(jsonObject) && jsonObject.get("paymargin_ext").equals("cmp_paymargin_withdraw")))) {
                        //赋值tradetype
                        tradetypeId = bdTransType.getId();
                        break;
                    }
                }
            }

        }
        return tradetypeId;
    }


    private CurrencyTenantDTO queryNatCurrency(String accentity) throws Exception{
        FinOrgDTO stringObjectMap = AccentityUtil.getFinOrgDTOByAccentityId(accentity);
//        List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accentity);
        CurrencyTenantDTO natCurrencyTenantDTO = new CurrencyTenantDTO();
        if (stringObjectMap != null) {
            natCurrencyTenantDTO = baseRefRpcService.queryCurrencyById(stringObjectMap.getCurrency());
        }
        return natCurrencyTenantDTO;
    }

    private CtmJSONObject getExchangeRate(BigDecimal oriExchRate, String exchangeratetypeId, String currency, String natCurrency, Date vouchdate) throws Exception{
        CtmJSONObject ctmJSONObject = new CtmJSONObject();
        if (ObjectUtils.isNotEmpty(oriExchRate)) {
            return ctmJSONObject;
        }
        if (ObjectUtils.isEmpty(exchangeratetypeId)) {
            // 汇率类型非必填，汇率默认设置为基准汇率
            ExchangeRateTypeVO exchangeRateTypeVO = baseRefRpcService.queryExchangeRateRateTypeByCode("01");
            exchangeratetypeId = exchangeRateTypeVO.getId();
        }
        ctmJSONObject.put("exchangeratetypeId", exchangeratetypeId);
        BigDecimal exchangerate = BigDecimal.ONE;
        Short exchRateOp = 1;
        if (!natCurrency.equals(currency)) {
            CmpExchangeRateVO exchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(currency, natCurrency, vouchdate, exchangeratetypeId);
            exchangerate = exchangeRateVO.getExchangeRate();
            exchRateOp = exchangeRateVO.getExchangeRateOps();
        }
        ctmJSONObject.put("exchangerate",exchangerate);
        ctmJSONObject.put("exchRateOp", exchRateOp);
        return ctmJSONObject;
    }


    private Long querysettlemode(){
        Long settlemodeId = null;
        //如果是否结算为是，默认等于预制的结算方式银行转账
        //取结算方式为银行转账的档案id 进行赋值
        SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
        settleMethodQueryParam.setCode(SYSTEMCODE);
        settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
        settleMethodQueryParam.setTenantId(AppContext.getTenantId());
        settleMethodQueryParam.setServiceAttr(CONSTANT_ZERO);
        List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
        if (dataList.size() > 0) {
            settlemodeId = dataList.get(0).getId();
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100828"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F6008204F80003", "获取结算方式失败！") /* "获取结算方式失败！" */);
        }
        return settlemodeId;
    }

    private static void checkOpposite(String oppositeid, String oppositeaccount, Short Settlestatus, Short Paymentsettlemode, Short Oppositetype) {
        if (!(Settlestatus.equals(FundSettleStatus.SettleSuccess.getValue()) || Settlestatus.equals(FundSettleStatus.SettlementSupplement.getValue()) || Paymentsettlemode.equals(PaymentSettlemode.CounterpartyDeduction.getValue())
                || Oppositetype.equals(MarginOppositeType.Other.getValue()))) {
            if (ObjectUtils.isEmpty(oppositeid)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100829"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F64F6C04F80007", "需结算时，对方档案ID不可为空！") /* "需结算时，对方档案ID不可为空！" */);
            }
//            if (ObjectUtils.isEmpty(oppositeaccount)) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100830"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F64FA404F80001", "需结算时，对方银行信息ID不可为空！") /* "需结算时，对方银行信息ID不可为空！" */);
//            }
        }

    }

    private static void checkOppositeaccount (String oppositename,String oppositebankaccountname,String oppositebankaccount,String oppositebankNumber,Short Settlestatus,Short Paymentsettlemode,Short Oppositetype) {
        if (!(Settlestatus.equals(FundSettleStatus.SettleSuccess.getValue()) || Paymentsettlemode.equals(PaymentSettlemode.CounterpartyDeduction.getValue())
                || !Oppositetype.equals(MarginOppositeType.Other.getValue()))) {

            if (ObjectUtils.isEmpty(oppositename)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100831"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_195A338A04200008", "需结算并且对方类型为其他时，对方名称不可为空！") /* "需结算并且对方类型为其他时，对方名称不可为空！" */);
            }

            if (ObjectUtils.isEmpty(oppositebankaccountname)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100832"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F6694005080006", "需结算并且对方类型为其他时，对方银行账户名称不可为空！") /* "需结算并且对方类型为其他时，对方银行账户名称不可为空！" */);
            }
            if (ObjectUtils.isEmpty(oppositebankaccount)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100833"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F66B4C04F80009", "需结算并且对方类型为其他时，对方银行账号不可为空！") /* "需结算并且对方类型为其他时，对方银行账号不可为空！" */);
            }

            if (ObjectUtils.isEmpty(oppositebankNumber)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100834"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F66CE205080007", "需结算并且对方类型为其他时，对方开户网点不可为空！") /* "需结算并且对方类型为其他时，对方开户网点不可为空！" */);
            }
        }
    }

    private static void checkConversion(Short tradetypeflag,BigDecimal conversionamount, Integer natMoneyDigit, PayMargin payMargin){
        if (tradetypeflag == MarginTradeTypeFlag.MarginWithdraw.getValue() && ObjectUtils.isNotEmpty(payMargin.getConversionmarginflag())) {
            //是否转换
            Short conversionmarginflag = payMargin.getConversionmarginflag();
            if (conversionmarginflag == 1) {
                //转换金额
                if (ObjectUtils.isEmpty(conversionamount)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100835"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F674D404F80003", "转换保证金为是时，转换金额不可为空！") /* "转换保证金为是时，转换金额不可为空！" */);
                }
                if (conversionamount.compareTo(BigDecimal.ZERO) == -1) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100836"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F6759C04F80004", "转换保证金为是时，转换金额不可小于0！") /* "转换保证金为是时，转换金额不可小于0！" */);
                }
                // 需要根据转换金额 计算转换本币金额
                if (ObjectUtils.isNotEmpty(conversionamount)) {
                    //计算natconversionamount
                    payMargin.setNatconversionamount(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(payMargin.getExchRateOps(), payMargin.getExchRate(), conversionamount, natMoneyDigit));
                } else {
                    payMargin.setConversionamount(BigDecimal.ZERO);
                    payMargin.setNatconversionamount(BigDecimal.ZERO);
                }

                //新保证金原始业务号
                String newmarginbusinessno = payMargin.getNewmarginbusinessno();
                if (ObjectUtils.isEmpty(newmarginbusinessno)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100837"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F6773A05080008", "转换保证金为是时，新保证金原始业务号不可为空！") /* "转换保证金为是时，新保证金原始业务号不可为空！" */);
                }
            } else {
                payMargin.setConversionamount(null);
                payMargin.setNewdept(null);
                payMargin.setNewproject(null);
                payMargin.setNewexpectedretrievaldate(null);
            }

        } else {
            payMargin.setConversionmarginflag((short) 0);
        }

    }

    private static void checkassociation(PayMargin payMargin){
        Short ourassociationstatus = payMargin.getOurassociationstatus();
        if (ObjectUtils.isEmpty(ourassociationstatus)) {
            payMargin.setOurassociationstatus(AssociationStatus.NoAssociated.getValue());
        } else {
            if (ourassociationstatus == AssociationStatus.Associated.getValue()) {
                //本方勾兑号
                String ourcheckno = payMargin.getOurcheckno();
                if (ObjectUtils.isEmpty(ourcheckno)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100838"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F682FA04F80004", "本方关联状态为已关联时，本方勾兑号不可为空！") /* "本方关联状态为已关联时，本方勾兑号不可为空！" */);
                }
                //本方银行对账单id
                String ourbankbillid = payMargin.getOurbankbillid();
                //本方认领单id
                String ourbillclaimid = payMargin.getOurbillclaimid();
                if (ObjectUtils.isEmpty(ourbillclaimid) && ObjectUtils.isEmpty(ourbankbillid)) {
                    //不能同时为空
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100839"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F6854605080005", "本方关联状态为已关联时，本方银行对账单id和本方认领单id不可同时为空！") /* "本方关联状态为已关联时，本方银行对账单id和本方认领单id不可同时为空！" */);

                } else if (ObjectUtils.isNotEmpty(ourbillclaimid) && ObjectUtils.isNotEmpty(ourbankbillid)) {
                    //不能同时有值
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100840"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F685A005080000", "本方关联状态为已关联时，本方银行对账单id和本方认领单id不可同时有值！") /* "本方关联状态为已关联时，本方银行对账单id和本方认领单id不可同时有值！" */);
                }
            }

        }

        Short oppassociationstatus = payMargin.getOppassociationstatus();
        if (ObjectUtils.isEmpty(oppassociationstatus)) {
            payMargin.setOppassociationstatus(AssociationStatus.NoAssociated.getValue());
        } else {
            if (oppassociationstatus == AssociationStatus.Associated.getValue()) {
                //对方勾兑号
                String oppcheckno = payMargin.getOppcheckno();
                if (ObjectUtils.isEmpty(oppcheckno)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100841"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F6873E04F80006", "对方关联状态为已关联时，对方勾兑号不可为空！") /* "对方关联状态为已关联时，对方勾兑号不可为空！" */);
                }
                //对方银行对账单id
                String oppbankbillid = payMargin.getOppbankbillid();
                //对方认领单id
                String oppbillclaimid = payMargin.getOppbillclaimid();
                if (ObjectUtils.isEmpty(oppbankbillid) && ObjectUtils.isEmpty(oppbillclaimid)) {
                    //不能同时为空
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100842"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F688E804F80004", "对方关联状态为已关联时，对方银行对账单id和对方认领单id不可同时为空！") /* "对方关联状态为已关联时，对方银行对账单id和对方认领单id不可同时为空！" */);
                } else if (ObjectUtils.isNotEmpty(oppbankbillid) && ObjectUtils.isNotEmpty(oppbillclaimid)) {
                    //不能同时有值
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100843"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F6888804F80003", "对方关联状态为已关联时，对方银行对账单id和对方认领单id不可同时有值！") /* "对方关联状态为已关联时，对方银行对账单id和对方认领单id不可同时有值！" */);
                }

            }
        }
    }

    //校验保证金虚拟户可用余额
    private void checkamount(Short tradetypeflag,BigDecimal marginamount,BigDecimal conversionamount,String marginbusinessno,PayMargin payMargin) {
        //取回保证金的时候 需要校验虚拟户的金额
        //1.先判断是否转换保证金  非转换的 校验取回金额
        //2.转换的需要校验转换金额+取回金额
        if (tradetypeflag == MarginTradeTypeFlag.MarginWithdraw.getValue() && ObjectUtils.isNotEmpty(payMargin.getConversionmarginflag())) {
            //是否转换
            Short conversionmarginflag = payMargin.getConversionmarginflag();
            BigDecimal sumamount = new BigDecimal("0.0");
            if (ObjectUtils.isEmpty(marginamount)) {
                marginamount = new BigDecimal("0.0");
            }
            if (ObjectUtils.isEmpty(conversionamount)) {
                conversionamount = new BigDecimal("0.0");
            }
            if (conversionmarginflag == 1) {
                // 相加
                sumamount = marginamount.add(conversionamount);
            } else {
                sumamount = marginamount;
            }
            //调用balanceQueryPay接口
            CommonRequestDataVo commonRequestDataVo = new CommonRequestDataVo();
            commonRequestDataVo.setMarginbusinessno(marginbusinessno);
            try {
                CommonResponseDataVo result = balanceQueryPay(commonRequestDataVo);
                if (ObjectUtils.isEmpty(result)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100844"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA3B69E04780003", "取回保证金时，根据原始业务号没有查询到保证金虚拟账户！") /* "取回保证金时，根据原始业务号没有查询到保证金虚拟账户！！" */);
                }
                //校验金额
                BigDecimal marginAvailableBalance = result.getMarginAvailableBalance();
                BigDecimal difference = marginAvailableBalance.subtract(sumamount);
                if (difference.compareTo(BigDecimal.ZERO) < 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100170"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA34D0604C00009", "该单据保证金发生额大于“保证金可用余额”，请检查！") /* "该单据保证金发生额大于“保证金可用余额”，请检查！" */);
                }
            } catch (Exception e){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100845"),e.getMessage());
            }
        }

    }
}
