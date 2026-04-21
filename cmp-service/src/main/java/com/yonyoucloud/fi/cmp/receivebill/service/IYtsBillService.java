package com.yonyoucloud.fi.cmp.receivebill.service;

import org.imeta.orm.base.BizObject;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * @author maliang
 * @version V1.0
 * @date 2021/5/14 13:36
 * @Copyright yonyou
 */
public interface IYtsBillService {


    /**
     * iris框架同步 sagas模式，通过cancel属性指定全局事务失败时，当前业务逻辑的反向操作，必须要由业务正确实现
     *
     * @return
     */
    @Transactional
    public abstract BizObject syncBill(BizObject dto, Map params) throws  Exception;

    /**
     * 异步sagas模式，业务接口需要加上异步注解，需要将业务正确的分解成两阶段执行，通过cancel指定全局事务失败后的回滚逻辑
     * 通过confirm指定全局事务成功后需要确认的提交逻辑
     *
     * @param dto
     * @return
     */
//    @ApiOperation(value = "异步单据操作", response = BizObject.class)
//    @YtsTransactional(cancel = "cancel", confirm = "confirm", mode = TransactionMode.ASYNCSAGAS)
//    @Async
//    public abstract BizObject asyncBill(BizObject dto, Map params)throws  Exception;

    /**
     * 同步TCC模式，需要将业务正确的分解成两阶段执行，通过cancel指定全局事务失败后的回滚逻辑              通过confirm指定全局事务成功后需要确认的提交逻辑
     *
     * @param dto
     * @return
     */
//    @ApiOperation(value = "二阶段操作", response = BizObject.class)
//    @YtsTransactional(cancel = "cancel", confirm = "confirm", mode = TransactionMode.TCC)
//    public abstract BizObject tccBill(BizObject dto, Map params)throws  Exception;

    /**
     * 提交逻辑，针对tcc模式或异步sagas模式，全局事务成功时，当前服务要处理的动作
     *
     * @param dto
     * @return
     */
//    @ApiOperation(value = "提交事务操作", response = BizObject.class)
//    public abstract BizObject confirm(BizObject dto, Map params)throws  Exception;

    /**
     * 回滚逻辑，对简单sagas模式、tcc模式或异步sagas模式，全局事务失败时，当前服务需要处理的动作
     *
     * @param dto
     * @return
     */
//    @ApiOperation(value = "回滚事务操作", response = BizObject.class)
//    public abstract BizObject cancel(BizObject dto, Map params)throws  Exception;



}
