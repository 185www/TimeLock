# 时间锁 (TimeLock) — MVP 开发文档

> 一个极简 Android 防沉迷工具。监控选定应用，强制你设定使用时长，到点自动锁回桌面。

---

## 核心用户流程

1. 用户打开"时间锁"，勾选需要监控的应用（如微信、抖音）
2. 当用户打开被监控的应用时
3. **时间锁瞬间接管**：全屏黑底白字弹窗 → "你打算用多久？"
4. 用户输入时间（预设 5/15/30/60 分钟 或 自定义），点击确认
5. 目标应用正常打开，后台倒计时启动
6. 倒计时到点 → 自动切回桌面 → 弹窗"时间到啦"

---

## 技术架构

| 组件 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | 无框架，纯 Composable + Activity |
| 最低 SDK | Android 8.0 (API 26) |

### 核心机制

```
┌─────────────────────────────────────────────────┐
│            时间锁核心流程                           │
├─────────────────────────────────────────────────┤
│                                                   │
│  LockAccessibilityService (无障碍服务)               │
│    ├─ 监听 android.intent.action.MAIN 的应用启动    │
│    ├─ 检测到被监控应用 → 创建 WindowManager 悬浮层  │
│    └─ 到点时执行 GLOBAL_ACTION_HOME 切回桌面        │
│                                                   │
│  OverlayController (WindowManager 悬浮层)          │
│    ├─ 直接绘制在屏幕最顶层，不依赖 Activity          │
│    ├─ 全屏黑底 → "你打算用多久？"                   │
│    ├─ 快捷按钮：5/15/30/60 分钟                    │
│    ├─ 自定义输入框                                │
│    └─ 确认 → 移除悬浮层 → 启动 CountdownService     │
│                                                   │
│  CountdownService (前台服务)                       │
│    ├─ 常驻通知栏显示剩余时间                       │
│    ├─ 倒计时精度 1秒                              │
│    └─ 到点 → OverlayController 显示"时间到啦"       │
│                                                   │
│  TimeUp 悬浮层                                    │
│    ├─ 全屏黑底 → "时间到啦" + "好的" 按钮          │
│    └─ 点击 → 移除悬浮层 → 5秒 cooldown             │
│                                                   │
└─────────────────────────────────────────────────┘
```

---

## 文件结构

```
com.timelock/
├── MainActivity.kt              # 主界面（应用列表）
├── TimeLockApp.kt               # Application
├── data/
│   ├── SelectedApp.kt           # 应用数据模型
│   ├── AppRepository.kt         # 应用列表管理 + 已选存储
│   └── LockState.kt             # 全局锁状态（单例）
├── service/
│   ├── LockAccessibilityService.kt  # 无障碍服务（核心）
│   └── CountdownService.kt         # 倒计时前台服务
└── ui/
    ├── screens/
    │   └── MainScreen.kt        # 主界面 Composable
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

---

## 权限清单

| 权限 | 用途 |
|------|------|
| `BIND_ACCESSIBILITY_SERVICE` | 监听应用启动 + 模拟 Home 键（核心） |
| `FOREGROUND_SERVICE` | 倒计时前台服务 |
| `POST_NOTIFICATIONS` | 通知栏显示剩余时间（API 33+） |

---

## 安全与防绕过设计

| 风险 | 对策 |
|------|------|
| 拦截时按返回键 | 悬浮层 OnKeyListener 拦截 KEYCODE_BACK |
| 拦截时按 Home 键 | 悬浮层属于系统窗口级别，Home 键无法移除 |
| 倒计时被系统杀进程 | 前台服务 + 忽略电池优化 |
| 用户关闭无障碍服务 | 主界面 ON_RESUME 实时检测状态 |
| 时间到后再次打开应用 | LockState 5秒 cooldown 防止重复拦截 |
| 后台启动拦截失效 | 使用 WindowManager 悬浮层，不依赖 Activity 启动 |

---

## 路线图

### Phase 1 — MVP (当前)
- ✅ 应用列表选择
- ✅ 无障碍服务监听启动
- ✅ 拦截弹窗 + 时间输入
- ✅ 前台倒计时服务
- ✅ 到点自动切回桌面
- ✅ GitHub Actions 自动构建 APK

### Phase 2 — 完善
- [ ] 设备管理员权限（防卸载）
- [ ] 白名单 App/联系人
- [ ] 重启恢复锁定
- [ ] 多语言支持
- [ ] 使用统计

### Phase 3 — 进阶
- [ ] 番茄钟模式
- [ ] 成就系统
- [ ] Material You 主题
- [ ] 强迫模式（不允许取消/暂停）

---

*文档版本: v1.0 · 最后更新: 2026-07-11*
