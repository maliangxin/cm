package com.yonyoucloud.fi.cmp.bankdealdetail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.yonbip.ctm.bankconnection.BankConnectionAdapterContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyou.yonbip.ctm.logger.business.CTMCMPBusinessLogService;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.RefundAutoCheckRuleService;
import com.yonyoucloud.fi.cmp.common.service.CtmCmpCheckRepeatDataService;
import com.yonyoucloud.fi.cmp.smartclassify.BillSmartClassifyService;
import com.yonyoucloud.fi.cmp.util.JedisLockUtils;
import com.yonyoucloud.fi.cmp.util.basedoc.CurrencyQueryService;
import com.yonyoucloud.fi.cmp.util.basedoc.EnterpriseBankQueryService;
import com.yonyoucloud.fi.cmp.util.business.CmpCommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Desc 逆向生成红冲数据与凭证测试用例
 * @Author zhaoyulong
 * @Email zhaoyulong@yonyou.com
 * @Date 2024/8/2
 * @Version 1.0.0
 **/
@Slf4j
@ExtendWith(MockitoExtension.class)
public class BankDealDetailServiceImplTest {

    @Mock
    YmsOidGenerator ymsOidGenerator;
    @Mock
    BankDetailRelationSettleService bankDetailRelationSettleService;

    @Mock
    private EnterpriseBankQueryService enterpriseBankQueryService;

    @Mock
    BaseRefRpcService baseRefRpcService;
    @Mock
    private BankConnectionAdapterContext bankConnectionAdapterContext;
    @Mock
    private CurrencyQueryService currencyQueryService;
    @Mock
    private CtmCmpCheckRepeatDataService checkRepeatDataService;
    //单据智能分类service
    @Mock
    private BillSmartClassifyService billSmartClassifyService;
    @Mock
    private RefundAutoCheckRuleService refundAutoCheckRuleService;

    @Mock
    private CTMCMPBusinessLogService ctmcmpBusinessLogService;
    MockedStatic<AppContext> appContextMockedStatic;
    MockedStatic<JedisLockUtils> jedisLockUtilsMockedStatic;
    MockedStatic<CmpCommonUtil> cmpCommonUtilMockedStatic;

    @InjectMocks
    BankDealDetailService2 bankDealDetailService;

    @BeforeEach
    public void before() {
        bankDealDetailService = new BankDealDetailService2(enterpriseBankQueryService, currencyQueryService, ymsOidGenerator);
        appContextMockedStatic = Mockito.mockStatic(AppContext.class);
        appContextMockedStatic.when(AppContext::getTenantId).thenReturn(3770614978368752L);
        jedisLockUtilsMockedStatic = Mockito.mockStatic(JedisLockUtils.class);
        cmpCommonUtilMockedStatic = Mockito.mockStatic(CmpCommonUtil.class);
    }

    @AfterEach
    public void after() {
        appContextMockedStatic.close();
        jedisLockUtilsMockedStatic.close();
        cmpCommonUtilMockedStatic.close();
    }

    @Test
    @DisplayName("执行流水数据分析")
    public void ExecuteAnalysisDetailData() throws Exception {
        String jsonString = "{\"is_refund\":\"2\",\"refund_original_transaction\":\"123456789789\",\"acct_bal\":\"0.56\",\"bank_check_code\":\"D9D68CB268496E61BC911A2E2A97BDD1\"," +
                "\"bank_reconciliation_code\":\"D9D68CB268496E61BC911A2E2A97BDD1\",\"bank_seq_no\":\"31351202409010923096505\",\"bank_temp_check_code\":\"70116768\"," +
                "\"curr_code\":\"CNY\",\"dc_flag\":\"d\",\"detail_check_id\":\"80432A88B2985AE84781E0EE5F492F59\",\"remark\":\"日常报销费用\",\"to_acct_bank_name\":\"招商银行\"," +
                "\"to_acct_name\":\"杨柳\",\"to_acct_no\":\"9555500104929723\",\"tran_amt\":\"0.01\",\"tran_date\":\"20240901\",\"tran_time\":\"070118\",\"unique_no\":\"237D15E531B0B6AA90B982FCA471BDEF\"," +
                "\"use_name\":\"日常报销费用\",\"value_date\":\"20240901\"},{\"is_refund\":\"1\",\"refund_original_transaction\":\"12345678123456\",\"acct_bal\":\"0.55\"," +
                "\"bank_check_code\":\"71B5BD3D264DF65CFE35FBD7ECFCE9BF\",\"bank_reconciliation_code\":\"71B5BD3D264DF65CFE35FBD7ECFCE9BF\",\"bank_seq_no\":\"31352202409010923096640\"," +
                "\"bank_temp_check_code\":\"70117433\",\"curr_code\":\"CNY\",\"dc_flag\":\"d\",\"detail_check_id\":\"A735A0587DB464FE10EDB777AA4EDC06\",\"remark\":\"取款\"," +
                "\"to_acct_bank_name\":\"广发银行股份有限公司北京万柳支行\",\"to_acct_name\":\"北京畅捷通支付技术有限公司\",\"to_acct_no\":\"9550880054837400440\",\"tran_amt\":\"0.01\"," +
                "\"tran_date\":\"20240901\",\"tran_time\":\"070119\",\"unique_no\":\"957240B33AF02E8331A5E2FACCD15EDD\",\"use_name\":\"取款\",\"value_date\":\"20240901\"}," +
                "{\"is_refund\":\"2\",\"refund_original_transaction\":\"123456789789\",\"acct_bal\":\"0.54\",\"bank_check_code\":\"6FBF96CB127FEFFECFE951F7A147CF8F\"," +
                "\"bank_reconciliation_code\":\"6FBF96CB127FEFFECFE951F7A147CF8F\",\"bank_seq_no\":\"31351202409010923103690\",\"bank_temp_check_code\":\"70149145\"," +
                "\"curr_code\":\"CNY\",\"dc_flag\":\"d\",\"detail_check_id\":\"8EE6C8F8B00C7EE85487F05654E49D19\",\"remark\":\"20240901MS11T10\",\"to_acct_bank_name\":\"广发银行股份有限公司北京万柳支行\"," +
                "\"to_acct_name\":\"北京畅捷通支付技术有限公司\",\"to_acct_no\":\"9550880054837400440\",\"tran_amt\":\"0.01\",\"tran_date\":\"20240901\",\"tran_time\":\"070151\"," +
                "\"unique_no\":\"A55A643C3F7B0AA799F4A449F971B4D0\",\"use_name\":\"20240901MS11T10\",\"value_date\":\"20240901\"},{\"is_refund\":\"1\",\"refund_original_transaction\":\"123456789789\"," +
                "\"acct_bal\":\"0.53\",\"bank_check_code\":\"67DCE3DB3652A7005558DE1AE713D244\",\"bank_reconciliation_code\":\"67DCE3DB3652A7005558DE1AE713D244\",\"bank_seq_no\":\"31351202409010923106532\"," +
                "\"bank_temp_check_code\":\"70202256\",\"curr_code\":\"CNY\",\"dc_flag\":\"d\",\"detail_check_id\":\"BDE09BC624708B41B4D754443335B76C\",\"remark\":\"20240901回归11B10\"," +
                "\"to_acct_bank_name\":\"广发银行股份有限公司北京万柳支行\",\"to_acct_name\":\"北京畅捷通支付技术有限公司\",\"to_acct_no\":\"9550880054837400440\",\"tran_amt\":\"0.01\",\"tran_date\":\"20240901\"," +
                "\"tran_time\":\"070204\",\"unique_no\":\"F8CC66779ADF485DD44E91EA761838E0\",\"use_name\":\"20240901回归11B10\",\"value_date\":\"20240901\"},{\"acct_bal\":\"0.52\"," +
                "\"bank_check_code\":\"9EC37E3134384F56DE0D6A3E84613ABD\",\"bank_reconciliation_code\":\"9EC37E3134384F56DE0D6A3E84613ABD\",\"bank_seq_no\":\"31351202409010923114087\"," +
                "\"bank_temp_check_code\":\"70235443\",\"curr_code\":\"CNY\",\"dc_flag\":\"d\",\"detail_check_id\":\"AC251024D0AF4774F0167A02B5374A23\",\"remark\":\"DQ20240901MS11T10P\"," +
                "\"to_acct_bank_name\":\"广发银行股份有限公司北京万柳支行\",\"to_acct_name\":\"北京畅捷通支付技术有限公司\",\"to_acct_no\":\"9550880054837400440\",\"tran_amt\":\"0.01\",\"tran_date\":\"20240901\",\"tran_time\":\"070237\",\"unique_no\":\"04582157AD943B7109CBA50641E642D5\",\"use_name\":\"DQ20240901MS11T10P\",\"value_date\":\"20240901\"},{\"acct_bal\":\"0.51\",\"bank_check_code\":\"CA177CAB293ACBEA47375E5C37D39AA9\",\"bank_reconciliation_code\":\"CA177CAB293ACBEA47375E5C37D39AA9\",\"bank_seq_no\":\"31353202409010923118618\",\"bank_temp_check_code\":\"70257389\",\"curr_code\":\"CNY\",\"dc_flag\":\"d\",\"detail_check_id\":\"21F4E161A56E8C16D29C530A65B24135\",\"remark\":\"20240901回归11B10\"," +
                "\"to_acct_bank_name\":\"广发银行股份有限公司北京万柳支行\",\"to_acct_name\":\"北京畅捷通支付技术有限公司\",\"to_acct_no\":\"9550880054837400440\"," +
                "\"tran_amt\":\"0.01\",\"tran_date\":\"20240901\",\"tran_time\":\"070258\",\"unique_no\":\"C2E15EFBC31ADE881622F91871B89D07\",\"use_name\":\"20240901回归11B10\",\"value_date\":\"20240901\"},{\"acct_bal\":\"0.50\",\"bank_check_code\":\"ECA2B835FB70FA01F2484D5417771CCF\",\"bank_reconciliation_code\":\"ECA2B835FB70FA01F2484D5417771CCF\",\"bank_seq_no\":\"31353202409010923121158\",\"bank_temp_check_code\":\"70308754\",\"curr_code\":\"CNY\",\"dc_flag\":\"d\",\"detail_check_id\":\"964C8F4808AB8BA87B57098084185974\",\"remark\":\"日常报销费用\",\"to_acct_bank_name\":\"招商银行\",\"to_acct_name\":\"杨柳\"," +
                "\"to_acct_no\":\"9555500104929723\",\"tran_amt\":\"0.01\",\"tran_date\":\"20240901\",\"tran_time\":\"070310\",\"unique_no\":\"93AA072FDEA83454E48297B4A66BACDA\",\"use_name\":\"日常报销费用\",\"value_date\":\"20240901\"},{\"acct_bal\":\"0.49\",\"bank_check_code\":\"FE53DE5470EC40BA22E3F2946B8C4B74\"," +
                "\"bank_reconciliation_code\":\"FE53DE5470EC40BA22E3F2946B8C4B74\",\"bank_seq_no\":\"31351202409010934561621\",\"bank_temp_check_code\":\"13410686\",\"curr_code\":\"CNY\",\"dc_flag\":\"d\",\"detail_check_id\":\"322F3F5D5A541CAF9C1F3D1BBF007F7D\",\"remark\":\"取款\",\"to_acct_bank_name\":\"中国工商银行股份有限公司深圳东门支行\",\"to_acct_name\":\"北京用友融联科技有限公司\",\"to_acct_no\":\"4000021119201298365\",\"tran_amt\":\"0.01\",\"tran_date\":\"20240901\",\"tran_time\":\"213412\",\"unique_no\":\"B2B4BFE5706800C88B3B9065B225BF58\",\"use_name\":\"取款\",\"value_date\":\"20240901\"}";
        try {
            ObjectMapper objectMapper = com.yonyou.yonbip.ctm.json.ObjectMapperUtils.objectMapper;
            // 将JSON字符串转换为Map对象
            java.util.Map<String, Object> map = objectMapper.readValue(jsonString, java.util.Map.class);
            CtmJSONObject detailData = CtmJSONObject.toJSON(map);
            Map<String, Object> enterpriseInfo = new HashMap<>();
            enterpriseInfo.put("accEntityId", "1977450024589066250");
            enterpriseInfo.put("accountId", "1977466817929543688");
            List<BankDealDetail> bankDealDetails = new ArrayList<>();
            List<BankReconciliation> bankRecords = new ArrayList<>();
            String currency = null;
            bankDealDetailService.analysisDetailData(detailData, enterpriseInfo, bankDealDetails, bankRecords, currency);
            Map<String, List<BankDealDetail>> allmap = bankDealDetailService.checkBankDealDetailRepeat(bankDealDetails);
            System.out.println(allmap.values());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
