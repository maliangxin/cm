package com.yonyoucloud.fi.cmp.event.dataFlatten;

import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.event.sendEvent.ICmpSendEventService;
import com.yonyoucloud.fi.fieaai.busievent.api.v1.IBusiStatusQueryService;
import com.yonyoucloud.fi.fieaai.busievent.dto.v1.BusiEventBriefInfo;
import com.yonyoucloud.fi.fieaai.busievent.dto.v1.BusiQueryStatusItemDTO;
import com.yonyoucloud.fi.fieaai.busievent.dto.v1.BusiStatusRespDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <h1>对接事项中心：业务端数据拉取</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-05-31 9:16
 */

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CmpMattersPostDataFlattenServiceImpl implements IBusiStatusQueryService {

    private final ICmpSendEventService cmpSendEventService;

    @Override
    public BusiStatusRespDTO queryStatus(BusiQueryStatusItemDTO busiQueryStatusItemDTO) {
        // 1.入参校验
        cmpSendEventService.dataVerify(busiQueryStatusItemDTO, Boolean.TRUE);
        // 2.查询领域数据
        BusiStatusRespDTO busiStatusRespDTO;
        try {
            // 获取查询参数
            List<Short> list = cmpSendEventService.getVoucherStatusId(busiQueryStatusItemDTO);
            QuerySchema querySchema = QuerySchema.create();
            busiStatusRespDTO = cmpSendEventService.getBusiStatusRespDTO(busiQueryStatusItemDTO, list, querySchema, Boolean.TRUE);
        } catch (Exception e) {
            log.error("CmpMattersPostDataFlattenServiceImpl, handle fail, busiQueryStatusItemDTO={}, errorMsg={}",
                    CtmJSONObject.toJSONString(busiQueryStatusItemDTO), e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100194"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B8003F",
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B8003F", "业务端数据拉取逻辑处理异常，请检查请求参数是否正确！") /* "业务端数据拉取逻辑处理异常，请检查请求参数是否正确！" */) /* "业务端数据拉取逻辑处理异常，请检查请求参数是否正确！" */ + "=>" + e.getMessage());
        }
        return busiStatusRespDTO;
    }

    @Override
    public BusiStatusRespDTO queryStatus(List<BusiQueryStatusItemDTO> busiQueryStatusItemDTOS) {
        // 1.入参校验
        busiQueryStatusItemDTOS.forEach(busiQueryStatusItemDTO -> {
            cmpSendEventService.dataVerify(busiQueryStatusItemDTO, Boolean.TRUE);
        });
        // 2.查询领域数据
        BusiStatusRespDTO busiStatusRespDTO = new BusiStatusRespDTO();
        List<BusiEventBriefInfo> busiEventBriefInfos = new ArrayList<>();
        try {
            busiQueryStatusItemDTOS.forEach(busiQueryStatusItemDTO -> {
                BusiStatusRespDTO busiStatusRespDTOInner;
                // 获取查询参数
                List<Short> list = cmpSendEventService.getVoucherStatusId(busiQueryStatusItemDTO);
                QuerySchema querySchema = QuerySchema.create();
                try {
                    busiStatusRespDTOInner = cmpSendEventService.getBusiStatusRespDTO(busiQueryStatusItemDTO, list, querySchema, Boolean.TRUE);
                    busiEventBriefInfos.addAll(busiStatusRespDTOInner.getBusiEventBriefInfos());
                } catch (Exception ex) {
                    log.error("getBusiStatusRespDTO执行失败" + ex.getMessage());
                }
            });
            busiStatusRespDTO.setMessage(ICmpConstant.SUCCESS);
            busiStatusRespDTO.setCode(ICmpConstant.REQUEST_SUCCESS_STATUS_CODE);
            busiStatusRespDTO.setIsSync(ICmpConstant.CONSTANT_STR_ZERO);
            busiStatusRespDTO.setTotal(CollectionUtils.isNotEmpty(busiEventBriefInfos)
                    ? String.valueOf(busiEventBriefInfos.size()) : ICmpConstant.CONSTANT_STR_ZERO);
            busiStatusRespDTO.setBusiEventBriefInfos(busiEventBriefInfos);
        } catch (Exception e) {
            log.error("CmpMattersPostDataFlattenServiceImpl, handle fail, busiQueryStatusItemDTO={}, errorMsg={}",
                    CtmJSONObject.toJSONString(busiQueryStatusItemDTOS), e.getMessage());
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100194"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B8003F",
                    com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_18502D8205B8003F", "业务端数据拉取逻辑处理异常，请检查请求参数是否正确！") /* "业务端数据拉取逻辑处理异常，请检查请求参数是否正确！" */) /* "业务端数据拉取逻辑处理异常，请检查请求参数是否正确！" */ + "=>" + e.getMessage());
        }
        return busiStatusRespDTO;
    }
}
