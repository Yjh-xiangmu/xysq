/*
 Navicat Premium Data Transfer

 Source Server         : yjh
 Source Server Type    : MySQL
 Source Server Version : 80026
 Source Host           : localhost:3306
 Source Schema         : xysq

 Target Server Type    : MySQL
 Target Server Version : 80026
 File Encoding         : 65001

 Date: 05/04/2026 15:53:48
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_activity
-- ----------------------------
DROP TABLE IF EXISTS `sys_activity`;
CREATE TABLE `sys_activity`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `community_id` int NOT NULL COMMENT '所属社群ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动内容/要求',
  `event_time` datetime(0) NOT NULL COMMENT '活动时间',
  `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '活动地点',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社群活动表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_activity
-- ----------------------------
INSERT INTO `sys_activity` VALUES (1, 1, '吉他教学分享', '测试的', '2026-04-05 15:14:00', '综合楼402', '2026-04-05 14:15:40');

-- ----------------------------
-- Table structure for sys_activity_sign
-- ----------------------------
DROP TABLE IF EXISTS `sys_activity_sign`;
CREATE TABLE `sys_activity_sign`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `activity_id` int NOT NULL COMMENT '活动ID',
  `student_id` int NOT NULL COMMENT '报名学生ID',
  `sign_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '报名时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_activity_student`(`activity_id`, `student_id`) USING BTREE COMMENT '防止重复报名'
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '活动报名记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_activity_sign
-- ----------------------------
INSERT INTO `sys_activity_sign` VALUES (1, 1, 1, '2026-04-05 14:16:04');

-- ----------------------------
-- Table structure for sys_admin
-- ----------------------------
DROP TABLE IF EXISTS `sys_admin`;
CREATE TABLE `sys_admin`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `role` tinyint NOT NULL DEFAULT 1 COMMENT '角色：1平台管理员 2社群管理员',
  `community_id` int NULL DEFAULT NULL COMMENT '管理的社群ID(仅角色为2的社群管理员有值)',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_admin
-- ----------------------------
INSERT INTO `sys_admin` VALUES (1, 'admin', '123456', 1, NULL);
INSERT INTO `sys_admin` VALUES (2, 'shezhang', '123456', 2, 1);

-- ----------------------------
-- Table structure for sys_announcement
-- ----------------------------
DROP TABLE IF EXISTS `sys_announcement`;
CREATE TABLE `sys_announcement`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '公告内容',
  `admin_id` int NOT NULL COMMENT '发布管理员ID',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '全站公告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_announcement
-- ----------------------------

-- ----------------------------
-- Table structure for sys_comment
-- ----------------------------
DROP TABLE IF EXISTS `sys_comment`;
CREATE TABLE `sys_comment`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `post_id` int NOT NULL COMMENT '帖子ID',
  `student_id` int NOT NULL COMMENT '评论者学生ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '评论时间',
  `parent_id` int NULL DEFAULT 0 COMMENT '父评论ID(0为顶级评论)',
  `reply_to_student_id` int NULL DEFAULT NULL COMMENT '回复的目标学生ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_comment
-- ----------------------------
INSERT INTO `sys_comment` VALUES (1, 1, 1, '我也哈哈', '2026-04-05 13:57:09', 0, NULL);
INSERT INTO `sys_comment` VALUES (2, 1, 1, '那我也哈哈', '2026-04-05 14:03:31', 1, 1);
INSERT INTO `sys_comment` VALUES (3, 1, 1, '那我继续哈哈', '2026-04-05 14:03:37', 1, 1);

-- ----------------------------
-- Table structure for sys_community
-- ----------------------------
DROP TABLE IF EXISTS `sys_community`;
CREATE TABLE `sys_community`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '社群名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '社群简介',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '社群头像',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '社群分类',
  `is_recommended` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否首页推荐(0否 1是)',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社群信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_community
-- ----------------------------
INSERT INTO `sys_community` VALUES (1, '吉他指弹交流社', '无论你是扫弦萌新还是指弹大神，这里都有你的舞台！', 'https://api.dicebear.com/7.x/bottts/svg?seed=music', '音乐', 1, '2026-04-05 13:18:39');
INSERT INTO `sys_community` VALUES (2, 'AI大模型探索者', '讨论LangChain、Prompt工程，跟上AIGC时代浪潮。', 'https://api.dicebear.com/7.x/bottts/svg?seed=ai', '科技', 0, '2026-04-05 13:18:39');
INSERT INTO `sys_community` VALUES (3, '光影摄影联盟', '用镜头记录校园，分享构图与调色的艺术。', 'https://api.dicebear.com/7.x/bottts/svg?seed=camera', '摄影', 0, '2026-04-05 13:18:39');
INSERT INTO `sys_community` VALUES (4, '王者荣耀开黑群', '无兄弟不电竞，校园赛组队滴滴！', 'https://api.dicebear.com/7.x/bottts/svg?seed=game', '游戏', 0, '2026-04-05 13:18:39');

-- ----------------------------
-- Table structure for sys_community_member
-- ----------------------------
DROP TABLE IF EXISTS `sys_community_member`;
CREATE TABLE `sys_community_member`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `community_id` int NOT NULL COMMENT '社群ID',
  `student_id` int NOT NULL COMMENT '学生ID',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态: 0待审核, 1已加入, 2已拒绝',
  `join_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '加入时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社群成员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_community_member
-- ----------------------------
INSERT INTO `sys_community_member` VALUES (1, 2, 1, 0, '2026-04-05 13:24:23');
INSERT INTO `sys_community_member` VALUES (2, 3, 1, 0, '2026-04-05 13:24:27');
INSERT INTO `sys_community_member` VALUES (3, 4, 1, 0, '2026-04-05 13:24:33');
INSERT INTO `sys_community_member` VALUES (4, 1, 1, 1, '2026-04-05 13:24:36');
INSERT INTO `sys_community_member` VALUES (5, 2, 2, 0, '2026-04-05 13:47:49');

-- ----------------------------
-- Table structure for sys_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_post`;
CREATE TABLE `sys_post`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `community_id` int NOT NULL COMMENT '所属社群ID',
  `student_id` int NOT NULL COMMENT '发布者学生ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '帖子内容',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态: 0隐藏 1正常',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '发布时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_post
-- ----------------------------
INSERT INTO `sys_post` VALUES (1, 1, 1, '哈哈', 1, '2026-04-05 13:50:29');

-- ----------------------------
-- Table structure for sys_post_like
-- ----------------------------
DROP TABLE IF EXISTS `sys_post_like`;
CREATE TABLE `sys_post_like`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `post_id` int NOT NULL COMMENT '帖子ID',
  `student_id` int NOT NULL COMMENT '点赞学生ID',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '点赞时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_student`(`post_id`, `student_id`) USING BTREE COMMENT '防止重复点赞'
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子点赞表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_post_like
-- ----------------------------
INSERT INTO `sys_post_like` VALUES (1, 1, 1, '2026-04-05 15:03:19');

-- ----------------------------
-- Table structure for sys_student
-- ----------------------------
DROP TABLE IF EXISTS `sys_student`;
CREATE TABLE `sys_student`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `student_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '学号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_no`(`student_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '学生表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_student
-- ----------------------------
INSERT INTO `sys_student` VALUES (1, '1008611', '123456', '摆烂奥特曼');
INSERT INTO `sys_student` VALUES (2, '10086', '123456', '测试');

SET FOREIGN_KEY_CHECKS = 1;
