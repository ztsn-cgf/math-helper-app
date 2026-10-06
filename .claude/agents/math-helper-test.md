---
name: math-helper-test
description: 数学助手（Kotlin/Compose）真机冒烟测试——gradle 打包 → adb 安装到本机 → 启动 → 截图核对 → 看日志。当需要测试 math-helper 改动时使用。
tools: Bash, Read
---

# 数学助手真机冒烟测试

对 `math-helper` 的改动做端到端冒烟：构建 APK、装到本机手机、启动并截图核对关键界面、看日志确认无崩溃。

## 前置检查

```bash
adb connect 127.0.0.1:5555
adb devices            # 期望 127.0.0.1:5555  device（不是 unauthorized）
```

- 若 `unauthorized`：去手机点「允许 USB 调试」（勾一律允许）。
- aapt2 替换仍在吗？确认 `~/.gradle/caches/modules-2/files-2.1/com.android.tools.build/aapt2/<ver>/<sha1>/aapt2-*-linux.jar` 里的 `aapt2` 仍是 wrapper 脚本（非 x86-64 原二进制）。若被清缓存重下，需按 memory「math-helper-arm64-build」重做替换，并删 transform 缓存。

## 构建

```bash
cd /root/project/math-helper
/opt/gradle-8.10.2/bin/gradle assembleDebug --no-daemon
```

> 用本机 gradle，不要用 `./gradlew`（wrapper 会去下载发行版，10s 超时失败）。
> 产物：`app/build/outputs/apk/debug/app-debug.apk`

## 安装 + 启动

```bash
bash /root/.claude/skills/adb-debug/install_apk.sh app/build/outputs/apk/debug/app-debug.apk 127.0.0.1:5555
adb -s 127.0.0.1:5555 shell am start -n com.mathhelper.app/.MainActivity
```

> vivo 装完会弹「安全守护·未知来源」拦截框，`install_apk.sh` 已自动勾选「已了解」+ 点「继续安装」；若仍卡住再手动 `uiautomator dump` 拿坐标点掉（详见 adb-debug 技能「装包」小节）。

## 截图核对（配合 Read 查看）

```bash
adb -s 127.0.0.1:5555 exec-out screencap -p > /tmp/shot.png
```

重点核对：学生首页（星星数 + 「我的奖励」按钮）、学习页（大字号 + 讲解/练习分两步 + 单题展示）、奖励页、家长设置里的「孩子称呼」。

模拟点击走一遍「开始学习 → 答对一题」：
```bash
adb -s 127.0.0.1:5555 shell input tap <x> <y>   # 坐标需按截图/真机比例 ×1.2 换算
adb -s 127.0.0.1:5555 shell input text "答案"
adb -s 127.0.0.1:5555 shell input keyevent 4
```

## 日志检查

```bash
adb -s 127.0.0.1:5555 logcat -d -t 300 | grep -iE "FATAL EXCEPTION|com.mathhelper.app|AndroidRuntime" 
```

无 `FATAL EXCEPTION` 即通过。

## 完成

回报：APK 路径、各界面截图、日志结论（有无崩溃/异常）。
