# LunaDesk Android 签名与版本发布

## 本地构建

项目继续使用私有 `keystore/lunadesk.jks`，并通过根目录 `key.properties` 提供：

```properties
storeFile=keystore/lunadesk.jks
storePassword=***
keyAlias=lunadesk
keyPassword=***
```

这两个文件均被 Git 忽略。执行 APK 打包任务时如果缺少稳定签名，Gradle 会直接失败，避免生成无法覆盖升级的临时签名包。

## CI Secrets

GitHub 仓库需要配置：

- `LUNADESK_KEYSTORE_BASE64`
- `LUNADESK_STORE_PASSWORD`
- `LUNADESK_KEY_ALIAS`
- `LUNADESK_KEY_PASSWORD`

`LUNADESK_KEYSTORE_BASE64` 是 `lunadesk.jks` 的 Base64 内容。Workflow 会将其还原到 Runner 临时目录，不写入仓库或构建产物。

## 版本与证书

- `applicationId`: `com.example.lunadesk`
- `versionCode`: `700000001`
- `versionName`: `0.2.0`
- 证书 SHA-256: `88:C3:71:AE:1A:25:49:F1:0F:B9:65:28:F4:ED:67:32:D5:73:63:69:9E:35:5A:DD:76:A4:9F:39:24:FC:A4:E5`

每次发布必须显式增加 `versionCode`。`BUILD_TAG` 只用于界面识别构建时间，不参与 Android 的升级排序。

如果手机现有 APK 不是上述证书签名，Android 不允许直接覆盖，需要卸载一次。安装稳定签名版本后，本机和 CI 产出的后续 APK 可以互相覆盖。

## 验证命令

```bash
./gradlew testDebugUnitTest assembleDebug signingReport
apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk
```
