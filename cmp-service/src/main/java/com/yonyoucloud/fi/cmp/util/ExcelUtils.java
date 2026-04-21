package com.yonyoucloud.fi.cmp.util;

import com.yonyou.ucf.mdd.ext.poi.model.CellData;
import com.yonyou.ucf.mdd.ext.poi.model.ExcelExportData;
import com.yonyou.ucf.mdd.ext.poi.model.ExcelField;
import com.yonyou.ucf.mdd.ext.util.Toolkit;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.export.CmpExportMap;
import com.yonyoucloud.fi.cmp.export.conditionhandler.ConditionHandler;
import org.apache.commons.lang3.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
@Data
@Slf4j
public class ExcelUtils {
    private static final Log logger = LogFactory.getLog(ExcelUtils.class);
    private String expression = "[/\\\\?*\\[\\]]+";
    private Pattern pattern = Pattern.compile(expression);
    private Map<String, String> sheetNameMap = new HashMap<>();

    private static final String CONTENT = "content";
    private static final String SUM = "sum";
    private static final Integer MAXBLANKROW = 100;
    public Workbook workbook;
    public CellStyle titleStyle;
    public Font titleFont;
    public CellStyle headStyle;
    public Font headFont;
    public Font sumFont;
    public Font bodyFont;//content字体
    public CellStyle bodyStyle;//内容样式

    public ExcelUtils() {
    }

    public void initWorkBook() {
        this.workbook = new HSSFWorkbook();
    }

    public void initImportBook(FileInputStream fis) throws IOException {
        this.workbook = new HSSFWorkbook(fis);
    }

    private void init() {
        this.initWorkBook();
        this.titleStyle = this.workbook.createCellStyle();
        this.titleFont = this.workbook.createFont();
        this.headStyle = this.workbook.createCellStyle();
        this.headFont = this.workbook.createFont();
        this.sumFont = this.workbook.createFont();
        this.initTitleFont();
        this.initTitleStyle();
        this.initHeadFont(this.headFont);
        this.initHeadStyle(this.headStyle, this.headFont);
        this.initSumFont(this.sumFont);
        bodyStyle = workbook.createCellStyle();
        bodyFont = workbook.createFont();
        initBodyFont(bodyFont);
        initBodyStyle(bodyStyle,bodyFont);
    }

//    public List<Map<String, Object>> getImportDatas(FileInputStream fis) throws Exception {
//        try {
//            this.initImportBook(fis);
//            int sheetNum = this.workbook.getNumberOfSheets();
//           // Map<String, Object> dataMaps = new HashMap();
//
//            //for(int i = 0; i < sheetNum; ++i) {
//                Sheet sheet = this.workbook.getSheetAt(0);
//                String sheetName = sheet.getSheetName();
//                if (sheetName.contains("(")) {
//                    sheetName = sheetName.substring(0, sheetName.indexOf("(")).trim();
//                }
//
//                int rowNum = sheet.getLastRowNum();
//                List<Map<String, Object>> datas = new ArrayList();
//
//                for(int row = 0; row <= rowNum; ++row) {
//                    Map<String,Object> map = new HashMap<String,Object>();
//                    for (int j =0;sheet.getRow(row)!=null&&j<sheet.getRow(row).getLastCellNum();++j){
//                        Cell cell = sheet.getRow(row).getCell(j);
//                        if (cell==null){
//                            map.put("key_"+j,null);
//                        }else {
//                            if (cell.getCellTypeEnum() == CellType.NUMERIC && HSSFDateUtil.isCellDateFormatted(cell)) {
//                                map.put("key_"+j, cell.getDateCellValue());
//                            } else if (cell.getCellTypeEnum() == CellType.FORMULA) {
//                                map.put("key_"+j, cell.getNumericCellValue());
//                            } else if (cell.getCellTypeEnum() == CellType.NUMERIC){
//                                NumberFormat nf = NumberFormat.getInstance();
//                                double value = cell.getNumericCellValue();
//                                String s = nf.format(value);
//                                if (s.indexOf(",") >= 0) {
//                                    s = s.replace(",", "");
//                                }
//                                map.put("key_" + j, s);
//                            }else {
//                                map.put("key_"+j, cell.getStringCellValue());
//                            }
//                        }
//                    }
//                    datas.add(map);
//                }
//                //dataMaps.put(sheetName,datas);
//            //}
//            //return dataMaps;
//            return datas;
//        } catch (Exception var22) {
//            log.info("[ExcelUtils] getImportDatas 异常" + var22.getMessage());
//            throw var22;
//        } finally {
//            if (fis != null) {
//                fis.close();
//            }
//            if (this.workbook != null) {
//                this.workbook.close();
//            }
//        }
//    }

    private int createTableTitleRow(ExcelExportData setInfo, Sheet Sheet, int sheetNum, int rowIndex) {
        if (setInfo.getTitles() != null && setInfo.getTitles().length > 0) {
            CellRangeAddress titleRange = new CellRangeAddress(1, 1, 0, ((String[])setInfo.getFieldNames().get(sheetNum)).length);
            Sheet.addMergedRegion(titleRange);
            Row titleRow = Sheet.createRow(rowIndex);
            titleRow.setHeightInPoints(40.0F);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellStyle(this.titleStyle);
            titleCell.setCellValue(setInfo.getTitles()[sheetNum]);
            return 1;
        } else {
            return 0;
        }
    }

    private int createTableFieldRow(ExcelExportData setInfo, Sheet Sheet, int sheetNum) {
        if (setInfo.getFieldNames() != null && !setInfo.getFieldNames().isEmpty() && setInfo.getFieldNames().get(sheetNum) != null && ((String[])setInfo.getFieldNames().get(sheetNum)).length > 0) {
            Row fieldRow = Sheet.createRow(0);
            fieldRow.setHeightInPoints(0.0F);
            String[] fieldNames = (String[])setInfo.getFieldNames().get(sheetNum);

            for(int i = 0; i < fieldNames.length; ++i) {
                Cell fieldCell = fieldRow.createCell(i);
                fieldCell.setCellValue(fieldNames[i]);
            }

            return 1;
        } else {
            return 0;
        }
    }

    /** @deprecated */
  @Deprecated
    public boolean isMergedRegion(Sheet sheet, int row, int col) {
        int sheetMergeCount = sheet.getNumMergedRegions();

        for(int i = 0; i < sheetMergeCount; ++i) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            int firstColumn = range.getFirstColumn();
            int lastColumn = range.getLastColumn();
            int firstRow = range.getFirstRow();
            int lastRow = range.getLastRow();
            if (row >= firstRow && row <= lastRow && col >= firstColumn && col <= lastColumn) {
                return true;
            }
        }

        return false;
    }

    private void setColumnDefaultStyle(Sheet sheet, int index, CellType cellType) {
        DataFormat format = this.workbook.createDataFormat();
        CellStyle cellStyle = this.workbook.createCellStyle();
        if (cellType.name().equals(CellType.NUMERIC.name())) {
            cellStyle.setDataFormat(format.getFormat("0.00"));
        } else {
            cellStyle.setDataFormat(format.getFormat("@"));
        }

        sheet.setDefaultColumnStyle(index, cellStyle);
    }

    private void createTableRow(ExcelExportData setInfo, Sheet sheet, int sheetNum, String type) {
        LinkedHashMap<String, List<Object>> dataMap = null;
        if ("content".equals(type)) {
            dataMap = setInfo.getDataMap();
        } else if ("sum".equals(type)) {
            dataMap = setInfo.getSumMap();
        }

        if (dataMap != null && !dataMap.isEmpty() && dataMap.get(sheet.getSheetName()) != null && ((List)dataMap.get(sheet.getSheetName())).size() > 0) {
            int rowIndex = sheet.getLastRowNum() + 1;
            List<Object> datas = (List)dataMap.get(sheet.getSheetName());
            ExcelField[] excelFields = null;
            if (setInfo.getFields() != null) {
                excelFields = (ExcelField[])setInfo.getFields().get(sheetNum);
            }

            for(Iterator var9 = datas.iterator(); var9.hasNext(); ++rowIndex) {
                Object data = var9.next();
                Row row = sheet.createRow(rowIndex);
                String[] fieldNames = (String[])setInfo.getFieldNames().get(sheetNum);

                for(int i = 0; i < fieldNames.length; ++i) {
                    Cell contentCell = row.createCell(i);
                    ExcelField excelField = null;
                    if (excelFields != null) {
                        excelField = excelFields[i];
                    }

                    Object value;
                    if (data instanceof Object[]) {
                        Object[] values = (Object[])((Object[])data);
                        if (i < values.length) {
                            if (i == 0 && "sum".equals(type)) {
                                contentCell.setCellValue(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805DA","合计") /* "合计" */);
                                CellStyle cellStyle = this.workbook.createCellStyle();
                                this.sumStyle(cellStyle, this.sumFont);
                                contentCell.setCellStyle(cellStyle);
                            } else if (values[i] != null) {
                                value = null;
                                CellData cellData;
                                if (values[i] instanceof CellData) {
                                    cellData = (CellData)values[i];
                                } else {
                                    cellData = new CellData(values[i]);
                                }

                                if ("sum".equals(type)) {
                                    this.formatSumCellValue(contentCell, cellData, excelField.getCellType());
                                } else {
                                    this.formatCellValue(contentCell, cellData, excelField.getCellType());
                                }
                            }
                        }
                    } else if (data instanceof Map) {
                        Map<String, Object> valueMap = (Map)data;
                        value = valueMap.get(fieldNames[i]);
                        CellData cellData = null;
                        if (value instanceof CellData) {
                            cellData = (CellData)value;
                        } else {
                            cellData = new CellData(value);
                        }

                        if (value != null && excelField != null) {
                            String enumString = excelField.getEnumString();
                            if (excelField.getIsEnum() != null && excelField.getIsEnum() && enumString != null && !"".equals(enumString.trim()) && value != null) {
                                Map<String, Object> enumMap = (Map) CtmJSONObject.parseObject(enumString, Map.class);
                                Boolean var22 = enumMap.get(cellData.getValue().toString()) == null;
                            }
                        }

                        if (i == 0 && "sum".equals(type)) {
                            contentCell.setCellValue(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805DA","合计") /* "合计" */);
                            CellStyle cellStyle = this.workbook.createCellStyle();
                            this.sumStyle(cellStyle, this.sumFont);
                            contentCell.setCellStyle(cellStyle);
                        } else if ("sum".equals(type)) {
                            this.formatSumCellValue(contentCell, cellData, excelField.getCellType());
                        } else {
                            this.formatCellValue(contentCell, cellData, excelField.getCellType());
                        }
                    }
                }
            }
        }

    }

    private void formatSumCellValue(Cell cell, CellData cellData, CellType cellType) {
        CellStyle cellStyle = this.workbook.createCellStyle();
        if (cellData != null) {
            Object value = Toolkit.getFormatDecimalValue(cellData.getValue());
            if (Toolkit.isNumber(value) && CellType.NUMERIC.name().equals(cellType.name())) {
                Integer numPoint = cellData.getiNumPoint();
                if (numPoint != null) {
                    if (numPoint == 0) {
                        cell.setCellValue((double)Integer.parseInt(value.toString()));
                    } else {
                        DataFormat format = this.workbook.createDataFormat();
                        String pattern = String.join("", Collections.nCopies(numPoint, "0"));
                        BigDecimal bd = new BigDecimal(value.toString().trim());
                        if (bd.compareTo(BigDecimal.ONE) < 0) {
                            pattern = "0." + pattern;
                        } else {
                            pattern = "#." + pattern;
                        }

                        cell.setCellValue(Double.parseDouble(value.toString()));
                        cellStyle.setDataFormat(format.getFormat(pattern));
                    }
                } else {
                    cell.setCellValue(Double.parseDouble(value.toString()));
                }
            } else {
                cell.setCellValue(value == null ? null : value.toString());
            }
        }

        this.sumStyle(cellStyle, this.sumFont);
        cell.setCellStyle(cellStyle);
    }

    private void formatCellValue(Cell cell, CellData cellData, CellType cellType) {
        if (cellData != null) {
            Object value = Toolkit.getFormatDecimalValue(cellData.getValue());
            if (Toolkit.isNumber(value) && CellType.NUMERIC.name().equals(cellType.name())) {
                Integer numPoint = cellData.getiNumPoint();
                if (numPoint != null) {
                    if (numPoint == 0) {
                        cell.setCellValue((double)Integer.parseInt(value.toString()));
                    } else {
                        CellStyle cellStyle = this.workbook.createCellStyle();
                        DataFormat format = this.workbook.createDataFormat();
                        String pattern = String.join("", Collections.nCopies(numPoint, "0"));
                        BigDecimal bd = new BigDecimal(value.toString().trim());
                        if (bd.compareTo(BigDecimal.ONE) < 0) {
                            pattern = "0." + pattern;
                        } else {
                            pattern = "#." + pattern;
                        }

                        cell.setCellValue(Double.parseDouble(value.toString()));
                        cellStyle.setDataFormat(format.getFormat(pattern));
                        cell.setCellStyle(cellStyle);
                    }
                } else {
                    cell.setCellValue(Double.parseDouble(value.toString()));
                }
            } else {
                cell.setCellValue(value == null ? null : value.toString());
            }
        }

    }

    private void createTableContentRow(ExcelExportData setInfo, Sheet Sheet, int sheetNum, int rowIndex) {
        this.createTableRow(setInfo, Sheet, sheetNum, "content");
    }

    private void createTableSumRow(ExcelExportData setInfo, Sheet Sheet, int sheetNum) {
        this.createTableRow(setInfo, Sheet, sheetNum, "sum");
    }

    private void adjustColumnSize(Sheet sheet, String[] fieldNames) {
        if (sheet instanceof SXSSFSheet) {
            SXSSFSheet currSheet = (SXSSFSheet)sheet;
            currSheet.trackAllColumnsForAutoSizing();

            for(int i = 0; i < fieldNames.length; ++i) {
                currSheet.autoSizeColumn(i);
            }
        } else {
            for(int i = 0; i < fieldNames.length; ++i) {
                sheet.autoSizeColumn(i, true);
            }
        }

    }

    private List<Sheet> getSheets(Set<String> sheetNames) {
        List<Sheet> sheets = new ArrayList();
        Iterator var3 = sheetNames.iterator();

        while(var3.hasNext()) {
            String sheetName = (String)var3.next();
            sheets.add(this.workbook.createSheet(sheetName));
        }

        return sheets;
    }

    private void initTitleStyle() {
        this.titleStyle.setAlignment(HorizontalAlignment.CENTER);
        this.titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        this.titleStyle.setFont(this.titleFont);
        this.titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        this.titleStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.index);
        this.setBorder(this.titleStyle);
    }

    private void initTitleFont() {
        this.titleFont.setFontName(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C54041805DC","华文楷体") /* "华文楷体" */);
        this.titleFont.setFontHeightInPoints((short) 15);
        this.titleFont.setBold(true);
        this.titleFont.setCharSet((byte)1);
        this.titleFont.setColor(IndexedColors.BLACK.index);
    }

    private void initHeadStyle(CellStyle cellStyle, Font font) {
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        cellStyle.setFont(font);
        this.setBorder(cellStyle);
    }

    private void setBorder(CellStyle cellStyle) {
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBottomBorderColor(IndexedColors.BLACK.index);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setLeftBorderColor(IndexedColors.BLACK.index);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setRightBorderColor(IndexedColors.BLACK.index);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setTopBorderColor(IndexedColors.BLACK.index);
    }

    private void initHeadFont(Font font) {
        font.setColor(IndexedColors.BLACK.index);
        font.setFontHeightInPoints((short)13);
    }

    private void initSumFont(Font font) {
        font.setColor(IndexedColors.RED.index);
        font.setFontHeightInPoints((short)13);
        font.setBold(true);
    }

    private void sumStyle(CellStyle cellStyle, Font font) {
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.index);
        cellStyle.setFont(font);
        this.setBorder(cellStyle);
    }

    private void createCellComment(Sheet sheet, String value, Cell cell, int width, int height) {
        if (!StringUtils.isEmpty(value)) {
            Drawing<?> patr = sheet.createDrawingPatriarch();
            ClientAnchor anchor = patr.createAnchor(0, 0, 0, 0, cell.getColumnIndex(), cell.getRowIndex(), cell.getColumnIndex() + width, cell.getRowIndex() + height);
            Comment comment = patr.createCellComment(anchor);
            Font font = this.workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.RED.getIndex());
            if (sheet instanceof SXSSFSheet) {
                XSSFRichTextString xssfRichTextString = new XSSFRichTextString(value);
                xssfRichTextString.applyFont(font);
                comment.setString(xssfRichTextString);
            } else {
                HSSFRichTextString hssfRichTextString = new HSSFRichTextString(value);
                hssfRichTextString.applyFont(font);
                comment.setString(hssfRichTextString);
            }

            comment.setVisible(false);
            cell.setCellComment(comment);
        }

    }

//    public void collectRequestInfo(Map<String, String> cellDatasMap) throws Exception {
//        XSSFWorkbook wb = null;
//        XSSFSheet sheet = null;
//        List<String> fieldNames = Arrays.asList("url", "method", "body", "token");
//        List<String> columnNames = Arrays.asList("url", MessageUtils.getMessage("P_YS_SD_UDHWN_0000121214") /* "接口类型" */, "body", "token");
//        FileOutputStream out = null;
//        FileInputStream in = null;
//
//        try {
//            int rowEnd = 0;
//            File dir = new File(AppContext.getEnvConfig("attachmentpath", "") + "requestCollects");
//            if (!dir.exists()) {
//                dir.mkdirs();
//            }
//
//            File file = new File(dir, "requestCollects.xlsx");
//            if (!file.exists()) {
//                file.createNewFile();
//                wb = new XSSFWorkbook();
//                sheet = wb.createSheet();
//            } else {
//                in = new FileInputStream(file);
//                wb = new XSSFWorkbook(in);
//                sheet = wb.getSheetAt(0);
//                rowEnd = sheet.getLastRowNum() + 1;
//            }
//
//            XSSFRow row;
//            if (rowEnd == 0) {
//                row = sheet.createRow(rowEnd);
//                XSSFCellStyle headStyle = wb.createCellStyle();
//                XSSFFont headerFont = wb.createFont();
//                this.initHeadFont(headerFont);
//                this.initHeadStyle(headStyle, headerFont);
//
//                for(int i = 0; i < columnNames.size(); ++i) {
//                    XSSFCell cell = row.createCell(i);
//                    cell.setCellValue((String)columnNames.get(i));
//                    cell.setCellStyle(headStyle);
//                }
//            }
//
//            rowEnd = sheet.getLastRowNum() + 1;
//            row = sheet.createRow(rowEnd);
//
//            for(int i = 0; i < fieldNames.size(); ++i) {
//                XSSFCell cell = row.createCell(i);
//                cell.setCellValue((String)cellDatasMap.get(fieldNames.get(i)));
//            }
//
//            this.adjustColumnSize(sheet, (String[])fieldNames.toArray(new String[fieldNames.size()]));
//            out = new FileOutputStream(file);
//            out.flush();
//            wb.write(out);
//        } catch (Exception var19) {
////            var19.printStackTrace();
//            log.info("[ExcelUtils] requestCollect导出excel失败{}", var19);
//            throw var19;
//        } finally {
//            if (in != null) {
//                in.close();
//            }
//
//            if (out != null) {
//                out.close();
//            }
//
//            if (wb != null) {
//                wb.close();
//            }
//
//        }
//    }

    private static Boolean isQfw(String str) {
        String pattern = "(-)?\\d{1,3}(,\\d{3})*(.\\d+)?";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(str);
        return m.matches();
    }


    /**
     * 将内容输出到流
     *
     * @param setInfo          excel导出的内容
     * @param cmpExportMapList
     * @return
     * @throws Exception
     */
    public ByteArrayOutputStream export2SAPStream(com.yonyou.ucf.mdd.ext.poi.model.ExcelExportData setInfo, List<CmpExportMap> cmpExportMapList)
            throws Exception {
        if (setInfo == null || setInfo.getDataMap() == null) {
            log.info("Excel导出数据Sheet数量为0");
        }
        init();
        LinkedHashMap<String, List<Object>> dataMaps = setInfo.getDataMap();
        Set<String> sheetNames = dataMaps.keySet();
        List<Sheet> sheets = getSAPSheets(sheetNames);
        if (CollectionUtils.isEmpty(sheets)) {// 导出的时候如果sheets=0，打印日志
            log.info("Excel导出数据Sheet数量为0");
        }
        List<CmpExportMap> headColumns = new ArrayList<>();
        if (cmpExportMapList!=null&&cmpExportMapList.size()>0){
            cmpExportMapList.forEach(
                    cmpExportMap -> {
                        //此字段是否开启映射
                        boolean isOpen = cmpExportMap.getIsOpen();
                        if (isOpen) {
                            headColumns.add(cmpExportMap);
                        }
                    });

        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        if (headColumns!=null&&headColumns.size()>0){
            //升级排列
            List<CmpExportMap> collectCmpExportMap = headColumns.stream().sorted(Comparator.comparing(CmpExportMap::getSort)).collect(Collectors.toList());
            int sheetNum = 0;
            for (Sheet sheet : sheets) {
                if (sheet instanceof SXSSFSheet) {
                    ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();
                }
                int row = 0;
                //创建表头
                createTableSAPHeadRow(setInfo, sheet, sheetNum, row, collectCmpExportMap);//创建表头列
                //创建表体
                createTableSAPContentRow(setInfo, sheet, sheetNum, row, collectCmpExportMap);//创建内容区域
                adjustSAPColumnSize(sheet, true,collectCmpExportMap);
                sheetNum++;
            }
            workbook.write(outputStream);
            return outputStream;
        }

           return outputStream;
    }

    /**
     * 创建表头
     *
     * @param setInfo
     * @param sheet
     * @param sheetNum
     * @param rowIndex
     */
    @SuppressWarnings("unchecked")
    private void createTableSAPHeadRow(com.yonyou.ucf.mdd.ext.poi.model.ExcelExportData setInfo, Sheet sheet, int sheetNum, int rowIndex, List<CmpExportMap> collectCmpExportMap) {
        if (setInfo.getColumnNames() != null && !setInfo.getColumnNames().isEmpty() && setInfo.getColumnNames().get(sheetNum) != null
                && setInfo.getColumnNames().get(sheetNum).length > 0) {
            Row row = sheet.createRow(rowIndex);
            row.setHeightInPoints(20);
            //获取列索引
            for (int i = 0; i < collectCmpExportMap.size(); i++) {
                // 列名称
                String headName = collectCmpExportMap.get(i).getTargetName();
                // 创建单元格;
                creCell(row, i, headName, headStyle);
            }


        }
    }


   //lixuejun创建sap行
    @SuppressWarnings("unchecked")
    private void createSAPTableRow(com.yonyou.ucf.mdd.ext.poi.model.ExcelExportData setInfo,
                                   Sheet sheet, int sheetNum,
                                   String type,
                                   int rowIndex,
                                   List<CmpExportMap> cmpExportMapList) throws Exception {
        LinkedHashMap<String, List<Object>> dataMap = new LinkedHashMap<>();
        if (CONTENT.equals(type)) {
            dataMap = setInfo.getDataMap();
        }
        if (MapUtils.isEmpty(dataMap)) {
            return;
        }
        com.yonyou.ucf.mdd.ext.poi.model.ExcelField[] excelFields=null;
        if(setInfo.getFields()!=null)
            excelFields=setInfo.getFields().get(sheetNum);
        List<Object> datas = dataMap.get(sheet.getSheetName());
        if (CollectionUtils.isEmpty(datas) && null != sheetNameMap) {
            datas = dataMap.get(sheetNameMap.get(sheet.getSheetName()));
        }
        if (dataMap != null && !dataMap.isEmpty() && datas != null && datas.size() > 0) {
            for (int y = 0; y < datas.size(); y++) {
                Object data = datas.get(y);
                rowIndex++;
                String[] fieldNames=setInfo.getFieldNames().get(sheetNum);
                Row row = sheet.createRow(rowIndex);
                for(int i=0;i<fieldNames.length;i++){
                    if (data instanceof Object[]) {
                        Object[] values = (Object[]) data;
                        //object 数组转化成keyMap结构
                        Map<String, Object> oldValueWap = new HashMap<>();
                        for (int x = 0; x < values.length; x++) {
                            if (values[x] != null) {
                                String fieldName = fieldNames[x];
                                oldValueWap.put(fieldName,values[x]);
                            }
                        }
                        setBodyCellValue(cmpExportMapList,oldValueWap,y+1,row);
                    } else if (data instanceof Map) {
                        Map<String, Object> valueMap = (Map<String, Object>) data;
                        setBodyCellValue(cmpExportMapList,valueMap,y+1,row);
                    }
                }
            }
        }

    }



    /**
     * 创建单元格
     *
     * @param row
     * @param c
     * @param cellValue
     * @param style
     */

    private static void creCell(Row row, int c, String cellValue, CellStyle style) {
        Cell cell = row.createCell(c);
        cell.setCellValue(cellValue);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void setBodyCellValue(List<CmpExportMap> cmpExportMapList,
                                  Map<String, Object> valueMap,
                                  Integer y,Row row
    ) throws Exception {
        //一次生成所有得处理类对象，减少反射创建对象得个数，以方便重复利用对象实例,提高性能
        Map<String, ConditionHandler> conditionHandlerHashMap = new HashMap<>();
        for (int m = 0; m <cmpExportMapList.size() ; m++) {
            CmpExportMap cmpExportMap = cmpExportMapList.get(m);
            if (!conditionHandlerHashMap.containsKey(cmpExportMap.getEvalHandler())){
                ConditionHandler conditionHandler = (ConditionHandler)CommonFactory.getInstance(cmpExportMap.getEvalHandler());
                conditionHandlerHashMap.put(cmpExportMap.getEvalHandler(), conditionHandler);
            }
            //规则处理
            ConditionHandler conditionHandler = conditionHandlerHashMap.get(cmpExportMap.getEvalHandler());
            String cellValue =(String) conditionHandler.handler(cmpExportMap, valueMap, y);
            //值set进body里
            creCell(row, m, cellValue, bodyStyle);

        }
    }

//=============================
    /**
     * 获取表格表体区域
     *
     * @param setInfo
     * @param Sheet
     * @param sheetNum
     * @throws Exception
     */
    private void createTableSAPContentRow(com.yonyou.ucf.mdd.ext.poi.model.ExcelExportData setInfo,
                                          Sheet Sheet, int sheetNum, int rowIndex,
                                          List<CmpExportMap> cmpExportMapList) throws Exception {
        createSAPTableRow(setInfo, Sheet, sheetNum, CONTENT, rowIndex, cmpExportMapList);
    }


    /**
     * 自动列宽
     *
     * @param sheet
     * @param cmpExportMap
     */
    private void adjustSAPColumnSize(Sheet sheet, List<CmpExportMap> cmpExportMap) {
        if (sheet instanceof SXSSFSheet) {
            SXSSFSheet currSheet = (SXSSFSheet) sheet;
            currSheet.trackAllColumnsForAutoSizing();
            for (int i = 0; i < cmpExportMap.size(); i++) {
                currSheet.autoSizeColumn(i);
            }
        } else {
            for (int i = 0; i < cmpExportMap.size(); i++) {
                sheet.autoSizeColumn(i, true);
            }
        }

    }

    /**
     * 自动列宽
     *
     * @param sheet
     * @param cmpExportMap
     */
    private void adjustSAPColumnSize(Sheet sheet, boolean bAuto,List<CmpExportMap> cmpExportMap) {
        if (bAuto) {
            adjustSAPColumnSize(sheet, cmpExportMap);
        } else {
            for (int i = 0; i < cmpExportMap.size(); i++) {
                sheet.setColumnWidth(i, 256 * 20);
            }
        }
    }
    private List<Sheet> getSAPSheets(Set<String> sheetNames) {
        List<Sheet> sheets = new ArrayList<Sheet>();
        for (String sheetName : sheetNames) {
            sheetName = pattern.matcher(sheetName).replaceAll("|");
            sheets.add(workbook.createSheet(sheetName));
            if (sheetName.length() > 31) {
                if (MapUtils.isEmpty(sheetNameMap)) {
                    sheetNameMap = new HashMap<>();
                }
                sheetNameMap.put(sheetName.substring(0, 31), sheetName);
            }
        }
        return sheets;
    }
    /**
     * @Description: 初始化内容字体
     */
    private void initBodyFont(Font bodyFont) {
        bodyFont.setFontHeightInPoints((short) 13);
        bodyFont.setBold(false);
        bodyFont.setColor(IndexedColors.BLACK.index);
    }

    /**
     * 初始化表头样式
     */
    private void initSAPHeadStyle(CellStyle cellStyle, Font font) {
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);//设置标题的背景色是灰色
        cellStyle.setFont(font);
        setSAPBorder(cellStyle);
    }


    private void setSAPBorder(CellStyle cellStyle) {
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBottomBorderColor(IndexedColors.BLACK.index);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setLeftBorderColor(IndexedColors.BLACK.index);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setRightBorderColor(IndexedColors.BLACK.index);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setTopBorderColor(IndexedColors.BLACK.index);
    }


    /**
     *
     * @param cellStyle
     * @param font
     */
    private void initBodyStyle(CellStyle cellStyle, Font font) {
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setFont(font);
    }

}
