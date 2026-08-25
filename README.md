# 暗区指令工具

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Android-12%2B-brightgreen)](https://developer.android.com/about/versions/12)
[![Release](https://img.shields.io/github/v/release/scpao5/aqzlgj-kotlin)](https://github.com/scpao5/aqzlgj-kotlin/releases)

**暗区指令工具** 是一款为《暗区突围》开发的指令辅助工具，采用 **Kotlin + Jetpack Compose** 全新重写，提供悬浮窗指令分类、搜索、一键复制与执行功能，支持 Root 与免 Root 两种执行模式，自动适配用户环境。

> 前身：[scpao5/aqzlgj](https://github.com/scpao5/aqzlgj)（Java 版）

## ✨ 功能特性

- **六大分类指令**：刀皮类、战术装备、钥匙类、针剂类、操作指令、大杂烩（2 万+ 条指令）
- **🔍 全局搜索**：按名称或指令内容实时过滤，结果带分类徽章
- **📋 一键复制 / ⚡ 一键执行**：Root（`su -c` 广播）与免 Root（普通广播）自动切换
- **🪟 游戏内悬浮窗**：可拖动、展开/折叠动画、搜索、分类浏览、一键执行，颜色跟随应用主题
- **⚡ 分段渲染**：大列表按 100 条/批懒加载，2 万+ 指令流畅滚动
- **🎨 双主题**：Miuix / Material 两套界面风格自由切换，暗色模式完整适配
- **🛡️ 权限精简**：仅需悬浮窗与通知权限
- **⚙️ 全局开关**：可隐藏 Toast 提示、隐藏更新弹窗

## 📥 下载

从 [Releases](https://github.com/scpao5/aqzlgj-kotlin/releases) 页面下载最新 APK 直接安装。

## 🚀 使用

1. 打开应用，授予**悬浮窗**与**通知**权限
2. 底部栏切换到「命令」页浏览/搜索指令
3. 点击「悬浮窗」开启游戏内悬浮窗
4. 悬浮窗内点分类/指令即可一键执行

> 执行指令需游戏端存在对应的广播接收器（`android.intent.action.RUN`）。

## 🛠️ 构建

```bash
# 需要 JDK 17+ / Android SDK 37
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/
```

## 📁 项目结构

```
app/src/main/java/com/sbby/aqzlgj/kotlin/
├── data/        数据层（CodeData / PrivilegeManager）
├── permission/  权限管理（通知 / 悬浮窗）
├── service/     悬浮窗服务（传统 View 实现）
└── ui/          Compose UI（主页 / 命令页 / 分类 / 搜索 / 设置 / 关于）
```

## 🔗 链接

- GitHub：https://github.com/scpao5/aqzlgj-kotlin
- Gitee：https://gitee.com/scpao5/aqzlgj-kotlin
- 旧版仓库：https://github.com/scpao5/aqzlgj

## 📄 许可证

本项目采用 [GNU General Public License v3.0](LICENSE) 开源协议。

## 👤 作者

scpao5（是白白吖）
