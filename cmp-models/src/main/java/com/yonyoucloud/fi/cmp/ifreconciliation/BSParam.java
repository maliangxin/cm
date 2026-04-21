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
public class BSParam {

    private String mattertype;
    private String transactiontype;
    private String mattertypecode;
    private List<Dimension> dimlist;

    public String getMattertype() {
        return mattertype;
    }

    public void setMattertype(String mattertype) {
        this.mattertype = mattertype;
    }

    public String getTransactiontype() {
        return transactiontype;
    }

    public void setTransactiontype(String transactiontype) {
        this.transactiontype = transactiontype;
    }

    public List<Dimension> getDimlist() {
        return dimlist;
    }

    public void setDimlist(List<Dimension> dimlist) {
        this.dimlist = dimlist;
    }

    public String getMattertypecode() {
        return mattertypecode;
    }

    public void setMattertypecode(String mattertypecode) {
        this.mattertypecode = mattertypecode;
    }
}
