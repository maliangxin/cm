package com.yonyoucloud.fi.cmp.util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;

public class Excel2003Utils extends ExcelUtils {
    public Excel2003Utils() {
    }

    @Override
    public void initWorkBook() {
        this.workbook = new HSSFWorkbook();
    }

    @Override
    public void initImportBook(FileInputStream fis) throws IOException {
        this.workbook = new HSSFWorkbook(fis);
    }
}