package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodModel;
import com.yonyou.iuap.bizdoc.service.model.SettleMethodQueryParam;
import com.yonyou.iuap.context.InvocationInfoProxy;
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
import com.yonyoucloud.fi.cmp.common.pushAndPull.PushAndPullServiceMargin;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.margincommon.service.MarginCommonService;
import com.yonyoucloud.fi.cmp.margintype.MarginType;
import com.yonyoucloud.fi.cmp.marginworkbench.MarginWorkbench;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.api.margin.CtmcmpReceiveMarginRpcService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonRequestDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.common.CommonResponseDataVo;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.margin.ReceiveMarginVO;
import com.yonyoucloud.fi.cmp.pushAndPull.PushAndPullModel;
import com.yonyoucloud.fi.cmp.receivemargin.ReceiveMargin;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.yonyoucloud.fi.cmp.constant.ICmpConstant.*;

/**
 * @author xuxbo
 * @date 2023/8/19 10:39
 */
@Service
public class CtmcmpReceiveMarginRpcServiceImpl implements CtmcmpReceiveMarginRpcService {

    private static final String SYSTEMCODE = "system_0001";

    @Autowired
    private MarginCommonService marginCommonService;


    /**
     * 单据转换code，详见单据转换表配置
     */
    private static final String AGREEMENTTOCMPRECEIVEMARGIN = "agreementToCmpReceivemargin";

    private static final String CREDITCHANGETOCMPRECEIVEMARGIN = "creditchangeToCmpReceivemargin";
    private static final String FINANCINGREGISTERTOCMPRECEIVEMARGIN = "financingregisterToCmpReceivemargin";
    private static final String FINANCEINTOCMPRECEIVEMARGIN = "financeinToCmpReceivemargin";
    private static final String REPAYMENTTOCMPRECEIVEMARGIN = "repaymentToCmpReceivemargin";
    private static final String FINANCINGROLLOVERTOCMPRECEIVEMARGIN = "financingrolloverToCmpReceivemargin";
//    private static final String AGREEMENTToCmpReceivemargin = "agreementToCmpReceivemargin";
//    private static final String AGREEMENTToCmpReceivemargin = "agreementToCmpReceivemargin";
//    private static final String AGREEMENTToCmpReceivemargin = "agreementToCmpReceivemargin";


    @Autowired
    private ITransTypeService iTransTypeService;

    @Autowired
    BaseRefRpcService baseRefRpcService;

    @Autowired
    private IFIBillService fiBillService;

    @Autowired
    private PushAndPullServiceMargin pushAndPullService;

    /**
     * 支付保证金存入接口实现*
     *
     * @param receiveMarginVO
     * @return
     * @throws Exception
     */
    @Override
    public ReceiveMarginVO receiveMarginSave(ReceiveMarginVO receiveMarginVO) throws Exception {
        ReceiveMargin receiveMargin = new ReceiveMargin();
        Short initflagvo = 0;
        if (ObjectUtils.isNotEmpty(receiveMarginVO.getInitflag())){
            if (receiveMarginVO.getInitflag()) {
                initflagvo = 1;
            }
        }
        Short settleflagvo = 0;
        if (ObjectUtils.isNotEmpty(receiveMarginVO.getSettleflag())){
            if (receiveMarginVO.getSettleflag()) {
                settleflagvo = 1;
            }
        }
        Short conversionmarginflagvo = 0;
        if (ObjectUtils.isNotEmpty(receiveMarginVO.getConversionmarginflag())){
            if (receiveMarginVO.getConversionmarginflag()) {
                conversionmarginflagvo = 1;
            }
        }

        Short autorefundflag = 0;
        if (ObjectUtils.isNotEmpty(receiveMarginVO.getAutorefundflag())) {
            if (receiveMarginVO.getAutorefundflag()) {
                autorefundflag = 1;
            }

        }

        receiveMarginVO.setInitflag(null);
        receiveMarginVO.setSettleflag(null);
        receiveMarginVO.setConversionmarginflag(null);
        receiveMarginVO.setAutorefundflag(null);
        receiveMargin =  Objectlizer.convert(JSONBuilderUtils.beanToMap(receiveMarginVO),ReceiveMargin.ENTITY_NAME);

        receiveMargin.setSettleflag(settleflagvo);
        receiveMargin.setConversionmarginflag(conversionmarginflagvo);
        receiveMargin.setAutorefundflag(autorefundflag);
        //收到保证金新增接口
        receiveMargin.setAssociationstatus(receiveMarginVO.getAssociationstatus());
        receiveMargin.setCheckno(receiveMarginVO.getCheckno());
        receiveMargin.setBankbillid(receiveMarginVO.getBankbillid());
        receiveMargin.setBillclaimid(receiveMarginVO.getBillclaimid());



        String accentity = receiveMargin.getAccentity();
        if (StringUtils.isEmpty(accentity)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100819"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803FC", "会计主体不可为空!") /* "会计主体不可为空!" */);
        }

        Date vouchdate = receiveMargin.getVouchdate();
        if (ObjectUtils.isEmpty(vouchdate)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100820"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00058", "单据业务日期不可为空！") /* "单据业务日期不可为空！" */);
        }

        String marginbusinessno = receiveMargin.getMarginbusinessno();
        if (StringUtils.isEmpty(marginbusinessno)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100821"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803FE", "保证金原始业务号不可为空!") /* "保证金原始业务号不可为空!" */);
        }

        Short tradetypeflag = receiveMargin.getTradetypeflag();
        if (ObjectUtils.isEmpty(tradetypeflag)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100822"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180401", "交易类型标识不可为空！") /* "交易类型标识不可为空！" */);
        }

        //根据单据类型的租户id和formId,查询单据类型的下的交易类型
        List<BdTransType> bdTransTypes = iTransTypeService.getTransTypesByFormId(InvocationInfoProxy.getTenantid().toString(), CM_CMP_RECEIVEMARGIN);

        //根据交易类型标识 0-支付保证金 1-取回保证金，获取交易类型的id
        if (tradetypeflag == MarginTradeTypeFlag.MarginReceive.getValue()) {
            //循环遍历bdTransTypes，取 已启用并且扩展字段中{"receivemargin_ext":"cmp_receivemargin_receive"}receivemargin_ext为cmp_receivemargin_receive的 第一条数据的id
            if (bdTransTypes.size() > 0) {
                //CtmJSONObject jsonObject = JSON.parseObject(bdTransType.getExtendAttrsJson()).get("depositType_ext")
                for (BdTransType bdTransType : bdTransTypes) {
                    if (ObjectUtils.isNotEmpty(bdTransType.getExtendAttrsJson())) {
                        CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
                        if (bdTransType.getEnable() == 1 && (bdTransType.getCode().equals("cmp_receivemargin_receive") || (ObjectUtils.isNotEmpty(jsonObject) && jsonObject.get("receivemargin_ext").equals("cmp_receivemargin_receive")))) {
                            //赋值tradetype
                            receiveMargin.setTradetype(bdTransType.getId());
                            break;
                        }
                    }

                }
            }
        } else if (tradetypeflag == MarginTradeTypeFlag.MarginReturn.getValue()) {
            //循环遍历bdTransTypes，取 已启用并且扩展字段中{"receiveMargin_ext":"cmp_receivemargin_return"} receiveMargin_ext为cmp_receivemargin_return的 第一条数据的id
            for (BdTransType bdTransType : bdTransTypes) {
                if (ObjectUtils.isNotEmpty(bdTransType.getExtendAttrsJson())) {
                    CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
                    if (bdTransType.getEnable() == 1 && (bdTransType.getCode().equals("cmp_receivemargin_return") || (ObjectUtils.isNotEmpty(jsonObject) && jsonObject.get("receivemargin_ext").equals("cmp_receivemargin_return")))) {
                        //赋值tradetype
                        receiveMargin.setTradetype(bdTransType.getId());
                        break;
                    }
                }
            }

        }

        String currency = receiveMargin.getCurrency();
        if (StringUtils.isEmpty(currency)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100823"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00057", "币种不可为空!") /* "币种不可为空!" */);
        }

        //todo 保证金类型

        //事项来源 srcitem
        Short srcitem = receiveMargin.getSrcitem();
        if (ObjectUtils.isEmpty(srcitem)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100824"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803ED", "事项来源不可为空！") /* "事项来源不可为空！" */);
        }
        //单据类型 billtype
        String billtype = receiveMargin.getBilltype();
        if (ObjectUtils.isEmpty(billtype)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100825"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F11D8604F80006", "单据类型不可为空！") /* "单据类型不可为空！" */);
        }
        //来源业务单据编号
        String srcbillno = receiveMargin.getSrcbillno();
        if (ObjectUtils.isEmpty(srcbillno)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100826"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803F1", "来源业务单据编号不可为空！") /* "来源业务单据编号不可为空！" */);
        }
        //来源业务单据id
        String srcbillid = receiveMargin.getSrcbillid();
        if (ObjectUtils.isEmpty(srcbillid)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100827"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803F4", "来源业务单据id不可为空！") /* "来源业务单据id不可为空！" */);
        }

        //汇率
        BigDecimal exchRate1 = receiveMargin.getExchRate();
        List<Map<String, Object>> accEntity = QueryBaseDocUtils.queryAccRawEntityByAccEntityId(accentity);
        CurrencyTenantDTO natCurrencyTenantDTO = new CurrencyTenantDTO();
        Integer natMoneyDigit = null;
        if (accEntity.size() != 0) {
            natCurrencyTenantDTO = baseRefRpcService.queryCurrencyById(accEntity.get(0).get("currency").toString());
            if (natCurrencyTenantDTO != null) {
                receiveMargin.setNatCurrency(natCurrencyTenantDTO.getId());
                natMoneyDigit = natCurrencyTenantDTO.getMoneydigit();
            }
        }

        if (ObjectUtils.isEmpty(exchRate1)) {
            BigDecimal exchangerate = BigDecimal.ONE; //汇率
            Short exchRateOp = 1;
            if (ObjectUtils.isEmpty(receiveMargin.getExchangeratetype())) {
                //汇率类型非必填，汇率 默认设置为基准汇率
                ExchangeRateTypeVO exchangeRateTypeVO = baseRefRpcService.queryExchangeRateRateTypeByCode("01");
                receiveMargin.setExchangeratetype(exchangeRateTypeVO.getId());
            }
            //本币币种
            if (!natCurrencyTenantDTO.getId().equals(currency)) {
                CmpExchangeRateVO newExchangeRateWithMode = CmpExchangeRateUtils.getNewExchangeRateWithMode(currency, natCurrencyTenantDTO.getId(), vouchdate, receiveMargin.getExchangeratetype());
                exchangerate = newExchangeRateWithMode.getExchangeRate();
                exchRateOp = newExchangeRateWithMode.getExchangeRateOps();
            }
            //存入汇率
            receiveMargin.setExchRate(exchangerate);
            receiveMargin.setExchRateOps(exchRateOp);
        } else if (ObjectUtils.isEmpty(receiveMargin.getExchRateOps())) {
            // 当传入汇率时，但是汇率折算方式没有传入时，默认赋值乘法
            receiveMargin.setExchRateOps(EXCHANGE_RATE_OPS_MULTIPLY);
        }

        //保证金金额：交易类型为收到保证金时必填，交易类型为退还保证金时，保证金金额和转换金额不可以同时为空
        BigDecimal marginamount = receiveMargin.getMarginamount();
        BigDecimal conversionamount = receiveMargin.getConversionamount();
        if (tradetypeflag == MarginTradeTypeFlag.MarginReceive.getValue()) {
            if (ObjectUtils.isEmpty(marginamount)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101080"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F9979E04F80009", "交易类型为收到保证金时,保证金金额不可为空！") /* "交易类型为收到保证金时,保证金金额不可为空！" */);
            }
        } else if(tradetypeflag == MarginTradeTypeFlag.MarginReturn.getValue()) { //交易类型为退还保证金时，保证金金额和转换金额不可以同时为空
            if (ObjectUtils.isEmpty(marginamount) && ObjectUtils.isEmpty(conversionamount)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101081"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F9989404F80008", "交易类型为退还保证金时，保证金金额和转换金额不可同时为空！") /* "交易类型为退还保证金时，保证金金额和转换金额不可同时为空！" */);
            }
        }

        if (ObjectUtils.isNotEmpty(marginamount)) {
            //计算natmarginamount
            receiveMargin.setNatmarginamount(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(receiveMargin.getExchRateOps(), receiveMargin.getExchRate(), marginamount, natMoneyDigit));
        } else {
            receiveMargin.setMarginamount(BigDecimal.ZERO);
            receiveMargin.setNatmarginamount(BigDecimal.ZERO);
        }

        //对方类型 oppositetype
        Short oppositetype = receiveMargin.getOppositetype();
        if (ObjectUtils.isEmpty(oppositetype)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100806"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803F9", "对方类型不可为空！") /* "对方类型不可为空！" */);
        }



        //是否结算  保证金金额大于0时，必填
        Short settleflag = receiveMargin.getSettleflag();
        if (marginamount.compareTo(BigDecimal.ZERO) == 1 && ObjectUtils.isEmpty(settleflag)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100807"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F13F3C04F80007", "保证金金额大于0时,是否结算标识不可为空！") /* "是否结算标识不可为空！" */);
        }
        //当是否结算为是时，必传 枚举值为1-付款/0-收款
        Short paymenttype = receiveMargin.getPaymenttype();
        if (settleflag == 1) {
            //付款结算模式
            Short paymentsettlemode = receiveMargin.getPaymentsettlemode();
            if (ObjectUtils.isEmpty(paymentsettlemode)) {
                //如果付款结算模式为空，则赋默认值为 主动结算
                receiveMargin.setPaymentsettlemode(PaymentSettlemode.ActiveSettlement.getValue());
            }
            if (ObjectUtils.isEmpty(paymenttype)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100808"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180402","需结算时，收付类型不可为空！") /* "需结算时，收付类型不可为空！" */);
            }
            Long settlemode = receiveMargin.getSettlemode();
            if (ObjectUtils.isEmpty(settlemode)) {
                //如果是否结算为是，默认等于预制的结算方式银行转账
                //取结算方式为银行转账的档案id 进行赋值
                SettleMethodQueryParam settleMethodQueryParam = new SettleMethodQueryParam();
                settleMethodQueryParam.setCode(SYSTEMCODE);
                settleMethodQueryParam.setIsEnabled(CONSTANT_ONE);
                settleMethodQueryParam.setTenantId(AppContext.getTenantId());
                settleMethodQueryParam.setServiceAttr(CONSTANT_ZERO);
                List<SettleMethodModel> dataList = baseRefRpcService.querySettleMethods(settleMethodQueryParam);
                if (dataList.size() > 0) {
                    receiveMargin.setSettlemode(dataList.get(0).getId());
                } else {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100828"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F6008204F80003", "获取结算方式失败！") /* "获取结算方式失败！" */);
                }
            }
            //结算状态
            Short settlestatus = receiveMargin.getSettlestatus();
            if (ObjectUtils.isEmpty(settlestatus)) {
                //不上送，是否结算为是时，系统默认赋值待结算
                receiveMargin.setSettlestatus(FundSettleStatus.WaitSettle.getValue());
            }

            if (receiveMargin.getSettlestatus().equals(FundSettleStatus.SettleSuccess.getValue())) {
                //上游传的结算状态如果是结算成功 咱们这边给赋值已结算补单
                receiveMargin.setSettlestatus(FundSettleStatus.SettlementSupplement.getValue());
            }

            //本方银行账户 enterprisebankaccount
            String enterprisebankaccount = receiveMargin.getEnterprisebankaccount();
            if (ObjectUtils.isEmpty(enterprisebankaccount)) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100809"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803EE", "需结算时，本方银行账户不可为空！") /* "需结算时，本方银行账户不可为空！" */);
            }

            String oppositeid = receiveMarginVO.getOppositeid();
            String oppositeaccount = receiveMarginVO.getOppositeaccount();
            // 对方档案id 和 对方银行信息id
            if (!(receiveMargin.getSettlestatus().equals(FundSettleStatus.SettleSuccess.getValue()) || receiveMargin.getSettlestatus().equals(FundSettleStatus.SettlementSupplement.getValue()) || receiveMargin.getPaymentsettlemode().equals(PaymentSettlemode.CounterpartyDeduction.getValue())
                    || receiveMargin.getOppositetype().equals(MarginOppositeType.Other.getValue()))) {

                if (ObjectUtils.isEmpty(oppositeid)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100829"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F64F6C04F80007", "需结算时，对方档案ID不可为空！") /* "需结算时，对方档案ID不可为空！" */);
                }
                if (ObjectUtils.isEmpty(oppositeaccount)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100830"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F64FA404F80001", "需结算时，对方银行信息ID不可为空！") /* "需结算时，对方银行信息ID不可为空！" */);
                }


            }
            //根据对方类型进行 对方档案id的映射
            switch (oppositetype) {
                case 1: //客户
                    receiveMargin.setCustomer(ValueUtils.isNotEmptyObj(oppositeid) ? Long.valueOf(oppositeid) : null);
                    receiveMargin.setCustomerbankaccount(ValueUtils.isNotEmptyObj(oppositeaccount) ? Long.valueOf(oppositeaccount) : null);
                    break;
                case 2: //供应商
                    receiveMargin.setSupplier(ValueUtils.isNotEmptyObj(oppositeid) ? Long.valueOf(oppositeid) : null);
                    receiveMargin.setSupplierbankaccount(ValueUtils.isNotEmptyObj(oppositeaccount) ? Long.valueOf(oppositeaccount) : null);
                    break;
                case 3: //其他
                    break;
                case 4: //内部单位
                    receiveMargin.setOurname(oppositeid);
                    receiveMargin.setOurbankaccount(oppositeaccount);
                    break;
                case 5: //资金业务对象
                    receiveMargin.setCapBizObj(oppositeid);
                    receiveMargin.setCapBizObjbankaccount(oppositeaccount);
                    break;
                default:
                    break;
            }
            //对方名称 对方银行账户名称 对方银行账号 对方开户行档案id
            if (!(receiveMargin.getSettlestatus().equals(FundSettleStatus.SettleSuccess.getValue()) || receiveMargin.getPaymentsettlemode().equals(PaymentSettlemode.CounterpartyDeduction.getValue())
                    || !receiveMargin.getOppositetype().equals(MarginOppositeType.Other.getValue()))) {
                //对方名称
                String oppositename = receiveMargin.getOppositename();
                if (ObjectUtils.isEmpty(oppositename)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100831"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_195A338A04200008", "需结算并且对方类型为其他时，对方名称不可为空！") /* "需结算并且对方类型为其他时，对方名称不可为空！" */);
                }
                //对方银行账户名称
                String oppositebankaccountname = receiveMargin.getOppositebankaccountname();
                if (ObjectUtils.isEmpty(oppositebankaccountname)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100832"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F6694005080006", "需结算并且对方类型为其他时，对方银行账户名称不可为空！") /* "需结算并且对方类型为其他时，对方银行账户名称不可为空！" */);
                }
                //对方银行账号
                String oppositebankaccount = receiveMargin.getOppositebankaccount();
                if (ObjectUtils.isEmpty(oppositebankaccount)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100833"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F66B4C04F80009", "需结算并且对方类型为其他时，对方银行账号不可为空！") /* "需结算并且对方类型为其他时，对方银行账号不可为空！" */);
                }
                //对方开户行档案id
                String oppositebankNumber = receiveMargin.getOppositebankNumber();
                if (ObjectUtils.isEmpty(oppositebankNumber)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100834"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F66CE205080007", "需结算并且对方类型为其他时，对方开户网点不可为空！") /* "需结算并且对方类型为其他时，对方开户网点不可为空！" */);
                }
            }
            //上游单据会把所有的对方信息都传递  对方单位如果不是其他，此时把 对方名称 对方银行账户名称 对方银行账号 对方开户行档案id 置空
            if(!receiveMargin.getOppositetype().equals(MarginOppositeType.Other.getValue())){
                receiveMargin.setOppositename(null);
                receiveMargin.setOppositebankaccountname(null);
                receiveMargin.setOppositebankaccount(null);
                receiveMargin.setOppositebankNumber(null);
            }
        } else {
            receiveMargin.setSettlemode(null);
            receiveMargin.setPaymentsettlemode(PaymentSettlemode.ActiveSettlement.getValue());
            //交易类型为收到保证金时，等于收款  交易类型为退还保证金时，等于付款
            if (ObjectUtils.isEmpty(paymenttype)) {
                if(tradetypeflag.equals(2)) {
                    receiveMargin.setPaymenttype(PaymentType.FundCollection.getValue());
                } else {
                    receiveMargin.setPaymenttype(PaymentType.FundPayment.getValue());
                }

            }

            if (ObjectUtils.isNotEmpty(receiveMargin.getSettlestatus())) {
                if (receiveMargin.getSettlestatus().equals(FundSettleStatus.SettleSuccess.getValue())) {
                    //上游传的结算状态如果是结算成功 咱们这边给赋值已结算补单
                    receiveMargin.setSettlestatus(FundSettleStatus.SettlementSupplement.getValue());
                }
            } else {
                //不传结算时：如果上游没有传结算状态 赋值为待结算
                receiveMargin.setSettlestatus(FundSettleStatus.WaitSettle.getValue());

            }
        }
        //交易类型为退还保证金时，可以上送，否则，上送系统也不入库，默认赋值否
        if (tradetypeflag == MarginTradeTypeFlag.MarginReturn.getValue() && ObjectUtils.isNotEmpty(receiveMargin.getConversionmarginflag())) {
            //是否转换
            Short conversionmarginflag = receiveMargin.getConversionmarginflag();
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
                    receiveMargin.setNatconversionamount(CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(receiveMargin.getExchRateOps(), receiveMargin.getExchRate(), conversionamount, natMoneyDigit));
                } else {
                    receiveMargin.setConversionamount(BigDecimal.ZERO);
                    receiveMargin.setNatconversionamount(BigDecimal.ZERO);
                }

                //新保证金原始业务号
                String newmarginbusinessno = receiveMargin.getNewmarginbusinessno();
                if (ObjectUtils.isEmpty(newmarginbusinessno)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100837"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18F6773A05080008", "转换保证金为是时，新保证金原始业务号不可为空！") /* "转换保证金为是时，新保证金原始业务号不可为空！" */);
                }

            } else {
                receiveMargin.setConversionamount(null);
                receiveMargin.setNewdept(null);
                receiveMargin.setNewproject(null);
                receiveMargin.setNewlatestreturndate(null);
            }

        } else {
            receiveMargin.setConversionmarginflag((short) 0);
        }

        //关联信息
        Short associationstatus = receiveMargin.getAssociationstatus();
        if (ObjectUtils.isEmpty(associationstatus)) {
            receiveMargin.setAssociationstatus(AssociationStatus.NoAssociated.getValue());
        } else {
            if (associationstatus == AssociationStatus.Associated.getValue()) {
                //勾兑号
                String checkno = receiveMargin.getCheckno();
                if (ObjectUtils.isEmpty(checkno)) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101082"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18FB232C05080008", "关联状态为已关联时，勾兑号不可为空！") /* "关联状态为已关联时，勾兑号不可为空！" */);
                }
                //银行对账单id
                String bankbillid = receiveMargin.getBankbillid();
                //认领单id
                String billclaimid = receiveMargin.getBillclaimid();
                if (ObjectUtils.isEmpty(billclaimid) && ObjectUtils.isEmpty(bankbillid)) {
                    //不能同时为空
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101083"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18FB241A04F80008", "关联状态为已关联时，银行对账单id和认领单id不可同时为空！") /* "关联状态为已关联时，银行对账单id和认领单id不可同时为空！" */);

                } else if (ObjectUtils.isNotEmpty(billclaimid) && ObjectUtils.isNotEmpty(bankbillid)) {
                    //不能同时有值
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101084"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18FB245804F80004", "关联状态为已关联时，银行对账单id和认领单id不可同时有值！") /* "关联状态为已关联时，银行对账单id和认领单id不可同时有值！" */);
                }
            }

        }


        //退还保证金的时候 需要校验虚拟户的金额
        //1.先判断是否转换保证金  非转换的 校验取回金额
        //2.转换的需要校验转换金额+取回金额
        if (tradetypeflag == MarginTradeTypeFlag.MarginReturn.getValue() && ObjectUtils.isNotEmpty(receiveMargin.getConversionmarginflag())) {
            //是否转换
            Short conversionmarginflag = receiveMargin.getConversionmarginflag();
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
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101085"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA3B61604C00009", "退还保证金时，根据原始业务号没有查询到保证金虚拟账户！") /* "退还保证金时，根据原始业务号没有查询到保证金虚拟账户！！" */);
                }
                //校验金额
                BigDecimal marginAvailableBalance = result.getMarginAvailableBalance();
                BigDecimal difference = marginAvailableBalance.subtract(sumamount);
                if (difference.compareTo(BigDecimal.ZERO) < 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100170"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CA34D0604C00009", "该单据保证金发生额大于“保证金可用余额”，请检查！") /* "该单据保证金发生额大于“保证金可用余额”，请检查！" */);
                }
            } catch (Exception e){
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101086"),e.getMessage());
            }
        }


        String billnum = receiveMarginVO.getBillnum();
        BizObject bizObject = transBill(srcbillid,billnum);
        if (ObjectUtils.isNotEmpty(bizObject)) {
            if (ObjectUtils.isNotEmpty(bizObject.get("margintype"))) {
                long margintype = Long.valueOf(bizObject.get("margintype"));
                receiveMargin.setMargintype(margintype);
                checkMarginType(receiveMargin.getMargintype().toString());
            }
            if (ObjectUtils.isNotEmpty(bizObject.get("newmargintype"))) {
                long newmargintype = Long.valueOf(bizObject.get("newmargintype"));
                receiveMargin.setNewmargintype(newmargintype);
                checkMarginType(receiveMargin.getNewmargintype().toString());
            }
        }

        //设置期初为：否
        receiveMargin.setInitflag((short) 0);

        BillDataDto billDataDto = new BillDataDto(IBillNumConstant.CMP_RECEIVEMARGIN);
        receiveMargin.setEntityStatus(EntityStatus.Insert);
        billDataDto.setData(receiveMargin);
        //根据原始业务号查询支付保证金有几条  marginbusinessno
        boolean deleteflag = marginCommonService.findRecMargin(marginbusinessno);
        YtsContext.setYtsContext("isdeleteMVA", deleteflag);
        fiBillService.executeUpdate(OperationTypeEnum.SAVE.getValue(), billDataDto);
        YtsContext.setYtsContext("billid", ((List<ReceiveMargin>) billDataDto.getData()).get(0).getId());
        if (ObjectUtils.isNotEmpty(receiveMargin.getId())){
            receiveMarginVO.setId(receiveMargin.getId().toString());
        }
        return receiveMarginVO;
    }

    /**
     * 支付保证金存入接口回滚*
     *
     * @param receiveMarginVO
     * @return
     * @throws Exception
     */
    @Override
    public ReceiveMarginVO receiveMarginSaveRollback(ReceiveMarginVO receiveMarginVO) throws Exception {
        BillDataDto billDataDto = new BillDataDto(IBillNumConstant.CMP_RECEIVEMARGIN);
        QuerySchema querySchema = QuerySchema.create().addSelect(ICmpConstant.PRIMARY_ID);
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name(ICmpConstant.PRIMARY_ID).eq(YtsContext.getYtsContext("billid")));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<ReceiveMargin> receiveMarginList = MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema, null);
        if (receiveMarginList != null && receiveMarginList.size() > 0) {
            ReceiveMargin receiveMargin = receiveMarginList.get(0);
            receiveMargin.setEntityStatus(EntityStatus.Delete);
            receiveMargin.set("isRPC",true);
            if(YtsContext.getYtsContext("isdeleteMVA").equals(true)){
                receiveMargin.set("isRPCDELETE",true);
            } else {
                receiveMargin.set("isRPCDELETE",false);
            }
            billDataDto.setData(receiveMargin);
            fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto);
        } else {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100810"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00055", "查询失败") /* "查询失败" */);
        }
        return receiveMarginVO;
    }

    @Override
    public CommonResponseDataVo balanceQueryPay(CommonRequestDataVo params) throws Exception {
        CommonResponseDataVo result = new CommonResponseDataVo();
        //获取保证金原始业务号
        String marginbusinessno = params.getMarginbusinessno();
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup queryConditionGroup = new QueryConditionGroup(ConditionOperator.and);
        queryConditionGroup.addCondition(QueryCondition.name(ICmpConstant.MARGINBUSINESSNO).eq(marginbusinessno));
        queryConditionGroup.addCondition(QueryCondition.name("marginFlag").eq(MarginFlag.RecMargin.getValue()));
        querySchema.appendQueryCondition(queryConditionGroup);
        List<MarginWorkbench> marginWorkbenchList = MetaDaoHelper.queryObject(MarginWorkbench.ENTITY_NAME, querySchema, null);
        if (marginWorkbenchList != null && marginWorkbenchList.size() > 0) {
            MarginWorkbench marginWorkbench = marginWorkbenchList.get(0);
            //收到金额
            BigDecimal receivedAmount = marginWorkbench.getReceivedAmount();
            //退还金额
            BigDecimal returnAmount = marginWorkbench.getReturnAmount();
            //转换金额
            BigDecimal conversionAmount = marginWorkbench.getConversionAmount();
            //保证金余额
            BigDecimal marginBalance = marginWorkbench.getMarginBalance();
            //保证金可用余额
            BigDecimal marginAvailableBalance = marginWorkbench.getMarginAvailableBalance();

            result.setReceivedAmount(receivedAmount);
            result.setReturnAmount(returnAmount);
            result.setConversionAmount(conversionAmount);
            result.setMarginbalance(marginBalance);
            result.setMarginAvailableBalance(marginAvailableBalance);
        }
//        else {
//            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100811"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803F2", "当前保证金原始业务号没有期初数据！") /* "当前保证金原始业务号没有期初数据！" */);
//        }

        return result;
    }

    /**
     * 支付保证金删除*
     *
     * @param param
     * @return
     * @throws Exception
     */
    @Override
    public String deletereceiveMarginByIds(CommonRequestDataVo param) throws Exception {
        List<ReceiveMargin> receiveMarginList = new ArrayList<>();
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
            receiveMarginList = MetaDaoHelper.queryObject(ReceiveMargin.ENTITY_NAME, querySchema, null);
            BillDataDto billDataDto = new BillDataDto(IBillNumConstant.CMP_RECEIVEMARGIN);
            List<MarginWorkbench> marginWorkbenchList_rollback = new ArrayList<>();
            if (receiveMarginList != null && receiveMarginList.size() > 0) {
                for (ReceiveMargin receiveMargin : receiveMarginList) {
                    if (!(ObjectUtils.isEmpty(receiveMargin.getVerifystate())) && (receiveMargin.getVerifystate() == VerifyState.SUBMITED.getValue() || receiveMargin.getVerifystate() == VerifyState.COMPLETED.getValue())) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101087"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18FB291605080003", "当前业务单据关联的收到保证金已进行业务处理，无法删除！") /* "当前业务单据关联的收到保证金已进行业务处理，无法删除！" */);
                    }
                    receiveMargin.set("isRPC",true);
                    billDataDto.setData(receiveMargin);
                    //如果虚拟户对应的数据只有一条 则回滚的时候需要插入一条
                    Boolean rollbackflag = marginCommonService.useByMulRecMargin(receiveMargin.getMarginvirtualaccount().toString());
                    if (rollbackflag) {
                        String marginBusinessNo  = receiveMargin.getMarginbusinessno();
                        QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                        QueryConditionGroup conditionGroup1 = new QueryConditionGroup(ConditionOperator.and);
                        conditionGroup1.appendCondition(QueryCondition.name("marginBusinessNo").in(marginBusinessNo));
                        conditionGroup1.addCondition(QueryCondition.name("marginFlag").eq(MarginFlag.RecMargin.getValue()));
                        querySchema1.addCondition(conditionGroup1);
                        List<MarginWorkbench> marginWorkbenchList = MetaDaoHelper.queryObject(MarginWorkbench.ENTITY_NAME, querySchema1, null);
                        if (marginWorkbenchList.size() > 0 && ObjectUtils.isNotEmpty(marginWorkbenchList)){
                            marginWorkbenchList_rollback.add(marginWorkbenchList.get(0));
                        }
                    }
                    fiBillService.executeUpdate(OperationTypeEnum.DELETE.getValue(), billDataDto);
                }
                YtsContext.setYtsContext("receiveMarginList_fail", receiveMarginList);
                YtsContext.setYtsContext("marginWorkbenchList_fail", marginWorkbenchList_rollback);
            } else {
                return ResultMessage.success();
            }

        } catch (Exception e) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101088"),e.getMessage());
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
    public String deletereceiveMarginRollbackNew(CommonRequestDataVo param) throws Exception {

        List<ReceiveMargin> receiveMarginList = (List<ReceiveMargin>) YtsContext.getYtsContext("receiveMarginList_fail");
        if (receiveMarginList != null && receiveMarginList.size() > 0) {
            for (ReceiveMargin receiveMargin : receiveMarginList) {
                receiveMargin.setEntityStatus(EntityStatus.Insert);
                CmpMetaDaoHelper.insert(ReceiveMargin.ENTITY_NAME,receiveMargin);
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
            pushAndPullModel.setMainids(mainids);
            pushAndPullModel.setIsMainSelect(1);
            //根据billnum进行映射不同的code
            String pushCode = "";
            if (ObjectUtils.isNotEmpty(billnum)) {
                switch (billnum) {
//                case "cam_agreement" : //授信合同
//                    pushCode = AGREEMENTTOCMPRECEIVEMARGIN;
//                    break;
//                case "cam_creditchange" : //授信变更
//                    pushCode = CREDITCHANGETOCMPRECEIVEMARGIN;
//                    break;
                    case "tlm_financingregister" : //融资登记
                        pushCode = FINANCINGREGISTERTOCMPRECEIVEMARGIN;
                        break;
                    case "tlm_financein" : //融入登记
                        pushCode = FINANCEINTOCMPRECEIVEMARGIN;
                        break;
                    case "tlm_repayment" : //融资还本
                        pushCode = REPAYMENTTOCMPRECEIVEMARGIN;
                        break;
                    case "tlm_financingrollover" : //融资展期
                        pushCode = FINANCINGROLLOVERTOCMPRECEIVEMARGIN;
                        break;
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
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101089"),e.getMessage());
        }
        return bizObject;
    }
}
