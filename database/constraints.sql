USE ai4note;

ALTER TABLE material
  ADD CONSTRAINT ck_material_is_key CHECK (is_key IN (0, 1));

ALTER TABLE knowledge_item
  ADD CONSTRAINT ck_importance_level CHECK (importance_level BETWEEN 1 AND 5);

ALTER TABLE material_file
  ADD CONSTRAINT ck_file_size CHECK (file_size >= 0);

