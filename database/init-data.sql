USE ai4note;

INSERT INTO export_template (template_name, target_agent, template_format, template_content)
VALUES
  ('Codex Knowledge Pack', 'Codex', 'ZIP', 'Generate AGENTS.md, manifest.json, index.md and markdown materials.'),
  ('General Agent Context', 'General', 'Markdown', 'Generate a single context markdown file.');

