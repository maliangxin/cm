-- iuap_ipaas 
-- ----------------------------
-- 创建时间：2026-04-02-10:07:01
-- 创建人：wmq
-- 查询条件：{"itemKey":"iuap-api-gateway_apiPubSys","projectName":"cmp","queryInfoList":[{"paramName":"api_id","paramValue":"2401384874613145608"}],"profile":"基准环境","dbType":"MYSQL","queryId":"b8ec52c95dfd4d299a726b8b47575485","needOriginData":false,"extraData":{}}
-- 工具服务所在环境: test
-- ----------------------------
-- ----------------------------
-- ucg_baseapi_param_return
-- ----------------------------
DELETE FROM ucg_baseapi_param_return WHERE api_id IN ('2401384874613145608');

-- beginBatch
REPLACE INTO  `ucg_baseapi_param_return`(`id`,`api_id`,`name`,`param_order`,`param_desc`,`param_type`,`base_type`,`arr`,`default_value`,`required`,`visible`,`gmt_create`,`gmt_update`,`dr`,`example`,`parent_id`,`ref_type_context`,`ref_type`,`ytenant_id`,`full_name`,`define_hidden`,`micro_service_code`,`applicationCode`,`def_param_id`,`integrate_object_id`,`format`,`decimals`,`max_length`,`enable_multi`) VALUES 
('2402745150900535308','2401384874613145608','code',0,'返回码，调用成功时返回200','string',1,0,'',0,1,'2025-11-12 10:59:11.000','2026-04-01 14:14:48.000',0,'200',NULL,NULL,'0','0',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,'0',0),
('2402745150900535309','2401384874613145608','data',1,'入参','string',1,0,'',0,1,'2025-11-12 10:59:11.000','2026-04-01 14:14:48.000',0,'',NULL,NULL,'0','0',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,'0',0),
('2402745150900535310','2401384874613145608','message',2,'调用失败时的错误信息','string',1,0,'',0,1,'2025-11-12 10:59:11.000','2026-04-01 14:14:48.000',0,'操作成功',NULL,NULL,'0','0',NULL,0,NULL,NULL,NULL,NULL,NULL,NULL,'0',0) ;
-- endBatch

