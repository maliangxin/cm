-- iuap_ipaas 
-- ----------------------------
-- 创建时间：2026-04-02-10:07:01
-- 创建人：wmq
-- 查询条件：{"itemKey":"iuap-api-gateway_apiPubSys","projectName":"cmp","queryInfoList":[{"paramName":"api_id","paramValue":"2401384874613145608"}],"profile":"基准环境","dbType":"MYSQL","queryId":"b8ec52c95dfd4d299a726b8b47575485","needOriginData":false,"extraData":{}}
-- 工具服务所在环境: test
-- ----------------------------
-- ----------------------------
-- ucg_baseapi_param_map
-- ----------------------------
DELETE FROM ucg_baseapi_param_map WHERE api_id IN ('2401384874613145608');

-- beginBatch
REPLACE INTO  `ucg_baseapi_param_map`(`id`,`gmt_create`,`name`,`param_order`,`param_desc`,`param_type`,`request_param_type`,`aggregated_value_object`,`api_id`,`arr`,`map_name`,`map_param_type`,`param_list`,`primitive`,`service_param_type`,`gmt_update`,`dr`,`base_type`,`example`,`parent_id`,`ytenant_id`,`enable_multi`) VALUES 
('2402745150900535311','2025-11-12 10:59:11.000','data',0,'货币兑换数据','object','BodyParam',0,'2401384874613145608',0,'data','BodyParam',NULL,0,'object','2026-04-01 14:14:48.000',0,1,NULL,NULL,'0',0),
('2402745150900535312','2025-11-12 10:59:11.000','id',0,'货币兑换id','string','BodyParam',0,'2401384874613145608',0,'id','BodyParam',NULL,0,'string','2026-04-01 14:14:48.000',0,1,NULL,'2402745150900535311','0',0),
('2402745150900535313','2025-11-12 10:59:11.000','contractNo',1,'外汇交易合约编号','string','BodyParam',0,'2401384874613145608',0,'contractNo','BodyParam',NULL,0,'string','2026-04-01 14:14:48.000',0,1,NULL,'2402745150900535311','0',0),
('2402745150900535314','2025-11-12 10:59:11.000','settlestatus',2,'交割状态；3：处理中，5：已交割，6：逾期，7：交割失败（已经交割完成的单据，不允许更新结果信息）','number','BodyParam',0,'2401384874613145608',0,'settlestatus','BodyParam',NULL,0,'number','2026-04-01 14:14:48.000',0,1,NULL,'2402745150900535311','0',0),
('2402745150900535315','2025-11-12 10:59:11.000','purchaseamount',3,'买入金额（必须大于0）','number','BodyParam',0,'2401384874613145608',0,'purchaseamount','BodyParam',NULL,0,'number','2026-04-01 14:14:48.000',0,1,NULL,'2402745150900535311','0',0),
('2402745150900535316','2025-11-12 10:59:11.000','sellamount',4,'卖出金额（必须大于0）','number','BodyParam',0,'2401384874613145608',0,'sellamount','BodyParam',NULL,0,'number','2026-04-01 14:14:48.000',0,1,NULL,'2402745150900535311','0',0),
('2402745150900535317','2025-11-12 10:59:11.000','exchangerate',5,'成交汇率','number','BodyParam',0,'2401384874613145608',0,'exchangerate','BodyParam',NULL,0,'number','2026-04-01 14:14:48.000',0,1,NULL,'2402745150900535311','0',0),
('2402745150900535318','2025-11-12 10:59:11.000','settledate',6,'交割日期','date','BodyParam',0,'2401384874613145608',0,'settledate','BodyParam',NULL,0,'date','2026-04-01 14:14:48.000',0,1,NULL,'2402745150900535311','0',0),
('2418522893653114911','2025-12-03 17:12:02.000','characterDef',7,'特征组','characteristic','BodyParam',0,'2401384874613145608',0,'characterDef','BodyParam',NULL,0,'characteristic','2026-04-01 14:14:48.000',0,1,NULL,'2402745150900535311','0',0) ;
-- endBatch

