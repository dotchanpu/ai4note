# Database Design

核心实体包括用户、课程、章节、课程关系、资料、文件、标签、文本分块、知识条目、用户知识掌握状态、真题题目、真题知识点映射、知识缺口报告、知识缺口条目、教师画像、教师画像证据、AI 接口配置、个性化生成配置、AI 生成任务、检索记录、导出模板和导出记录。

## 核心关系

- `user_account` 1 对多 `course`
- `course` 1 对多 `chapter`
- `course` 1 对多 `material`
- `course` 多对多 `course`，通过 `course_relation` 维护前置课、关联课和后续课关系
- `material` 多对多 `tag`，通过 `material_tag` 维护资料标签
- `material` 1 对多 `text_chunk`
- `course` 1 对多 `knowledge_item`
- `user_account` 多对多 `knowledge_item`，通过 `user_knowledge_status` 维护掌握状态
- `material` 1 对多 `exam_question`
- `exam_question` 多对多 `knowledge_item`，通过 `exam_question_knowledge_map` 维护真题与知识点关系
- `knowledge_gap_report` 1 对多 `knowledge_gap_item`
- `knowledge_gap_item` 关联 `knowledge_item` 和来源课程，用于说明缺口来自当前课程还是前置课程
- `course` 1 对多 `teacher_profile`
- `teacher_profile` 1 对多 `teacher_profile_evidence`
- `material` 1 对多 `teacher_profile_evidence`
- `course` 1 对多 `review_generation_profile`
- `review_generation_profile` 1 对多 `ai_generation_task`
- `ai_provider_config` 1 对多 `ai_generation_task`

## 3NF 说明

数据库设计目标满足第三范式。课程名称、章节标题、标签名称、教师画像、真题题目、知识缺口报告、AI 接口配置等信息拆分成独立实体并通过外键关联。多对多关系使用关联表实现，例如 `material_tag`、`course_relation`、`exam_question_knowledge_map`。用户对知识点的掌握状态单独存储在 `user_knowledge_status`，避免把个人学习状态混入知识点定义表。知识缺口报告与缺口条目拆分，报告保存一次检测概要，条目保存具体薄弱知识点和建议。

## 新增功能表

### course_relation

保存课程之间的关系，例如“数据结构与算法”的前置课程是“C 语言程序设计”。生成复习资料或导出知识包时，可以根据该表自动带入前置课程内容。

### user_knowledge_status

保存用户对某个知识点的掌握状态，例如未知、学习中、已掌握、薄弱、需要复习。知识缺口检测会参考该表判断用户是否存在前置知识薄弱点。

### exam_question

保存从往年真题中拆分出的题目，包括题号、题型、题干、答案、难度、分值、年份、来源页码等信息。

### exam_question_knowledge_map

保存真题题目与知识点之间的映射关系，可由 AI 自动识别，也可人工修正。系统可基于该表统计高频考点。

### knowledge_gap_report

保存一次知识缺口检测报告，记录检测课程、是否包含前置课程、报告名称和概要。

### knowledge_gap_item

保存具体知识缺口条目，包括薄弱知识点、来源课程、关联课程关系、缺口类型、严重程度、原因和补学建议。

### teacher_profile

保存某门课对应教师的画像分析结果。该结果主要由 AI 根据课件、往年试题、实验要求、评分标准等资料自动归纳，字段包括出题风格、常见题型、评分偏好、重点章节、规避内容、分析状态、置信度和最近分析时间。用户可以对 AI 结果进行人工修正。

### teacher_profile_evidence

保存教师画像的证据来源。每条证据关联一份课程资料，记录证据类型、来源页码、证据摘要和置信度。例如“2024 年期末题中连续出现图的遍历与最短路径，说明该教师偏好图算法综合题”。

### ai_provider_config

保存 AI 接口配置，例如服务商名称、base URL、模型名称和 API Key 标识。实际 API Key 后续可放在环境变量或安全配置中，不建议明文保存。

### review_generation_profile

保存一次复习生成的配置模板，例如生成目标、难度、输出类型、是否包含前置课程、教师画像、知识缺口和个性化要求等。

### ai_generation_task

保存 AI 生成任务记录。任务类型可以包括教师画像分析、真题知识点映射、知识缺口检测、复习提纲生成、模拟题生成、知识包摘要生成等。任务记录保存提示词、执行状态、结果路径和失败原因。
