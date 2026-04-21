package com.yonyoucloud.fi.cmp.controller.bankreceipt;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.autoparam.AutoConfig;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptService;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.YQLUtils;
import io.edap.util.E;
import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author: liaojbo
 * @Date: 2025年04月07日 16:56
 * @Description:
 */
@Controller
@Slf4j
@Lazy
@RequestMapping("/test")
public class TestBankReceiptController  extends BaseController {

    @Autowired
    private BankReceiptService bankReceiptService;
    @Autowired
    private ITransCodeConstant iTransCodeConstant;

    @PostMapping(value = "/writeYQLTestData")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECEIPTMATCH)
    public void writeYQLTestData(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        String transCode = params.get("transCode").toString();
        //Object acctNo = params.get("acct_no");
        //Object currCode = params.get("curr_code");
        //Object startDate = params.get("startDate");
        //Object endDate = params.get("endDate");
        CtmJSONObject testData = (CtmJSONObject) params.get("testData");
        YQLUtils.writeYQLTestData(transCode, testData, params);

    }

    @PostMapping(value = "/payment/queryReceiptDetailUnNeedUkey")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECEIPTMATCH)
    public void queryAccountReceiptDetailUnNeedUkey(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        try {
    //        {
    //            "uid": "a42666e4ab9cec95ca24d9802a0359cc1605f3d7e708623fa16b8a1d9c2c",
    //                "accEntity": [
    //            "1877541844060471306"
    //],
    //            "accountId": [
    //            "2065053544925888519"
    //],
    //            "startDate": "2025-04-07",
    //                "endDate": "2025-04-07",
    //                "begNum": 1
    //        }

            String transCode = ITransCodeConstant.QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL;
            String uri = BankElectronicReceipt.ENTITY_NAME;

            // 测试数据配置
            final String testUid = "a42666e4ab9cec95ca24d9802a0359cc1605f3d7e708623fa16b8a1d9c2c";
            final String testAccEntity = "1877541844060471306";
            final String testAccountId = "2065053544925888519";
            final String testStartDate = "2025-02-14";
            final String testEndDate = "2025-02-14";
            final String testCurrencyCode = "CNY";
            //生成一个随机数，用于生成唯一标识符
            String unique_no = UUID.randomUUID().toString();

            CtmJSONObject queryParam = new CtmJSONObject();
            queryParam.put("uid", testUid); // 用于回写前端进度条
            queryParam.put("accEntity", testAccEntity); // 会计主体，可以是单个字符串或多个会计主体的列表
            queryParam.put("accountId", testAccountId); // 银行账户ID，可以是单个字符串或多个银行账户ID的列表
            queryParam.put("startDate", testStartDate); // 查询的开始日期，格式为 "yyyy-MM-dd"
            queryParam.put("endDate", testEndDate); // 查询的结束日期，格式为 "yyyy-MM-dd"
            //queryParam.put("currencyCode", "币种代码"); // 币种代码，用于指定查询的币种
            //queryParam.put("channel", "渠道"); // 渠道信息，用于指定查询的渠道
            //queryParam.put("serviceCode", "服务代码"); // 服务代码，用于标识当前操作的服务
            CtmJSONObject directoryParam = new CtmJSONObject();
            EnterpriseBankAcctVO enterpriseBankAcctVO = QueryBaseDocUtils.queryEnterpriseBankAccountVOById(testAccountId).get();
            directoryParam.put("acct_no", enterpriseBankAcctVO.getAccount()); // 会计主体，可以是单个字符串或多个会计主体的列表
            directoryParam.put("acct_name", enterpriseBankAcctVO.getAcctName()); // 会计主体，可以是单个字符串或多个会计主体的列表
            directoryParam.put("curr_code", testCurrencyCode); // 银行账户ID，可以是单个字符串或多个银行账户ID的列表
            directoryParam.put("startDate", testStartDate); // 查询的开始日期，格式为 "yyyy-MM-dd"
            directoryParam.put("endDate", testEndDate); // 查询的结束日期，格式为 "yyyy-MM-dd"

            CtmJSONObject testData = new CtmJSONObject();
            if (params.getJSONObject("data")!= null) {
                testData = params.getJSONObject("data");
                CtmJSONObject record = (CtmJSONObject) testData.getJSONObject("response_body").getJSONArray("record").get(0);
                unique_no = record.getString("unique_no");
            } else {
                testData = YQLUtils.buildTestData(directoryParam, unique_no);
            }
            CtmJSONObject testResult = new CtmJSONObject();

            if (testWriteData(uri, unique_no)) {
                testResult.put("写入数据前检验", "测试成功");
            } else {
                testResult.put("写入数据前检验", "测试失败");
                throw new CtmException("写入数据前检验失败，已经存在唯一码相同的数据");
            }
            YQLUtils.writeYQLTestData(transCode, testData, directoryParam);
            bankReceiptService.queryAccountReceiptDetailUnNeedUkey(directoryParam);
            if (testWriteData(uri, unique_no)) {
                testResult.put("写入数据", "测试成功");
            } else {
                testResult.put("写入数据", "测试失败");
            }
            try {
                bankReceiptService.queryAccountReceiptDetailUnNeedUkey(directoryParam);
                testResult.put("不能重复写入数据", "测试失败");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                testResult.put("不能重复写入数据", "测试成功:" + e.getMessage());
            }

            if (delTestData(uri, unique_no)) {
                testResult.put("删除测试数据", "删除成功");
            } else {
                testResult.put("删除测试数据", "删除失败");
            }
            renderJson(response, ResultMessage.data(testResult.toString()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            renderJson(response, ResultMessage.error("测试查询账户电子回单异常：" + e.getMessage()));
        }
    }

    private boolean delTestData(String uri, String uniqueNo) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("id");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("uniqueCode").eq(uniqueNo));
        querySchema.addCondition(group);
        List list = MetaDaoHelper.query(uri, querySchema);
        try {
            MetaDaoHelper.delete(uri, list);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private boolean testWriteData(String uri, String unique_no) throws Exception {
        QuerySchema querySchema = QuerySchema.create().addSelect("count(1)");
        QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("uniqueCode").eq(unique_no));
        querySchema.addCondition(group);
        List<Map<String, Object>> countList = MetaDaoHelper.query(uri, querySchema);
        int count = Integer.parseInt(countList.get(0).get("count").toString());
        if (count == 1) {
            return true;
        } else {
            return false;
        }
    }

}
