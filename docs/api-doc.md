# AI4Note 接口文档

## 1. 文档说明

本文档描述 AI4Note 前后端接口约定。

- 后端基础地址：`http://localhost:8080`
- API 前缀：`/api`
- 请求数据格式：`application/json`
- 文件上传格式：`multipart/form-data`
- 字符编码：UTF-8

接口状态说明：

| 状态 | 含义 |
|---|---|
| 已实现 | 当前后端已有对应接口，可以直接联调 |
| 规划中 | 已完成业务和数据库设计，尚未编写后端实现 |

当前登录功能暂未接入 JWT。已实现的课程和章节接口临时使用 `userId` 标识当前用户，后续接入认证后应从登录凭证中获取用户身份。

## 2. 通用错误响应

请求参数错误、业务校验失败时返回：

```json
{
  "status": 400,
  "message": "具体错误信息"
}
```

服务器内部异常时返回：

```json
{
  "status": 500,
  "message": "服务器处理请求失败"
}
```

常见 HTTP 状态码：

| 状态码 | 含义 |
|---|---|
| `200` | 请求成功 |
| `400` | 参数错误或业务校验失败 |
| `404` | 目标资源不存在 |
| `500` | 服务器内部错误 |

## 3. 健康检查

### 3.1 后端健康检查

- 状态：已实现
- 方法：`GET`
- 路径：`/api/health`

响应示例：

```json
{
  "status": "ok"
}
```

## 4. 用户认证

### 4.1 用户注册

- 状态：已实现
- 方法：`POST`
- 路径：`/api/auth/register`

请求体：

```json
{
  "username": "student01",
  "password": "123456",
  "email": "student01@example.com"
}
```

字段说明：

| 字段 | 必填 | 说明 |
|---|---|---|
| `username` | 是 | 用户名，最长 64 个字符，必须唯一 |
| `password` | 是 | 密码，长度为 6 至 64 个字符 |
| `email` | 否 | 邮箱，最长 128 个字符 |

响应示例：

```json
{
  "id": 1,
  "username": "student01",
  "email": "student01@example.com",
  "role": "USER",
  "createTime": "2026-06-18T14:00:00",
  "updateTime": "2026-06-18T14:00:00"
}
```

说明：后端使用 BCrypt 保存密码，不返回密码散列。

### 4.2 用户登录

- 状态：已实现
- 方法：`POST`
- 路径：`/api/auth/login`

请求体：

```json
{
  "username": "student01",
  "password": "123456"
}
```

响应格式与注册接口相同。

## 5. 课程知识库

### 5.1 查询用户课程列表

- 状态：已实现
- 方法：`GET`
- 路径：`/api/courses`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |

请求示例：

```text
GET /api/courses?userId=1
```

响应示例：

```json
[
  {
    "id": 1,
    "userId": 1,
    "courseName": "数据结构与算法",
    "courseCode": "CS-DSA",
    "description": "数据结构与算法课程知识库",
    "semester": "2026春",
    "createTime": "2026-06-18T14:10:00",
    "updateTime": "2026-06-18T14:10:00"
  }
]
```

### 5.2 查询课程详情

- 状态：已实现
- 方法：`GET`
- 路径：`/api/courses/{courseId}`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID，用于检查课程归属 |

### 5.3 创建课程知识库

- 状态：已实现
- 方法：`POST`
- 路径：`/api/courses`

请求体：

```json
{
  "userId": 1,
  "courseName": "数据结构与算法",
  "courseCode": "CS-DSA",
  "semester": "2026春",
  "description": "数据结构与算法课程知识库"
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 创建者 ID |
| `courseName` | 是 | 课程名称 |
| `courseCode` | 否 | 课程编号 |
| `semester` | 否 | 所属学期 |
| `description` | 否 | 课程简介 |

### 5.4 修改课程

- 状态：规划中
- 方法：`PUT`
- 路径：`/api/courses/{courseId}`

### 5.5 删除课程

- 状态：规划中
- 方法：`DELETE`
- 路径：`/api/courses/{courseId}`

## 6. 课程章节

### 6.1 查询课程章节

- 状态：已实现
- 方法：`GET`
- 路径：`/api/courses/{courseId}/chapters`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |

响应示例：

```json
[
  {
    "id": 1,
    "courseId": 1,
    "chapterNo": "第1章",
    "chapterTitle": "线性表",
    "sortOrder": 1
  }
]
```

### 6.2 创建课程章节

- 状态：已实现
- 方法：`POST`
- 路径：`/api/courses/{courseId}/chapters`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |

请求体：

```json
{
  "chapterNo": "第1章",
  "chapterTitle": "线性表",
  "sortOrder": 1
}
```

同一课程内的章节编号不能重复。

### 6.3 修改章节

- 状态：规划中
- 方法：`PUT`
- 路径：`/api/chapters/{chapterId}`

### 6.4 删除章节

- 状态：规划中
- 方法：`DELETE`
- 路径：`/api/chapters/{chapterId}`

## 7. 课程关系

### 7.1 查询课程关系

- 状态：规划中
- 方法：`GET`
- 路径：`/api/courses/{courseId}/relations`

响应内容包括课程的前置课、关联课和后续课。

### 7.2 创建课程关系

- 状态：规划中
- 方法：`POST`
- 路径：`/api/courses/{courseId}/relations`

请求体：

```json
{
  "relatedCourseId": 2,
  "relationType": "PREREQUISITE",
  "reason": "数据结构中的链表和树依赖 C 语言指针与结构体",
  "sortOrder": 1
}
```

`relationType` 可选值：

| 值 | 含义 |
|---|---|
| `PREREQUISITE` | 前置课程 |
| `RELATED` | 关联课程 |
| `FOLLOW_UP` | 后续课程 |

### 7.3 删除课程关系

- 状态：规划中
- 方法：`DELETE`
- 路径：`/api/course-relations/{relationId}`

## 8. 课程资料

### 8.1 查询课程资料

- 状态：规划中
- 方法：`GET`
- 路径：`/api/courses/{courseId}/materials`

支持按章节、资料类型、年份、标签和重点标记筛选。

### 8.2 上传课程资料

- 状态：规划中
- 方法：`POST`
- 路径：`/api/materials`
- 类型：`multipart/form-data`

表单字段：

| 字段 | 必填 | 说明 |
|---|---|---|
| `file` | 是 | 原始资料文件 |
| `courseId` | 是 | 所属课程 |
| `chapterId` | 否 | 所属章节 |
| `title` | 是 | 资料标题 |
| `materialType` | 是 | 资料类型 |
| `year` | 否 | 资料年份 |
| `isKey` | 否 | 是否为重点资料 |
| `summary` | 否 | 资料摘要 |

建议的 `materialType`：

```text
SLIDE
LAB_REPORT
EXAM
NOTE
CODE
OTHER
```

### 8.3 查询资料详情

- 状态：规划中
- 方法：`GET`
- 路径：`/api/materials/{materialId}`

### 8.4 修改资料信息

- 状态：规划中
- 方法：`PUT`
- 路径：`/api/materials/{materialId}`

### 8.5 删除资料

- 状态：规划中
- 方法：`DELETE`
- 路径：`/api/materials/{materialId}`

### 8.6 解析资料文本

- 状态：规划中
- 方法：`POST`
- 路径：`/api/materials/{materialId}/parse`

系统解析 PDF、Word、Markdown 或 TXT，并按页码和文本块保存结果。

### 8.7 查询解析文本

- 状态：规划中
- 方法：`GET`
- 路径：`/api/materials/{materialId}/text-chunks`

## 9. 标签与知识条目

### 9.1 查询标签

- 状态：规划中
- 方法：`GET`
- 路径：`/api/tags`

### 9.2 为资料绑定标签

- 状态：规划中
- 方法：`POST`
- 路径：`/api/materials/{materialId}/tags`

### 9.3 查询课程知识条目

- 状态：规划中
- 方法：`GET`
- 路径：`/api/courses/{courseId}/knowledge-items`

### 9.4 创建知识条目

- 状态：规划中
- 方法：`POST`
- 路径：`/api/courses/{courseId}/knowledge-items`

请求体示例：

```json
{
  "materialId": 10,
  "chapterId": 2,
  "title": "单链表的插入",
  "itemType": "KEY_POINT",
  "content": "单链表插入需要修改前驱结点和新结点的指针。",
  "sourcePage": 12,
  "importanceLevel": 4
}
```

## 10. 资料检索

### 10.1 全文与条件检索

- 状态：规划中
- 方法：`GET`
- 路径：`/api/search`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |
| `courseId` | 否 | 课程范围 |
| `keyword` | 是 | 检索关键词 |
| `chapterId` | 否 | 章节筛选 |
| `materialType` | 否 | 资料类型筛选 |
| `isKey` | 否 | 是否只检索重点资料 |

搜索范围包括资料标题、摘要、标签和解析正文。

## 11. 真题知识点映射

### 11.1 从真题资料中抽取题目

- 状态：规划中
- 方法：`POST`
- 路径：`/api/materials/{materialId}/exam-questions/extract`

系统将真题资料拆分为独立题目，并提取题号、题型、题干、答案、分值和年份等信息。

### 11.2 查询课程真题

- 状态：规划中
- 方法：`GET`
- 路径：`/api/courses/{courseId}/exam-questions`

### 11.3 建立题目与知识点映射

- 状态：规划中
- 方法：`POST`
- 路径：`/api/exam-questions/{questionId}/knowledge-map`

请求体示例：

```json
{
  "knowledgeItemId": 20,
  "matchSource": "AI",
  "confidenceScore": 92.5,
  "reason": "题目要求实现二叉树中序遍历"
}
```

### 11.4 查询高频考点统计

- 状态：规划中
- 方法：`GET`
- 路径：`/api/courses/{courseId}/exam-knowledge-stats`

可按年份、章节和题型统计知识点出现频率。

## 12. 用户掌握状态与知识缺口

### 12.1 更新知识点掌握状态

- 状态：规划中
- 方法：`PUT`
- 路径：`/api/knowledge-items/{knowledgeItemId}/mastery`

请求体示例：

```json
{
  "userId": 1,
  "masteryStatus": "WEAK",
  "masteryScore": 45,
  "note": "指针操作仍不熟练"
}
```

`masteryStatus` 可选值：

```text
UNKNOWN
LEARNING
MASTERED
WEAK
NEED_REVIEW
```

### 12.2 生成知识缺口报告

- 状态：规划中
- 方法：`POST`
- 路径：`/api/courses/{courseId}/knowledge-gap-reports`

请求体示例：

```json
{
  "userId": 1,
  "includePrerequisites": true,
  "reportName": "数据结构期末知识缺口分析"
}
```

系统综合当前课程知识点、前置课程知识点、真题高频考点和用户掌握状态生成报告。

### 12.3 查询知识缺口报告

- 状态：规划中
- 方法：`GET`
- 路径：`/api/knowledge-gap-reports/{reportId}`

### 12.4 查询知识缺口明细

- 状态：规划中
- 方法：`GET`
- 路径：`/api/knowledge-gap-reports/{reportId}/items`

缺口类型：

```text
PREREQUISITE_MISSING
LOW_MASTERY
HIGH_FREQUENCY_WEAK
UNREVIEWED
```

## 13. 教师画像

### 13.1 查询课程教师画像

- 状态：规划中
- 方法：`GET`
- 路径：`/api/courses/{courseId}/teacher-profiles`

### 13.2 发起教师画像 AI 分析

- 状态：规划中
- 方法：`POST`
- 路径：`/api/courses/{courseId}/teacher-profiles/analyze`

请求体示例：

```json
{
  "userId": 1,
  "teacherName": "张老师",
  "materialIds": [11, 12, 13],
  "providerConfigId": 1
}
```

系统综合课件、往年真题、实验要求和评分标准，分析教师的出题风格、常见题型、评分偏好和重点章节。

### 13.3 查询教师画像证据

- 状态：规划中
- 方法：`GET`
- 路径：`/api/teacher-profiles/{profileId}/evidence`

证据响应应包含资料、来源页码、证据摘要和置信度。

### 13.4 人工确认或修正教师画像

- 状态：规划中
- 方法：`PUT`
- 路径：`/api/teacher-profiles/{profileId}`

确认后 `analysisStatus` 可更新为 `MANUAL_REVIEWED`。

教师画像分析状态：

```text
PENDING
RUNNING
SUCCESS
FAILED
MANUAL_REVIEWED
```

## 14. AI 服务与任务

### 14.1 查询 AI 服务配置

- 状态：规划中
- 方法：`GET`
- 路径：`/api/ai/providers`

### 14.2 新建 AI 服务配置

- 状态：规划中
- 方法：`POST`
- 路径：`/api/ai/providers`

请求体示例：

```json
{
  "userId": 1,
  "providerName": "OpenAI Compatible",
  "baseUrl": "https://example.com/v1",
  "modelName": "example-model",
  "apiKeyAlias": "AI4NOTE_API_KEY",
  "enabled": true
}
```

安全要求：数据库只保存 API Key 的环境变量别名，不保存明文密钥。

### 14.3 创建个性化复习配置

- 状态：规划中
- 方法：`POST`
- 路径：`/api/review-profiles`

请求体示例：

```json
{
  "userId": 1,
  "courseId": 1,
  "teacherProfileId": 1,
  "profileName": "期末冲刺",
  "target": "期末考试",
  "difficultyLevel": "MEDIUM_HARD",
  "outputType": "REVIEW_NOTE",
  "includePrerequisites": true,
  "customRequirement": "重点覆盖树、图和排序"
}
```

### 14.4 创建 AI 生成任务

- 状态：规划中
- 方法：`POST`
- 路径：`/api/ai-generation-tasks`

任务类型建议值：

```text
TEACHER_PROFILE_ANALYSIS
EXAM_KNOWLEDGE_MAPPING
KNOWLEDGE_GAP_ANALYSIS
REVIEW_NOTE_GENERATION
MOCK_EXAM_GENERATION
KNOWLEDGE_PACKAGE_SUMMARY
```

### 14.5 查询 AI 生成任务

- 状态：规划中
- 方法：`GET`
- 路径：`/api/ai-generation-tasks/{taskId}`

任务状态：

```text
PENDING
RUNNING
SUCCESS
FAILED
CANCELED
```

响应示例：

```json
{
  "id": 100,
  "taskType": "REVIEW_NOTE_GENERATION",
  "status": "SUCCESS",
  "resultPath": "storage/ai-results/review/100.md",
  "errorMessage": null,
  "createTime": "2026-06-18T15:00:00",
  "finishTime": "2026-06-18T15:01:20"
}
```

## 15. Agent 知识包导出

### 15.1 查询导出模板

- 状态：规划中
- 方法：`GET`
- 路径：`/api/export-templates`

### 15.2 导出课程知识包

- 状态：规划中
- 方法：`POST`
- 路径：`/api/exports`

请求体示例：

```json
{
  "userId": 1,
  "courseId": 1,
  "templateId": 1,
  "exportName": "数据结构期末知识包",
  "exportFormat": "ZIP",
  "chapterIds": [1, 2, 3],
  "materialTypes": ["SLIDE", "EXAM", "NOTE"],
  "onlyKeyMaterials": true,
  "includePrerequisites": true,
  "includeTeacherProfile": true,
  "includeExamStats": true,
  "knowledgeGapReportId": 5
}
```

导出包可包含：

```text
AGENTS.md
README.md
manifest.json
index.md
context/
materials/
summaries/
prompts/
source/
```

### 15.3 查询导出记录

- 状态：规划中
- 方法：`GET`
- 路径：`/api/exports`

### 15.4 下载导出知识包

- 状态：规划中
- 方法：`GET`
- 路径：`/api/exports/{exportId}/download`

## 16. 接口实现顺序建议

后续开发建议按以下顺序实现：

1. 资料上传、文件保存和资料查询
2. PDF 文本解析与文本分块
3. 标签、知识条目和全文检索
4. 课程前置关系
5. 真题题目抽取与知识点映射
6. 用户掌握状态与知识缺口检测
7. AI 服务配置与教师画像分析
8. 个性化复习生成
9. Agent 知识包导出
