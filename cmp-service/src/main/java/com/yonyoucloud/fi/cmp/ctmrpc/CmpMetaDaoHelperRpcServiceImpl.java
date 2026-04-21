package com.yonyoucloud.fi.cmp.ctmrpc;

import com.yonyou.cloud.yts.YtsContext;
import com.yonyou.iuap.yms.id.generator.YmsOidGenerator;
import com.yonyou.ucf.mdd.common.enums.OperationTypeEnum;
import com.yonyou.ucf.mdd.ext.bill.biz.BillBiz;
import com.yonyou.ucf.mdd.ext.bill.dto.BillDataDto;
import com.yonyou.ucf.mdd.ext.bpm.model.VerifyState;
import com.yonyou.ucf.mdd.ext.dao.meta.MetaDaoHelper;
import com.yonyou.ucf.mdd.ext.model.BillContext;
import com.yonyoucloud.fi.cmp.common.CtmException;
import com.yonyou.yonbip.ctm.json.CtmJSONObject;
import com.yonyoucloud.fi.basecom.itf.IFIBillService;
import com.yonyoucloud.fi.cmp.api.ctmrpc.CmpMetaDaoHelperRpcService;
import com.yonyoucloud.fi.cmp.constant.ICmpConstant;
import com.yonyoucloud.fi.cmp.util.CmpMetaDaoHelper;
import com.yonyoucloud.fi.cmp.util.StringUtils;
import com.yonyoucloud.fi.cmp.vo.CmpRpcMetaDaoUpdateDto;
import org.imeta.orm.base.BizObject;
import org.imeta.orm.base.EntityStatus;
import org.imeta.orm.schema.QueryCondition;
import org.imeta.orm.schema.QuerySchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CmpMetaDaoHelperRpcServiceImpl implements CmpMetaDaoHelperRpcService {

    @Autowired
    private IFIBillService fiBillService;

    @Autowired
    YmsOidGenerator ymsOidGenerator;

    @Transactional
    @Override
    public void updateWithCharacterDef(String mainfullname, HashMap<String, Object> mainData, String charaterKey, HashMap<String, Object> subData) throws Exception {
        if (StringUtils.isEmpty(mainfullname) || mainData == null || mainData.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100602"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0012F", "主表全类名和主表数据不能为空") /* "主表全类名和主表数据不能为空" */);
        }
        Object id = mainData.get(ICmpConstant.ID);
        if (id == null || (id instanceof String && StringUtils.isEmpty((String) id))) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100603"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00131", "主表主键不能为空") /* "主表主键不能为空" */);
        }
        //待更新数据
        BizObject bizObject = MetaDaoHelper.findById(mainfullname, id);
        //旧数据
        BizObject oldbizObject = MetaDaoHelper.findById(mainfullname, id);
        if (bizObject == null) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100604"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0012E", "根据主表主键未查到数据") /* "根据主表主键未查到数据" */);
        }
        Set<String> keys = mainData.keySet();
        Iterator<String> iterator = keys.iterator();
        while (bizObject != null && iterator.hasNext()) {
            String key = iterator.next();
            bizObject.put(key, mainData.get(key));
        }
        bizObject.setEntityStatus(EntityStatus.Update);
        oldbizObject.setEntityStatus(EntityStatus.Update);
        //特征
        if (!StringUtils.isEmpty(charaterKey) && subData != null && !subData.isEmpty() && bizObject.get(charaterKey) != null) {
            //设置特征状态
            BizObject subBizObject = bizObject.get(charaterKey);
            //设置回滚数据状态
            BizObject oldSubBizObject = oldbizObject.get(charaterKey);
            Set<String> subkeys = subData.keySet();
            Iterator<String> subiterator = subkeys.iterator();
            while (subiterator.hasNext()) {
                String key = subiterator.next();
                subBizObject.put(key, subData.get(key));
            }
            subBizObject.setEntityStatus(EntityStatus.Update);
            oldSubBizObject.setEntityStatus(EntityStatus.Update);
        }
        //装填需回滚数据
        YtsContext.setYtsContext("oldbizObject", oldbizObject);
        MetaDaoHelper.update(mainfullname, bizObject);
    }

    @Override
    public void updateWithCharacterDefCancel(String mainfullname, HashMap<String, Object> mainData, String charaterKey, HashMap<String, Object> subData) throws Exception {
        //装填需回滚数据
        BizObject oldbizObject = (BizObject) YtsContext.getYtsContext("oldbizObject");
        if (oldbizObject != null) {
            oldbizObject.setPubts(MetaDaoHelper.findById(mainfullname,oldbizObject.getId()).getPubts());
            MetaDaoHelper.update(mainfullname, oldbizObject);
        }
    }

    @Transactional
    @Override
    public void batchUpdateWithCharacterDef(String mainfullname, String charaterKey, List<CmpRpcMetaDaoUpdateDto> list) throws Exception {
        if (list == null || list.isEmpty()) {
            return;
        }
        ArrayList<BizObject> updateDataList = new ArrayList<BizObject>();
        ArrayList<BizObject> oldDataList = new ArrayList<BizObject>();
        for (int i = 0; i < list.size(); i++) {
            HashMap<String, Object> mainData = list.get(i).getMainData();
            HashMap<String, Object> subData = list.get(i).getSubData();
            if (StringUtils.isEmpty(mainfullname) || mainData == null || mainData.isEmpty()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100602"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0012F", "主表全类名和主表数据不能为空") /* "主表全类名和主表数据不能为空" */);
            }
            Object id = mainData.get(ICmpConstant.ID);
            if (id == null || (id instanceof String && StringUtils.isEmpty((String) id))) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100603"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00131", "主表主键不能为空") /* "主表主键不能为空" */);
            }
            BizObject bizObject = MetaDaoHelper.findById(mainfullname, id);
            BizObject oldBizObject = MetaDaoHelper.findById(mainfullname, id);
            if (bizObject == null) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100604"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0012E", "根据主表主键未查到数据") /* "根据主表主键未查到数据" */);
            }
            Set<String> keys = mainData.keySet();
            Iterator<String> iterator = keys.iterator();
            while (bizObject != null && iterator.hasNext()) {
                String key = iterator.next();
                bizObject.put(key, mainData.get(key));
            }
            bizObject.setEntityStatus(EntityStatus.Update);
            oldBizObject.setEntityStatus(EntityStatus.Update);
            //特征
            if (!StringUtils.isEmpty(charaterKey) && subData != null && !subData.isEmpty() && bizObject.get(charaterKey) != null) {
                BizObject subBizObject = bizObject.get(charaterKey);
                BizObject oldSubBizObject = oldBizObject.get(charaterKey);
                Set<String> subkeys = subData.keySet();
                Iterator<String> subiterator = subkeys.iterator();
                while (subiterator.hasNext()) {
                    String key = subiterator.next();
                    subBizObject.put(key, subData.get(key));
                }
                //设置特征状态
                subBizObject.setEntityStatus(EntityStatus.Update);
                oldSubBizObject.setEntityStatus(EntityStatus.Update);
            }
            updateDataList.add(bizObject);
            oldDataList.add(oldBizObject);
        }
        if (updateDataList.isEmpty()) {
            return;
        }
        YtsContext.setYtsContext("oldBizObjectList", oldDataList);//装填需回滚数据
        MetaDaoHelper.update(mainfullname, updateDataList);
    }

    @Override
    public void batchUpdateWithCharacterDefCancel(String mainfullname, String charaterKey, List<CmpRpcMetaDaoUpdateDto> list) throws Exception {
        //回滚数据
        ArrayList<BizObject> oldBizObjectList = (ArrayList<BizObject>) YtsContext.getYtsContext("oldBizObjectList");
        if (oldBizObjectList != null) {
            MetaDaoHelper.fillLastPubts(mainfullname, oldBizObjectList);
            MetaDaoHelper.update(mainfullname, oldBizObjectList);
        }
    }

    @Override
    public void batchInsertWithCharacterDef(String mainfullname, String charaterKey, List<CmpRpcMetaDaoUpdateDto> list) throws Exception {
        if (list == null || list.isEmpty()) {
            return;
        }
        ArrayList<BizObject> insertDataList = new ArrayList<BizObject>();
        for (int i = 0; i < list.size(); i++) {
            HashMap<String, Object> mainData = list.get(i).getMainData();
            HashMap<String, Object> subData = list.get(i).getSubData();
            if (StringUtils.isEmpty(mainfullname) || mainData == null || mainData.isEmpty()) {
                throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100602"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0012F", "主表全类名和主表数据不能为空") /* "主表全类名和主表数据不能为空" */);
            }
            Set<String> keys = mainData.keySet();
            Iterator<String> iterator = keys.iterator();
            BizObject bizObject = new BizObject();
            while (iterator.hasNext()) {
                String key = iterator.next();
                bizObject.put(key, mainData.get(key));
            }
            bizObject.setId(ymsOidGenerator.nextId());
            bizObject.setEntityStatus(EntityStatus.Insert);
            //特征
            if (!StringUtils.isEmpty(charaterKey) && subData != null && !subData.isEmpty() && bizObject.get(charaterKey) != null) {
                BizObject subBizObject = new BizObject();
                Set<String> subkeys = subData.keySet();
                Iterator<String> subiterator = subkeys.iterator();
                while (subiterator.hasNext()) {
                    String key = subiterator.next();
                    subBizObject.put(key, subData.get(key));
                }
                //设置特征状态
                subBizObject.setId(ymsOidGenerator.nextId());
                subBizObject.setEntityStatus(EntityStatus.Insert);
                bizObject.put(charaterKey, subBizObject);
            }
            insertDataList.add(bizObject);
        }
        if (insertDataList.isEmpty()) {
            return;
        }
        YtsContext.setYtsContext("oldBizObjectList", insertDataList);//装填需回滚数据
        CmpMetaDaoHelper.insert(mainfullname, insertDataList);
    }

    @Override
    public void batchInsertWithCharacterDefCancel(String mainfullname, String charaterKey, List<CmpRpcMetaDaoUpdateDto> list) throws Exception {
        //回滚数据
        ArrayList<BizObject> oldBizObjectList = (ArrayList<BizObject>) YtsContext.getYtsContext("oldBizObjectList");
        if (oldBizObjectList != null) {
            MetaDaoHelper.fillLastPubts(mainfullname, oldBizObjectList);
            MetaDaoHelper.delete(mainfullname, oldBizObjectList);
        }
    }

    @Transactional
    @Override
    public void submitByIds(String fullname, List<String> ids, String billnum) throws Exception {
        if (ids == null || ids.size() == 0) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100605"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B00130", "主键不能为空") /* "主键不能为空" */);
        }
        QuerySchema querySchema = QuerySchema.create().addSelect("*");
        querySchema.appendQueryCondition(QueryCondition.name("id").in(ids));
        List<Map<String, Object>> bizObject = MetaDaoHelper.query(fullname, querySchema);
        if (bizObject == null || bizObject.isEmpty()) {
            throw new CtmException(new com.yonyoucloud.fi.cmp.common.CtmErrorCode("033-502-100604"),com.yonyou.iuap.ucf.common.i18n.InternationalUtils.getMessageWithDefault("UID:P_CM-BE_181197B805B0012E", "根据主表主键未查到数据") /* "根据主表主键未查到数据" */);
        }
        BillDataDto bill = new BillDataDto();
        bill.setBillnum(billnum);
        bill.setData(bizObject);
        YtsContext.setYtsContext("submitData", bill);//装填需回滚数据
        boolean submitNow = true;
        if ("cmp.fundcollection.FundCollection".equals(fullname)) {
            for (Map<String, Object> bizObjectElem : bizObject) {
                if (null != bizObjectElem.get("isWfControlled") && !(Boolean) bizObjectElem.get("isWfControlled")) {
                    //不知道为什么到提交就变成3了，这里改回去，保证能正常生成凭证
                    bizObjectElem.put("voucherstatus", 2);
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("param",bill);
                    BillContext billContext = new BillContext();
                    billContext.setCardKey("cmp_fundcollection");
                    billContext.setSubid("CM");
                    billContext.setBilltype("ArchiveList");
                    billContext.setEntityCode(billnum);
                    billContext.setAction("submit");
                    billContext.setBillnum(billnum);
                    billContext.setFullname(fullname);
                    BillBiz.executeRule("audit", billContext, paramMap);
                    submitNow = false;
                    break;
                }
            }
        } else if ("cmp.fundpayment.FundPayment".equals(fullname)) {
            for (Map<String, Object> bizObjectElem : bizObject) {
                if (null != bizObjectElem.get("isWfControlled") && !(Boolean) bizObjectElem.get("isWfControlled")) {
                    //不知道为什么到提交就变成3了，这里改回去，保证能正常生成凭证
                    bizObjectElem.put("voucherstatus", 2);
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("param", bill);
                    BillContext billContext = new BillContext();
                    billContext.setCardKey("cmp_fundpayment");
                    billContext.setSubid("CM");
                    billContext.setBilltype("ArchiveList");
                    billContext.setEntityCode(billnum);
                    billContext.setAction("submit");
                    billContext.setBillnum(billnum);
                    billContext.setFullname(fullname);
                    BillBiz.executeRule("audit", billContext, paramMap);
                    submitNow = false;
                    break;
                }
            }
        }
        if(submitNow) {
            fiBillService.executeUpdate(OperationTypeEnum.SUBMIT.getValue(), bill);
        }
    }

    @Override
    public void submitByIdsCancel(String fullname, List<String> ids, String billnum) throws Exception {
        BillDataDto submitData = (BillDataDto) YtsContext.getYtsContext("submitData");
        if (submitData != null) {
            List<HashMap<String, Object>> bizObjectList = (List<HashMap<String, Object>>) submitData.getData();
            List<BizObject> unSubmitBizObjectList = new ArrayList<>();
            List<BizObject> unApproveBizObjectList = new ArrayList<>();
            for (HashMap<String, Object> oldbizObject : bizObjectList) {
                BizObject bizObject = MetaDaoHelper.findById(fullname, oldbizObject.get(ICmpConstant.ID));
                Short verifystate = bizObject.getShort("verifystate");
                if (verifystate == VerifyState.SUBMITED.getValue()) {
                    unSubmitBizObjectList.add(bizObject);
                } else if (verifystate == VerifyState.COMPLETED.getValue()) {
                    unApproveBizObjectList.add(bizObject);
                }
            }
            //未审批通过的直接撤回
            if(!unSubmitBizObjectList.isEmpty()){
                BillDataDto unSubmitBill = new BillDataDto();
                unSubmitBill.setBillnum(billnum);
                unSubmitBill.setData(unSubmitBizObjectList);
                fiBillService.executeUpdate(OperationTypeEnum.UNSUBMIT.getValue(), unSubmitBill);
            }
            //审批通过的取审
            if(!unApproveBizObjectList.isEmpty()){
                BillDataDto unApprovebill = new BillDataDto();
                unApprovebill.setBillnum(billnum);
                unApprovebill.setData(unApproveBizObjectList);
                fiBillService.executeUpdate(OperationTypeEnum.UNAUDIT.getValue(), unApprovebill);
            }
        }
    }

    @Override
    public List<Map<String, Object>> queryById(String fullname, String selectFieldString, Object id) throws Exception {
        List<Map<String, Object>> retList = MetaDaoHelper.queryById(fullname, selectFieldString, id);
        return retList;
    }

    @Override
    public List<Map<String, Object>> queryObject(String fullname, QuerySchema schema) throws Exception {
        List<Map<String, Object>> retList = MetaDaoHelper.query(fullname, schema);
        return retList;
    }

    @Override
    public List<Map<String, Object>> queryByIds(String fullname, String selectFieldString, List<String> ids) throws Exception {
        List<Map<String, Object>> retList = MetaDaoHelper.queryByIds(fullname, selectFieldString, ids);
        return retList;
    }

    @Override
    public Map<String, Object> queryById(String fullname, Object id) throws Exception {
        BizObject bizObject = MetaDaoHelper.findById(fullname, id);
        String json = CtmJSONObject.toJSONString(bizObject);
        Map<String, Object> retMap = CtmJSONObject.parseObject(json, new HashMap<String, Object>().getClass());
        return retMap;
    }

    @Override
    public List<Map<String, Object>> queryByIds(String fullname, List<String> ids) throws Exception {
        List<Map<String, Object>> retList = new ArrayList<Map<String, Object>>();
        if (ids == null || ids.isEmpty()) {
            return retList;
        }
        for (String id : ids) {
            BizObject bizObject = MetaDaoHelper.findById(fullname, id);
            String json = CtmJSONObject.toJSONString(bizObject);
            Map<String, Object> retMap = CtmJSONObject.parseObject(json, new HashMap<String, Object>().getClass());
            retList.add(retMap);
        }
        return retList;
    }

    @Override
    public String getKeyField(String fullname) throws Exception {
        String keyField = MetaDaoHelper.getKeyField(fullname);
        return keyField;
    }

    @Override
    public String getCodeField(String fullname) throws Exception {
        String codeField = MetaDaoHelper.getCodeField(fullname);
        return codeField;
    }
}
