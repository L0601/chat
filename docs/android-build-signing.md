# LunaDesk Android 签名与版本发布

## Debug 构建

项目将固定的公开 debug 证书 `keystore/lunadesk-debug.keystore` 纳入仓库。本机和 GitHub Actions 构建 debug APK 时自动使用该证书，不依赖本机默认的 `~/.android/debug.keystore`，也不需要配置 GitHub Secrets。

这能保证不同开发机器和 CI 产出的 debug APK 签名一致。由于证书从原私有证书切换为公开 debug 证书，手机上已有旧版本需要先卸载一次；安装新 debug APK 后，后续 CI 产物可以持续覆盖升级。

公开 debug 证书只用于开发与测试，不得用于正式发布。

## Release 构建

release 继续使用私有 `keystore/lunadesk.jks`，并通过根目录 `key.properties` 提供：

```properties
storeFile=keystore/lunadesk.jks
storePassword=***
keyAlias=lunadesk
keyPassword=***
```

这两个私有文件均被 Git 忽略。执行 release 打包任务时如果缺少私有签名，Gradle 会直接失败，避免误生成无法正式发布的未签名 APK。

## 版本与证书

- `applicationId`: `com.example.lunadesk`
- `versionCode`: `700000001`
- `versionName`: `0.2.0`
- Debug 证书 SHA-256: `A3:3E:71:84:1B:3A:E3:53:98:0F:7C:6C:D4:98:9C:F9:7C:28:AC:43:F5:67:10:F4:FD:53:9E:DF:6C:D6:2A:9A`
- Release 证书 SHA-256: `88:C3:71:AE:1A:25:49:F1:0F:B9:65:28:F4:ED:67:32:D5:73:63:69:9E:35:5A:DD:76:A4:9F:39:24:FC:A4:E5`

每次发布必须显式增加 `versionCode`。`BUILD_TAG` 只用于界面识别构建时间，不参与 Android 的升级排序。

Android 只允许相同 `applicationId` 且签名一致的 APK 覆盖安装。debug 与 release 使用不同证书，因此二者不能互相覆盖；GitHub Actions 与本地 debug 构建之间可以互相覆盖。

## 验证命令

```bash
./gradlew testDebugUnitTest assembleDebug signingReport
apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk
```
