CREATE DATABASE IF NOT EXISTS ai4note DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai4note;

CREATE TABLE user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  email VARCHAR(128),
  role VARCHAR(32) NOT NULL DEFAULT 'USER',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE course (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_name VARCHAR(128) NOT NULL,
  course_code VARCHAR(64),
  description TEXT,
  semester VARCHAR(64),
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_course_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);

CREATE TABLE chapter (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  chapter_no VARCHAR(64) NOT NULL,
  chapter_title VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_chapter_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT uk_chapter_no UNIQUE (course_id, chapter_no)
);

CREATE TABLE course_relation (
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

CREATE TABLE material (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  chapter_id BIGINT,
  title VARCHAR(255) NOT NULL,
  material_type VARCHAR(32) NOT NULL,
  summary TEXT,
  year INT,
  is_key TINYINT NOT NULL DEFAULT 0,
  upload_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_material_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_material_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id)
);

CREATE TABLE material_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_id BIGINT NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  stored_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(512) NOT NULL,
  file_type VARCHAR(32) NOT NULL,
  file_size BIGINT NOT NULL,
  upload_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_file_material FOREIGN KEY (material_id) REFERENCES material(id)
);

CREATE TABLE tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tag_name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE material_tag (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_id BIGINT NOT NULL,
  tag_id BIGINT NOT NULL,
  CONSTRAINT fk_mt_material FOREIGN KEY (material_id) REFERENCES material(id),
  CONSTRAINT fk_mt_tag FOREIGN KEY (tag_id) REFERENCES tag(id),
  CONSTRAINT uk_material_tag UNIQUE (material_id, tag_id)
);

CREATE TABLE text_chunk (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  material_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  page_no INT,
  content LONGTEXT NOT NULL,
  word_count INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_chunk_material FOREIGN KEY (material_id) REFERENCES material(id)
);

CREATE TABLE knowledge_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  course_id BIGINT NOT NULL,
  material_id BIGINT,
  chapter_id BIGINT,
  title VARCHAR(255) NOT NULL,
  item_type VARCHAR(32) NOT NULL,
  content LONGTEXT NOT NULL,
  source_page INT,
  importance_level INT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ki_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_ki_material FOREIGN KEY (material_id) REFERENCES material(id),
  CONSTRAINT fk_ki_chapter FOREIGN KEY (chapter_id) REFERENCES chapter(id)
);

CREATE TABLE search_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT,
  keyword VARCHAR(255) NOT NULL,
  search_type VARCHAR(32) NOT NULL,
  result_count INT NOT NULL DEFAULT 0,
  search_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_search_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_search_course FOREIGN KEY (course_id) REFERENCES course(id)
);

CREATE TABLE export_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_name VARCHAR(128) NOT NULL,
  target_agent VARCHAR(64) NOT NULL,
  template_format VARCHAR(32) NOT NULL,
  template_content TEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE export_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  course_id BIGINT NOT NULL,
  template_id BIGINT,
  export_name VARCHAR(255) NOT NULL,
  export_format VARCHAR(32) NOT NULL,
  export_path VARCHAR(512) NOT NULL,
  export_scope TEXT,
  export_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_export_user FOREIGN KEY (user_id) REFERENCES user_account(id),
  CONSTRAINT fk_export_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_export_template FOREIGN KEY (template_id) REFERENCES export_template(id)
);

CREATE TABLE teacher_profile (
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

CREATE TABLE ai_provider_config (
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

CREATE TABLE review_generation_profile (
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

CREATE TABLE ai_generation_task (
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
