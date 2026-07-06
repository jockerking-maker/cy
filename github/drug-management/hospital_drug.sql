/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80042
 Source Host           : localhost:3306
 Source Schema         : hospital_drug

 Target Server Type    : MySQL
 Target Server Version : 80042
 File Encoding         : 65001

 Date: 03/06/2026 14:14:23
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for audit_record
-- ----------------------------
DROP TABLE IF EXISTS `audit_record`;
CREATE TABLE `audit_record`  (
  `record_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
  `order_id` bigint(0) NOT NULL COMMENT '关联采购单 ID',
  `audit_user_id` bigint(0) NULL DEFAULT NULL COMMENT '审核人 ID',
  `audit_user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核人姓名',
  `audit_result` int(0) NULL DEFAULT NULL COMMENT '审核结果（1通过/2驳回）',
  `audit_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '审核意见',
  `audit_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `audit_level` int(0) NULL DEFAULT NULL COMMENT '审核级别（1一级/2二级/3三级）',
  PRIMARY KEY (`record_id`) USING BTREE,
  INDEX `idx_order_id`(`order_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '审核记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of audit_record
-- ----------------------------
INSERT INTO `audit_record` VALUES (1, 1, 2, '李审核', 1, '审核通过，药品需求合理', '2026-05-10 15:00:00', 1);
INSERT INTO `audit_record` VALUES (2, 2, 2, '李审核', 1, '审核通过', '2026-05-12 16:30:00', 1);
INSERT INTO `audit_record` VALUES (3, 5, 2, '李审核', 2, '驳回，供应商反馈暂时缺货，建议更换供应商', '2026-05-21 09:00:00', 1);
INSERT INTO `audit_record` VALUES (5, 7, 2, '李审核', 1, '通过', '2026-06-02 15:30:39', 1);
INSERT INTO `audit_record` VALUES (7, 8, 2, '李审核', 1, 'tongyi', '2026-06-03 09:08:37', 1);

-- ----------------------------
-- Table structure for drug_in
-- ----------------------------
DROP TABLE IF EXISTS `drug_in`;
CREATE TABLE `drug_in`  (
  `in_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '入库 ID',
  `in_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '入库单号',
  `order_id` bigint(0) NULL DEFAULT NULL COMMENT '采购订单 ID',
  `in_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '采购入库' COMMENT '入库类型（采购入库/退货入库/调拨入库/其他）',
  `drug_id` bigint(0) NOT NULL COMMENT '药品 ID',
  `warehouse_id` bigint(0) NOT NULL COMMENT '仓库 ID',
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批号',
  `quantity` int(0) NOT NULL COMMENT '入库数量',
  `purchase_price` decimal(10, 2) NOT NULL COMMENT '入库单价',
  `production_date` date NULL DEFAULT NULL COMMENT '生产日期',
  `expiry_date` date NULL DEFAULT NULL COMMENT '有效期',
  `in_date` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `create_user_id` bigint(0) NULL DEFAULT NULL COMMENT '操作人 ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除/1已删除）',
  PRIMARY KEY (`in_id`) USING BTREE,
  UNIQUE INDEX `in_no`(`in_no`) USING BTREE,
  INDEX `idx_drug_id`(`drug_id`) USING BTREE,
  INDEX `idx_warehouse_id`(`warehouse_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品入库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of drug_in
-- ----------------------------
INSERT INTO `drug_in` VALUES (1, 'RK20260511001', 1, '采购入库', 1, 1, 'PC20260511001', 100, 8.80, '2026-03-15', '2028-03-15', '2026-05-11 10:00:00', 4, '采购订单CG20260510001到货', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_in` VALUES (2, 'RK20260511002', 1, '采购入库', 2, 1, 'PC20260511002', 80, 12.50, '2026-04-01', '2028-04-01', '2026-05-11 10:30:00', 4, '采购订单CG20260510001到货', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_in` VALUES (3, 'RK20260511003', 1, '采购入库', 5, 1, 'PC20260511003', 50, 15.60, '2026-02-20', '2028-02-20', '2026-05-11 11:00:00', 4, '采购订单CG20260510001到货', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_in` VALUES (4, 'RK20260511004', 1, '采购入库', 13, 2, 'PC20260511004', 200, 2.20, '2026-05-01', '2029-05-01', '2026-05-11 14:00:00', 4, '采购订单CG20260510001到货', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_in` VALUES (5, 'RK20260511005', 1, '采购入库', 15, 2, 'PC20260511005', 20, 19.50, '2026-01-10', '2030-01-10', '2026-05-11 14:30:00', 4, '采购订单CG20260510001到货', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_in` VALUES (6, 'RK20260513001', 2, '采购入库', 3, 1, 'PC20260513001', 150, 4.20, '2026-04-10', '2029-04-10', '2026-05-13 09:00:00', 4, '采购订单CG20260512001到货', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_in` VALUES (7, 'RK20260513002', 2, '采购入库', 4, 2, 'PC20260513002', 200, 2.10, '2026-04-15', '2029-04-15', '2026-05-13 09:30:00', 4, '采购订单CG20260512001到货', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_in` VALUES (8, 'RK20260513003', 2, '采购入库', 9, 2, 'PC20260513003', 100, 3.20, '2026-03-20', '2029-03-20', '2026-05-13 10:00:00', 4, '采购订单CG20260512001到货', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_in` VALUES (20, 'RK20260602161003', 3, '采购入库', 1, 3, 'PC20260602001', 80, 8.80, '2026-06-01', '2028-06-01', '2026-06-02 16:10:03', 4, '80 阿莫西林', '2026-06-02 16:10:03', 0);
INSERT INTO `drug_in` VALUES (21, 'RK20260602161121', 3, '采购入库', 1, 2, 'PC20260602002', 100, 8.80, '2026-06-01', '2028-06-01', '2026-06-02 16:11:22', 4, '100 阿莫西林', '2026-06-02 16:11:21', 0);
INSERT INTO `drug_in` VALUES (22, 'RK20260602164938', NULL, '补货入库', 15, 2, 'PC20260511005', 10, 19.50, '2026-01-10', '2030-01-10', '2026-06-02 16:49:38', 4, '库存不足补货（追加至现有批次）', '2026-06-02 16:49:38', 0);
INSERT INTO `drug_in` VALUES (23, 'RK20260602165000', NULL, '补货入库', 15, 2, 'PC20260511005', 1, 19.50, '2026-01-10', '2030-01-10', '2026-06-02 16:50:01', 4, '库存不足补货（追加至现有批次）', '2026-06-02 16:50:00', 0);
INSERT INTO `drug_in` VALUES (24, 'RK20260602165258', NULL, '补货入库', 6, 1, 'PC20260401001', 21, 10.20, '2026-01-15', '2028-01-15', '2026-06-02 16:52:58', 4, '库存不足补货（追加至现有批次）', '2026-06-02 16:52:58', 0);
INSERT INTO `drug_in` VALUES (25, 'RK20260602165317', NULL, '补货入库', 7, 1, 'PC20260405001', 1, 14.80, '2026-02-01', '2029-02-01', '2026-06-02 16:53:17', 4, '库存不足补货（追加至现有批次）', '2026-06-02 16:53:17', 0);
INSERT INTO `drug_in` VALUES (26, 'RK20260602165622', NULL, '补货入库', 6, 3, 'PC20260505002', 21, 10.20, '2026-04-10', '2028-04-10', '2026-06-02 16:56:22', 4, '库存不足补货（追加至现有批次）', '2026-06-02 16:56:22', 0);
INSERT INTO `drug_in` VALUES (27, 'RK20260602172010', 7, '采购入库', 10, 1, 'PC20260602003', 100, 2.50, '2026-06-01', '2029-06-01', '2026-06-02 17:20:10', 1, '100维生素C\n', '2026-06-02 17:20:10', 0);
INSERT INTO `drug_in` VALUES (28, 'RK20260602172847', 7, '采购入库', 10, 1, 'PC20260602004', 100, 2.50, '2026-06-01', '2029-06-01', '2026-06-02 17:28:47', 1, '1', '2026-06-02 17:28:47', 0);

-- ----------------------------
-- Table structure for drug_info
-- ----------------------------
DROP TABLE IF EXISTS `drug_info`;
CREATE TABLE `drug_info`  (
  `drug_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '药品 ID',
  `drug_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '药品编码',
  `drug_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '药品名称',
  `drug_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '药品类型（西药/中药/中成药/耗材）',
  `spec` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单位',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '销售单价',
  `purchase_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '采购单价',
  `warning_num` int(0) NULL DEFAULT 10 COMMENT '库存预警值',
  `max_warning_num` int(0) NULL DEFAULT 100 COMMENT '最大库存预警值',
  `shelf_life` int(0) NULL DEFAULT NULL COMMENT '保质期（月）',
  `supplier_id` bigint(0) NULL DEFAULT NULL COMMENT '供应商 ID',
  `production_enterprise` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '生产企业',
  `approval_num` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批准文号',
  `status` int(0) NULL DEFAULT 1 COMMENT '状态（0 下架/1 上架）',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除/1已删除）',
  PRIMARY KEY (`drug_id`) USING BTREE,
  UNIQUE INDEX `drug_code`(`drug_code`) USING BTREE,
  INDEX `idx_supplier_id`(`supplier_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of drug_info
-- ----------------------------
INSERT INTO `drug_info` VALUES (1, 'YP001', '阿莫西林胶囊', '西药', '0.5g×24粒/盒', '盒', 12.50, 8.80, 20, 150, 24, 1, '国药集团医药有限公司', '国药准字H13023964', 1, '2026-05-24 16:30:46', '2026-06-03 08:53:58', 0);
INSERT INTO `drug_info` VALUES (2, 'YP002', '头孢克洛胶囊', '西药', '0.25g×12粒/盒', '盒', 18.00, 12.50, 50, 400, 24, 2, '华润医药商业集团有限公司', '国药准字H10940157', 1, '2026-05-24 16:30:46', '2026-06-03 08:54:05', 0);
INSERT INTO `drug_info` VALUES (3, 'YP003', '布洛芬片', '西药', '0.2g×20片/盒', '盒', 6.80, 4.20, 30, 190, 36, 1, '国药集团医药有限公司', '国药准字H10900089', 1, '2026-05-24 16:30:46', '2026-06-03 08:54:10', 0);
INSERT INTO `drug_info` VALUES (4, 'YP004', '对乙酰氨基酚片', '西药', '0.5g×12片/盒', '盒', 3.50, 2.10, 30, 150, 36, 4, '广州医药集团有限公司', '国药准字H61021285', 1, '2026-05-24 16:30:46', '2026-06-03 08:54:54', 0);
INSERT INTO `drug_info` VALUES (5, 'YP005', '阿奇霉素片', '西药', '0.25g×6片/盒', '盒', 22.00, 15.60, 40, 300, 24, 4, '广州医药集团有限公司', '国药准字H20066921', 1, '2026-05-24 16:30:46', '2026-06-03 08:54:48', 0);
INSERT INTO `drug_info` VALUES (6, 'YP006', '奥美拉唑胶囊', '西药', '20mg×14粒/盒', '盒', 15.80, 10.20, 50, 300, 24, 3, '阿斯利康制药有限公司', '国药准字H20031110', 1, '2026-05-24 16:30:46', '2026-06-02 15:18:07', 0);
INSERT INTO `drug_info` VALUES (7, 'YP007', '蒙脱石散', '西药', '3g×10袋/盒', '盒', 19.50, 13.80, 30, 200, 36, 4, '博福-益普生制药有限公司', '国药准字H20000690', 1, '2026-05-24 16:30:46', '2026-06-02 15:18:12', 0);
INSERT INTO `drug_info` VALUES (8, 'YP008', '氯雷他定片', '西药', '10mg×6片/盒', '盒', 16.00, 11.00, 20, 100, 36, 4, '上海先灵葆雅制药有限公司', '国药准字H20040549', 1, '2026-05-24 16:30:46', '2026-06-02 15:18:17', 0);
INSERT INTO `drug_info` VALUES (9, 'YP009', '复方甘草片', '中成药', '100片/瓶', '瓶', 5.50, 3.20, 20, 200, 36, 2, '北京同仁堂制药有限公司', '国药准字H11022441', 1, '2026-05-24 16:30:46', '2026-06-02 15:18:29', 0);
INSERT INTO `drug_info` VALUES (10, 'YP010', '维生素C片', '中成药', '0.1g×100片/瓶', '瓶', 4.00, 2.50, 15, 40, 36, 5, '东北制药总厂', '国药准字H21020733', 1, '2026-05-24 16:30:46', '2026-06-02 15:19:05', 0);
INSERT INTO `drug_info` VALUES (11, 'YP011', '葡萄糖注射液', '中成药', '5%×500ml/瓶', '瓶', 2.80, 1.60, 50, 500, 24, 5, '四川科伦药业股份有限公司', '国药准字H51020636', 1, '2026-05-24 16:30:46', '2026-06-02 15:19:17', 0);
INSERT INTO `drug_info` VALUES (12, 'YP012', '氯化钠注射液', '中成药', '0.9%×500ml/瓶', '瓶', 2.50, 1.40, 50, 500, 24, 5, '四川科伦药业股份有限公司', '国药准字H51021058', 1, '2026-05-24 16:30:46', '2026-06-02 15:19:21', 0);
INSERT INTO `drug_info` VALUES (13, 'YP013', '红霉素软膏', '中药', '10g/支', '支', 3.80, 2.20, 20, 150, 36, 1, '马鞍山丰原制药有限公司', '国药准字H34020497', 1, '2026-05-24 16:30:46', '2026-06-02 15:19:26', 0);
INSERT INTO `drug_info` VALUES (14, 'YP014', '开塞露', '中药', '20ml/支', '支', 2.00, 1.10, 30, 300, 24, 2, '上海运佳黄浦制药有限公司', '国药准字H31020583', 1, '2026-05-24 16:30:46', '2026-06-02 15:19:30', 0);
INSERT INTO `drug_info` VALUES (15, 'YP015', '云南白药', '中药', '4g/瓶', '瓶', 28.00, 19.50, 25, 100, 48, 4, '云南白药集团股份有限公司', '国药准字Z53020798', 1, '2026-05-24 16:30:46', '2026-06-02 15:19:35', 0);

-- ----------------------------
-- Table structure for drug_lock
-- ----------------------------
DROP TABLE IF EXISTS `drug_lock`;
CREATE TABLE `drug_lock`  (
  `lock_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '锁定 ID',
  `lock_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '锁定单号',
  `drug_id` bigint(0) NOT NULL COMMENT '药品 ID',
  `warehouse_id` bigint(0) NOT NULL COMMENT '仓库 ID',
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批次号',
  `lock_num` int(0) NOT NULL DEFAULT 0 COMMENT '锁定数量',
  `unlock_num` int(0) NOT NULL DEFAULT 0 COMMENT '已解锁数量',
  `lock_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '锁定原因',
  `lock_user_id` bigint(0) NULL DEFAULT NULL COMMENT '锁定人 ID',
  `lock_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '锁定时间',
  `unlock_user_id` bigint(0) NULL DEFAULT NULL COMMENT '解锁人 ID',
  `unlock_time` datetime(0) NULL DEFAULT NULL COMMENT '解锁时间',
  `status` int(0) NULL DEFAULT 0 COMMENT '状态（0 锁定中/1 已解锁/2 已取消）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除/1已删除）',
  PRIMARY KEY (`lock_id`) USING BTREE,
  UNIQUE INDEX `lock_no`(`lock_no`) USING BTREE,
  INDEX `idx_drug_id`(`drug_id`) USING BTREE,
  INDEX `idx_warehouse_id`(`warehouse_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品锁定记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of drug_lock
-- ----------------------------
INSERT INTO `drug_lock` VALUES (1, 'SD20260524164720', 15, 2, 'PC20260511005', 5, 0, '备用', 1, '2026-05-24 16:47:21', NULL, NULL, 0, NULL, '2026-05-24 16:47:20', '2026-05-24 16:47:20', 0);
INSERT INTO `drug_lock` VALUES (14, 'SD20260602150531', 1, 1, 'PC20250101001', 30, 0, '备用', 1, '2026-06-02 15:05:32', NULL, NULL, 0, NULL, '2026-06-02 15:05:31', '2026-06-02 15:05:31', 0);
INSERT INTO `drug_lock` VALUES (15, 'SD20260602154820', 1, 1, 'PC20250101001', 15, 0, '1', 4, '2026-06-02 15:48:20', NULL, NULL, 0, NULL, '2026-06-02 15:48:20', '2026-06-02 15:48:20', 0);
INSERT INTO `drug_lock` VALUES (16, 'SD20260602161146', 1, 2, 'PC20260602002', 100, 0, '备用', 4, '2026-06-02 16:11:46', NULL, NULL, 0, NULL, '2026-06-02 16:11:46', '2026-06-02 16:11:46', 0);
INSERT INTO `drug_lock` VALUES (17, 'SD20260602212857', 6, 3, 'PC20260505002', 3, 0, '备用', 1, '2026-06-02 21:28:58', NULL, NULL, 0, NULL, '2026-06-02 21:28:57', '2026-06-02 21:28:57', 0);

-- ----------------------------
-- Table structure for drug_out
-- ----------------------------
DROP TABLE IF EXISTS `drug_out`;
CREATE TABLE `drug_out`  (
  `out_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '出库 ID',
  `out_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '出库单号',
  `drug_id` bigint(0) NOT NULL COMMENT '药品 ID',
  `warehouse_id` bigint(0) NOT NULL COMMENT '仓库 ID',
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批号',
  `quantity` int(0) NOT NULL COMMENT '出库数量',
  `sale_price` decimal(10, 2) NOT NULL COMMENT '出库单价',
  `out_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '出库类型（领用/销售/报损）',
  `relate_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '关联单号',
  `out_date` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '出库时间',
  `create_user_id` bigint(0) NULL DEFAULT NULL COMMENT '操作人 ID',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除/1已删除）',
  PRIMARY KEY (`out_id`) USING BTREE,
  UNIQUE INDEX `out_no`(`out_no`) USING BTREE,
  INDEX `idx_drug_id`(`drug_id`) USING BTREE,
  INDEX `idx_warehouse_id`(`warehouse_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品出库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of drug_out
-- ----------------------------
INSERT INTO `drug_out` VALUES (1, 'CK20260512001', 1, 1, 'PC20260511001', 20, 12.50, '科室领用', 'LY20260512001', '2026-05-12 09:00:00', 4, '内科门诊领用', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_out` VALUES (2, 'CK20260514001', 11, 1, 'PC20260420001', 50, 2.80, '科室领用', 'LY20260514001', '2026-05-14 10:00:00', 4, '输液室领用', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_out` VALUES (3, 'CK20260515001', 3, 1, 'PC20260513001', 30, 6.80, '科室领用', 'LY20260515001', '2026-05-15 11:00:00', 4, '发热门诊领用', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_out` VALUES (4, 'CK20260516001', 12, 1, 'PC20260420002', 20, 2.50, '科室领用', 'LY20260516001', '2026-05-16 14:00:00', 4, '急诊科领用', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_out` VALUES (5, 'CK20260518001', 6, 1, 'PC20260401001', 10, 15.80, '科室领用', 'LY20260518001', '2026-05-18 09:30:00', 4, '消化内科领用', '2026-05-24 16:30:46', 0);
INSERT INTO `drug_out` VALUES (8, 'CK20260531084017', 1, 1, 'PC20260511001', 5, 12.50, '门诊领药', '001', '2026-05-31 08:40:17', 1, '', '2026-05-31 08:40:17', 0);
INSERT INTO `drug_out` VALUES (9, 'CK20260531084136', 1, 1, 'PC20260511001', 5, 12.50, '调拨', '002', '2026-05-31 08:41:37', 1, '', '2026-05-31 08:41:36', 0);
INSERT INTO `drug_out` VALUES (10, 'CK20260531084251', 11, 1, 'PC20260420001', 5, 2.80, '住院领药', '003', '2026-05-31 08:42:51', 1, '', '2026-05-31 08:42:51', 0);
INSERT INTO `drug_out` VALUES (11, 'CK20260602154235', 1, 1, 'PC20260511001', 5, 20.00, '住院领药', NULL, '2026-06-02 15:42:36', 4, '住院领药', '2026-06-02 15:42:36', 0);
INSERT INTO `drug_out` VALUES (12, 'CK20260602154803', 1, 1, 'PC20260511001', 1, 12.50, '调拨', NULL, '2026-06-02 15:48:04', 4, '测试', '2026-06-02 15:48:03', 0);
INSERT INTO `drug_out` VALUES (13, 'CK20260602211558', 1, 1, 'PC20260511001', 1, 12.50, '门诊领药', NULL, '2026-06-02 21:15:59', 1, 'test', '2026-06-02 21:15:58', 0);

-- ----------------------------
-- Table structure for drug_stock
-- ----------------------------
DROP TABLE IF EXISTS `drug_stock`;
CREATE TABLE `drug_stock`  (
  `stock_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '库存 ID',
  `drug_id` bigint(0) NOT NULL COMMENT '药品 ID',
  `warehouse_id` bigint(0) NOT NULL COMMENT '仓库 ID',
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批号',
  `quantity` int(0) NOT NULL DEFAULT 0 COMMENT '库存数量',
  `lock_num` int(0) NOT NULL DEFAULT 0 COMMENT '锁定数量',
  `production_date` date NULL DEFAULT NULL COMMENT '生产日期',
  `expiry_date` date NULL DEFAULT NULL COMMENT '有效期',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`stock_id`) USING BTREE,
  UNIQUE INDEX `uk_drug_warehouse_batch`(`drug_id`, `warehouse_id`, `batch_no`) USING BTREE,
  INDEX `idx_drug_id`(`drug_id`) USING BTREE,
  INDEX `idx_warehouse_id`(`warehouse_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品库存表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of drug_stock
-- ----------------------------
INSERT INTO `drug_stock` VALUES (1, 1, 1, 'PC20260511001', 70, 0, '2026-03-15', '2028-03-15', '2026-05-24 16:30:46', '2026-06-02 21:48:49');
INSERT INTO `drug_stock` VALUES (2, 2, 1, 'PC20260511002', 55, 0, '2026-04-01', '2028-04-01', '2026-05-24 16:30:46', '2026-06-02 21:48:49');
INSERT INTO `drug_stock` VALUES (3, 5, 1, 'PC20260511003', 50, 0, '2026-02-20', '2028-02-20', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (4, 3, 1, 'PC20260513001', 120, 0, '2026-04-10', '2029-04-10', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (5, 13, 2, 'PC20260511004', 195, 0, '2026-05-01', '2029-05-01', '2026-05-24 16:30:46', '2026-06-02 21:43:10');
INSERT INTO `drug_stock` VALUES (6, 15, 2, 'PC20260511005', 31, 5, '2026-01-10', '2030-01-10', '2026-05-24 16:30:46', '2026-06-02 16:50:01');
INSERT INTO `drug_stock` VALUES (7, 4, 2, 'PC20260513002', 180, 0, '2026-04-15', '2029-04-15', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (8, 9, 2, 'PC20260513003', 80, 0, '2026-03-20', '2029-03-20', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (9, 6, 1, 'PC20260401001', 51, 0, '2026-01-15', '2028-01-15', '2026-05-24 16:30:46', '2026-06-02 16:52:58');
INSERT INTO `drug_stock` VALUES (10, 7, 1, 'PC20260405001', 25, 0, '2026-02-01', '2029-02-01', '2026-05-24 16:30:46', '2026-06-02 16:53:17');
INSERT INTO `drug_stock` VALUES (11, 8, 2, 'PC20260410001', 15, 0, '2026-03-01', '2029-03-01', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (12, 10, 2, 'PC20260315001', 50, 0, '2026-01-20', '2029-01-20', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (13, 11, 1, 'PC20260420001', 195, 0, '2026-04-01', '2028-04-01', '2026-05-24 16:30:46', '2026-05-31 08:42:51');
INSERT INTO `drug_stock` VALUES (14, 12, 1, 'PC20260420002', 178, 0, '2026-04-05', '2028-04-05', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (15, 14, 2, 'PC20260425001', 100, 0, '2026-03-10', '2028-03-10', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (16, 1, 2, 'PC20260401002', 10, 0, '2026-01-20', '2028-01-20', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (17, 11, 3, 'PC20260501001', 60, 0, '2026-04-15', '2028-04-15', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (18, 12, 3, 'PC20260501002', 50, 0, '2026-04-20', '2028-04-20', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (19, 3, 3, 'PC20260505001', 30, 0, '2026-04-25', '2029-04-25', '2026-05-24 16:30:46', '2026-05-24 16:30:46');
INSERT INTO `drug_stock` VALUES (20, 6, 3, 'PC20260505002', 38, 3, '2026-04-10', '2028-04-10', '2026-05-24 16:30:46', '2026-06-02 21:28:58');
INSERT INTO `drug_stock` VALUES (21, 1, 1, 'PC20250101001', 40, 30, '2024-01-15', '2026-07-23', '2026-05-24 16:52:07', '2026-06-02 21:48:16');
INSERT INTO `drug_stock` VALUES (22, 3, 1, 'PC20240601001', 8, 8, '2024-06-01', '2026-05-20', '2026-05-24 16:52:07', '2026-05-24 16:52:07');
INSERT INTO `drug_stock` VALUES (23, 6, 3, 'PC20240615001', 15, 0, '2024-06-15', '2026-07-08', '2026-05-24 16:52:07', '2026-06-02 21:27:26');
INSERT INTO `drug_stock` VALUES (24, 9, 2, 'PC20240301001', 12, 0, '2024-03-01', '2026-06-18', '2026-05-24 16:52:07', '2026-05-24 16:52:07');
INSERT INTO `drug_stock` VALUES (25, 1, 3, 'PC20260602001', 80, 0, '2026-06-01', '2028-06-01', '2026-06-02 16:10:03', '2026-06-02 16:10:03');
INSERT INTO `drug_stock` VALUES (26, 1, 2, 'PC20260602002', 100, 50, '2026-06-01', '2028-06-01', '2026-06-02 16:11:21', '2026-06-02 16:12:06');
INSERT INTO `drug_stock` VALUES (27, 10, 1, 'PC20260602003', 100, 0, '2026-06-01', '2029-06-01', '2026-06-02 17:20:10', '2026-06-02 17:20:10');
INSERT INTO `drug_stock` VALUES (28, 10, 1, 'PC20260602004', 100, 0, '2026-06-01', '2029-06-01', '2026-06-02 17:28:47', '2026-06-02 17:28:47');

-- ----------------------------
-- Table structure for purchase_order
-- ----------------------------
DROP TABLE IF EXISTS `purchase_order`;
CREATE TABLE `purchase_order`  (
  `order_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '订单 ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号',
  `supplier_id` bigint(0) NULL DEFAULT NULL COMMENT '供应商 ID',
  `order_date` datetime(0) NULL DEFAULT NULL COMMENT '订单日期',
  `total_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '订单总金额',
  `status` int(0) NULL DEFAULT 0 COMMENT '状态（0 待审核/1 已审核/2 已入库/3 已取消）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_user_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人 ID',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除/1已删除）',
  PRIMARY KEY (`order_id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no`) USING BTREE,
  INDEX `idx_supplier_id`(`supplier_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of purchase_order
-- ----------------------------
INSERT INTO `purchase_order` VALUES (1, 'CG20260510001', 1, '2026-05-10 09:30:00', 3520.00, 2, '5月份抗生素类药品常规采购', 3, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `purchase_order` VALUES (2, 'CG20260512001', 2, '2026-05-12 10:15:00', 1860.00, 2, '解热镇痛类药品补货', 3, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `purchase_order` VALUES (5, 'CG20260520001', 5, '2026-05-20 16:45:00', 980.00, 3, '维生素类采购，已驳回-供应商缺货', 3, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `purchase_order` VALUES (6, 'CG20260602152731', 1, '2026-06-02 07:27:31', 440.00, 3, '', 1, '2026-06-02 15:28:18', '2026-06-02 15:28:18', 0);
INSERT INTO `purchase_order` VALUES (7, 'CG20260602152909', 5, '2026-06-02 07:29:09', 250.00, 2, '100瓶维生素C', 3, '2026-06-02 15:29:44', '2026-06-02 15:29:44', 0);
INSERT INTO `purchase_order` VALUES (8, 'CG20260602171828', 4, '2026-06-02 17:18:28', 2715.00, 1, '需要药品', 3, '2026-06-02 17:18:55', '2026-06-02 17:18:55', 0);

-- ----------------------------
-- Table structure for purchase_order_item
-- ----------------------------
DROP TABLE IF EXISTS `purchase_order_item`;
CREATE TABLE `purchase_order_item`  (
  `item_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '明细 ID',
  `order_id` bigint(0) NOT NULL COMMENT '订单 ID',
  `drug_id` bigint(0) NOT NULL COMMENT '药品 ID',
  `purchase_num` int(0) NOT NULL COMMENT '采购数量',
  `purchase_price` decimal(10, 2) NOT NULL COMMENT '采购单价',
  `amount` decimal(12, 2) NOT NULL COMMENT '小计金额',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`item_id`) USING BTREE,
  INDEX `idx_order_id`(`order_id`) USING BTREE,
  INDEX `idx_drug_id`(`drug_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购订单明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of purchase_order_item
-- ----------------------------
INSERT INTO `purchase_order_item` VALUES (1, 1, 1, 100, 8.80, 880.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (2, 1, 2, 80, 12.50, 1000.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (3, 1, 5, 50, 15.60, 780.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (4, 1, 13, 200, 2.20, 440.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (5, 1, 15, 20, 19.50, 390.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (6, 2, 3, 150, 4.20, 630.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (7, 2, 4, 200, 2.10, 420.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (8, 2, 9, 100, 3.20, 320.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (9, 2, 14, 150, 1.10, 165.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (10, 2, 8, 30, 11.00, 330.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (11, 3, 6, 100, 10.20, 1020.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (12, 3, 7, 80, 13.80, 1104.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (13, 3, 8, 60, 11.00, 660.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (14, 3, 1, 100, 8.80, 880.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (15, 3, 11, 200, 1.60, 320.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (16, 3, 12, 200, 1.40, 280.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (21, 5, 10, 200, 2.50, 500.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (22, 5, 11, 150, 1.60, 240.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (23, 5, 12, 150, 1.40, 210.00, '2026-05-24 16:30:46');
INSERT INTO `purchase_order_item` VALUES (24, 6, 1, 50, 8.80, 440.00, '2026-06-02 15:28:18');
INSERT INTO `purchase_order_item` VALUES (25, 7, 10, 100, 2.50, 250.00, '2026-06-02 15:29:44');
INSERT INTO `purchase_order_item` VALUES (26, 8, 7, 50, 13.80, 690.00, '2026-06-02 17:18:55');
INSERT INTO `purchase_order_item` VALUES (27, 8, 8, 60, 11.00, 660.00, '2026-06-02 17:18:55');
INSERT INTO `purchase_order_item` VALUES (28, 8, 15, 70, 19.50, 1365.00, '2026-06-02 17:18:55');

-- ----------------------------
-- Table structure for stock_check
-- ----------------------------
DROP TABLE IF EXISTS `stock_check`;
CREATE TABLE `stock_check`  (
  `check_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '盘点 ID',
  `check_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '盘点单号',
  `warehouse_id` bigint(0) NOT NULL COMMENT '仓库 ID',
  `check_time` datetime(0) NULL DEFAULT NULL COMMENT '盘点日期',
  `status` int(0) NULL DEFAULT 0 COMMENT '状态（0 盘点中/1 已完成/2 已取消）',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `check_user_id` bigint(0) NULL DEFAULT NULL COMMENT '盘点人ID',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`check_id`) USING BTREE,
  UNIQUE INDEX `check_no`(`check_no`) USING BTREE,
  INDEX `idx_warehouse_id`(`warehouse_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存盘点表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock_check
-- ----------------------------
INSERT INTO `stock_check` VALUES (10, 'PD20260602212656', 3, '2026-06-02 21:26:57', 1, '六月盘点', 1, '2026-06-02 21:26:56', '2026-06-02 21:26:56');
INSERT INTO `stock_check` VALUES (13, 'PD20260602214301', 2, '2026-06-02 21:43:01', 1, '5月盘点', 1, '2026-06-02 21:43:01', '2026-06-02 21:43:01');
INSERT INTO `stock_check` VALUES (14, 'PD20260602214804', 1, '2026-06-02 21:48:04', 1, '六月盘点', 1, '2026-06-02 21:48:04', '2026-06-02 21:48:04');
INSERT INTO `stock_check` VALUES (15, 'PD20260602214839', 1, '2026-06-02 21:48:40', 1, '6月盘点', 1, '2026-06-02 21:48:39', '2026-06-02 21:48:39');
INSERT INTO `stock_check` VALUES (16, 'PD20260603091135', 1, '2026-06-03 09:11:36', 0, '', 4, '2026-06-03 09:11:35', '2026-06-03 09:11:35');

-- ----------------------------
-- Table structure for stock_check_item
-- ----------------------------
DROP TABLE IF EXISTS `stock_check_item`;
CREATE TABLE `stock_check_item`  (
  `item_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '明细 ID',
  `check_id` bigint(0) NOT NULL COMMENT '盘点 ID',
  `drug_id` bigint(0) NOT NULL COMMENT '药品 ID',
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批号',
  `system_num` int(0) NULL DEFAULT NULL COMMENT '系统数量',
  `actual_num` int(0) NULL DEFAULT NULL COMMENT '实际数量',
  `diff_num` int(0) NULL DEFAULT NULL COMMENT '差异数量',
  `handle_way` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理方式',
  `handle_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理备注',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`item_id`) USING BTREE,
  INDEX `idx_check_id`(`check_id`) USING BTREE,
  INDEX `idx_drug_id`(`drug_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 47 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存盘点明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock_check_item
-- ----------------------------
INSERT INTO `stock_check_item` VALUES (9, 2, 13, 'PC20260511004', 200, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (10, 2, 15, 'PC20260511005', 20, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (11, 2, 4, 'PC20260513002', 180, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (12, 2, 9, 'PC20260513003', 80, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (13, 2, 8, 'PC20260410001', 15, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (14, 2, 10, 'PC20260315001', 50, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (15, 2, 14, 'PC20260425001', 100, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (16, 2, 1, 'PC20260401002', 10, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (17, 3, 1, NULL, 80, 81, 1, '待处理', '', NULL);
INSERT INTO `stock_check_item` VALUES (18, 3, 2, NULL, 58, 58, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (19, 3, 5, NULL, 50, 50, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (20, 3, 3, NULL, 120, 120, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (21, 3, 6, NULL, 30, 29, -1, '报损', '', NULL);
INSERT INTO `stock_check_item` VALUES (22, 3, 7, NULL, 24, 24, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (23, 3, 11, NULL, 200, 199, -1, '报损', '', NULL);
INSERT INTO `stock_check_item` VALUES (24, 3, 12, NULL, 178, 178, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (25, 3, 1, NULL, 15, 13, -2, '报损', '', NULL);
INSERT INTO `stock_check_item` VALUES (26, 3, 3, NULL, 8, 8, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (27, 4, 1, NULL, 80, 80, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (28, 4, 2, NULL, 58, 58, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (29, 4, 5, NULL, 50, 50, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (30, 4, 3, NULL, 120, 120, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (31, 4, 6, NULL, 30, 30, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (32, 4, 7, NULL, 24, 24, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (33, 4, 11, NULL, 200, 200, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (34, 4, 12, NULL, 178, 178, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (35, 4, 1, NULL, 15, 10, -5, '报损', '', NULL);
INSERT INTO `stock_check_item` VALUES (36, 4, 3, NULL, 8, 8, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (47, 6, 11, NULL, 60, 60, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (48, 6, 12, NULL, 50, 50, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (49, 6, 3, NULL, 30, 30, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (50, 6, 6, NULL, 20, 20, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (51, 6, 6, NULL, 10, 10, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (52, 6, 1, NULL, 80, 80, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (53, 7, 1, NULL, 78, 77, -1, '调整库存', '', NULL);
INSERT INTO `stock_check_item` VALUES (54, 7, 2, NULL, 58, 58, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (55, 7, 5, NULL, 50, 50, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (56, 7, 3, NULL, 120, 120, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (57, 7, 6, NULL, 51, 51, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (58, 7, 7, NULL, 25, 25, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (59, 7, 11, NULL, 195, 195, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (60, 7, 12, NULL, 178, 178, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (61, 7, 1, NULL, 45, 45, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (62, 7, 3, NULL, 8, 8, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (63, 7, 10, NULL, 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (64, 7, 10, NULL, 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (65, 8, 1, NULL, 78, 75, -3, '调整库存', '', NULL);
INSERT INTO `stock_check_item` VALUES (66, 8, 2, NULL, 58, 58, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (67, 8, 5, NULL, 50, 50, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (68, 8, 3, NULL, 120, 120, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (69, 8, 6, NULL, 51, 51, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (70, 8, 7, NULL, 25, 25, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (71, 8, 11, NULL, 195, 195, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (72, 8, 12, NULL, 178, 178, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (73, 8, 1, NULL, 44, 44, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (74, 8, 3, NULL, 8, 8, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (75, 8, 10, NULL, 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (76, 8, 10, NULL, 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (77, 9, 1, NULL, 78, 75, -3, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (78, 9, 2, NULL, 58, 58, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (79, 9, 5, NULL, 50, 50, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (80, 9, 3, NULL, 120, 120, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (81, 9, 6, NULL, 51, 51, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (82, 9, 7, NULL, 25, 25, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (83, 9, 11, NULL, 195, 195, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (84, 9, 12, NULL, 178, 178, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (85, 9, 1, NULL, 41, 41, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (86, 9, 3, NULL, 8, 8, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (87, 9, 10, NULL, 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (88, 9, 10, NULL, 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (89, 10, 11, 'PC20260501001', 60, 60, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (90, 10, 12, 'PC20260501002', 50, 50, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (91, 10, 3, 'PC20260505001', 30, 30, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (92, 10, 6, 'PC20260505002', 41, 38, -3, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (93, 10, 6, 'PC20240615001', 10, 15, 5, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (94, 10, 1, 'PC20260602001', 80, 80, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (113, 13, 13, 'PC20260511004', 200, 195, -5, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (114, 13, 15, 'PC20260511005', 31, 31, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (115, 13, 4, 'PC20260513002', 180, 180, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (116, 13, 9, 'PC20260513003', 80, 80, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (117, 13, 8, 'PC20260410001', 15, 15, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (118, 13, 10, 'PC20260315001', 50, 50, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (119, 13, 14, 'PC20260425001', 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (120, 13, 1, 'PC20260401002', 10, 10, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (121, 13, 9, 'PC20240301001', 12, 12, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (122, 13, 1, 'PC20260602002', 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (123, 14, 1, 'PC20260511001', 75, 75, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (124, 14, 2, 'PC20260511002', 58, 60, 2, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (125, 14, 5, 'PC20260511003', 50, 50, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (126, 14, 3, 'PC20260513001', 120, 120, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (127, 14, 6, 'PC20260401001', 51, 51, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (128, 14, 7, 'PC20260405001', 25, 25, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (129, 14, 11, 'PC20260420001', 195, 195, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (130, 14, 12, 'PC20260420002', 178, 178, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (131, 14, 1, 'PC20250101001', 41, 40, -1, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (132, 14, 3, 'PC20240601001', 8, 8, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (133, 14, 10, 'PC20260602003', 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (134, 14, 10, 'PC20260602004', 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (135, 15, 1, 'PC20260511001', 75, 70, -5, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (136, 15, 2, 'PC20260511002', 60, 55, -5, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (137, 15, 5, 'PC20260511003', 50, 50, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (138, 15, 3, 'PC20260513001', 120, 120, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (139, 15, 6, 'PC20260401001', 51, 51, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (140, 15, 7, 'PC20260405001', 25, 25, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (141, 15, 11, 'PC20260420001', 195, 195, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (142, 15, 12, 'PC20260420002', 178, 178, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (143, 15, 1, 'PC20250101001', 40, 40, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (144, 15, 3, 'PC20240601001', 8, 8, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (145, 15, 10, 'PC20260602003', 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (146, 15, 10, 'PC20260602004', 100, 100, 0, '', '', NULL);
INSERT INTO `stock_check_item` VALUES (147, 16, 1, 'PC20260511001', 70, 70, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (148, 16, 2, 'PC20260511002', 55, 55, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (149, 16, 5, 'PC20260511003', 50, 50, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (150, 16, 3, 'PC20260513001', 120, 120, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (151, 16, 6, 'PC20260401001', 51, 51, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (152, 16, 7, 'PC20260405001', 25, 25, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (153, 16, 11, 'PC20260420001', 195, 195, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (154, 16, 12, 'PC20260420002', 178, 178, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (155, 16, 1, 'PC20250101001', 40, 40, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (156, 16, 3, 'PC20240601001', 8, 8, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (157, 16, 10, 'PC20260602003', 100, 100, 0, NULL, NULL, NULL);
INSERT INTO `stock_check_item` VALUES (158, 16, 10, 'PC20260602004', 100, 100, 0, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for stock_warning
-- ----------------------------
DROP TABLE IF EXISTS `stock_warning`;
CREATE TABLE `stock_warning`  (
  `warning_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '预警 ID',
  `warning_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预警单号',
  `drug_id` bigint(0) NOT NULL COMMENT '药品 ID',
  `warehouse_id` bigint(0) NOT NULL COMMENT '仓库 ID',
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批号',
  `stock_num` int(0) NOT NULL DEFAULT 0 COMMENT '当前库存',
  `warning_type` int(0) NOT NULL DEFAULT 0 COMMENT '预警类型（0 低于最低预警/1 高于最高预警）',
  `warning_type_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '预警类型名称',
  `min_warning_num` int(0) NULL DEFAULT NULL COMMENT '最低预警值',
  `max_warning_num` int(0) NULL DEFAULT NULL COMMENT '最高预警值',
  `days_to_expiry` int(0) NULL DEFAULT NULL COMMENT '距过期天数(临期预警用)',
  `warning_level` int(0) NULL DEFAULT 0 COMMENT '预警级别（0 一般/1 重要/2 紧急）',
  `handle_status` int(0) NULL DEFAULT 0 COMMENT '处理状态（0 未处理/1 已处理）',
  `handle_user_id` bigint(0) NULL DEFAULT NULL COMMENT '处理人 ID',
  `assign_user_id` bigint(0) NULL DEFAULT NULL COMMENT '指派处理人ID',
  `assign_time` datetime(0) NULL DEFAULT NULL COMMENT '指派时间',
  `handle_time` datetime(0) NULL DEFAULT NULL COMMENT '处理时间',
  `handle_remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理备注',
  `notify_status` int(0) NULL DEFAULT 0 COMMENT '通知状态(0未通知/1已通知)',
  `notify_channels` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '通知渠道(逗号分隔: system,email,sms)',
  `notify_time` datetime(0) NULL DEFAULT NULL COMMENT '通知时间',
  `suggestion` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理建议',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除/1已删除）',
  PRIMARY KEY (`warning_id`) USING BTREE,
  UNIQUE INDEX `warning_no`(`warning_no`) USING BTREE,
  INDEX `idx_drug_id`(`drug_id`) USING BTREE,
  INDEX `idx_warehouse_id`(`warehouse_id`) USING BTREE,
  INDEX `idx_handle_status`(`handle_status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 181 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存预警记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of stock_warning
-- ----------------------------
INSERT INTO `stock_warning` VALUES (300, 'YJ20260603085652509232', 1, 1, 'PC20250101001', 40, 2, '临期预警', NULL, NULL, 50, 1, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20250101001]距过期仅剩50天，建议优先出库', '2026-06-03 08:56:53', '2026-06-03 08:56:52', 0);
INSERT INTO `stock_warning` VALUES (301, 'YJ20260603085652517236', 3, 1, 'PC20240601001', 8, 3, '过期预警', NULL, NULL, -14, 2, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20240601001]已过期14天，建议立即下架处理', '2026-06-03 08:56:53', '2026-06-03 08:56:52', 0);
INSERT INTO `stock_warning` VALUES (302, 'YJ20260603085652521132', 6, 3, 'PC20240615001', 15, 2, '临期预警', NULL, NULL, 35, 1, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20240615001]距过期仅剩35天，建议优先出库', '2026-06-03 08:56:53', '2026-06-03 08:56:52', 0);
INSERT INTO `stock_warning` VALUES (303, 'YJ20260603085652527785', 9, 2, 'PC20240301001', 12, 2, '临期预警', NULL, NULL, 15, 2, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20240301001]距过期仅剩15天，建议优先出库', '2026-06-03 08:56:53', '2026-06-03 08:56:52', 0);
INSERT INTO `stock_warning` VALUES (304, 'YJ20260603085653878724', 8, 2, 'PC20260410001', 15, 4, '滞销预警', NULL, NULL, NULL, 0, 1, 1, NULL, NULL, '2026-06-03 08:58:08', '已处理', 0, NULL, NULL, '批次[PC20260410001]已90天无出入库记录，建议评估是否继续采购', '2026-06-03 08:56:54', '2026-06-03 08:56:53', 0);
INSERT INTO `stock_warning` VALUES (305, 'YJ20260603085653882660', 10, 2, 'PC20260315001', 50, 4, '滞销预警', NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20260315001]已90天无出入库记录，建议评估是否继续采购', '2026-06-03 08:56:54', '2026-06-03 08:56:53', 0);
INSERT INTO `stock_warning` VALUES (306, 'YJ20260603085653890282', 14, 2, 'PC20260425001', 100, 4, '滞销预警', NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20260425001]已90天无出入库记录，建议评估是否继续采购', '2026-06-03 08:56:54', '2026-06-03 08:56:53', 0);
INSERT INTO `stock_warning` VALUES (307, 'YJ20260603085653897772', 1, 2, 'PC20260401002', 10, 4, '滞销预警', NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20260401002]已90天无出入库记录，建议评估是否继续采购', '2026-06-03 08:56:54', '2026-06-03 08:56:53', 0);
INSERT INTO `stock_warning` VALUES (308, 'YJ20260603085653901276', 11, 3, 'PC20260501001', 60, 4, '滞销预警', NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20260501001]已90天无出入库记录，建议评估是否继续采购', '2026-06-03 08:56:54', '2026-06-03 08:56:53', 0);
INSERT INTO `stock_warning` VALUES (309, 'YJ20260603085653907394', 12, 3, 'PC20260501002', 50, 4, '滞销预警', NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20260501002]已90天无出入库记录，建议评估是否继续采购', '2026-06-03 08:56:54', '2026-06-03 08:56:53', 0);
INSERT INTO `stock_warning` VALUES (310, 'YJ20260603085653913357', 3, 3, 'PC20260505001', 30, 4, '滞销预警', NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20260505001]已90天无出入库记录，建议评估是否继续采购', '2026-06-03 08:56:54', '2026-06-03 08:56:53', 0);
INSERT INTO `stock_warning` VALUES (311, 'YJ20260603085653919825', 1, 1, 'PC20250101001', 40, 4, '滞销预警', NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20250101001]已90天无出入库记录，建议评估是否继续采购', '2026-06-03 08:56:54', '2026-06-03 08:56:53', 0);
INSERT INTO `stock_warning` VALUES (312, 'YJ20260603085653925631', 3, 1, 'PC20240601001', 8, 4, '滞销预警', NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20240601001]已90天无出入库记录，建议评估是否继续采购', '2026-06-03 08:56:54', '2026-06-03 08:56:53', 0);
INSERT INTO `stock_warning` VALUES (313, 'YJ20260603085653930614', 6, 3, 'PC20240615001', 15, 4, '滞销预警', NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20240615001]已90天无出入库记录，建议评估是否继续采购', '2026-06-03 08:56:54', '2026-06-03 08:56:53', 0);
INSERT INTO `stock_warning` VALUES (314, 'YJ20260603085653934640', 9, 2, 'PC20240301001', 12, 4, '滞销预警', NULL, NULL, NULL, 0, 0, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, '批次[PC20240301001]已90天无出入库记录，建议评估是否继续采购', '2026-06-03 08:56:54', '2026-06-03 08:56:53', 0);

-- ----------------------------
-- Table structure for supplier_info
-- ----------------------------
DROP TABLE IF EXISTS `supplier_info`;
CREATE TABLE `supplier_info`  (
  `supplier_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '供应商 ID',
  `supplier_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '供应商编码',
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '供应商名称',
  `contact_person` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `license_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '营业执照号',
  `status` int(0) NULL DEFAULT 1 COMMENT '状态（0 停用/1 启用）',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除/1已删除）',
  PRIMARY KEY (`supplier_id`) USING BTREE,
  UNIQUE INDEX `supplier_code`(`supplier_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '供应商信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of supplier_info
-- ----------------------------
INSERT INTO `supplier_info` VALUES (1, 'GYS001', '国药集团医药有限公司', '张建国', '010-65529988', '北京市朝阳区建国路128号', '京药经营20250001', 1, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `supplier_info` VALUES (2, 'GYS002', '华润医药商业集团有限公司', '李明辉', '010-65158866', '北京市东城区安定门外大街56号', '京药经营20250002', 1, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `supplier_info` VALUES (3, 'GYS003', '上海医药分销控股有限公司', '王志强', '021-63298877', '上海市黄浦区福州路666号', '沪药经营20250003', 1, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `supplier_info` VALUES (4, 'GYS004', '广州医药集团有限公司', '陈伟东', '020-81886655', '广州市荔湾区沙面北街45号', '粤药经营20250004', 1, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `supplier_info` VALUES (5, 'GYS005', '南京医药股份有限公司', '刘家明', '025-83779922', '南京市鼓楼区中山北路288号', '苏药经营20250005', 1, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `supplier_info` VALUES (6, 'GYS006', '北京云南白药有限公司', '李杰请', '101-16548912', '北京市朝阳区人民街道12号', '京药经营20250006', 1, '2026-06-02 15:21:41', '2026-06-02 15:21:41', 0);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `menu_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '菜单 ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(0) NULL DEFAULT 0 COMMENT '父菜单 ID',
  `path` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单路径',
  `component` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `sort` int(0) NULL DEFAULT 0 COMMENT '排序',
  `status` int(0) NULL DEFAULT 1 COMMENT '状态（0 禁用/1 启用）',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统菜单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '首页', 0, 'dashboard', 'Dashboard.vue', 'HomeFilled', 1, 1, '2026-04-11 12:17:09', '2026-05-24 13:44:10');
INSERT INTO `sys_menu` VALUES (2, '药品管理', 0, 'drug', 'drug/DrugList.vue', 'Aim', 2, 1, '2026-04-11 12:17:09', '2026-05-24 13:44:10');
INSERT INTO `sys_menu` VALUES (3, '供应商管理', 0, 'supplier', 'supplier/SupplierList.vue', 'OfficeBuilding', 3, 1, '2026-04-11 12:17:09', '2026-05-24 13:44:10');
INSERT INTO `sys_menu` VALUES (4, '采购管理', 0, 'purchase', 'purchase/PurchaseOrderList.vue', 'ShoppingCart', 4, 1, '2026-04-11 12:17:09', '2026-05-24 13:44:10');
INSERT INTO `sys_menu` VALUES (5, '采购审核', 0, 'purchase-audit', 'purchase/PurchaseAuditList.vue', 'Check', 5, 1, '2026-04-11 12:17:09', '2026-05-24 13:44:10');
INSERT INTO `sys_menu` VALUES (6, '库存管理', 0, 'stock', 'stock/StockList.vue', 'Box', 6, 1, '2026-04-11 12:17:09', '2026-05-24 13:44:10');
INSERT INTO `sys_menu` VALUES (7, '入库管理', 0, 'drug-in', 'inout/DrugInList.vue', 'Bottom', 7, 1, '2026-04-11 12:17:09', '2026-05-24 13:44:10');
INSERT INTO `sys_menu` VALUES (8, '出库管理', 0, 'drug-out', 'inout/DrugOutList.vue', 'Top', 8, 1, '2026-04-11 12:17:09', '2026-05-24 13:44:10');
INSERT INTO `sys_menu` VALUES (10, '用户管理', 0, 'user', 'system/UserList.vue', 'User', 12, 1, '2026-04-11 12:17:09', '2026-05-24 16:20:21');
INSERT INTO `sys_menu` VALUES (11, '角色管理', 0, 'role', 'system/RoleList.vue', 'Setting', 13, 1, '2026-04-11 12:17:09', '2026-05-24 16:20:21');
INSERT INTO `sys_menu` VALUES (12, '仓库管理', 0, 'warehouse', 'warehouse/WarehouseList.vue', 'House', 9, 1, '2026-05-24 13:44:10', '2026-05-24 13:44:10');
INSERT INTO `sys_menu` VALUES (13, '库存盘点', 0, 'stock-check', 'stock/StockCheckList.vue', 'List', 10, 1, '2026-05-24 13:44:10', '2026-05-24 16:20:21');
INSERT INTO `sys_menu` VALUES (14, '预警中心', 0, 'warning-center', 'stock/WarningCenter.vue', 'Bell', 11, 1, '2026-05-24 14:49:58', '2026-05-24 16:20:21');

-- ----------------------------
-- Table structure for sys_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`  (
  `log_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '日志 ID',
  `user_id` bigint(0) NULL DEFAULT NULL COMMENT '操作人 ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `operation` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作类型（如：新增/修改/删除/审核/入库/出库）',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作模块（如：药品管理/采购管理/库存管理）',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作描述',
  `method` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '请求方法',
  `params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '请求参数',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作 IP',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_module`(`module`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `role_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '角色 ID',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色描述',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除/1已删除）',
  PRIMARY KEY (`role_id`) USING BTREE,
  UNIQUE INDEX `role_code`(`role_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '系统管理员', 'ADMIN', '拥有系统所有权限', '2026-04-11 12:17:09', '2026-04-11 12:17:09', 0);
INSERT INTO `sys_role` VALUES (2, '采购审核员', 'AUDITOR', '负责采购订单审核', '2026-04-11 12:17:09', '2026-04-11 12:17:09', 0);
INSERT INTO `sys_role` VALUES (3, '采购员', 'PURCHASER', '负责采购订单创建', '2026-04-11 12:17:09', '2026-06-02 21:39:17', 0);
INSERT INTO `sys_role` VALUES (4, '库管员', 'WAREHOUSE', '负责药品出入库和库存管理', '2026-04-11 12:17:09', '2026-04-11 12:17:09', 0);

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `role_id` bigint(0) NOT NULL COMMENT '角色 ID',
  `menu_id` bigint(0) NOT NULL COMMENT '菜单 ID',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_menu`(`role_id`, `menu_id`) USING BTREE COMMENT '角色菜单唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 251 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (224, 1, 1, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (225, 1, 2, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (226, 1, 3, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (227, 1, 4, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (228, 1, 5, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (229, 1, 6, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (230, 1, 7, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (231, 1, 8, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (232, 1, 12, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (233, 1, 10, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (234, 1, 11, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (235, 1, 13, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (236, 1, 14, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (237, 3, 1, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (238, 3, 2, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (239, 3, 3, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (240, 3, 4, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (244, 4, 1, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (245, 4, 6, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (246, 4, 7, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (247, 4, 8, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (248, 4, 12, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (249, 4, 13, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (250, 4, 14, '2026-05-24 16:20:21');
INSERT INTO `sys_role_menu` VALUES (251, 2, 1, '2026-06-02 15:31:09');
INSERT INTO `sys_role_menu` VALUES (252, 2, 5, '2026-06-02 15:31:09');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `user_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（加密存储）',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `role_id` bigint(0) NULL DEFAULT NULL COMMENT '角色 ID',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `status` int(0) NULL DEFAULT 1 COMMENT '状态（0 禁用/1 启用）',
  `last_login_time` datetime(0) NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除/1已删除）',
  PRIMARY KEY (`user_id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$154x.iqMmoZ92xwVLRsy1uoETK4tKyghLQzPjO/cM2WObzMY3t.Zm', '系统管理员', 1, '13800138000', 'admin@hospital.com', 1, '2026-06-03 09:05:21', '2026-04-11 12:17:09', '2026-04-11 20:10:26', 0);
INSERT INTO `sys_user` VALUES (2, 'shenhe', '$2a$10$PAeEiFlIYS0r6FOG.JyJVO5EzFTb5eIrsSFExtEhR8TwX8X/.UKDa', '李审核', 2, '13800138001', 'auditor@hospital.com', 1, '2026-06-03 09:08:23', '2026-04-11 12:17:09', '2026-05-24 13:44:09', 0);
INSERT INTO `sys_user` VALUES (3, 'caigou', '$2a$10$G2rfZa7T2AyfmWGwXyoGz.pctYywtgO3GrmneOqvSx8uz2P9NAexy', '王采购', 3, '13800138002', 'purchaser@hospital.com', 1, '2026-06-03 09:05:46', '2026-04-11 12:17:09', '2026-04-11 13:30:27', 0);
INSERT INTO `sys_user` VALUES (4, 'kuguan', '$2a$10$hmu9bUEK1hyKZ1o5EKgTHe9roeevVrHOuxiryVeQpObxXkM5IClgm', '张库管', 4, '13800138003', 'warehouse@hospital.com', 1, '2026-06-03 09:10:47', '2026-04-11 12:17:09', '2026-06-02 20:59:49', 0);
INSERT INTO `sys_user` VALUES (9, 'caigou1', '$2a$10$bgUpODvNBcwjMX0bdQCsHuDgS7dlWCtCtDY2EuWfV64djf3QOhcDS', 'yuchen', 3, '15374324849', '15374324849@126.com', 1, '2026-06-03 08:12:32', '2026-06-03 08:12:24', '2026-06-03 08:12:24', 0);

-- ----------------------------
-- Table structure for system_notice
-- ----------------------------
DROP TABLE IF EXISTS `system_notice`;
CREATE TABLE `system_notice`  (
  `notice_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '公告 ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告内容',
  `create_user_id` bigint(0) NULL DEFAULT NULL COMMENT '创建人 ID',
  `create_user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人姓名',
  `status` int(0) NULL DEFAULT 1 COMMENT '状态（0-停用 1-启用）',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除/1已删除）',
  PRIMARY KEY (`notice_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统公告表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of system_notice
-- ----------------------------
INSERT INTO `system_notice` VALUES (1, '关于5月份药品盘点安排的通知', '各科室注意：5月份药品盘点将于5月22日进行，请各药房提前做好准备，暂停非紧急出库操作。盘点期间如有紧急用药需求，请联系药库张库管。', 1, '系统管理员', 1, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `system_notice` VALUES (2, '新增供应商-南京医药股份有限公司', '经资质审核，新增供应商\"南京医药股份有限公司\"（GYS005），主要供应维生素类和输液类药品，请采购员关注。', 1, '系统管理员', 1, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `system_notice` VALUES (3, '夏季药品储存注意事项', '夏季高温来临，请各药房注意：1.需要冷藏的药品严格控制在2-8°C；2.避光药品注意遮光保存；3.定期检查药品有效期，近效期药品优先使用。', 1, '系统管理员', 1, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);

-- ----------------------------
-- Table structure for warehouse_info
-- ----------------------------
DROP TABLE IF EXISTS `warehouse_info`;
CREATE TABLE `warehouse_info`  (
  `warehouse_id` bigint(0) NOT NULL AUTO_INCREMENT COMMENT '仓库 ID',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '仓库名称',
  `warehouse_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '仓库编码',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库地址',
  `manager` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '管理员',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `status` int(0) NULL DEFAULT 1 COMMENT '状态（0 停用/1 启用）',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0未删除/1已删除）',
  PRIMARY KEY (`warehouse_id`) USING BTREE,
  UNIQUE INDEX `warehouse_code`(`warehouse_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '仓库信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of warehouse_info
-- ----------------------------
INSERT INTO `warehouse_info` VALUES (1, '主仓库', 'WH001', '医院地下一层A区', '张库管', '0755-86001234', 1, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `warehouse_info` VALUES (2, '门诊药房', 'WH002', '门诊楼一层西侧', '赵药士', '0755-86001235', 1, '2026-05-24 16:30:46', '2026-05-24 16:30:46', 0);
INSERT INTO `warehouse_info` VALUES (3, '急诊药房', 'WH003', '急诊楼一层东侧', '孙药师', '0755-86001236', 1, '2026-05-24 16:30:46', '2026-06-02 15:50:23', 0);

SET FOREIGN_KEY_CHECKS = 1;
