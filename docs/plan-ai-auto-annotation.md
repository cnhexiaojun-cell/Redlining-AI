# AI 自动批注实现计划

## 需求摘要

- **操作流程**：AI 审核完成后，系统自动在合同对应条款位置添加批注，无需手动操作。
- **批注内容**：风险等级、风险描述、合规建议；颜色区分（高=红，中=黄，低=橙）。
- **批注操作**：点击查看完整内容；支持编辑、删除、隐藏批注（不显示在合同文本中）。
- **约束**：批注内容不超过 500 字，超过提示「批注内容过长，请控制在500字以内」。
- **验收**：AI 批注精准、颜色区分合理，手动批注操作流畅，协作回复正常，批注筛选与状态更新正常。

## 技术选型

- **文档内插入批注**：采用**后端在 DOCX 中写入 Word 评论**的方式（项目已有 Apache POI），OnlyOffice 打开文档后原生展示评论，支持查看/编辑/删除/解决（隐藏）。不依赖 OnlyOffice 付费 Connector。
- **仅 DOCX 支持自动批注**：PDF 不写入批注；若用户上传为 PDF，报告页仍展示分析结果与批注建议列表，仅不提供「带批注文档」预览。
- **颜色区分**：Word OOXML 通过评论作者对应 `clrIdx` 区分颜色。后端创建 3 个虚拟作者（如「AI-高风险」「AI-中风险」「AI-低风险」）并绑定不同颜色索引（红/黄/橙），每条批注使用对应作者。

---

## 一、整体流程

1. 用户在扫描页点击「查看完整报告」。
2. 前端若为 DOCX：调用 `POST /api/annotate`（file + analysisResult），后端生成带批注 DOCX 存 MinIO，返回 OnlyOffice 配置。
3. 前端若为 PDF 或 annotate 失败：退化为仅传 analysisResult（或仅预览 config），报告页不展示带批注文档。
4. 报告页：OnlyOffice 展示带批注文档（comment 权限），同屏展示批注建议列表；用户点击/编辑/删除/隐藏批注由 OnlyOffice 原生完成。

---

## 二、后端实现

### 2.1 新接口：生成带批注文档

- **路由**：`POST /api/annotate`
- **入参**：`file`（MultipartFile，仅 .docx/.doc）、`analysisResult`（JSON 字符串或 part，与 `AnalysisResultDto` 一致）。
- **逻辑**：
  1. 校验文件为 DOCX/DOC。
  2. 解析 `analysisResult`，遍历 `risks`。
  3. 对每条 risk：批注正文 =「风险等级：X\n风险描述：…\n合规建议：…」，**总长度 ≤ 500 字**；超过则截断并追加「（内容已截断）」或返回 400 提示「批注内容过长，请控制在500字以内」（建议截断避免整单失败）。
  4. 使用 **Apache POI**（XWPFDocument）：在文档中**搜索 clause 文本**（按段落/Run 首次出现），在该位置插入 Word 评论（CommentRangeStart、XWPFComment、CommentRangeEnd、CommentReference）；评论作者按 severity 使用「AI-高风险」「AI-中风险」「AI-低风险」，在评论作者列表中为三者设置不同 `clrIdx`（红/黄/橙）。
  5. 将生成的 DOCX 写入 MinIO（如 `annotate/{uuid}/{filename}.docx`），生成预览 token，拼装 OnlyOffice 配置（与 preview 兼容），`permissions.comment: true`、`mode: "edit"`。
- **异常**：非 DOCX、解析失败、POI 失败返回 4xx；某条 clause 未找到可跳过并记录日志。

### 2.2 实现要点

- 使用已有 **poi-ooxml**：XWPFDocument、XWPFComment、CommentRangeStart/End、评论作者列表及 clrIdx。
- 与现有 preview 复用：返回的 config 结构一致，仅 document.url 指向新 MinIO key + 新 token；可抽公共方法拼装 config。

### 2.3 文件清单（后端）

- **新建**：`AnnotationService`（或 `DocxAnnotationService`）：接收 AnalysisResultDto + DOCX InputStream，返回写入批注后的 DOCX InputStream；内部 clause 搜索、评论插入、作者颜色。
- **新建**：`AnnotateController` 或并入 `PreviewController`：`POST /api/annotate`，接收 file + analysisResult，调用 AnnotationService，存 MinIO，返回 OnlyOffice 配置。
- **可选**：抽「构建 OnlyOffice 配置」公共方法供 preview 与 annotate 共用。

---

## 三、前端实现

### 3.1 调用与数据流

- **ScanningPage**：点击「查看完整报告」时，若为 **DOCX/DOC**，先调用 `POST /api/annotate`（file + analysisResult），成功则 navigate 带返回的 `onlyOfficeConfig` + `analysisResult`；失败或为 PDF 则退化为现有 `uploadPreviewDocument` 或仅传 analysisResult。
- **ReportPage**：从 state 取 `analysisResult`、`onlyOfficeConfig`、`fileName`。若有 onlyOfficeConfig，用 OnlyOffice 展示文档（编辑+评论），同页展示批注建议列表；若无则仅报告+批注建议列表。

### 3.2 报告页布局

- 上/左：OnlyOffice 文档区（带批注 DOCX 时批注已在文档内）。
- 下/右：审查报告 + **批注建议**列表（条款、建议、等级、复制/导出）。
- 编辑/删除/隐藏由 OnlyOffice 原生支持；可选在批注列表增加「按风险等级筛选」。

### 3.3 500 字提示

- 若后端返回 400 且消息含「批注内容过长」，前端展示「批注内容过长，请控制在500字以内」。
- 若后端采用截断策略，则无需前端校验。

### 3.4 文件清单（前端）

- **api**：新增 `annotateDocument(file, analysisResult, token)`，请求 `POST /api/annotate`，返回 OnlyOffice 配置。
- **ScanningPage**：DOCX 时先 `annotateDocument`，再 navigate；否则沿用现有逻辑。
- **ReportPage**：OnlyOffice 区域 + 批注建议列表；config 中 `mode: "edit"`、`permissions.comment: true`。
- **i18n**：新增「批注内容过长，请控制在500字以内」、可选「仅支持 DOCX 自动批注」。

---

## 四、验收对照

| 验收项 | 实现方式 |
|--------|----------|
| AI 批注精准对应风险条款 | 后端在 DOCX 中按 clause 文本搜索，首次匹配位置插入评论 |
| 颜色区分合理 | 高/中/低 → 红/黄/橙，通过评论作者 + clrIdx |
| 手动批注操作流畅 | OnlyOffice 原生：点击查看、编辑、删除、解决（隐藏） |
| 协作回复功能正常 | OnlyOffice 评论线程与回复 |
| 批注筛选、状态更新 | OnlyOffice 评论侧边栏；可选报告页列表按等级筛选 |
| 500 字限制 | 后端截断或 400 + 前端提示 |

---

## 五、依赖与风险

- **POI 评论 API**：需确认 XWPF 对 CommentsPart、作者颜色（clrIdx）的完整写法；必要时先用不同作者名区分。
- **clause 匹配**：对 clause 做规范化（去多余空白）；过长可截取前 N 字搜索。
- **仅 DOCX**：PDF 不生成带批注文档，报告页可提示「仅 DOCX 支持自动批注到文档」。
