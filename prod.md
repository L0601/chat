可以，下面给你一份**可直接复制**的 Markdown 技术方案。
先说明几个已确认的关键事实：

* **LM Studio** 同时提供 **OpenAI 兼容接口**（如 `GET /v1/models`、`POST /v1/chat/completions`）和原生 **REST v1 接口**；原生接口里有 **`POST /api/v1/models/load`** 用于显式加载/切换模型，且支持 `context_length`、`flash_attention` 等加载参数。([LM Studio][1])
* Android 官方当前推荐的应用架构至少包含 **UI layer（界面层）** 和 **Data layer（数据层）**；APK / AAB 的构建由 Android Gradle Build System 负责。([Android Developers][2])
* **GitHub Actions** 官方支持用工作流自动执行 CI/CD，因此很适合做 Android APK 的自动打包。([GitHub Docs][3])

下面就是正文，你可以整段复制到 `.md` 文件里：

````markdown
# LunaDesk 技术方案（Android 本地局域网 LLM 客户端）

## 1. 项目概述

### 1.1 项目目标
开发一个 **Android 本地安装使用** 的局域网大模型客户端，支持以下能力：

- 配置局域网内模型服务地址
- 通过 **OpenAI 兼容接口** 发起聊天
- 拉取模型列表并选择模型
- 使用 **LM Studio 的 `/api/v1/models/load`** 同步切换模型
- 切换模型完成后，拿到服务端成功响应，再提示用户“模型已切换”
- 支持常用推理参数配置
- 默认支持流式 SSE 响应，并兼容非流式降级
- 默认 **不保留聊天记录到本地**
- 支持 GitHub Actions 自动打包 APK

### 1.2 适用场景
- 本地家庭/办公局域网调用 LM Studio
- 不需要分发到应用商店
- 以“尽快可用、尽量少踩坑、维护简单”为优先目标

### 1.3 非目标
本项目第一阶段 **不做**：

- 用户登录
- 云端同步
- 本地数据库保存聊天历史
- 多会话持久化
- WebSocket
- 多端同步
- 插件系统
- Markdown 富文本编辑器
- 多模块复杂架构
- 复杂权限系统

---

## 2. 项目名称与图标方案

### 2.1 推荐名称
**LunaDesk**

命名含义：
- **Luna**：月亮、安静、本地、陪伴感
- **Desk**：桌面侧、本地工作台、工具属性强

适合作为：
- Android App 名
- GitHub 仓库名
- 包名映射前缀

### 2.2 备选名称
- LocalMuse
- LANChat
- PocketLLM
- HomeModel
- LocalBridge

### 2.3 最终建议
如果你希望：
- 名字简洁
- 不土
- 工具感强
- 不像玩具项目

建议使用：

**LunaDesk**

### 2.4 图标设计方案
风格建议：

- 深蓝 / 靛蓝主色
- 一个简洁圆角机器人头
- 底部一条对话气泡线
- 不要写实
- 偏工具型、现代感
- 避免太花

### 2.5 图标生成提示词（给图像模型）
可用于图标生成：

```text
Create a clean Android app icon for an AI local network chat client called "LunaDesk".
Style: minimal, modern, flat design.
Main subject: a rounded robot head combined with a chat bubble.
Colors: dark blue, indigo, white.
Background: simple gradient or solid dark blue.
Mood: calm, professional, technical, local-first.
No text.
Centered composition.
Suitable for Android launcher icon.
````

### 2.6 启动图标资源建议

最终落地建议：

* `ic_launcher_foreground.xml` 使用矢量图或 PNG
* `ic_launcher_background.xml` 使用纯色或简单渐变
* Android Studio Image Asset 自动生成各密度图标

---

## 3. 技术栈选型

## 3.1 总体选择

为保证“越快越好”，采用：

* **语言**：Kotlin
* **UI**：Jetpack Compose
* **架构**：MVVM
* **并发**：Kotlin Coroutines + Flow
* **网络**：OkHttp
* **JSON**：kotlinx.serialization
* **配置存储**：DataStore
* **依赖注入**：第一版不使用 Hilt，手动注入
* **持久化聊天记录**：不做

## 3.2 为什么不用 Retrofit

本项目推荐直接使用 **OkHttp**，原因：

* 需要兼容 OpenAI 风格接口
* 后续要支持流式 SSE
* LM Studio 还会有 `/api/v1/models/load`
* 手动控制 Header、Timeout、流式读取更灵活
* 第一版直接手写请求更快

## 3.3 为什么不用 Room

因为需求明确：

* 聊天记录不用保留本地
* 不需要本地消息数据库
* 不需要复杂查询

只保留少量配置，用 **DataStore** 就够了。

---

## 4. 功能范围

## 4.1 第一阶段功能

* 配置服务地址（例如 `http://192.168.31.30:1234`）
* 获取模型列表
* 模型选择
* 同步切换模型
* 聊天请求发送
* 流式返回展示
* 非流式兼容降级
* 配置常用参数
* 展示请求中状态
* 展示模型切换中状态
* 错误提示
* 保留基础配置到本地
* GitHub Actions 自动构建 APK

## 4.2 第二阶段预留功能

* 停止生成
* 系统提示词模板
* 参数预设
* 深色主题优化
* 会话导出
* 响应复制
* token / TTFT / TPS 调试信息

---

## 5. 系统架构

## 5.1 分层结构

采用轻量分层：

* **UI Layer**

  * Compose 页面
  * ViewModel
  * UI State

* **Data Layer**

  * API Client
  * Repository
  * Settings DataStore

### 5.1.1 不引入 Domain Layer 的原因

项目规模较小，Domain Layer 会增加样板代码。第一版先省略。

## 5.2 模块结构

采用单模块 `app`，目录建议如下：

```text
app/
  src/main/java/com/example/lunadesk/
    MainActivity.kt
    LunaDeskApp.kt

    ui/
      navigation/
        AppNavHost.kt
      screen/
        chat/
          ChatScreen.kt
          ChatViewModel.kt
          ChatUiState.kt
        settings/
          SettingsScreen.kt
          SettingsViewModel.kt
          SettingsUiState.kt
      component/
        TopBar.kt
        MessageBubble.kt
        LoadingDialog.kt
        ModelSwitchDialog.kt

    data/
      api/
        OpenAiApiClient.kt
        LmStudioApiClient.kt
      model/
        ChatDtos.kt
        ModelDtos.kt
        SettingsDtos.kt
      repository/
        ChatRepository.kt
        ChatRepositoryImpl.kt
        ModelRepository.kt
        ModelRepositoryImpl.kt
        SettingsRepository.kt
        SettingsRepositoryImpl.kt
      local/
        AppPreferences.kt

    common/
      Result.kt
      DispatchersProvider.kt
      NetworkError.kt
      Extensions.kt
```

---

## 6. 接口设计

## 6.1 服务地址规则

统一由用户配置：

```text
http://192.168.31.30:1234
```

应用内部派生出：

* `GET {baseUrl}/v1/models`
* `POST {baseUrl}/v1/chat/completions`
* `POST {baseUrl}/api/v1/models/load`

注意：

* 不要在代码里写死 host/port
* 所有请求都由 `baseUrl` 拼出

---

## 6.2 OpenAI 兼容：获取模型列表

### 请求

```http
GET /v1/models
```

### 用途

* 展示可选模型
* 作为聊天时 `model` 的候选来源

### Kotlin 数据结构

```kotlin
@Serializable
data class OpenAiModelsResponse(
    val data: List<OpenAiModelItem> = emptyList()
)

@Serializable
data class OpenAiModelItem(
    val id: String,
    val `object`: String? = null,
    val owned_by: String? = null
)
```

---

## 6.3 OpenAI 兼容：聊天接口

### 请求

```http
POST /v1/chat/completions
Content-Type: application/json
```

### 请求体

```json
{
  "model": "qwen/qwen3.5-9b",
  "messages": [
    {
      "role": "user",
      "content": "Hello"
    }
  ],
  "reasoning_effort": "none",
  "max_tokens": 256,
  "temperature": 0.2,
  "stream": false
}
```

### Kotlin 数据结构

```kotlin
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val reasoning_effort: String? = null,
    val max_tokens: Int? = null,
    val temperature: Double? = null,
    val top_p: Double? = null,
    val stream: Boolean = false
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String
)
```

### 响应体

```kotlin
@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ChatChoice(
    val index: Int? = null,
    val message: ChatAssistantMessage? = null,
    val finish_reason: String? = null
)

@Serializable
data class ChatAssistantMessage(
    val role: String? = null,
    val content: String? = null
)
```

---

## 6.4 LM Studio：同步切换模型

### 请求

```http
POST /api/v1/models/load
Content-Type: application/json
```

### 第一版请求体

```json
{
  "model": "qwen/qwen3.5-9b"
}
```

### 可扩展请求体

```json
{
  "model": "qwen/qwen3.5-9b",
  "context_length": 8192,
  "flash_attention": true
}
```

### Kotlin 数据结构

```kotlin
@Serializable
data class LoadModelRequest(
    val model: String,
    val context_length: Int? = null,
    val flash_attention: Boolean? = null
)
```

### 响应结构

LM Studio 文档未要求你必须依赖某个复杂对象结构。第一版建议采用“宽松解析”：

```kotlin
@Serializable
data class LoadModelResponse(
    val model: String? = null,
    val status: String? = null,
    val instance_id: String? = null
)
```

如果实际返回字段不稳定，则可先保留原始字符串作为 fallback。

---

## 7. 关键业务流程

## 7.1 启动流程

1. 应用启动
2. 加载本地配置
3. 进入聊天页
4. 如果已配置 `baseUrl`，自动拉一次模型列表
5. 若失败，显示轻提示，不阻塞界面

## 7.2 获取模型列表流程

1. 用户点击“刷新模型”
2. 调用 `GET /v1/models`
3. 成功则更新可选模型列表
4. 失败则 toast/snackbar 提示

## 7.3 切换模型流程（重点）

### 目标

必须做到：

* 用户点击某个模型
* 发起 `/api/v1/models/load`
* **等待服务端成功响应**
* 拿到响应后，才提示“模型已切换”
* 切换期间禁止再次点击切模
* 切换成功后同步更新当前模型状态

### 详细流程

1. 用户在模型选择器中点击目标模型
2. 如果目标模型就是当前模型，直接关闭弹窗，不发请求
3. ViewModel 设置 `isSwitchingModel = true`
4. 弹出 loading 状态：“正在切换模型…”
5. 调用 `POST /api/v1/models/load`
6. 请求成功：

   * 更新 `selectedModel`
   * 更新 DataStore 中的 `selectedModel`
   * 关闭 loading
   * 提示“模型已切换为 xxx”
7. 请求失败：

   * 关闭 loading
   * 提示错误
   * 保持旧模型不变

### 切模伪代码

```kotlin
fun switchModel(targetModel: String) {
    if (targetModel == uiState.selectedModel) return

    viewModelScope.launch {
        updateState { copy(isSwitchingModel = true, modelSwitchError = null) }

        val result = modelRepository.loadModel(
            model = targetModel,
            contextLength = uiState.contextLength,
            flashAttention = uiState.flashAttention
        )

        result.onSuccess {
            settingsRepository.saveSelectedModel(targetModel)
            updateState {
                copy(
                    selectedModel = targetModel,
                    isSwitchingModel = false,
                    modelSwitchSuccessMessage = "模型已切换：$targetModel"
                )
            }
        }.onFailure { error ->
            updateState {
                copy(
                    isSwitchingModel = false,
                    modelSwitchError = error.message ?: "模型切换失败"
                )
            }
        }
    }
}
```

---

## 7.4 聊天流程

1. 用户输入文本
2. 点击发送
3. 当前输入作为 user message 插入 UI
4. 立即插入一个 assistant 占位消息（空）
5. 发起 `/v1/chat/completions`
6. 成功后把 assistant 占位消息替换为真实文本
7. 失败则将该 assistant 消息替换为错误提示或移除

### 注意

第一版默认使用：

* `stream = true`

当服务端不支持流式或流式请求失败时，可降级为非流式请求。

---

## 8. UI 设计

## 8.1 页面结构

建议只做两个页面：

* **聊天页**
* **设置页**

## 8.2 聊天页功能

* 顶部栏显示当前模型
* 模型切换按钮
* 聊天消息列表
* 底部输入框
* 发送按钮
* 请求中 loading
* 模型切换中 loading
* 错误 snackbar

## 8.3 设置页功能

* Base URL 输入框
* 请求超时设置
* temperature
* top_p
* max_tokens
* reasoning_effort
* context_length
* flash_attention 开关
* 测试连接按钮
* 刷新模型列表按钮

## 8.4 聊天页状态定义

```kotlin
data class ChatUiState(
    val input: String = "",
    val messages: List<UiMessage> = emptyList(),
    val selectedModel: String = "",
    val availableModels: List<String> = emptyList(),

    val isSending: Boolean = false,
    val isRefreshingModels: Boolean = false,
    val isSwitchingModel: Boolean = false,

    val modelSwitchSuccessMessage: String? = null,
    val modelSwitchError: String? = null,
    val requestError: String? = null,

    val temperature: Double = 0.2,
    val topP: Double = 1.0,
    val maxTokens: Int = 256,
    val reasoningEffort: String = "none",

    val contextLength: Int? = null,
    val flashAttention: Boolean = false
)
```

### UI Message

```kotlin
data class UiMessage(
    val id: String,
    val role: String,
    val content: String
)
```

---

## 9. 本地配置存储

## 9.1 保存内容

通过 DataStore 保存：

* `baseUrl`
* `selectedModel`
* `temperature`
* `topP`
* `maxTokens`
* `reasoningEffort`
* `contextLength`
* `flashAttention`
* `connectTimeoutSeconds`
* `readTimeoutSeconds`

## 9.2 不保存内容

明确不保存：

* 聊天历史
* 会话记录
* 响应内容
* 用户输入草稿（可选）

## 9.3 Preferences Key 方案

```kotlin
object PreferenceKeys {
    val BASE_URL = stringPreferencesKey("base_url")
    val SELECTED_MODEL = stringPreferencesKey("selected_model")
    val TEMPERATURE = doublePreferencesKey("temperature")
    val TOP_P = doublePreferencesKey("top_p")
    val MAX_TOKENS = intPreferencesKey("max_tokens")
    val REASONING_EFFORT = stringPreferencesKey("reasoning_effort")
    val CONTEXT_LENGTH = intPreferencesKey("context_length")
    val FLASH_ATTENTION = booleanPreferencesKey("flash_attention")
    val CONNECT_TIMEOUT_SECONDS = intPreferencesKey("connect_timeout_seconds")
    val READ_TIMEOUT_SECONDS = intPreferencesKey("read_timeout_seconds")
}
```

> 注：`doublePreferencesKey` 没有现成实现时，可转 `String` 存。

---

## 10. 网络层设计

## 10.1 OkHttpClient 配置

```kotlin
fun provideOkHttpClient(
    connectTimeoutSeconds: Long,
    readTimeoutSeconds: Long
): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()
}
```

## 10.2 Content-Type

所有 JSON 请求统一：

```http
Content-Type: application/json
```

## 10.3 可选认证

如果后续 LM Studio Server 开启 API Token，则增加：

```http
Authorization: Bearer <token>
```

第一版先不做认证输入。

---

## 11. Repository 设计

## 11.1 ChatRepository

```kotlin
interface ChatRepository {
    suspend fun sendChat(
        baseUrl: String,
        request: ChatCompletionRequest
    ): Result<ChatCompletionResponse>
}
```

## 11.2 ModelRepository

```kotlin
interface ModelRepository {
    suspend fun fetchModels(baseUrl: String): Result<List<String>>

    suspend fun loadModel(
        model: String,
        contextLength: Int?,
        flashAttention: Boolean
    ): Result<LoadModelResponse>
}
```

## 11.3 SettingsRepository

```kotlin
interface SettingsRepository {
    suspend fun saveBaseUrl(baseUrl: String)
    suspend fun saveSelectedModel(model: String)
    suspend fun saveInferenceParams(
        temperature: Double,
        topP: Double,
        maxTokens: Int,
        reasoningEffort: String
    )

    fun observeSettings(): Flow<AppSettings>
}
```

---

## 12. API Client 设计

## 12.1 OpenAiApiClient

职责：

* `GET /v1/models`
* `POST /v1/chat/completions`

```kotlin
class OpenAiApiClient(
    private val client: OkHttpClient,
    private val json: Json
) {
    suspend fun fetchModels(baseUrl: String): OpenAiModelsResponse { ... }

    suspend fun chat(
        baseUrl: String,
        request: ChatCompletionRequest
    ): ChatCompletionResponse { ... }
}
```

## 12.2 LmStudioApiClient

职责：

* `POST /api/v1/models/load`

```kotlin
class LmStudioApiClient(
    private val client: OkHttpClient,
    private val json: Json
) {
    suspend fun loadModel(
        baseUrl: String,
        request: LoadModelRequest
    ): LoadModelResponse { ... }
}
```

---

## 13. 错误处理设计

## 13.1 错误分类

建议统一分类：

* 网络错误
* HTTP 状态码错误
* JSON 解析错误
* 服务端业务错误
* 用户输入错误
* 模型切换错误

## 13.2 错误映射

```kotlin
sealed class AppError(message: String) : Throwable(message) {
    class Network(message: String = "网络连接失败") : AppError(message)
    class Http(val code: Int, message: String = "请求失败") : AppError(message)
    class Parse(message: String = "响应解析失败") : AppError(message)
    class Business(message: String = "服务端返回异常") : AppError(message)
    class Validation(message: String = "输入不合法") : AppError(message)
}
```

## 13.3 UI 提示规则

* 模型切换成功：Snackbar
* 模型切换失败：Snackbar / Dialog
* 聊天失败：消息区域错误或 Snackbar
* Base URL 为空：表单错误提示
* 请求超时：明确提示“请求超时，请检查模型是否正在加载或服务是否可用”

---

## 14. Android 特殊配置

## 14.1 Manifest 权限

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 14.2 明文 HTTP 配置

由于局域网通常直接用 `http://192.168.x.x:1234`，需要允许 cleartext traffic。

### AndroidManifest.xml

```xml
<application
    android:name=".LunaDeskApp"
    android:usesCleartextTraffic="true"
    android:networkSecurityConfig="@xml/network_security_config"
    ... />
```

### `res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
```

> 如果后续你想更严格，可以改为只对局域网特定域名 / IP 放开。

---

## 15. Gradle 依赖建议

## 15.1 `libs.versions.toml`

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.21"
compose-bom = "2024.09.03"
activity-compose = "1.9.2"
lifecycle = "2.8.6"
coroutines = "1.8.1"
okhttp = "4.12.0"
serialization = "1.7.3"
datastore = "1.1.1"

[libraries]
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }

kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
```

## 15.2 `app/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.lunadesk"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.lunadesk"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
```

---

## 16. ViewModel 交互接口设计

## 16.1 ChatViewModel

### 对外方法

```kotlin
fun onInputChange(value: String)
fun sendMessage()
fun refreshModels()
fun selectModel(model: String)
fun dismissSnackbar()
fun updateTemperature(value: Double)
fun updateTopP(value: Double)
fun updateMaxTokens(value: Int)
fun updateReasoningEffort(value: String)
```

## 16.2 SettingsViewModel

### 对外方法

```kotlin
fun updateBaseUrl(value: String)
fun updateContextLength(value: String)
fun updateFlashAttention(value: Boolean)
fun updateTimeout(connect: Int, read: Int)
fun testConnection()
fun saveSettings()
```

---

## 17. 同步切模实现要求（必须满足）

这一节是强约束，Codex 编码时必须满足：

### 17.1 行为要求

* 切模是显式动作，不自动在聊天时切换
* 必须调用 `/api/v1/models/load`
* 必须等待接口响应成功再视为切换完成
* 切换中不允许再次切换
* 切换中不允许发送聊天（第一版建议禁止）
* 切换失败时当前模型保持不变
* 成功切换后立即更新顶部栏显示

### 17.2 状态机

```text
Idle
  -> Switching
      -> Success
      -> Failure
```

### 17.3 UI 表现

* 切换中：显示 modal loading
* 成功：toast/snackbar “已切换到 xxx”
* 失败：toast/snackbar “切换失败：xxx”

---

## 18. 流式响应设计（第一阶段）

第一版直接实现 `stream=true`，并保留非流式兼容能力：

## 18.1 预留接口

```kotlin
interface StreamingChatRepository {
    fun sendChatStream(
        baseUrl: String,
        request: ChatCompletionRequest
    ): Flow<StreamChunk>
}
```

## 18.2 StreamChunk

```kotlin
data class StreamChunk(
    val delta: String? = null,
    val done: Boolean = false,
    val error: String? = null
)
```

## 18.3 交互要求

* 发送消息后，先插入 assistant 占位消息
* 首个 delta 到达前展示“正在生成”
* 收到流式增量后实时拼接并刷新 UI
* 收到完成事件后结束生成状态
* 出错时保留当前内容并提示错误
* 为后续“停止生成”预留请求取消能力

---

## 19. GitHub Actions 自动打包方案

## 19.1 目标

每次 push 到 `main` 或手动触发 workflow 时：

* 自动 checkout 代码
* 安装 JDK
* 安装 Android SDK 构建环境
* 执行 Gradle 构建
* 输出 Debug APK
* 将 APK 上传为 Artifact

## 19.2 工作流文件

路径：

```text
.github/workflows/android-build.yml
```

## 19.3 示例工作流

```yaml
name: Android Build

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x ./gradlew

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: lunadesk-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

## 19.4 后续可扩展

后续如需生成 release 包，可增加：

* keystore secret
* release signing config
* `assembleRelease`
* 上传 release APK / AAB

---

## 20. GitHub 仓库结构建议

```text
LunaDesk/
  .github/
    workflows/
      android-build.yml
  app/
  gradle/
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  README.md
```

---

## 21. README 建议内容

README 至少包含：

* 项目简介
* 截图占位
* 技术栈
* 最低 Android 版本
* 如何配置 LM Studio
* 如何运行
* GitHub Actions 构建说明

### README 关键说明

#### LM Studio 端准备

1. 在 LM Studio 中启动本地服务器
2. 保证安卓手机与电脑在同一局域网
3. 服务监听端口例如 `1234`
4. 手机访问 `http://<电脑局域网IP>:1234/v1/models`
5. 若可访问，则应用可接入

---

## 22. 代码实现优先级

## P0：必须先完成

* App 启动与导航
* 设置页
* 保存 Base URL
* 拉模型列表
* 模型选择
* `/api/v1/models/load` 同步切模
* `/v1/chat/completions` 流式聊天
* 错误提示
* GitHub Actions Debug APK 打包

## P1：随后优化

* 更好的消息列表样式
* 发送中禁用输入
* 连接测试
* 参数输入校验
* Loading 对话框
* 请求日志开关

## P2：后续扩展

* 停止生成
* 多轮上下文策略
* 响应复制
* 导出会话

---

## 23. 测试计划

## 23.1 功能测试

* 能否保存 Base URL
* 能否拉取模型列表
* 是否能成功切模
* 切模成功后是否有提示
* 切模失败后是否保持旧模型
* 能否发送聊天请求
* 请求失败时是否正确提示

## 23.2 边界测试

* Base URL 为空
* Base URL 末尾带 `/`
* 局域网服务不可达
* 模型列表为空
* 模型切换超时
* 聊天接口超时
* JSON 返回不完整

## 23.3 真机测试

至少测试：

* Android 10+
* Android 13+
* Android 14/15+

---

## 24. Codex 编码约束

请直接按以下约束编码：

1. 使用 Kotlin + Jetpack Compose
2. 使用 MVVM
3. 网络层使用 OkHttp，不使用 Retrofit
4. 配置存储使用 DataStore
5. 第一版不接入数据库
6. 第一版不做聊天记录持久化
7. 所有接口地址基于用户输入的 `baseUrl`
8. 模型切换必须通过 `/api/v1/models/load`
9. 切模必须是同步等待成功响应后再提示
10. GitHub Actions 必须能生成 Debug APK Artifact
11. 第一版聊天默认使用 `stream=true` SSE
12. 所有 UI 文案先写中文
13. 出错信息尽量人类可读
14. 避免引入 Hilt / Room / Retrofit / 多模块复杂架构

---

## 25. 最小可交付版本定义（MVP）

MVP 完成标准：

* 可以输入 LM Studio 局域网地址
* 可以获取模型列表
* 可以点击模型并同步切换
* 切换成功后看到提示
* 可以发送一条消息并收到回答
* 回答内容可以流式增量展示
* 可以调整温度和最大输出参数
* 可以通过 GitHub Actions 自动打一个 Debug APK

---

## 26. 后续升级路线图

### V0.2

* 停止生成
* 更好看的消息列表
* 支持 system prompt

### V0.3

* 最近配置模板
* 复制回复
* 性能调试面板
* 更细的切模参数

### V0.4

* 会话导出
* 多服务器配置
* 模型参数预设方案

---

## 27. 结论

本项目建议采用：

* **Kotlin + Compose + MVVM + Coroutines + OkHttp + DataStore**

这是当前“开发最快、维护简单、最适合本地局域网 LLM 客户端”的方案。

核心实现重点只有两个：

1. **用 OpenAI 兼容接口聊天**
2. **用 LM Studio `/api/v1/models/load` 同步切换模型**

其余设计全部围绕“尽快做成一个稳定可用的小工具”展开，避免过度工程化。

```

你要的话，我下一条可以继续给你两份内容里的任意一份：

1. **基于这份方案的完整项目目录 + Kotlin 代码骨架**  
2. **专门给 Codex 的“执行指令版 prompt”**，让它按这个方案直接开写。
::contentReference[oaicite:3]{index=3}
```

[1]: https://lmstudio.ai/docs/developer/rest/load?utm_source=chatgpt.com "Load a model | LM Studio Docs"
[2]: https://developer.android.com/topic/architecture?utm_source=chatgpt.com "Guide to app architecture - Android Developers"
[3]: https://docs.github.com/actions?utm_source=chatgpt.com "GitHub Actions documentation"
