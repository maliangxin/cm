package com.yonyoucloud.fi.cmp.bill;

import com.yonyoucloud.fi.cmp.cmpentity.RpType;
import com.yonyoucloud.fi.cmp.constant.IBussinessConstant;
import com.yonyoucloud.fi.cmp.journal.Journal;
import com.yonyoucloud.fi.cmp.newapi.ctmrpc.enums.Direction;
import org.imeta.orm.base.BizObject;

import java.math.BigDecimal;

public class AfterPaybillSaveBillRuleToCmp extends AfterSaveBillRuleToCmp {

	@Override
	public Journal generateJournal(Journal journal,BizObject bizObject) {
		journal.set("direction", Direction.Credit.getValue());
		journal.set("debitoriSum",BigDecimal.ZERO);
		journal.set("debitnatSum",BigDecimal.ZERO);
		journal.set("creditoriSum",bizObject.get(IBussinessConstant.ORI_SUM));
		journal.set("creditnatSum",bizObject.get(IBussinessConstant.NAT_SUM));
		journal.set("rptype", RpType.PayBill.getValue());
		return journal;
	}
}
