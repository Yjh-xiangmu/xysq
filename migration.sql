-- ===================================================
-- XYSQ 需求迁移脚本
-- 执行前请备份数据库！
-- ===================================================

-- 1. sys_community 新增字段
ALTER TABLE `sys_community`
    ADD COLUMN `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0待审核 1正常 2已拒绝' AFTER `create_time`,
    ADD COLUMN `creator_student_id` int NULL DEFAULT NULL COMMENT '创建者学生ID（学生创群时有值，平台直接建的为NULL）' AFTER `status`;

-- 2. sys_admin 新增字段（社群管理员个人信息 + 学生绑定）
ALTER TABLE `sys_admin`
    ADD COLUMN `nickname` varchar(50) NULL DEFAULT NULL COMMENT '昵称' AFTER `role`,
    ADD COLUMN `avatar` varchar(255) NULL DEFAULT NULL COMMENT '头像' AFTER `nickname`,
    ADD COLUMN `phone` varchar(20) NULL DEFAULT NULL COMMENT '手机号' AFTER `avatar`,
    ADD COLUMN `email` varchar(100) NULL DEFAULT NULL COMMENT '邮箱' AFTER `phone`,
    ADD COLUMN `student_id` int NULL DEFAULT NULL COMMENT '关联学生ID（学生创群审批通过后自动绑定）' AFTER `email`;

-- 3. sys_activity_sign 新增字段（报名需填写信息 + 审核状态）
ALTER TABLE `sys_activity_sign`
    ADD COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0待审核 1已通过 2已拒绝' AFTER `sign_time`,
    ADD COLUMN `real_name` varchar(50) NULL DEFAULT NULL COMMENT '报名填写的真实姓名' AFTER `status`,
    ADD COLUMN `phone` varchar(20) NULL DEFAULT NULL COMMENT '报名填写的联系电话' AFTER `real_name`,
    ADD COLUMN `remark` varchar(255) NULL DEFAULT NULL COMMENT '报名备注' AFTER `phone`;

-- 4. 新建 sys_follow 关注关系表
CREATE TABLE IF NOT EXISTS `sys_follow` (
    `id` int NOT NULL AUTO_INCREMENT,
    `follower_id` int NOT NULL COMMENT '关注者学生ID',
    `followed_id` int NOT NULL COMMENT '被关注者学生ID',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_follow` (`follower_id`, `followed_id`) USING BTREE COMMENT '防止重复关注'
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户关注关系表' ROW_FORMAT = Dynamic;

-- 5. 新建 sys_message 私信消息表
CREATE TABLE IF NOT EXISTS `sys_message` (
    `id` int NOT NULL AUTO_INCREMENT,
    `from_student_id` int NOT NULL COMMENT '发送者学生ID',
    `to_student_id` int NOT NULL COMMENT '接收者学生ID',
    `content` text NULL COMMENT '消息文字内容',
    `image_url` varchar(255) NULL DEFAULT NULL COMMENT '图片消息URL',
    `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读: 0未读 1已读',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_conversation` (`from_student_id`, `to_student_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '私信消息表' ROW_FORMAT = Dynamic;

-- 6. 新建 sys_report 举报记录表
CREATE TABLE IF NOT EXISTS `sys_report` (
    `id` int NOT NULL AUTO_INCREMENT,
    `reporter_id` int NOT NULL COMMENT '举报人学生ID',
    `community_id` int NOT NULL COMMENT '被举报社群ID',
    `reason` text NOT NULL COMMENT '举报原因',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0待审核 1已处理',
    `handle_result` varchar(255) NULL DEFAULT NULL COMMENT '处理结果备注',
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社群举报记录表' ROW_FORMAT = Dynamic;
