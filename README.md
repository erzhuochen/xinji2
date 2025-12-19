# 心迹 (XinJi) - AI驱动的个人心理成长服务

<div align="center">
  <h1>💜 心迹</h1>
  <p>记录心情，见证成长</p>
  <p>基于人工智能的个人心理成长服务系统</p>
</div>

## 📖 项目简介

心迹是一款面向普通用户的移动端日记应用，通过AI技术提供情绪分析、周报生成、深度洞察等功能，帮助用户更好地了解和管理自己的心理状态。

### 核心功能

- 📝 **日记记录** - 简洁优雅的日记编写体验
- 🤖 **AI情绪分析** - 基于大语言模型的情绪识别和分析
- 📊 **情绪周报** - 可视化的情绪趋势和分布统计
- 🔮 **深度洞察** - PRO用户专属的心理成长报告
- 🔒 **隐私保护** - 端到端加密，保护用户隐私

## 🛠 技术栈

### 后端
- **框架**: Spring Boot 3.2 + Java 17
- **数据库**: MySQL 8.0 + MongoDB 6.0
- **缓存**: Redis 7
- **安全**: Spring Security + JWT
- **AI服务**: 阿里云 DashScope (通义千问)

### 前端
- **框架**: Vue 3.4 + TypeScript 5
- **构建**: Vite 5
- **UI库**: Element Plus 2.4
- **图表**: ECharts 5
- **状态管理**: Pinia 2

## 📁 项目结构

```
web-work2/
├── xinji-backend/          # 后端项目
│   ├── src/main/java/
│   │   └── com/xinji/
│   │       ├── config/     # 配置类
│   │       ├── controller/ # 控制器
│   │       ├── dto/        # 数据传输对象
│   │       ├── entity/     # 实体类
│   │       ├── repository/ # 数据访问层
│   │       ├── security/   # 安全相关
│   │       ├── service/    # 业务逻辑
│   │       └── task/       # 定时任务
│   ├── docs/               # 文档
│   │   └── init.sql        # 数据库初始化脚本
│   └── Dockerfile
│
├── xinji-frontend/         # 前端项目
│   ├── src/
│   │   ├── api/            # API接口
│   │   ├── router/         # 路由配置
│   │   ├── store/          # Pinia状态
│   │   ├── styles/         # 样式文件
│   │   ├── types/          # TypeScript类型
│   │   └── views/          # 页面组件
│   ├── nginx.conf          # Nginx配置
│   └── Dockerfile
│
├── docker-compose.yml      # Docker编排
├── .env.example            # 环境变量模板
└── README.md
```

## 🚀 快速开始

### 环境要求
- Docker 20.10+
- Docker Compose 2.0+
- Node.js 18+ (本地开发)
- Java 17+ (本地开发)

### Docker部署

1. **克隆项目**
```bash
git clone https://github.com/your-repo/xinji.git
cd xinji
```

2. **配置环境变量**
```bash
cp .env.example .env
# 编辑 .env 填入实际配置
```

3. **启动服务**
```bash
docker-compose up -d
```

4. **访问应用**
- 前端: http://localhost
- 后端API: http://localhost:8080

### 本地开发

#### 后端
```bash
cd xinji-backend
./mvnw spring-boot:run
```

#### 前端
```bash
cd xinji-frontend
npm install
npm run dev
```

## 📋 API文档

### 认证接口
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/auth/send-code | 发送验证码 |
| POST | /api/auth/login | 登录/注册 |
| POST | /api/auth/logout | 退出登录 |

### 日记接口
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/diary/list | 获取日记列表 |
| POST | /api/diary/create | 创建日记 |
| PUT | /api/diary/:id | 更新日记 |
| DELETE | /api/diary/:id | 删除日记 |

### 分析接口
| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/analysis/submit | 提交AI分析 |
| GET | /api/analysis/:id | 获取分析结果 |

### 报告接口
| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/report/weekly | 获取周报 |
| GET | /api/report/insights | 获取深度洞察 (PRO) |

## 🔐 安全设计

- **敏感数据加密**: 手机号使用AES加密存储，哈希值用于查询
- **JWT认证**: 7天有效期，支持自动刷新
- **日记内容加密**: 可选的端到端加密保护
- **请求限流**: 基于Redis的API限流保护

## 📊 会员权益

| 功能 | 免费版 | PRO版 |
|------|--------|-------|
| 日记记录 | ✅ | ✅ |
| AI情绪分析 | 3次/天 | 无限 |
| 情绪周报 | ✅ | ✅ |
| 深度洞察 | ❌ | ✅ |
| 情绪预测 | ❌ | ✅ |
| 数据导出 | ❌ | ✅ |

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

---

<div align="center">
  <p>Made with 💜 by XinJi Team</p>
</div>
