USE ai4note;

ALTER TABLE teacher_profile
  ADD COLUMN generated_by_ai TINYINT NOT NULL DEFAULT 1 AFTER teacher_name,
  ADD COLUMN analysis_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' AFTER generated_by_ai,
  ADD COLUMN confidence_score DECIMAL(5,2) AFTER analysis_status,
  ADD COLUMN last_analyzed_time DATETIME AFTER source_summary;

CREATE TABLE IF NOT EXISTS teacher_profile_evidence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  teacher_profile_id BIGINT NOT NULL,
  material_id BIGINT NOT NULL,
  evidence_type VARCHAR(32) NOT NULL,
  evidence_summary TEXT,
  source_page INT,
  confidence_score DECIMAL(5,2),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_teacher_evidence_profile FOREIGN KEY (teacher_profile_id) REFERENCES teacher_profile(id),
  CONSTRAINT fk_teacher_evidence_material FOREIGN KEY (material_id) REFERENCES material(id)
);

ALTER TABLE teacher_profile
  ADD CONSTRAINT ck_teacher_generated_by_ai CHECK (generated_by_ai IN (0, 1));

ALTER TABLE teacher_profile
  ADD CONSTRAINT ck_teacher_analysis_status CHECK (analysis_status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED', 'MANUAL_REVIEWED'));

ALTER TABLE teacher_profile
  ADD CONSTRAINT ck_teacher_confidence CHECK (confidence_score IS NULL OR confidence_score BETWEEN 0 AND 100);

ALTER TABLE teacher_profile_evidence
  ADD CONSTRAINT ck_teacher_evidence_confidence CHECK (confidence_score IS NULL OR confidence_score BETWEEN 0 AND 100);
