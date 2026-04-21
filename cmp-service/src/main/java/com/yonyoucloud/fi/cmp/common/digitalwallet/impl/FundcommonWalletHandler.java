package com.yonyoucloud.fi.cmp.common.digitalwallet.impl;

import com.yonyou.ucf.basedoc.model.BankAcctLinkedVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyoucloud.fi.cmp.cmpentity.CaObject;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.digitalwallet.AbstractWalletHandler;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.fundcollection.FundCollection_b;
import com.yonyoucloud.fi.cmp.fundpayment.FundPayment_b;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.VendorQueryService;
import com.yonyoucloud.fi.tmsp.openapi.ITmspRefRpcService;
import com.yonyoucloud.fi.tmsp.vo.FundBusinObjArchivesDTO;
import com.yonyoucloud.fi.tmsp.vo.FundBusinObjArchivesItemDTO;
import com.yonyoucloud.fi.tmsp.vo.FundBusinObjTypeDTO;
import com.yonyoucloud.fi.tmsp.vo.TmspRequestParams;
import com.yonyoucloud.iuap.upc.api.IMerchantServiceV2;
import com.yonyoucloud.iuap.upc.dto.AgentFinancialDTO;
import com.yonyoucloud.iuap.upc.dto.MerchantDTO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorBankVO;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorVO;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.yonyoucloud.fi.cmp.cmpentity.CaObject.CapBizObj;
import static com.yonyoucloud.fi.cmp.cmpentity.CaObject.InnerUnit;

/**
 * @Description 资金收付款单数字钱包实现类
 * @Author hanll
 * @Date 2025/11/5-14:37
 */
@Service("fundcommonWalletHandler")
public class FundcommonWalletHandler extends AbstractWalletHandler {

    @Autowired
    private IMerchantServiceV2 merchantService;

    @Autowired
    private VendorQueryService vendorQueryService;

    @Autowired
    private ITmspRefRpcService iTmspRefRpcService;

    @Autowired
    private CmCommonService commonService;
    /**
     * 非内部组织保存校验扩展
     * @param bizObject
     * @return
     */
    @Override
    protected String checkOuterOrgSaveExt(BizObject bizObject) throws Exception {
        String account = "";
        // 付款方是数币钱包
        if (super.payWalletFlag) {
            if (bizObject instanceof FundPayment_b) {
                FundPayment_b fundPaymentB = (FundPayment_b) bizObject;
                account = fundPaymentB.getOppositeaccountno();
            } else if (bizObject instanceof FundCollection_b) {
                FundCollection_b fundCollectionB = (FundCollection_b) bizObject;
                account = fundCollectionB.getOppositeaccountno();
            }
        } else if (super.recWalletFlag) {
            // 收款方是数币钱包
            EnterpriseBankAcctVO payEnterpriseBankAcctVO = this.getEnterpriseBankAcctVO(bizObject, Direction.Debit);
            // 本方银行账号
            account = payEnterpriseBankAcctVO.getAccount();
        }
        return super.checkBankAccount(account);
    }

    /**
     * 判断是否为内部组织
     * @param bizObject
     * @return true 是；false 否
     */
    @Override
    public boolean judgeInnerOrg(BizObject bizObject) throws Exception {
        // 对方类型
        CaObject caObject = getCaObject(bizObject);
        // 对方类型 内部组织
        // 客户、供应商、资金业务伙伴、内部单位
        List<Short> innerOrgTypes = Arrays.asList(CaObject.Customer.getValue(), CaObject.Supplier.getValue(),
                                   InnerUnit.getValue(), CapBizObj.getValue());
        if (!innerOrgTypes.contains(caObject.getValue())) {
            return false;
        }
        // 对方单位id
        String oppositeObjectId = getOppositeObjectId(bizObject);
        if(oppositeObjectId != null){
            switch (caObject) {
                case Customer:
                    // 查询客户
                    MerchantDTO merchantDTO = merchantService.getMerchantById(Long.valueOf(oppositeObjectId), new String[]{"internalOrg"});
                    return merchantDTO != null && merchantDTO.getInternalOrg() != null && merchantDTO.getInternalOrg();
                case Supplier:
                    VendorVO vendorVO = vendorQueryService.getVendorById(Long.valueOf(oppositeObjectId));
                    return vendorVO != null && vendorVO.get("internalunit") != null && (Boolean) vendorVO.get("internalunit");
                case InnerUnit:
                    return true;
                case CapBizObj:
                    TmspRequestParams params = new TmspRequestParams();
                    params.setIds(new String[]{oppositeObjectId});
                    List<FundBusinObjArchivesDTO> fundBusinObjArchivesDTOS = iTmspRefRpcService.queryFundBusinObjArchivesByIdList(params);
                    if (CollectionUtils.isEmpty(fundBusinObjArchivesDTOS)) {
                        throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400806", "资金业务对象档案不存在！") /* "资金业务对象档案不存在！" */);
                    }
                    // 资金伙伴类型id
                    String fundBusinObjTypeId = fundBusinObjArchivesDTOS.get(0).getFundbusinobjtypeid();
                    TmspRequestParams fundTypeParams = new TmspRequestParams();
                    fundTypeParams.setIds(new String[]{fundBusinObjTypeId});
                    List<FundBusinObjTypeDTO> fundBusinObjTypeDTOS = iTmspRefRpcService.queryFundBusinObjTypeByIdList(fundTypeParams);
                    return CollectionUtils.isNotEmpty(fundBusinObjTypeDTOS) && ("TBOT0007").equals(fundBusinObjTypeDTOS.get(0).getCode());
                default:
                    break;

            }
        }
        return false;
    }

    /**
     * 获取对方单位id
     * @param bizObject
     * @return
     */
    private String getOppositeObjectId(BizObject bizObject) {
        String oppositeObjectId;
        if (bizObject instanceof FundPayment_b) {
            FundPayment_b fundPaymentB = (FundPayment_b) bizObject;
            oppositeObjectId =  fundPaymentB.getOppositeobjectid();
        } else if (bizObject instanceof FundCollection_b) {
            FundCollection_b fundCollectionB = (FundCollection_b) bizObject;
            oppositeObjectId =  fundCollectionB.getOppositeobjectid();
        } else {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400805", "传入的单据非资金收付，请检查！") /* "传入的单据非资金收付，请检查！" */);
        }
        return oppositeObjectId;
    }

    /**
     * 获取对方账户id
     * @param bizObject
     * @return
     */
    private String getOppositeAccountId(BizObject bizObject, Direction directionAccount) {
        String oppositeAccountId;
        if (bizObject instanceof FundPayment_b) {
            FundPayment_b fundPaymentB = (FundPayment_b) bizObject;
            oppositeAccountId = directionAccount == Direction.Credit ? fundPaymentB.getOppositeaccountid() : fundPaymentB.getEnterprisebankaccount();
        } else if (bizObject instanceof FundCollection_b) {
            FundCollection_b fundCollectionB = (FundCollection_b) bizObject;
            oppositeAccountId = directionAccount == Direction.Credit ? fundCollectionB.getOppositeaccountid() : fundCollectionB.getEnterprisebankaccount();
        } else {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400805", "传入的单据非资金收付，请检查！") /* "传入的单据非资金收付，请检查！" */);
        }
        return oppositeAccountId;
    }

    /**
     * 获取本方或对方的账户id
     * @param bizObject
     * @param directionAccount
     * @return
     */
    private String getEnterpriseBankAcctId(BizObject bizObject, Direction directionAccount) {
        String enterpriseAccountId;
        if (bizObject instanceof FundPayment_b) {
            FundPayment_b fundPaymentB = (FundPayment_b) bizObject;
            enterpriseAccountId = directionAccount == Direction.Debit ? fundPaymentB.getEnterprisebankaccount() : fundPaymentB.getOppositeaccountno();
        } else if (bizObject instanceof FundCollection_b) {
            FundCollection_b fundCollectionB = (FundCollection_b) bizObject;
            enterpriseAccountId = directionAccount == Direction.Debit ? fundCollectionB.getEnterprisebankaccount() : fundCollectionB.getOppositeaccountno();
        } else {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400805", "传入的单据非资金收付，请检查！") /* "传入的单据非资金收付，请检查！" */);
        }
        return enterpriseAccountId;
    }

    /**
     * 对方账户类型
     * @param bizObject
     * @return
     */
    private CaObject getCaObject(BizObject bizObject) {
        CaObject caObject;
        if (bizObject instanceof FundPayment_b) {
            FundPayment_b fundPaymentB = (FundPayment_b) bizObject;
            caObject = fundPaymentB.getCaobject();
        } else if (bizObject instanceof FundCollection_b) {
            FundCollection_b fundCollectionB = (FundCollection_b) bizObject;
            caObject = fundCollectionB.getCaobject();
        } else {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400805", "传入的单据非资金收付，请检查！") /* "传入的单据非资金收付，请检查！" */);
        }
        return caObject;
    }

    /**
     * 获取企业银行档案
     * @param bizObject
     * @param directionAccount
     * @return
     */
    @Override
    public EnterpriseBankAcctVO getEnterpriseBankAcctVO(BizObject bizObject, Direction directionAccount) throws Exception {
        String enterpriseAccountId = getEnterpriseBankAcctId(bizObject, directionAccount);
        if (StringUtils.isEmpty(enterpriseAccountId)) {
            EnterpriseBankAcctVO enterpriseBankAcctVO = new EnterpriseBankAcctVO();
            enterpriseBankAcctVO.setAcctopentype(-1);
            return enterpriseBankAcctVO;
        }
        return directionAccount == Direction.Credit ? super.getEnterpriseBankAcctVOByAccount(enterpriseAccountId)
                : super.getEnterpriseBankAcctVO(enterpriseAccountId);
    }

    /**
     * 过滤本方银行账户
     * @param billDataDto
     * @param bizObject
     * @param directionAccount
     * @throws Exception
     */
    public void filterEnterpriseBankAccount(BillDataDto billDataDto, BizObject bizObject, Direction directionAccount) throws Exception {
        // 对方银行账户为空不处理
        String oppositeAccountId = getOppositeAccountId(bizObject, directionAccount);
        if (StringUtils.isEmpty(oppositeAccountId)) {
            return;
        }
        // 结算方式的业务属性为银行业务进行数币钱包的过滤
        if (!checkSettleAttrOfEnterprise(bizObject)) {
            return;
        }

        super.filter(billDataDto, bizObject, directionAccount);
    }

    public void filterOppositeAccount(BillDataDto billDataDto, BizObject bizObject, Direction directionAccount) throws Exception {
        // 本方账户为空不处理
        EnterpriseBankAcctVO enterpriseBankAcctVO = getEnterpriseBankAcctVO(bizObject, directionAccount);
        if (enterpriseBankAcctVO.getAcctopentype() == -1) {
            return;
        }
        // 对方档案类型非内部组织跳过
        if (!this.judgeInnerOrg(bizObject)) {
            return;
        }
        // 本方银行账户为数币钱包
        boolean enterpriseAccountIsDw = checkEnterpriseAccountIsDw(bizObject);
        // 对方类型
        CaObject caObject = getCaObject(bizObject);
        filterOppositeAccountOfEnterPriseDw(billDataDto, bizObject, directionAccount, enterpriseAccountIsDw, caObject);
        filterOppositeAccountOfEnterPriseNDw(billDataDto, bizObject, directionAccount, enterpriseAccountIsDw, caObject);

    }

    /**
     * 本方银行账户为非数币账户的过滤
     * @param billDataDto
     * @param bizObject
     * @param directionAccount
     * @param enterpriseAccountIsDw
     * @param caObject
     */
    private void filterOppositeAccountOfEnterPriseNDw(BillDataDto billDataDto, BizObject bizObject, Direction directionAccount, boolean enterpriseAccountIsDw, CaObject caObject) throws Exception {
        if (enterpriseAccountIsDw) {
            return;
        }
        // 对方单位id
        String oppositeObjectId = getOppositeObjectId(bizObject);
        switch (caObject) {
            case Customer:
                // 查询客户银行账户
                List<AgentFinancialDTO> agentFinancialDTOS = merchantService.listMerchantAgentFinancial(null, Collections.singletonList(Long.valueOf(oppositeObjectId)), null);
                if (CollectionUtils.isEmpty(agentFinancialDTOS)) {
                    return;
                }
                // 银行账号
                List<String> customerBankaccountList = agentFinancialDTOS.stream().map(AgentFinancialDTO::getBankAccount).collect(Collectors.toList());
                // 客户银行账号过滤
                billDataDto.getCondition().appendCondition("bankAccount", "in", getFilteredAccountNdwList(bizObject, customerBankaccountList));
                break;
            case Supplier:
                // 查询供应商银行账户
                List<VendorBankVO> vendorBankVOList = vendorQueryService.getVendorBanksByVendorId(Long.valueOf(oppositeObjectId), null);
                if (CollectionUtils.isEmpty(vendorBankVOList)) {
                    return;
                }
                // 银行账号
                List<String> vendorBandaccountList = vendorBankVOList.stream().map(VendorBankVO::getAccount).collect(Collectors.toList());
                // 供应商银行账号过滤
                billDataDto.getCondition().appendCondition("account", "in", getFilteredAccountNdwList(bizObject, vendorBandaccountList));
                break;

            case InnerUnit:
                // 企业银行账户
                super.filter(billDataDto, bizObject, directionAccount);
                break;
            case CapBizObj:
                // 资金伙伴银行账号
                List<String> fundBusinObjBankAccountList = getFundBusinObjBankAccountList(oppositeObjectId);
                // 客户银行账号过滤
                billDataDto.getCondition().appendCondition("bankaccount", "in", getFilteredAccountNdwList(bizObject, fundBusinObjBankAccountList));
                break;
            default:
                break;

        }

    }

    /**
     * 获取资金业务对象的银行账户
     * @param oppositeObjectId
     * @return
     */
    private List<String> getFundBusinObjBankAccountList(String oppositeObjectId) throws Exception {
        TmspRequestParams params = new TmspRequestParams();
        params.setId(oppositeObjectId);
        List<FundBusinObjArchivesDTO> fundBusinObjArchivesDTOS = iTmspRefRpcService.queryFundBusinObjArchivesByIdList(params);
        if (CollectionUtils.isEmpty(fundBusinObjArchivesDTOS)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400806", "资金业务对象档案不存在！") /* "资金业务对象档案不存在！" */);
        }
        List<FundBusinObjArchivesItemDTO> fundBusinObjArchivesItemDTOS = fundBusinObjArchivesDTOS.get(0).getFundBusinObjArchivesItemDTO();
        if (CollectionUtils.isEmpty(fundBusinObjArchivesItemDTOS)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400804", "该资金业务对象未配置银行账户信息，请检查！") /* "该资金业务对象未配置银行账户信息，请检查！" */);
        }
        return fundBusinObjArchivesItemDTOS.stream().map(FundBusinObjArchivesItemDTO::getBankaccount).collect(Collectors.toList());
    }

    /**
     * 获取过滤的对方银行账号
     * @param bizObject
     * @param bankAccountList 银行账号集合
     * @return
     * @throws Exception
     */
    private List<String> getFilteredAccountNdwList(BizObject bizObject, List<String> bankAccountList) throws Exception {
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOList = super.getEnterpriseAccountListByAccount(bankAccountList);
        // 过滤出数币钱包的银行账户
        List<EnterpriseBankAcctVO> dwEnterpriseBankAcctVOList = enterpriseBankAcctVOList.stream().filter(this::judgeDwAccount).collect(Collectors.toList());
        // 过滤出非数币钱包的银行账号
        List<String> ndwCustomerAccountList = enterpriseBankAcctVOList.stream().filter(this::judgeNDwAccount).map(EnterpriseBankAcctVO::getAccount).collect(Collectors.toList());
        Map<String, List<BankAcctLinkedVO>> dwCustomerAccountMap = dwEnterpriseBankAcctVOList.stream().collect(Collectors.toMap(EnterpriseBankAcctVO::getAccount, EnterpriseBankAcctVO::getLinkedAccountList));
        List<String> dwRetailCustomerAccountList = new ArrayList<>();
        dwCustomerAccountMap.forEach((account, linkedAccountList) -> {
            for (BankAcctLinkedVO bankAcctLinkedVO : linkedAccountList) {
                if (bankAcctLinkedVO.getLinkedBankAccountId().equals(getEnterpriseBankAcctId(bizObject, Direction.Debit))) {
                    dwRetailCustomerAccountList.add(account);
                    break;
                }
            }
        });
        List<String> filterCustomerAccountList = new ArrayList<>();
        filterCustomerAccountList.addAll(ndwCustomerAccountList);
        filterCustomerAccountList.addAll(dwRetailCustomerAccountList);
        return filterCustomerAccountList;
    }

    /**
     * 本方银行账户为数币账户的过滤
     * @param billDataDto
     * @param bizObject
     * @param directionAccount
     * @param enterpriseAccountIsDw
     * @param caObject
     */
    private void filterOppositeAccountOfEnterPriseDw(BillDataDto billDataDto, BizObject bizObject, Direction directionAccount, boolean enterpriseAccountIsDw, CaObject caObject) throws Exception {
        if (!enterpriseAccountIsDw) {
            return;
        }
        // 对方单位id
        String oppositeObjectId = getOppositeObjectId(bizObject);
        switch (caObject) {
            case Customer:
                // 查询客户银行账户
                List<AgentFinancialDTO> agentFinancialDTOS = merchantService.listMerchantAgentFinancial(null, Collections.singletonList(Long.valueOf(oppositeObjectId)), null);
                if (CollectionUtils.isEmpty(agentFinancialDTOS)) {
                    return;
                }
                // 银行账号
                List<String> customerBandaccountList = agentFinancialDTOS.stream().map(AgentFinancialDTO::getBankAccount).collect(Collectors.toList());
                // 客户银行账号过滤
                billDataDto.getCondition().appendCondition("bankAccount", "in", getFilteredAccountDwList(bizObject, customerBandaccountList));
                break;
            case Supplier:
                // 查询供应商银行账户
                List<VendorBankVO> vendorBankVOList = vendorQueryService.getVendorBanksByVendorId(Long.valueOf(oppositeObjectId), null);
                if (CollectionUtils.isEmpty(vendorBankVOList)) {
                    return;
                }
                // 银行账号
                List<String> vendorBandaccountList = vendorBankVOList.stream().map(VendorBankVO::getAccount).collect(Collectors.toList());
                // 供应商银行账号
                billDataDto.getCondition().appendCondition("account", "in", getFilteredAccountDwList(bizObject, vendorBandaccountList));
                break;

            case InnerUnit:
                // 企业银行账户
                super.filter(billDataDto, bizObject, directionAccount);
                break;
            case CapBizObj:
                // 资金伙伴银行账号
                List<String> fundBusinObjBankAccountList = getFundBusinObjBankAccountList(oppositeObjectId);
                // 客户银行账号过滤
                billDataDto.getCondition().appendCondition("bankaccount", "in", getFilteredAccountDwList(bizObject, fundBusinObjBankAccountList));
                break;
            default:
                break;

        }
    }

    /**
     * 获取过滤的对方银行账号-本方是数币钱包
     * @param bizObject
     * @param bankAccountList 银行账号集合
     * @return
     * @throws Exception
     */
    private List<String> getFilteredAccountDwList(BizObject bizObject, List<String> bankAccountList) throws Exception {
        // 过滤出数币钱包的银行账号
        List<String> dwAccountList = getDwAccountList(bankAccountList);
        // 获取本方银行账户对应的关联银行账户id
        List<String> dwRelateAccountList = getRelateEnterpriseBankAccount(bizObject);
        dwRelateAccountList.addAll(dwAccountList);
        return dwRelateAccountList;
    }

    /**
     * 获取本方银行账户关联的数币钱包银行账号
     * @param bizObject
     * */
    private List<String> getRelateEnterpriseBankAccount(BizObject bizObject) throws Exception {
        List<String> relationsAccountIds = super.getBankAccountRelateWallet(this.getEnterpriseBankAcctVO(bizObject, Direction.Debit));
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setIdList(relationsAccountIds);
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOList = EnterpriseBankQueryService.query(enterpriseParams);
        return enterpriseBankAcctVOList.stream().map(EnterpriseBankAcctVO::getAccount).collect(Collectors.toList());
    }

    /**
     * 获取数币钱包的银行账号
     * @param bankAccountList
     * @return
     */
    private List<String> getDwAccountList(List<String> bankAccountList) throws Exception {
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOList = super.getEnterpriseAccountListByAccount(bankAccountList);
        if (CollectionUtils.isEmpty(enterpriseBankAcctVOList)) {
            return new ArrayList<>();
        }
        // 过滤出数币账户的银行账号
        return enterpriseBankAcctVOList.stream().filter(this::judgeDwAccount).map(EnterpriseBankAcctVO::getAccount).collect(Collectors.toList());
    }





    private boolean judgeDwAccount(EnterpriseBankAcctVO enterpriseBankAcctVO) {
        return AcctopenTypeEnum.DigitalWallet.getValue() == enterpriseBankAcctVO.getAcctopentype();
    }

    private boolean judgeNDwAccount(EnterpriseBankAcctVO enterpriseBankAcctVO) {
        return !judgeDwAccount(enterpriseBankAcctVO);
    }

    /**
     * 校验结算方式的业务属性是否银行业务
     * @param bizObject
     * @return
     */
    public boolean checkSettleAttrOfEnterprise(BizObject bizObject) {
        Integer serviceAttr = getSettleModeServiceAttr(bizObject);
        // 结算方式的业务属性为银行业务进行数币钱包的过滤
        return 0 == serviceAttr;
    }

    /**
     * 判断本方银行账户是否是数币钱包
     * @param bizObject
     * @return
     * @throws Exception
     */
    public boolean checkEnterpriseAccountIsDw(BizObject bizObject) throws Exception {
        return super.isDigitalWallet(this.getEnterpriseBankAcctVO(bizObject, Direction.Debit));
    }
    /**
     * 获取结算方式的业务属性
     * @param bizObject
     * @return
     */
    private Integer getSettleModeServiceAttr(BizObject bizObject) {
        Long settleMode;
        if (bizObject instanceof FundPayment_b) {
            FundPayment_b fundPaymentB = (FundPayment_b) bizObject;
            settleMode = fundPaymentB.getSettlemode();
        } else if (bizObject instanceof FundCollection_b) {
            FundCollection_b fundCollectionB = (FundCollection_b) bizObject;
            settleMode = fundCollectionB.getSettlemode();
        } else {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400805", "传入的单据非资金收付，请检查！") /* "传入的单据非资金收付，请检查！" */);
        }
        return commonService.getServiceAttr(settleMode);
    }
}
