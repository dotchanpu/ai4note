# API Draft

## Auth

- `POST /api/auth/login`: 用户登录
- `POST /api/auth/register`: 用户注册

## Course

- `GET /api/courses`: 查询课程知识库
- `POST /api/courses`: 创建课程知识库
- `PUT /api/courses/{id}`: 更新课程知识库
- `DELETE /api/courses/{id}`: 删除课程知识库

## Course Relation

- `GET /api/courses/{id}/relations`: 查询课程前置关系和关联课程
- `POST /api/courses/{id}/relations`: 添加前置课程或关联课程
- `DELETE /api/course-relations/{relationId}`: 删除课程关系

## Material

- `POST /api/materials`: 上传课程资料
- `POST /api/materials/{id}/parse`: 解析资料文本
- `GET /api/search`: 检索资料

## Teacher Profile

- `GET /api/courses/{courseId}/teacher-profiles`: 查询教师画像
- `POST /api/courses/{courseId}/teacher-profiles`: 创建教师画像
- `PUT /api/teacher-profiles/{id}`: 更新教师画像
- `DELETE /api/teacher-profiles/{id}`: 删除教师画像

## AI Config And Generation

- `GET /api/ai/providers`: 查询 AI 接口配置
- `POST /api/ai/providers`: 创建 AI 接口配置
- `POST /api/review-profiles`: 创建个性化复习生成配置
- `POST /api/ai-generation-tasks`: 创建 AI 生成任务
- `GET /api/ai-generation-tasks/{id}`: 查询 AI 生成任务状态和结果

## Export

- `POST /api/exports`: 导出 Agent 知识包
- `POST /api/exports/with-relations`: 导出包含前置课程内容的 Agent 知识包
