# Live Hider — 设计文档

> 版本：v6（最终，可动手蓝本）
> 目标游戏：Minecraft **1.21.11** + **Fabric**（后期可选 NeoForge）
> 定位：面向直播安全的**「显示内容处理」客户端模组**。

---

## 1. 定位

- 一个 **Fabric 客户端模组**（Windows），分为两层：
  - **第一层（保留）**：obs-overlay 的 **overlay 隐藏**——借 overlay framebuffer + `wglSwapBuffers` 原生钩子，做「主播可见、观众不可见」的 HUD / 全屏界面隐藏。
  - **第二层（新增）**：本项目核心 **`SafeText` 内容重写管线**，把直播画面里可能引发风险/不想露的内容统一处理成安全形式。
- 因为上游 obs-overlay **已不再更新**，Live Hider 作为其**维护型延续**，完整保留并升级它原本的全部功能。
- **纯客户端**：不改任何服务端数据、不提供作弊性质的能力。

### 明确不做（Scope 之外）

- **不做第三方叠加层的通用适配**（MiniMap / CPS / FPS / Waypoint 等）。原因：各 mod 渲染方式不一（GuiGraphics / raw OpenGL / 自带 framebuffer / shader），没有通用入口可抓，「仅主播可见」更难。这些 mod 大多自带显示/隐藏快捷键，按需绑定即可，零开发。
- **不做服务端侧**任何改动。
- **不做跨加载器同步**的复杂抽象以外的工作（见 §5）。

---

## 2. 架构

本项目由**两层**组成：
- **Layer ①（保留）**：obs-overlay 的 overlay 隐藏层（framebuffer + swap 钩子），用于「主播可见、观众不可见」的隐藏。
- **Layer ②（新增）**：`SafeText` 重写层，用于把内容重写成安全形式（对玩家与观众一致）。

两者互补，可独立使用、也可叠加。

### 2.1 统一 `SafeText` 重写管线

这是核心资产，承载「名字匿名化」与「敏感词遮蔽」两类处理。

```java
enum Surface { NAMETAG, TAB, CHAT, CHAT_INPUT, SCOREBOARD, TITLE, ACTIONBAR, SERVER_NAME, ITEM_NAME }

interface SafeTextRewriter {
    // 以 surface 决定本轮重写规则（匿名名 / 遮蔽词 / 是否启用），返回安全文本
    MutableText rewrite(MutableText text, Surface surface);
}
```

- 输入：一段 `Text` + 上下文（`Surface`）。
- 输出：重写后的 `Text`，**保留原有样式（颜色/格式）与 click/hover 事件**。
- 内部流程：先做**名字匿名化**（该 surface 是否暴露玩家名），再做**敏感词遮蔽**（关键字/正则 + 服务器名/IP）。

### 2.2 名字匿名化映射

- **统一模板**，对所有玩家一致：`{可编辑标签} + '#' + 随机数字`。
  - 默认标签：`[Player]`；默认数字位数：4（可按玩家数自动加位防碰撞），均可配置。
- **每个玩家一个数字**，按 `UUID（或名字） + 局盐` 生成 → **每局/每次加入变、局内稳定**。
- 映射表记录 `真名 → 匿名名`；`SafeText` 只查表替换，不关心数字来源。
- **两级匹配**（避免子串误伤）：
  1. **整节点精确匹配**：聊天发送者名 / nametag / TAB 条目在组件树中是独立 `Text` 节点，内容恰好等于玩家名 → 整节点替换，零误伤。
  2. **词边界替换**：仅当名字被嵌入长句（`[ouker] joined`、actionbar、计分板行）时兜底；用 `\b名字\b`，并**优先长名**，避免 `Ouk` 覆盖 `Ouker`。
- 边缘情况：名字含正则特殊字符或为单字符时，降级为「只匹配整节点，不做过子串」，彻底避免误伤。

### 2.3 聊天输入框脱敏

- **只改「显示」、不改「真实字符串」**：在输入框渲染处对显示文本过一遍 `SafeText`，发送时仍用原始输入。
- 附加 **「按住显示真名」快捷键**：按住才临时显示原始输入，平时默认脱敏。既安全又可确认。

---

## 3. 功能

### 3.1 名字（主功能）

| 对象 | 行为 |
|---|---|
| 对方玩家 | **全局匿名**：nametag（头顶）/ TAB / 聊天 / 实体上方，统一模板 `[Player]+#随机数字` |
| 自己 | 三模式：**隐藏 / 自定义 nick / 随机 nick**（每局变） |
| 被改名实体（mob） | checkbox 开关「是否显示其 nametag」 |

### 3.2 物品显示名

- 被铁砧/命名牌改名的物品 → 显示其**绑定注册名**（如 `netherite_sword`），而不是 NBT 自定义名。
- 独立于通用 `rewrite`，走 `getHoverName()` 分支。

### 3.3 内容遮蔽引擎

- **关键字/正则列表 + 自动抓当前服务器 IP/名**。
- 作用于计分板 / Titles / actionbar 底边 / 服务器名·IP 等文本，把敏感内容替换为安全占位。
- 复用 `SafeText` 管线。

---

## 4. 工程 / 加载器

### 4.1 映射

- 使用 **Mojang Mappings**：`mappings loom.officialMojangMappings()`（1.21.11 的 Fabric **已弃用 Yarn**；与 NeoForge 一致）。
- 好处：Fabric 与 NeoForge 类名/方法名一致 → **公共 mixin 大幅可共享**，双加载器协作成本大降。
- 工具：`mappings.dev` / `Linkie` 查名；Loom 的 `migrateMappings` 任务可把 Yarn 源码一键迁到 Mojang 映射。

### 4.2 模块

- **Architectury 多加载器**：`common`（纯逻辑：SafeText、名字映射、遮蔽引擎）+ `fabric` + 后加 `neoforge`。
- **第一版只出 Fabric**，验证后再加 NeoForge（届时 common 逻辑已稳定，NeoForge 只是机械重写挂钩点）。

### 4.3 依赖（参照 1.21.11 端口）

- Minecraft `1.21.11`
- Fabric Loader `>=0.16.14`
- Java `>=21`
- Architectury API `>=19.0.1`
- Cloth Config `>=21.11.151`
- Fabric API `*`
- （可选）Mod Menu

---

## 5. 源基座与许可

- **完整保留 obs-overlay 原功能**（符合「维护延续」定位与 MIT 义务）：全部 17 个 HUD/世界内组件 + 全屏界面隐藏 + overlay 机制 + 对外 API。
- 以 **obs-overlay 1.21.4 源码**为底（功能最全，含世界内组件），用 Mojang 映射迁移到 1.21.11，参照 **1.21.11 端口的改法**（合成层改为 pipelines / screen-texture，mixin 跟上 `Gui` / `GuiRenderer` / `Minecraft` 命名）。
- **MIT 许可**，保留上游出处（zziger/obs-overlay）。
- 注：1.21.11 端口把「世界内组件」（箱子/牌子/地图/旗帜/信标/名牌）整体禁用。因我们以 1.21.4 源码为底，**这些世界内组件一并保留**，不随端口一起丢弃。

---

## 6. 里程碑

| 阶段 | 内容 |
|---|---|
| **M1** | 工程骨架 + obs-overlay 核心移植验证：1.21.11 Fabric + Mojang 映射 + Architectury；打通 overlay framebuffer + swap 钩子，复刻 1~2 个 HUD 组件验证「主播可见、观众不可见」 |
| **M2** | `SafeText` 管线 + 名字匿名（对方全局 / 自己三模式 / 实体开关 / 输入框脱敏 / 按住看真名） |
| **M3** | 物品名规范化 + 内容遮蔽引擎 + 自动抓服务器 IP/名 |
| **M4** | 补全 obs-overlay 全组件（含世界内组件）+ 配置 UI（Cloth Config）/ 快捷键 / Mod Menu |
| **M5** | 验证双加载器（NeoForge 模块）+ 发布 / 持续跟进上游 MC 版本 |

---

## 7. 待开发时细化的点

- 玩家上线/下线的映射更新时机；离线玩家名是否也脱敏。
- 随机数字位数与碰撞策略（按玩家数自动扩位）。
- 聊天发送者名在组件树中作为独立节点的精确匹配策略。
- 遮蔽引擎的服务器名/IP 自动抓取的具体 API（连接时取 `ServerData.address` 等）。

---

## 8. 已知问题与调试记录

### 8.1 计分板/文本匹配失效：Minecraft `§` 格式化代码（已确认根因）

**现象**：计分板顶框（`Objective.getDisplayName`）与每行名（`PlayerScoreEntry.ownerName`）的关键词/匿名匹配，在多数服务器失效。实测仅 Hoplite、Hypixel 的顶框可被替换，其余服务器与所有行名均不能。

**根因**：服务器在设置计分板标题/行名时普遍使用 `§` 格式化代码（如 `§aTotal Wins`、`§eip: hoplite.gg`）。`Component.literal("§a...")` 会**把 `§` 代码作为字面量留在文本里**（不自动解析成 Style）。因此：
- 项文本是 `§aTotal Wins`（含 `§a`）。
- 我们配置/映射的关键词是干净的 `Total Wins` → **匹配不上**。
- Hoplite/Hypixel 的 objective 显示名是**干净字符串**（无 `§`）→ `getDisplayName()` 匹配成功 → 可替换。这与观察完全吻合（也证明计分板 mixin 本体是挂上的，问题在文本内容）。

**修法（待实施）**：匹配/替换前**先剥离 `§` 格式化代码**（`§[0-9a-fk-orx]`，以及十六进制 `§x§...§...`），再做词边界匹配。应用于 `NameAnonymizer.applyToText` 与 `Redactor.replaceWords`/`combinedPattern`。剥离后：
- `§aTotal Wins` → `Total Wins` → 命中映射 → 替换为你指定的词。
- 未命中的 `§` 序列不影响其他内容。

### 8.2 边缘字符名（如 `.xqciisuuig`）匹配失效（已修复）
- `\b...\b` 词边界在 `.`（非字母数字）前不成立。已改为 `(?<!\w)...(?!\w)`，可匹配 `.xqciisuuig`、`cpvp.zip`、`hoplite.gg` 等含 `.`/特殊字符的名字，同时保留不误伤 `cocktail`/`fucking` 的效果。已打包验证。

### 8.3 计分板词映射语义（已实现）
- 计分板采用 `scoreboardReplacements` 映射：**关键词 → 指定替换词**（多对，仅计分板）。
- 顺序：**先映射（词→词），再 autoGrab/redactPatterns（→ `***`），最后名字匿名**。避免 autoGrab 的 `***` 覆盖掉用户映射词。
- 计分板**不套用** safetext 预设（避免 2673 词误伤计分板），只用精准映射 + autoGrab/redactPatterns + 名字匿名。
