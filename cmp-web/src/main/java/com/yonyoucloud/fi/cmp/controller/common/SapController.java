package com.yonyoucloud.fi.cmp.controller.common;


import com.yonyou.ucf.mdd.ext.base.BaseController;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.ucf.mdd.ext.poi.model.ExcelExportData;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.common.service.CmCommonService;
import com.yonyoucloud.fi.cmp.export.ICtmExportMapService;
import com.yonyoucloud.fi.cmp.export.ISAPExportService;
import com.yonyoucloud.fi.cmp.enums.ExportTypeEnum;
import com.yonyoucloud.fi.cmp.enums.IsOpenEnum;
import com.yonyoucloud.fi.cmp.export.CmpExportMap;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.SAPUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/sapexport")
@Slf4j
public class SapController extends BaseController {


    @Autowired
    private ISAPExportService SAPExportService;
    @Autowired
    private ICtmExportMapService ctmExportMapService;
    @Autowired
    private CmCommonService cmCommonService;

@RequestMapping(value = "/siFangbankreconciliation", method = RequestMethod.POST)
    public void siFangSapBankreconciliationExport(@RequestBody BillDataDto bill, HttpServletRequest request, HttpServletResponse response) {
        try {
            ExcelExportData exportData = SAPExportService.sapExport(bill);
            if (exportData == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101481"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C5404180318","数据已经删除") /* "数据已经删除" */);
            }
            String billnum = bill.getBillnum();
            List<CmpExportMap> cmpExportMapList = ctmExportMapService.queryCtmExportMapList(billnum, ExportTypeEnum.SAP_EXPORT.getCode(), IsOpenEnum.OPEN.getCode());
            if (cmpExportMapList != null && cmpExportMapList.size() > 0) {
                //处理上下文
                dealSiFangSapContext(exportData, SAPUtils.BankSeqNo,SAPUtils.DcFlag);
                com.yonyou.ucf.mdd.ext.poi.model.StreamParam streamParam = new com.yonyou.ucf.mdd.ext.poi.model.StreamParam(bill.getBillnum() + DateUtils.getUserDate(DateUtils.YYYYMMDDHHMMSS), exportData, response);
                SAPUtils.downLoadToResponse(streamParam, cmpExportMapList);
            } else {
                log.warn("无配置");
            }
        } catch (Exception e) {
            renderJson(response, ResultMessage.error(e.getMessage()));
            log.error("错误信息:", e);
        }finally {
            SAPUtils.remove();
        }
    }



    //处理四方sap导出的上下文 每个公司单独构建上下文，防止数据过大，内存溢出，尽量缩小放入上下文得数据
    public void dealSiFangSapContext(ExcelExportData exportData, String bankSeqNoColume, String dlFlagColum) throws Exception {
        Map<String, Object> sapexportContext = SAPUtils.getSapexportContext();
        Map<String, String> resonCodeMap = SAPExportService.getResonCode(exportData, bankSeqNoColume, dlFlagColum);
        Map<String, String>  orgs  = cmCommonService.getOrgs();
        if (sapexportContext==null){
            //构建上下文
             sapexportContext = new HashMap<String, Object>();
             if (resonCodeMap==null){
             //构建原因码值
                  resonCodeMap = new HashMap<String, String>();
             }
             if(orgs==null){
                 //构建组织
                 orgs= new HashMap<String, String>();
             }

        }
        sapexportContext.put(SAPUtils.ResonCode,resonCodeMap);
        sapexportContext.put(SAPUtils.OrgsKey,orgs);
        SAPUtils.setSapexportContext(sapexportContext);
    }

}
