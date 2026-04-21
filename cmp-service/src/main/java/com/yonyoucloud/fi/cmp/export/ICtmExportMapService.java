package com.yonyoucloud.fi.cmp.export;

import java.util.List;
/*
 *@author lixuejun
 *@create 2020-08-25-19:20
 */
public interface ICtmExportMapService {

    List<CmpExportMap> queryCtmExportMapList(String billnum, int type, boolean isOpen) throws Exception;
}
