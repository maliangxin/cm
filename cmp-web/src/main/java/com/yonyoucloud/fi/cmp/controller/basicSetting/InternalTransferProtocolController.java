package com.yonyoucloud.fi.cmp.controller.basicSetting;

import com.yonyou.diwork.permission.annotations.DiworkPermission;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.util.ResultMessage;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.IServicecodeConstant;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.InternalTransferProtocolVO;
import com.yonyoucloud.fi.cmp.internaltransferprotocol.service.InternalTransferProtocolService;
import com.yonyoucloud.fi.cmp.util.ValueUtils;
import com.yonyoucloud.fi.cmp.vo.ResultMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.yonyou.iuap.framework.sdk.common.utils.ResponseUtils.renderJson;

/**
 * <h1>内转协议Controller</h1>
 *
 * @author Sun GuoCai
 * @version 1.0
 * @since 2023-09-10 9:19
 */
@Controller
@RequestMapping("/transfer-protocol")
@Slf4j
@RequiredArgsConstructor
public class InternalTransferProtocolController {

    private final InternalTransferProtocolService internalTransferProtocolService;

    /**
     * <h2>内转协议启停用按钮后端逻辑处理</h2>
     *
     * @param bill:     入参
     * @param response: 响应体
     * @author Sun GuoCai
     * @since 2020/11/18 10:51
     */
    @PostMapping("/enabledSwitch")
    @DiworkPermission({IServicecodeConstant.INTERNAL_TRANSFER_PROTOCOL_SERVICE_CODE})
    public void updatePayApplicationBillStatusByClosed(@RequestBody BillDataDto bill, HttpServletResponse response) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) bill.getData();
        String ids = ValueUtils.isNotEmptyObj(params.get("ids")) ? params.get("ids").toString() : null;
        if (!ValueUtils.isNotEmptyObj(ids)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100105"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418005F", "操作的单据ID不能为空！") /* "操作的单据ID不能为空！" */);
        }
        String isEnabledType = ValueUtils.isNotEmptyObj(params.get("isEnabledType")) ? params.get("isEnabledType").toString() : null;
        if (!ValueUtils.isNotEmptyObj(isEnabledType)) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100106"), com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_17FE8C540418005E", "参数错误！") /* "参数错误！" */);
        }
        ResultMessageVO<String> result = internalTransferProtocolService.updateEnabledStatusOfTransferProtocolByIds(ids, isEnabledType);
        renderJson(response, ResultMessage.data(result));
    }


    @PostMapping("/generatorBill")
    public void generatorBill(@RequestBody InternalTransferProtocolVO internalTransferProtocolVO, HttpServletResponse response) {
        ResultMessageVO<String> resultMessageVO = internalTransferProtocolService.internalTransferBillGeneratesFundPaymentBill(internalTransferProtocolVO);
        renderJson(response, ResultMessage.data(resultMessageVO));
    }

    @PostMapping("deleteInternalTransferBillByIds")
    public void deleteInternalTransferBillByIds(@RequestBody BillDataDto param, HttpServletResponse response) throws Exception {
        ResultMessageVO<Object> object = internalTransferProtocolService.deleteInternalTransferBillByIds(param);
        renderJson(response, ResultMessage.data(object));
    }

}
