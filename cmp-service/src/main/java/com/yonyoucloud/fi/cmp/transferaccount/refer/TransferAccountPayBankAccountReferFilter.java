package com.yonyoucloud.fi.cmp.transferaccount.refer;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cm.transferaccount.TransferAccount;
import com.yonyoucloud.fi.cmp.autoparam.service.AutoConfigService;
import com.yonyoucloud.fi.cmp.bankaccountsetting.BankAccountSettingService;
import com.yonyoucloud.fi.cmp.common.digitalwallet.impl.TransferAccountWalletHandler;
import com.yonyoucloud.fi.cmp.common.service.SettleMethodService;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.base.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 转账工作台 付款银行账户参照过滤*
 *
 * @author xuxbo
 * @date 2023/6/8 14:34
 */
@Component
public class TransferAccountPayBankAccountReferFilter extends AbstractCommonRule {

    @Autowired
    EnterpriseBankQueryService enterpriseBankQueryService;

    @Autowired
    private SettleMethodService settleMethodService;

    @Autowired
    private BankAccountSettingService bankAccountSettingService;

    @Autowired
    private AutoConfigService autoConfigService;

    @Autowired
    TransTypeQueryService transTypeQueryService;

    @Autowired
    @Qualifier("transferAccountWalletHandler")
    private TransferAccountWalletHandler transferAccountWalletHandler;

    /**
     * 需要进行过滤的billnum集合
     */
    private static final List<String> BILLNUM_MAP = new ArrayList<>();

    public TransferAccountPayBankAccountReferFilter() {
        BILLNUM_MAP.add(IBillNumConstant.TRANSFERACCOUNT);
    }

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> map) throws Exception {
        //银行开户 :acctopentype ==0 财务公司开户 acctopentype == 2 结算中心开户 acctopentype == 1 其他金融机构 acctopentype == 3
        BillDataDto billDataDto = (BillDataDto) getParam(map);
        // 只处理付款银行账户
        if (!"payBankAccount_name".equals(billDataDto.getKey())) {
            return new RuleExecuteResult();
        }
        String billnum = billDataDto.getBillnum();
        if (BILLNUM_MAP.contains(billnum)) {
            //判断现金参数是否为是
            Boolean pushSettlement = autoConfigService.getCheckFundTransfer();
            if (IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode())) {
                List<TransferAccount> transferAccounts = (ArrayList) billDataDto.getData();
                if (CollectionUtils.isEmpty(transferAccounts)) {
                    return new RuleExecuteResult();
                }
                FilterVO conditon = new FilterVO();
                Short[] acctopentype = new Short[] {0, 2, 3};
                if (null != transferAccounts.get(0).get("tradetype")) {
                    // 交易类型
                    BdTransType bdTransType = transTypeQueryService.findById(transferAccounts.get(0).get("tradetype"));
                    CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
                    String tradeTypeCode = (String) jsonObject.get("transferType_zz");
                    //根据交易类型判断是否是银行转账
                    if (("BT".equals(transferAccounts.get(0).get("tradetype_code")) || "yhzz".equals(tradeTypeCode)) && pushSettlement) {
                        if (null != transferAccounts.get(0).get("recBankAccount")) {
                            //1.交易类型为银行转账时，如果收款银行账户有值，付款银行账户参照需要增加过滤条件：开户类型=收款银行账户开户类型；
                            String accountId = transferAccounts.get(0).get("recBankAccount");
                            doFilter(accountId, conditon, billDataDto);
                        } else {
                            SimpleFilterVO NoJszx = new SimpleFilterVO(ConditionOperator.and);
                            // 银行开户  财务公司开户 其他金融机构
                            NoJszx.addCondition(new SimpleFilterVO("acctopentype", "in", acctopentype));

                            SimpleFilterVO Jszx = new SimpleFilterVO(ConditionOperator.and);
                            // 结算中心开户并且是活期
                            Jszx.addCondition(new SimpleFilterVO("acctopentype", "eq", 1));
                            Jszx.addCondition(new SimpleFilterVO("accountNature", "eq", 0));

//                            Short[] acctopentypeall = new Short[] {0,1,2};
                            if (billDataDto.getCondition() == null) {
                                conditon.appendCondition(ConditionOperator.or, new SimpleFilterVO[]{NoJszx, Jszx});
                                billDataDto.setCondition(conditon);
                            } else {
                                billDataDto.getCondition().appendCondition(ConditionOperator.or, new SimpleFilterVO[]{NoJszx, Jszx});
                            }
                        }
                    } else if (("TPT".equals(transferAccounts.get(0).get("tradetype_code")) || "dsfzz".equals(tradeTypeCode)) && pushSettlement) {
                        //交易类型=第三方转账，可参照到开户类型=银行开户、财务公司、其他金融机构 的企业银行账户。
                        if (billDataDto.getCondition() == null) {
                            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", acctopentype));
                            billDataDto.setCondition(conditon);
                        } else {
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", acctopentype));
                        }
                    } else if (("EC".equals(transferAccounts.get(0).get("tradetype_code")) || "SC".equals(transferAccounts.get(0).get("tradetype_code")) || "tqxj".equals(tradeTypeCode) || "jcxj".equals(tradeTypeCode)) && pushSettlement) { //提取现金
                        // 交易类型=提取现金 缴存现金 结算中心开户、账户性质=活期 和 银行开户  财务公司开户 其他金融机构
                        SimpleFilterVO NoJszx = new SimpleFilterVO(ConditionOperator.and);
                        // 银行开户  财务公司开户 其他金融机构
                        NoJszx.addCondition(new SimpleFilterVO("acctopentype", "in", acctopentype));

                        SimpleFilterVO Jszx = new SimpleFilterVO(ConditionOperator.and);
                        // 结算中心开户并且是活期
                        Jszx.addCondition(new SimpleFilterVO("acctopentype", "eq", 1));
                        Jszx.addCondition(new SimpleFilterVO("accountNature", "eq", 0));

                        if (billDataDto.getCondition() == null) {
                            conditon.appendCondition(ConditionOperator.or, new SimpleFilterVO[]{NoJszx, Jszx});
                            billDataDto.setCondition(conditon);
                        } else {
                            billDataDto.getCondition().appendCondition(ConditionOperator.or, new SimpleFilterVO[]{NoJszx, Jszx});
                        }

                    } else if (("BT".equals(transferAccounts.get(0).get("tradetype_code")) || "yhzz".equals(tradeTypeCode) || "EC".equals(transferAccounts.get(0).get("tradetype_code")) || "SC".equals(transferAccounts.get(0).get("tradetype_code"))
                            || "tqxj".equals(tradeTypeCode) || "jcxj".equals(tradeTypeCode) || "TPT".equals(transferAccounts.get(0).get("tradetype_code")) || "dsfzz".equals(tradeTypeCode))
                            && !pushSettlement) {
                        //参数为转账单不传结算  任何交易类型都不可以选择到结算中心开户的银行账户，其余控制与传结算时一致。
                        if (billDataDto.getCondition() == null) {
                            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", acctopentype));
                            billDataDto.setCondition(conditon);
                        } else {
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", acctopentype));
                        }
                    } else if ("WCZ".equals(transferAccounts.get(0).get("tradetype_code"))) {
                        if (billDataDto.getCondition() == null) {
                            billDataDto.setCondition(conditon);
                        }
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", new int[]{AcctopenTypeEnum.BankAccount.getValue(),AcctopenTypeEnum.OtherFinancial.getValue()}));
                        // 付款银行账户
                        transferAccountWalletHandler.filterTransferAccountWallet(billDataDto, transferAccounts.get(0), Direction.Credit);
                    } else if ("WTX".equals(transferAccounts.get(0).get("tradetype_code"))) {
                        if (billDataDto.getCondition() == null) {
                            billDataDto.setCondition(conditon);
                        }
                        // 付款银行账户
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", AcctopenTypeEnum.DigitalWallet.getValue()));
                    } else if ("WHZ".equals(transferAccounts.get(0).get("tradetype_code"))) {
                        if (billDataDto.getCondition() == null) {
                            billDataDto.setCondition(conditon);
                        }
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", AcctopenTypeEnum.DigitalWallet.getValue()));
                    } else {
                        //2.其他情况 银行开户 开户类型=银行开户 :acctopentype ==0
                        if (billDataDto.getCondition() == null) {
                            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", 0));
                            billDataDto.setCondition(conditon);
                        } else {
                            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", 0));
                        }
                    }
                } else { //交易类型为空的情况：开户类型=银行开户 :acctopentype ==0 财务公司开户 acctopentype == 2 其他金融机构 acctopentype == 3
                    if (billDataDto.getCondition() == null) {
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", acctopentype));
                        billDataDto.setCondition(conditon);
                    } else {
                        billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", acctopentype));
                    }
                }
                // 结算方式为银行转账且结算方式的是否直连为是时，国机-需要拿付款银行账号去账户直连状态的档案里查是否开通银企联=是
                String payBankAccount="payBankAccount";
                //判断payBankAccount是为了避免将收款方银行账号也给过滤了
                if (billDataDto.getDatasource() != null) {
                    if (billDataDto.getDatasource().contains(payBankAccount) && null != transferAccounts.get(0).get("settlemode")){
                        if (settleMethodService.checkSettleMethod(transferAccounts.get(0).get("settlemode").toString())){
                            // 去账户直连状态的档案里查是否开通银企联=是
                            List<String> bankAccountList=bankAccountSettingService.queryBankAccountSettingByFlag();
                            String[] bankAccountIDs= bankAccountList.toArray(new String[0]);
                            SimpleFilterVO businessBankAccountCondition = new SimpleFilterVO(ConditionOperator.and);
                            businessBankAccountCondition.addCondition(new SimpleFilterVO("id", "in", bankAccountIDs));

                            // 结算中心开户
                            SimpleFilterVO settleCenterBankAccountCondition = new SimpleFilterVO(ConditionOperator.and);
                            settleCenterBankAccountCondition.addCondition(new SimpleFilterVO("acctopentype", "eq", 1));
                            if (billDataDto.getCondition() == null) {
                                conditon.appendCondition(ConditionOperator.or, businessBankAccountCondition, settleCenterBankAccountCondition);
                                billDataDto.setCondition(conditon);
                            } else {
                                billDataDto.getCondition().appendCondition(ConditionOperator.or, businessBankAccountCondition, settleCenterBankAccountCondition);
                            }
                        }
                    }
                }
            }
        }
        return new RuleExecuteResult();
    }

    private void doFilter(String accountId, FilterVO conditon, BillDataDto billDataDto) throws Exception {
        //根据收款银行账户id 查询出收款银行账户的开户类型
        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(accountId);
        //开户类型
        Integer acctopentype = enterpriseBankAcctVO.getAcctopentype();
        String bankNumber =enterpriseBankAcctVO.getBankNumber();
        Short[] acctopentypes = new Short[]{0, 2, 3};
        if (billDataDto.getCondition() == null) {
            if (!ObjectUtils.isEmpty(acctopentype)) {
                if (acctopentype.equals(1)) {
                    // 若收款银行账户的开户类型 = 结算中心开户，可参照到相同结算中心的企业银行账户
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", acctopentype));
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankNumber", "eq", bankNumber));
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("accountNature", "eq", 0));
                } else {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", acctopentypes));
                }
                billDataDto.setCondition(conditon);
            }

        } else if (!ObjectUtils.isEmpty(acctopentype)) {
            if (acctopentype.equals(1)) {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", acctopentype));
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("bankNumber", "eq", bankNumber));
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("accountNature", "eq", 0));
            } else {
                billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", acctopentypes));
            }

        }
    }
}
