## Use APK as commandline

运行

```bash
APK=$(adb shell pm path com.github.uiautomator | cut -d: -f2)
adb shell export CLASSPATH="$APK"\; \
    exec app_process /system/bin com.github.uiautomator.Console --version
```