package com.yonyoucloud.fi.cmp.initdata.rule;

import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.bd.period.Period;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateUtils;
import com.yonyoucloud.fi.cmp.common.service.exchangerate.CmpExchangeRateVO;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.initdata.InitData;
import com.yonyoucloud.fi.cmp.initdata.service.InitDataService;
import com.yonyoucloud.fi.cmp.util.*;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by Administrator on 2019/4/16 0016.
 */
@Slf4j
@Component
public abstract class InitDataSaveRule extends AbstractCommonRule {

    public abstract void initData(BizObject bizObject);
    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Autowired
    YmsOidGenerator ymsOidGenerator;
    @Autowired
    InitDataService initDataService;
    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        RuleExecuteResult result = new RuleExecuteResult();
        if (bills != null && bills.size()>0) {
            for (BizObject bill : bills) {
                InitData bizObject =  (InitData) bill;
                InitData initDataOld = new InitData();
                if(bizObject.getId()!=null){
                    initDataOld = MetaDaoHelper.findById(InitData.ENTITY_NAME,bizObject.getId());
                }
                if(initDataOld==null){
                    initDataOld = new InitData();
                }
                //公共校验(前校验)
                excuteCommonRuleBefore(bizObject, initDataOld);
                //校验银行账户不能重复
                //导入
                BillDataDto billDataDto = (BillDataDto) getParam(map);
                //导入
                boolean importFlag =  "import".equals(billDataDto.getRequestAction());
                // OpenApi
                boolean openApiFlag = bizObject.containsKey("_fromApi") && bizObject.get("_fromApi").equals(true);
                boolean fromApi = billDataDto.getFromApi();
                if (importFlag || openApiFlag || fromApi) {
                    excuteFromApi(bizObject, initDataOld, result);
                }else{
                    //页面逻辑处理
                    excuteFromPage(bizObject, initDataOld);
                }
                //公共校验(后校验)
                excuteCommonRuleAfter(bizObject, initDataOld);
            }
        }
        return result;
    }

    //处理导入相关逻辑
    private void excuteFromPage(InitData bizObject,InitData initDataOld) throws Exception {
        QuerySchema queryRepeatSchema = QuerySchema.create().addSelect("1");
        QueryConditionGroup groupRepeat = null;
        if(ValueUtils.isNotEmptyObj(bizObject.get("bankaccount"))){
            groupRepeat = QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(bizObject.get("bankaccount")));
            groupRepeat.addCondition(QueryCondition.name("currency").eq(bizObject.get("currency")));
            groupRepeat.addCondition(QueryCondition.name("accentity").eq(bizObject.get("accentity")));
        }
        if(ValueUtils.isNotEmptyObj(bizObject.get("cashaccount"))){
            groupRepeat = QueryConditionGroup.and(QueryCondition.name("cashaccount").eq(bizObject.get("cashaccount")));
            groupRepeat.addCondition(QueryCondition.name("accentity").eq(bizObject.get("accentity")));
        }
        if(ValueUtils.isNotEmptyObj(groupRepeat)){
            queryRepeatSchema.addCondition(groupRepeat);
            List<InitData> initDataList = MetaDaoHelper.query(InitData.ENTITY_NAME, queryRepeatSchema);
            if (ValueUtils.isNotEmpty(initDataList) && initDataList.size() > 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102129"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180493","银行账户已存在，不能重复添加！") /* "银行账户已存在，不能重复添加！" */);
            }
        }
//        initDataOld = MetaDaoHelper.findById(InitData.ENTITY_NAME,bizObject.getId());
        this.initData(bizObject);
        try {
            if(FIDubboUtils.isSingleOrg()){
                BizObject singleOrg = FIDubboUtils.getSingleOrg();
                if(singleOrg != null){
                    bizObject.set(IBussinessConstant.ACCENTITY,singleOrg.get("id"));
                    bizObject.set("accentity_name",singleOrg.get("name"));
                }
            }
        } catch (Exception e) {
            log.error("单组织判断异常!", e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102130"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180490","单组织判断异常！") /* "单组织判断异常！" */ + e.getMessage());
        }
        if(bizObject.get("currency") != null){
            CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bizObject.get("currency"));
            if (bizObject.get("coinitlocalbalance") == null || bizObject.get("coinitloribalance") == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102131"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F353405600006", "金额不能为空") /* "金额不能为空" */);
            }
            if(currencyDTO != null){
                bizObject.setCoinitloribalance(bizObject.getCoinitloribalance().setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount()));
            }
        }
        String currency = AccentityUtil.getNatCurrencyIdByAccentityId(bizObject.get(IBussinessConstant.ACCENTITY));
        if(currency != null){
            CurrencyTenantDTO currencyOrgDTO = baseRefRpcService.queryCurrencyById(currency);
            if(currencyOrgDTO != null) {
                // 如果前端编辑了本币余额，则以前端输入的为准
                if (bizObject.getCoinitloribalance() != null && bizObject.getCobooklocalbalance() == null) {
                    bizObject.setCoinitlocalbalance(bizObject.getCoinitloribalance().multiply(bizObject.getExchangerate()).setScale(currencyOrgDTO.getMoneydigit(),currencyOrgDTO.getMoneyrount()));
                }
            }
        }
    }

    private void excuteFromApi(InitData bizObject,InitData initDataOld,RuleExecuteResult result ) throws Exception {
        QuerySchema queryRepeatSchema = QuerySchema.create().addSelect("*");
        QueryConditionGroup groupRepeat = null;
        if(ValueUtils.isNotEmptyObj(bizObject.get("bankaccount"))){
            groupRepeat = QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(bizObject.get("bankaccount")));
            groupRepeat.addCondition(QueryCondition.name("currency").eq(bizObject.get("currency")));
            groupRepeat.addCondition(QueryCondition.name("accentity").eq(bizObject.get("accentity")));
        }
        if(ValueUtils.isNotEmptyObj(groupRepeat)){
            queryRepeatSchema.addCondition(groupRepeat);
            List<InitData> initDataList = MetaDaoHelper.query(InitData.ENTITY_NAME, queryRepeatSchema);
            if (!ValueUtils.isNotEmpty(initDataList)) {
//                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102132"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418048F","当前银行账户不存在！") /* "当前银行账户不存在！" */);
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102133"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1CB5A19A04380000","银行账户尚未同步余额，请先进行账户同步！！"
                        /* "银行账户尚未同步余额，请先进行账户同步！！" */));
            }
            if (initDataList.size() > 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102129"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180493","银行账户已存在，不能重复添加！") /* "银行账户已存在，不能重复添加！" */);
            }
            initDataOld.init(initDataList.get(0));
            if (initDataOld == null) {
                initDataOld = new InitData();
            }
        }
        if(null == initDataOld.getCoinitloribalance()){
            initDataOld.setCoinitloribalance(BigDecimal.ZERO);
        }

        if(null == initDataOld.getBankinitoribalance()){
            initDataOld.setBankinitoribalance(BigDecimal.ZERO);
        }

        if(null == initDataOld.getCobookoribalance()){
            initDataOld.setCobookoribalance(BigDecimal.ZERO);
        }

        if (ValueUtils.isNotEmptyObj(bizObject.get("coinitloribalance"))) {
            CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bizObject.get("currency"));
            if(currencyDTO != null ){
                bizObject.setCoinitloribalance(bizObject.getCoinitloribalance().setScale(currencyDTO.getMoneydigit(),currencyDTO.getMoneyrount()));
            }
        }
        String currency = AccentityUtil.getNatCurrencyIdByAccentityId(bizObject.get(IBussinessConstant.ACCENTITY));
        CurrencyTenantDTO currencyOrgDTO = baseRefRpcService.queryCurrencyById(currency);
        BigDecimal currencyRateNew = getExchangeRate(bizObject, initDataOld);;
        if(currency != null && ValueUtils.isNotEmptyObj(bizObject.get("coinitloribalance"))){
            if(currencyOrgDTO != null){
                BigDecimal coinitloribalance = bizObject.get("coinitloribalance");
                BigDecimal coinitlocalbalance = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(initDataOld.getExchRateOps(), currencyRateNew, coinitloribalance, currencyOrgDTO.getMoneydigit());
                bizObject.setCoinitlocalbalance(coinitlocalbalance.setScale(currencyOrgDTO.getMoneydigit(), currencyOrgDTO.getMoneyrount()));
                coinitloribalance.setScale(currencyOrgDTO.getMoneydigit(), currencyOrgDTO.getMoneyrount());
            }
        }
        if(currency != null && ValueUtils.isNotEmptyObj(bizObject.get("bankinitoribalance"))){
            if(currencyOrgDTO != null){
                BigDecimal bankinitoribalance = bizObject.get("bankinitoribalance");
                BigDecimal bankinitlocalbalance = CmpExchangeRateUtils.getExchangeRateAndAmountCalResult(initDataOld.getExchRateOps(), currencyRateNew, bankinitoribalance, currencyOrgDTO.getMoneydigit());
                bizObject.setBankinitlocalbalance(bankinitlocalbalance.setScale(currencyOrgDTO.getMoneydigit(), currencyOrgDTO.getMoneyrount()));
                bankinitoribalance.setScale(currencyOrgDTO.getMoneydigit(), currencyOrgDTO.getMoneyrount());
            }
        }
        if (bizObject.getBigDecimal("bankinitlocalbalance") == null || bizObject.get("coinitlocalbalance") == null || bizObject.getBigDecimal("bankinitoribalance") == null || bizObject.get("coinitloribalance") == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-102131"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_187F353405600006", "金额不能为空") /* "金额不能为空" */);
        }

        InitData initData = new InitData();
        if (initDataOld == null || initDataOld.getId() == null) {
            initData.setId(ymsOidGenerator.nextId());
        } else {
            initData.setId(initDataOld.getId());
        }
        if(ValueUtils.isNotEmptyObj(bizObject.get("coinitloribalance"))){
            initData.setCoinitloribalance(bizObject.getCoinitloribalance());
            initData.setCoinitlocalbalance(bizObject.getCoinitlocalbalance());
            //重算账面金额 导入期初 和 老期初做差后 加上原来的余额
            initData.setCobookoribalance(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(bizObject.getCoinitloribalance(),
                    initDataOld.getCoinitloribalance()!=null?initDataOld.getCoinitloribalance():BigDecimal.ZERO), initDataOld.getCobookoribalance()!=null?initDataOld.getCobookoribalance():BigDecimal.ZERO));
            initData.setCobooklocalbalance(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(bizObject.getCoinitlocalbalance(),
                    initDataOld.getCoinitlocalbalance()!=null?initDataOld.getCoinitlocalbalance():BigDecimal.ZERO), initDataOld.getCobooklocalbalance()!=null?initDataOld.getCobooklocalbalance():BigDecimal.ZERO));
        }
        if(ValueUtils.isNotEmptyObj(bizObject.get("bankinitoribalance"))){
            initData.setBankinitoribalance(bizObject.getBankinitoribalance());
        }
        if(ValueUtils.isNotEmptyObj(bizObject.get("bankdirection"))){
            initData.setBankdirection(bizObject.getBankdirection());
        }
        if(ValueUtils.isNotEmptyObj(bizObject.get("direction"))){
            initData.setDirection(bizObject.getDirection());
        }
        if(ValueUtils.isNotEmptyObj(bizObject.get("exchangerate"))){
            initData.setExchangerate(bizObject.get("exchangerate"));
        } else {
            initData.setExchangerate(currencyRateNew);
        }
        if(ValueUtils.isNotEmptyObj(bizObject.get("description"))){
            initData.setDescription(bizObject.get("description"));
        }
        if(ValueUtils.isNotEmptyObj(bizObject.get("overdraftCtrl"))){
            initData.setOverdraftCtrl(bizObject.getOverdraftCtrl());
        }
        EntityTool.setUpdateStatus(initData);
        MetaDaoHelper.update(InitData.ENTITY_NAME,initData);
        result.setCancel(true);
    }

    /**
     * 获取汇率
     * @param bizObject
     * @param initDataOld
     * @return
     * @throws Exception
     */
    private BigDecimal getExchangeRate(InitData bizObject, InitData initDataOld) throws Exception {
        BigDecimal currencyRateNew = bizObject.get("exchangerate") == null ? initDataOld.getExchangerate() : bizObject.get("exchangerate") ;
        if (currencyRateNew == null) {
            // 重新寻汇
            CmpExchangeRateVO cmpExchangeRateVO = CmpExchangeRateUtils.getNewExchangeRateWithMode(initDataOld.getCurrency(), initDataOld.getNatCurrency(), initDataOld.getAccountdate(), initDataOld.getExchangeRateType());
            currencyRateNew = cmpExchangeRateVO.getExchangeRate();
        }
        return currencyRateNew;
    }
    //处理页面相关逻辑

    //公共赋值校验 前规则
    private void excuteCommonRuleBefore(InitData bizObject,InitData initDataOld) throws Exception {
        // 汇率非必输，赋默认值为1
        if(bizObject.get("exchangerate") == null){
            if (initDataOld.getExchangerate() != null ) {
                bizObject.set("exchangerate",initDataOld.getExchangerate());
            }
        }
        //如果当前环境为单组织，则赋值当前组织
        if(bizObject.InitDatab()!=null && FIDubboUtils.isSingleOrg()){
            bizObject.InitDatab().get(0).setAccentity(FIDubboUtils.getSingleOrg().get("id"));
        }
        if (bizObject.get("accountdate") == null) {
            Date begindate = new Date();
            String orgId = bizObject.get(IBussinessConstant.ACCENTITY);
            List<String> orgIdList = new ArrayList<>();
            orgIdList.add(orgId);
            Map<String, Period> periodMap = initDataService.queryListFinanceOrg(orgIdList);
            checkPeriodMap(periodMap, orgId);
            if (bizObject.getBankaccount() != null) {//银行期初
                initDataService.initAccountDate(periodMap, orgId, bizObject, enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bizObject.getBankaccount()));
                if(periodMap.get(orgId).getBegindate()!=null){
                    begindate = periodMap.get(orgId).getBegindate();
                }
            } else {//现金期初
                initDataService.initCashAccountDate(periodMap, orgId, bizObject, QueryBaseDocUtils.queryCashBankAccountById(bizObject.getCashaccount()));
                if (periodMap.get(orgId).getBegindate() != null) {
                    begindate = periodMap.get(orgId).getBegindate();
                }
            }
            bizObject.set("period",QueryBaseDocUtils.queryPeriodIdByAccbodyAndDate(bizObject.get(IBussinessConstant.ACCENTITY),begindate));/* 暂不修改 内部调用财务公共接口 需要财务公共修改*/
        }
    }

    /**
     * 校验业务单元的期初期间
     * @param periodMap
     * @param orgId
     * @throws Exception
     */
    private void checkPeriodMap(Map<String, Period> periodMap, String orgId) throws Exception {
        if (periodMap.get(orgId) == null) {
            List<Map<String, Object>> accentityObj = QueryBaseDocUtils.getOrgMVByIds(Collections.singletonList(orgId));
            Object name = accentityObj !=null?accentityObj.get(0).get("name"):"";
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101482"),
                    String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1F474F8404080003", "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！") /* "未能获取到组织【%s】现金管理模块的期初日期，请到业务单元节点，通过“期初设置”功能维护现金管理期初期间！" */, name));
        }
    }


    //公共赋值校验 后规则
    private void excuteCommonRuleAfter(InitData bizObject,InitData initDataOld) throws Exception {
        if(ValueUtils.isNotEmptyObj(bizObject.get("coinitloribalance"))){
            //重算账面金额
            bizObject.setCobookoribalance(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(bizObject.getCobookoribalance(),initDataOld.getCoinitloribalance()), bizObject.getCoinitloribalance()));
            bizObject.setCobooklocalbalance(BigDecimalUtils.safeAdd(BigDecimalUtils.safeSubtract(bizObject.getCobooklocalbalance(),initDataOld.getCoinitlocalbalance()), bizObject.getCoinitlocalbalance()));
        }
    }

}
