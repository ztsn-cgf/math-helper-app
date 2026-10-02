# 小学数学错题辅助工具 — 项目上下文

完整设计见同目录 `DESIGN.md`（唯一权威文档，实现前先读它）。

## 硬约束（不可违反）

1. 语言 Kotlin，UI 用 Jetpack Compose，支持手机 + 平板自适应。
2. 无自有后端服务器：数据全本地（Room），AI 直接调云端 API（默认 DeepSeek，可配置）。
3. AI 服务商的 API key 不写死，从本地配置读取（gitignore）。
4. 单设备使用，第一版不做多设备同步、不做账号系统（用 PIN 切换家长/学生模式）。
5. 讲解内容分三类：video（现成微课链接/本地资源）、card（图文参照卡）、interactive（Compose 动画，空间想象类首选）。
6. 视频/讲解硬上限 90 秒。
7. 掌握度状态机按 DESIGN.md 第 4 节实现，勿复杂化。
8. 手机 + 平板都要适配：平板（≥10 寸）用 Jetpack Compose 自适应布局（WindowSizeClass 区分 compact/medium/expanded）。大屏下学生模式要更大按钮与字号，家长模式可用双栏（列表 + 详情）。

## 数据地基（种子数据，App 启动时预置进 Room）

- `data/knowledge-points.json` — 知识点树（按领域组织，版本无关）
- `data/misconceptions.json` — 典型误区清单（归因 + 纠正讲解用）
- `data/reference-materials.json` — 参照物/量感锚点（图文参照卡用）
- `docs/research-notes.md` — 权威来源与教材版本决策

实现第一步（知识点树）时先读这三份 JSON，作为种子数据的唯一来源。

## 教材

北师大版，深圳，三年级数学起步；知识点树需向前覆盖二年级的量感遗留（长度单位等）。

## 开发顺序（按 DESIGN.md 第 11 节）

1. 知识点树 + 误区 + 参照物素材（地基，数据先行）
2. 拍照 → OCR → 归因 原型
3. 家长端 + 学生端界面
4. 掌握度追踪 + 弱项加练

## 本机环境

无 Java / Gradle / Android SDK / Kotlin。代码只能写、不能在此构建，构建由用户在自己电脑的 Android Studio 完成。因此：源码要完整、自洽、目录结构标准，方便直接用 Android Studio 打开。
