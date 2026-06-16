USE ai4note;

ALTER TABLE course_relation
  ADD CONSTRAINT ck_course_relation_type CHECK (relation_type IN ('PREREQUISITE', 'RELATED', 'FOLLOW_UP'));

ALTER TABLE ai_provider_config
  ADD CONSTRAINT ck_ai_provider_enabled CHECK (enabled IN (0, 1));

ALTER TABLE review_generation_profile
  ADD CONSTRAINT ck_review_include_prerequisites CHECK (include_prerequisites IN (0, 1));

ALTER TABLE ai_generation_task
  ADD CONSTRAINT ck_ai_task_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELED'));
