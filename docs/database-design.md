# Database Design

核心实体包括用户、课程、章节、课程关系、资料、文件、标签、文本分块、知识条目、教师画像、AI 接口配置、个性化生成配置、AI 生成任务、检索记录、导出模板和导出记录。

## 核心关系

- `user_account` 1 对多 `course`
- `course` 1 对多 `chapter`
- `course` 1 对多 `material`
- `course` 多对多 `course`，通过 `course_relation` 维护前置课、关联课和后续课关系
- `material` 多对多 `tag`，通过 `material_tag` 维护资料标签
- `material` 1 对多 `text_chunk`
- `course` 1 对多 `knowledge_item`
- `course` 1 对多 `teacher_profile`
- `course` 1 对多 `review_generation_profile`
- `review_generation_profile` 1 对多 `ai_generation_task`
- `ai_provider_config` 1 对多 `ai_generation_task`

## 3NF 说明

数据库设计目标满足第三范式。课程名称、章节标题、标签名称、教师画像、AI 接口配置等信息不在资料表或导出表中重复保存，而是拆分成独立实体并通过外键关联。多对多关系使用关联表实现，例如 `material_tag` 和 `course_relation`。解析文本、知识条目正文和导出结果路径分别表达不同业务事实，不属于同一字段的重复存储。

## 新增功能表

### course_relation

保存课程之间的关系，例如“数据结构与算法”的前置课程是“C 语言程序设计”。生成复习资料或导出知识包时，可以根据该表自动带入前置课程内容。

### teacher_profile

保存某门课对应教师的出题风格、常见题型、评分偏好和重点章节，供 AI 生成个性化复习资料时参考。

### ai_provider_config

保存 AI 接口配置，例如服务商名称、base URL、模型名称和 API Key 标识。实际 API Key 后续可放在环境变量或安全配置中，不建议明文保存。

### review_generation_profile

保存一次复习生成的配置模板，例如生成目标、难度、输出类型、是否包含前置课程、个性化要求等。

### ai_generation_task

保存 AI 生成任务记录，包括任务类型、提示词、执行状态、结果路径和失败原因。
