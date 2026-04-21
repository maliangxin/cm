package com.yonyoucloud.fi.cmp.journal;

public class SetOtherMethod{

    public SetOtherForJournalService serivce;

    public Journal journal;

    /**
     * 根据不同的参数 指定查询不同的serivce
     * @param journal
     */
    public SetOtherMethod(Journal journal) throws Exception {
        if(journal.getSupplier() != null){
            this.serivce = new SetOtherForJournalServiceImpl().new SetOtherForJournalSupplier();
        }else if(journal.getCustomer() !=null){
            this.serivce = new SetOtherForJournalServiceImpl().new SetOtherForJournalCustomer();
        }else if(journal.getEmployee()!=null){
            this.serivce = new SetOtherForJournalServiceImpl().new SetOtherForJournalEmployee();
        }else if(journal.getInnerunit()!=null){
            this.serivce = new SetOtherForJournalServiceImpl().new SetOtherForJournalInnerunit();
        }else if(journal.getOthername()!=null){
            this.serivce = new SetOtherForJournalServiceImpl().new SetOtherForJournalOthername();
        }else if(journal.getCapBizObj()!=null){
            this.serivce = new SetOtherForJournalServiceImpl().new SetOtherForJournalCapBizObj();
        }else
            return;
        serivce.setOtherInfo(journal);
    }

}
