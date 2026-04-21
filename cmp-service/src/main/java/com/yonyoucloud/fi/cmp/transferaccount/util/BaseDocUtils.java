package com.yonyoucloud.fi.cmp.transferaccount.util;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author hanll
 * @Date 2025/11/3-15:24
 */
public class BaseDocUtils {
    /**
     * 根据银行账户的集合获取银行账户信息
     * @param bankAcctIds
     * @return
     * @throws Exception
     */
    public static Map<String, EnterpriseBankAcctVO> getBankAcctMap(List<String> bankAcctIds) throws Exception {
        if (CollectionUtils.isEmpty(bankAcctIds)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400567", "参数不能为空，请检查!") /* "参数不能为空，请检查!" */);
        }
        List<EnterpriseBankAcctVO> bankAcctVOList = EnterpriseBankQueryService.findByIdList(bankAcctIds);
        if (CollectionUtils.isEmpty(bankAcctVOList)) {
            throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400568", "本方和对方的银行账户不存在，请检查!") /* "本方和对方的银行账户不存在，请检查!" */);
        }
        return bankAcctVOList.stream().collect(Collectors.toMap(EnterpriseBankAcctVO::getId, v -> v));
    }

    /**
     * 判断同名账户划转单据是否是现金柜业务
     * @param transferAccount
     * @return
     * @throws Exception
     */
    public static boolean isCashBox(TransferAccount transferAccount) throws Exception {
        if (!"SC".equalsIgnoreCase(transferAccount.getType())) {
            return false;
        }
        // 收方账户开户类型为结算中心
        EnterpriseBankAcctVO oppositebankaccount =  new EnterpriseBankAcctVO();
        if (ObjectUtils.isNotEmpty(transferAccount.getRecBankAccount())) {
            oppositebankaccount = EnterpriseBankQueryService.findById(transferAccount.getRecBankAccount());
        }
        return ObjectUtils.isNotEmpty(oppositebankaccount) && oppositebankaccount.getAcctopentype().equals(1);
    }
}
