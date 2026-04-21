package com.yonyoucloud.fi.cmp.bankaccountsetting.bo;

import lombok.Data;

import java.util.Objects;

/**
 * @Author guoyangy
 * @Date 2023/11/23 17:17
 * @Description todo
 * @Version 1.0
 */
@Data
public class BankAccountSettingBO {

    private String account;
    private String status ;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankAccountSettingBO that = (BankAccountSettingBO) o;
        return Objects.equals(account, that.account) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(account, status);
    }
}
