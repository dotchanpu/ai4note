USE ai4note;

CREATE TABLE IF NOT EXISTS user_knowledge_status (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  knowledge_item_id BIGINT NOT NULL,
  mastery_status VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN',
  mastery_score DECIMAL(5,2),
  last_review_time DATETIME,
  note TEXT,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_uks_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_uks_knowledge FOREIGN KEY (knowledge_item_id) REFERENCES knowledge_item(id),
  CONSTRAINT uk_user_knowledge UNIQUE (user_id, knowledge_item_id)
);

CREATE TABLE IF NOT EXISTS exam_question (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  material_id BIGINT,
  chapter_id BIGINT,
  question_no VARCHAR(64),
  question_type VARCHAR(32),
  question_text LONGTEXT NOT NULL,
  answer_text LONGTEXT,
  difficulty_level VARCHAR(32),
  score DECIMAL(6,2),
  exam_year INT,
  source_page INT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_exam_question_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_exam_question_material FOREIGN KEY (material_id) REFERENCES material(id),
  CONSTRAINT fk_exam_question_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id)
);

CREATE TABLE IF NOT EXISTS exam_question_knowledge_map (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  exam_question_id BIGINT NOT NULL,
  knowledge_item_id BIGINT NOT NULL,
  match_source VARCHAR(32) NOT NULL DEFAULT 'AI',
  confidence_score DECIMAL(5,2),
  reason TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_eqkm_question FOREIGN KEY (exam_question_id) REFERENCES exam_question(id),
  CONSTRAINT fk_eqkm_knowledge FOREIGN KEY (knowledge_item_id) REFERENCES knowledge_item(id),
  CONSTRAINT uk_question_knowledge UNIQUE (exam_question_id, knowledge_item_id)
);

CREATE TABLE IF NOT EXISTS knowledge_gap_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  include_prerequisites TINYINT NOT NULL DEFAULT 1,
  report_name VARCHAR(128) NOT NULL,
  summary TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_gap_report_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_gap_report_course FOREIGN KEY (course_id) REFERENCES course(id)
);

CREATE TABLE IF NOT EXISTS knowledge_gap_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  report_id BIGINT NOT NULL,
  knowledge_item_id BIGINT NOT NULL,
  source_course_id BIGINT NOT NULL,
  related_course_relation_id BIGINT,
  gap_type VARCHAR(32) NOT NULL,
  severity_level INT NOT NULL DEFAULT 1,
  reason TEXT,
  suggestion TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_gap_item_report FOREIGN KEY (report_id) REFERENCES knowledge_gap_report(id),
  CONSTRAINT fk_gap_item_knowledge FOREIGN KEY (knowledge_item_id) REFERENCES knowledge_item(id),
  CONSTRAINT fk_gap_item_source_course FOREIGN KEY (source_course_id) REFERENCES course(id),
  CONSTRAINT fk_gap_item_relation FOREIGN KEY (related_course_relation_id) REFERENCES course_relation(id)
);

ALTER TABLE user_knowledge_status
  ADD CONSTRAINT ck_mastery_status CHECK (mastery_status IN ('UNKNOWN', 'LEARNING', 'MASTERED', 'WEAK', 'NEED_REVIEW'));

ALTER TABLE user_knowledge_status
  ADD CONSTRAINT ck_mastery_score CHECK (mastery_score IS NULL OR mastery_score BETWEEN 0 AND 100);

ALTER TABLE exam_question_knowledge_map
  ADD CONSTRAINT ck_question_map_confidence CHECK (confidence_score IS NULL OR confidence_score BETWEEN 0 AND 100);

ALTER TABLE knowledge_gap_report
  ADD CONSTRAINT ck_gap_include_prerequisites CHECK (include_prerequisites IN (0, 1));

ALTER TABLE knowledge_gap_item
  ADD CONSTRAINT ck_gap_type CHECK (gap_type IN ('PREREQUISITE_MISSING', 'LOW_MASTERY', 'HIGH_FREQUENCY_WEAK', 'UNREVIEWED'));

ALTER TABLE knowledge_gap_item
  ADD CONSTRAINT ck_gap_severity CHECK (severity_level BETWEEN 1 AND 5);
