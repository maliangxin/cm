package com.yonyoucloud.fi.cmp.common.digitalwallet.impl;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.digitalwallet.AbstractWalletHandler;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import org.imeta.orm.base.BizObject;
import org.springframework.stereotype.Service;


/**
 * @Description 同名账户划转实现类
 * @Author hanll
 * @Date 2025/8/29-10:50
 */
@Service("transferAccountWalletHandler")
public class TransferAccountWalletHandler extends AbstractWalletHandler {

    @Override
    protected String checkOuterOrgSaveExt(BizObject bizObject) throws Exception {
        return null;
    }

    /**
     * 判断是否为内部组织
     * 同名账户划转 默认为内部组织
     * @param bizObject
     * @return true 是；false 否
     */
    @Override
    public boolean judgeInnerOrg(BizObject bizObject) {
        return true;
    }

    /**
     * 获取企业银行档案
     *
     * @param bizObject
     * @param directionAccount 账户方向  Debit 付款(本方)；Credit 收款(对方)
     * @return
     */
    @Override
    public EnterpriseBankAcctVO getEnterpriseBankAcctVO(BizObject bizObject, Direction directionAccount) throws Exception {
        TransferAccount transferAccount = (TransferAccount) bizObject;
        String enterpriseAccountId = directionAccount == Direction.Debit ? transferAccount.getPayBankAccount() : transferAccount.getRecBankAccount();
        if (StringUtils.isEmpty(enterpriseAccountId)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A420054007A6", "银行账户在企业资金账户中不存在，请检查！") /* "银行账户在企业资金账户中不存在，请检查！" */);
        }
        return super.getEnterpriseBankAcctVO(enterpriseAccountId);
    }

    /**
     * 数币钱包过滤
     * @param billDataDto
     * @param transferAccount
     * @param direction
     */
    public void filterTransferAccountWallet(BillDataDto billDataDto, TransferAccount transferAccount, Direction direction) throws Exception {
        String enterpriseAccountId = direction == Direction.Debit ? transferAccount.getPayBankAccount() : transferAccount.getRecBankAccount();
        if (StringUtils.isEmpty(enterpriseAccountId)) {
            return;
        }
        super.filter(billDataDto, transferAccount, direction);
    }

}
