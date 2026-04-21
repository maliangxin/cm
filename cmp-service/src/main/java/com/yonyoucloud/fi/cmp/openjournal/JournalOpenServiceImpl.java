package com.yonyoucloud.fi.cmp.openjournal;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.api.ctmrpc.JournalOpeninService;
import com.yonyoucloud.fi.cmp.journal.JournalVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Date 2021/6/10 14:39
 * @Author shangxd
 * @Description 登日记账的接口，生成日记账明细
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JournalOpenServiceImpl implements JournalOpeninService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JournalOpenServiceImpl.class);

    private final JournalOpenCommonService journalForeginCommonService;

    private static final String JOURNAL_REGISTER_ROLLBACK="JOURNAL_REGISTER_ROLLBACK";

    private static final String JOURNAL_UPDATE_ROLLBACK="JOURNAL_UPDATE_ROLLBACK";

    private static final String JOURNAL_DELETE_ROLLBACK="JOURNAL_DELETE_ROLLBACK";

    /**
     * 登日记账
     * @param journalVo 日记账对外接口类
     * @return
     */
    @Override
    @Transactional
    public Boolean journalRegister(JournalVo journalVo) throws Exception {
        log.error("*******JournalOpenServiceImpl.generateJournal interface parms:{}", CtmJSONObject.toJSONString(journalVo));
        YtsContext.setYtsContext(JOURNAL_REGISTER_ROLLBACK, 0);
        journalForeginCommonService.journalRegister(journalVo);
        YtsContext.setYtsContext(JOURNAL_REGISTER_ROLLBACK, 1);
        return true;
    }

    /**
     * 登日记账回滚方法
     * @param journalVo 日记账对外接口类
     * @return
     * @throws Exception
     */
    @Override
    public Boolean journalRegisterCancel(@NotNull JournalVo journalVo) throws Exception{
        log.error("**JournalOpenServiceImpl.generateJournalCancel journal param:{}", CtmJSONObject.toJSONString(journalVo));
        if((int)YtsContext.getYtsContext(JOURNAL_REGISTER_ROLLBACK) == 1){
            journalForeginCommonService.rollbackJournalRegister(journalVo);
        }
        return true;
    }


    @Override
    @Transactional
    public Boolean journalUpdate(JournalVo journalVo) throws Exception {
        log.info("*******JournalOpenServiceImpl.journalUpdate interface parms[]:" + journalVo.toString());
        YtsContext.setYtsContext(JOURNAL_UPDATE_ROLLBACK, 0);
        journalForeginCommonService.journalUpdate(journalVo);
        YtsContext.setYtsContext(JOURNAL_UPDATE_ROLLBACK, 1);
        return true;
    }

    @Override
    public Boolean rollbackJournalUpdate(JournalVo journalVo) throws Exception{
        log.info("**JournalOpenServiceImpl.rollbackJournalUpdate journal info：[]", journalVo.toString());
        if((int) YtsContext.getYtsContext(JOURNAL_UPDATE_ROLLBACK) == 1){
            journalForeginCommonService.rollbackJournalUpdate(journalVo);
        }
        return true;
    }

    /**
     *  日记账删除接口
     * @param journalVo 日记账信息
     * @return
     */
    @Override
    @Transactional
    public Boolean journalDelete(JournalVo journalVo) throws Exception{
        log.info("*******JournalOpenServiceImpl.journalDelete interface parms[]:" + journalVo.toString());
        YtsContext.setYtsContext(JOURNAL_DELETE_ROLLBACK, 0);
        journalForeginCommonService.journalDelete(journalVo);
        YtsContext.setYtsContext(JOURNAL_DELETE_ROLLBACK, 1);
        return true;
    }

    /**
     * 日记账删除回滚接口
     * @param journalVo 日记账信息
     * @return
     */
    @Override
    @Transactional
    public Boolean rollbackJournalDelete(JournalVo journalVo) throws Exception{
        log.info("**JournalOpenServiceImpl.rollbackJournalDelete journal info：[]", journalVo.toString());
        if((int) YtsContext.getYtsContext(JOURNAL_DELETE_ROLLBACK) == 1){
            journalForeginCommonService.rollbackJournalDelete(journalVo);
        }
        return true;
    }


}
