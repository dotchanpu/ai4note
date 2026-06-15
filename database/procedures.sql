USE ai4note;

DELIMITER //

CREATE PROCEDURE count_course_materials(IN p_course_id BIGINT)
BEGIN
  SELECT material_type, COUNT(*) AS total
  FROM material
  WHERE course_id = p_course_id
  GROUP BY material_type;
END//

DELIMITER ;

