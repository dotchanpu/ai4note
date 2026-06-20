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
- `user_account` 1 对多 `search_record`
- `course` 1 对多 `search_record`

## 3NF 说明

数据库设计目标满足第三范式。课程名称、章节标题、标签名称、教师画像、真题题目、知识缺口报告、AI 接口配置等信息拆分成独立实体并通过外键关联。多对多关系使用关联表实现，例如 `material_tag`、`course_relation`、`exam_question_knowledge_map`。用户对知识点的掌握状态单独存储在 `user_knowledge_status`，避免把个人学习状态混入知识点定义表。知识缺口报告与缺口条目拆分，报告保存一次检测概要，条目保存具体薄弱知识点和建议。

## 删除策略

- 删除资料时，由业务服务按外键依赖顺序删除资料关联的教师画像证据、真题知识映射、知识缺口条目、用户知识状态、真题、知识条目、标签关系、文本分块和文件记录，最后删除资料记录。
- 删除章节时不删除资料、知识条目或真题，只将这些记录的 `chapter_id` 置为 `NULL`，避免误删课程内容。
- 删除课程时，由业务服务在事务内清理课程下所有依赖记录，事务提交后再删除存储目录中的原始资料文件。
- 文件删除限定在配置的 `ai4note.storage-root` 内；规范化后的路径若越出存储根目录会被拒绝删除。

当前删除级联主要由 `ContentDeletionService` 显式控制，而不是依赖数据库 `ON DELETE CASCADE`，便于同时协调数据库事务和文件系统清理。

## 检索记录

`search_record` 保存用户在课程内执行的资料检索，包括关键词、检索类型、结果数量和检索时间。当前检索类型使用 `MATERIAL_FULLTEXT`，搜索范围覆盖资料标题、摘要和 `text_chunk` 解析正文。

## AI 知识整理

AI 知识整理读取 `text_chunk` 中带页码的解析正文，通过 DeepSeek 输出结构化标签和知识条目。标签写入 `tag` 和 `material_tag`，知识条目写入 `knowledge_item`，并保留来源资料、章节、来源页码、知识类型与重要程度。

系统不使用简单关键词频率直接生成知识条目，而是要求模型基于全文语义合并重复概念，并区分定义、重点、公式、方法、例子和易错点。

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
