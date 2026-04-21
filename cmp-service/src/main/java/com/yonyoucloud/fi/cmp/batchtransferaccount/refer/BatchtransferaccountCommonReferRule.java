package com.yonyoucloud.fi.cmp.batchtransferaccount.refer;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.FilterVO;
import com.yonyou.ucf.mdd.common.model.uimeta.filter.vo.SimpleFilterVO;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.base.AbstractCommonRule;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyou.ucf.transtype.model.BdTransType;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount;
import com.yonyoucloud.fi.cmp.batchtransferaccount.BatchTransferAccount_b;
import com.yonyoucloud.fi.cmp.batchtransferaccount.utils.BatchTransferAccountUtil;
import com.yonyoucloud.fi.cmp.cmpentity.VirtualBank;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.common.digitalwallet.impl.BatchTransferAccountWalletHandler;
import com.yonyoucloud.fi.cmp.constant.IBillNumConstant;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.constant.IRefCodeConstant;
import com.yonyoucloud.fi.cmp.enums.AcctopenTypeEnum;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.TransTypeQueryService;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.core.base.ConditionOperator;
import org.imeta.core.base.ObjectUtils;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.schema.QueryOrderby;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * @author xuxbo
 * @date 2025/6/5 16:18
 */
@Component
public class BatchtransferaccountCommonReferRule extends AbstractCommonRule {

    private static final String TRADE_TYPE = "tradeType";

    @Autowired
    private TransTypeQueryService transTypeQueryService;
    @Autowired
    private EnterpriseBankQueryService enterpriseBankQueryService;
    @Autowired
    @Qualifier("batchTransferAccountWalletHandler")
    private BatchTransferAccountWalletHandler batchTransferAccountWalletHandler;

    @Override
    public RuleExecuteResult execute(BillContext billContext, Map<String, Object> paramMap) throws Exception {
        BillDataDto billDataDto = (BillDataDto) getParam(paramMap);
        if (!IBillNumConstant.CMP_BATCHTRANSFERACCOUNT.equals(billDataDto.getBillnum())) {
            return new RuleExecuteResult();
        }
        BizObject bizObject = null;
        List<BizObject> bills = getBills(billContext, paramMap);
        if (CollectionUtils.isNotEmpty(bills)) {
            bizObject = bills.get(0);
        }
        boolean isfilter = "filter".equals(billDataDto.getExternalData());
        if (isfilter) {//如果是过滤区，直接跳过
            // 列表要过滤交易类型
            filterTradeTypeList(billDataDto);
            return new RuleExecuteResult();
        }

        // 银行网点参照过滤
        filterBankDot(billDataDto);

        // 交易类型参照过滤
        filterBillType(billDataDto, bizObject);

        // 银行账户过滤
        filterBankAccount(billDataDto, bizObject);

        // 结算方式过滤
        filterSettleMode(billDataDto, bizObject);

        // 现金账户过滤
        BatchTransferAccountUtil.filterCashAccount(billDataDto, bizObject);

        return new RuleExecuteResult();
    }


    /**
     * 过滤结算方式
     * @param billDataDto
     * @param bizObject
     */
    private void filterSettleMode(BillDataDto billDataDto, BizObject bizObject) throws Exception {
        if (!IRefCodeConstant.REF_SETTLEMENT.equals(billDataDto.getrefCode())) {
            return;
        }
        // 付款结算方式
        filterPaySettleModel(billDataDto);

        // 收款结算方式
        filterRecSettleModel(billDataDto);
    }

    /**
     * 过滤付款结算方式
     * @param billDataDto
     */
    private void filterPaySettleModel(BillDataDto billDataDto) throws Exception {
        if (null == billDataDto.getKey() || !billDataDto.getKey().equals("paySettlemode_name")) {
            return;
        }

        List l = (List)billDataDto.getData();
        if (l.size() > 0){
            BatchTransferAccount batchTransferAccount = (BatchTransferAccount)((List)billDataDto.getData()).get(0);
            BdTransType bdTransType = transTypeQueryService.findById(batchTransferAccount.get(TRADE_TYPE));
            CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
            String tradeTypeCode = (String) jsonObject.get("batchtransferType_ext");

            if (billDataDto.getTreeCondition() == null){
                FilterVO conditon = new FilterVO();
                filterPaySettleModelAttr(conditon, tradeTypeCode, batchTransferAccount);
                billDataDto.setTreeCondition(conditon);
            } else {
                filterPaySettleModelAttr(billDataDto.getTreeCondition(), tradeTypeCode, batchTransferAccount);
            }
            List<QueryOrderby> queryOrderlyList = new ArrayList<>();
            QueryOrderby orders = new QueryOrderby("order",ICmpConstant.ORDER_ASC);
            queryOrderlyList.add(orders);
            billDataDto.setQueryOrders(queryOrderlyList);
        }
    }

    /**
     * 过滤付款结算方式属性
     * @param conditon
     * @param tradeTypeCode
     * @param batchTransferAccount
     */
    private void filterPaySettleModelAttr(FilterVO conditon, String tradeTypeCode, BatchTransferAccount batchTransferAccount) {
        switch (tradeTypeCode) {
            // 现金互转
            case "xjhz":
            // 现金存入
            case "jcxj":
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 1));
                break;
            // 第三方转账
            case "dsfzz":
                if (null != batchTransferAccount.getVirtualBank()) {
                    if (batchTransferAccount.getVirtualBank() == 0) {
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 10));
                    } else {
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                    }
                } else {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 10));
                }
                break;
            // 现金支取
            case "tqxj":
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_IN, new Integer[]{0, 8}));
                break;
            // 银行转账
            case "yhzz":
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                break;
            // 数币钱包充值
            case "sbqbcz":
            // 数币钱包提现
            case "sbqbtx":
            // 数币钱包互转
            case "sbqbhz":
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                break;
            default:
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400685", "不支持的交易类型:") /* "不支持的交易类型:" */ + tradeTypeCode);
        }
    }


    /**
     * 过滤收款结算方式
     * @param billDataDto
     * @throws Exception
     */
    private void filterRecSettleModel(BillDataDto billDataDto) throws Exception {
        if (billDataDto.getKey() == null ||  !billDataDto.getKey().equals("recSettlemode_name")) {
            return;
        }
        // 收款结算方式
        List l = (List)billDataDto.getData();
        if (l.size() > 0) {
            BatchTransferAccount batchTransferAccount = (BatchTransferAccount) ((List) billDataDto.getData()).get(0);
            BdTransType bdTransType = transTypeQueryService.findById(batchTransferAccount.get(TRADE_TYPE));
            CtmJSONObject jsonObject = CtmJSONObject.parseObject(bdTransType.getExtendAttrsJson());
            String tradeTypeCode = (String) jsonObject.get("batchtransferType_ext");
            if (billDataDto.getCondition() == null) {
                FilterVO conditon = new FilterVO();
                filterRecSettleModelAttr(tradeTypeCode, conditon, batchTransferAccount);
                billDataDto.setTreeCondition(conditon);
            } else {
                filterRecSettleModelAttr(tradeTypeCode, billDataDto.getTreeCondition(), batchTransferAccount);
            }
        }

    }

    /**
     * 过滤收款结算方式业务属性
     * @param tradeTypeCode 交易类型编码
     * @param conditon
     * @param batchTransferAccount
     */
    private void filterRecSettleModelAttr(String tradeTypeCode, FilterVO conditon, BatchTransferAccount batchTransferAccount) {
        switch (tradeTypeCode) {
            case "tqxj": // 现金提取
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 1));
                break;
            case "jcxj": // 现金缴存
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                break;
            case "dsfzz": // 三方转账
                if (null != batchTransferAccount.getVirtualBank()) {
                    if (batchTransferAccount.getVirtualBank() == 0) {
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                    } else {
                        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 10));
                    }
                } else {
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                }
                break;
            case "xjhz": // 现金互转
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 1));
                break;
            case "yhzz": // 银行转账
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                break;
            // 数币钱包充值
            case "sbqbcz":
            // 数币钱包提现
            case "sbqbtx":
            // 数币钱包互转
            case "sbqbhz":
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("serviceAttr", ICmpConstant.QUERY_EQ, 0));
                break;
            default:
                throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400685", "不支持的交易类型:") /* "不支持的交易类型:" */ + tradeTypeCode);
        }
    }

    /**
     * 过滤企业银行账户
     * @param billDataDto
     * @param bizObject
     */
    private void filterBankAccount(BillDataDto billDataDto, BizObject bizObject) throws Exception {
        if (!IRefCodeConstant.UCFBASEDOC_BD_ENTERPRISEBANKACCT.equals(billDataDto.getrefCode())) {
            return;
        }
        //银行账户
        BatchTransferAccount_b billb = ((List<BatchTransferAccount_b>)bizObject.get("BatchTransferAccount_b")).get(0);
        if (billDataDto.getCondition() == null) {
            billDataDto.setCondition(new FilterVO());
        }
        // 过滤币种
        filterCurrency(billDataDto.getCondition(), bizObject);
        // 过滤付款银行账户
        filterPayBankAccount(bizObject, billDataDto.getCondition(), billDataDto, billb);
        // 过滤收款银行账户
        filterRecBankAccount(bizObject, billDataDto.getCondition(), billDataDto, billb);
    }

    /**
     * 过滤收款银行账户
     *
     * @param bizObject
     * @param conditon
     * @param billDataDto
     * @param billb
     */
    private void filterRecBankAccount(BizObject bizObject, FilterVO conditon, BillDataDto billDataDto, BatchTransferAccount_b billb) throws Exception {
        if (!"recBankAccountId_account".equals(billDataDto.getKey())) {
            return;
        }
        String tradeTypeCode = bizObject.getString("tradeType_code");
        switch (tradeTypeCode) {
            // 银行转账
            case "BT":
                String accountId = billb.get("payBankAccountId");
                if (StringUtils.isNotEmpty(accountId)) {
                    //1.交易类型为银行转账时，如果收款银行账户有值，付款银行账户参照需要增加过滤条件：开户类型=收款银行账户开户类型；
                    doFilter(accountId, conditon, billDataDto);
                }
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "neq", AcctopenTypeEnum.DigitalWallet.getValue()));
                break;
            // 第三方转账类型
            case "TPT":
                // 银行账户转虚拟户
                if (VirtualBank.BankToVirtual.getValue() == bizObject.getShort("virtualBank")) {
                    // 其他金融机构开户
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", AcctopenTypeEnum.OtherFinancial.getValue()));
                } else if (VirtualBank.VirtualToBank.getValue() == bizObject.getShort("virtualBank")) {
                    // 结算中心开户
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "neq", AcctopenTypeEnum.SettlementCenter.getValue()));
                }
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "neq", AcctopenTypeEnum.DigitalWallet.getValue()));
                break;
            // 现金存入
            case "SC":
                String accountIdSc = billb.get("recBankAccountId");
                if (StringUtils.isNotEmpty(accountIdSc)) {
                    doFilter(accountIdSc, conditon, billDataDto);
                }
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "neq", AcctopenTypeEnum.DigitalWallet.getValue()));
                break;
            // 数币钱包充值
            // 开户类型=钱包账户
            case "WCZ":
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", AcctopenTypeEnum.DigitalWallet.getValue()));
                break;
            // 数币钱包提现
            // 开户类型 = （银行开户、其他金融机构）
            case "WTX":
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", new int[]{AcctopenTypeEnum.BankAccount.getValue(),AcctopenTypeEnum.OtherFinancial.getValue()}));
                filterBatchTransferAccountWallet(billDataDto, billb, Direction.Debit);
                break;
            // 数币钱包互转
            // 开户类型=钱包账户
            case "WHZ":
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", AcctopenTypeEnum.DigitalWallet.getValue()));
                break;
            default:
                throw new CtmException("REC_CMP_BATCHTRANSFERACCOUNT_TRADETYPE_NOT_SUPPORT");
        }

    }

    /**
     * 过滤付款银行账户
     *
     * @param bizObject
     * @param conditon
     * @param billDataDto
     * @param billb
     */
    private void filterPayBankAccount(BizObject bizObject, FilterVO conditon, BillDataDto billDataDto, BatchTransferAccount_b billb) throws Exception {
        if (!"payBankAccountId_account".equals(billDataDto.getKey())) {
            return;
        }
        String tradeTypeCode = bizObject.getString("tradeType_code");
        switch (tradeTypeCode) {
            // 银行转账
            case "BT":
                String accountId = billb.get("recBankAccountId");
                if (StringUtils.isNotEmpty(accountId)) {
                    //1.交易类型为银行转账时，如果收款银行账户有值，付款银行账户参照需要增加过滤条件：开户类型=收款银行账户开户类型；
                    doFilter(accountId, conditon, billDataDto);
                }
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "neq", AcctopenTypeEnum.DigitalWallet.getValue()));
                break;
            // 第三方转账类型
            case "TPT":
                if (bizObject.get("virtualBank") == null) {
                    throw new CtmException(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400686", "请先选择三方转账类型!") /* "请先选择三方转账类型!" */);
                }
                // 虚拟户转银行账户
                if (VirtualBank.VirtualToBank.getValue() == bizObject.getShort("virtualBank")) {
                    // 其他金融机构开户
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", AcctopenTypeEnum.OtherFinancial.getValue()));
                } else if (VirtualBank.BankToVirtual.getValue() == bizObject.getShort("virtualBank")) {
                    // 结算中心开户
                    conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "neq", AcctopenTypeEnum.SettlementCenter.getValue()));
                }
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "neq", AcctopenTypeEnum.DigitalWallet.getValue()));
                break;
            // 现金支取
            case "EC":
                String accountIdEc = billb.get("payBankAccountId");
                if (StringUtils.isNotEmpty(accountIdEc)) {
                    doFilter(accountIdEc, conditon, billDataDto);
                }
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "neq", AcctopenTypeEnum.DigitalWallet.getValue()));
                break;
            // 数币钱包充值
            // 开户类型 = （银行开户、其他金融机构）
            case "WCZ":
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", new int[]{AcctopenTypeEnum.BankAccount.getValue(),AcctopenTypeEnum.OtherFinancial.getValue()}));
                filterBatchTransferAccountWallet(billDataDto, billb, Direction.Credit);
                break;
            // 数币钱包提现
            // 开户类型=钱包账户
            case "WTX":
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", AcctopenTypeEnum.DigitalWallet.getValue()));
                break;
            // 数币钱包互转
            // 开户类型=钱包账户
            case "WHZ":
                conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", AcctopenTypeEnum.DigitalWallet.getValue()));
                break;
            default:
                throw new CtmException("PAY_CMP_BATCHTRANSFERACCOUNT_TRADETYPE_NOT_SUPPORT");
        }
    }

    /**
     * 数币钱包过滤
     * @param billDataDto
     * @param billb
     * @param direction
     */
    private void filterBatchTransferAccountWallet(BillDataDto billDataDto, BatchTransferAccount_b billb, Direction direction) throws Exception {
        String enterpriseAccountId = direction == Direction.Debit ? billb.getPayBankAccountId() : billb.getRecBankAccountId();
        if (StringUtils.isEmpty(enterpriseAccountId)) {
            return;
        }
        batchTransferAccountWalletHandler.filter(billDataDto, billb, direction);
    }

    /**
     * 币种过滤
     * @param conditon
     * @param bizObject
     */
    private void filterCurrency(FilterVO conditon, BizObject bizObject) {
        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CURRENCY_ENABLE_REf, ICmpConstant.QUERY_EQ, "1"));
        if (bizObject.get("currency") != null) {
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO(ICmpConstant.CURRENCY_REf, ICmpConstant.QUERY_EQ, bizObject.get("currency")));
        }
    }

    /**
     * 过滤交易类型
     * @param billDataDto
     * @param bizObject
     */
    private void filterBillType(BillDataDto billDataDto, BizObject bizObject) {
        if (!IRefCodeConstant.TRANSTYPE_BD_BILLTYPEREF.equals(billDataDto.getrefCode())) {
            return;
        }

        if (billDataDto.getCondition() == null){
            FilterVO conditon = new FilterVO();
            filterInnerBillType(bizObject, conditon);
            billDataDto.setCondition(conditon);
        } else {
            filterInnerBillType(bizObject, billDataDto.getCondition());
        }
    }

    /**
     * 过滤交易类型内部方法
     * @param bizObject
     * @param conditon
     */
    private void filterInnerBillType(BizObject bizObject, FilterVO conditon) {
        conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("billtype_id", ICmpConstant.QUERY_EQ, "2283268970412769285"));
        if (bizObject != null) {
            String transferType;
            switch (bizObject.getString("tradeType_code")) {
                // 银行转账
                case "BT":
                    transferType = "yhzz";
                    break;
                // 现金存入
                case "SC":
                    transferType = "jcxj";
                    break;
                // 现金支取
                case "EC":
                    transferType = "tqxj";
                    break;
                // 现金互转
                case "CT":
                    transferType = "xjhz";
                    break;
                // 第三方转账
                case "TPT":
                    transferType = "dsfzz";
                    break;
                // 数币钱包充值
                case "WCZ":
                    transferType = "sbqbcz";
                    break;
                // 数币钱包提现
                case "WTX":
                    transferType = "sbqbtx";
                    break;
                // 数币钱包互转
                case "WHZ":
                    transferType = "sbqbhz";
                    break;
                default:
                    throw new CtmException("BILLTYPE_CMP_BATCHTRANSFERACCOUNT_TRADETYPE_NOT_SUPPORT");
            }
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("extend_attrs_json", ICmpConstant.QUERY_LIKE, transferType));
        }
    }

    /**
     * 银行网点过滤
     * @param billDataDto
     */
    private void filterBankDot(BillDataDto billDataDto) {
        if(!IRefCodeConstant.UCFBASEDOC_BD_BANKDOTREF.equals(billDataDto.getrefCode())){
            return;
        }
        FilterVO conditon = new FilterVO();
        if (billDataDto.getCondition() == null) {
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ,1));
            billDataDto.setCondition(conditon);
        } else {
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("enable", ICmpConstant.QUERY_EQ,1));
        }
    }

    /**
     * 交易类型过滤
     * @param billDataDto
     */
    private void filterTradeTypeList(BillDataDto billDataDto) {
        if(!IRefCodeConstant.UCFBASEDOC_BD_BANKDOTREF.equals(billDataDto.getrefCode())){
            return;
        }
        if (billDataDto.getCondition() == null) {
            FilterVO conditon = new FilterVO();
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("form_id", ICmpConstant.QUERY_EQ,ICmpConstant.BATCH_TRANSFER_ACCOUNT_FIRM_ID));
            billDataDto.setCondition(conditon);
        } else {
            billDataDto.getCondition().appendCondition(ConditionOperator.and, new SimpleFilterVO("form_id", ICmpConstant.QUERY_EQ,ICmpConstant.BATCH_TRANSFER_ACCOUNT_FIRM_ID));
        }
    }


    /**
     * 账户过滤
     * @param accountId
     * @param conditon
     * @param billDataDto
     * @throws Exception
     */
    private void doFilter(String accountId, FilterVO conditon, BillDataDto billDataDto) throws Exception {
        //根据收款银行账户id 查询出收款银行账户的开户类型
        EnterpriseBankAcctVO enterpriseBankAcctVO = enterpriseBankQueryService.findById(accountId);
        //开户类型
        Integer acctopentype = enterpriseBankAcctVO.getAcctopentype();
        String bankNumber =enterpriseBankAcctVO.getBankNumber();
        // 银行开户、财务公司开户、其他金融机构开户
        Integer[] acctopentypes = new Integer[]{AcctopenTypeEnum.BankAccount.getValue(), AcctopenTypeEnum.FinancialCompany.getValue(),
                AcctopenTypeEnum.OtherFinancial.getValue()};
        if (billDataDto.getCondition() == null) {
            if (!ObjectUtils.isEmpty(acctopentype)) {
                filterWithOppBankAccount(conditon, acctopentype, bankNumber, acctopentypes);
                billDataDto.setCondition(conditon);
            }
        } else if (!ObjectUtils.isEmpty(acctopentype)) {
            filterWithOppBankAccount(billDataDto.getCondition(), acctopentype, bankNumber, acctopentypes);
        }
    }

    /**
     * 根据对方的账户过滤本方的账户
     * @param conditon
     * @param acctopentype
     * @param bankNumber
     * @param acctopentypes
     */
    private void filterWithOppBankAccount(FilterVO conditon, Integer acctopentype, String bankNumber, Integer[] acctopentypes) {
        List<Integer> acctopentypeList = Arrays.asList(acctopentypes);
        if (acctopentype.equals(AcctopenTypeEnum.SettlementCenter.getValue())) {
            // 若d对方银行账户的开户类型 = 结算中心开户，可参照到相同结算中心的企业银行账户
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "eq", acctopentype));
            // conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("bankNumber", "eq", bankNumber));
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("accountNature", "eq", 0));
        } else if (acctopentypeList.contains(acctopentype)) {
            // 若对方银行账户的开户类型 = 银行开户、财务公司开户、其他金融机构开户，可参照到相同开户类型的企业银行账户
            conditon.appendCondition(ConditionOperator.and, new SimpleFilterVO("acctopentype", "in", acctopentypes));
        }
    }

}
