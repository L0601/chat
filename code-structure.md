# 代码结构说明

## 1. 项目概览

当前项目是一个基于 Android + Jetpack Compose 的单模块应用，核心目标是连接本地或局域网内的 LM Studio 服务，完成模型拉取、模型切换、流式聊天和本地配置保存。

当前代码整体分为 4 层：

- 应用入口层
- UI 展示层
- 数据访问层
- 数据模型层

整体调用链路比较清晰：

`Application / Activity` -> `ViewModel` -> `Repository` -> `LM Studio API / DataStore`

## 2. 目录结构

主要代码目录如下：

```text
app/src/main/java/com/example/lunadesk
├── LunaDeskApp.kt
├── MainActivity.kt
├── data
│   ├── AppContainer.kt
│   ├── local
│   │   └── SettingsRepository.kt
│   ├── model
│   │   └── ChatModels.kt
│   └── remote
│       └── LmStudioRepository.kt
└── ui
    ├── LunaDeskApp.kt
    ├── LunaDeskViewModel.kt
    ├── LunaDeskViewModelFactory.kt
    ├── screen
    │   ├── chat
    │   │   └── ChatScreen.kt
    │   └── settings
    │       └── SettingsScreen.kt
    └── theme
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## 3. 各模块职责

### 3.1 应用入口层

#### [LunaDeskApp.kt](/Users/ltc/chat/app/src/main/java/com/example/lunadesk/LunaDeskApp.kt)

职责：

- 作为 `Application` 入口。
- 在应用启动时初始化 `AppContainer`。
- 为整个应用提供统一依赖入口。

说明：

- 当前没有引入 DI 框架，例如 Hilt 或 Koin。
- 项目采用轻量级手工依赖注入，简单直接，适合当前规模。

#### [MainActivity.kt](/Users/ltc/chat/app/src/main/java/com/example/lunadesk/MainActivity.kt)

职责：

- 作为 Android 页面入口。
- 初始化 Compose 视图树。
- 通过 `LunaDeskViewModelFactory` 创建 `ViewModel`。
- 将 `ViewModel` 传给根 UI 组件。

说明：

- `MainActivity` 本身尽量保持轻量，不承载业务逻辑。
- 这一点是合理的，后续继续保持即可。

### 3.2 数据访问层

#### [AppContainer.kt](/Users/ltc/chat/app/src/main/java/com/example/lunadesk/data/AppContainer.kt)

职责：

- 统一创建和管理仓库对象。
- 初始化 `OkHttpClient`。
- 暴露：
  - `settingsRepository`
  - `lmStudioRepository`

说明：

- 当前所有依赖集中在这里组装。
- 这是项目的依赖装配中心，后面如果加行情、价格、历史记录、本地数据库，也应该继续从这里接出。

#### [SettingsRepository.kt](/Users/ltc/chat/app/src/main/java/com/example/lunadesk/data/local/SettingsRepository.kt)

职责：

- 负责本地配置的读取和保存。
- 底层使用 `DataStore Preferences`。

当前维护的配置项：

- `baseUrl`
- `selectedModel`
- `temperature`
- `maxTokens`

说明：

- 这是一个典型的本地配置仓库，不掺杂 UI 状态。
- `UserSettings` 作为本地配置对象，语义比较清晰。
- 如果后面增加“主题模式”“价格页偏好”“默认时间周期”等，也建议继续放在这里统一管理。

#### [LmStudioRepository.kt](/Users/ltc/chat/app/src/main/java/com/example/lunadesk/data/remote/LmStudioRepository.kt)

职责：

- 负责访问 LM Studio 接口。
- 处理：
  - 连接测试
  - 模型列表拉取
  - 模型切换
  - 流式聊天
  - 非流式降级兜底
  - 取消当前流式请求

核心方法说明：

- `testConnection`
  - 请求 `/v1/models` 验证服务可用性。
- `fetchModels`
  - 拉取模型列表。
- `loadModel`
  - 调用模型加载接口切换当前模型。
- `streamChat`
  - 发起 SSE/流式聊天请求。
  - 边读边解析 `data:` 事件。
  - 逐段向上层发出 `StreamChunk`。
- `cancelActiveChat`
  - 取消当前正在进行的聊天请求。

说明：

- 该类是当前最核心的业务仓库。
- 网络异常、人性化错误提示、流式降级都集中在这里处理，做法是对的。
- 如果未来支持 OpenAI、Ollama、Claude 等不同服务源，建议把这里再抽一层接口，而不是继续在一个类里累加分支。

### 3.3 数据模型层

#### [ChatModels.kt](/Users/ltc/chat/app/src/main/java/com/example/lunadesk/data/model/ChatModels.kt)

职责：

- 定义网络请求与响应模型。
- 定义聊天消息展示模型。
- 定义流式分片对象。

主要对象：

- `ModelInfo`
- `ModelsResponse`
- `LoadModelRequest`
- `ChatCompletionRequest`
- `ChatCompletionResponse`
- `ChatStreamResponse`
- `StreamChunk`
- `ChatMessageUi`

说明：

- 当前把接口模型和 UI 消息模型放在同一个文件里，规模小时问题不大。
- 如果后面模型继续增多，建议拆成：
  - `remote model`
  - `ui model`
  两类文件，降低混杂度。

### 3.4 UI 展示层

#### [ui/LunaDeskApp.kt](/Users/ltc/chat/app/src/main/java/com/example/lunadesk/ui/LunaDeskApp.kt)

职责：

- 整个 Compose UI 的根布局。
- 控制页面主框架与导航切换。
- 当前已改为：
  - 默认聊天页
  - 左侧抽屉导航
  - 设置页从侧边栏进入

说明：

- 这个文件负责“页面骨架”，不负责处理细节业务。
- 属于容器型 UI 组件。

#### [LunaDeskViewModel.kt](/Users/ltc/chat/app/src/main/java/com/example/lunadesk/ui/LunaDeskViewModel.kt)

职责：

- 管理全局 UI 状态。
- 协调界面层与仓库层。
- 承接所有用户动作。

主要职责包括：

- 页面切换
- 输入更新
- 保存配置
- 连接测试
- 拉取模型
- 切换模型
- 发送消息
- 停止生成
- 重置会话

说明：

- 当前 `LunaDeskUiState` 聚合了聊天页和设置页的所有状态。
- 对现阶段功能规模来说，这种集中式状态管理是合适的。
- 如果后面价格页、历史页、账户页继续增加，建议拆分成多个子状态或多个 ViewModel，避免单个 ViewModel 过胖。

#### [LunaDeskViewModelFactory.kt](/Users/ltc/chat/app/src/main/java/com/example/lunadesk/ui/LunaDeskViewModelFactory.kt)

职责：

- 为 `ViewModel` 提供容器依赖。

说明：

- 这是无 DI 框架情况下的标准写法。
- 保证 `MainActivity` 不直接 new 各种仓库对象。

#### [ChatScreen.kt](/Users/ltc/chat/app/src/main/java/com/example/lunadesk/ui/screen/chat/ChatScreen.kt)

职责：

- 渲染聊天页面。
- 展示消息列表、输入框、顶部栏和提示信息。
- 处理聊天页的视觉结构。

当前页面结构：

- 顶部操作栏
- Inline 提示条
- 消息列表区
- 底部输入区

说明：

- 该文件以展示逻辑为主，不直接做网络调用。
- 聊天页的键盘适配、底部输入栏高度控制、消息自动滚动都在这里完成。

#### [SettingsScreen.kt](/Users/ltc/chat/app/src/main/java/com/example/lunadesk/ui/screen/settings/SettingsScreen.kt)

职责：

- 渲染设置页面。
- 配置服务地址、温度、最大输出。
- 提供连接测试、拉取模型、切换模型、重置上下文等操作入口。

说明：

- 当前设置页已经承接“重置上下文”能力，和聊天主页面做了职责隔离。
- 后续如有更多设置项，可以继续按卡片分块增加。

#### `ui/theme`

职责：

- 管理 Compose 主题、颜色、排版。

说明：

- 当前主题层较轻。
- 如果后面要继续强化品牌风格、支持浅深模式或页面级主题统一，这里会成为重要扩展点。

## 4. 状态流转说明

当前最核心的状态流转是：

1. 用户在界面上操作。
2. UI 调用 `ViewModel` 方法。
3. `ViewModel` 更新 `MutableStateFlow`。
4. 或者 `ViewModel` 调用 Repository 获取数据。
5. Repository 返回结果后，`ViewModel` 再更新 UI 状态。
6. Compose 根据状态自动重组页面。

以发送消息为例：

1. 用户输入内容并点击发送。
2. `ChatScreen` 调用 `onSend`。
3. `LunaDeskViewModel.sendMessage()` 校验输入与配置。
4. 构造用户消息和助手占位消息。
5. 调用 `LmStudioRepository.streamChat()`。
6. 仓库持续返回 `StreamChunk`。
7. `ViewModel` 把增量内容拼接到助手消息上。
8. Compose 自动刷新消息列表。

## 5. 当前结构的优点

- 分层比较明确，UI、数据访问、本地配置职责边界清楚。
- `ViewModel + StateFlow` 的状态管理路径清晰。
- 仓库层没有和 Compose 强耦合，后续便于替换实现。
- 目前项目规模不大，手工依赖注入比引入完整 DI 框架更轻。

## 6. 当前结构的改进建议

### 6.1 ViewModel 未来可能变胖

目前 `LunaDeskViewModel` 同时承担：

- 聊天状态
- 设置状态
- 导航状态

后面如果接入价格页、历史页、收藏、自选、公告、提醒等能力，建议逐步拆分。

建议方向：

- `ChatViewModel`
- `SettingsViewModel`
- `PriceViewModel`

或者至少先拆分子状态对象。

### 6.2 模型定义文件后面可以拆分

`ChatModels.kt` 当前承载的对象种类偏多。

建议后续根据规模拆成：

- `api_models.kt`
- `chat_ui_models.kt`
- `stream_models.kt`

这样更利于维护。

### 6.3 Repository 后面可以抽接口

如果未来不止一个模型服务源，建议定义统一接口，例如：

```kotlin
interface ChatRepository
interface ModelRepository
```

然后让 `LmStudioRepository` 成为其中一个实现，避免服务类型一多就把一个仓库写成大杂烩。

### 6.4 文档和代码同步维护

当前已经有：

- [progress.md](/Users/ltc/chat/progress.md)
- [price-ui-design.md](/Users/ltc/chat/price-ui-design.md)

建议后续每次做较大结构调整时，同步更新这份结构文档，避免代码变了文档还停留在旧版本。

## 7. 建议的新增模块方向

如果后面继续扩展，我建议优先按下面方式生长：

### 7.1 历史会话模块

- `data/local/chat`
- `ui/screen/history`

### 7.2 价格/行情模块

- `data/remote/market`
- `data/model/market`
- `ui/screen/price`

### 7.3 通用组件模块

可以逐步增加：

- `ui/component`

把复用组件沉淀进去，例如：

- 顶部栏
- 输入栏
- 状态卡片
- 空状态组件
- 标准列表行

## 8. 总结

当前代码结构总体是健康的，适合继续快速迭代：

- 入口清晰
- 状态集中
- 仓库职责明确
- UI 结构已经开始向页面骨架化演进

下一阶段如果功能继续扩张，重点不是推翻重写，而是顺着现在这套结构继续做“按领域拆分”，这样成本最低，也最稳。
