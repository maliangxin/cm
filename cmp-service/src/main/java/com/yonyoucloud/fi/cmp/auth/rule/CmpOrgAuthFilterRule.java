package com.yonyoucloud.fi.cmp.auth.rule;

import com.yonyou.ucf.mdd.common.model.ref.RefEntity;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.constant.IBillConst;
import com.yonyoucloud.fi.basecom.rule.ICustRefBDAuthFilter;
import com.yonyoucloud.fi.basecom.util.ValueUtils;
import com.yonyoucloud.fi.basecom.utils.AuthUtil;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.auth.filter.*;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.*;
import com.yonyoucloud.fi.cmp.util.CmpAuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.imeta.biz.base.BizContext;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.model.Entity;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.Json;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class CmpOrgAuthFilterRule extends AbstractCommonRule implements InitializingBean {

    private static final List<String> SKIP_BILL_NUMBER = new ArrayList<>();
    private static final List<String> SKIP_BILL_KEY = new ArrayList<>();

    @Autowired
    OrgDataPermissionService orgDataPermissionService;

    @Value("${cmp_def_ref_filter:true}")
    String cmp_def_ref_filter;

    private final Map<String, ICustRefBDAuthFilter> fullnameFilters = new HashMap<>();
    private final Map<String, ICustRefBDAuthFilter> refcodeFilters = new HashMap<>();

    static {
        SKIP_BILL_NUMBER.add(IBillNumConstant.ELECTRONICSTATEMENTCONFIRMLIST);
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_BILLCLAIMCENTER_LIST);//到账认领中心
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_MYBILLCLAIM_LIST);//我的认领
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_BILLCLAIM_CARD);//我的认领卡片
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_BILLCLAIMCENTER);//到账认领中心卡片
        SKIP_BILL_NUMBER.add(IBillNumConstant.BANKRECONCILIATIONLIST);//银行对账单列表
        SKIP_BILL_NUMBER.add(IBillNumConstant.BANKRECONCILIATION);//银行对账单卡片
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_RETIBALIST);//实时余额列表
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_HISBA);//历史余额
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_BANKELECTRONICRECEIPTLIST);//银行交易回单列表
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_BANKELECTRONICRECEIPT);//银行交易回单
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_DLLIST);// 银行交易流水列表
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_MERCHANT);// 银行交易流水
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_BANKRECONCILIATIONSETTING);
        SKIP_BILL_NUMBER.add(IBillNumConstant.BANKRECONCILIATIONREPEATLIST);//疑重列表
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_BANKVOURHCERCHECK_QUICKRECONCILIATION);//银企对账工作台-快速对账弹框
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_BANKVOURHCERCHECK_INFOLIST);//银账对账工作台
        SKIP_BILL_NUMBER.add(IBillNumConstant.CMP_BANKJOURNALCHECK_INFOLIST);//银企对账工作台
        SKIP_BILL_KEY.add(IFieldConstant.ORGID);//所属组织
        SKIP_BILL_KEY.add(IFieldConstant.ACCENTITY_NAME);//授权使用组织
        SKIP_BILL_KEY.add(IFieldConstant.BANKACCOUNT_NAME);//银行账户
        SKIP_BILL_KEY.add(IFieldConstant.BANKACCOUNT);//银行账户
        SKIP_BILL_KEY.add(IFieldConstant.EnterpriseBankAccount);
        SKIP_BILL_KEY.add(IFieldConstant.ACCENTITY);
        SKIP_BILL_KEY.add(IFieldConstant.DEPT_NAME);
    }


    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap)
            throws Exception {
        RuleExecuteResult result = new RuleExecuteResult();
        if (null == billContext) {
            return result;
        }
        //非现金领域的单据上的参照不进行过滤，除日结cmp_settlementlist以外，日结没有subid
        if (!ICmpConstant.CMP_MODUAL_NAME.equals(billContext.getSubid()) && !IBillNumConstant.CMP_SETTLEMENTLIST.equals(billContext.getBillnum())) {
            return result;
        }
        /**
         * 1.针对组织过滤，到账认领相关的，要区分卡片和列表，列表需要根据主组织权限进行过滤，卡片则是需要根据账号查询对应的授权组织范围
         */
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        //CZFW-456982 【DSP支持问题】新增特征：投资协议，引用基本档案-投融资协议，分配到资金收款单，在UI模板配置数据过滤条件，选择金融产品id，选择关系人借贷，保存后再打开，页面空白，报错：获取参照meta信息出错，关闭报错再打开，会显示配置内容并报错。但是该配置可以生效
        if (billDataDto != null && billDataDto.getFullname() != null && billDataDto.getFullname().split("\\.").length != 3) {
            return result;
        }
        //特征字段参照不走过滤逻辑不进行过滤,默认是走过滤的，添加参数值不为true则不进行过滤
        Entity entity = BizContext.getMetaRepository().entity(billDataDto.getFullname());
        if(entity != null && entity.isCharacterEntity() &&  !"true".equals(cmp_def_ref_filter)){
            return result;
        }
        boolean iscard = !"filter".equals(billDataDto.getExternalData());
        try {
            //1. 初始化条件
            String billnum = billDataDto.getBillnum();
            String key = billDataDto.getKey();
            String refcode = billDataDto.getrefCode();
            FilterVO filterVO = new FilterVO();
            RefEntity refentity = billDataDto.getRefEntity();
            String fullname = refentity.cDataGrid_FullName;
            boolean treeRefType = false;
            if ("Tree".equalsIgnoreCase(refentity.cTpltype)) {
                if (billDataDto.getTreeCondition() != null) {
                    filterVO = billDataDto.getTreeCondition();
                } else {
                    billDataDto.setTreeCondition(filterVO);
                }
                treeRefType = true;
            } else {
                if (billDataDto.getCondition() != null) {
                    filterVO = billDataDto.getCondition();
                } else {
                    billDataDto.setCondition(filterVO);
                }
            }

            // 如果没有组织权限则需要加一个 1=2过滤掉数据
            String serviceCode = AppContext.getThreadContext("serviceCode");
            if(StringUtils.isEmpty(serviceCode)){
                serviceCode = billContext.getParameter("serviceCode");
            }
            Set<String> orgset = orgDataPermissionService.queryAuthorizedOrgByServiceCode(serviceCode);
            if (CollectionUtils.isEmpty(orgset)) {
                if (treeRefType) {
                    billDataDto.getTreeCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("1", "eq", 2));
                } else {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("1", "eq", 2));
                }
            }
            BizObject data = getBizObject(billDataDto, billContext, paramMap);
            // 公共的参照过滤
            if (handleCommonAuthFilter(billContext,billDataDto,data,filterVO,fullname)) {
                return result;
            }

            // 审批流模型管理分支条件中，设置参照值跳过过滤
            if (StringUtils.isEmpty(key)) {
                return result;
            }
            // UI模板设计器中，设置参照默认值跳过过滤
            Map<String, Object> custMap = billDataDto.getCustMap();
            if (MapUtils.isNotEmpty(custMap)) {
                String ideDesignType = MapUtils.getString(custMap, "ideDesignType");
                if (ValueUtils.isNotEmpty(ideDesignType) && "ysmdd".equals(ideDesignType)) {
                    return result;
                }
            }
            //基础档案的默认加逻辑删除标识过滤
            if ("ucfbasedoc".equals(refentity.domain)) {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("dr", "eq", "0"));
            }

            //2  处理启用禁用标记,列表 下生效[记者]
            if (iscard) {
                if (AuthUtil.enableField(refcode, fullname) != null) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO(AuthUtil.enableField(refcode, fullname), "eq", "1"));
                } else if (AuthUtil.disableField(refcode, fullname) != null) {
                    if ("aa.merchant.Merchant".equals(fullname)) {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO(AuthUtil.disableField(refcode, fullname), "in", "0"));
                    } else {
                        filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO(AuthUtil.disableField(refcode, fullname), "eq", "0"));
                    }
                }
            }

            if (data == null) {
                return result;
            }
            // 处理列表肩部批改时，参照过滤赋值
            Map mapCondition;
            String accentity;
            String currency;
            Object simpleVOs;
            ArrayList simpleVOList;
            Iterator var18;
            Object condition;
            Map simpleVO;
            if (AuthUtil.containCmpBillNum(billnum)) {
                mapCondition = billDataDto.getMapCondition();
                accentity = null;
                currency = null;
                if (mapCondition != null && mapCondition.size() > 0) {
                    simpleVOs = mapCondition.get("simpleVOs");
                    if (simpleVOs != null) {
                        simpleVOList = (ArrayList) simpleVOs;
                        var18 = simpleVOList.iterator();

                        while (var18.hasNext()) {
                            condition = var18.next();
                            simpleVO = (Map) condition;
                            if (simpleVO.containsKey("field") && simpleVO.get("field").equals("accentity")) {
                                accentity = (String) simpleVO.get("value");
                            }

                            if (simpleVO.containsKey("field") && simpleVO.get("field").equals("currency")) {
                                currency = (String) simpleVO.get("value");
                            }
                        }
                    }
                }

                if (StringUtils.isNotEmpty(accentity)) {
                    data.set("accentity", accentity);
                }

                if (StringUtils.isNotEmpty(currency)) {
                    data.set("currency", currency);
                }
            }
            //3  处理组织管控，
            filterOrgMC(billContext, iscard, billnum, fullname, refcode, data, filterVO, key);

            //4  银行账号币种过滤
            filterCurrency(fullname, billnum, filterVO, data, iscard);

            //6 处理个性化 String fullname,String refcode,BizObject data, FilterVO filterVO
//            filterBillCommon(billContext, iscard, billnum, fullname, refcode, data, filterVO, key, billDataDto);

            if (!AuthUtil.needMCOrg(fullname)) {
                billDataDto.setIsDistinct(true);
            }

        } catch (Exception e) {
            throw e;
        }
        if (!iscard) {
            billDataDto.setData(null);
        }
        return result;
    }

    private void filterOrgMC(BillContext billContext, boolean iscard, String billnum, String fullname, String refcode, BizObject data,
                             FilterVO filterVO, String key) throws Exception {
        if (("cmp_paymargin".equals(billnum) || "cmp_receivemargin".equals(billnum)) && "ourbankaccount_name".equals(key)) {
            return;
        }
        if (("cmp_paymargin".equals(billnum) || "cmp_receivemargin".equals(billnum) || "cmp_foreignpayment".equals(billnum)) && "ourname_name".equals(key)) {
            return;
        }
        if ("cmp_foreignpayment".equals(billnum) && ("ourbankaccount_account".equals(key) || "paymenterprisebankaccount_account".equals(key))) {
            return;
        }
        // 历史余额卡片页参照、银行账户期初上账参照不进行组织过滤
        if (("cmp_hisba".equals(billnum) || "cmp_initdatayh".equals(billnum)) && "bd.enterprise.OrgFinBankacctVO".equals(fullname)) {
            return;
        }

        if (AuthUtil.notFilterOrg(billnum, fullname) || !AuthUtil.containOrg(fullname)) {
            return;
        }
        List<String> orgs;
        try {
            // 针对单组织
            if(FIDubboUtils.isSingleOrg()){
                BizObject singleOrg = FIDubboUtils.getSingleOrg();
                if(singleOrg != null){
                    data.set(IBussinessConstant.ACCENTITY,singleOrg.get("id"));
                    data.set("accentity_name",singleOrg.get("name"));
                }
            }
            // CZFW-423315【DSP支持问题】同一业务类别的单据不同公司，批量认领不能认领应该在认领界面支持选择多个单据进行认领，跳转到认领界面后，选择业务小类时报错，业务小类时企业级账号范围，不应该按组织过滤业务小类
            if (IBillNumConstant.CMP_BILLCLAIM_CARD.equals(billnum) && data.get("accentity") == null && !key.equals("oppositeobjectname")) {
                orgs = new ArrayList();
                //批量认领的时候前端会清空这个字段 需求给方案 这里设置为企业级
                orgs.add("666666");
            } else {
                //获取的是会计主体类型的组织
                orgs = AuthUtil.getAccentitys(billContext, data, billnum, iscard);
            }
        } catch (Exception e) {
            log.error("AuthUtil.getAccentitys error:{}", e.getMessage(), e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-999999"), e.getMessage());
        }
        boolean needDelegate = true;
        //1.以会计主体+库存组织 为过滤
        if (AuthUtil.isOrgFinAccdata(fullname)) {
            needDelegate = false;
        }
        if (orgs != null) {
            AuthUtil.filterOrgMC(filterVO, fullname, needDelegate, orgs.toArray(new String[0])); //data,orgfunc,
        }
    }

    private void filterBillCommon(BillContext billContext, boolean iscard, String billnum, String fullname, String refcode, BizObject data,
                                  FilterVO filterVO, String key, BillDataDto billDataDto) throws Exception {
        ICustRefBDAuthFilter filter = fullnameFilters.get(fullname);
        if (filter != null) {
            filter.filterBillCommon(billContext, iscard, billnum, fullname, refcode, data, filterVO, key, billDataDto);
        }
        filter = refcodeFilters.get(refcode);
        if (filter != null) {
            filter.filterBillCommon(billContext, iscard, billnum, fullname, refcode, data, filterVO, key, billDataDto);
        }

    }

    /**
     * 过滤币种
     */
    private void filterCurrency(String fullname, String billnum, FilterVO filterVO, BizObject data, boolean iscard) {
        if (AuthUtil.needCurrencyFilter(fullname, billnum)) {
            List<String> currencys = AuthUtil.getBizObjectAttr(data, IBillConst.CURRENCY);
            // 企业银行账户多币种，参照只显示已启用的币种
            if ("bd.enterprise.OrgFinBankacctVO".equals(fullname) && iscard) {
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("currencyList.enable", "eq", "1"));
            }
            if (currencys != null && !billnum.equals(IBillNumConstant.CMP_CHECKSTOCKAPPLY)) {
                if ("bd.enterprise.OrgFinBankacctVO".equals(fullname)) {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("currencyList.currency", "in", currencys));
                } else {
                    filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("currency", "in", currencys));
                }
            }
        }
    }

    private BizObject getBizObject(BillDataDto billDataDto, BillContext billContext, Map<String, Object> paramMap) throws Exception {
        Object ret = billDataDto.getData();

        List<?> data = null;
        if (ret instanceof String) {
            // 临时修改,形成表单报错 lnm
            //data = this.getBills(billContext, paramMap);
            data = innerGetBizObject((String) ret);
        } else {
            data = (List<?>) ret;
        }
        if (data == null) {
            return null;
        }
        if (data.size() > 0) {
            return (BizObject) data.get(0);
        }
        return null;
    }


    private List<BizObject> innerGetBizObject(String data) {
        Json json = new Json(data);
        List<BizObject> objs = json.decode();
        if (objs == null || objs.size() == 0) {
            return null;
        }
        return objs;
    }

    @Override
    public void afterPropertiesSet() {
        Map<String, ICustRefBDAuthFilter> allFilters = AppContext.getBeansForType(ICustRefBDAuthFilter.class);
        allFilters.values().forEach((filter) -> {
            try {
                List<String> matchFullName = filter.matchFullname();
                List<String> matchRefCode = filter.matchRefcode();
                if (matchFullName != null) {
                    matchFullName.forEach(fullName -> {
                        ICustRefBDAuthFilter oldFilter = fullnameFilters.get(fullName);
                        if (oldFilter == null || oldFilter.priority() < filter.priority()) {
                            fullnameFilters.put(fullName, filter);
                        }
                    });
                }
                if (matchRefCode != null) {
                    matchRefCode.forEach(refCode -> {
                        ICustRefBDAuthFilter oldFilter = refcodeFilters.get(refCode);
                        if (oldFilter == null || oldFilter.priority() < filter.priority()) {
                            refcodeFilters.put(refCode, filter);
                        }
                    });
                }
            } catch (Exception e) {
                log.error("exception when invoke DBAuthFilterRule after properties set method", e);
            }
        });

    }

    /**
     * 参照的组织权限过滤
     *
     * @param billDataDto
     * @return
     * @throws Exception
     */
    private boolean handleCommonAuthFilter(BillContext billContext,BillDataDto billDataDto,BizObject data,FilterVO filterVO,String fullname) throws Exception {
        boolean iscard = !"filter".equals(billDataDto.getExternalData());
        String billnum = billDataDto.getBillnum();
        String refcode = billDataDto.getrefCode();
        String key = billDataDto.getKey();
        if (StringUtils.isNotEmpty(refcode)) {
            String serviceCode = AppContext.getThreadContext("serviceCode");
            if(StringUtils.isEmpty(serviceCode)){
                serviceCode = billContext.getParameter("serviceCode");
            }
            Set<String> orgids = orgDataPermissionService.queryAuthorizedOrgByServiceCode(serviceCode);
            //公共的资金组织参照权限过滤
            if (refcode != null && IRefCodeConstant.FUNDS_ORG_ADN_FINANCE_ORG_LIST.contains(refcode)) {

                if (IBillNumConstant.CMP_BILLCLAIM_CARD.equals(billnum) || ((IBillNumConstant.FUND_PAYMENT.equals(billnum) || (IBillNumConstant.FUND_COLLECTION.equals(billnum)) || (IBillNumConstant.BANKRECONCILIATION.equals(billnum))) && ICmpConstant.OPPOSITEOBJECTNAME.equals(key))) {
                    return true;
                }
                //银行流水按指定组织发布去掉主组织权限过滤;PublishAssignOrg为前端传递的标识
                if((IBillNumConstant.BANKRECONCILIATION.equals(billnum) || IBillNumConstant.BANKRECONCILIATIONLIST.equals(billnum)) && "PublishAssignOrg".equals(key)){
                    return true;
                }

                // 添加企业账户级
                if (CmpAuthUtils.GLOBAL_ACCENTITY_BILLNUMLIST.contains(billnum)) {
                    orgids.add("666666");
                }

                if (IBillNumConstant.CMP_INTERNALTRANSFERPROTOCOL.equals(billnum)) {
                    if (ICmpConstant.INTO_ACCENTITY_NAME.equals(key)) {
                        return true;
                    }
                }
                filterVO.appendCondition(ConditionOperator.and, new SimpleFilterVO("id", "in", orgids));
                return true;
            } else if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(refcode)) {//公共的企业银行账户参照权限过滤
                // 企业银行账户过滤时，是先选银行账户，所以需要按照所有有权限的银行账户进行过滤
                if (SKIP_BILL_NUMBER.contains(billDataDto.getBillnum())) {
                    billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("orgid", "in", orgids));
                    return true;
                }
            } else if (IRefCodeConstant.DEPT_REF.equals(refcode) && iscard) {//公共的部门参照权限过滤
                DeptAuthFilter.filter(billContext, iscard, billnum, refcode, data, filterVO, billDataDto);
                return true;
            } else if (iscard && (IRefCodeConstant.AA_OPERATOR.equals(refcode) || IRefCodeConstant.BD_STAFF_REF.equals(refcode)
                    || IRefCodeConstant.DOMAIN_BD_STAFF_REF.equals(refcode) || IRefCodeConstant.BD_STAFF_LEAVE_REF.equals(refcode))) {//公共的部门参照权限过滤
                StaffAuthFilter.filter(billContext, iscard, billnum, refcode, data, filterVO, billDataDto);
                return true;
            } else if (iscard && (IRefCodeConstant.AA_ORG.equals(refcode)||refcode.equals("ucf-org-center.org_pure_tree_ref"))) {//公共的组织参照权限过滤
                OrgAuthFilter.filter(billContext, iscard, billnum, refcode, data, filterVO, billDataDto);
                return true;
            } else if (iscard && (IRefCodeConstant.YSSUPPLIER_AA_VENDORBANKREF.equals(refcode) || IRefCodeConstant.SUPPLIER_REF_FULLNAME.equals(fullname))) {//公共的供应商银行参照权限过滤
                SupplierBankAuthFilter.filter(billContext, iscard, billnum, refcode, data, filterVO, billDataDto);
                return true;
            } else if (iscard
                    && (IRefCodeConstant.AA_MERCHANTBANKREF.equals(refcode)
                    || IRefCodeConstant.PRODUCTCENTER_AA_MERCHANTAGENTFINANCIALREF.equals(refcode)
                    || IRefCodeConstant.CUSTOMER_REF_FULLNAME.equals(fullname) )) {//公共的客户银行参照权限过滤
                CustomerBankAuthFilter.filter(billContext, iscard, billnum, refcode, data, filterVO, billDataDto);
                return true;
            } else if (iscard
                    && (IRefCodeConstant.UCFBASEDOC_BD_PROJECTCLASSTREEREF.equals(refcode)
                    || IRefCodeConstant.PROJECT_REF_FULLNAME.equals(fullname) )) {//公共的项目参照权限过滤
                ProjectAuthFilter.filter(billContext, iscard, billnum, refcode, data, filterVO, billDataDto);
                return true;
            } else if (iscard
                    && (IRefCodeConstant.BD_STAFF_BANKACCT_REF.equals(refcode)
                    || IRefCodeConstant.STAFF_BANKACCT_REF_FULLNAME.equals(fullname) )) {//公共的项目参照权限过滤
                StaffBankAcctBDAuthFilter.filter(billContext, iscard, billnum, refcode, data, filterVO, billDataDto);
                return true;
            }
        }
        return false;
    }


}