# 时间锁 (TimeLock)

> 打开受限应用 → 设定本次使用时间 → 时间到自动退出。

何同学在戒手机视频里发布的“时间锁”是个锁住整个手机的思路，实际体验差、多年不更新。
本项目换了一个更实用的思路：**按应用限制**——你打开某个应用，时间锁先弹出窗口让你设定本次能用多久，倒计时开始，时间一到就自动退出该应用。

技术实现参考开源项目 [Mindful](https://github.com/ivstka95/Mindful)：用
`AccessibilityService` 监听前台应用、用 `<queries>` 只枚举可启动的应用（不申请
`QUERY_ALL_PACKAGES`），并在需要时弹出一个全屏窗口拦截。所有判断都在本地完成，不上传任何数据。

## 工作方式

1. 在“选择要限制的应用”里勾选你容易沉迷的应用。
2. 开启 **无障碍** 权限（应用内会引导你）。
3. 之后你打开任意一个受限应用，时间锁会**先弹窗让你设定使用时间**。
4. 设定后开始倒计时，你可以正常使用。
5. 时间一到，时间锁**自动退出该应用**，并提示“时间到”。

## 技术要点（照搬 Mindful 的成熟做法）

- **前台应用检测**：`AccessibilityService` 监听 `TYPE_WINDOW_STATE_CHANGED`，过滤掉自身、桌面、系统 UI。
- **应用列表**：只通过 `PackageManager` 查询 `MAIN/LAUNCHER` 应用，不申请 `QUERY_ALL_PACKAGES`，尊重隐私。
- **后台保护 / 倒计时**：`CountdownService` 前台服务持续计时并在通知栏显示剩余时间，即使主界面关掉也在运行；开机自启时若仍有未结束的会话会自动恢复。
- **强制操作**：时间到时通过无障碍的 `performGlobalAction(GLOBAL_ACTION_HOME)` 把用户送回桌面并退出目标应用。

## 构建

```bash
./gradlew assembleRelease
```

APK 生成在 `app/build/outputs/apk/release/`。

仓库已配置 GitHub Actions（`Build APK`），push 到 `main` 或打 `v*` tag 会自动构建。

## 许可证

MIT
