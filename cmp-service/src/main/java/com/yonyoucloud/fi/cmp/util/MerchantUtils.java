package com.yonyoucloud.fi.cmp.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.ucf.basedoc.service.itf.UCFBasedocBankDubboService;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dubbo.DubboReference;
import com.yonyou.yonbip.ctm.json.CtmJSONArray;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.json.JsonUtils;
import com.yonyoucloud.fi.basecom.utils.FIDubboUtils;
import com.yonyoucloud.fi.basecom.utils.HttpTookit;
import com.yonyoucloud.fi.cmp.bankdealdetail.BankDealDetail;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.cmpentity.MerchantFlag;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.constant.IStwbConstantForCmp;
import com.yonyoucloud.fi.cmp.constant.MerchantConstant;
import com.yonyoucloud.fi.cmp.enums.MerchantOperateEnum;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.iuap.upc.api.IMerchantServiceV2;
import com.yonyoucloud.iuap.upc.dto.*;
import com.yonyoucloud.upc.pub.api.vendor.service.vendor.IVendorPubQueryService;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorExtendVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorOrgVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.time.Duration;
import java.util.*;

/**
 * 客户和供应商操作
 *
 * @author miaowb
 */
@Slf4j
public class MerchantUtils {

    private static final Logger logger = LoggerFactory.getLogger(MerchantUtils.class);

    private static final String ORG_ENTITYNAME = "aa.baseorg.OrgMV";
    private static final Cache<String, MerchantResult> merchantCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();

    private static final Cache<String, Set<String>> orgByAccentityCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .concurrencyLevel(4)
            .maximumSize(1000)
            .softValues()
            .build();

    /**
     * http 同步客商
     *
     * @param billDataDto
     * @param merchantOperateEnum
     * @return
     */
    public static MerchantResult operateMerchant(BillDataDto billDataDto, MerchantOperateEnum merchantOperateEnum) throws JsonProcessingException {
        MerchantResult merchantResult = new MerchantResult();
        String json = CtmJSONObject.toJSONString(billDataDto);
        try {
            Preconditions.checkNotNull(billDataDto);
            String merchantUrl = AppContext.getEnvConfig(MerchantConstant.MERCHANT_URL);
            switch (merchantOperateEnum) {
                case QUERY:
                    merchantUrl += MerchantConstant.UPC_QUERY;
                    MerchantResult merchantResultFromCache = merchantCache.getIfPresent(json);
                    if (merchantResultFromCache != null) {
                        return merchantResultFromCache;
                    }
                    break;
                case SAVE:
                    merchantUrl += MerchantConstant.UPC_SAVE;
                    break;
                default:
                    break;
            }
            merchantUrl += InvocationInfoProxy.getYhtAccessToken();
            Map<String, String> headersMap = new HashMap<>();
            // 执行post请求的方法
            String resultJSON = HttpTookit.doPostWithJson(merchantUrl, json, headersMap);
            merchantResult = CtmJSONObject.parseObject(resultJSON, MerchantResult.class);
        } catch (Exception e) {
            if(log.isInfoEnabled()) {
                log.info("MerchantUtils--operateMerchant--exception:", e);
            }
            merchantResult.setCode(500);
            merchantResult.setMessage(e.getMessage());
        }
        if (merchantOperateEnum == MerchantOperateEnum.QUERY) {
            merchantCache.put(json, merchantResult);
        }

        return merchantResult;
    }

    /**
     * 客户查询
     *
     * @param accentity
     * @param bankAccountName
     * @param bankAccount
     * @return
     */
    public static MerchantResult queryCust(String accentity, String bankAccountName, String bankAccount) {
        MerchantResult merchantResult = new MerchantResult();
        merchantResult.setCode(200);
        try {
            Set<String> orgSet = MerchantUtils.getOrgByAccentity(accentity);

            // 先根据银行账户，账户名称查询对应的客户银行账户
            IMerchantServiceV2 merchantService = AppContext.getBean(IMerchantServiceV2.class);
            AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
            agentFinancialQryDTO.setFields(new String[]{MerchantConstant.MERCHANTID,MerchantConstant.ID,MerchantConstant.STOPSTATUS,MerchantConstant.BANKACCOUNTNAME,
                    MerchantConstant.JOINTLINENO,MerchantConstant.OPENBANK,"merchantName","bankAccount"});
            agentFinancialQryDTO.setBankAccount(bankAccount);
            // 启用状态
            agentFinancialQryDTO.setStopStatus(false);
            List<AgentFinancialDTO> agentFinancialDTOS = merchantService.listMerchantAgentFinancial(agentFinancialQryDTO);
            if (agentFinancialDTOS.isEmpty()) {
                return merchantResult;
            }
            List<AgentFinancialDTO> agentFinancialDTOList = new ArrayList<>();
            for (AgentFinancialDTO agentFinancialDTO : agentFinancialDTOS) {
                if (StringUtils.isEmpty(bankAccountName)) {
                    agentFinancialDTOList.add(agentFinancialDTO);
                } else if (bankAccountName.equals(agentFinancialDTO.getBankAccountName())){
                    agentFinancialDTOList.add(agentFinancialDTO);
                }
            }
            //大北农需求（2611001828SR24091904173），设置yms 参数，如果是精准本组织匹配=true，过滤掉分配停用的供应商和客户
            boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
            if(isAccuratematching) {
                // 大北农需求 ，匹配不上账户+名称，直接返回匹配失败
                if (agentFinancialDTOList.isEmpty()) {
                    merchantResult.setCode(202);
                    return merchantResult;
                }
                // 根据客户id查询客户分配关系
                    int m = 0;
                    for (AgentFinancialDTO agentFinancialDTO : agentFinancialDTOList) {
                        // 先查询当前资金组织是否在客户分配关系内，若分配给当前资金组织，默认取当前资金组织
                        MerchantDTO merchantByIdAndOrg = merchantService.getMerchantByIdAndOrg(agentFinancialDTO.getMerchantId(), Long.valueOf(accentity), new String[]{"detailStopStatus", MerchantConstant.ID, MerchantConstant.NAME, ICmpConstant.ORGID});
                        if (merchantByIdAndOrg != null && (merchantByIdAndOrg.getDetailStopStatus() == null || !merchantByIdAndOrg.getDetailStopStatus().booleanValue())) {
                            merchantResult.setAgentFinancialDTO(agentFinancialDTO);
                            merchantResult.setOrgId(accentity);
                          m++;
                        }
                    }
                    //同一个组织匹配多条，设置成空,前端匹配不上
                    if(m>1){
                        merchantResult.setAgentFinancialDTO(null);
                        merchantResult.setOrgId(null);
                        // 匹配多个供应商 返回201
                        merchantResult.setCode(201);
                    }
                    return merchantResult;
            }else{
                // 根据客户id查询客户分配关系
                if (CollectionUtils.isNotEmpty(agentFinancialDTOList)) {
                    MerchantDTO merchantByIdAndOrg;
                    for (AgentFinancialDTO agentFinancialDTO : agentFinancialDTOList) {
                        // 判断客户档案的启停用状态
                        merchantByIdAndOrg =  merchantService.getMerchantById(agentFinancialDTO.getMerchantId(), new String[]{"detailStopStatus"});
                        if (merchantByIdAndOrg == null || merchantByIdAndOrg.getDetailStopStatus()) {
                            continue;
                        }
                        // 先查询当前资金组织是否在客户分配关系内，若分配给当前资金组织，默认取当前资金组织
                        merchantByIdAndOrg = merchantService.getMerchantByIdAndOrg(agentFinancialDTO.getMerchantId(), Long.valueOf(accentity), new String[]{"detailStopStatus", MerchantConstant.ID, MerchantConstant.NAME, ICmpConstant.ORGID});
                        if (merchantByIdAndOrg != null && (merchantByIdAndOrg.getDetailStopStatus() == null || !merchantByIdAndOrg.getDetailStopStatus())) {
                            merchantResult.setAgentFinancialDTO(agentFinancialDTO);
                            merchantResult.setOrgId(accentity);
                            return merchantResult;
                        } else {
                            MerchantApplyRangeQryDTO merchantApplyRangeQryDTO = new MerchantApplyRangeQryDTO();
                            merchantApplyRangeQryDTO.setMerchantId(agentFinancialDTO.getMerchantId());
                            merchantApplyRangeQryDTO.setRangeType(Integer.valueOf(1));
                            merchantApplyRangeQryDTO.setFields(new String [] {"orgId"});
                            List<MerchantApplyRangeDTO> merchantApplyRangeDTOS = merchantService.listMerchantApplyRange(merchantApplyRangeQryDTO);
                            for (MerchantApplyRangeDTO merchantApplyRangeDTO : merchantApplyRangeDTOS) {
                                if (orgSet.contains(merchantApplyRangeDTO.getOrgId())) {
                                    merchantResult.setAgentFinancialDTO(agentFinancialDTO);
                                    merchantResult.setOrgId(merchantApplyRangeDTO.getOrgId());
                                    return merchantResult;
                                }
                            }
                        }

                        // 若当前资金组织不在客户分配关系内，则找企业级的
                        merchantByIdAndOrg = merchantService.getMerchantByIdAndOrg(agentFinancialDTO.getMerchantId(), Long.valueOf(IStwbConstantForCmp.GLOBAL_ACCENTITY), new String[]{"detailStopStatus", MerchantConstant.ID, MerchantConstant.NAME, ICmpConstant.ORGID});
                        if (merchantByIdAndOrg != null && (merchantByIdAndOrg.getDetailStopStatus() == null || !merchantByIdAndOrg.getDetailStopStatus().booleanValue())) {
                            merchantResult.setAgentFinancialDTO(agentFinancialDTO);
                            merchantResult.setOrgId(accentity);
                            return merchantResult;
                        }
                    }
            }

         }
        } catch (Exception e) {
            if(log.isInfoEnabled()) {
                log.info("MerchantUtils--queryCust--exception:", e);
            }
            merchantResult.setCode(500);
            merchantResult.setMessage(e.getMessage());
        }
        return merchantResult;
    }

    /**
     * 保存客户
     *
     * @param merchantObj
     * @return
     */
    public static MerchantResult saveCust(CtmJSONObject merchantObj) {
        MerchantResult merchantResult = new MerchantResult();
        try {
            // 保存客商参数 必填校验
            MerchantUtils.checkParam4SaveCust(merchantObj);

            // 先查询客户分类
            BillDataDto data = new BillDataDto();
            data.setFullname(MerchantConstant.AA_CUSTCATEGORY_CUSTCATEGORY);
            data.setData(ICmpConstant.SELECT_TOTAL_PARAM);
            FilterVO condition = new FilterVO();
            condition.setIsExtend(true);
            SimpleFilterVO simpleVO = new SimpleFilterVO();
            simpleVO.setField(MerchantConstant.CODE);
            simpleVO.setOp(ICmpConstant.QUERY_EQ);

            simpleVO.setValue1(merchantObj.getString(MerchantConstant.CUSTOMERCLASS_NAME));
            SimpleFilterVO[] simpleVOs = {simpleVO};
            condition.setSimpleVOs(simpleVOs);
            data.setCondition(condition);
            merchantResult = MerchantUtils.operateMerchant(data, MerchantOperateEnum.QUERY);
            if (merchantResult.getCode() != 200) {
                merchantResult.setCode(500);
                merchantResult.setMessage(merchantResult.getMessage());
                merchantResult.setCaObject(CaObject.Customer);
                return merchantResult;
            }
            if (merchantResult.getData() != null
                    && CtmJSONArray.parseArray(CtmJSONObject.toJSONString(merchantResult.getData())).isEmpty()) {
                // 不存在则同步客户分类
                CtmJSONObject custObject = new CtmJSONObject();
                custObject.put(MerchantConstant.NAME, com.yonyou.iuap.ucf.common.i18n.MessageUtils
                        .getMessage(MerchantConstant.TEMP_CUSTOMER_CLASS_NAME));
                custObject.put(MerchantConstant.CODE, merchantObj.getString(MerchantConstant.CUSTOMERCLASS_NAME));
                custObject.put(MerchantConstant.ERPCODE, merchantObj.getString(MerchantConstant.CUSTOMERCLASS_NAME));
                custObject.put(MerchantConstant.ISENABLED, MerchantConstant.TRUE);
                custObject.put(MerchantConstant.STATUS, MerchantConstant.INSERT);
                BillDataDto custBillData = new BillDataDto();
                custBillData.setBillnum(MerchantConstant.AA_CUSTCATEGORY);
                custBillData.setData(custObject);
                merchantResult = MerchantUtils.operateMerchant(custBillData, MerchantOperateEnum.SAVE);
            }

            BillDataDto billDataDto = new BillDataDto();
            billDataDto.setBillnum(MerchantConstant.AA_MERCHANT);
            billDataDto.setData(merchantObj);
            merchantResult = MerchantUtils.operateMerchant(billDataDto, MerchantOperateEnum.SAVE);
            if (merchantResult.getCode() != 200) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100280"),merchantResult.getMessage());
            }
            CtmJSONArray msgArray = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(merchantResult.getData()))
                    .getJSONArray(MerchantConstant.MESSAGES);
            if (!msgArray.isEmpty()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100281"),msgArray.getString(0));
            }
        } catch (Exception e) {
            if(log.isInfoEnabled()) {
                log.info("MerchantUtils--saveCust--exception:", e);
            }
            merchantResult.setCode(500);
            merchantResult.setMessage(e.getMessage());
        }
        merchantResult.setCaObject(CaObject.Customer);
        return merchantResult;
    }

    /**
     * 客商保存，参数校验
     *
     * @param merchantObj
     * @throws Exception
     */
    public static void checkParam4SaveCust(CtmJSONObject merchantObj) throws Exception {
        Preconditions.checkNotNull(merchantObj,
                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803DD","参数不能为空") /* "参数不能为空" */);
        Preconditions.checkNotNull(merchantObj.getString(MerchantConstant.CREATEORG),
                com.yonyou.iuap.ucf.common.i18n.MessageUtils
                        .getMessage("P_YS_FI_CM_0000026275") /* "创建组织createOrg不能为空" */);
        Preconditions.checkNotNull(merchantObj.getString(MerchantConstant.CREATEORG), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803DE","所属组织belongOrg不能为空") /* "所属组织belongOrg不能为空" */);
        Preconditions.checkNotNull(merchantObj.getString(MerchantConstant.CUSTOMERCLASS), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803DF","客户分类不能为空") /* "客户分类不能为空" */);
        Preconditions.checkNotNull(merchantObj.getString(MerchantConstant.CODE),
                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803E1","客户编码code不能为空") /* "客户编码code不能为空" */);
        Preconditions.checkNotNull(merchantObj.getString(MerchantConstant.NAME),
                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803E2","客户名称name不能为空") /* "客户名称name不能为空" */);

        // 银行信息节点判断
        if (merchantObj.get(MerchantConstant.MERCHANTAGENTFINANCIALINFOS) != null
                && !merchantObj.getJSONArray(MerchantConstant.MERCHANTAGENTFINANCIALINFOS).isEmpty()) {
            CtmJSONObject financialObj = merchantObj.getJSONArray(MerchantConstant.MERCHANTAGENTFINANCIALINFOS)
                    .getJSONObject(0);
            Preconditions.checkNotNull(financialObj.getString(MerchantConstant.COUNTRY),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803E4","国家不能为空") /* "国家不能为空" */);
            Preconditions.checkNotNull(financialObj.getString(MerchantConstant.CURRENCY),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803E5","币种不能为空") /* "币种不能为空" */);
            Preconditions.checkNotNull(financialObj.getString(MerchantConstant.ACCOUNTTYPE),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803E6","账户类型不能为空") /* "账户类型不能为空" */);
            Preconditions.checkNotNull(financialObj.getString(MerchantConstant.BANK),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803E7","银行类别不能为空") /* "银行类别不能为空" */);
            Preconditions.checkNotNull(financialObj.getString(MerchantConstant.OPENBANK),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803E8","银行网点不能为空") /* "银行网点不能为空" */);
            Preconditions.checkNotNull(financialObj.getString(MerchantConstant.BANKACCOUNT),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D7","银行账号不能为空") /* "银行账号不能为空" */);
            Preconditions.checkNotNull(financialObj.getString(MerchantConstant.BANKACCOUNTNAME),
                    com.yonyou.iuap.ucf.common.i18n.MessageUtils
                            .getMessage("P_YS_FI_CM_0000026120") /* "银行账户名称不能为空" */);

        }
    }

    /**
     * 查询供应商
     *
     * @param accentity
     * @param accountName
     * @param account
     * @return
     */
    public static MerchantResult queryVendor(String accentity, String accountName, String account) {
        MerchantResult merchantResult = new MerchantResult();
        merchantResult.setCode(200);
        try {
            Set<String> orgSet = MerchantUtils.getOrgByAccentity(accentity);
            VendorQueryService vendorQueryService = AppContext.getBean(VendorQueryService.class);
            Map<String, Object> accCondition = new HashMap<>();
            if(!StringUtils.isEmpty(account)){
                accCondition.put("account", account);
            }
            accCondition.put("stopstatus", "0");
            if (!StringUtils.isEmpty(accountName)) {
                accCondition.put("accountname", accountName);
            }
            List<VendorBankVO> vendorBanksByCondition = vendorQueryService.getVendorBanksByCondition(accCondition);
            if (vendorBanksByCondition.isEmpty()) {
                return merchantResult;
            }
            //大北农需求（2611001828SR24091904173），设置yms 参数，如果是精准本组织匹配=true，过滤掉分配停用的供应商和客户
            boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
            if (isAccuratematching) {
               // 根据供应商id查询供应商分配关系
               IVendorPubQueryService vendorPubQueryService = AppContext.getBean(IVendorPubQueryService.class);
               int m=0;
               for (VendorBankVO vendorBank : vendorBanksByCondition) {
                   // 优先判断供应商与当前资金组织是否存在分配关系(去掉停用的)
                   VendorExtendVO vendorExtendVO = vendorPubQueryService.getVendorExtendFieldByVendorIdAndOrgIdV2(vendorBank.getVendor(), accentity);
                   // 停用状态的跳过
                   if (vendorExtendVO == null || vendorExtendVO.getStopstatus()) {
                       continue;
                   }
                   //因供应商银行账户返回值无供应商名称，查询供应商档案
                   VendorVO vendorById = vendorQueryService.getVendorById(vendorBank.getVendor());
                   merchantResult.setVendorBankVO(vendorBank);
                   merchantResult.setOrgId(accentity);
                   merchantResult.setVendorName(vendorById.getName());
                   m++;
               }
                if ( m > 1) {
                    merchantResult.setVendorBankVO(null);
                    merchantResult.setOrgId(null);
                    merchantResult.setVendorName(null);
                    // 匹配多个供应商 返回201
                    merchantResult.setCode(201);
                }
               return merchantResult;
           } else {
               // 根据供应商id查询供应商分配关系
               IVendorPubQueryService vendorPubQueryService = AppContext.getBean(IVendorPubQueryService.class);
               List<String> selectFields = new ArrayList<>();
               // 启停用状态
               selectFields.add("stopstatus");
               // 供应商名称
               selectFields.add("vendor.name");
               for (VendorBankVO vendorBank : vendorBanksByCondition) {
                   // 查询档案的基本信息
                   VendorExtendVO vendorExtendVO = vendorPubQueryService.getVendorExtendFieldByVendorIdAndOrgIdV2(vendorBank.getVendor(), null, selectFields.toArray(new String[0]));
                   // 停用状态的跳过
                   if (vendorExtendVO == null || vendorExtendVO.getStopstatus()) {
                       continue;
                   }
                   List<Long> vendorIds = new ArrayList<>();
                   vendorIds.add(vendorBank.getVendor());
                   List<VendorVO> vendorExtendVOList = vendorPubQueryService.getVendorFieldByIdListAndApplyRangeOrg(vendorIds, accentity, false);
                   if (CollectionUtils.isNotEmpty(vendorExtendVOList)) {
                       merchantResult.setVendorBankVO(vendorBank);
                       merchantResult.setOrgId(accentity);
                       merchantResult.setVendorName(vendorExtendVO.get("vendor_name").toString());
                       return merchantResult;
                   } else {
                       List<VendorOrgVO> vendorOrgByVendorIdList = vendorQueryService.getVendorOrgByVendorId(vendorBank.getVendor());
                       if (!vendorOrgByVendorIdList.isEmpty()) {
                           for (VendorOrgVO vendorOrgVO : vendorOrgByVendorIdList) {
                               if (orgSet.contains(vendorOrgVO.getOrg())) {
                                   VendorVO vendorVO = vendorQueryService.getVendorById(vendorBank.getVendor());
                                   merchantResult.setVendorBankVO(vendorBank);
                                   merchantResult.setOrgId(vendorOrgVO.getOrg());
                                   merchantResult.setVendorName(vendorVO.getName());
                                   return merchantResult;
                               }
                           }
                       }
                   }
                   // 查询企业账号级
                   if (vendorPubQueryService.judgeVendorOrgV2(vendorBank.getVendor(), IStwbConstantForCmp.GLOBAL_ACCENTITY)) {
                       merchantResult.setVendorBankVO(vendorBank);
                       merchantResult.setOrgId(accentity);
                       merchantResult.setVendorName(vendorExtendVO.get("vendor_name") == null ? null : vendorExtendVO.get("vendor_name").toString());
                       return merchantResult;
                   }
               }
           }

        } catch (Exception e) {
            log.error("MerchantUtils--queryVender--exception:{}", e.getMessage(), e);
            merchantResult.setCode(500);
            merchantResult.setMessage(e.getMessage());
        }
        return merchantResult;
    }


    /**
     * 查询供应商
     *
     * @param accentity
     * @param accountName
     * @param account
     * @return
     */
    public static MerchantResult queryVendorForCheck(String accentity, String accountName, String account, String mark) {
        MerchantResult merchantResult = new MerchantResult();
        merchantResult.setCode(200);
        try {
            Set<String> orgSet = MerchantUtils.getOrgByAccentity(accentity);
            VendorQueryService vendorQueryService = AppContext.getBean(VendorQueryService.class);
            Map<String, Object> accCondition = new HashMap<>();
            accCondition.put("stopstatus", "0");
            if ("1".equals( mark) || "2".equals( mark)){
                if(!StringUtils.isEmpty(account)){
                    accCondition.put("account", account);
                }
            }
            if ("1".equals(mark) || "3".equals( mark)){
                if (!StringUtils.isEmpty(accountName)) {
                    accCondition.put("accountname", accountName);
                }
            }
            List<VendorBankVO> vendorBanksByCondition = vendorQueryService.getVendorBanksByCondition(accCondition);
            if (vendorBanksByCondition.isEmpty()) {
                return merchantResult;
            }
            //大北农需求（2611001828SR24091904173），设置yms 参数，如果是精准本组织匹配=true，过滤掉分配停用的供应商和客户
            boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
            if (isAccuratematching) {
                // 根据供应商id查询供应商分配关系
                IVendorPubQueryService vendorPubQueryService = AppContext.getBean(IVendorPubQueryService.class);
                int m=0;
                for (VendorBankVO vendorBank : vendorBanksByCondition) {
                    // 优先判断供应商与当前资金组织是否存在分配关系(去掉停用的)
                    VendorExtendVO vendorExtendVO = vendorPubQueryService.getVendorExtendFieldByVendorIdAndOrgIdV2(vendorBank.getVendor(), accentity);
                    if (vendorExtendVO!=null) {
                        //因供应商银行账户返回值无供应商名称，查询供应商档案
                        VendorVO vendorById = vendorQueryService.getVendorById(vendorBank.getVendor());
                        merchantResult.setVendorBankVO(vendorBank);
                        merchantResult.setOrgId(accentity);
                        merchantResult.setVendorName(vendorById.getName());
                        m++;

                    }
                }
                if ( m > 1) {
                    merchantResult.setVendorBankVO(null);
                    merchantResult.setOrgId(null);
                    merchantResult.setVendorName(null);
                    // 匹配多个供应商 返回201
                    merchantResult.setCode(201);
                }
                return merchantResult;
            } else {
                // 根据供应商id查询供应商分配关系
                IVendorPubQueryService vendorPubQueryService = AppContext.getBean(IVendorPubQueryService.class);
                List<String> selectFields = new ArrayList<>();
                // 启停用状态
                selectFields.add("stopstatus");
                // 供应商名称
                selectFields.add("vendor.name");
                for (VendorBankVO vendorBank : vendorBanksByCondition) {
                    // 查询档案的基本信息
                    VendorExtendVO vendorExtendVO = vendorPubQueryService.getVendorExtendFieldByVendorIdAndOrgIdV2(vendorBank.getVendor(), null, selectFields.toArray(new String[0]));
                    // 停用状态的跳过
                    if (vendorExtendVO == null || vendorExtendVO.getStopstatus()) {
                        continue;
                    }
                    List<Long> vendorIds = new ArrayList<>();
                    vendorIds.add(vendorBank.getVendor());
                    List<VendorVO> vendorExtendVOList = vendorPubQueryService.getVendorFieldByIdListAndApplyRangeOrg(vendorIds, accentity, false);
                    if (CollectionUtils.isNotEmpty(vendorExtendVOList)) {
                        merchantResult.setVendorBankVO(vendorBank);
                        merchantResult.setOrgId(accentity);
                        merchantResult.setVendorName(vendorExtendVO.get("vendor_name").toString());
                        return merchantResult;
                    } else {
                        List<VendorOrgVO> vendorOrgByVendorIdList = vendorQueryService.getVendorOrgByVendorId(vendorBank.getVendor());
                        if (!vendorOrgByVendorIdList.isEmpty()) {
                            for (VendorOrgVO vendorOrgVO : vendorOrgByVendorIdList) {
                                if (orgSet.contains(vendorOrgVO.getOrg())) {
                                    VendorVO vendorVO = vendorQueryService.getVendorById(vendorBank.getVendor());
                                    merchantResult.setVendorBankVO(vendorBank);
                                    merchantResult.setOrgId(vendorOrgVO.getOrg());
                                    merchantResult.setVendorName(vendorVO.getName());
                                    return merchantResult;
                                }
                            }
                        }
                    }
                    // 查询企业账号级
                    if (vendorPubQueryService.judgeVendorOrgV2(vendorBank.getVendor(), IStwbConstantForCmp.GLOBAL_ACCENTITY)) {
                        merchantResult.setVendorBankVO(vendorBank);
                        merchantResult.setOrgId(accentity);
                        merchantResult.setVendorName(vendorExtendVO.get("vendor_name") == null ? null : vendorExtendVO.get("vendor_name").toString());
                        return merchantResult;
                    }
                }
            }

        } catch (Exception e) {
            log.error("MerchantUtils--queryVender--exception:{}", e.getMessage(), e);
            merchantResult.setCode(500);
            merchantResult.setMessage(e.getMessage());
        }
        return merchantResult;
    }

    /**
     * 保存供应商
     *
     * @param vendorObj
     * @return
     */
    public static MerchantResult saveVendor(CtmJSONObject vendorObj) {
        MerchantResult merchantResult = new MerchantResult();
        try {
            Preconditions.checkNotNull(vendorObj, com.yonyou.iuap.ucf.common.i18n.MessageUtils
                    .getMessage("P_YS_PF_PROCENTER_0000023470") /* "参数不能为空" */);
            Preconditions.checkNotNull(vendorObj.getString(MerchantConstant.ORG),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803E0","组织org不能为空") /* "组织org不能为空" */);
            Preconditions.checkNotNull(vendorObj.getString(MerchantConstant.CODE),
                    com.yonyou.iuap.ucf.common.i18n.MessageUtils
                            .getMessage("P_YS_FI_CM_0000026192") /* "编码code不能为空" */);
            Preconditions.checkNotNull(vendorObj.getString(MerchantConstant.NAME),
                    com.yonyou.iuap.ucf.common.i18n.MessageUtils
                            .getMessage("P_YS_FI_CM_0000026325") /* "名称name不能为空" */);

            // 先查询供应商分类
            BillDataDto data = new BillDataDto();
            data.setFullname(MerchantConstant.AA_VENDORCLASS_VENDORCLASS);
            data.setData(ICmpConstant.SELECT_TOTAL_PARAM);
            FilterVO condition = new FilterVO();
            condition.setIsExtend(true);
            SimpleFilterVO simpleVO = new SimpleFilterVO();
            simpleVO.setField(MerchantConstant.CODE);
            simpleVO.setOp(ICmpConstant.QUERY_EQ);

            simpleVO.setValue1(vendorObj.getString(MerchantConstant.VENDORCLASS_NAME));
            SimpleFilterVO[] simpleVOs = {simpleVO};
            condition.setSimpleVOs(simpleVOs);
            data.setCondition(condition);

            merchantResult = MerchantUtils.operateMerchant(data, MerchantOperateEnum.QUERY);
            if (merchantResult.getCode() != 200) {
                merchantResult.setCode(500);
                merchantResult.setMessage(merchantResult.getMessage());
                merchantResult.setCaObject(CaObject.Supplier);
                return merchantResult;
            }
            if (merchantResult.getData() != null
                    && CtmJSONArray.parseArray(CtmJSONObject.toJSONString(merchantResult.getData())).isEmpty()) {
                // 不存在则同步供应商分类
                CtmJSONObject vendorObject = new CtmJSONObject();
                vendorObject.put(MerchantConstant.NAME, com.yonyou.iuap.ucf.common.i18n.MessageUtils
                        .getMessage(MerchantConstant.TEMP_VENDOR_CLASS_NAME));
                vendorObject.put(MerchantConstant.CODE, vendorObj.getString(MerchantConstant.VENDORCLASS_NAME));
                vendorObject.put(MerchantConstant.ERPCODE, vendorObj.getString(MerchantConstant.VENDORCLASS_NAME));
                vendorObject.put(MerchantConstant.ISENABLED, MerchantConstant.TRUE);
                vendorObject.put(MerchantConstant.STATUS, MerchantConstant.INSERT);
                BillDataDto vendorBillData = new BillDataDto();
                vendorBillData.setBillnum(MerchantConstant.AA_VENDORCLASSIFICATION);
                vendorBillData.setData(vendorObject);
                merchantResult = MerchantUtils.operateMerchant(vendorBillData, MerchantOperateEnum.SAVE);
            }

            if (vendorObj.get(MerchantConstant.VENDORBANKS) != null
                    && !vendorObj.getJSONArray(MerchantConstant.VENDORBANKS).isEmpty()) {
                CtmJSONObject vendorBankObj = vendorObj.getJSONArray(MerchantConstant.VENDORBANKS).getJSONObject(0);
                Preconditions.checkNotNull(vendorBankObj.getString(MerchantConstant.COUNTRY),
                        com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                .getMessage("P_YS_FI_CM_0000026067") /* "国家country不能为空" */);
                Preconditions.checkNotNull(vendorBankObj.getString(MerchantConstant.CURRENCY),
                        com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                .getMessage("P_YS_FI_CM_0000026270") /* "币种currency不能为空" */);
                Preconditions.checkNotNull(vendorBankObj.getString(MerchantConstant.BANK),
                        com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                .getMessage("P_YS_FI_CM_0000026226") /* "银行类别bank不能为空" */);
                Preconditions.checkNotNull(vendorBankObj.getString(MerchantConstant.OPENACCOUNTBANK),
                        com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                .getMessage("P_YS_FI_CM_0000026254") /* "银行网点openaccountbank不能为空" */);
                Preconditions.checkNotNull(vendorBankObj.getString(MerchantConstant.ACCOUNT),
                        com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                .getMessage("P_YS_FI_CM_0000026031") /* "银行账号account不能为空" */);
                Preconditions.checkNotNull(vendorBankObj.getString(MerchantConstant.ACCOUNTNAME),
                        com.yonyou.iuap.ucf.common.i18n.MessageUtils
                                .getMessage("P_YS_FI_CM_0000026312") /* "银行账号名称accountname不能为空" */);

            }

            BillDataDto billDataDto = new BillDataDto();
            billDataDto.setBillnum(MerchantConstant.AA_VENDOR);
            billDataDto.setData(vendorObj);
            merchantResult = MerchantUtils.operateMerchant(billDataDto, MerchantOperateEnum.SAVE);
            if (merchantResult.getCode() != 200) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100282"),merchantResult.getMessage());
            }
            CtmJSONArray msgArray = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(merchantResult.getData()))
                    .getJSONArray(MerchantConstant.MESSAGES);
            if (!msgArray.isEmpty()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100283"),msgArray.getString(0));
            }
        } catch (Exception e) {
            if(log.isInfoEnabled()) {
                log.info("MerchantUtils--saveVender--exception:", e);
            }
            merchantResult.setCode(500);
            merchantResult.setMessage(e.getMessage());
        }
        merchantResult.setCaObject(CaObject.Supplier);
        return merchantResult;
    }

    /**
     * 银行类别和银行网点同步
     *
     * @param bankTypeName 银行类别名
     * @param bankDotName  银行网点名
     * @return
     */
    public static Map<String, Object> synBankDotAndType(String bankTypeName, String bankDotName) {
        Map<String, Object> resultMap = new HashMap<>();
        try {
//            RpcContext.getContext().setAttachment(MerchantConstant.RPCTOKEN, AppContext.getToken());
            UCFBasedocBankDubboService billService = (UCFBasedocBankDubboService) DubboReference.getInstance()
                    .getReference(UCFBasedocBankDubboService.class, IDomainConstant.MDD_DOMAIN_UCFBASEDOC, null);
            Map<String, Object> map = new HashMap<>();
            map.put(MerchantConstant.BANKTYPENAME, bankTypeName);
            map.put(MerchantConstant.BANKDOTNAME, bankDotName);
            resultMap = billService.queryBankExist(map);
        } catch (Exception e) {
            if(log.isInfoEnabled()) {
                log.info("MerchantUtils--synBankDotAndType--exception:", e);
            }
        }
        return resultMap;
    }

    /**
     * 银行网点名 获取银行类别和银行网点信息
     *
     * @param bankDotName 银行网点名
     * @return
     * @throws Exception
     */
    public static Map<String, Object> getBankInfo(String bankDotName) throws Exception {
//        RpcContext.getContext().setAttachment(MerchantConstant.RPCTOKEN, AppContext.getToken());
        UCFBasedocBankDubboService billService = (UCFBasedocBankDubboService) DubboReference.getInstance()
                .getReference(UCFBasedocBankDubboService.class, IDomainConstant.MDD_DOMAIN_UCFBASEDOC, null);
        Map<String, Object> map = new HashMap<>();
        map.put(MerchantConstant.BANKDOTNAME, bankDotName);
        Map<String, Object> bankInfoMap = null;
        try {
            bankInfoMap = billService.queryBankByName(map);
        } catch (Exception e) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540062C", "通过对方开户行名查找银行网点失败，请维护对应的银行网点信息。错误信息：") /* "通过对方开户行名查找银行网点失败，请维护对应的银行网点信息。错误信息：" */ + e.getMessage(), e);
        }
        if (MapUtils.isEmpty(bankInfoMap)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540062B", "通过对方开户行名查找银行网点失败，请维护对应的银行网点信息。") /* "通过对方开户行名查找银行网点失败，请维护对应的银行网点信息。" */);
        }

        Map<String, Object> resultMap = new HashMap<>();
        if (MapUtils.isNotEmpty(bankInfoMap)) {
            resultMap.put(MerchantConstant.BANKTYPENAME, bankInfoMap.get(MerchantConstant.NAME));
            resultMap.put(MerchantConstant.BANKTYPEID, bankInfoMap.get(MerchantConstant.ID));
            CtmJSONObject bankDotObj = CtmJSONObject.parseObject(CtmJSONObject.toJSONString(bankInfoMap.get(MerchantConstant.BANKDOT)));
            resultMap.put(MerchantConstant.BANKDOTID, bankDotObj.getString(MerchantConstant.ID));
            resultMap.put(MerchantConstant.BANKDOTNAME, bankDotObj.getString(MerchantConstant.NAME));
        }
        return resultMap;
    }

    /**
     * 判断是否存在客商档案
     *
     * @param detail
     * @return
     */
    public static boolean dealMerchantFlag(BankDealDetail detail) {
        try {
            if (detail == null || StringUtils.isEmpty(detail.getAccentity())
                    || StringUtils.isEmpty(detail.getTo_acct_name()) || StringUtils.isEmpty(detail.getTo_acct_no())) {
                return false;
            }
            // 判断客户是否存在
            MerchantResult queryCust = MerchantUtils.queryCust(detail.getAccentity(), detail.getTo_acct_name(),
                    detail.getTo_acct_no());
            if (queryCust.getCode() != 200) {
                return false;
            }
            if (queryCust.getAgentFinancialDTO() != null) {
                detail.setMerchant_flag(MerchantFlag.EXIST);
                return true;
            }
            // 判断供应商是否存在
            MerchantResult queryVendor = MerchantUtils.queryVendor(detail.getAccentity(), detail.getTo_acct_name(),
                    detail.getTo_acct_no());
            if (queryVendor.getCode() != 200) {
                return false;
            }
            if (queryVendor.getVendorBankVO() != null) {
                detail.setMerchant_flag(MerchantFlag.EXIST);
                return true;
            }
            detail.setMerchant_flag(MerchantFlag.NOT_EXIST);
            return true;
        } catch (Exception e) {
            if(log.isInfoEnabled()) {
                log.info("dealMerchantFlag-exception", e);
            }
            return false;
        }
    }

    /**
     * 检查客户是否存在
     *
     * @param requst
     * @return
     * @throws Exception
     */
    public static CtmJSONObject cust2Check(MerchantRequst requst) throws Exception {
        CtmJSONObject obj = new CtmJSONObject();
        Preconditions.checkNotNull(requst.getAccentity(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050000", "资金组织不能为空") /* "资金组织不能为空" */);
        if (requst.isAccNameMust()) {
            Preconditions.checkNotNull(requst.getAccName(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D9","银行账户名称不能为空") /* "银行账户名称不能为空" */);
        }
        Preconditions.checkNotNull(requst.getAccNo(),
                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D7","银行账号不能为空") /* "银行账号不能为空" */);
        // 查询客户名称是否存在
        MerchantResult queryCust = MerchantUtils.queryCust(requst.getAccentity(), requst.getAccName(), requst.getAccNo());
        // 大北农匹配到多个供应商，不再匹配，直接返回【其他】
        if (queryCust.getCode() == 201) {
            obj.put(MerchantConstant.OTHERFLAG, MerchantConstant.TRUE);
            return obj;
        }
        if (queryCust.getCode() != 200) {
            if(log.isInfoEnabled()) {
                log.info("cust2Check----exception:", JsonUtils.toJSON(queryCust));
            }
            obj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
            return obj;
        }

        if (queryCust.getAgentFinancialDTO() == null) {
            obj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
            return obj;
        }
        obj.put(MerchantConstant.CUSTOMERID, queryCust.getAgentFinancialDTO().getMerchantId());
        obj.put(MerchantConstant.NAME, queryCust.getAgentFinancialDTO().getMerchantName());
        obj.put(ICmpConstant.ORGID, queryCust.getOrgId());
        obj.put(MerchantConstant.CUSTOMERBANKID, queryCust.getAgentFinancialDTO().getId());
        obj.put(MerchantConstant.BANKACCOUNTNAME, queryCust.getAgentFinancialDTO().getBankAccountName());
        obj.put(MerchantConstant.BANKACCOUNT, queryCust.getAgentFinancialDTO().getBankAccount());
        obj.put(MerchantConstant.JOINTLINENO, queryCust.getAgentFinancialDTO().getJointLineNo());
        obj.put(MerchantConstant.OPENBANK, queryCust.getAgentFinancialDTO().getOpenBank());
        obj.put(MerchantConstant.STOPSTATUS, queryCust.getAgentFinancialDTO().getStopStatus());
        obj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.TRUE);
        return obj;
    }
    /**
     * 检查客户是否存在
     *
     * @param requst
     * @return
     * @throws Exception
     */
    public static CtmJSONObject cust2CheckForCheck(MerchantRequst requst, String mark) throws Exception {
        CtmJSONObject obj = new CtmJSONObject();
        Preconditions.checkNotNull(requst.getAccentity(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050000", "资金组织不能为空") /* "资金组织不能为空" */);
        if ("1".equals(mark) || "2".equals(mark)){
            Preconditions.checkNotNull(requst.getAccNo(),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D7","银行账号不能为空") /* "银行账号不能为空" */);
        }
        if ("1".equals(mark) || "3".equals(mark)){
            if (requst.isAccNameMust()) {
                Preconditions.checkNotNull(requst.getAccName(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D9","银行账户名称不能为空") /* "银行账户名称不能为空" */);
            }
        }

        // 查询客户名称是否存在
        MerchantResult queryCust = MerchantUtils.queryCustForCheck(requst.getAccentity(), requst.getAccName(), requst.getAccNo(), mark);
        // 大北农匹配到多个供应商，不再匹配，直接返回【其他】
        if (queryCust.getCode() == 201) {
            obj.put(MerchantConstant.OTHERFLAG, MerchantConstant.TRUE);
            return obj;
        }
        if (queryCust.getCode() != 200) {
            if(log.isInfoEnabled()) {
                log.info("cust2Check----exception:", JsonUtils.toJSON(queryCust));
            }
            obj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
            return obj;
        }

        if (queryCust.getAgentFinancialDTO() == null) {
            obj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
            return obj;
        }
        obj.put(MerchantConstant.CUSTOMERID, queryCust.getAgentFinancialDTO().getMerchantId());
        obj.put(MerchantConstant.NAME, queryCust.getAgentFinancialDTO().getMerchantName());
        obj.put(ICmpConstant.ORGID, queryCust.getOrgId());
        obj.put(MerchantConstant.CUSTOMERBANKID, queryCust.getAgentFinancialDTO().getId());
        obj.put(MerchantConstant.BANKACCOUNTNAME, queryCust.getAgentFinancialDTO().getBankAccountName());
        obj.put(MerchantConstant.BANKACCOUNT, queryCust.getAgentFinancialDTO().getBankAccount());
        obj.put(MerchantConstant.JOINTLINENO, queryCust.getAgentFinancialDTO().getJointLineNo());
        obj.put(MerchantConstant.OPENBANK, queryCust.getAgentFinancialDTO().getOpenBank());
        obj.put(MerchantConstant.STOPSTATUS, queryCust.getAgentFinancialDTO().getStopStatus());
        obj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.TRUE);
        return obj;
    }

    /**
     * 检查供应商是否存在
     *
     * @param requst
     * @return
     * @throws Exception
     */
    public static CtmJSONObject vendor2Check(MerchantRequst requst)
            throws Exception {
        CtmJSONObject obj = new CtmJSONObject();
        Preconditions.checkNotNull(requst.getAccentity(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050000", "资金组织不能为空") /* "资金组织不能为空" */);
        if (requst.isAccNameMust()) {
            Preconditions.checkNotNull(requst.getAccName(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D9","银行账户名称不能为空") /* "银行账户名称不能为空" */);
        }
        Preconditions.checkNotNull(requst.getAccNo(),
                com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D7","银行账号不能为空") /* "银行账号不能为空" */);
        // 查询供应商是否存在
        MerchantResult queryVendor = MerchantUtils.queryVendor(requst.getAccentity(), requst.getAccName(), requst.getAccNo());
        // 大北农匹配到多个供应商，不再匹配，直接返回【其他】
        if (queryVendor.getCode() == 201) {
            obj.put(MerchantConstant.OTHERFLAG, MerchantConstant.TRUE);
            return obj;
        }
        if (queryVendor.getCode() != 200) {
            log.error("vendor2Check----exception:{}", JsonUtils.toJSON(queryVendor));
            obj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
            return obj;
        }

        if (queryVendor.getVendorBankVO() == null) {
            obj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
            return obj;
        }
        obj.put(MerchantConstant.VENDORID, queryVendor.getVendorBankVO().getVendor());
        obj.put(MerchantConstant.NAME, queryVendor.getVendorName());
        obj.put(ICmpConstant.ORGID, queryVendor.getOrgId());
        obj.put(MerchantConstant.VENDORBANKID, queryVendor.getVendorBankVO().getId());
        obj.put(MerchantConstant.ACCOUNTNAME, queryVendor.getVendorBankVO().getAccountname());
        // 账户id
        obj.put(MerchantConstant.ACCOUNT, queryVendor.getVendorBankVO().getAccount());
        obj.put(MerchantConstant.CORRESPONDENTCODE, queryVendor.getVendorBankVO().getCorrespondentcode());
        obj.put(MerchantConstant.OPENBANK, queryVendor.getVendorBankVO().getOpenaccountbank());
        obj.put(MerchantConstant.STOPSTATUS, queryVendor.getVendorBankVO().getStopstatus());
        obj.put(MerchantConstant.VENDORFLAG, MerchantConstant.TRUE);
        return obj;
    }
    /**
     * 检查供应商是否存在
     *
     * @param requst
     * @return
     * @throws Exception
     */
    public static CtmJSONObject vendor2CheckForCheck(MerchantRequst requst,String mark)throws Exception {
        CtmJSONObject obj = new CtmJSONObject();
        Preconditions.checkNotNull(requst.getAccentity(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050000", "资金组织不能为空") /* "资金组织不能为空" */);
        if ("1".equals(mark) || "2".equals(mark)){
            Preconditions.checkNotNull(requst.getAccNo(),
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D7","银行账号不能为空") /* "银行账号不能为空" */);
        }
        if ("1".equals(mark) || "3".equals(mark)){
            if (requst.isAccNameMust()) {
                Preconditions.checkNotNull(requst.getAccName(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D9","银行账户名称不能为空") /* "银行账户名称不能为空" */);
            }
        }

        // 查询供应商是否存在
        MerchantResult queryVendor = MerchantUtils.queryVendorForCheck(requst.getAccentity(), requst.getAccName(), requst.getAccNo(), mark);
        // 大北农匹配到多个供应商，不再匹配，直接返回【其他】
        if (queryVendor.getCode() == 201) {
            obj.put(MerchantConstant.OTHERFLAG, MerchantConstant.TRUE);
            return obj;
        }
        if (queryVendor.getCode() != 200) {
            log.error("vendor2Check----exception:{}", JsonUtils.toJSON(queryVendor));
            obj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
            return obj;
        }

        if (queryVendor.getVendorBankVO() == null) {
            obj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
            return obj;
        }
        obj.put(MerchantConstant.VENDORID, queryVendor.getVendorBankVO().getVendor());
        obj.put(MerchantConstant.NAME, queryVendor.getVendorName());
        obj.put(ICmpConstant.ORGID, queryVendor.getOrgId());
        obj.put(MerchantConstant.VENDORBANKID, queryVendor.getVendorBankVO().getId());
        obj.put(MerchantConstant.ACCOUNTNAME, queryVendor.getVendorBankVO().getAccountname());
        // 账户id
        obj.put(MerchantConstant.ACCOUNT, queryVendor.getVendorBankVO().getAccount());
        obj.put(MerchantConstant.CORRESPONDENTCODE, queryVendor.getVendorBankVO().getCorrespondentcode());
        obj.put(MerchantConstant.OPENBANK, queryVendor.getVendorBankVO().getOpenaccountbank());
        obj.put(MerchantConstant.STOPSTATUS, queryVendor.getVendorBankVO().getStopstatus());
        obj.put(MerchantConstant.VENDORFLAG, MerchantConstant.TRUE);
        return obj;
    }
    /**
     * 检查供应商是否存在
     *
     * @param requst
     * @return
     * @throws Exception
     */
    public static CtmJSONObject vendor2CheckByName(MerchantRequst requst)
            throws Exception {
        CtmJSONObject obj = new CtmJSONObject();
        Preconditions.checkNotNull(requst.getAccentity(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050000", "资金组织不能为空") /* "资金组织不能为空" */);
        if (requst.isAccNameMust()) {
            Preconditions.checkNotNull(requst.getAccName(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D9","银行账户名称不能为空") /* "银行账户名称不能为空" */);
        }
        // 查询供应商是否存在
        MerchantResult queryVendor = MerchantUtils.queryVendor(requst.getAccentity(), requst.getAccName(), requst.getAccNo());
        if (queryVendor.getCode() != 200) {
            if(log.isInfoEnabled()) {
                log.info("vendor2CheckByName----exception:", JsonUtils.toJSON(queryVendor));
            }
            obj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
            return obj;
        }

        if (queryVendor.getVendorBankVO() == null) {
            obj.put(MerchantConstant.VENDORFLAG, MerchantConstant.FALSE);
            return obj;
        }
        obj.put(MerchantConstant.VENDORID, queryVendor.getVendorBankVO().getVendor());
        obj.put(MerchantConstant.NAME, queryVendor.getVendorName());
        obj.put(ICmpConstant.ORGID, queryVendor.getOrgId());
        // 根据账户名称辨识去掉对方账号Id，防止辨识错误
        // obj.put(MerchantConstant.VENDORBANKID, queryVendor.getVendorBankVO().getId());
        obj.put(MerchantConstant.ACCOUNTNAME, queryVendor.getVendorBankVO().getAccountname());
        obj.put(MerchantConstant.ACCOUNT, queryVendor.getVendorBankVO().getAccount());
        obj.put(MerchantConstant.CORRESPONDENTCODE, queryVendor.getVendorBankVO().getCorrespondentcode());
        obj.put(MerchantConstant.OPENBANK, queryVendor.getVendorBankVO().getOpenaccountbank());
        obj.put(MerchantConstant.STOPSTATUS, queryVendor.getVendorBankVO().getStopstatus());
        obj.put(MerchantConstant.VENDORFLAG, MerchantConstant.TRUE);
        return obj;
    }

    /**
     * 资金组织  委托关系查找组织
     *
     * @param accentity
     * @return
     * @throws Exception
     */
    public static Set<String> getOrgByAccentity(String accentity) throws Exception {
        return orgByAccentityCache.get(accentity, () -> {
            Set<String> retorgIds = FIDubboUtils.getDelegateHasSelf(accentity);
            retorgIds = FIDubboUtils.orgMCFilterHasSelf(ORG_ENTITYNAME, retorgIds.toArray(new String[0]));
            retorgIds.add(accentity);
            return retorgIds;
        });
    }

    /**
     * 获取uuid
     *
     * @return
     */
    public static String getUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * 根据户名检查客户是否存在
     *
     * @param requst
     * @return
     * @throws Exception
     */
    public static CtmJSONObject cust2CheckByName(MerchantRequst requst) throws Exception {
        CtmJSONObject obj = new CtmJSONObject();
        Preconditions.checkNotNull(requst.getAccentity(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:YS_CTM_CM-BE_LOCAL_00050000", "资金组织不能为空") /* "资金组织不能为空" */);
        if (requst.isAccNameMust()) {
            Preconditions.checkNotNull(requst.getAccName(), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041803D9","银行账户名称不能为空") /* "银行账户名称不能为空" */);
        }
        // 查询客户名称是否存在
        MerchantResult queryCust = MerchantUtils.queryCust(requst.getAccentity(), requst.getAccName(), requst.getAccNo());
        if (queryCust.getCode() != 200) {
            if(log.isInfoEnabled()) {
                log.info("cust2CheckByName----exception:", JsonUtils.toJSON(queryCust));
            }
            obj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
            return obj;
        }

        if (queryCust.getAgentFinancialDTO() == null) {
            obj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.FALSE);
            return obj;
        }
        obj.put(MerchantConstant.CUSTOMERID, queryCust.getAgentFinancialDTO().getMerchantId());
        obj.put(MerchantConstant.NAME, queryCust.getAgentFinancialDTO().getMerchantName());
        obj.put(ICmpConstant.ORGID, queryCust.getOrgId());
        // 根据账户名称辨识去掉对方账号Id
        // obj.put(MerchantConstant.CUSTOMERBANKID, queryCust.getAgentFinancialDTO().getId());
        obj.put(MerchantConstant.BANKACCOUNTNAME, queryCust.getAgentFinancialDTO().getBankAccountName());
        obj.put(MerchantConstant.BANKACCOUNT, queryCust.getAgentFinancialDTO().getBankAccount());
        obj.put(MerchantConstant.JOINTLINENO, queryCust.getAgentFinancialDTO().getJointLineNo());
        obj.put(MerchantConstant.OPENBANK, queryCust.getAgentFinancialDTO().getOpenBank());
        obj.put(MerchantConstant.STOPSTATUS, queryCust.getAgentFinancialDTO().getStopStatus());
        obj.put(MerchantConstant.CUSTOMERFLAG, MerchantConstant.TRUE);
        return obj;
    }

    /**
     * 判断对方类型的数据是否有变更
     * @param paramData 变更的数据
     * @param dbData 数据库存在的数据
     * @return
     */
    public static boolean checkOppositeIsChanged(BankReconciliation paramData, BankReconciliation dbData) {
        // 对方类型不相同
        if (dbData.getOppositetype() != null && !dbData.getOppositetype().equals(paramData.getOppositetype())
            || paramData.getOppositetype() != null && !paramData.getOppositetype().equals(dbData.getOppositetype())) {
            return true;
        }
        // 对方账号不相同
        if (dbData.getTo_acct_no() != null && !dbData.getTo_acct_no().equals(paramData.getTo_acct_no())
                || paramData.getTo_acct_no() != null && !paramData.getTo_acct_no().equals(dbData.getTo_acct_no())) {
            return true;
        }
        // 对方单位Id不相同
        if (dbData.getOppositeobjectid() != null && !dbData.getOppositeobjectid().equals(paramData.getOppositeobjectid())
               || paramData.getOppositeobjectid() != null && !paramData.getOppositeobjectid().equals(dbData.getOppositeobjectid())) {
            return true;
        }
        return false;
    }
    /**
     * 客户查询
     *
     * @param accentity
     * @param bankAccountName
     * @param bankAccount
     * @return
     */
    public static MerchantResult queryCustForCheck(String accentity, String bankAccountName, String bankAccount, String mark) {
        MerchantResult merchantResult = new MerchantResult();
        merchantResult.setCode(200);
        try {
            Set<String> orgSet = MerchantUtils.getOrgByAccentity(accentity);
            List<AgentFinancialDTO> agentFinancialDTOList = new ArrayList<>();
            // 先根据银行账户，账户名称查询对应的客户银行账户
            IMerchantServiceV2 merchantService = AppContext.getBean(IMerchantServiceV2.class);
            
            // 提取公共字段数组定义，避免重复
            String[] commonFields = new String[]{MerchantConstant.MERCHANTID,MerchantConstant.ID,MerchantConstant.STOPSTATUS,MerchantConstant.BANKACCOUNTNAME,
                        MerchantConstant.JOINTLINENO,MerchantConstant.OPENBANK,"merchantName","bankAccount"};
            
            if ("1".equals(mark) || "2".equals(mark) || "3".equals(mark)){
                AgentFinancialQryDTO agentFinancialQryDTO = new AgentFinancialQryDTO();
                agentFinancialQryDTO.setFields(commonFields);
                
                // 根据不同的mark设置不同的查询条件
                if ("1".equals(mark) || "2".equals(mark)) {
                    agentFinancialQryDTO.setBankAccount(bankAccount);
                } else if ("3".equals(mark)) {
                    agentFinancialQryDTO.setBankAccountName(bankAccountName);
                }
                
                // 启用状态
                agentFinancialQryDTO.setStopStatus(false);
                List<AgentFinancialDTO> agentFinancialDTOS = merchantService.listMerchantAgentFinancial(agentFinancialQryDTO);
                if (agentFinancialDTOS.isEmpty()) {
                    return merchantResult;
                }
                
                // 根据不同mark进行不同的处理
                for (AgentFinancialDTO agentFinancialDTO : agentFinancialDTOS) {
                    // "1" 需要匹配银行账户名称，"2" 不需要匹配，"3" 不涉及银行账户
                    if ("1".equals(mark)) {
                        // 使用StringUtils.isEmpty和StringUtils.equals避免空指针异常
                        if (StringUtils.isNotEmpty(bankAccountName) && Objects.equals(bankAccountName, agentFinancialDTO.getBankAccountName())) {
                            agentFinancialDTOList.add(agentFinancialDTO);
                        }
                    } else {
                        agentFinancialDTOList.add(agentFinancialDTO);
                    }
                }
            } else {
                return merchantResult;
            }

            //大北农需求（2611001828SR24091904173），设置yms 参数，如果是精准本组织匹配=true，过滤掉分配停用的供应商和客户
            boolean isAccuratematching = Boolean.parseBoolean(AppContext.getEnvConfig("cmp.bankreconciliation.isAccuratematching","false"));
            if(isAccuratematching) {
                // 根据客户id查询客户分配关系
                if (!agentFinancialDTOList.isEmpty()) {
                    int m = 0;
                    for (AgentFinancialDTO agentFinancialDTO : agentFinancialDTOList) {
                        // 先查询当前资金组织是否在客户分配关系内，若分配给当前资金组织，默认取当前资金组织
                        try {
                            MerchantDTO merchantByIdAndOrg = merchantService.getMerchantByIdAndOrg(agentFinancialDTO.getMerchantId(), Long.valueOf(accentity), new String[]{"detailStopStatus", MerchantConstant.ID, MerchantConstant.NAME, ICmpConstant.ORGID});
                            if (merchantByIdAndOrg != null && (merchantByIdAndOrg.getDetailStopStatus() == null || !merchantByIdAndOrg.getDetailStopStatus().booleanValue())) {
                                merchantResult.setAgentFinancialDTO(agentFinancialDTO);
                                merchantResult.setOrgId(accentity);
                                m++;
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Invalid accentity format: {}", accentity);
                            merchantResult.setCode(500);
                            merchantResult.setMessage("Invalid accentity format");
                            return merchantResult;
                        }
                    }
                    //同一个组织匹配多条，设置成空,前端匹配不上
                    if(m>1){
                        merchantResult.setAgentFinancialDTO(null);
                        merchantResult.setOrgId(null);
                        // 匹配多个供应商 返回201
                        merchantResult.setCode(201);
                    }
                    return merchantResult;
                }
            }else{
                // 根据客户id查询客户分配关系
                if (CollectionUtils.isNotEmpty(agentFinancialDTOList)) {
                    MerchantDTO merchantByIdAndOrg;
                    for (AgentFinancialDTO agentFinancialDTO : agentFinancialDTOList) {
                        // 判断客户档案的启停用状态
                        merchantByIdAndOrg =  merchantService.getMerchantById(agentFinancialDTO.getMerchantId(), new String[]{"detailStopStatus"});
                        if (merchantByIdAndOrg == null || merchantByIdAndOrg.getDetailStopStatus()) {
                            continue;
                        }
                        // 先查询当前资金组织是否在客户分配关系内，若分配给当前资金组织，默认取当前资金组织
                        try {
                            merchantByIdAndOrg = merchantService.getMerchantByIdAndOrg(agentFinancialDTO.getMerchantId(), Long.valueOf(accentity), new String[]{"detailStopStatus", MerchantConstant.ID, MerchantConstant.NAME, ICmpConstant.ORGID});
                            if (merchantByIdAndOrg != null && (merchantByIdAndOrg.getDetailStopStatus() == null || !merchantByIdAndOrg.getDetailStopStatus())) {
                                merchantResult.setAgentFinancialDTO(agentFinancialDTO);
                                merchantResult.setOrgId(accentity);
                                return merchantResult;
                            } else {
                                MerchantApplyRangeQryDTO merchantApplyRangeQryDTO = new MerchantApplyRangeQryDTO();
                                merchantApplyRangeQryDTO.setMerchantId(agentFinancialDTO.getMerchantId());
                                merchantApplyRangeQryDTO.setRangeType(Integer.valueOf(1));
                                merchantApplyRangeQryDTO.setFields(new String [] {"orgId"});
                                List<MerchantApplyRangeDTO> merchantApplyRangeDTOS = merchantService.listMerchantApplyRange(merchantApplyRangeQryDTO);
                                for (MerchantApplyRangeDTO merchantApplyRangeDTO : merchantApplyRangeDTOS) {
                                    if (orgSet.contains(merchantApplyRangeDTO.getOrgId())) {
                                        merchantResult.setAgentFinancialDTO(agentFinancialDTO);
                                        merchantResult.setOrgId(merchantApplyRangeDTO.getOrgId());
                                        return merchantResult;
                                    }
                                }
                            }

                            // 若当前资金组织不在客户分配关系内，则找企业级的
                            merchantByIdAndOrg = merchantService.getMerchantByIdAndOrg(agentFinancialDTO.getMerchantId(), Long.valueOf(IStwbConstantForCmp.GLOBAL_ACCENTITY), new String[]{"detailStopStatus", MerchantConstant.ID, MerchantConstant.NAME, ICmpConstant.ORGID});
                            if (merchantByIdAndOrg != null && (merchantByIdAndOrg.getDetailStopStatus() == null || !merchantByIdAndOrg.getDetailStopStatus().booleanValue())) {
                                merchantResult.setAgentFinancialDTO(agentFinancialDTO);
                                merchantResult.setOrgId(accentity);
                                return merchantResult;
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Invalid accentity format: {}", accentity);
                            merchantResult.setCode(500);
                            merchantResult.setMessage("Invalid accentity format");
                            return merchantResult;
                        }
                    }
                }

            }
        } catch (Exception e) {
            if(log.isInfoEnabled()) {
                log.info("MerchantUtils--queryCustForCheck--exception:", e);
            }
            merchantResult.setCode(500);
            merchantResult.setMessage(e.getMessage());
        }
        return merchantResult;
    }

}
