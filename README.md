# 数学助手（math-helper）

面向深圳地区三年级小学生的数学错题辅助工具（北师大版教材）。
Kotlin + Jetpack Compose，手机 / 平板自适应（WindowSizeClass 区分 compact/medium/expanded）。

## 核心功能

**核心闭环**：拍照录错题 → OCR 识别 → AI 归因到知识点/误区 → 短讲解 → 同类题 → 掌握度追踪 → 弱项加练。

- **拍照录入**：可拍一整页作业/试卷，AI 自动拆题、判断对错（重点/轻微分级），勾选存错题。
- **短讲解**：49 条孩子话讲解文案 + 15 类会动插画（几何 / 数与代数 / 量感），讲解 ≤ 90 秒。
- **加练**：AI 出带生活场景的应用题，解题思路 ①②③ 分步；选择题 / 计算题双模式。
- **掌握度追踪**：新学 / 薄弱 / 巩固中 / 已掌握 四态 + 做对做错统计；间隔复习（艾宾浩斯）。
- **学生端**：闯关（按知识点设关卡）、奖励商店（星星兑换，家长 PIN 确认）。
- **家长端**：掌握度地图、错题列表、AI 设置、孩子称呼；PIN 切换家长/学生模式。

## 技术要点

- 无自有后端：数据全本地（Room），AI 直接调云端 API（默认 DeepSeek，可配置）。
- 单设备使用：无账号系统，用 PIN 切换家长/学生模式。
- AI 服务商 key 不写死，从本地配置读取（已 gitignore）。

## 构建

用 Android Studio 打开项目即可（compileSdk 35，JDK 17）；或命令行：

```bash
./gradlew assembleDebug   # 产物在 app/build/outputs/apk/debug/
```

> 本机无 Android SDK 时，构建在装有 Android Studio 的电脑上完成；GitHub Actions（`.github/workflows/build.yml`）也会在 push 后自动打包并发布 Release。

## 配置 AI Key（本地，不入库）

两种方式，前者优先级更高：

1. **App 内**：家长模式 → AI 设置 → 填 DeepSeek API key。
2. **文件**：在 `app/src/main/assets/` 放一个 `secrets.properties`（该文件已 gitignore）：

```properties
deepseek.api_key=sk-xxx
deepseek.base_url=https://api.deepseek.com
deepseek.model=deepseek-chat
```

key 获取：platform.deepseek.com → 注册 → 充值（几块钱就够）→ API Keys → 创建。

## 文档

- `DESIGN.md` — 完整设计（唯一权威文档，实现前先读）
- `CLAUDE.md` — 项目上下文与硬约束
- `ROADMAP.md` — 功能清单、已完成 / 待办
- `docs/research-notes.md` — 权威来源与教材版本决策

## 状态

核心闭环已完成、可交付；待办（学习报告、草稿纸/竖式、错题导出等）见 `ROADMAP.md`。
