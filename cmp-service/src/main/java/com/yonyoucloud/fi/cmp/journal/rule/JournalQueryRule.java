package com.yonyoucloud.fi.cmp.journal.rule;

import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterCommonVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.dao.meta.UiMetaDaoHelper;
import com.yonyou.ucf.mdd.ext.filter.util.StringUtil;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.EventType;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.cmpentity.SettleStatus;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.ctmrpc.CtmCmpBankReconciliationSettingRpcServiceImpl;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.BankReconciliationSettingVO;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.vo.receipt.PlanParam;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.util.bankreconciliation.checkandfilter.BankreconciliationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryOrderby;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 *   银行对账/余额调节表节点过滤条件匹配
 */
@Component
public class JournalQueryRule extends AbstractCommonRule {

    public static  String BANKRECONCILIATIONSCHEME = "bankreconciliationscheme";
    public static  String RECONCILIATIONDATASOURCE = "reconciliationdatasource";
    public static  String DZDATE = "dzdate";//对账日期
    public static  String CHECKDATE = "checkdate";//勾兑日期
    public static String ORTHER_CHECKDATE ="other_checkdate"; //总账勾对日期
    public static  String CMP_BANKJOURNALLIST = "cmp_bankjournallist";
    public static  String CMP_BANKRECONCILIATIONLISTCHECK = "cmp_bankreconciliationlistcheck";
    public static  String GL_BANKRECONCILIATIONLIST = "gl_bankreconciliationlist";
    public static  String CMP_CHECK = "cmp_check";
    public static  String CMP_BALANCEADJUST = "cmp_balanceadjust";
    public static  String BANKRECONCILIATIONSETTING_B = "bankReconciliationSetting_b";
    public static  String CHECKFLAG = "checkflag";
    public static  String OTHER_CHECKFLAG = "other_checkflag";
    public static  String BILLTYPE = "billtype";
    public static String DZENDDATE = "dzenddate"; //银行对账截止日期
    public static String TRANSDATE = "tran_date"; //交易日期
    public static String ACCENTITY = "accentity"; //会计主体
    public static String ISQC = "isQc"; //期初标识
    private static  String INIT_FLAG = "initflag"; //是否期初
    public static String ENABLEDATE = "enableDate"; //方案启用日期
    public static String VOUCHDATE = "vouchdate"; //日记账日期

    public static String ORDER1 ="1";//倒序
    public static String ORDER2 ="2";//正序

    @Autowired
    CtmCmpBankReconciliationSettingRpcServiceImpl cmpBankReconciliationSettingRpcService;

    @Autowired
    private CmpCheckService cmpCheckService;
    @Autowired
    AutoConfigService autoConfigService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto)getParam(paramMap);
        String action = billDataDto.getAction();
        if("output".equals(action)){
            return new RuleExecuteResult();
        }
        String externalData = (String) billDataDto.getExternalData();
        String billnum = billDataDto.getBillnum();
        Integer  reconciliationdatasource =0;
        //设置yms 参数 默认倒序，配置参数正序
        String cmp_check_sort = AppContext.getEnvConfig("cmp.check.cmp_check_sort","1");

        FilterVO filterVO = new FilterVO();
        if(billDataDto.getCondition() != null) {
            String accentityIsNoProcess = StringUtils.EMPTY;
            filterVO = billDataDto.getCondition();
            FilterCommonVO[] commonVOs = filterVO.getCommonVOs();
            List<Object> bankaccounts = new ArrayList<Object>();  //对账方案下的银行账户
            BizObject bankReconciliationSetting = null;
            Long bankreconciliationscheme = null;
            if(commonVOs!= null&&commonVOs.length>0){
                List<QueryOrderby> queryOrderlyList = new ArrayList<>();
                // start 20230912 wangdengk oracle适配 返回的顺序不一致导致查询报错
                FilterCommonVO schameFilter = Arrays.stream(commonVOs)
                        .filter(item -> BANKRECONCILIATIONSCHEME.equals(item.getItemName())).findFirst().orElse(null);
                if(schameFilter == null){
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100876"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000A3", "未查询到该对账方案相关信息,请检查!") /* "未查询到该对账方案相关信息,请检查!" */);
                }
                Long schameId = Long.valueOf(schameFilter.getValue1().toString());
                bankreconciliationscheme = schameId;
                bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME,schameId);
                if (bankReconciliationSetting == null){
                    if(CMP_BANKJOURNALLIST.equals(billnum)){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100877"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B544EF405A00020","日记账查询异常：未查询到该对账方案相关信息,请检查！") /* "日记账查询异常：未查询到该对账方案相关信息,请检查！" */);
                    }else {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100878"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B544EF405A00021","银行对账单查询异常：未查询到该对账方案相关信息,请检查!") /* "银行对账单查询异常：未查询到该对账方案相关信息,请检查!" */);
                    }
                }

                //保存态对账方案不可查询
                if ( bankReconciliationSetting.getShort("enableStatus") == EnableStatus.Saved.getValue()){
                    if(CMP_BANKJOURNALLIST.equals(billnum)){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100879"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B544EF405A00023","日记账查询异常：对账方案未启用！") /* "日记账查询异常：对账方案未启用！" */);
                    }else {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100880"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B544EF405A00024","银行对账单查询异常：对账方案未启用！") /* "银行对账单查询异常：对账方案未启用！" */);
                    }
                }

                reconciliationdatasource = bankReconciliationSetting.get(RECONCILIATIONDATASOURCE);
                PlanParam planParam = new PlanParam(null,null,bankReconciliationSetting.getString("id"));
                //现金适配资金组织用cmpCheckService去查询
                List<BankReconciliationSettingVO> infoList = cmpCheckService.findUseOrg(planParam);

                // end 20230912 wangdengk oracle适配 返回的顺序不一致导致查询报错
                // 获取已勾兑查询条件值
                String checkflagValue = null;
                FilterCommonVO checkFlagSchameFilter = Arrays.stream(commonVOs)
                        .filter(item -> CHECKFLAG.equals(item.getItemName())).findFirst().orElse(null);
                if (checkFlagSchameFilter != null){
                    checkflagValue = String.valueOf(checkFlagSchameFilter.getValue1());
                }
                //是否有授权使用组织选项
                boolean isContainUseorg = false;
                for(int i =0;i<commonVOs.length;i++){
                    FilterCommonVO  filterCommonVO = commonVOs[i];
                     if(ACCENTITY.equals(filterCommonVO.getItemName())){
                        if(FIDubboUtils.isSingleOrg()){  //单组织逻辑
                            List<String> accentitys = new ArrayList<>();
                            String accentity = null;
                            BizObject singleOrg = FIDubboUtils.getSingleOrg();
                            if(singleOrg!=null){
                                accentity = singleOrg.get("id");
                            }
                            if(StringUtil.isNotEmpty(accentity)){
                                accentitys.add(accentity);
                            }
                            if(GL_BANKRECONCILIATIONLIST.equals(billnum)){
                                filterCommonVO.setValue1(accentitys);
                            }else {
                                filterCommonVO.setValue1(accentity);
                            }
                            //账户共享：建投场景过滤条件去除所属组织；产品王东方
//                            if (CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)){
//                                commonVOs = ArrayUtils.remove(commonVOs,i);
//                                i--;
//                            }
                        }
                        accentityIsNoProcess = filterCommonVO.getValue1().toString();
                    }
                    if(BANKRECONCILIATIONSCHEME.equals(filterCommonVO.getItemName())){
                        //对账方案的过滤条件
                        Long id = Long.valueOf(filterCommonVO.getValue1().toString());
                        bankReconciliationSetting = MetaDaoHelper.findById(BankReconciliationSetting.ENTITY_NAME,id);
                        if(ValueUtils.isEmpty(bankReconciliationSetting)){
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100876"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000A3", "未查询到该对账方案相关信息,请检查!") /* "未查询到该对账方案相关信息,请检查!" */);
                        }
                        bankreconciliationscheme = id;
                        reconciliationdatasource = bankReconciliationSetting.get(RECONCILIATIONDATASOURCE);
                        if(CMP_BANKJOURNALLIST.equals(billnum)){
                            //查询日记账数据，增加过滤条件，不查汇兑损益生成的日记账
                            UiMetaDaoHelper.appendCondition(filterVO,BILLTYPE,ICmpConstant.QUERY_NEQ, EventType.ExchangeBill.getValue());
                            UiMetaDaoHelper.appendCondition(filterVO, ICmpConstant.SETTLE_STATUS, ICmpConstant.QUERY_EQ, SettleStatus.alreadySettled.getValue());
                        }
                        List<BizObject> list = bankReconciliationSetting.get(BANKRECONCILIATIONSETTING_B);

                        if(externalData.equals(CMP_BALANCEADJUST) || externalData.equals(CMP_CHECK)){
                            SimpleFilterVO filterOr = new SimpleFilterVO(ConditionOperator.or);
                            for(BizObject bankReconciliationSetting_b :list){
                                //202505 增加是否勾对=否时的停用明细过滤
                                if (bankReconciliationSetting_b.getShort("enableStatus_b") == EnableStatus.Disabled.getValue()
                                    && !BooleanUtils.toBoolean(checkflagValue)){
                                    continue;
                                }
                                SimpleFilterVO filterAnd = new SimpleFilterVO(ConditionOperator.and);
                                String bankaccount = bankReconciliationSetting_b.get(ICmpConstant.BANKACCOUNT);
                                String bankAccountCurrency = bankReconciliationSetting_b.get(ICmpConstant.CURRENCY);
                                filterAnd.addCondition(new SimpleFilterVO(ICmpConstant.BANKACCOUNT, ICmpConstant.QUERY_EQ, bankaccount));
                                filterAnd.addCondition(new SimpleFilterVO(ICmpConstant.CURRENCY, ICmpConstant.QUERY_EQ, bankAccountCurrency));
                                bankaccounts.add(bankaccount);
                                filterOr.addCondition(filterAnd);
                            }
                            //为0时代表没有可以查询的数据
                            if (bankaccounts.size() == 0 && !BooleanUtils.toBoolean(checkflagValue)) {
                                SimpleFilterVO filterAnd = new SimpleFilterVO(ConditionOperator.and);
                                filterAnd.addCondition(new SimpleFilterVO(ICmpConstant.BANKACCOUNT, ICmpConstant.QUERY_EQ, "0"));
                                filterAnd.addCondition(new SimpleFilterVO(ICmpConstant.CURRENCY, ICmpConstant.QUERY_EQ, "0"));
                                filterOr.addCondition(filterAnd);
                            }
                            filterVO.appendCondition(ConditionOperator.and, filterOr);
                        }else{
                            for(BizObject bankReconciliationSetting_b :list){
                                String bankaccount = bankReconciliationSetting_b.get(ICmpConstant.BANKACCOUNT);
                                bankaccounts.add(bankaccount);
                            }
                            UiMetaDaoHelper.appendCondition(filterVO, ICmpConstant.BANKACCOUNT, ICmpConstant.QUERY_IN, bankaccounts);
                        }



//                        UiMetaDaoHelper.appendCondition(filterVO, ICmpConstant.BANKACCOUNT, ICmpConstant.QUERY_IN, bankaccounts);
                        commonVOs =ArrayUtils.remove(commonVOs,i);
                        i--;
                    }
                    //单据日期
                    if(DZDATE.equals(filterCommonVO.getItemName())){
                        String enableDate = DateUtils.dateFormat(bankReconciliationSetting.get(ENABLEDATE),DateUtils.DATE_TIME_PATTERN);//启用日期
                        String dzDate =(String)filterCommonVO.getValue1();
                        if(CMP_BALANCEADJUST.equals(externalData)){
                            SimpleFilterVO filterOr = new SimpleFilterVO(ConditionOperator.or);
                            if(dzDate!=null){
                                if((CMP_BANKJOURNALLIST.equals(billnum)&&ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource)||CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)){
                                    SimpleFilterVO filterAnd1 = new SimpleFilterVO(ConditionOperator.and);
                                    filterAnd1.addCondition(new SimpleFilterVO(DZDATE, ICmpConstant.QUERY_ELT, dzDate));
                                    filterAnd1.addCondition(new SimpleFilterVO(DZDATE, ICmpConstant.QUERY_EGT, enableDate));
                                    filterAnd1.addCondition(new SimpleFilterVO(INIT_FLAG, ICmpConstant.QUERY_EQ, 0));

                                    SimpleFilterVO filterAnd2 = new SimpleFilterVO(ConditionOperator.and);
                                    filterAnd2.addCondition(new SimpleFilterVO(INIT_FLAG, ICmpConstant.QUERY_EQ, 1));
                                    filterAnd2.addCondition(new SimpleFilterVO(BANKRECONCILIATIONSCHEME, ICmpConstant.QUERY_EQ, bankreconciliationscheme));

                                    filterOr.addCondition(filterAnd1);
                                    filterOr.addCondition(filterAnd2);


                                    QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_ASC);
                                    queryOrderlyList.add(orders1);
                                }

                            }
                            filterVO.appendCondition(ConditionOperator.and, filterOr);
                        }else{
                            if(CMP_BANKJOURNALLIST.equals(billnum)){
                                if(externalData.equals(CMP_CHECK)) {
                                    appendQueryCond(filterVO, filterCommonVO, ICmpConstant.VOUCHDATE);
                                }else{
                                    if(ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                                        UiMetaDaoHelper.appendCondition(filterVO, DZDATE, ICmpConstant.QUERY_EGT, enableDate);
                                        UiMetaDaoHelper.appendCondition(filterVO, DZDATE, ICmpConstant.QUERY_ELT, dzDate);

                                        if(cmp_check_sort.equals(ORDER1)){
                                            QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_DESC);
                                            queryOrderlyList.add(orders1);
                                        }else if(cmp_check_sort.equals(ORDER2)){
                                            QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_ASC);
                                            queryOrderlyList.add(orders1);
                                        }
                                        else {
                                            QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_DESC);
                                            queryOrderlyList.add(orders1);
                                        }

                                    }
                                }
                            }else if(CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)){
                                if(externalData.equals(CMP_CHECK)) {
                                    appendQueryCond(filterVO, filterCommonVO, DZDATE);
                                }else{
                                    UiMetaDaoHelper.appendCondition(filterVO, DZDATE, ICmpConstant.QUERY_EGT, enableDate);
                                    UiMetaDaoHelper.appendCondition(filterVO, TRANSDATE, ICmpConstant.QUERY_ELT, dzDate);
                                    if(cmp_check_sort.equals(ORDER1)){
                                        QueryOrderby orders = new QueryOrderby(TRANSDATE,ICmpConstant.ORDER_DESC);
                                        queryOrderlyList.add(orders);
                                    }else if(cmp_check_sort.equals(ORDER2)){
                                        QueryOrderby orders = new QueryOrderby(TRANSDATE,ICmpConstant.ORDER_ASC);
                                        queryOrderlyList.add(orders);
                                    }else{
                                        QueryOrderby orders = new QueryOrderby(TRANSDATE,ICmpConstant.ORDER_DESC);
                                        queryOrderlyList.add(orders);
                                    }

                                }
                            }
                        }

                        commonVOs =ArrayUtils.remove(commonVOs,i);
                        i--;
                    }

                    //勾对日期
                    if(CHECKDATE.equals(filterCommonVO.getItemName())){
                        if(CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)&&ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                            appendQueryCond(filterVO, filterCommonVO, ORTHER_CHECKDATE);
                        }else{
                            appendQueryCond(filterVO, filterCommonVO, CHECKDATE);
                        }
                        commonVOs =ArrayUtils.remove(commonVOs,i);
                        i--;
                    }
                    //-----------------------------------银行对账新加单据日期查询条件 end-----------------------------------------------------------------------------------------------------------
                    if(CHECKFLAG.equals(filterCommonVO.getItemName())){
                        checkflagValue = String.valueOf(filterCommonVO.getValue1());
                    }
                    if(CHECKFLAG.equals(filterCommonVO.getItemName())&&CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)&&ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                        String checkflag = String.valueOf(filterCommonVO.getValue1());
                        UiMetaDaoHelper.appendCondition(filterVO, OTHER_CHECKFLAG, ICmpConstant.QUERY_EQ, checkflag);
                        commonVOs =ArrayUtils.remove(commonVOs,i);
                        i--;
                    }
                    if(CHECKFLAG.equals(filterCommonVO.getItemName())&&GL_BANKRECONCILIATIONLIST.equals(billnum)&&ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                        String checkflag = String.valueOf(filterCommonVO.getValue1());
                        UiMetaDaoHelper.appendCondition(filterVO, OTHER_CHECKFLAG, ICmpConstant.QUERY_EQ, checkflag);
                        commonVOs =ArrayUtils.remove(commonVOs,i);
                        i--;
                    }
                    if(DZENDDATE.equals(filterCommonVO.getItemName())){
                        SimpleFilterVO filterOr = new SimpleFilterVO(ConditionOperator.or);
                        Date enableDate = bankReconciliationSetting.getDate(ENABLEDATE); //启用日期
                        String dzEndDate =(String)filterCommonVO.getValue1();   //对账截止日期
                        if(dzEndDate!=null){
                            if((CMP_BANKJOURNALLIST.equals(billnum)&&ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource)||CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)){
                                //
                                SimpleFilterVO filterAnd1 = new SimpleFilterVO(ConditionOperator.and);
                                filterAnd1.addCondition(new SimpleFilterVO(DZDATE, ICmpConstant.QUERY_ELT, dzEndDate));
                                filterAnd1.addCondition(new SimpleFilterVO(DZDATE, ICmpConstant.QUERY_EGT, DateUtils.dateFormat(enableDate,"yyyy-MM-dd HH:mm:ss")));
                                filterAnd1.addCondition(new SimpleFilterVO(INIT_FLAG, ICmpConstant.QUERY_EQ, 0));

                                SimpleFilterVO filterAnd2 = new SimpleFilterVO(ConditionOperator.and);
                                filterAnd2.addCondition(new SimpleFilterVO(INIT_FLAG, ICmpConstant.QUERY_EQ, 1));
                                filterAnd2.addCondition(new SimpleFilterVO(BANKRECONCILIATIONSCHEME, ICmpConstant.QUERY_EQ, bankreconciliationscheme));

                                filterOr.addCondition(filterAnd1);
                                filterOr.addCondition(filterAnd2);

                                if(cmp_check_sort.equals(ORDER1)){
                                    QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_DESC);
                                    queryOrderlyList.add(orders1);
                                } else if (cmp_check_sort.equals(ORDER2)) {
                                    QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_ASC);
                                    queryOrderlyList.add(orders1);
                                }else {
                                    QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_DESC);
                                    queryOrderlyList.add(orders1);
                                }

                            }

                        }
                        filterVO.appendCondition(ConditionOperator.and, filterOr);
                        commonVOs =ArrayUtils.remove(commonVOs,i);
                        i--;
                    }
                    if(ISQC.equals(filterCommonVO.getItemName())){
                        String isQc =(String)filterCommonVO.getValue1();
                        if((CMP_BANKJOURNALLIST.equals(billnum)&&ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource)||CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)){
                            if(StringUtils.isNotEmpty(isQc)){
                                UiMetaDaoHelper.appendCondition(filterVO, INIT_FLAG, ICmpConstant.QUERY_EQ, 1);
                                UiMetaDaoHelper.appendCondition(filterVO, BANKRECONCILIATIONSCHEME, ICmpConstant.QUERY_EQ, bankreconciliationscheme);
                            }
                        }
                        commonVOs =ArrayUtils.remove(commonVOs,i);
                        i--;
                    }
                    //region 账户共享需求
                    //交易日期只对银行对账单有效
                    if ("tran_date".equals(filterCommonVO.getItemName())){
                        if((CMP_BANKJOURNALLIST.equals(billnum)&&ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource)){
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                        if(CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)){
                            String startDate = (String) filterCommonVO.getValue1();
                            String endDate = (String) filterCommonVO.getValue2();
                            if(StringUtils.isNotEmpty(startDate)){
                                UiMetaDaoHelper.appendCondition(filterVO,"dzdate", ICmpConstant.QUERY_EGT, startDate);
                            }
                            if(StringUtils.isNotEmpty(endDate)){
                                UiMetaDaoHelper.appendCondition(filterVO,"dzdate", ICmpConstant.QUERY_ELT, endDate);
                            }
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                    }
                    //业务日期 过滤日记账；对账单不过滤
                    //默认取银行日记账（取登账日期：对应系统日记账日期字段）\凭证（取制单日期：对应系统的日期字段）日期范围的业务数据
                    if ("makeTime".equals(filterCommonVO.getItemName())){
                        if(CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)){
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }else if (CMP_BANKJOURNALLIST.equals(billnum)&&ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                            SimpleFilterVO filterOr = new SimpleFilterVO(ConditionOperator.or);
                            Date enableDate = bankReconciliationSetting.getDate(ENABLEDATE); //启用日期
                            String startDate = (String) filterCommonVO.getValue1();
                            String endDate = (String) filterCommonVO.getValue2();
                            if(StringUtils.isNotEmpty(startDate)){
                                SimpleFilterVO filterAnd1 = new SimpleFilterVO(ConditionOperator.and);
                                if (StringUtils.isNotEmpty(endDate)){
                                    filterAnd1.addCondition(new SimpleFilterVO(DZDATE, ICmpConstant.QUERY_ELT, endDate));
                                }
                                filterAnd1.addCondition(new SimpleFilterVO(DZDATE, ICmpConstant.QUERY_EGT, startDate));
                                filterAnd1.addCondition(new SimpleFilterVO(DZDATE, ICmpConstant.QUERY_EGT, DateUtils.dateFormat(enableDate,"yyyy-MM-dd HH:mm:ss")));
                                filterAnd1.addCondition(new SimpleFilterVO(INIT_FLAG, ICmpConstant.QUERY_EQ, 0));

                                SimpleFilterVO filterAnd2 = new SimpleFilterVO(ConditionOperator.and);
                                filterAnd2.addCondition(new SimpleFilterVO(INIT_FLAG, ICmpConstant.QUERY_EQ, 1));
                                filterAnd2.addCondition(new SimpleFilterVO(BANKRECONCILIATIONSCHEME, ICmpConstant.QUERY_EQ, bankreconciliationscheme));

                                filterOr.addCondition(filterAnd1);
                                filterOr.addCondition(filterAnd2);
                                if(cmp_check_sort.equals(ORDER1)){
                                    QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_DESC);
                                    queryOrderlyList.add(orders1);
                                } else if (cmp_check_sort.equals(ORDER2)) {
                                    QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_DESC);
                                    queryOrderlyList.add(orders1);
                                }else {
                                    QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_DESC);
                                    queryOrderlyList.add(orders1);
                                }

                            }
                            if(StringUtils.isNotEmpty(endDate)){
                                SimpleFilterVO filterAnd1 = new SimpleFilterVO(ConditionOperator.and);
                                if (StringUtils.isNotEmpty(startDate)){
                                    filterAnd1.addCondition(new SimpleFilterVO(DZDATE, ICmpConstant.QUERY_EGT, startDate));
                                }
                                filterAnd1.addCondition(new SimpleFilterVO(DZDATE, ICmpConstant.QUERY_ELT, endDate));
                                filterAnd1.addCondition(new SimpleFilterVO(DZDATE, ICmpConstant.QUERY_EGT, DateUtils.dateFormat(enableDate,"yyyy-MM-dd HH:mm:ss")));
                                filterAnd1.addCondition(new SimpleFilterVO(INIT_FLAG, ICmpConstant.QUERY_EQ, 0));

                                SimpleFilterVO filterAnd2 = new SimpleFilterVO(ConditionOperator.and);
                                filterAnd2.addCondition(new SimpleFilterVO(INIT_FLAG, ICmpConstant.QUERY_EQ, 1));
                                filterAnd2.addCondition(new SimpleFilterVO(BANKRECONCILIATIONSCHEME, ICmpConstant.QUERY_EQ, bankreconciliationscheme));

                                filterOr.addCondition(filterAnd1);
                                filterOr.addCondition(filterAnd2);
                                if(cmp_check_sort.equals(ORDER1)){
                                    QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_DESC);
                                    queryOrderlyList.add(orders1);
                                } else if (cmp_check_sort.equals(ORDER2)) {
                                    QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_ASC);
                                    queryOrderlyList.add(orders1);
                                }else{
                                    QueryOrderby orders1 = new QueryOrderby(DZDATE,ICmpConstant.ORDER_DESC);
                                    queryOrderlyList.add(orders1);
                                }

                            }
                            filterVO.appendCondition(ConditionOperator.and, filterOr);
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                    }
                    //金额
                    if ("tranAmt".equals(filterCommonVO.getItemName())){
                        if (CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)){
                            Object minAmt = filterCommonVO.getValue1();
                            Object maxAmt = filterCommonVO.getValue2();
                            //借方金额
                            SimpleFilterVO debitFilter = new SimpleFilterVO(ConditionOperator.and);
                            SimpleFilterVO creditFilter = new SimpleFilterVO(ConditionOperator.and);
                            if (minAmt != null){
                                debitFilter.addCondition(new SimpleFilterVO("debitamount", ICmpConstant.QUERY_EGT, minAmt));
                                creditFilter.addCondition(new SimpleFilterVO("creditamount", ICmpConstant.QUERY_EGT, minAmt));
                            }
                            if (maxAmt != null){
                                debitFilter.addCondition(new SimpleFilterVO("debitamount", ICmpConstant.QUERY_ELT, maxAmt));
                                creditFilter.addCondition(new SimpleFilterVO("creditamount", ICmpConstant.QUERY_ELT, maxAmt));
                            }
                            filterVO.appendCondition(ConditionOperator.or, new SimpleFilterVO[]{debitFilter, creditFilter});

                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                        if (CMP_BANKJOURNALLIST.equals(billnum)&&ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                            Object minAmt = filterCommonVO.getValue1();
                            Object maxAmt = filterCommonVO.getValue2();
                            //借方金额
                            SimpleFilterVO debitFilter = new SimpleFilterVO(ConditionOperator.and);
                            SimpleFilterVO creditFilter = new SimpleFilterVO(ConditionOperator.and);
                            if (minAmt != null || maxAmt != null){
                                if (minAmt != null){
                                    debitFilter.addCondition(new SimpleFilterVO("debitoriSum", ICmpConstant.QUERY_EGT, minAmt));
                                    creditFilter.addCondition(new SimpleFilterVO("creditoriSum", ICmpConstant.QUERY_EGT, minAmt));
                                }else {
                                    debitFilter.addCondition(new SimpleFilterVO("debitoriSum", ICmpConstant.QUERY_NEQ, "0"));
                                    creditFilter.addCondition(new SimpleFilterVO("creditoriSum", ICmpConstant.QUERY_NEQ, "0"));
                                }
                                if (maxAmt != null){
                                    debitFilter.addCondition(new SimpleFilterVO("debitoriSum", ICmpConstant.QUERY_ELT, maxAmt));
                                    creditFilter.addCondition(new SimpleFilterVO("creditoriSum", ICmpConstant.QUERY_ELT, maxAmt));
                                }else {
                                    debitFilter.addCondition(new SimpleFilterVO("debitoriSum", ICmpConstant.QUERY_NEQ, "0"));
                                    creditFilter.addCondition(new SimpleFilterVO("creditoriSum", ICmpConstant.QUERY_NEQ, "0"));
                                }
                            }
                            filterVO.appendCondition(ConditionOperator.or, new SimpleFilterVO[]{debitFilter, creditFilter});

                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                    }

                    //勾对号
                    if ("checkNo".equals(filterCommonVO.getItemName())){
                        String checkno = String.valueOf(filterCommonVO.getValue1());
                        if (CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum) && ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                            UiMetaDaoHelper.appendCondition(filterVO, "other_checkno", ICmpConstant.QUERY_EQ, checkno);
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                        if (CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum) && ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                            UiMetaDaoHelper.appendCondition(filterVO, "checkno", ICmpConstant.QUERY_EQ, checkno);
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                        if (CMP_BANKJOURNALLIST.equals(billnum)&&ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                            UiMetaDaoHelper.appendCondition(filterVO, "checkno", ICmpConstant.QUERY_EQ, checkno);
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                    }

                    //勾对时间
                    if ("checktime".equals(filterCommonVO.getItemName())){
                        String startDate = (String) filterCommonVO.getValue1();
                        String endDate = (String) filterCommonVO.getValue2();
                        if (CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum) && ReconciliationDataSource.Voucher.getValue()==reconciliationdatasource){
                            if(StringUtils.isNotEmpty(startDate)){
                                UiMetaDaoHelper.appendCondition(filterVO,"other_checktime", ICmpConstant.QUERY_EGT, startDate);
                            }
                            if(StringUtils.isNotEmpty(endDate)){
                                UiMetaDaoHelper.appendCondition(filterVO,"other_checktime", ICmpConstant.QUERY_ELT, endDate);
                            }
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                        if (CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum) && ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                            if(StringUtils.isNotEmpty(startDate)){
                                UiMetaDaoHelper.appendCondition(filterVO,"checktime", ICmpConstant.QUERY_EGT, startDate);
                            }
                            if(StringUtils.isNotEmpty(endDate)){
                                UiMetaDaoHelper.appendCondition(filterVO,"checktime", ICmpConstant.QUERY_ELT, endDate);
                            }
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                        if (CMP_BANKJOURNALLIST.equals(billnum)&&ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                            if(StringUtils.isNotEmpty(startDate)){
                                UiMetaDaoHelper.appendCondition(filterVO,"checktime", ICmpConstant.QUERY_EGT, startDate);
                            }
                            if(StringUtils.isNotEmpty(endDate)){
                                UiMetaDaoHelper.appendCondition(filterVO,"checktime", ICmpConstant.QUERY_ELT, endDate);
                            }
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                    }

                    //对账账簿，传凭证的
                    if ("accbookId".equals(filterCommonVO.getItemName())){
                        commonVOs = ArrayUtils.remove(commonVOs,i);
                        i--;
                    }
                    //授权使用组织
                    if ("useorg".equals(filterCommonVO.getItemName())){
                        if (CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)){
                            Object useorg = filterCommonVO.getValue1();
                            UiMetaDaoHelper.appendCondition(filterVO, "accentity", ICmpConstant.QUERY_EQ, useorg);
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                        if (CMP_BANKJOURNALLIST.equals(billnum)&&ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                            Object useorg = filterCommonVO.getValue1();
                            UiMetaDaoHelper.appendCondition(filterVO, "accentity", ICmpConstant.QUERY_EQ, useorg);
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                            isContainUseorg = true;
                        }
                    }
                    //账户共享 所属组织 银行对账单=orgid 银行日记账=parentAccentity
                    if(ACCENTITY.equals(filterCommonVO.getItemName())){
                        if (CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)){
//                            Object orgid = filterCommonVO.getValue1();
//                            UiMetaDaoHelper.appendCondition(filterVO, "orgid", ICmpConstant.QUERY_EQ, orgid);
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                        if (CMP_BANKJOURNALLIST.equals(billnum)&&ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                            //202407 日记账只根据授权使用组织 + 账户币种
//                            Object orgid = filterCommonVO.getValue1();
//                            UiMetaDaoHelper.appendCondition(filterVO, "parentAccentity", ICmpConstant.QUERY_EQ, orgid);
                            commonVOs = ArrayUtils.remove(commonVOs,i);
                            i--;
                        }
                    }
                    //endregion

                }
                if (BooleanUtils.toBoolean(checkflagValue)){
                    if (CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)){
                        queryOrderlyList.clear();
                        if(cmp_check_sort.equals(ORDER1)){
                            QueryOrderby orders = new QueryOrderby(TRANSDATE,ICmpConstant.ORDER_DESC);
                            queryOrderlyList.add(orders);
                        }else if (cmp_check_sort.equals(ORDER2)){
                            QueryOrderby orders = new QueryOrderby(TRANSDATE,ICmpConstant.ORDER_ASC);
                            queryOrderlyList.add(orders);
                        }else {
                            QueryOrderby orders = new QueryOrderby(TRANSDATE,ICmpConstant.ORDER_DESC);
                            queryOrderlyList.add(orders);
                        }

                        //已勾兑要添加对应的对账方案id
                        if(ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource){
                            UiMetaDaoHelper.appendCondition(filterVO, "bankreconciliationsettingid", ICmpConstant.QUERY_EQ, bankReconciliationSetting.getId());
                        }else {
                            UiMetaDaoHelper.appendCondition(filterVO, "gl_bankreconciliationsettingid", ICmpConstant.QUERY_EQ, bankReconciliationSetting.getId());
                        }
                    } else if (CMP_BANKJOURNALLIST.equals(billnum)){
                        queryOrderlyList.clear();
                        if(cmp_check_sort.equals(ORDER1)){
                            QueryOrderby orders = new QueryOrderby(VOUCHDATE,ICmpConstant.ORDER_DESC);
                            queryOrderlyList.add(orders);
                        }else if (cmp_check_sort.equals(ORDER2)){
                            QueryOrderby orders = new QueryOrderby(VOUCHDATE,ICmpConstant.ORDER_ASC);
                            queryOrderlyList.add(orders);
                        }else {
                            QueryOrderby orders = new QueryOrderby(VOUCHDATE,ICmpConstant.ORDER_DESC);
                            queryOrderlyList.add(orders);
                        }

                        //已勾兑要添加对应的对账方案id
                        UiMetaDaoHelper.appendCondition(filterVO, "bankreconciliationsettingid", ICmpConstant.QUERY_EQ, bankReconciliationSetting.getId());
                    }
                }
                billDataDto.setQueryOrders(queryOrderlyList);

                //已停用方案，后台查询报错
                if (externalData.equals(CMP_CHECK)){
                    if (!BooleanUtils.toBoolean(checkflagValue) && bankReconciliationSetting.getShort("enableStatus") == EnableStatus.Disabled.getValue()){
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100881"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1B544EF405A00022","对账方案已停用，只可查询已勾对数据！") /* "对账方案已停用，只可查询已勾对数据！" */);
                    }
                }

                //账户共享
                //银行日记账在勾对=否是只过滤未停用对账明细；勾对=是不做处理
                if (!isContainUseorg && CMP_BANKJOURNALLIST.equals(billnum)&&ReconciliationDataSource.BankJournal.getValue()==reconciliationdatasource
                && !BooleanUtils.toBoolean(checkflagValue) ){
                    SimpleFilterVO filterOr = new SimpleFilterVO(ConditionOperator.or);
                    for (BankReconciliationSettingVO settingVO : infoList){
                        if (settingVO.getEnableStatus() == EnableStatus.Disabled.getValue()){
                            continue;
                    }
                        SimpleFilterVO filterAnd = new SimpleFilterVO(ConditionOperator.and);
                        String bankaccount = settingVO.getBankAccount();
                        String bankAccountCurrency = settingVO.getCurrency();
                        filterAnd.addCondition(new SimpleFilterVO(ICmpConstant.BANKACCOUNT, ICmpConstant.QUERY_EQ, bankaccount));
                        filterAnd.addCondition(new SimpleFilterVO(ICmpConstant.CURRENCY, ICmpConstant.QUERY_EQ, bankAccountCurrency));
                        filterAnd.addCondition(new SimpleFilterVO("accentity", ICmpConstant.QUERY_EQ, settingVO.getUseOrg()));
                        filterOr.addCondition(filterAnd);
                    }
                    filterVO.appendCondition(ConditionOperator.and, filterOr);
                }

                if (CMP_BANKRECONCILIATIONLISTCHECK.equals(billnum)) {
                    List<String> accentityList = AppContext.getBean(AutoConfigService.class).getAccentityListNoProecss();
                    if (CollectionUtils.isNotEmpty(accentityList)) {
                        List<String> bankreconciliationIds = BankreconciliationUtils.getNoProcessDatasByAccentitys(accentityList);
                        //组织参数 无需处理的流水，是否参与银企对账、银行账户余额弥 为否，需要把无需处理的银行流水过滤掉
                        if (CollectionUtils.isNotEmpty(bankreconciliationIds)) {
                            UiMetaDaoHelper.appendCondition(filterVO, "id", ICmpConstant.QUERY_NOTIN, bankreconciliationIds);
                        }
                    }
                }

                //银行流水对账银行账户数据权限适配
                String[] bankAccountPermissions = cmpCheckService.getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
                if(bankAccountPermissions != null && bankAccountPermissions.length > 0){
                    UiMetaDaoHelper.appendCondition(filterVO, ICmpConstant.BANKACCOUNT, ICmpConstant.QUERY_IN, Arrays.asList(bankAccountPermissions));
                }
            }
            filterVO.setCommonVOs(commonVOs);
        }

        billDataDto.setCondition(filterVO);
        putParam(paramMap, billDataDto);
        return new RuleExecuteResult();
    }

    /**
     * 添加查询条件的方法
     * @param filterVO
     * @param filterCommonVO
     */
    private void appendQueryCond(FilterVO filterVO, FilterCommonVO filterCommonVO,String cond) {
        String startDate = (String) filterCommonVO.getValue1();
        String endDate = (String) filterCommonVO.getValue2();
        if(StringUtils.isNotEmpty(startDate)){
            UiMetaDaoHelper.appendCondition(filterVO, cond, ICmpConstant.QUERY_EGT, startDate);
        }
        if(StringUtils.isNotEmpty(endDate)){
            UiMetaDaoHelper.appendCondition(filterVO, cond, ICmpConstant.QUERY_ELT, endDate);
        }
    }

}
