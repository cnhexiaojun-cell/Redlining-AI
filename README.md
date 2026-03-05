# Redlining AI
基于https://github.com/wjl110/Redlining-AI 项目实现的，增加了后端服务、大模型接入、onlyoffice集成

**Redlining AI** 是一款现代化的 Web 应用程序，专为智能合同审查和风险分析而设计。它通过模拟 AI 驱动的扫描过程并提供可操作的建议，帮助法律专业人士和企业快速识别合同中的潜在风险。

## 🚀 功能特性

- **智能文档上传**:

  - 支持拖拽上传文件 (PDF, DOCX)。
  - 即时文件识别和大小验证。
- **可配置的审查设置**:

  - **审查立场**: 选择您的立场（甲方 - 买方/雇主，乙方 - 卖方/雇员，或中立）。
  - **高级规则**: 输入自定义关键字或指令，让 AI 重点关注。
- **实时扫描可视化**:

  - 沉浸式仪表盘风格的扫描界面。
  - 带有动态雷达图的可视化进度跟踪。
  - 模拟风险检测的实时“系统活动日志”。
- **综合风险报告**:

  - 风险详细分类（高、中、低）。
  - 针对特定条款（如赔偿、付款条款）的可操作 AI 建议。
  - 带有健康评分的专业报告布局。

- **在线 Word/PPT 编辑**（自建 OnlyOffice）:
  - 上传 Word、Excel、PPT 等 Office 文档，在浏览器中在线查看与编辑。
  - 数据存储在己方（MinIO + 应用后端），通过 OnlyOffice Document Server 自建部署。

## 🛠️ 技术栈

- **前端框架**: [React](https://react.dev/) + [TypeScript](https://www.typescriptlang.org/)
- **构建工具**: [Vite](https://vitejs.dev/)
- **样式**: [Tailwind CSS v4](https://tailwindcss.com/)
- **动画**: [Framer Motion](https://www.framer.com/motion/)
- **图标**: [Lucide React](https://lucide.dev/)
- **路由**: [React Router v6](https://reactrouter.com/)

## 📦 快速开始

### 前置要求

- Node.js (v16 或更高版本)
- npm 或 yarn

### 安装步骤

1. **克隆仓库**

   ```bash
   git clone https://github.com/cnhexiaojun-cell/Redlining-AI.git
   cd Redlining-AI
   ```
2. **安装依赖**

   ```bash
   npm install
   ```
3. **启动开发服务器**

   ```bash
   npm run dev
   ```
4. **构建生产版本**

   ```bash
   npm run build
   ```

### 在线文档（OnlyOffice）部署说明

如需使用「在线文档」的查看与编辑功能，需自建 OnlyOffice Document Server，并与后端、MinIO 配合使用。

1. **启动 OnlyOffice Document Server（Docker）**

   ```bash
   docker compose -f docker-compose.onlyoffice.yml up -d
   ```

   默认在宿主机 `http://127.0.0.1:8080` 提供文档服务。

2. **后端配置（Spring Boot）**

   在 `application.yml` 或环境变量中配置：

   - `app.onlyoffice.document-server-url`：OnlyOffice 对外访问地址（如 `http://127.0.0.1:8080` 或前端能访问的地址）。
   - `app.onlyoffice.api-base-url`：**后端 API 的基地址**，且必须能被 OnlyOffice 容器访问（用于拉取文档 URL 与回调保存）。  
     若 OnlyOffice 跑在 Docker 而后端在宿主机，可设为 `http://host.docker.internal:8003`（端口与后端一致）。

3. **一键启动（本机已安装 Docker 时）**

   ```bash
   ./scripts/start-onlyoffice.sh
   ```
   或：`docker compose -f docker-compose.onlyoffice.yml up -d`

4. **前端**

   登录后进入「在线文档」页，上传 Word/Excel/PPT 后点击「编辑」即可在浏览器中打开 OnlyOffice 编辑器；上传页选择 PDF/DOCX/DOC 合同后也会用 OnlyOffice 插件预览全文。

## 📄 许可证

本项目采用 MIT 许可证。
