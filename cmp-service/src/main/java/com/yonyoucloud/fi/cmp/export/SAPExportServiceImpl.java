package com.yonyoucloud.fi.cmp.export;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yonyou.iuap.international.MultiLangText;
import com.yonyou.iuap.ml.vo.LanguageVO;
import com.yonyou.iuap.ucf.common.i18n.MessageUtils;
import com.yonyou.iuap.ucf.common.ml.MultiLangContext;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.common.model.Pager;
import com.yonyou.ucf.mdd.common.model.rule.RuleExecuteResult;
import com.yonyou.ucf.mdd.common.model.uimeta.ui.Entity;
import com.yonyou.ucf.mdd.common.model.uimeta.ui.Field;
import com.yonyou.ucf.mdd.common.model.uimeta.ui.ViewModel;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bill.rule.template.CommonOperator;
import com.yonyou.ucf.mdd.ext.bill.utils.BillUtils;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.UIMetaUtils;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.poi.model.CellData;
import com.yonyou.ucf.mdd.ext.poi.model.ExcelExportData;
import com.yonyou.ucf.mdd.ext.poi.model.ExcelField;
import com.yonyou.ucf.mdd.ext.service.DefaultBillService;
import com.yonyou.ucf.mdd.ext.util.Toolkit;
import com.yonyou.ucf.mdd.ext.util.json.GsonHelper;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.imeta.core.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
@Slf4j
public class SAPExportServiceImpl extends DefaultBillService implements ISAPExportService {
    private static final String OUTPUTEXPORTTYPE = "output";
    private static Pattern SHEET_NAME_PATTERN = Pattern.compile("[(\\\\*),(\\/*),(\\?*),(\\**),(\\[*),(\\]*)]");
    private static String DATA_FORMAT = "dataformat";
    private static String SHEET_SUFFIX_NAME = "SAP";
    @Autowired
    private ICtmExportMapService ctmExportMapService;
    @Autowired
    private CmCommonService cmCommonService;

    @Override
    public ExcelExportData sapExport(BillDataDto bill) throws Exception {
        return export(bill);
    }



    @Override
    public ExcelExportData export(BillDataDto bill) throws Exception {
        return excelSapExport(bill);
    }

    public ExcelExportData excelSapExport(BillDataDto bill) throws Exception {
        try {
            Long start = System.currentTimeMillis();
            bill.setIsIncludeMeta(true);
            RuleExecuteResult result = null;
            Object vm = null;
            boolean isSubmit = (bill.getData() != null);
            String exportType = bill.getAction();
            boolean outputFlag = false;
            if (!StringUtils.isEmpty(exportType) && OUTPUTEXPORTTYPE.equalsIgnoreCase(exportType)) {
                outputFlag = true;
            }
            if (isSubmit) { // 提交数据
                result = new RuleExecuteResult(bill.getData());
                vm = UIMetaUtils.getViewModel(bill.getBillnum(), bill.getPartParam());
            } else if (bill.getId() != null) {// 卡片
                result = new CommonOperator(OperationTypeEnum.DETAIL).execute(bill);
            } else {
                // TODO:导出暂时过滤i18doc配置
                AppContext.setThreadContext("disableI18n", true);
                if (!Toolkit.isEmpty(bill.getTreename())) {
                    result = new CommonOperator(OperationTypeEnum.QUERYTREE).execute(bill);
                } else {
                    result = new CommonOperator(OperationTypeEnum.QUERY).execute(bill);
                }
            }
            log.info("getdata time:" + (System.currentTimeMillis() - start));
            if (result.getMsgCode() != 1) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101277"),result.getMessage());
            } else {
                ExcelExportData exportData = null;
                List<Object> datas = null;
                List sumDatas = null;
                HashMap<String, Object> data = new HashMap<>();
                if (isSubmit) { // 提交数据
                    datas = CtmJSONObject.parseObject(result.getData().toString(), List.class);
                } else if (bill.getId() != null) { // 卡片
                    data = (HashMap) result.getData();
                    vm = data.get("_viewmodel");
                } else {
                    Pager pager = (Pager) result.getData();
                    vm = pager.getViewmodel();
                    if (vm == null) vm = UIMetaUtils.getViewModel(bill.getBillnum(), bill.getPartParam());
                    datas = pager.getRecordList();
                    sumDatas = pager.getSumRecordList();
//                    log.info("BillBiz导出的SumMap数据:" + CtmJSONObject.toJSONString(sumDatas));
                }
                if (vm != null) {
                    ViewModel viewModel = (ViewModel) vm;
                    LinkedHashSet<Entity> entities = viewModel.getEntities();
                    List<String[]> columnNameList = new ArrayList<String[]>();
                    List<String[]> fieldNameList = new ArrayList<String[]>();
                    List<ExcelField[]> excelFields = new ArrayList<ExcelField[]>();
                    LinkedHashMap<String, List<Object>> dataMap = new LinkedHashMap<String, List<Object>>();
                    LinkedHashMap<String, List<Object>> sumDataMap = new LinkedHashMap<String, List<Object>>();
                    LinkedHashMap<String, List<Map<String, Object>>> headersMap = new LinkedHashMap<String, List<Map<String, Object>>>();
                    if (entities != null && !entities.isEmpty()) {
                        for (Entity entity : entities) {
                            if (!BooleanUtils.b(entity.getIsTplExport(), true)) continue;
                            String sheetName = null;
                            if (outputFlag) {
                                String entityName = entity.getEntityName();
                                String entityNameResid = entity.getEntityNameResid();
                                String enableI18n = AppContext.getEnvConfig("mdd.i18n.enable");
                                if (StringUtils.isNotBlank(enableI18n) && Boolean.parseBoolean(enableI18n)) {
                                    Map<String, MultiLangText> multiLangTextMap = MessageUtils.getOriginalMessages(entityNameResid);
                                    LanguageVO currentLangVO = MultiLangContext.getInstance().getCurrentLangVO();
                                    MultiLangText multiLangText = multiLangTextMap.get(entityNameResid);
                                    if (null != multiLangText) {
                                        String text = multiLangText.getText(currentLangVO.getLangCode());
                                        if (StringUtils.isNotBlank(text)) {
                                            Matcher matcher = SHEET_NAME_PATTERN.matcher(text);
                                            entityName = matcher.replaceAll("");
                                        }
                                    }
                                }
                                sheetName = entityName + SHEET_SUFFIX_NAME;
                            } else {
                                if (null != entity.getDataSourceName()) {
                                    sheetName = entity.getDataSourceName().substring(entity.getDataSourceName().lastIndexOf(".") + 1) + SHEET_SUFFIX_NAME;
                                } else {
                                    sheetName = entity.getCode() + SHEET_SUFFIX_NAME;
                                }
                            }
                            if (!"Bill".equalsIgnoreCase(entity.getType())) {
                                continue;
                            }
                            List<Field> fields = entity.getFields();
                            if (fields != null) {
                                List<String> cloumnNames = new ArrayList<String>();
                                List<ExcelField> fieldsArray = new ArrayList<ExcelField>();
                                List<String> fieldNames = new ArrayList<String>();
                                List<Map<String, Object>> headerMap = new ArrayList<Map<String, Object>>();
                                for (Field field : fields) {
                                    cloumnNames.add(field.getCaption());
                                    fieldNames.add(field.getItemName());
                                    ExcelField excelField = new ExcelField();
                                    excelField.setFieldName(field.getItemName());
                                    excelField.setEnumString(field.getEnumString());
                                    excelField.setIsEnum(field.getEnum());
                                    excelField.setIsNull(field.getIsNull());
                                    if ("InputNumber".equalsIgnoreCase(field.getControlType()) || "money".equalsIgnoreCase(field.getControlType())) {
                                        excelField.setCellType(CellType.NUMERIC);
                                    } else {
                                        excelField.setCellType(CellType.STRING);
                                    }
                                    fieldsArray.add(excelField);
                                    headerMap.add(transFieldToMap(field));
                                }
                                headersMap.put(sheetName, headerMap);
                                columnNameList.add(cloumnNames.toArray(new String[columnNameList.size()]));
                                fieldNameList.add(fieldNames.toArray(new String[fieldNames.size()]));
                                excelFields.add(fieldsArray.toArray(new ExcelField[fieldsArray.size()]));
                                if (bill.getId() != null) {
                                    if (entity.getIsMain()) {
                                        datas = new ArrayList<>(1);
                                        datas.add(data);
                                    } else {
                                        if (null != data.get(entity.getChildrenField())) {
                                            datas = (List<Object>) data.get(entity.getChildrenField());
                                        } else {  //孙表
                                            if (null != datas && datas.size() > 0) {
                                                List<Object> grandSonDatas = new ArrayList<>();
                                                for (Object sonData : datas) {
                                                    List<Object> grandSonDataList = (List<Object>) ((Map) sonData).get(entity.getChildrenField());
                                                    if (null != grandSonDataList && grandSonDataList.size() > 0) {
                                                        grandSonDatas.addAll(grandSonDataList);
                                                    }
                                                }
                                                datas = grandSonDatas;
                                            }
                                        }
                                    }
                                } else {
                                    sumDataMap.put(sheetName, sumDatas);
                                    //这里的数据须为最外层主表数据，如果传了子表，可能导致去叔叔表里找子表导致找不到的问题
                                    // datas = getChildrenData( ((Pager) result.getData()).getRecordList(),entity);
                                }
                                Long start1 = System.currentTimeMillis();
                                prepareDateFormatBeforeExport();
                                //特殊处理
                                processData(data, datas, fields, false);
                                if (datas != null && datas.size() > 0) {
                                    //特殊处理格式
                                    processExportData(null, sumDatas, fields, true);
                                }
                                log.info("processData time:"
                                        + (System.currentTimeMillis() - start1));
                                dataMap.put(sheetName, datas);

                            }
                        }
                        exportData = new ExcelExportData(dataMap, null,
                                columnNameList, fieldNameList);
                        exportData.setFields(excelFields);
                        exportData.setHeadersMap(headersMap);
                        exportData.setSumMap(sumDataMap);
                        exportData.setFileName(viewModel.getBillName());
                        exportData.setIsExportTemp(false);
                    }
                }
                log.info("exportData time:"
                        + (System.currentTimeMillis() - start));
                return exportData;
            }
        } catch (Exception e) {
            log.info("[billbiz] export导出excel失败{}", e.getMessage(),e);
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101278"),e.getMessage());
        }

    }

    @Override
    public Map<String, String> getResonCode(ExcelExportData exportData, String bankSeqNoColume, String dlFlagColum) throws Exception {

        Map resonCodeMap = new HashMap<String, String>();
        List<String> bankSqeNos = batchBankSeqNo(exportData, bankSeqNoColume, dlFlagColum);
        Map bankdetailnoToBankseqnoMap = new HashMap<String, String>();
        //判断空bankSqeNos
        if (CollectionUtils.isEmpty(bankSqeNos)) {
            return resonCodeMap;
        } else {
            Map<String, String> projectIdNameMap = new HashMap<>();
            Map<String, String> transeqNoprojectIdMap = new HashMap<>();
            //通过银行交易流水号去查交易明细，取出交易流水号
            List<Map<String, Object>> bankDealdetailByBankSeqNos = cmCommonService.getBankDealdetailByBankSeqNos(bankSqeNos);
            if (!CollectionUtils.isEmpty(bankDealdetailByBankSeqNos)) {
                //批量交易流水号
                List<String> bankDealdetails = new ArrayList<>();
                for (int i = 0; i < bankDealdetailByBankSeqNos.size(); i++) {
                    Map<String, Object> bankDealdetailMap = bankDealdetailByBankSeqNos.get(i);
                    Object bankseqno = bankDealdetailMap.get("bankseqno");
                    Object bankdetailno = bankDealdetailMap.get("bankdetailno");
                    if (bankdetailno != null) {
                        bankDealdetails.add(bankdetailno.toString());
                        bankdetailnoToBankseqnoMap.put(bankdetailno.toString(), bankseqno.toString());
                    }
                }
                if (!CollectionUtils.isEmpty(bankDealdetails)) {
                    //通过批量交易流水号去查付款工作台
                    List<Map<String, Object>> payBillByDealdetails = cmCommonService.getPayBillByBankDealdetails(bankDealdetails);
                    if (!CollectionUtils.isEmpty(payBillByDealdetails)) {
                        //批量获取项目id
                        List<String> projectIds = new ArrayList<>();
                        for (int i = 0; i < payBillByDealdetails.size(); i++) {
                            Map<String, Object> payBillMap = payBillByDealdetails.get(i);
                            //参照项目id
                            Object projectId = payBillMap.get("project");
                            String transeqno = (String) payBillMap.get("transeqno");
                            if (projectId != null) {
                                if (!StringUtils.isEmpty((String) projectId)) {
                                    projectIds.add((String) projectId);
                                    transeqNoprojectIdMap.put(transeqno, (String) projectId);
                                }
                            }
                        }
                        if (projectIds.size() > 0) {
                            projectIdNameMap = getprojectIdNameMap(projectIds);
                        }

                    }

                }
            }

            //设置线程快照数据
            if (projectIdNameMap.size() < 1) {
                return resonCodeMap;
            } else {
                Iterator iter = transeqNoprojectIdMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    String transeqNoEntry = (String) entry.getKey();
                    String projectIdEntry = (String) entry.getValue();
                    String projectName = projectIdNameMap.get(projectIdEntry);
                    if (!MapUtils.isEmpty(bankdetailnoToBankseqnoMap)) {
                        if (bankdetailnoToBankseqnoMap.containsKey(transeqNoEntry)) {
                            Object tranSeqNoObject = bankdetailnoToBankseqnoMap.get(transeqNoEntry);
                            resonCodeMap.put(tranSeqNoObject.toString(), projectName);

                        }

                    }

                }
            }
        }
        return resonCodeMap;
    }

    //批量获取项目参照信息
    public Map<String, String> getprojectIdNameMap(List<String> projectIds) throws Exception {
        Map<String, String> projectIdNameMap = new HashMap<>();
        //批量获取项目参照
        List<Map<String, Object>> projectVOs = cmCommonService.getProjectVOs(projectIds);
        if (!CollectionUtils.isEmpty(projectVOs)) {
            for (int y = 0; y < projectVOs.size(); y++) {
                Map<String, Object> projectMap = projectVOs.get(y);
                String projectVOId = (String) projectMap.get("id");
                Object projectVOObject = projectMap.get("name");
                if (projectVOObject != null) {
                    if (!StringUtils.isEmpty(projectVOObject.toString())) {
                        projectIdNameMap.put(projectVOId, projectVOObject.toString());
                    }
                }

            }


        }
        return projectIdNameMap;
    }



    public List<String> batchBankSeqNo(ExcelExportData exportData, String bankSeqNoColume, String dlFlagColum) throws Exception {
        List<String> bankSqeNos = new ArrayList<>();
        LinkedHashMap<String, List<Object>> dataMap = exportData.getDataMap();
        Iterator iter = dataMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            List<Object> recordList = (List<Object>) entry.getValue();
            for (int i = 0; i < recordList.size(); i++) {
                Map<String, Object> record = (Map<String, Object>) recordList.get(i);
                Object bankSeqNoObject = record.get(bankSeqNoColume);
                String bankSeqNo = "";
                //借贷标志
                String dlFlag = "";
                if (bankSeqNoObject != null) {
                    if (bankSeqNoObject instanceof CellData) {
                        if (((CellData) bankSeqNoObject).getValue() != null) {
                            bankSeqNo = ((CellData) bankSeqNoObject).getValue().toString();
                        }
                    } else {
                        bankSeqNo = bankSeqNoObject.toString();
                    }

                }

                Object dlFlagObject = record.get(dlFlagColum);
                if (dlFlagObject != null) {
                    if (dlFlagObject instanceof CellData) {
                        if (((CellData) dlFlagObject).getValue() != null) {
                            dlFlag = ((CellData) dlFlagObject).getValue().toString();
                        }
                    } else {
                        dlFlag = dlFlagObject.toString();
                    }

                }
                //判断是否是付款数据
                if (!StringUtils.isEmpty(dlFlag) && ((Short)Direction.Debit.getValue()).toString().equals(dlFlag)) {
                    if (!StringUtils.isEmpty(bankSeqNo)) {
                        bankSqeNos.add(bankSeqNo);
                    }

                }
            }
        }
        return bankSqeNos;
    }


    /**
     * 从租户的配置中拉取数字\日期\时间等格式参数备用,后续环节多有依赖
     * 不相信本地缓存,直接调接口取
     */
    private static void prepareDateFormatBeforeExport() {
//        try {
//            FetchLoginUserHandler<Map<String, Object>> fetchLoginUserHandler = FetchLoginFactory.getHandler(ConstanceLogin.loginTypeEnums);
//            Map<String, Object> yhtmap = fetchLoginUserHandler.fetchLoginUser(InvocationInfoProxy.getYhtAccessToken());
//            if (yhtmap.get("tenant") != null && yhtmap.get("tenant") instanceof Map) {
//                Map tenantMap = (Map) yhtmap.get("tenant");
//                if (tenantMap.get(DATA_FORMAT) == null) {
//                    throw new IllegalArgumentException(DATA_FORMAT + " is null!");
//                } else {
//                    CtmJSONObject dataformat = CtmJSONObject.parseObject(tenantMap.get(DATA_FORMAT).toString());
//                    AppContext.getCurrentUser().put(DATA_FORMAT, dataformat);
//                }
//            } else {
//                throw new IllegalArgumentException("tenant is " + yhtmap.get("tenant"));
//            }
//        } catch (Throwable e) {
//            log.error("==>error happend while fetching yhttenant configuration of dataformat, date format might be wrong!!", e);
//        }
////        }

    }


    @SuppressWarnings("unchecked")
    private static Map<String, Object> transFieldToMap(Object field)
            throws Exception {
        // TODO 需要改成ctm-base-core方式
        Gson gson = new Gson();
        String json = gson.toJson(field);
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> map = gson.fromJson(json, type);
        Object obj = map.get("parent1");
        if (obj != null && obj instanceof Field) {
            map.put("parent", transFieldToMap(obj));
        }
        return map;

    }

    public static void processData(Map<String, Object> mainData, List<?> dataList, List<Field> fields, Boolean isSum) throws Exception {
        processData(mainData, dataList, fields, isSum, true, false);
    }
    public static void processExportData(Map<String, Object> mainData, List<?> dataList, List<Field> fields, Boolean isSum) throws Exception {
        processData(mainData, dataList, fields, isSum, true, true);
    }

    /**
     * 数据格式化
     * mainData:主表数据
     *
     * @param fields
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static void processData(Map<String, Object> mainData, List<?> dataList, List<Field> fields, Boolean isSum, Boolean hasSplitter, Boolean isExport)
            throws Exception {
        if (null == dataList) return;
        Map<String, Field> fieldMap = new HashMap<>(fields.size());
        for (Field field : fields) {
            fieldMap.put(field.getItemName(), field);
        }
        for (Object obj : dataList) {
            if (obj instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) obj;
                for (String fieldCode : map.keySet()) {
                    Object value = map.get(fieldCode);
                    if (value != null) {
                        Field field = fieldMap.get(fieldCode);
                        if (null != field) {
                            String cControlType = field.getControlType();
                            if (value != null && !StringUtils.isEmpty(value.toString())) {
                                try {
                                    if ("DatePicker".equalsIgnoreCase(cControlType) || "DateTimePicker".equalsIgnoreCase(cControlType)) {
                                        String formatStr = field.getFormatData();
                                        CtmJSONObject dataformat = AppContext.getCurrentUser().get("dataformat") == null ? new CtmJSONObject() : AppContext.getCurrentUser().get("dataformat");
                                        boolean isLower = false;
                                        if (StringUtils.isEmpty(formatStr)) {
                                            formatStr = "DatePicker".equalsIgnoreCase(cControlType) ? dataformat.getString("dateFormat") : dataformat.getString("dateTimeFormat");
                                            if (StringUtils.isEmpty(formatStr)) {
                                                formatStr = "DatePicker".equalsIgnoreCase(cControlType) ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss";
                                            }
                                            if (formatStr.endsWith("tt")) {
                                                formatStr = formatStr.replace("HH", "hh");
                                                formatStr = formatStr.replace("tt", "a");
                                                isLower = true;
                                            }

                                            if (formatStr.endsWith("TT")) {
                                                formatStr = formatStr.replace("HH", "hh");
                                                formatStr = formatStr.replace("TT", "a");
                                            }
                                        }
                                        formatStr = formatStr.replace("DD", "dd");
                                        formatStr = formatStr.replace("YYYY", "yyyy");
                                        SimpleDateFormat sdf = new SimpleDateFormat(formatStr, Locale.ENGLISH);
                                        String formatedvalue = sdf.format(value);
                                        Object v = isLower ? formatedvalue.toLowerCase() : formatedvalue;
                                        if (isExport) {
                                            CellData cellData = new CellData(v, formatStr);
                                            map.put(fieldCode, cellData);
                                        } else {
                                            map.put(fieldCode, v);
                                        }
                                    } else {
                                        if ("InputNumber".equalsIgnoreCase(cControlType)) {
                                            Integer numpoint = field.getNumPoint();
                                            if (numpoint != null && numpoint >= 0) {
                                                value = Toolkit.formatDecimal(value, numpoint);
                                            }
                                        }

                                        String formatStr = field.getFormatData();
                                        if ("money".equalsIgnoreCase(cControlType)) {
                                            formatStr = "{\"decimal\":\"<%option.amountofdecimal%>\"}";
                                        } else if ("price".equalsIgnoreCase(cControlType)) {
                                            formatStr = "{\"decimal\":\"<%option.monovalentdecimal%>\"}";
                                        }
                                        if (GsonHelper.isGoodJson(formatStr)) {
                                            Map<String, Object> formatMap = CtmJSONObject.parseObject(formatStr, Map.class);
                                            Set<String> formatKeys = formatMap.keySet();
                                            Boolean splitter = BooleanUtils.b(formatMap.get("splitter"), false);//是否千分位
                                            if (hasSplitter && ("money".equalsIgnoreCase(cControlType) || splitter)) {
                                                splitter = true;
                                            }
                                            for (String formatKey : formatKeys) {
                                                Object formatValueObj = formatMap.get(formatKey);
                                                if (formatValueObj != null && !StringUtils.isEmpty(formatValueObj.toString())) {
                                                    if ("related".equalsIgnoreCase(formatKey)) {
                                                        processRelate(value, formatValueObj, mainData, map, fieldCode, splitter, isExport);
                                                        break;
                                                    }
                                                    if ("after".equalsIgnoreCase(formatKey)) {
                                                        if (formatValueObj != null) {
                                                            Object v = value.toString() + BillUtils.getPredicateValue(formatValueObj.toString());
                                                            if (isExport) {
                                                                CellData cellData = new CellData(v);
                                                                map.put(fieldCode, cellData);
                                                            } else {
                                                                map.put(fieldCode, v);
                                                            }
                                                        }
                                                        break;
                                                    }
                                                    if ("decimal".equalsIgnoreCase(formatKey)) {
                                                        if (null != value && !Toolkit.isEmpty(value.toString())) {
                                                            Integer numpoint = Integer.valueOf(BillUtils.getPredicateValue(formatValueObj.toString()));
                                                            if (numpoint != null && numpoint >= 0) {
                                                                String ret = Toolkit.numberFormat(value, numpoint, splitter);
                                                                if (null != ret) {
                                                                    if (isExport) {
                                                                        CellData cellData = new CellData(ret, numpoint);
                                                                        cellData.setIsQFW(splitter);
                                                                        map.put(fieldCode, cellData);
                                                                    } else {
                                                                        map.put(fieldCode, ret);
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        break;
                                                    }
                                                }
                                            }
                                        } else {
                                            Integer numpoint = null;
                                            if ("InputNumber".equalsIgnoreCase(cControlType) && field.getNumPoint() != null) {
                                                numpoint = field.getNumPoint();
                                                if (numpoint != null && numpoint >= 0) {
                                                    value = Toolkit.formatDecimal(value, numpoint);
                                                } else {
                                                    value = Toolkit.getFormatDecimalValue(value);
                                                }
                                            }
                                            if (isExport) {
                                                CellData cellData = new CellData(value, numpoint);
                                                map.put(fieldCode, cellData);
                                            } else {
                                                map.put(fieldCode, value);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    if (isExport) {
                                        map.put(fieldCode, new CellData(value));
                                    } else {
                                        map.put(fieldCode, value);
                                    }
                                }
                            }

                            try {
                                if (field.getEnum() != null && field.getEnum() && !StringUtils.isEmpty(field.getEnumString())) {
                                    Map<String, Object> enumMap = CtmJSONObject.parseObject(field.getEnumString(), Map.class);
                                    if (!(isExport && null != field.getSelfDefine() && field.getSelfDefine() && null != field.getRefType() && "u8c-userdefine.pb_userdefine".equalsIgnoreCase(field.getRefType()))) {
                                        //特殊处理枚举值
                                        value = getEnumValue(value, enumMap);
                                    }
                                    if (isExport) {
                                        map.put(fieldCode, new CellData(value));
                                    } else {
                                        map.put(fieldCode, value);
                                    }
                                }
                            } catch (Exception e) {
                                if (isExport) {
                                    map.put(fieldCode, new CellData(value));
                                } else {
                                    map.put(fieldCode, value);
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    private static final String PARENTDATA = "parentdata";

    private static void processRelate(Object value, Object formatValueObj, Map<String, Object> mainData, Map<String, Object> map, String fieldCode, Boolean splitter, Boolean isExport) {
        if (value != null && !Toolkit.isEmpty(value.toString())) {
            if (formatValueObj != null && !StringUtils.isEmpty(formatValueObj.toString())) {
                String formatValue = formatValueObj.toString();
                Integer numpoint = null;
                String[] arr = formatValue.split("\\.");
                if (arr.length > 1 && mainData != null) {
                    if (PARENTDATA.equalsIgnoreCase(arr[0]) && arr[1] != null && mainData.get(arr[1]) != null) {
                        numpoint = Integer.valueOf(mainData.get(arr[1]).toString());
                    }
                } else {
                    if (formatValue != null && map.get(formatValue) != null) {
                        Object format = map.get(formatValue);
                        if (format instanceof CellData) {
                            CellData cellData = (CellData) format;
                            if (cellData.getValue() == null) {
                                numpoint = null;
                            } else {
                                numpoint = Integer.valueOf(cellData.getValue().toString());
                            }
                        } else {
                            numpoint = Integer.valueOf(format.toString());
                        }
                    }
                }

                if (numpoint != null && numpoint >= 0) {
                    value = Toolkit.numberFormat(value, numpoint, splitter);
                }
                if (isExport) {
                    CellData cellData = new CellData(value, numpoint);
                    cellData.setIsQFW(splitter);
                    map.put(fieldCode, cellData);
                } else {
                    map.put(fieldCode, value);
                }
            }
        }
    }


    /**
     * 特殊处理枚举值
     *
     * @param value
     * @return
     */
    private static Object getEnumValue(Object value, Map<String, Object> enumMap) {
        if (value != null && enumMap != null) {
            String valueStr = value.toString();
            if (!StringUtils.isEmpty(valueStr)) {
                String[] valueArray = valueStr.split(",");
                String valueStrs = "";
                for (int i = 0; i < valueArray.length; i++) {
                    if (i < valueArray.length - 1) {
                        valueStrs = valueStrs
                                + (value.toString() != null ? value.toString() : valueStrs) + "/";
                    } else {
                        valueStrs = valueStrs
                                + (value != null ? value.toString(): valueStrs);
                    }
                }
                return valueStrs;
            } else {
                return valueStr;
            }

        } else {
            return value;
        }
    }

}
