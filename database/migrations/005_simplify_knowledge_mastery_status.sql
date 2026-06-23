USE ai4note;

UPDATE user_knowledge_status
SET mastery_status = 'LEARNING'
WHERE mastery_status IN ('WEAK', 'NEED_REVIEW');

UPDATE knowledge_gap_item
SET gap_type = 'WEAK_MASTERY'
WHERE gap_type IN ('LOW_MASTERY', 'HIGH_FREQUENCY_WEAK');

UPDATE knowledge_gap_item
SET gap_type = 'PREREQUISITE_GAP'
WHERE gap_type = 'PREREQUISITE_MISSING';

ALTER TABLE user_knowledge_status
  DROP CHECK ck_mastery_status;

ALTER TABLE user_knowledge_status
  ADD CONSTRAINT ck_mastery_status CHECK (mastery_status IN ('UNKNOWN', 'LEARNING', 'MASTERED'));

ALTER TABLE knowledge_gap_item
  DROP CHECK ck_gap_type;

ALTER TABLE knowledge_gap_item
  ADD CONSTRAINT ck_gap_type CHECK (gap_type IN ('WEAK_MASTERY', 'HIGH_FREQUENCY', 'PREREQUISITE_GAP', 'UNASSESSED'));
