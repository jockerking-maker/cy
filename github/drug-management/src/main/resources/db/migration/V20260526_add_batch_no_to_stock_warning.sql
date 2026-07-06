-- 预警表增加批号字段（已有数据库执行一次即可，若列已存在可忽略报错）
ALTER TABLE `stock_warning`
    ADD COLUMN `batch_no` varchar(50) NULL COMMENT '批号' AFTER `warehouse_id`;
