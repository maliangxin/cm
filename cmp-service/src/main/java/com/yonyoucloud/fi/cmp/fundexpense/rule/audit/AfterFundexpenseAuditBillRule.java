package com.yonyoucloud.fi.cmp.fundexpense.rule.audit;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.ctm.stwb.datasettled.DataSettled;
import com.yonyoucloud.ctm.stwb.openapi.IOpenApiService;
import com.yonyoucloud.ctm.stwb.openapi.ResponseResult;
import com.yonyoucloud.fi.cmp.cmpentity.EventSource;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.enums.Dftype;
import com.yonyoucloud.fi.cmp.enums.SystemIntegrationParamsEnum;
import com.yonyoucloud.fi.cmp.fundcommon.service.IFundCommonService;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense;
import com.yonyoucloud.fi.cmp.fundexpense.Fundexpense_b;
import com.yonyoucloud.fi.cmp.fundexpense.service.FundexpenseService;
import com.yonyoucloud.fi.cmp.util.BillAction;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("afterFundexpenseAuditBillRule")
public class AfterFundexpenseAuditBillRule extends AbstractCommonRule {

    public static final String CMP_TOACCNTTYPE_CUSTOMER = "1"; // 对方类型 客户 1
    public static final String CMP_TOACCNTTYPE_SUPPLIER = "2"; // 对方类型 供应商 2
    public static final String CMP_TOACCNTTYPE_FUNDOBJECT = "5"; // 对方类型 资金业务对象 5
    public static final String CMP_YES = "1";
    public static final String CMP_NO = "0";
    @Resource
    private IOpenApiService openApiService;
    @Autowired
    private YmsOidGenerator ymsOidGenerator;
    @Resource
    private FundexpenseService fundexpenseService;
    @Resource
    private IFundCommonService fundCommonService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        List<BizObject> bills = getBills(billContext, paramMap);
        if (bills != null && bills.size() > 0) {
            //获取前端传过来的值对象
            Fundexpense fundexpense = (Fundexpense) bills.get(0);
            List<Fundexpense_b> details = fundexpense.detail();
            //资金结算
            boolean checkFundPlanIsEnabled = fundCommonService.checkFundPlanControlIsEnabled(SystemIntegrationParamsEnum.FundCollection.getValue());
            if (checkFundPlanIsEnabled) {
                for (Fundexpense_b detail : details) {
                    fundexpenseService.fundCollectionEmployActualOccupySuccessAudit(bills.get(0), detail, IBillNumConstant.FUNDEXPENSE, BillAction.APPROVE_PASS);
                }
            }
            Short expenseparam = fundexpense.getExpenseparam();
            //资金结算
            if (expenseparam == 0) {
                pushCharge(fundexpense);
            }
        }
        return new RuleExecuteResult();
    }

    /**
     * 传待结算
     * @throws Exception
     */
    private void pushCharge(Fundexpense fundexpense) throws Exception {
        Short expenseparam = fundexpense.getExpenseparam();
        //资金结算
        if(expenseparam == 0){
            //构建报文实体
            List<DataSettled> dataSettleds = buildStwbMsgSingle(fundexpense);
            //推送资金结算
            ResponseResult responseResult =openApiService.builtSystem(dataSettleds);
            if (responseResult.getCode() == 200) {
                //推送资金结算成功,更改状态为结算中
                fundexpense.setSettlestate(FundSettleStatus.SettleProssing.getValue());
            } else {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101154"),responseResult.getMessage());
            }
        }
        //应收应付 todo
        if(expenseparam == 1){
        }
    }
    /**
     * 单条报文推送
     * @throws Exception
     */
    private List<DataSettled> buildStwbMsgSingle(Fundexpense fundexpense) throws Exception {
        //单个资金结算单
        ArrayList<DataSettled> dataSettledList = new ArrayList<>();
        DataSettled dataSettled = new DataSettled();
        //请求流水id
        dataSettled.setSerialNumber(ymsOidGenerator.nextId()+"fundexpense");
        //来源业务系统
        dataSettled.setWdataOrigin(String.valueOf(EventSource.Cmpchase.getValue()));
        //会计主体
        dataSettled.setAccentity(fundexpense.getOrg());
        dataSettled.setDept(fundexpense.getDept());
        dataSettled.setProject(fundexpense.getProject());
        dataSettled.setExpenseitem(fundexpense.getExpenseitem());
        //业务单据类型 TODO
        dataSettled.setBusinessbilltype("87");
        //业务单据编号-单据编号
        dataSettled.setBusinessbillnum(fundexpense.getCode());
        //业务单据ID-单据id
        dataSettled.setBusinessId(fundexpense.getId().toString());
        //业务单据明细ID-无子表则传入主表id 即单据id
        dataSettled.setBusinessdetailsid(fundexpense.getId().toString());
        //交易类型 (存放id)——业务单据明细对应的交易类型
        dataSettled.setTradetype(fundexpense.getBustype());
        //收付类型 付款
        dataSettled.setRecpaytype(fundexpense.getExpenseDirect()==1?"2":"1");// 收付类型 付款=1，收款=2
        //原币币种 即费用币种
        dataSettled.setOricurrency(fundexpense.getExpensenatCurrency());// 原币币种（存放id）
        //原币金额 即费用金额
        dataSettled.setOricurrencyamount(fundexpense.getExpenseSum_fy().setScale(2, BigDecimal.ROUND_HALF_UP));
        //本币金额
        dataSettled.setNatSum(fundexpense.getExpenseSumBenbi().setScale(2, BigDecimal.ROUND_HALF_UP));
        //本币币种-存放id
        dataSettled.setNatcurrency(fundexpense.getNatCurrency());//本币币种（存放id）
        //汇率 即组织本币汇率
        dataSettled.setExchangerate(fundexpense.getExchRate());
        // 汇率类型
        dataSettled.setExchangePaymentRateType(fundexpense.getExchangeRateType());

        //来源数据结算状态:按结算处理模式传值，为空默认一般结算
        dataSettled.setOpenwsettlestatus(fundexpense.getSettlestate()==null?null:fundexpense.getSettlestate()+"");
        //付款结算模式
        //如果主动付款=是，默认主动结算，否则默认对方扣款
        dataSettled.setPaySettlementMode(fundexpense.getExpensePayMode()==1?"2":"1");
        // 对方类型
        if(fundexpense.getDftype()== Dftype.Merchant.getValue()){
            dataSettled.setToaccnttype(CMP_TOACCNTTYPE_CUSTOMER);
            dataSettled.setCounterpartyid(fundexpense.getDfenterprise_customer());
            dataSettled.setShowtoaccntname(fundexpense.getDfenterprise_customer_name());
            dataSettled.setCounterpartybankaccount(fundexpense.getDfcustomerbankaccount()==null?null:fundexpense.getDfcustomerbankaccount().toString());
            dataSettled.setShowoppositebankaccount(fundexpense.getDfcustomerbankaccount_account());
            dataSettled.setCapBizObjType(null);
        }
        if(fundexpense.getDftype()==Dftype.Supplier.getValue()){
            dataSettled.setToaccnttype(CMP_TOACCNTTYPE_SUPPLIER);
            dataSettled.setCounterpartyid(fundexpense.getDfenterprise_supplier()==null?null:fundexpense.getDfenterprise_supplier().toString());
            dataSettled.setShowtoaccntname(fundexpense.getDfenterprise_supplier_name()==null?null:fundexpense.getDfenterprise_supplier_name().toString());
            dataSettled.setCounterpartybankaccount(fundexpense.getDfsupplierbankaccount()==null?null:fundexpense.getDfsupplierbankaccount().toString());
            dataSettled.setShowoppositebankaccount(fundexpense.getDfsupplierbankaccount_account());
            dataSettled.setCapBizObjType(null);
        }
        if(fundexpense.getDftype()==Dftype.Funbusobj.getValue()){
            dataSettled.setToaccnttype(CMP_TOACCNTTYPE_FUNDOBJECT);
            dataSettled.setCounterpartyid(fundexpense.getDfenterprise_funbusobj());
            dataSettled.setShowtoaccntname(fundexpense.getDfenterprise_funbusobj_name());
            dataSettled.setCounterpartybankaccount(fundexpense.getDffunbusobjbankaccount());
            dataSettled.setShowoppositebankaccount(fundexpense.getDffunbusobjbankaccount_account());
            Map<String,Object> object = MetaDaoHelper.findById("tmsp.fundbusinobjarchives.FundBusinObjArchives", Long.parseLong(fundexpense.getDfenterprise_funbusobj()), "yonbip-fi-ctmtmsp");
            dataSettled.setCapBizObjType(object.get("fundbusinobjtypeid").toString());
        }
        //追索人账号名称
        dataSettled.setShowoppositebankaccountname(fundexpense.getDfenterprisecountbankname());
        //按追索人开户行名称传值
        dataSettled.setShowoppositebankname(fundexpense.getDfbankcountopenbankname());
        dataSettled.setShowoppositebanklineno(fundexpense.getDfaccountlinenumber());
        dataSettled.setOppositeBankTypeName(fundexpense.getDfbankacounttype());
        //是否结算系统可修改:否
        dataSettled.setIssettlementcanmodified(CMP_YES);
        //是否可拆分结算:fou
        dataSettled.setIssplit(CMP_NO);
        //是否可合并结算：否
        dataSettled.setIsmerge(CMP_NO);
        //是否登日记账：是
        dataSettled.setIsjournalregistered(CMP_YES);
        //是否生成结算凭证:是
        dataSettled.setIsGenerateVoucher(CMP_YES);
        //外部扩展字段
        dataSettled.setExternaloutdefine1(CMP_YES);
        dataSettled.setVouchdate(fundexpense.getVouchdate());
        dataSettledList.add(dataSettled);
        return dataSettledList;
    }

}
