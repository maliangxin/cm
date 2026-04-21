package com.yonyoucloud.fi.cmp.bankreceipt.rule;

import com.yonyou.cloud.utils.CollectionUtils;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyoucloud.fi.cmp.bankreceipt.service.TaskBankReceiptService;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.iuap.yms.lock.YmsLock;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVOWithRange;
import com.yonyou.ucf.basedoc.model.puborggroup.OrgRangeVO;
import com.yonyou.ucf.basedoc.model.rpcparams.CurrencyBdParams;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.bill.rule.util.GetRoundModeUtils;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.basecom.precision.CheckPrecisionVo;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.cmp.auth.OrgDataPermissionService;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.CommonSaveUtils;
import com.yonyoucloud.fi.cmp.cmpentity.DateOrigin;
import com.yonyoucloud.fi.cmp.cmpentity.ReceiptassociationStatus;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.util.EntityTool;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component("bankReceiptBeforeSaveRule")
public class BankReceiptBeforeSaveRule extends AbstractCommonRule {

    @Autowired
    BaseRefRpcService baseRefRpcService;
    @Resource
    OrgDataPermissionService orgDataPermissionService;
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        List<BizObject> bills = getBills(billContext, map);
        List<YmsLock> ymsLockList = new ArrayList<>();
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        // 导入
        boolean importFlag =  "import".equals(billDataDto.getRequestAction());

        if (bills != null && bills.size() > 0) {
            for (BizObject bill : bills) {
                BankElectronicReceipt bizObject = (BankElectronicReceipt) bill;
                // OpenApi
                boolean openApiFlag = bizObject.containsKey(ICmpConstant.FROM_API) && bizObject.get(ICmpConstant.FROM_API).equals(true);
                if (importFlag) {
                    // 币种校验
                    if (bizObject.get("currency") != null) {
                        //有币种id时，不用再转换了
                        ;
                    } else if (bizObject.get("currency_name") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101366"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000FA", "币种不能为空！") /* "币种不能为空！" */);
                    } else {
                        String currency = bizObject.get("currency_name");
                        /** 币种名称转成ID */
                        CurrencyBdParams currencyBdParams = new CurrencyBdParams();
                        currencyBdParams.setName(currency);
                        List<CurrencyTenantDTO> currencylist = baseRefRpcService.queryCurrencyByParams(currencyBdParams);
                        if (currencylist == null || currencylist.size() == 0) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101367"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000EE", "币种名称不存在！") /* "币种名称不存在！" */);
                        }
                        for (CurrencyTenantDTO currencyTenantDTO : currencylist) {
                            if (currency.equals(currencyTenantDTO.getName())) {
                                bizObject.setCurrency(currencyTenantDTO.getId());//设置币种ID
                                break;
                            }
                        }
                    }

                    if (bizObject.get("enterpriseBankAccount") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101368"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000F4", "银行账户不能为空！") /* "银行账户不能为空！" */);
                    }else {
                        EnterpriseBankAcctVO enterpriseBankAcctVO = baseRefRpcService.queryEnterpriseBankAccountById(bizObject.get("enterpriseBankAccount"));
                        if (enterpriseBankAcctVO == null) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101369"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080035", "数据中的银行账户[") /* "数据中的银行账户[" */ + bizObject.get("enterpriseBankAccount_account") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080034", "]未启用或不存在！") /* "]未启用或不存在！" */);
                        }
                        // 单租户
                        if (FIDubboUtils.isSingleOrg()) {
                            // 银行账户无所属组织 全局组织进行赋值
                            enterpriseBankAcctVO.setOrgid(FIDubboUtils.getSingleOrg().get("id"));
                        }
                        // 判断所属组织
                        if(bizObject.get("accentity")!=null){
                            // 判断导入所属组织与银行账户所属组织是否一致
                            if(!bizObject.get("accentity").equals(enterpriseBankAcctVO.getOrgid())){
                                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400687", "银行账户与所属组织不一致，请检查！") /* "银行账户与所属组织不一致，请检查！" */);
                            }
                        }else {
                            // 如果导入数据所属组织为空，取银行账户所属组织赋值
                            bizObject.set("accentity",enterpriseBankAcctVO.getOrgid());
                        }
                        // 非openApi才校验
                        if (!openApiFlag) {
                            // 判断授权使用组织
                            /**
                             * 1，获取授权的组织
                             * 2，获取银行账户的适用范围
                             * 3，判断适用范围内的组织是否已授权，只要存在一个授权的，就可以导入
                             */
                            // 获取授权的组织
                            Set<String> orgs = orgDataPermissionService.queryAuthorizedOrgByServiceCode(IServicecodeConstant.BANKRECEIPTMATCH);
                            if(orgs != null && orgs.size() <1){
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101371"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080037", "当前用户无银行账号[%s]的导入权限，请检查！") /* "当前用户无银行账号[%s]的导入权限，请检查！" */,enterpriseBankAcctVO.getAccount()));
                            }
                            EnterpriseBankAcctVOWithRange enterpriseBankAcctVoWithRange = enterpriseBankQueryService.queryEnterpriseBankAcctVOWithRangeById(bizObject.get("enterpriseBankAccount"));
                            if(enterpriseBankAcctVoWithRange != null){
                                List<OrgRangeVO> orgRangeVOS = enterpriseBankAcctVoWithRange.getAccountApplyRange();
                                // 使用范围中的组织是否是授权的组织
                                Boolean containFlag = false;
                                for(OrgRangeVO orgRangeVO : orgRangeVOS){
                                    if(orgs.contains(orgRangeVO.getRangeOrgId())){
                                        containFlag = true;
                                        break;
                                    }
                                }
                                if(!containFlag){
                                    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101371"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080037", "当前用户无银行账号[%s]的导入权限，请检查！") /* "当前用户无银行账号[%s]的导入权限，请检查！" */,enterpriseBankAcctVO.getAccount()));
                                }
                            }
                        }
                    }
                    if (bizObject.get("receiptno") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101372"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000F6", "回单编号不能为空") /* "回单编号不能为空" */);
                    }
                    if (bizObject.get("filename") == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101373"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000F8", "电子回单文件名称为空，不允许导入回单明细，请填写电子回单文件名称！") /* "电子回单文件名称为空，不允许导入回单明细，请填写电子回单文件名称！" */);
                    } else {
                        checkFilename(bizObject.get("filename"));
                        QuerySchema querySchema = QuerySchema.create().addSelect(" receiptno ");
                        QueryConditionGroup queryConditionGroup = QueryConditionGroup.and(QueryCondition.name("filename").eq(bizObject.get("filename")));
                        querySchema.addCondition(queryConditionGroup);
                        List<BankElectronicReceipt> list = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, querySchema, null);
                        if (list != null && list.size() > 0) {
                            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101374"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000FD", "导入文件中回单编号") /* "导入文件中回单编号" */ + bizObject.get("receiptno") + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000FC", "电子回单文件名称与现有回单") /* "电子回单文件名称与现有回单" */ + list.get(0).getReceiptno() + com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000FE", "电子回单文件名称重复，不允许导入！") /* "电子回单文件名称重复，不允许导入！" */);
                        }
                    }
                    if (bizObject.getDc_flag() == null || bizObject.getTran_amt() == null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101375"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000EF", "借贷标和交易金额不能为空!") /* "借贷标和交易金额不能为空!" */);
                    }
                    //if (bizObject.getTran_amt().compareTo(BigDecimal.ZERO) <= 0) {
                    //    throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101376"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000F1", "交易金额需大于0") /* "交易金额需大于0" */);
                    //}
                    //重复校验
                    checkIsunique(bizObject);
                    bizObject.setDataOrigin(DateOrigin.Created);
                    Map<String, Object> bankAccountObject = QueryBaseDocUtils.queryEnterpriseBankAccountById(bizObject.getEnterpriseBankAccount());
                    bizObject.setBanktype(bankAccountObject.get("bank").toString());
                    //精度处理
                    CurrencyTenantDTO currencyDTO = baseRefRpcService.queryCurrencyById(bizObject.get("currency"));
                    String curID =  String.valueOf(currencyDTO.getId());
                    //账户币种和导入币种一致性校验
                    if (bankAccountObject.get("currencyList") != null) {
                        List<HashMap<String,String>> currencyList =(List<HashMap<String,String>>) bankAccountObject.get("currencyList");
                        if (CollectionUtils.isNotEmpty(currencyList)) {
                            int currencyFlag = 0;
                            for (int i = 0;i < currencyList.size();i++){
                                String cur = String.valueOf(currencyList.get(i).getOrDefault("currency",null));
                                if(cur.equals(curID)){
                                    currencyFlag = 1;
                                    break;
                                }
                            }
                            if(currencyFlag ==0){
                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101377"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A05080036", "银行账户币种与导入币种不一致!") /* "银行账户币种与导入币种不一致!" */);
                            }
//                            List<String> currencyIdList = currencyList.stream().map(currency -> String.valueOf(currency.get("currency"))).collect(Collectors.toList());
//                            if (CollectionUtils.isNotEmpty(currencyIdList) && !currencyIdList.contains(currencyDTO.getId())) {
//                                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101378"),"银行账户币种与导入币种不一致!");
//                            }
                        }
                    }
                    //报错校验后 再进行锁校验
                    String lockStr = "fromApi_Bankreceipt_Lock_" + bizObject.get("filename") + InvocationInfoProxy.getTenantid();
                    YmsLock ymsLock;
                    if ((ymsLock= JedisLockUtils.lockRuleWithOutTrace(lockStr,map))==null) {
                        throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100323"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803B7","导入数据重复，请检查后重试！") /* "导入数据重复，请检查后重试！" */);
                    }
                    ymsLockList.add(ymsLock);
                    Integer moneydigit = currencyDTO.getMoneydigit();
                    RoundingMode moneyRound = GetRoundModeUtils.getCurrencyPriceRoundMode(bizObject.get("currency"), 1);
                    CheckPrecisionVo checkPrecisionVo = new CheckPrecisionVo();
                    checkPrecisionVo.setPrecisionId(bizObject.get("currency"));
                    bizObject.setTran_amt(bizObject.getTran_amt().setScale(moneydigit, moneyRound));
                    //openAPI自己做了校验，且字段和导入不同，不能用导入的校验
                } else if (openApiFlag) {
                    ;
                } else {
                    bizObject.setDataOrigin(DateOrigin.DownFromYQL);
                }
                //QuerySchema querySchema1 = QuerySchema.create().addSelect("*");
                //QueryConditionGroup queryCondition = new QueryConditionGroup();
                //queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("bank_seq_no").eq(bizObject.get("bankseqno"))));
                ////queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("accentity").eq(bizObject.get("accentity"))));
                //queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("dc_flag").eq(bizObject.get("dc_flag"))));
                //queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("tran_amt").eq(bizObject.get("tran_amt"))));
                //queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("bankaccount").eq(bizObject.get("enterpriseBankAccount"))));
                //queryCondition.addCondition(QueryConditionGroup.and(QueryCondition.name("receiptassociation").eq(ReceiptassociationStatus.NoAssociated.getValue())));
                //querySchema1.addCondition(queryCondition);
                //List<BankReconciliation> bankReconciliationList = MetaDaoHelper.queryObject(BankReconciliation.ENTITY_NAME, querySchema1, null);
                //if (bankReconciliationList.size() > 0 && bankReconciliationList != null) {
                //    BankReconciliation bankReconciliation = bankReconciliationList.get(0);
                //    //设置更新状态，设置回单关联状态为自动关联
                //    EntityTool.setUpdateStatus(bankReconciliation);
                //    bankReconciliation.setReceiptassociation(ReceiptassociationStatus.AutomaticAssociated.getValue());
                //    try {
                //        CommonSaveUtils.updateBankReconciliation(bankReconciliation);
                //    } catch (Exception e) {
                //        log.error(e.getMessage(), e);
                //        return new RuleExecuteResult();
                //    }
                //    bizObject.setBankreconciliationid(bankReconciliation.getId().toString());
                //    bizObject.setAssociationstatus(ReceiptassociationStatus.AutomaticAssociated.getValue());
                //} else {
                //    bizObject.setAssociationstatus(ReceiptassociationStatus.NoAssociated.getValue());
                //}
            }
        }
        return new RuleExecuteResult();
    }

    private void checkFilename(String filename) {
        String suffixes = "pdf|png|ofd|jpeg|jpg|bmp";
        String suffix = "";
        if (filename != null && filename.contains(".")) {
            suffix = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase(Locale.ENGLISH);
        }
        if ("".equals(suffix)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101379"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000F7", "电子回单文件名称不正确，文件名称后缀需为pdf、ofd、jpeg、jpg、png、bmp中的一种，请检查！") /* "电子回单文件名称不正确，文件名称后缀需为pdf、ofd、jpeg、jpg、png、bmp中的一种，请检查！" */);
        }
        if (!suffixes.contains(suffix)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101379"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000F7", "电子回单文件名称不正确，文件名称后缀需为pdf、ofd、jpeg、jpg、png、bmp中的一种，请检查！") /* "电子回单文件名称不正确，文件名称后缀需为pdf、ofd、jpeg、jpg、png、bmp中的一种，请检查！" */);
        }
    }

    /**
     * 数据重复校验
     * 修改校验规则：去掉会计主体，添加摘要字段，交易流水号和前面条件做成或，先判断交易流水，有重复的不判断其他了，交易流水号无重复，再判断其他
     *
     * @param bankElectronicReceipt
     * @throws Exception
     */
    private void checkIsunique(BankElectronicReceipt bankElectronicReceipt) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect(" receiptno ");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name(IBussinessConstant.ACCENTITY).eq(bankElectronicReceipt.getAccentity()),
                QueryCondition.name("enterpriseBankAccount").eq(bankElectronicReceipt.getEnterpriseBankAccount()),
                QueryCondition.name("tranDate").eq(bankElectronicReceipt.getTranDate()),
                QueryCondition.name("bankseqno").eq(bankElectronicReceipt.getBankseqno()),
                QueryCondition.name("receiptno").eq(bankElectronicReceipt.getReceiptno()),
                QueryCondition.name("accentity").eq(bankElectronicReceipt.getAccentity()));
        if (!StringUtils.isEmpty(bankElectronicReceipt.getCurrency())) {
            group.addCondition(QueryCondition.name("currency").eq(bankElectronicReceipt.getCurrency()));
        }
        if (!StringUtils.isEmpty(bankElectronicReceipt.getRemark())) {
            group.addCondition(QueryCondition.name("remark").eq(bankElectronicReceipt.getRemark()));
        }
        if (bankElectronicReceipt.getDc_flag() != null) {
            group.addCondition(QueryCondition.name("dc_flag").eq(bankElectronicReceipt.getDc_flag().getValue()));
        }
        if (bankElectronicReceipt.getTran_amt() != null && bankElectronicReceipt.getTran_amt().compareTo(BigDecimal.ZERO) > 0) {
            group.addCondition(QueryCondition.name("tran_amt").eq(bankElectronicReceipt.getTran_amt()));
        }
        querySchema.addCondition(group);
        List<BankElectronicReceipt> list = MetaDaoHelper.queryObject(BankElectronicReceipt.ENTITY_NAME, querySchema, null);
        if (list != null && list.size() > 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101380"),String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B000FB", "回单编号：[%s]，已存在，不允许导入！") /* "回单编号：[%s]，已存在，不允许导入！" */, list.get(0).getReceiptno()));
        }
    }


}
