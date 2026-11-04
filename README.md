# chat

Android 聊天应用。工程包含 app 模块、界面与接口设计文档。

## 目录内容

- `app/`
- `app-ui-design.md`
- `build.gradle.kts`
- `code-structure.md`
- `docs/`
- `fcbf40dec63397c16754b160592bccf9.jpg`
- `gradle/`
- `gradle.properties`
- `gradlew`
- `gradlew.bat`
- `key.properties`
- `key.properties.template`
- `keystore/`
- `local.properties`
- `prod.md`
- `progress.md`
- `requirements.md`
- `settings.gradle.kts`

## 开发检查

安装项目 Gradle 配置要求的 JDK 后，可使用工程自带的 Gradle Wrapper：

```sh
./gradlew test build
```

依赖尚未缓存时需要网络。Android 工程还需要本机 Android SDK 配置。
