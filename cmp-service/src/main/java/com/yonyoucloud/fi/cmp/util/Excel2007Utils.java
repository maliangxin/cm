package com.yonyoucloud.fi.cmp.util;

import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

public class Excel2007Utils extends ExcelUtils {
    private static final Integer CACHEDATA = 1000;

    public Excel2007Utils() {
    }


    @Override
    public void initWorkBook() {
        this.workbook = new SXSSFWorkbook(CACHEDATA);
    }

    @Override
    public void initImportBook(FileInputStream fis) throws IOException {
        this.workbook = new XSSFWorkbook(fis);
    }

    public void dispose() {
        if (this.workbook != null && this.workbook instanceof SXSSFWorkbook) {
            ((SXSSFWorkbook)this.workbook).dispose();
        }

    }
}