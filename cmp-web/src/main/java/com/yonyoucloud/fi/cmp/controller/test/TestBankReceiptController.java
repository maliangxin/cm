package com.yonyoucloud.fi.cmp.controller.test;

import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyou.yonbip.ctm.error.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.util.auth.CMPDiworkPermission;
import com.yonyoucloud.fi.cmp.bankelectronicreceipt.BankElectronicReceipt;
import com.yonyoucloud.fi.cmp.bankreceipt.service.BankReceiptService;
import com.yonyoucloud.fi.cmp.constant.EnvConstant;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.constant.ITransCodeConstant;
import com.yonyoucloud.fi.cmp.util.JsonUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.YQLUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

import static com.yonyoucloud.fi.cmp.util.YQLUtils.writeYQLTestDataToFile;

/**
 * @author: liaojbo
 * @Date: 2025年04月07日 16:56
 * @Description:
 */
@Controller
@Slf4j
@Lazy
@RequestMapping("/test")
public class TestBankReceiptController extends BaseController {

    static Map<String, Object> sheetNameToTranscode = new HashMap<>();

    static {
        sheetNameToTranscode.put("账户交易流水", ITransCodeConstant.QUERY_ACCOUNT_TRANSACTION_DETAIL);//@notranslate
        sheetNameToTranscode.put("账户交易回单", ITransCodeConstant.QUERY_ACCOUNT_RECEIPT_TRANSACTION_DETAIL);//@notranslate
        sheetNameToTranscode.put("历史余额", ITransCodeConstant.QUERY_HIS_ACCOUNT_BALANCE);//@notranslate
        sheetNameToTranscode.put("实时余额", ITransCodeConstant.QUERY_ACCOUNT_BALANCE);//@notranslate
        sheetNameToTranscode.put("电子对账单", ITransCodeConstant.QUERY_ELECTRONIC_STATEMENT_CONFIRM);//@notranslate
    }



    @Autowired
    private BankReceiptService bankReceiptService;

    @PostMapping(value = "/writeYQLTestData")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECEIPTMATCH)
    public void writeYQLTestData(@RequestBody CtmJSONObject params, HttpServletResponse response) {
        if (!EnvConstant.YQLTestData) {
            return;
        }
        String transCode = params.get("transCode").toString();
        //Object acctNo = params.get("acct_no");
        //Object currCode = params.get("curr_code");
        //Object startDate = params.get("startDate");
        //Object endDate = params.get("endDate");
        CtmJSONObject testData = new CtmJSONObject((Map<String, Object>) params.get("testData"));
        YQLUtils.writeYQLTestData(transCode, testData, params);
        renderJson(response, ResultMessage.success());

    }

    @PostMapping(value = "/writeYQLTestDataFromFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void writeYQLTestDataFromFile(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
        if (!EnvConstant.YQLTestData) {
            return;
        }

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.toLowerCase().endsWith(".txt")) {
                // 处理txt文件
                handleTxtFile(file);
            } else {
                // 解析Excel文件
                Map<String, List<Map<String, Object>>> excelDataMap = parseExcelFile(file);

                for (Map.Entry<String, List<Map<String, Object>>> excelData : excelDataMap.entrySet()) {

                    // 获取transCode和testData
                    //String transCode = (String) excelData.get("transCode");
                    String sheetName = (String) excelData.getKey();
                    String transCode = (String) sheetNameToTranscode.get(sheetName);
                    List<Map<String, Object>> rowList = excelData.getValue();
                    // 调用原有的写入测试数据方法
                    YQLUtils.writeYQLTestDataFromRowList(transCode, rowList);
                }
            }
            renderJson(response, ResultMessage.success());
        } catch (Exception e) {
            log.error("解析Excel文件或处理测试数据时发生错误", e);
            renderJson(response, ResultMessage.error("处理Excel文件失败"));//@notranslate
        }
    }

    private void handleTxtFile(MultipartFile file) throws IOException {
        // 将txt文件内容读取为字符串
        String content = new String(file.getBytes(), "UTF-8");

        // 可以在这里对content进行进一步处理
        // 例如：去除首尾空白字符
        content = content.trim();

        // 记录日志或进行其他业务处理
        log.info("解析到的txt文件内容: {}", content);
        CtmJSONObject jsonObject = CtmJSONObject.parseObject(content);
        YQLUtils.writeYQLTestDataToFile(jsonObject, file.getOriginalFilename());
    }

    @PostMapping(value = "/writeYQLTestDataFromExcel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void writeYQLTestData(@RequestParam("file") MultipartFile file, HttpServletResponse response) {
        if (!EnvConstant.YQLTestData) {
            return;
        }

        try {
            // 解析Excel文件
            Map<String, List<Map<String, Object>>> excelDataMap = parseExcelFile(file);

            for (Map.Entry<String, List<Map<String, Object>>> excelData : excelDataMap.entrySet()) {

                // 获取transCode和testData
                //String transCode = (String) excelData.get("transCode");
                String sheetName = (String) excelData.getKey();
                String transCode = (String) sheetNameToTranscode.get(sheetName);
                List<Map<String, Object>> rowList = excelData.getValue();
                // 调用原有的写入测试数据方法
                YQLUtils.writeYQLTestDataFromRowList(transCode, rowList);
            }
            renderJson(response, ResultMessage.success());
        } catch (Exception e) {
            log.error("解析Excel文件或处理测试数据时发生错误", e);
            renderJson(response, ResultMessage.error("处理Excel文件失败"));//@notranslate
        }
    }

private Map<String, List<Map<String, Object>>> parseExcelFile(MultipartFile file) throws IOException {
    // 实现Excel解析逻辑
    // 返回值修改为 Map<String, List<Map<String, Object>>>
    // 外层Map的key是sheet名称，内层List包含该sheet中每一行的数据
    Workbook workbook = WorkbookFactory.create(file.getInputStream());

    Map<String, List<Map<String, Object>>> result = new HashMap<>();

    // 遍历所有sheet
    for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
        Sheet sheet = workbook.getSheetAt(sheetIndex);
        String sheetName = sheet.getSheetName();

        List<Map<String, Object>> sheetData = new ArrayList<>();

        if (sheet.getLastRowNum() < 1) {
            // 如果sheet少于2行(标题行+数据行)，跳过处理
            result.put(sheetName, sheetData);
            continue;
        }

        // 第3行是标题行
        Row headerRow = sheet.getRow(2);

        // 从第4行开始处理数据行
        for (int rowIndex = 3; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row dataRow = sheet.getRow(rowIndex);
            if (dataRow == null) continue;

            Map<String, Object> rowData = new HashMap<>();
            // 第1行是标记列，不读取
            for (int cellIndex = 1; cellIndex < headerRow.getLastCellNum(); cellIndex++) {
                Cell headerCell = headerRow.getCell(cellIndex);
                Cell dataCell = dataRow.getCell(cellIndex);

                String headerValue = getCellValueAsString(headerCell);
                Object dataValue = getCellValue(dataCell);

                rowData.put(headerValue, dataValue);
            }

            sheetData.add(rowData);
        }

        result.put(sheetName, sheetData);
    }

    workbook.close();
    return result;
}


    private String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            default:
                return null;
        }
    }

    @PostMapping(value = "/payment/queryReceiptDetailUnNeedUkey")
    @CMPDiworkPermission(IServicecodeConstant.BANKRECEIPTMATCH)
    public void queryAccountReceiptDetailUnNeedUkey(@RequestBody CtmJSONObject params, HttpServletResponse response) throws Exception {
        if (!EnvConstant.YQLTestData) {
            return;
        }

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
        if (params.getJSONObject("data") != null) {
            testData = params.getJSONObject("data");
            CtmJSONObject record = (CtmJSONObject) testData.getJSONObject("response_body").getJSONArray("record").get(0);
            unique_no = record.getString("unique_no");
        } else {
            testData = YQLUtils.buildTestData(directoryParam, unique_no);
        }
        CtmJSONObject testResult = new CtmJSONObject();

        if (testWriteData(uri, unique_no)) {
            testResult.put("写入数据前检验", "测试成功");//@notranslate
        } else {
            testResult.put("写入数据前检验", "测试失败");//@notranslate
            throw new CtmException("写入数据前检验失败，已经存在唯一码相同的数据");//@notranslate
        }
        YQLUtils.writeYQLTestData(transCode, testData, directoryParam);
        bankReceiptService.queryAccountReceiptDetailUnNeedUkey(directoryParam);
        if (testWriteData(uri, unique_no)) {
            testResult.put("写入数据", "测试成功");//@notranslate
        } else {
            testResult.put("写入数据", "测试失败");//@notranslate
        }
        try {
            bankReceiptService.queryAccountReceiptDetailUnNeedUkey(directoryParam);
            testResult.put("不能重复写入数据", "测试失败");//@notranslate
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            testResult.put("不能重复写入数据", "测试成功:" + e.getMessage());//@notranslate
        }

        if (delTestData(uri, unique_no)) {
            testResult.put("删除测试数据", "删除成功");//@notranslate
        } else {
            testResult.put("删除测试数据", "删除失败");//@notranslate
        }
        renderJson(response, ResultMessage.data(testResult.toString()));
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
