-- 005: AI 生成答案溯源字段

ALTER TABLE exam_question
  ADD COLUMN answer_source VARCHAR(512) COMMENT '答案来源描述，如 SLIDE:课件名 P3',
  ADD COLUMN answer_source_page INT COMMENT '答案来源页码';
