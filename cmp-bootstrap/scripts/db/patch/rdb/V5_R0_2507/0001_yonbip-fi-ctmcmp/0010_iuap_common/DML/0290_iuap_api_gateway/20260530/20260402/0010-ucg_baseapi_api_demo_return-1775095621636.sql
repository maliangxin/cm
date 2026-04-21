-- iuap_ipaas 
-- ----------------------------
-- 创建时间：2026-04-02-10:07:01
-- 创建人：wmq
-- 查询条件：{"itemKey":"iuap-api-gateway_apiPubSys","projectName":"cmp","queryInfoList":[{"paramName":"api_id","paramValue":"2401384874613145608"}],"profile":"基准环境","dbType":"MYSQL","queryId":"b8ec52c95dfd4d299a726b8b47575485","needOriginData":false,"extraData":{}}
-- 工具服务所在环境: test
-- ----------------------------
-- ----------------------------
-- ucg_baseapi_api_demo_return
-- ----------------------------
DELETE FROM ucg_baseapi_api_demo_return WHERE api_id IN ('2401384874613145608');

-- beginBatch
REPLACE INTO  `ucg_baseapi_api_demo_return`(`id`,`api_id`,`content`,`gmt_create`,`api_demo_return_desc`,`return_type`,`right_or_not`,`gmt_update`,`dr`,`ytenant_id`,`micro_service_code`,`applicationCode`) VALUES 
('2402745150900535298','2401384874613145608','{\n	"code": "200",\n	"data": "",\n	"message": "操作成功"\n}','2025-11-12 10:59:11.000',NULL,'JSON',1,'2026-04-01 14:14:48.000',0,'0',NULL,NULL),
('2402745150900535299','2401384874613145608','','2025-11-12 10:59:11.000',NULL,'JSON',0,'2026-04-01 14:14:48.000',0,'0',NULL,NULL) ;
-- endBatch

