package com.yonyoucloud.fi.cmp.bankreconciliation.service;

import com.yonyou.diwork.service.IApplicationService;
import com.yonyou.iuap.bd.staff.dto.Staff;
import com.yonyou.iuap.bd.staff.service.itf.IStaffService;
import com.yonyou.iuap.message.platform.entity.MessageInfoEntity;
import com.yonyou.iuap.message.platform.rpc.IMsgSendService;
import com.yonyou.ucf.basedoc.model.CurrencyTenantDTO;
import com.yonyou.ucf.basedoc.model.EnterpriseBankAcctVO;
import com.yonyou.ucf.mdd.ext.core.AppContext;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.workbench.model.ApplicationVO;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.service.ref.BaseRefRpcService;
import com.yonyoucloud.fi.basecom.util.HttpTookitYts;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankPublishSendMsgService;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliation;
import com.yonyoucloud.fi.cmp.bankreconciliation.BankReconciliationPublishedStaff;
import com.yonyoucloud.fi.cmp.cmpentity.OppositeType;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyoucloud.fi.cmp.constant.ICsplConstant;
import com.yonyoucloud.fi.cmp.constant.IDomainConstant;
import com.yonyoucloud.fi.cmp.enums.PublishedType;
import com.yonyoucloud.fi.cmp.util.DateUtils;
import com.yonyoucloud.fi.cmp.util.QueryBaseDocUtils;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.util.message.CMMessageInfoClient;
import com.yonyoucloud.iuap.upc.api.IMerchantServiceV2;
import com.yonyoucloud.iuap.upc.dto.MerchantDTO;
import com.yonyoucloud.iuap.upc.dto.PrincipalDTO;
import com.yonyoucloud.iuap.upc.dto.PrincipalQryDTO;
import com.yonyoucloud.upc.pub.api.vendor.service.vendor.IVendorPubQueryService;
import com.yonyoucloud.upc.pub.api.vendor.vo.vendor.VendorExtendVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QueryConditionGroup;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @description: 银行流水发布时发送业务消息接口具体实现
 * @author: wanxbo@yonyou.com
 * @date: 2024/9/25 10:59
 */

@Slf4j
@Service
public class BankPublishSendMsgServiceImpl implements BankPublishSendMsgService {

    @Autowired
    private IMsgSendService iMsgSendService;

    @Autowired
    private BaseRefRpcService baseRefRpcService;

    @Autowired
    IVendorPubQueryService vendorPubQueryService;

    //消息中心
    private static final String CHANNEL = "uspace";
    //待办消息
    private static final String MESSAGE_TYPE = "createToDo";

    @Override
    public void sendPublishMsgToCreateToDo(BankReconciliation bankReconciliation) throws Exception {
        //CZFW-376916 设置需要跳过发送待办消息的租户
        String skipYtenantIdStr = AppContext.getEnvConfig("cmp.createtodo.skipYtenantId","jd0bc7k0");
        List<String> skipYtenantIdList = Arrays.asList(skipYtenantIdStr.split(","));
        if (skipYtenantIdList.contains(AppContext.getCurrentUser().getYhtTenantId())){
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-101050"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_1D89379A050800DF", "该租户跳过银行流水待办消息发送,skiplist:") /* "该租户跳过银行流水待办消息发送,skiplist:" */ + skipYtenantIdStr);
        }

        // 只有发布给组织,或者发布给员工时，发送待办消息
        if (bankReconciliation.getPublished_type() == null ||
                (PublishedType.ORG.getCode() != bankReconciliation.getPublished_type() && PublishedType.EMPLOYEE.getCode() != bankReconciliation.getPublished_type() ) ) {
            log.error("sendPublishMsgToCreateToDo 只有按组织发布和按员工发布会推送待办");
            return;
        }

        // 按组织发布时，限定对方类型为客户和供应商
        if (PublishedType.ORG.getCode() == bankReconciliation.getPublished_type() && (bankReconciliation.getOppositetype() != OppositeType.Customer.getValue()
                && bankReconciliation.getOppositetype() != OppositeType.Supplier.getValue())){
            log.error("sendPublishMsgToCreateToDo 按组织发布时，限定对方类型为客户和供应商");
            return;
        }

        //接收人集合
        List<String> receiverList = new ArrayList<>();
        setReceiverList(receiverList, bankReconciliation);
        if (CollectionUtils.isEmpty(receiverList)) {
            log.error("sendPublishMsgToCreateToDo 发送银行流水发布消息接收人信息为空");
            return;
        }

        //设置信息发送渠道；消息中心
        List<String> channels = new ArrayList<>();
        channels.add(CHANNEL);

        //待办信息
//        Map<String, Object> extParams = new HashMap<>();
        Map<String, Object> createTodoExt = new HashMap<>();
        ApplicationVO applicationVO = AppContext.getBean(IApplicationService.class).findByTenantIdAndApplicationCode(AppContext.getCurrentUser().getYhtTenantId(), "CM");
        createTodoExt.put("appId", applicationVO.getApplicationId());
        // 删除 或者 完成 代办时 需要 businessKey
        createTodoExt.put("businessKey", bankReconciliation.getId().toString());
        // 待办消息跳转信息赋值；用来定位对应的对账单
        createTodoExt.put("webUrl", genWebUrl(bankReconciliation));
        //移动端信息
        createTodoExt.put("mUrl",genMUrl(bankReconciliation));
//        extParams.put("createTodoExt", createTodoExt);

        //发送消息到待办
        // 0515调整
        //发消息时组装消息体
        MessageInfoEntity entity = new MessageInfoEntity(ICsplConstant.SYSID_DIWORK, AppContext.getCurrentUser().getYhtTenantId());
        entity.setMsgId(UUID.randomUUID().toString()); // 新加参数必填幂等，可以直接使用UUID
        entity.setSrcId(IDomainConstant.MDD_DOMAIN_CM); //新加参数必填幂等，srcId+msgId两个参数控制幂等，不确定填什么可以填微服务编码
        entity.setChannels(channels);
        entity.setSubject(getTitle(bankReconciliation));
        entity.setContent(getContent(bankReconciliation));
        entity.setReceiver(receiverList);
        entity.setMessageType(MESSAGE_TYPE);
        //放以前extParams里的createTodoExt里的businessKey，没有则不需要加这行
        entity.setBusinessKey(bankReconciliation.getId().toString());
        //放以前extParams里的createTodoExt里的appId，没有则不需要加这行
        entity.setAppId(applicationVO.getApplicationId());
        entity.setCreateToDoExt(createTodoExt);
        //消息落库
        CMMessageInfoClient.sendMessageByDefaultDs(entity);
//        iMsgSendService.sendPlainMessageToUsers(ICsplConstant.SYSID_DIWORK, AppContext.getCurrentUser().getYhtTenantId(),
//                receiverList, channels, MESSAGE_TYPE, getTitle(bankReconciliation), getContent(bankReconciliation), extParams);
    }

    @Override
    public void handleConfirmMsg(BankReconciliation bankReconciliation) throws Exception {
        CtmJSONObject params = new CtmJSONObject();
        params.put("msgTsLong",System.currentTimeMillis());
        //幂等校验id
        params.put("srcMsgId","confirmDone" + bankReconciliation.getId().toString());
        params.put("srcAppId","cmp_billclaimcenterlist");
        params.put("tenantId",AppContext.getCurrentUser().getYhtTenantId());
        ApplicationVO applicationVO = AppContext.getBean(IApplicationService.class).findByTenantIdAndApplicationCode(AppContext.getCurrentUser().getYhtTenantId(), "CM");
        params.put("appId",applicationVO.getApplicationId());
        params.put("businessKey",bankReconciliation.getId().toString());

        //待办已处理接口
//        String url = AppContext.getEnvConfig("domain.iuap-apcom-messageplatform") + "/message-platform-web/integration/todo/update/done";
        // 0515对接消息中心
        String url = AppContext.getEnvConfig("domain.iuap-apcom-messagecenter") + "/rest/v3/todo/idempotent/push/item/done";
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        String result = HttpTookitYts.doPostWithJson(url, CtmJSONObject.toJSONString(params), header,"UTF-8");
        log.error("handleConfirmMsg result={}",result);
    }

    @Override
    public void handleDeleteMsg(BankReconciliation bankReconciliation) throws Exception {
        CtmJSONObject params = new CtmJSONObject();
        params.put("msgTsLong",System.currentTimeMillis());
        //幂等校验id
        params.put("srcMsgId","deleteMsg" + bankReconciliation.getId().toString()+System.currentTimeMillis());
        params.put("srcAppId","cmp_billclaimcenterlist");
        params.put("tenantId",AppContext.getCurrentUser().getYhtTenantId());
        ApplicationVO applicationVO = AppContext.getBean(IApplicationService.class).findByTenantIdAndApplicationCode(AppContext.getCurrentUser().getYhtTenantId(), "CM");
        params.put("appId",applicationVO.getApplicationId());
        params.put("businessKey",bankReconciliation.getId().toString());

        CtmJSONObject initiatedVars = new CtmJSONObject();
        initiatedVars.put("delete",true);
        params.put("initiatedVars",initiatedVars);

        //待办已处理接口
//        String url = AppContext.getEnvConfig("domain.iuap-apcom-messageplatform") + "/message-platform-web/integration/todo/update/revocation";
        // 0515对接消息中心
        String url = AppContext.getEnvConfig("domain.iuap-apcom-messagecenter") + "/rest/v3/todo/idempotent/push/item/revocation";
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        String result = HttpTookitYts.doPostWithJson(url, CtmJSONObject.toJSONString(params), header,"UTF-8");
        log.error("handleDeleteMsg result={}",result);
    }

    /**
     * 设置接收人信息
     * 对方类型=客户，接收人为客户专员；对方类型=供应商，接收人为供应商专员
     * @param receiverList 接收人id集合
     * @param bankReconciliation 银行流水信息
     * @throws Exception
     */
    private void setReceiverList(List<String> receiverList,BankReconciliation bankReconciliation) throws Exception{
        if (PublishedType.ORG.getCode() == bankReconciliation.getPublished_type() && StringUtils.isEmpty(bankReconciliation.getOppositeobjectid())){
            return;
        }
        //对方类型客户；接收人为客户专员
        if (PublishedType.ORG.getCode() == bankReconciliation.getPublished_type() &&  bankReconciliation.getOppositetype() == OppositeType.Customer.getValue()) {
            MerchantDTO merchantById = QueryBaseDocUtils.getMerchantById(Long.valueOf(bankReconciliation.getOppositeobjectid()));
            IMerchantServiceV2 merchantService = AppContext.getBean(IMerchantServiceV2.class);
            PrincipalQryDTO principalQryDTO = new PrincipalQryDTO();
            principalQryDTO.setMerchantId(Long.valueOf(bankReconciliation.getOppositeobjectid()));
            principalQryDTO.setMerchantApplyRangeOrgId(merchantById.getRangesOrgId());
            List<PrincipalDTO> principalDTOList = merchantService.getPrincipal(principalQryDTO);
            //客户专员是员工，需要查询对应的用户id-user_id
            IStaffService iStaffService = AppContext.getBean(IStaffService.class);
            if (principalDTOList != null && !principalDTOList.isEmpty()) {
                for (PrincipalDTO principalDTO : principalDTOList) {
                    Staff staff = iStaffService.getById(principalDTO.getProfessSalesmanId());
                    if (staff != null){
                        receiverList.add(staff.getUser_id());
                    }
                }
            }
        }
        //对方类型供应商；接收人为供应商专员
        if (PublishedType.ORG.getCode() == bankReconciliation.getPublished_type() &&  bankReconciliation.getOppositetype() == OppositeType.Supplier.getValue()) {
            VendorExtendVO vendorExtendVO = vendorPubQueryService.getVendorExtendByVendorIdAndOrgIdV2(Long.valueOf(bankReconciliation.getOppositeobjectid()),null);
            if (vendorExtendVO != null && !StringUtils.isEmpty(vendorExtendVO.getPerson())) {
                //供应商专员是员工，需要查询对应的用户id-user_id
                IStaffService iStaffService = AppContext.getBean(IStaffService.class);
                Staff staff = iStaffService.getById(vendorExtendVO.getPerson());
                if (staff != null){
                    receiverList.add(staff.getUser_id());
                }
            }
        }

        // 按员工发布,给对应的员工推送待办
        if (PublishedType.EMPLOYEE.getCode() == bankReconciliation.getPublished_type()){
            QuerySchema querySchema = QuerySchema.create().addSelect("*");
            QueryConditionGroup group = QueryConditionGroup.and(QueryCondition.name("mainid").eq(bankReconciliation.getId().toString()));
            querySchema.addCondition(group);
            List<BankReconciliationPublishedStaff> staffList = MetaDaoHelper.queryObject(BankReconciliationPublishedStaff.ENTITY_NAME, querySchema, null);
            IStaffService iStaffService = AppContext.getBean(IStaffService.class);
            for (BankReconciliationPublishedStaff bankReconciliationPublishedStaff : staffList){
                Staff staff = iStaffService.getById(bankReconciliationPublishedStaff.getStaff());
                if (staff != null){
                    receiverList.add(staff.getUser_id());
                }
            }
        }
    }

    /**
     * 获取待办信息内容
     * @return String 【银行交易流号：XXX】、【本方账号】【交易日期】、【金额XXX】、【币种】、
     * * 【对方单位】、【摘要】【发布人】（调度任务触发时，默认为系统用户）、【发布时间】MM-DD HH:ss
     */
    private String getContent(BankReconciliation bankReconciliation) throws Exception{
        StringBuilder content = new StringBuilder();
        if (!StringUtils.isEmpty(bankReconciliation.getBank_seq_no())){
            content.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400376", "【银行交易流水号：%s、") /* "【银行交易流水号：%s、" */, bankReconciliation.getBank_seq_no()));
        }
        // 银行账户
        EnterpriseBankAcctVO enterpriseBankAcctVO= baseRefRpcService.queryEnterpriseBankAccountById(bankReconciliation.getBankaccount());
        //【本方账号】【交易日期】
        content.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540037A", "银行账号：%s、交易日期：%s、") /* "银行账号：%s、交易日期：%s、" */, enterpriseBankAcctVO.getAcctName(), DateUtils.dateFormat(bankReconciliation.getTran_date(),"yyyy-MM-dd")));
        //【金额XXX】
        content.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540037D", "金额：%s、") /* "金额：%s、" */, bankReconciliation.getTran_amt()));
        //【币种】
        CurrencyTenantDTO currencyTenantDTO = baseRefRpcService.queryCurrencyById(bankReconciliation.getCurrency());
        content.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400374", "币种：%s、") /* "币种：%s、" */, currencyTenantDTO.getName()));
        if (!StringUtils.isEmpty(bankReconciliation.getOppositeobjectname())){
            //【对方单位】
            content.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400375", "对方单位：%s、") /* "对方单位：%s、" */, bankReconciliation.getOppositeobjectname()));
        }
        //【摘要】
        if (!StringUtils.isEmpty(bankReconciliation.getRemark())) {
            content.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400377", "摘要：%s、") /* "摘要：%s、" */, bankReconciliation.getRemark()));
        }
        //【发布人】
        content.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400379", "发布人：%s、") /* "发布人：%s、" */, AppContext.getCurrentUser().getName()));
        //【发布时间】MM-DD HH:ss
        content.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540037B", "发布时间：%s】") /* "发布时间：%s】" */, DateUtils.dateFormat(bankReconciliation.getPublish_time(),"MM-dd HH:ss")));

        return content.toString();
    }

    /**
     * 获取待办标题
     * @return String 【银行交易流号：XXX】+【交易日期】+【金额XXX】+【对方单位】已发布，请及时认领！
     */
    private String getTitle(BankReconciliation bankReconciliation) throws Exception {
        StringBuilder content = new StringBuilder();
        if (!StringUtils.isEmpty(bankReconciliation.getBank_seq_no())){
            content.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400378", "【银行交易流号：%s】") /* "【银行交易流号：%s】" */, bankReconciliation.getBank_seq_no()));
        }
        content.append(String.format("【%s】", DateUtils.dateFormat(bankReconciliation.getTran_date(),"yyyy-MM-dd")));//@notranslate
        if (!StringUtils.isEmpty(bankReconciliation.getOppositeobjectname())){
            content.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A4200540037C", "【金额%s】【%s】已发布，请及时认领！") /* "【金额%s】【%s】已发布，请及时认领！" */, bankReconciliation.getTran_amt(),bankReconciliation.getOppositeobjectname()));
        }else {
            content.append(String.format(com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_21B2A42005400373", "【金额%s】已发布，请及时认领！") /* "【金额%s】已发布，请及时认领！" */, bankReconciliation.getTran_amt()));
        }
        return content.toString();
    }

    /**
     * 获取待办详情地址，微博端地址 isMobile=false
     */
    private String genWebUrl(BankReconciliation bankReconciliation) {
        return AppContext.getEnvConfig("domain.url") + "/mdf-node/meta/voucher/cmp_billclaimcenterlist/" + bankReconciliation.getId().toString() + "?domainKey=ctm-cmp&businessStepCode=&&tenantId=" + AppContext.getCurrentUser().getYhtTenantId()
                + "&apptype=mdf&serviceCode=cmp_billclaimcenterlist&concat_url=mc&from_mc_workflow=0&isMobile=false&url_actual_build_source=iuap-apcom-messageplatform&adt=wf&bankid=" + bankReconciliation.getId().toString() + "&fromCreateToDoFlag=true&publishType=" + bankReconciliation.getPublished_type();
    }
    /**
     * 获取待办详情地址，移动端地址 isMobile=true
     */
    private String genMUrl(BankReconciliation bankReconciliation) {
        //发布到组织到公共认领池。移动端公共认领池=cmp_billclaimcenter_commonlist
        if (bankReconciliation.getPublished_type() == PublishedType.ORG.getCode()){
            return AppContext.getEnvConfig("domain.url") + "/mdf-node/meta/voucherlist/cmp_billclaimcenter_commonlist/" + bankReconciliation.getId().toString() + "?domainKey=ctm-cmp&businessStepCode=&&tenantId=" + AppContext.getCurrentUser().getYhtTenantId()
                    + "&apptype=mdf&serviceCode=cmp_billclaimcenterlist&concat_url=mc&from_mc_workflow=0&isMobile=true&url_actual_build_source=iuap-apcom-messageplatform&adt=wf&bankid=" + bankReconciliation.getId().toString() + "&fromCreateToDoFlag=true&publishType=" + bankReconciliation.getPublished_type();
        }else {
            //发布到非组织的为到我的认领池。移动端公共认领池=cmp_billclaimcenter_m_list
            return AppContext.getEnvConfig("domain.url") + "/mdf-node/meta/voucherlist/cmp_billclaimcenter_m_list/" + bankReconciliation.getId().toString() + "?domainKey=ctm-cmp&businessStepCode=&&tenantId=" + AppContext.getCurrentUser().getYhtTenantId()
                    + "&apptype=mdf&serviceCode=cmp_billclaimcenterlist&concat_url=mc&from_mc_workflow=0&isMobile=true&url_actual_build_source=iuap-apcom-messageplatform&adt=wf&bankid=" + bankReconciliation.getId().toString() + "&fromCreateToDoFlag=true&publishType=" + bankReconciliation.getPublished_type();
        }
    }
}
