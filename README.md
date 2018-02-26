# Purpose
[![Build Status](https://travis-ci.org/openatx/android-uiautomator-server.svg?branch=master)](https://travis-ci.org/openatx/android-uiautomator-server)

[UIAutomator](http://developer.android.com/tools/testing/testing_ui.html) is a
great tool to perform Android UI testing, but to do it, you have to write java
code, compile it, install the jar, and run. It's a complex steps for all
testers...

This project is to build a light weight jsonrpc server in Android device, so
that we can just write PC side script to write UIAutomator tests.

# Build

- Run command:

        $ ./gradlew build
        $ ./gradlew packageDebugAndroidTest

- Run the jsonrcp server on Android device

        $ ./gradlew cC
        $ adb forward tcp:9008 tcp:9008 # tcp forward

# The buildin input method
**Fast input method**

Encode the text into UTF-8 and then Base64

For example:

    "Hello 你好" -> (UTF-8 && Base64) = SGVsbG8g5L2g5aW9

Send to FastInputIME with broadcast

    $ adb shell am broadcast -a ADB_INPUT_TEXT --es text SGVsbG8g5L2g5aW9

# How to use

```python
from uiautomator import device as d

d.screen.on()
d(text="Settings").click()
d(scrollable=True).scroll.vert.forward()
```

Refer to python wrapper library [uiautomator](https://github.com/xiaocong/uiautomator).

# How to generate changelog
[conventional-changelog](https://github.com/conventional-changelog/conventional-changelog/tree/master/packages/conventional-changelog-cli)

```bash
npm install -g conventional-changelog-cli
conventional-changelog -p grunt -i CHANGELOG.md -s -r 0
```

# Notes

If you have any idea, please email xiaocong@gmail.com or [submit tickets](https://github.com/xiaocong/uiautomator/issues/new).

# Dependencies

- [nanohttpd](https://github.com/NanoHttpd/nanohttpd)
- [jsonrpc4j](https://github.com/briandilley/jsonrpc4j)
- [jackson](https://github.com/FasterXML/jackson)

# TODO

- ~~move from java to kotlin~~
- [x] support unicode input
