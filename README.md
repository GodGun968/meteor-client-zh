
<p align="center">
<img src="https://meteorclient.com/icon.png" alt="meteor-client-logo" width="15%"/>
</p>

<h1 align="center">Meteor</h1>
<p align="center">A Minecraft Fabric Utility Mod for anarchy servers.</p>

<div align="center">
    <a href="https://discord.gg/bBGQZvd"><img src="https://img.shields.io/discord/689197705683140636?logo=discord" alt="Discord"/></a>
    <br>
    <img src="https://img.shields.io/github/last-commit/MeteorDevelopment/meteor-client" alt="GitHub last commit"/>
    <img src="https://img.shields.io/github/commit-activity/w/MeteorDevelopment/meteor-client" alt="GitHub commit activity"/>
    <img src="https://img.shields.io/github/contributors/MeteorDevelopment/meteor-client" alt="GitHub contributors"/>
    <br>
    <img src="https://img.shields.io/github/languages/code-size/MeteorDevelopment/meteor-client" alt="GitHub code size in bytes"/>
    <img src="https://img.shields.io/endpoint?url=https://ghloc.vercel.app/api/MeteorDevelopment/meteor-client/badge?filter=.java$&label=lines%20of%20code&color=blue" alt="GitHub lines of code"/>
</div>

## Usage

### Building
- Clone this repository
- Run `./gradlew build`

> 国内网络下 `plugins.gradle.org` / `maven.meteordev.org` 经常握手失败。可以先在 `~/.gradle/init.d/` 放一个把插件门户指向阿里云镜像的 init 脚本，或者直接多重试几次（依赖是有缓存累积的）。产物在 `build/libs/meteor-client-26.3-local.jar`。

### Installation
Follow the [guide](https://meteorclient.com/faq/installation) on the wiki.

## 本分支说明（简体中文汉化版）

这个分支把客户端界面全部改成了中文，并且修掉了「开自定义字体后中文不显示」的问题。

### 汉化

- 汉化表放在 `src/main/resources/assets/meteor-client/ui_zh.json`，分两部分：
  - `names`：以配置项/模块的 id（如 `velocity-height`、`auto-jump`）为键，值是界面上显示的标题；
  - `texts`：以英文原文整句为键，值是对应的中文。
- 加载与查询逻辑在 `meteordevelopment/meteorclient/utils/misc/Translate.java`，查不到时**原样返回英文**，所以不会出现空白或 `null`。
- 生效的位置：模块名/描述、配置项名/描述、配置分组标题、命令名/描述、HUD 元素名/描述、下拉框（枚举）选项、界面标签与按钮、聊天提示。
- 因为标题是在 `Utils.nameToTitle()` 里统一的，**模块 id、配置项 id、命令名仍然是英文**，`.toggle auto-jump` 这类命令和已有的配置文件都不受影响。
- `lang/` 下只保留 `zh_cn.json` 和 `en_us.json`（后者内容也改成中文，保证游戏语言不是简体中文时依然显示中文），其余语言文件已删除。

### 中文显示问题的修复

Meteor 自带四个字体（Comfortaa、JetBrains Mono、Tw Cen MT、Pixelation）都不含汉字，而 `Font` 原来只把拉丁字符集打进字体图集，所以开着自定义字体时中文会变成一片空白。

现在的做法：

1. `renderer/text/CjkFont.java`：从系统里找一个支持中文的字体，把 **GB2312 全部 6763 个汉字 + 符号** 打包进一张 4096×4096 的图集（40px，约 100–200 毫秒），所有字号共用这张图集；
2. `renderer/text/Font.java`：新增按码点数组打包的能力（`stbtt_PackFontRanges` 的 `array_of_unicode_codepoints`），并支持 TTC 字体集合（微软雅黑是 `.ttc`）；
3. `renderer/text/CustomTextRenderer.java`：渲染时按字形来源把字符串切成若干段，当前字体没有的字形走中文图集，两张图集各画一次，宽度计算同步走同一套逻辑。

系统字体按以下顺序查找，找到第一个含汉字且能解析的就用：

- Windows：`msyh.ttc`（微软雅黑）→ `NotoSansSC-VF.ttf`（思源黑体）→ `Deng.ttf`（等线）→ `simhei.ttf`（黑体）→ `simsun.ttc`（宋体），另外还会找 `%LOCALAPPDATA%\Microsoft\Windows\Fonts`
- macOS：`PingFang.ttc` → `STHeiti Medium.ttc`
- Linux：`NotoSansCJK-Regular.ttc` → `wqy-microhei.ttc` → `DroidSansFallbackFull.ttf`

如果系统里一个中文字体都没有，中文会退回原来的空白表现，并在日志里打印一行提示。

> 首次进游戏会让显存多占约 16 MB（中文图集），启动时间多约 0.2 秒。

## Contributions
We will review and help with all reasonable pull requests as long as the guidelines below are met.

- The license header must be applied to all java source code files.
- IDE or system-related files should be added to the `.gitignore`, never committed in pull requests.
- In general, check existing code to make sure your code matches relatively close to the code already in the project.
- Favour readability over compactness.
- If you need help, check out the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) for a reference.

## Bugs and Suggestions
Bug reports and suggestions should be made in this repo's [issue tracker](https://github.com/MeteorDevelopment/meteor-client/issues) using the templates provided.  
Please provide as much information as you can to best help us understand your issue and give a better chance of it being resolved.

## Donations
All of our work is completely free and non-profit (donations pay only for hosting costs), therefore we are very grateful for all donations made to support us in running our community.  
Donations can be made via our [website](https://meteorclient.com/donate) and the minimum amount to get donor benefits is €5.  
You will be rewarded with a role on our Discord server and a customisable in-game cape.  
⚠️ _Make sure to create a Meteor account and link your Discord and Minecraft accounts to fully experience your rewards._ ⚠️

## Credits
[Cabaletta](https://github.com/cabaletta) and [WagYourTail](https://github.com/wagyourtail) for [Baritone](https://github.com/cabaletta/baritone)  
The [Fabric Team](https://github.com/FabricMC) for [Fabric](https://github.com/FabricMC/fabric-loader) and [Yarn](https://github.com/FabricMC/yarn)

## Licensing
This project is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.en.html). 

If you use **ANY** code from the source:
- You must disclose the source code of your modified work and the source code you took from this project. This means you are not allowed to use code from this project (even partially) in a closed-source and/or obfuscated application.
- You must state clearly and obviously to all end users that you are using code from this project.
- Your application must also be licensed under the same license.

*If you have any other questions, check our [FAQ](https://meteorclient.com/faq) or ask in our [Discord](https://meteorclient.com/discord) server.*
