USE ai4note;

ALTER TABLE material
  ADD CONSTRAINT ck_material_is_key CHECK (is_key IN (0, 1));

ALTER TABLE knowledge_item
  ADD CONSTRAINT ck_importance_level CHECK (importance_level BETWEEN 1 AND 5);

ALTER TABLE material_file
  ADD CONSTRAINT ck_file_size CHECK (file_size >= 0);

ALTER TABLE course_relation
  ADD CONSTRAINT ck_course_relation_type CHECK (relation_type IN ('PREREQUISITE', 'RELATED', 'FOLLOW_UP'));

ALTER TABLE ai_provider_config
  ADD CONSTRAINT ck_ai_provider_enabled CHECK (enabled IN (0, 1));

ALTER TABLE review_generation_profile
  ADD CONSTRAINT ck_review_include_prerequisites CHECK (include_prerequisites IN (0, 1));

ALTER TABLE ai_generation_task
  ADD CONSTRAINT ck_ai_task_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELED'));

ALTER TABLE teacher_profile
  ADD CONSTRAINT ck_teacher_generated_by_ai CHECK (generated_by_ai IN (0, 1));

ALTER TABLE teacher_profile
  ADD CONSTRAINT ck_teacher_analysis_status CHECK (analysis_status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'MANUAL_REVIEWED'));

ALTER TABLE teacher_profile
  ADD CONSTRAINT ck_teacher_confidence CHECK (confidence_score IS NULL OR confidence_score BETWEEN 0 AND 100);

ALTER TABLE teacher_profile_evidence
  ADD CONSTRAINT ck_teacher_evidence_confidence CHECK (confidence_score IS NULL OR confidence_score BETWEEN 0 AND 100);

ALTER TABLE user_knowledge_status
  ADD CONSTRAINT ck_mastery_status CHECK (mastery_status IN ('UNKNOWN', 'LEARNING', 'MASTERED'));

ALTER TABLE user_knowledge_status
  ADD CONSTRAINT ck_mastery_score CHECK (mastery_score IS NULL OR mastery_score BETWEEN 0 AND 100);

ALTER TABLE exam_question_knowledge_map
  ADD CONSTRAINT ck_question_map_confidence CHECK (confidence_score IS NULL OR confidence_score BETWEEN 0 AND 100);

ALTER TABLE knowledge_gap_report
  ADD CONSTRAINT ck_gap_include_prerequisites CHECK (include_prerequisites IN (0, 1));

ALTER TABLE knowledge_gap_item
  ADD CONSTRAINT ck_gap_type CHECK (gap_type IN ('WEAK_MASTERY', 'HIGH_FREQUENCY', 'PREREQUISITE_GAP', 'UNASSESSED'));

ALTER TABLE knowledge_gap_item
  ADD CONSTRAINT ck_gap_severity CHECK (severity_level BETWEEN 1 AND 5);
