-- 公告增加优先级和有效期
ALTER TABLE sys_announcement
    ADD COLUMN priority TINYINT DEFAULT 0 COMMENT '0普通 1重要 2紧急',
    ADD COLUMN expire_time DATETIME DEFAULT NULL COMMENT '公告有效期，NULL表示永久有效';

-- 帖子增加定时发布时间
ALTER TABLE sys_post
    ADD COLUMN scheduled_time DATETIME DEFAULT NULL COMMENT '定时发布时间，NULL表示立即发布';

-- 活动增加结束时间
ALTER TABLE sys_activity
    ADD COLUMN end_time DATETIME DEFAULT NULL COMMENT '活动结束时间，NULL则以开始时间判断是否结束';

-- 学生表增加通知已读时间
ALTER TABLE sys_student
    ADD COLUMN last_notif_read DATETIME DEFAULT NULL COMMENT '上次查看通知的时间';
