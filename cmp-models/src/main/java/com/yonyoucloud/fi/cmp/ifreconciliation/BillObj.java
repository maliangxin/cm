/**
 * Copyright (c) 2020 ucsmy.com, All rights reserved.
 */
package com.yonyoucloud.fi.cmp.ifreconciliation;

import java.util.List;

/**
 * @Description:
 * @Author: wsl
 * @Created Date: 2020年01月03日
 * @Version:
 */
public class BillObj {

    private String voucherno;
    private String billtype;
    private List<String> billids;

    public String getVoucherno() {
        return voucherno;
    }

    public void setVoucherno(String voucherno) {
        this.voucherno = voucherno;
    }

    public String getBilltype() {
        return billtype;
    }

    public void setBilltype(String billtype) {
        this.billtype = billtype;
    }

    public List<String> getBillids() {
        return billids;
    }

    public void setBillids(List<String> billids) {
        this.billids = billids;
    }
}
