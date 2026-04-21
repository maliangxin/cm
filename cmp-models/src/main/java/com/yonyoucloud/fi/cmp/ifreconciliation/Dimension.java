/**
 * Copyright (c) 2019 ucsmy.com, All rights reserved.
 */
package com.yonyoucloud.fi.cmp.ifreconciliation;

import java.util.List;

/**
 * @Description:
 * @Author: wsl
 * @Created Date: 2019年12月31日
 * @Version:
 */
public class Dimension {
    private String dimensiondoc;
    private List<String> dimensionvalue;

    public String getDimensiondoc() {
        return dimensiondoc;
    }

    public void setDimensiondoc(String dimensiondoc) {
        this.dimensiondoc = dimensiondoc;
    }

    public List<String> getDimensionvalue() {
        return dimensionvalue;
    }

    public void setDimensionvalue(List<String> dimensionvalue) {
        this.dimensionvalue = dimensionvalue;
    }
}
