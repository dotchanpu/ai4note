USE ai4note;

CREATE TABLE IF NOT EXISTS course_relation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  prerequisite_course_id BIGINT NOT NULL,
  relation_type VARCHAR(32) NOT NULL DEFAULT 'PREREQUISITE',
  reason VARCHAR(255),
  sort_order INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_relation_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_relation_prerequisite FOREIGN KEY (prerequisite_course_id) REFERENCES course(id),
  CONSTRAINT uk_course_prerequisite UNIQUE (course_id, prerequisite_course_id)
);

CREATE TABLE IF NOT EXISTS teacher_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  teacher_name VARCHAR(128) NOT NULL,
  exam_style TEXT,
  question_preference TEXT,
  grading_preference TEXT,
  focus_topics TEXT,
  avoid_topics TEXT,
  source_summary TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_teacher_course FOREIGN KEY (course_id) REFERENCES course(id)
);

CREATE TABLE IF NOT EXISTS ai_provider_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  provider_name VARCHAR(64) NOT NULL,
  base_url VARCHAR(255) NOT NULL,
  model_name VARCHAR(128) NOT NULL,
  api_key_alias VARCHAR(128),
  enabled TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_ai_provider_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);

CREATE TABLE IF NOT EXISTS review_generation_profile (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  teacher_profile_id BIGINT,
  profile_name VARCHAR(128) NOT NULL,
  target VARCHAR(128),
  difficulty_level VARCHAR(32),
  output_type VARCHAR(32) NOT NULL DEFAULT 'REVIEW_NOTE',
  include_prerequisites TINYINT NOT NULL DEFAULT 1,
  custom_requirement TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_profile_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_review_profile_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_review_profile_teacher FOREIGN KEY (teacher_profile_id) REFERENCES teacher_profile(id)
);

CREATE TABLE IF NOT EXISTS ai_generation_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  review_profile_id BIGINT,
  teacher_profile_id BIGINT,
  provider_config_id BIGINT,
  task_type VARCHAR(32) NOT NULL,
  prompt LONGTEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  result_path VARCHAR(512),
  error_message TEXT,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finish_time DATETIME,
  CONSTRAINT fk_ai_task_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_ai_task_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_ai_task_review_profile FOREIGN KEY (review_profile_id) REFERENCES review_generation_profile(id),
  CONSTRAINT fk_ai_task_teacher FOREIGN KEY (teacher_profile_id) REFERENCES teacher_profile(id),
  CONSTRAINT fk_ai_task_provider FOREIGN KEY (provider_config_id) REFERENCES ai_provider_config(id)
);

