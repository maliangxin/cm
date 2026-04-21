package com.yonyoucloud.fi.cmp.bill;

import com.yonyoucloud.fi.cmp.cmpentity.RpType;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import org.imeta.orm.base.BizObject;

import java.math.BigDecimal;

public class AfterReceivebillSaveBillRuleToCmp extends AfterSaveBillRuleToCmp {

	@Override
	public Journal generateJournal(Journal journal,BizObject bizObject) {
		journal.set("direction", Direction.Debit.getValue());
		journal.set("debitoriSum",bizObject.get(IBussinessConstant.ORI_SUM));
		journal.set("debitnatSum",bizObject.get(IBussinessConstant.NAT_SUM));
		journal.set("creditoriSum",BigDecimal.ZERO);
		journal.set("creditnatSum",BigDecimal.ZERO);
		journal.set("rptype", RpType.ReceiveBill.getValue());
		return journal;
	}
}
