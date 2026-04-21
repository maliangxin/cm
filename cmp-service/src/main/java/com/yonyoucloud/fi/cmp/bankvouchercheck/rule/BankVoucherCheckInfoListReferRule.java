package com.yonyoucloud.fi.cmp.bankvouchercheck.rule;

import com.google.common.collect.Lists;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.common.utils.MddBaseUtils;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.bankreconciliationsetting.BankReconciliationSetting_b;
import com.yonyoucloud.fi.cmp.bankvouchercheck.service.BankVoucherCheckService;
import com.yonyoucloud.fi.cmp.bankvouchercheck.vo.BankAccountInfoQueryVO;
import com.yonyoucloud.fi.cmp.cmpcheck.service.CmpCheckService;
import com.yonyoucloud.fi.cmp.cmpentity.EnableStatus;
import com.yonyoucloud.fi.cmp.cmpentity.ReconciliationDataSource;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @description: 银企对账工作台，对账概览列表参照过滤规则
 * v1:20250331 增加凭证数据源与银行流水对账时的参照过滤逻辑
 * v2:20250513 增加银行日记账与银行流水对账时的参照过滤逻辑
 * @author: wanxbo@yonyou.com
 * @date: 2025/2/13 16:48
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class BankVoucherCheckInfoListReferRule extends AbstractCommonRule {

    @Autowired
    private BankVoucherCheckService bankVoucherCheckService;
    @Autowired
    private CmpCheckService cmpCheckService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        String billnum = billContext.getBillnum();
        BillDataDto bill = (BillDataDto) getParam(paramMap);
        //cmp_bankvourhcercheck_infolist凭证对账，cmp_bankjournalcheck_infolist银行日记账对账
        short reconciliationDataSource = ReconciliationDataSource.Voucher.getValue();
        if (!("cmp_bankvourhcercheck_infolist".equals(billnum) || "cmp_bankjournalcheck_infolist".equals(billnum) || "cmp_bankvourhcercheck_quickreconciliation".equals(billnum))) {
            return new RuleExecuteResult();
        }
        if ("cmp_bankjournalcheck_infolist".equals(billnum)) {
            reconciliationDataSource = ReconciliationDataSource.BankJournal.getValue();
        }
        if ("filter".equals(bill.getExternalData())) {
            List<String> accentityList;
            if (StringUtils.isNotEmpty(bill.getMasterOrgValue())) {
                String[] accentitys = bill.getMasterOrgValue().split(",");
                accentityList = Lists.newArrayList(accentitys);
            } else {
                //获取凭证数据源的对账方案组织id集合
                accentityList = bankVoucherCheckService.getReconciliationSchemeAccentityList(reconciliationDataSource);
                if (accentityList.size() == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105053"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_220F3BB005000005", "缺少已启用的对账方案，请先配置对应数据！") /* "缺少已启用的对账方案，请先配置对应数据！" */);
                }
            }
            FilterVO filterVO = bill.getCondition();
            if (filterVO == null) {
                filterVO = new FilterVO();
            }
            //对账概览，对账组织过滤逻辑
            if (IRefCodeConstant.FUNDS_ORGTREE.equals(bill.getRefCode())) {
                //对账组织需要获取全部的数据
                accentityList = bankVoucherCheckService.getReconciliationSchemeAccentityList(reconciliationDataSource);
                //组织合集为0，则不展示
                if (accentityList.size() == 0) {
                    accentityList.add("0");
                }
                bill.getTreeCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "in", accentityList));
            }

            //银行账户过滤逻辑
            if ("ucfbasedoc.bd_enterprisebankacct".equals(bill.getRefCode())) {
                BankAccountInfoQueryVO queryVO = new BankAccountInfoQueryVO();
                queryVO.setAccentityList(accentityList);
                SimpleFilterVO[] filterVOSimpleVOs = filterVO.getSimpleVOs();
                if (filterVOSimpleVOs != null) {
                    //银行类别
                    SimpleFilterVO bank = Arrays.stream(filterVOSimpleVOs)
                            .filter(item -> "bank".equals(item.getField())).findFirst().orElse(null);
                    if (bank != null) {
                        CtmJSONArray bankArray = CtmJSONArray.parseArray(bank.getValue1().toString());
                        Long[] banktypes = bankArray.toArray(new Long[0]);
                        queryVO.setBanktypeList(Lists.newArrayList(Arrays.stream(banktypes)
                                .map(String::valueOf)
                                .toArray(String[]::new)));
                    }
                    //币种
                    SimpleFilterVO currency = Arrays.stream(filterVOSimpleVOs)
                            .filter(item -> "currency".equals(item.getField())).findFirst().orElse(null);
                    if (currency != null) {
                        CtmJSONArray currencyArray = CtmJSONArray.parseArray(currency.getValue1().toString());
                        Long[] currencys = currencyArray.toArray(new Long[0]);
                        queryVO.setCurrencyList(Lists.newArrayList(Lists.newArrayList(Arrays.stream(currencys)
                                .map(String::valueOf)
                                .toArray(String[]::new))));
                    }
                    //对账方案
                    SimpleFilterVO reconciliation_scheme = Arrays.stream(filterVOSimpleVOs)
                            .filter(item -> "reconciliation_scheme".equals(item.getField())).findFirst().orElse(null);
                    if (reconciliation_scheme != null) {
                        CtmJSONArray reconciliation_schemeArray = CtmJSONArray.parseArray(reconciliation_scheme.getValue1().toString());
                        Long[] reconciliation_schemes = reconciliation_schemeArray.toArray(new Long[0]);
                        queryVO.setReconciliationSchemeList(Lists.newArrayList(Arrays.stream(reconciliation_schemes)
                                .map(String::valueOf)
                                .toArray(String[]::new)));
                    }
                }
                List<CtmJSONObject> bankAccountList = getBankAccountList(queryVO, reconciliationDataSource);
                if (bankAccountList.size() == 0) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "eq", "0"));
                    return new RuleExecuteResult();
                }
                List<SimpleFilterVO> simpleFilterVOS = new ArrayList<>();
                for (CtmJSONObject c : bankAccountList) {
                    SimpleFilterVO debitFilter = new SimpleFilterVO(ConditionOperator.and);
                    debitFilter.addCondition(new SimpleFilterVO("id", "eq", c.getString("bankaccount")));
                    debitFilter.addCondition(new SimpleFilterVO("currencyList.currency", "eq", c.getString("currency")));
                    simpleFilterVOS.add(debitFilter);
                }
                //需求CM202400683，银行账户增加对应的数据权限
                String[] bankAccountPermissions = cmpCheckService.getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
                if (bankAccountPermissions != null && bankAccountPermissions.length > 0) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "in", Arrays.asList(bankAccountPermissions)));
                }
                filterVO.appendCondition(ConditionOperator.or, simpleFilterVOS.toArray(new SimpleFilterVO[0]));
            }

            //银行类别
            if ("ucfbasedoc.bd_bankcardref".equals(bill.getRefCode()) || "ucfbasedoc.bd_bankcard".equals(bill.getRefCode())) {
                //组织合集为0，则不展示
                List<String> ids = getFilterIdList(accentityList, "bankaccount.bank as id", reconciliationDataSource);
                if (ids.size() == 0) {
                    ids.add("0");
                }
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "in", ids));
            }

            //币种
            if ("ucfbasedoc.bd_currencytenantref".equals(bill.getRefCode())) {
                //组织合集为0，则不展示
                List<String> ids = getFilterIdList(accentityList, "currency as id", reconciliationDataSource);
                if (ids.size() == 0) {
                    ids.add("0");
                }
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "in", ids));
            }

            //对账方案
            if ("cm_bankreconciliationsetref".equals(bill.getRefCode())) {
                //组织合集为0，则不展示
                List<String> ids = getFilterIdList(accentityList, "mainid as id", reconciliationDataSource);
                if (ids.size() == 0) {
                    ids.add("0");
                }
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "in", ids));
            }
            bill.setCondition(filterVO);
        }

        if (null != bill.getKey() && "cmp_bankvourhcercheck_quickreconciliation".equals(billnum)) {
            List<Map<String, Object>> list = (List<Map<String, Object>>) bill.getData();
            List<String> accentityList;
            if (list.get(0) != null && list.get(0).get("accentity") != null) {
                CtmJSONObject jsonObject = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(list.get(0)));
                CtmJSONArray accentityArray = jsonObject.getJSONArray("accentity");
                String[] accentitys = accentityArray.toArray(new String[0]);
                accentityList = Lists.newArrayList(accentitys);
            } else {
                //获取凭证数据源的对账方案组织id集合
                accentityList = bankVoucherCheckService.getReconciliationSchemeAccentityList(reconciliationDataSource);
                if (accentityList.size() == 0) {
                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-105053"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_220F3BB005000005", "缺少已启用的对账方案，请先配置对应数据！") /* "缺少已启用的对账方案，请先配置对应数据！" */);
                }
            }
            FilterVO filterVO = bill.getCondition();
            if (filterVO == null) {
                filterVO = new FilterVO();
            }
            //对账概览，对账组织过滤逻辑
            if (IRefCodeConstant.FUNDS_ORGTREE.equals(bill.getRefCode())) {
                //对账组织获取全部的数据
                accentityList = bankVoucherCheckService.getReconciliationSchemeAccentityList(reconciliationDataSource);
                //组织合集为0，则不展示
                if (accentityList.size() == 0) {
                    accentityList.add("0");
                }
                MddBaseUtils.appendCondition(bill.getTreeCondition(), "id", "in", accentityList);
            }

            //银行账户过滤逻辑
            if ("ucfbasedoc.bd_enterprisebankacct".equals(bill.getRefCode())) {
                BankAccountInfoQueryVO queryVO = new BankAccountInfoQueryVO();
                queryVO.setAccentityList(accentityList);
                if (list.get(0) != null) {
                    CtmJSONObject jsonObject = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(list.get(0)));
                    if (list.get(0).get("banktype") != null) {
                        CtmJSONArray array = jsonObject.getJSONArray("banktype");
                        String[] s = array.toArray(new String[0]);
                        queryVO.setBanktypeList(Lists.newArrayList(s));
                    }
                    if (list.get(0).get("currency") != null) {
                        CtmJSONArray array = jsonObject.getJSONArray("currency");
                        String[] s = array.toArray(new String[0]);
                        queryVO.setCurrencyList(Lists.newArrayList(s));
                    }
                    if (list.get(0).get("reconciliationScheme") != null) {
                        CtmJSONArray array = jsonObject.getJSONArray("reconciliationScheme");
                        String[] s = array.toArray(new String[0]);
                        queryVO.setReconciliationSchemeList(Lists.newArrayList(s));
                    }
                }
                List<CtmJSONObject> bankAccountList = getBankAccountList(queryVO, reconciliationDataSource);
                if (bankAccountList.size() == 0) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "eq", "0"));
                    return new RuleExecuteResult();
                }
                List<SimpleFilterVO> simpleFilterVOS = new ArrayList<>();
                for (CtmJSONObject c : bankAccountList) {
                    SimpleFilterVO debitFilter = new SimpleFilterVO(ConditionOperator.and);
                    debitFilter.addCondition(new SimpleFilterVO("id", "eq", c.getString("bankaccount")));
                    debitFilter.addCondition(new SimpleFilterVO("currencyList.currency", "eq", c.getString("currency")));
                    simpleFilterVOS.add(debitFilter);
                }
                //需求CM202400683，银行账户增加对应的数据权限
                String[] bankAccountPermissions = cmpCheckService.getBankAccountDataPermission(IServicecodeConstant.BANKRECONCILIATION);
                if (bankAccountPermissions != null && bankAccountPermissions.length > 0) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "in", Arrays.asList(bankAccountPermissions)));
                }
                filterVO.appendCondition(ConditionOperator.or, simpleFilterVOS.toArray(new SimpleFilterVO[0]));
            }

            //银行类别
            if ("ucfbasedoc.bd_bankcardref".equals(bill.getRefCode()) || "ucfbasedoc.bd_bankcard".equals(bill.getRefCode())) {
                //组织合集为0，则不展示
                List<String> ids = getFilterIdList(accentityList, "bankaccount.bank as id", reconciliationDataSource);
                if (ids.size() == 0) {
                    ids.add("0");
                }
                MddBaseUtils.appendCondition(filterVO, "id", "in", ids);
            }

            //币种
            if ("ucfbasedoc.bd_currencytenantref".equals(bill.getRefCode())) {
                //组织合集为0，则不展示
                List<String> ids = getFilterIdList(accentityList, "currency as id", reconciliationDataSource);
                if (ids.size() == 0) {
                    ids.add("0");
                }
                MddBaseUtils.appendCondition(filterVO, "id", "in", ids);
            }

            //对账方案
            if ("cm_bankreconciliationsetref".equals(bill.getRefCode())) {
                //组织合集为0，则不展示
                List<String> ids = getFilterIdList(accentityList, "mainid as id", reconciliationDataSource);
                if (ids.size() == 0) {
                    ids.add("0");
                }
                MddBaseUtils.appendCondition(filterVO, "id", "in", ids);
            }
            bill.setCondition(filterVO);
        }

        return new RuleExecuteResult();
    }

    /**
     * 查询凭证对账方案中启用的各种参照
     *
     * @param accentityList 已选的方案
     * @param selectColumn
     * @return
     * @throws Exception
     */
    private List<String> getFilterIdList(List<String> accentityList, String selectColumn, short reconciliationDataSource) throws Exception {
        List<String> bankAccountList = new ArrayList<>();
        //获取对账方案，银行账户+币种+对账组织维度数据
        QuerySchema schema = QuerySchema.create().distinct().addSelect(selectColumn);
        QueryConditionGroup group = QueryConditionGroup.and(
                QueryCondition.name("mainid.accentity").in(accentityList)
        );
        //启用状态
        List<Object> statusList = new ArrayList<>();
        //启用的
        statusList.add(EnableStatus.Enabled.getValue());
        group.appendCondition(
                QueryCondition.name("mainid.enableStatus").in(statusList),
                QueryCondition.name("mainid.reconciliationdatasource").eq(reconciliationDataSource)
        );
        schema.addCondition(group);
        List<Map<String, Object>> bankReconciliationSetting_bs = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        for (Map<String, Object> map : bankReconciliationSetting_bs) {
            if (map.get("id") != null) {
                bankAccountList.add(map.get("id").toString());
            }
        }
        return bankAccountList;
    }


    private List<CtmJSONObject> getBankAccountList(BankAccountInfoQueryVO queryVO, short reconciliationDataSource) throws Exception {
        List<CtmJSONObject> bankAccountList = new ArrayList<>();
        //获取对账方案，银行账户+币种+对账组织维度数据
        QuerySchema schema = QuerySchema.create().distinct().addSelect("bankaccount,currency");
        QueryConditionGroup group = new QueryConditionGroup();
        if (CollectionUtils.isNotEmpty(queryVO.getAccentityList())) {
            group = QueryConditionGroup.and(
                    QueryCondition.name("mainid.accentity").in(queryVO.getAccentityList())
            );
        }

        //启用状态
        List<Object> statusList = new ArrayList<>();
        //启用的
        statusList.add(EnableStatus.Enabled.getValue());
        group.appendCondition(
                QueryCondition.name("enableStatus_b").in(statusList),
                QueryCondition.name("mainid.enableStatus").in(statusList),
                QueryCondition.name("mainid.reconciliationdatasource").eq(reconciliationDataSource)
        );
        //币种
        if (queryVO.getCurrencyList().size() > 0) {
            group.appendCondition(QueryCondition.name("currency").in(queryVO.getCurrencyList()));
        }
        //银行类别
        if (queryVO.getBanktypeList().size() > 0) {
            group.appendCondition(QueryCondition.name("bankaccount.bankNumber.bank").in(queryVO.getBanktypeList()));
        }
        //对账方案
        if (queryVO.getReconciliationSchemeList().size() > 0) {
            group.appendCondition(QueryCondition.name("mainid").in(queryVO.getReconciliationSchemeList()));
        }
        schema.addCondition(group);
        List<Map<String, Object>> bankReconciliationSetting_bs = MetaDaoHelper.query(BankReconciliationSetting_b.ENTITY_NAME, schema);
        for (Map<String, Object> map : bankReconciliationSetting_bs) {
            CtmJSONObject bankAccountInfoVO = new CtmJSONObject();
            if (map.get("bankaccount") != null && map.get("currency") != null) {
                bankAccountInfoVO.put("bankaccount", map.get("bankaccount").toString());
                bankAccountInfoVO.put("currency", map.get("currency").toString());
                bankAccountList.add(bankAccountInfoVO);
            }
        }
        return bankAccountList;
    }
}
