package com.yonyoucloud.fi.cmp.arap;

import java.math.BigDecimal;

public class ConutMoneyInfo {
    BigDecimal balance = null;
    BigDecimal localbalance = null;
    BigDecimal amount = null;
    BigDecimal localamount = null;
    BigDecimal oriSum = null;
    BigDecimal natSum = null;
    BigDecimal bookAmount = null;

    public ConutMoneyInfo() {
    }

    public BigDecimal getBookAmount() {
        return this.bookAmount;
    }

    public void setBookAmount(BigDecimal bookAmount) {
        this.bookAmount = bookAmount;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getLocalamount() {
        return this.localamount;
    }

    public void setLocalamount(BigDecimal localamount) {
        this.localamount = localamount;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getLocalbalance() {
        return this.localbalance;
    }

    public void setLocalbalance(BigDecimal localbalance) {
        this.localbalance = localbalance;
    }

    public BigDecimal getOriSum() {
        return this.oriSum;
    }

    public void setOriSum(BigDecimal oriSum) {
        this.oriSum = oriSum;
    }

    public BigDecimal getNatSum() {
        return this.natSum;
    }

    public void setNatSum(BigDecimal natSum) {
        this.natSum = natSum;
    }

    public String toString() {
        return "ConutMoneyInfo{balance=" + this.balance + ", localbalance=" + this.localbalance + ", amount=" + this.amount + ", localamount=" + this.localamount + ", oriSum=" + this.oriSum + ", natSum=" + this.natSum + ", bookAmount=" + this.bookAmount + '}';
    }
}