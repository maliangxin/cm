package com.yonyoucloud.fi.cmp.common.digitalwallet;

import com.yonyou.ucf.basedoc.model.BankAcctLinkedVO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.basedoc.model.rpcparams.EnterpriseParams;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyoucloud.fi.cmp.cmpentity.FundSettleStatus;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author hanll
 * @Date 2025/8/29-10:09
 */
@Component
@Slf4j
public abstract class AbstractWalletHandler {

    /**
     * 付款方数币钱包标识
     */
    protected boolean payWalletFlag;
    /**
     * 收款方数币钱包标识
     */
    protected boolean recWalletFlag;


    /**
     * 非内部组织保存校验
     * @param bizObject
     * @return
     */
    public String checkOuterOrgSave(BizObject bizObject) throws Exception {

        // 已结算补单场景下不校验数币账户
        if(bizObject.getShort("settlestatus") == FundSettleStatus.SettlementSupplement.getValue()){
            return null;
        }

        // 内部组织
        if (judgeInnerOrg(bizObject)) {
            return null;
        }

        // 付款方判断
        payWalletFlag = isDigitalWallet(getEnterpriseBankAcctVO(bizObject, Direction.Debit));
        // 收款方判断
        recWalletFlag = isDigitalWallet(getEnterpriseBankAcctVO(bizObject, Direction.Credit));
        if ((payWalletFlag && recWalletFlag) || (!payWalletFlag && !recWalletFlag)) {
            return null;
        }
        return checkOuterOrgSaveExt(bizObject);
    }

    /**
     * 非内部组织保存校验扩展
     * @param bizObject
     * @return
     */
    protected abstract String checkOuterOrgSaveExt(BizObject bizObject) throws Exception;

    /**
     * 保存校验
     * @param bizObject {@link BizObject}
     * @throws Exception
     */
    public void checkSave(BizObject bizObject) throws Exception {
        // 内部组织保存校验
        checkInnerOrgSave(bizObject);
    }

    /**
     * 内部组织保存校验
     * @param bizObject
     * @throws Exception
     */
    private void checkInnerOrgSave(BizObject bizObject) throws Exception {
        if (!judgeInnerOrg(bizObject)) {
            return;
        }
        checkInnerOrgSaveExt(bizObject);
    }

    /**
     * 内部组织保存校验扩展
     * @param bizObject
     * @throws Exception
     */
    private void checkInnerOrgSaveExt(BizObject bizObject) throws Exception {
        EnterpriseBankAcctVO payEnterpriseBankAcctVO = this.getEnterpriseBankAcctVO(bizObject, Direction.Debit);
        EnterpriseBankAcctVO recEnterpriseBankAcctVO = this.getEnterpriseBankAcctVO(bizObject, Direction.Credit);
        // 本方和对方非数币钱包直接返回
        if (!(this.isDigitalWallet(payEnterpriseBankAcctVO) || this.isDigitalWallet(recEnterpriseBankAcctVO))) {
            return;
        }
        String lineNo = bizObject.getString("lineno");
        // BigDecimal换换成字符串
        if (!StringUtils.isEmpty(lineNo)) {
            BigDecimal lineNoBd = new BigDecimal(lineNo);
            lineNo = lineNoBd.setScale(0, RoundingMode.DOWN) + "";
        }
        // 先本方后对方
        // 数币钱包
        if (this.isDigitalWallet(payEnterpriseBankAcctVO)) {
            List<String> linkedAccountIdList = this.getBankAccountRelateWallet(payEnterpriseBankAcctVO);
            if (!this.isDigitalWallet(recEnterpriseBankAcctVO) && !linkedAccountIdList.contains(recEnterpriseBankAcctVO.getId())){
                if (StringUtils.isNotEmpty(lineNo)) {
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400399", "明细行号【%s】：本方银行账户为数币钱包，对方银行账户可为数币钱包，也可为本方数币钱包关联的银行账户！") /* "明细行号【%s】：本方银行账户为数币钱包，对方银行账户可为数币钱包，也可为本方数币钱包关联的银行账户！" */, lineNo));
                } else {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540039B", "本方银行账户为数币钱包，对方银行账户可为数币钱包，也可为本方数币钱包关联的银行账户！") /* "本方银行账户为数币钱包，对方银行账户可为数币钱包，也可为本方数币钱包关联的银行账户！" */);
                }
            }
        } else {
            // 非数币钱包
            List<String> linkedAccountIdList = this.getBankAccountRelateWallet(recEnterpriseBankAcctVO);
            if (this.isDigitalWallet(recEnterpriseBankAcctVO) && !linkedAccountIdList.contains(payEnterpriseBankAcctVO.getId())) {
                if (StringUtils.isNotEmpty(lineNo)) {
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400397", "明细行号【%s】：本方银行账户非数币钱包，对方银行账户可非数币钱包，也可为关联了本方银行账户的数币钱包！") /* "明细行号【%s】：本方银行账户非数币钱包，对方银行账户可非数币钱包，也可为关联了本方银行账户的数币钱包！" */, lineNo));
                } else {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400398", "本方银行账户非数币钱包，对方银行账户可非数币钱包，也可为关联了本方银行账户的数币钱包！") /* "本方银行账户非数币钱包，对方银行账户可非数币钱包，也可为关联了本方银行账户的数币钱包！" */);
                }
            }
        }

        // 先对方后本方
        // 数币钱包
        if (this.isDigitalWallet(recEnterpriseBankAcctVO)) {
            List<String> linkedAccountIdList = this.getBankAccountRelateWallet(recEnterpriseBankAcctVO);
            if (!this.isDigitalWallet(payEnterpriseBankAcctVO) && !linkedAccountIdList.contains(payEnterpriseBankAcctVO.getId())){
                if (StringUtils.isNotEmpty(lineNo)) {
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540039E", "明细行号【%s】：对方银行账户为数币钱包，本方银行账户可为数币钱包，也可为对方数币钱包关联的银行账户！") /* "明细行号【%s】：对方银行账户为数币钱包，本方银行账户可为数币钱包，也可为对方数币钱包关联的银行账户！" */, lineNo));
                } else {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054003A0", "对方银行账户为数币钱包，本方银行账户可为数币钱包，也可为对方数币钱包关联的银行账户！") /* "对方银行账户为数币钱包，本方银行账户可为数币钱包，也可为对方数币钱包关联的银行账户！" */);
                }
            }
        } else {
            // 非数币钱包
            List<String> linkedAccountIdList = this.getBankAccountRelateWallet(payEnterpriseBankAcctVO);
            if (this.isDigitalWallet(payEnterpriseBankAcctVO) && !linkedAccountIdList.contains(recEnterpriseBankAcctVO.getId())) {
                if (StringUtils.isNotEmpty(lineNo)) {
                    throw new CtmException(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540039A", "明细行号【%s】：对方银行账户非数币钱包，本方银行账户可非数币钱包，也可为关联了对方银行账户的数币钱包！") /* "明细行号【%s】：对方银行账户非数币钱包，本方银行账户可非数币钱包，也可为关联了对方银行账户的数币钱包！" */, lineNo));
                } else {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540039C", "对方银行账户非数币钱包，本方银行账户可非数币钱包，也可为关联了对方银行账户的数币钱包！") /* "对方银行账户非数币钱包，本方银行账户可非数币钱包，也可为关联了对方银行账户的数币钱包！" */);
                }
            }
        }
    }

    /**
     * 过滤
     * @param billDataDto
     * @param bizObject
     */
    public void filter(BillDataDto billDataDto, BizObject bizObject, Direction directionAccount) throws Exception {
        if (!judgeInnerOrg(bizObject)) {
            return;
        }
        // 对方账户是否在企业银行档案中
        EnterpriseBankAcctVO enterpriseBankAcctVO = this.getEnterpriseBankAcctVO(bizObject, directionAccount);
        // 开户类型数币钱包
        fiterDigitalWallet(billDataDto, enterpriseBankAcctVO);
        // 开户类型非数币钱包
        filterNDigitalWallet(billDataDto, enterpriseBankAcctVO);
    }

    /**
     * 过滤非数币钱包
     * @param billDataDto
     * @param enterpriseBankAcctVO
     */
    private void filterNDigitalWallet(BillDataDto billDataDto, EnterpriseBankAcctVO enterpriseBankAcctVO) {
        if (isDigitalWallet(enterpriseBankAcctVO)) {
            return;
        }
        // 开户类型非数币钱包
        SimpleFilterVO noWalletFilter = new SimpleFilterVO(ConditionOperator.and);
        noWalletFilter.addCondition(new SimpleFilterVO("acctopentype", "neq", AcctopenTypeEnum.DigitalWallet.getValue()));
        // 开户类型数币钱包，且为对方数币钱包关联的银行账户
        SimpleFilterVO walletFilter = new SimpleFilterVO(ConditionOperator.and);
        // 获取数币钱包关联的银行账户
        // List<String> linkedAccountIdList = getBankAccountRelateWallet(enterpriseBankAcctVO);
        walletFilter.addCondition(new SimpleFilterVO("acctopentype", "eq", AcctopenTypeEnum.DigitalWallet.getValue()));
        walletFilter.addCondition(new SimpleFilterVO("linkedAccountList.linkedBankAccountId", "eq", enterpriseBankAcctVO.getId()));
        if(billDataDto.getCondition() == null){
            FilterVO condition = new FilterVO();
            billDataDto.setCondition(condition);
        }
        billDataDto.getCondition().appendCondition(ConditionOperator.or, noWalletFilter, walletFilter);
    }

    /**
     * 过滤数币钱包
     * @param billDataDto
     * @param enterpriseBankAcctVO
     */
    private void fiterDigitalWallet(BillDataDto billDataDto, EnterpriseBankAcctVO enterpriseBankAcctVO) {
        if (!isDigitalWallet(enterpriseBankAcctVO)) {
            return;
        }

        // 开户类型=数币钱包
        SimpleFilterVO walletFilter = new SimpleFilterVO(ConditionOperator.and);
        walletFilter.addCondition(new SimpleFilterVO("acctopentype", "eq", AcctopenTypeEnum.DigitalWallet.getValue()));
        // 开户类型非数币钱包，且为对方数币钱包关联的银行账户
        SimpleFilterVO noWalletFilter = new SimpleFilterVO(ConditionOperator.and);
        // 获取数币钱包关联的银行账户
        List<String> linkedAccountIdList = getBankAccountRelateWallet(enterpriseBankAcctVO);
        noWalletFilter.addCondition(new SimpleFilterVO("acctopentype", "neq", AcctopenTypeEnum.DigitalWallet.getValue()));
        noWalletFilter.addCondition(new SimpleFilterVO("id", "in", linkedAccountIdList));
        billDataDto.getCondition().appendCondition(ConditionOperator.or, walletFilter, noWalletFilter);
    }

    /**
     * 根据企业银行账户id获取银行账户
     * @param accountId
     * @return
     * @throws Exception
     */
    protected EnterpriseBankAcctVO getEnterpriseBankAcctVO(String accountId) throws Exception {
        //根据收款银行账户id
        EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findById(accountId);
        if (enterpriseBankAcctVO == null) {
            EnterpriseBankAcctVO returnVO = new EnterpriseBankAcctVO();
            returnVO.setAcctopentype(-1);
            return returnVO;
        }
        return enterpriseBankAcctVO;
    }

    /**
     * 根据企业银行账号获取银行账户
     * @param account
     * @return
     * @throws Exception
     */
    protected EnterpriseBankAcctVO getEnterpriseBankAcctVOByAccount(String account) throws Exception {
        //根据收款银行账户id
        EnterpriseBankAcctVO enterpriseBankAcctVO = EnterpriseBankQueryService.findByAccount(account);
        if (enterpriseBankAcctVO == null) {
            EnterpriseBankAcctVO returnVO = new EnterpriseBankAcctVO();
            returnVO.setAcctopentype(-1);
            return returnVO;
        }
        return enterpriseBankAcctVO;
    }

    /**
     * 判断是否为内部组织
     * @param bizObject
     * @return true 是；false 否
     */
    public abstract boolean judgeInnerOrg(BizObject bizObject) throws Exception;

    /**
     * 获取企业银行档案
     * @param bizObject
     * @return
     */
    public abstract EnterpriseBankAcctVO getEnterpriseBankAcctVO(BizObject bizObject, Direction directionAccount) throws Exception;

    /**
     * 判断是否为数币钱包
     * @param enterpriseBankAcctVO
     * @return
     */
    protected boolean isDigitalWallet(EnterpriseBankAcctVO enterpriseBankAcctVO) {
        return enterpriseBankAcctVO.getAcctopentype() != null && AcctopenTypeEnum.DigitalWallet.getValue() == enterpriseBankAcctVO.getAcctopentype();
    }

    /**
     * 获取数币钱包关联的银行账户Id集合
     * @param enterpriseBankAcctVO
     * @return
     */
    protected List<String> getBankAccountRelateWallet(EnterpriseBankAcctVO enterpriseBankAcctVO) {
        List<BankAcctLinkedVO> linkedAccountList = enterpriseBankAcctVO.getLinkedAccountList();
        if (CollectionUtils.isEmpty(linkedAccountList)) {
            return new ArrayList<>();
        }
        return linkedAccountList.stream().map(BankAcctLinkedVO::getLinkedBankAccountId).collect(Collectors.toList());
    }


    /**
     * 校验银行账号 数币钱包 16位且开头为0
     * @param account
     * @return
     */
    protected String checkBankAccount(String account) {
        if (StringUtils.isEmpty(account) || (account.length() == 16 && account.startsWith("0"))) {
            return null;
        }
        if (payWalletFlag) {
            return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540039D", "对方银行账号可能不为数币钱包。") /* "对方银行账号可能不为数币钱包。" */;
        } else if (recWalletFlag) {
            return com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540039F", "本方银行账号可能不为数币钱包。") /* "本方银行账号可能不为数币钱包。" */;
        }
        return null;
    }

    /**
     * 根据企业银行账户银行账号获取企业银行账户
     * @param bankAccountList
     * @return
     */
    protected List<EnterpriseBankAcctVO> getEnterpriseAccountListByAccount(List<String> bankAccountList) throws Exception {
        EnterpriseParams enterpriseParams = new EnterpriseParams();
        enterpriseParams.setAccountList(bankAccountList);
        List<EnterpriseBankAcctVO> enterpriseBankAcctVOList = EnterpriseBankQueryService.query(enterpriseParams);
        if (CollectionUtils.isEmpty(enterpriseBankAcctVOList)) {
            return new ArrayList<>();
        }
        return enterpriseBankAcctVOList;
    }

}
