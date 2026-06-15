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

