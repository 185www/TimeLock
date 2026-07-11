# TimeLock · 时间锁

> 强制专注，戒掉手机瘾。一个真正安全、开源、不可绕过的 Android 防沉迷应用。

[![Build APK](https://github.com/185www/TimeLock/actions/workflows/build.yml/badge.svg)](https://github.com/185www/TimeLock/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 它是怎么工作的？

1. 打开时间锁，勾选你想监控的应用（微信、抖音、微博等）
2. 当你打开被监控的应用时，**时间锁瞬间接管屏幕**
3. 黑屏提示"你打算用多久？"→ 输入时间 → 确认
4. 应用正常打开，后台倒计时开始
5. **时间到！** 自动切回桌面，弹出"时间到啦"

## ✨ 特点

- **🔒 强制拦截** — 打开被监控应用时强制弹窗，不可跳过
- **⏱️ 灵活计时** — 5/15/30/60 分钟快捷选择，也支持自定义
- **🛡️ 防绕过** — 拦截界面无法返回，后台服务保活
- **📱 轻量极简** — 无需登录，无广告，无多余权限
- **🔓 完全开源** — 代码可审计，无后门

## 📥 下载

[Releases 页面](https://github.com/185www/TimeLock/releases) 下载最新 APK

## 🔧 使用方式

1. 安装 APK
2. 打开 App → 勾选要监控的应用
3. **前往系统设置 → 无障碍 → 已安装的应用 → 时间锁 → 开启**
4. 打开被监控的应用，即可体验

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material3
- **核心**: AccessibilityService + Foreground Service
- **CI/CD**: GitHub Actions → 自动构建 APK

## 📄 许可证

[MIT License](LICENSE)
