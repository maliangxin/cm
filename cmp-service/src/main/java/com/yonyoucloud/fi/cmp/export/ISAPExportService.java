package com.yonyoucloud.fi.cmp.export;

import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.poi.model.ExcelExportData;

import java.util.Map;

public interface ISAPExportService {

    ExcelExportData sapExport(BillDataDto bill) throws Exception;

    /**
     * 得到原因码值
     * @param exportData
     * @param bankSeqNoColume
     * @param dlFlagColum
     * @return
     * @throws Exception
     */
    Map<String, String> getResonCode(ExcelExportData exportData, String bankSeqNoColume, String dlFlagColum) throws Exception;




}
