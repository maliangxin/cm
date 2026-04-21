package com.yonyoucloud.fi.cmp.util.business;


import lombok.extern.slf4j.Slf4j;
import org.imeta.orm.base.BizObject;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class TransactionEventListenerToCmp {


    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void beforeCommit(PayloadApplicationEvent<BizObject> event){
       log.info("============== beforeCommit =============");
    }


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(PayloadApplicationEvent<BizObject> event){
        log.info("============== afterCommit =============");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void afterCompletion(PayloadApplicationEvent<BizObject> event){
        log.info("============== afterCompletion =============");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void afterRollback(PayloadApplicationEvent<BizObject> event){
        log.info("============== afterRollback =============");
    }

}
