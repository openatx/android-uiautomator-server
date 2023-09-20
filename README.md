# Purpose

[UIAutomator](http://developer.android.com/tools/testing/testing_ui.html) is a
great tool to perform Android UI testing, but to do it, you have to write java
code, compile it, install the jar, and run. It's a complex steps for all
testers...

This project is to build a light weight jsonrpc server in Android device, so
that we can just write PC side script to write UIAutomator tests.

# Build

- Run command:

```bash
$ ./gradlew build
$ ./gradlew packageDebugAndroidTest
```

- Run the jsonrpc server on Android device

```bash
$ ./gradlew cC
$ adb forward tcp:9008 tcp:9008 # tcp forward
```

If debug apk already installed, There is no need to use gradle.

simply run the following command

```
adb forward tcp:9008 tcp:9008
adb shell am instrument -w -r -e debug false -e class com.github.uiautomator.stub.Stub \
    com.github.uiautomator.test/androidx.test.runner.AndroidJUnitRunner
```

# Run
```bash
$ curl -X POST -d '{"jsonrpc": "2.0", "id": "1f0f2655716023254ed2b57ba4198815", "method": "deviceInfo", "params": {}}' 'http://127.0.0.1:9008/jsonrpc/0'
{'currentPackageName': 'com.smartisanos.launcher',
 'displayHeight': 1920,
 'displayRotation': 0,
 'displaySizeDpX': 360,
 'displaySizeDpY': 640,
 'displayWidth': 1080,
 'productName': 'surabaya',
 'screenOn': True,
 'sdkInt': 23,
 'naturalOrientation': True}
```


# The buildin input method
**Fast input method**

Encode the text into UTF-8 and then Base64

For example:

    "Hello 你好" -> (UTF-8 && Base64) = SGVsbG8g5L2g5aW9

Send to FastInputIME with broadcast

```bash
# Append text to input field
$ adb shell am broadcast -a ADB_INPUT_TEXT --es text SGVsbG8g5L2g5aW9

# Clear text
$ adb shell am broadcast -a ADB_CLEAR_TEXT

# Clear text before append text
$ adb shell am broadcast -a ADB_SET_TEXT --es text SGVsbG8g5L2g5aW9

# Send keycode, eg: ENTER
$ adb shell am broadcast -a ADB_INPUT_KEYCODE --ei code 66

# Send Editor code
$ adb shell am broadcast -a ADB_EDITOR_CODE --ei code 2 # IME_ACTION_GO

# Get clipboard (without data)
$ adb shell am broadcast -a ADB_GET_CLIPBOARD
Broadcasting: Intent { act=ADB_GET_CLIPBOARD flg=0x400000 }
Broadcast completed: result=0

# How to use

```python

from uiautomator import device as d

d.info
d.screen.on()
d(text="Settings").click()
d(scrollable=True).scroll.vert.forward()
d().gestureM((100,200),(100,300),(100,400)).to((100,400),(100,400),(100,400),100)

```

Refer to python wrapper library [uiautomator](https://github.com/xiaocong/uiautomator).

# How to generate changelog
[conventional-changelog](https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-cli)

```bash
npm install -g conventional-changelog-cli
conventional-changelog -p grunt -i CHANGELOG.md -s -r 0
```

# Notes

If you have any idea, please email xiaocong@gmail.com, hongbin.bao@gmail.com or [submit tickets](https://github.com/xiaocong/uiautomator/issues/new).
If you have any idea, please email codeskyblue@gmail.com or [submit tickets](https://github.com/openatx/android-uiautomator-server/issues/new).

# Dependencies

- [nanohttpd](https://github.com/NanoHttpd/nanohttpd)
- [jsonrpc4j](https://github.com/briandilley/jsonrpc4j)
- [jackson](https://github.com/FasterXML/jackson)
- [androidx.test.uiautomator](https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator-v18)

# Added features

- [x] support unicode input

# Resources
- [Google UiAutomator Tutorial](https://developer.android.com/training/testing/ui-testing/uiautomator-testing?hl=zh-cn)
- [Google UiAutomator API](https://developer.android.com/reference/android/support/test/uiautomator/package-summary?hl=zh-cn)
- [Maven repository of uiautomator-v18](https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator-v18)

Clipboard, Thanks to @fplust

- https://github.com/majido/clipper
- https://github.com/appium/io.appium.settings/blob/759d2febbc95a6832eb74fa0a4a56c5975dc694d/app/src/main/java/io/appium/settings/receivers/ClipboardReceiver.java#L30

# Thanks to
- [xiaocong](https://github.com/xiaocong)
- https://github.com/willerce/WhatsInput
- https://github.com/senzhk/ADBKeyBoard
- https://github.com/amotzte/android-mock-location-for-development
# TODO
- android O support
