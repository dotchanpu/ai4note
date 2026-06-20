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

当前登录功能暂未接入 JWT。已实现的课程、章节和资料接口临时使用 `userId` 标识当前用户，后续接入认证后应从登录凭证中获取用户身份。

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
| `400` | 参数错误、业务校验失败、目标资源不存在或无权访问 |
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

- 状态：已实现
- 方法：`PUT`
- 路径：`/api/courses/{courseId}`

请求体与创建课程相同，`userId` 用于校验课程归属。

### 5.5 删除课程

- 状态：已实现
- 方法：`DELETE`
- 路径：`/api/courses/{courseId}`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID，用于检查课程归属 |

请求示例：

```text
DELETE /api/courses/1?userId=1
```

删除课程会在同一事务中清理课程关联的章节、资料、文件记录、解析文本、知识条目、真题映射、教师画像、生成任务、检索记录和导出记录。事务提交后会删除该课程对应的本地资料文件。

成功响应：HTTP `200`，响应体为空。

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

- 状态：已实现
- 方法：`PUT`
- 路径：`/api/courses/{courseId}/chapters/{chapterId}`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |

请求体与创建章节相同；修改章节编号时仍会校验同一课程内不能重复。

### 6.4 删除章节

- 状态：已实现
- 方法：`DELETE`
- 路径：`/api/courses/{courseId}/chapters/{chapterId}`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |

删除章节不会删除章节内的资料、知识条目和真题，而是将这些记录的 `chapterId` 置空，使其变为未关联章节。

成功响应：HTTP `200`，响应体为空。

## 7. 课程关系

### 7.1 查询课程关系

- 状态：已实现
- 方法：`GET`
- 路径：`/api/courses/{courseId}/relations`

查询参数：`userId`。响应内容包括课程的前置课、关联课和后续课，并返回关联课程名称、编号、学期和说明。

### 7.2 创建课程关系

- 状态：已实现
- 方法：`POST`
- 路径：`/api/courses/{courseId}/relations`

查询参数：`userId`。

请求体：

```json
{
  "relatedCourseId": 2,
  "relationType": "PREREQUISITE",
  "reason": "数据结构中的链表和树依赖 C 语言指针与结构体",
  "sortOrder": 1
}
```

后端会校验当前课程和关联课程都属于当前用户，禁止课程关联自身，禁止同一课程重复关联同一门课。

`relationType` 可选值：

| 值 | 含义 |
|---|---|
| `PREREQUISITE` | 前置课程 |
| `RELATED` | 关联课程 |
| `FOLLOW_UP` | 后续课程 |

### 7.3 删除课程关系

- 状态：已实现
- 方法：`DELETE`
- 路径：`/api/course-relations/{relationId}`

查询参数：`userId`。只有课程归属用户可以删除该关系。

## 8. 课程资料

### 8.1 查询课程资料

- 状态：已实现
- 方法：`GET`
- 路径：`/api/courses/{courseId}/materials`

当前返回指定课程的全部资料，按上传时间倒序排列。章节、类型、年份、标签和重点标记筛选将在检索模块中实现。

### 8.2 上传课程资料

- 状态：已实现
- 方法：`POST`
- 路径：`/api/materials`
- 类型：`multipart/form-data`

表单字段：

| 字段 | 必填 | 说明 |
|---|---|---|
| `file` | 是 | 原始资料文件 |
| `userId` | 是 | 当前用户 ID |
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

- 状态：已实现
- 方法：`GET`
- 路径：`/api/materials/{materialId}`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID，用于检查资料所属课程 |

### 8.4 修改资料信息

- 状态：已实现
- 方法：`PUT`
- 路径：`/api/materials/{materialId}`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |

可修改资料标题、类型、所属章节、年份、重点标记和摘要。该接口不替换原始文件。

### 8.5 删除资料

- 状态：已实现
- 方法：`DELETE`
- 路径：`/api/materials/{materialId}`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID，用于检查资料所属课程 |

删除资料会清理资料文件记录、解析文本、标签关系、来源知识条目、来源真题及其映射、教师画像证据和用户知识状态。数据库事务提交后会删除对应的本地原始文件。

成功响应：HTTP `200`，响应体为空。

### 8.6 解析资料文本

- 状态：已实现（PDF）
- 方法：`POST`
- 路径：`/api/materials/{materialId}/parse`

当前支持 PDF 按页提取文字，并在页内进行段落和句子感知的文本分块：

- 短页面保持为一个文本块，避免过度切分。
- 自动合并 PDF 排版产生的无意义硬换行；中文折行直接连接，英文折行自动补空格。
- 英文行尾断词连字符会与下一行单词合并。
- PDF 字体映射产生的特殊项目符号 `` 会转换为强制换行。
- 列表项和真实空段落保持换行。
- 长页面优先在段落和中英文句末标点处分块。
- 单个文本块最长约 1200 个字符。
- 相邻文本块最多保留约 120 个字符的上下文重叠，减少切分位置造成的语义丢失。
- 每个文本块保留原始 PDF 页码，便于定位资料来源。

重复解析会覆盖该资料原有的文本块；扫描版 PDF 若无法提取文字会返回业务错误。

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |

响应示例：

```json
{
  "materialId": 10,
  "pageCount": 24,
  "chunkCount": 23,
  "characterCount": 18560
}
```

### 8.7 查询解析文本

- 状态：已实现
- 方法：`GET`
- 路径：`/api/materials/{materialId}/text-chunks`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |

响应按 `chunkIndex` 升序返回，字段包括页码、正文和非空白字符数。同一页内容较长时可能对应多个连续文本块。

## 9. 标签与知识条目

### 9.1 查询标签

- 状态：已实现
- 方法：`GET`
- 路径：`/api/courses/{courseId}/tags`

查询参数：`userId`。返回当前课程资料已经使用的标签名称数组。

### 9.2 为资料绑定标签

- 状态：已实现
- 方法：`PUT`
- 路径：`/api/materials/{materialId}/tags`

查询参数：`userId`。

请求体：

```json
{
  "tagNames": ["编译原理", "词法分析", "有限自动机"]
}
```

该接口会整体替换资料标签，自动复用或创建标签。单份资料最多绑定 20 个标签。

查询单份资料标签：

```text
GET /api/materials/{materialId}/tags?userId={userId}
```

### 9.3 查询课程知识条目

- 状态：已实现
- 方法：`GET`
- 路径：`/api/courses/{courseId}/knowledge-items`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |
| `materialId` | 否 | 按来源资料筛选 |
| `chapterId` | 否 | 按章节筛选 |
| `itemType` | 否 | 按知识类型筛选 |

### 9.4 创建知识条目

- 状态：已实现
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

知识类型支持：

```text
DEFINITION
KEY_POINT
FORMULA
METHOD
EXAMPLE
WARNING
```

修改和删除接口：

```text
PUT    /api/courses/{courseId}/knowledge-items/{itemId}?userId={userId}
DELETE /api/courses/{courseId}/knowledge-items/{itemId}?userId={userId}
```

### 9.5 使用 AI 整理资料知识

- 状态：已实现
- 方法：`POST`
- 路径：`/api/materials/{materialId}/knowledge-items/ai-generate`

查询参数：`userId`。

请求体：

```json
{
  "maxItems": 12,
  "replaceExisting": false,
  "model": "deepseek-v4-flash"
}
```

该接口要求资料已经完成 PDF 文本解析。系统将带页码的解析正文交给 DeepSeek，让模型基于完整语义整理：

- 3 至 10 个资料标签；
- 定义、重点、公式、方法、例子和易错点；
- 每条知识的来源页码与 1 至 5 级重要程度。

AI 被明确要求合并重复概念、禁止无依据补充，并以 JSON 结构返回。后端会再次校验类型、长度、数量和重要程度后落库。`replaceExisting=true` 时，会先清理该资料已有知识条目及其关联状态。

## 10. 资料检索

### 10.1 全文与条件检索

- 状态：已实现
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

搜索范围包括：

- 资料标题与摘要；
- 资料标签；
- PDF 解析正文；
- AI 或人工整理的知识条目标题与内容。

当前检索限定在一个课程内，后端会校验课程是否属于当前用户。

匹配规则：

- 每份资料最多返回一条资料结果；知识条目按条返回；总数最多 100 条。
- 排序优先级为标题命中、摘要命中、正文命中，同级按资料上传时间倒序。
- 正文命中会返回首个匹配文本块的 PDF 页码。
- 知识条目结果返回 `knowledgeItemId`、`itemType`、`importanceLevel` 和来源页码。
- `matchedSnippet` 返回关键词附近最多约 220 个字符的上下文。
- 每次成功检索都会写入 `search_record`，保存用户、课程、关键词、检索类型、结果数量和检索时间。

请求示例：

```text
GET /api/search?userId=7&courseId=4&keyword=编译&materialType=SLIDE&isKey=false
```

响应示例：

```json
[
  {
    "resultType": "MATERIAL",
    "knowledgeItemId": null,
    "materialId": 11,
    "courseId": 4,
    "chapterId": 2,
    "chapterNo": "第1章",
    "chapterTitle": "编译系统概述",
    "title": "Chapter 1 Courseware",
    "materialType": "SLIDE",
    "summary": "编译原理课程第一章课件",
    "year": 2026,
    "key": true,
    "originalName": "chapter1.pdf",
    "fileType": "pdf",
    "fileSize": 1258291,
    "parsedChunkCount": 18,
    "matchSource": "CONTENT",
    "matchedPageNo": 3,
    "matchedSnippet": "……编译程序通常由词法分析、语法分析和语义分析等阶段组成……"
  }
]
```

`resultType` 可选值：

| 值 | 含义 |
|---|---|
| `MATERIAL` | 资料标题、摘要、标签或解析正文命中 |
| `KNOWLEDGE_ITEM` | 知识条目标题或内容命中 |

`matchSource` 可选值：

| 值 | 含义 |
|---|---|
| `TITLE` | 资料标题命中 |
| `SUMMARY` | 资料摘要命中 |
| `TAG` | 资料标签命中 |
| `CONTENT` | PDF 解析正文命中 |
| `KNOWLEDGE_TITLE` | 知识条目标题命中 |
| `KNOWLEDGE_CONTENT` | 知识条目内容命中 |

## 11. 真题知识点映射

### 11.1 从真题资料中抽取题目

- 状态：已实现
- 方法：`POST`
- 路径：`/api/materials/{materialId}/exam-questions/extract`

请求参数：

- `userId`：必填
- `overwrite`：可选，默认 `false`

当前行为说明：

- 当前仅支持 `EXAM` 类型且文件类型为 `PDF` 的资料
- 抽题依赖已有解析文本；若资料尚未解析，会返回“需先解析后再抽题”的业务错误
- 支持 `overwrite=true` 覆盖重写
- 覆盖重写会先清空该资料已有的真题及其知识点映射，再写入本次抽取结果
- 抽题完成后，会自动尝试映射到当前课程下已经存在的知识条目；若课程尚无可用知识条目，则不会生成自动映射结果

抽取字段至少包括题号、题型、题干、答案、分值、年份和来源页码。其中题型会在入库阶段统一清洗为中文标准值。

### 11.2 查询课程真题

- 状态：已实现
- 方法：`GET`
- 路径：`/api/courses/{courseId}/exam-questions`

查询参数：

- `userId`：必填
- `page`：可选，默认 `1`
- `size`：可选，默认 `12`
- `year`：可选
- `chapterId`：可选
- `questionType`：可选
- `materialId`：可选

接口返回分页对象，而不是裸数组：

```json
{
  "items": [
    {
      "id": 101,
      "materialId": 12,
      "questionType": "简答题",
      "questionText": "说明事务的 ACID 特性",
      "sourcePage": 3,
      "mappings": []
    }
  ],
  "total": 147,
  "page": 1,
  "size": 12,
  "totalPages": 13
}
```

列表按卷面顺序优先返回，当前排序口径为：`examYear DESC -> materialId DESC -> sourcePage ASC -> normalizedQuestionNo ASC -> id ASC`。

### 11.3 建立题目与知识点映射

- 状态：已实现
- 方法：`POST`
- 路径：`/api/exam-questions/{questionId}/knowledge-map`

该接口当前主要用于人工修正或补充题目与知识点映射，不再作为主映射入口；主流程会在抽题完成后自动尝试进行 AI 映射。

请求体示例：

```json
{
  "knowledgeItemId": 20,
  "matchSource": "MANUAL",
  "confidenceScore": 92.5,
  "reason": "题目要求实现二叉树中序遍历"
}
```

### 11.4 查询高频考点统计

- 状态：已实现
- 方法：`GET`
- 路径：`/api/courses/{courseId}/exam-knowledge-stats`

该接口会基于当前真题映射结果做实时聚合统计，可按年份、章节和题型筛选知识点命中次数、累计分值和最近出现年份。

## 12. 用户掌握状态与知识缺口

### 12.1 查询知识点掌握状态

- 状态：已实现
- 方法：`GET`
- 路径：`/api/knowledge-items/{knowledgeItemId}/mastery`

查询参数：`userId`。如果用户尚未维护该知识点掌握状态，返回默认状态 `UNKNOWN`。

### 12.2 更新知识点掌握状态

- 状态：已实现
- 方法：`PUT`
- 路径：`/api/knowledge-items/{knowledgeItemId}/mastery`

请求体示例：

```json
{
  "userId": 1,
  "masteryStatus": "WEAK",
  "masteryScore": 45,
  "note": "指针操作仍不熟练",
  "lastReviewTime": "2026-06-20T21:30:00"
}
```

`lastReviewTime` 可省略；省略时后端会使用当前时间。知识条目列表接口也会返回当前用户的 `masteryStatus`、`masteryScore`、`masteryNote`、`lastReviewTime` 和 `masteryUpdateTime`。

`masteryStatus` 可选值：

```text
UNKNOWN
LEARNING
MASTERED
WEAK
NEED_REVIEW
```

### 12.3 生成知识缺口报告

- 状态：已实现
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

生成规则：

- 当前课程知识点始终参与检测。
- `includePrerequisites` 为 `true` 时，会纳入当前课程的前置课程知识点。
- 根据掌握状态、掌握分数、真题命中次数和是否来自前置课程计算 `severityLevel`。
- 明细 `gapType` 包括 `WEAK_MASTERY`、`NEED_REVIEW`、`HIGH_FREQUENCY`、`PREREQUISITE_GAP` 和 `UNASSESSED`。

### 12.4 查询课程知识缺口报告列表

- 状态：已实现
- 方法：`GET`
- 路径：`/api/courses/{courseId}/knowledge-gap-reports`

查询参数：`userId`。按创建时间倒序返回该课程的历史知识缺口报告。

### 12.5 查询知识缺口报告

- 状态：已实现
- 方法：`GET`
- 路径：`/api/knowledge-gap-reports/{reportId}`

查询参数：`userId`。后端会校验报告归属和课程归属。

### 12.6 查询知识缺口明细

- 状态：已实现
- 方法：`GET`
- 路径：`/api/knowledge-gap-reports/{reportId}/items`

查询参数：`userId`。返回知识点标题、来源课程、前置关系、掌握状态、掌握分数、真题命中次数、严重程度、原因和建议。

缺口类型：

```text
PREREQUISITE_MISSING
LOW_MASTERY
HIGH_FREQUENCY_WEAK
UNREVIEWED
```

## 13. 教师画像

### 13.1 查询课程教师画像

- 状态：已实现
- 方法：`GET`
- 路径：`/api/courses/{courseId}/teacher-profiles`

查询参数：`userId`。按最近更新时间倒序返回课程教师画像，字段包括教师名称、分析状态、置信度、出题风格、题型偏好、评分偏好、重点章节、规避内容、依据摘要和最近分析时间。

### 13.2 发起教师画像 AI 分析

- 状态：已实现
- 方法：`POST`
- 路径：`/api/courses/{courseId}/teacher-profiles/analyze`

请求体示例：

```json
{
  "userId": 1,
  "teacherName": "张老师",
  "materialIds": [11, 12, 13],
  "providerConfigId": 1,
  "model": "deepseek-v4-flash"
}
```

系统综合课件、往年真题、实验要求和评分标准，分析教师的出题风格、常见题型、评分偏好和重点章节。

当前实现使用 DeepSeek 默认配置调用结构化 JSON 分析；`providerConfigId` 字段预留给多供应商 AI 配置模块。分析成功后写入 `teacher_profile`，并保存 `teacher_profile_evidence` 证据来源；失败时保留 `FAILED` 状态记录。

### 13.3 查询教师画像证据

- 状态：已实现
- 方法：`GET`
- 路径：`/api/teacher-profiles/{profileId}/evidence`

查询参数：`userId`。证据响应包含资料 ID、资料标题、资料类型、证据类型、来源页码、证据摘要和置信度。后端会校验教师画像归属和课程归属。

### 13.4 人工确认或修正教师画像

- 状态：已实现
- 方法：`PUT`
- 路径：`/api/teacher-profiles/{profileId}`

请求体示例：

```json
{
  "userId": 1,
  "teacherName": "张老师",
  "confidenceScore": 85,
  "examStyle": "重视基础概念与综合应用",
  "questionPreference": "简答题和设计题占比较高",
  "gradingPreference": "过程分明显，强调关键步骤",
  "focusTopics": "图、排序、查找",
  "avoidTopics": "低频扩展阅读内容",
  "sourceSummary": "基于近三年真题和课件重点修正",
  "analysisStatus": "MANUAL_REVIEWED"
}
```

确认后 `analysisStatus` 可更新为 `MANUAL_REVIEWED`。后端会校验画像归属，置信度会限制在 0 到 100。

教师画像分析状态：

```text
PENDING
RUNNING
SUCCESS
FAILED
MANUAL_REVIEWED
```

## 14. AI 服务与任务

### 14.1 查询 DeepSeek 配置状态

- 状态：已实现
- 方法：`GET`
- 路径：`/api/ai/status`

该接口不会返回 API Key，只返回是否已配置、基础地址、默认模型和支持的模型。

响应示例：

```json
{
  "provider": "DeepSeek",
  "configured": true,
  "baseUrl": "https://api.deepseek.com",
  "defaultModel": "deepseek-v4-flash",
  "supportedModels": [
    "deepseek-v4-flash",
    "deepseek-v4-pro"
  ]
}
```

### 14.2 课程 AI 对话

- 状态：已实现
- 方法：`POST`
- 路径：`/api/ai/chat`

请求体示例：

```json
{
  "userId": 1,
  "courseId": 1,
  "message": "请解释 LL(1) 文法的判断方法",
  "model": "deepseek-v4-flash",
  "thinking": false,
  "maxTokens": 2048
}
```

| 字段 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |
| `courseId` | 是 | 当前课程 ID，后端会校验课程归属 |
| `message` | 是 | 用户问题，最长 12000 个字符 |
| `model` | 否 | `deepseek-v4-flash` 或 `deepseek-v4-pro` |
| `thinking` | 否 | 是否启用思考模式，默认 `false` |
| `maxTokens` | 否 | 最大输出 token，范围 1 至 8192，默认 2048 |

响应包含 `content`、可选的 `reasoningContent`、停止原因和 token 用量。

后端调用 DeepSeek OpenAI 兼容接口 `POST /chat/completions`。API Key 只从环境变量 `DEEPSEEK_API_KEY` 读取，不保存到数据库，也不会返回给前端。

### 14.3 新建 AI 服务配置

- 状态：已实现
- 方法：`POST`
- 路径：`/api/ai/providers`

请求体示例：

```json
{
  "userId": 1,
  "providerName": "DeepSeek",
  "baseUrl": "https://api.deepseek.com",
  "modelName": "deepseek-v4-flash",
  "apiKeyAlias": "DEEPSEEK_API_KEY",
  "enabled": true
}
```

数据库只保存 API Key 的环境变量别名，不保存明文密钥。`apiKeyAlias` 必须是环境变量名格式，例如 `DEEPSEEK_API_KEY`。

### 14.4 查询 AI 服务配置

- 状态：已实现
- 方法：`GET`
- 路径：`/api/ai/providers`

查询参数：`userId`。

### 14.5 修改 AI 服务配置

- 状态：已实现
- 方法：`PUT`
- 路径：`/api/ai/providers/{configId}`

请求体同创建接口。后端会校验配置归属。

### 14.6 删除 AI 服务配置

- 状态：已实现
- 方法：`DELETE`
- 路径：`/api/ai/providers/{configId}`

查询参数：`userId`。

### 14.7 创建个性化复习配置

- 状态：已实现
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

字段约束：

- `difficultyLevel`：`EASY`、`MEDIUM`、`MEDIUM_HARD`、`HARD`
- `outputType`：`REVIEW_NOTE`、`OUTLINE`、`FLASHCARDS`、`MOCK_EXAM`、`CHECKLIST`
- `teacherProfileId` 可为空；传入时后端会校验教师画像属于当前用户和课程。

### 14.8 查询个性化复习配置

- 状态：已实现
- 方法：`GET`
- 路径：`/api/review-profiles`

查询参数：`userId`、`courseId`。返回该课程的复习生成配置列表。

### 14.9 修改个性化复习配置

- 状态：已实现
- 方法：`PUT`
- 路径：`/api/review-profiles/{profileId}`

请求体同创建接口。后端会校验配置归属和课程归属。

### 14.10 删除个性化复习配置

- 状态：已实现
- 方法：`DELETE`
- 路径：`/api/review-profiles/{profileId}`

查询参数：`userId`。

### 14.11 查询 AI 生成任务记录

- 状态：已实现
- 方法：`GET`
- 路径：`/api/ai-generation-tasks`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |
| `courseId` | 否 | 课程 ID；传入时只返回该课程任务，并校验课程归属 |

当前教师画像分析和资料知识整理流程会自动写入任务记录。后续复习资料生成、模拟题生成等流程可复用同一张任务表。

任务类型：

```text
TEACHER_PROFILE
EXAM_MAPPING
KNOWLEDGE_GAP
REVIEW_GENERATION
MOCK_EXAM
PACKAGE_SUMMARY
KNOWLEDGE_EXTRACTION
```

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
[
  {
    "id": 100,
    "userId": 1,
    "courseId": 1,
    "reviewProfileId": null,
    "teacherProfileId": 8,
    "providerConfigId": 1,
    "taskType": "TEACHER_PROFILE",
    "prompt": "你是课程考试分析助手...",
    "status": "SUCCESS",
    "resultPath": "teacher-profile:8",
    "errorMessage": null,
    "createTime": "2026-06-18T15:00:00",
    "finishTime": "2026-06-18T15:01:20"
  }
]
```

### 14.12 更新 AI 生成任务状态

- 状态：已实现
- 方法：`PUT`
- 路径：`/api/ai-generation-tasks/{taskId}/status`

请求体示例：

```json
{
  "userId": 1,
  "status": "FAILED",
  "resultPath": null,
  "errorMessage": "模型返回为空"
}
```

该接口用于后续异步生成流程回写状态。当前同步 AI 流程会由后端服务内部自动标记 `RUNNING`、`SUCCESS` 或 `FAILED`。

## 15. Agent 知识包导出

### 15.1 查询导出模板

- 状态：已实现
- 方法：`GET`
- 路径：`/api/export-templates`

返回系统可用的 Agent 导出模板列表，字段包括模板名称、目标 Agent、模板格式和模板说明。

### 15.2 导出课程知识包

- 状态：已实现
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
  "includeExamStats": true,
  "includePrerequisiteCourses": true,
  "includeRelatedCourses": false,
  "includeFollowUpCourses": false
}
```

当前支持 `ZIP` 格式。导出范围支持按章节、资料类型、重点资料筛选；可选择是否包含高频考点统计，以及是否包含前置课程、关联课程、后续课程的重点内容。

关联课程导出规则：

- `includePrerequisiteCourses`：包含当前课程前置课程的重点资料和重点知识。
- `includeRelatedCourses`：包含当前课程关联课程的重点资料和重点知识。
- `includeFollowUpCourses`：包含当前课程后续课程的重点资料和重点知识。
- 后端会根据课程关系校验课程归属，只纳入当前用户拥有的课程。
- 关联课程只导出重点内容：重点资料、重点资料关联知识、高重要度知识和重点类型知识。
- 导出记录的 `export_scope` 会保存用户选择的关联课程开关，以及实际纳入的课程 ID。

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

开启关联课程导出时，ZIP 会额外包含：

```text
context/related-courses.md
materials/related-course-materials.md
summaries/related-course-key-points.md
source/related-course-files.md
```

成功后会在 `storage/exports/` 下生成 ZIP 文件，并写入 `export_record`。

### 15.3 查询导出记录

- 状态：已实现
- 方法：`GET`
- 路径：`/api/exports`

查询参数：

| 参数 | 必填 | 说明 |
|---|---|---|
| `userId` | 是 | 当前用户 ID |
| `courseId` | 否 | 课程 ID；传入时只查询该课程导出记录 |

### 15.4 下载导出知识包

- 状态：已实现
- 方法：`GET`
- 路径：`/api/exports/{exportId}/download`

查询参数：`userId`。后端会校验导出记录归属，并限制文件路径必须位于配置的存储根目录下。

## 16. 接口实现顺序建议

后续开发建议按以下顺序实现：

1. 课程前置关系
2. 用户掌握状态与知识缺口检测
3. AI 服务配置与教师画像分析
4. 个性化复习生成
