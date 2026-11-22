<h1 align="center">Zefgram</h1>

Zefgram is an unofficial Telegram client for Android. It is based on the open-source Telegram for Android code and on [Easygram](https://github.com/webarmour/Easygram), and adds extra features on top of the familiar messenger. The main focus right now is video message ("circle") recording.

> Zefgram is an independent project and is not affiliated with Telegram.

## Custom camera pipeline

On Android 9 and newer, Zefgram records circles through a separate Camera2-based pipeline. Recording is integrated into the chat UI, but it uses its own handling of the camera, preview, audio and video. On older Android versions the original recording path is used.

What this gives you:

- **More control over quality.** In the video message settings you can choose resolution, frame rate, video and audio bitrate, maximum duration and image processing options.
- **Tuning for your device's camera.** Available modes are checked for both the front and the main camera. Unsupported options are marked as unavailable and are not saved.
- **A cleaner start of recording.** Recording begins only after the camera is ready and the first preview frame has arrived. An extra delay helps keep the exposure jump at the moment of starting out of the finished video.
- **Easy camera switching.** Zefgram can remember the camera used for the last recording and start the next circle with it. If this option is off, recording starts with the front camera.
- **Light and focus settings.** Exposure, white balance, focus, stabilization and other camera modes are available if your device supports them.

You can find these under **Settings → Video Messages**. Camera behavior and the availability of individual features may differ between devices.

<!-- Uncomment when the account limit has actually been changed in UserConfig.java
## Multiple accounts

Zefgram raises the number of accounts you can be logged into at the same time beyond the standard Telegram limit.
-->

## What's next

Zefgram will keep evolving, with planned improvements to circle recording, image quality and general usability. Changes will appear gradually.

## APK updates

New builds check [GitHub Releases](https://github.com/zefronxd/tirai/releases) directly. A release marked **Pre-release** is meant for the beta channel (`io.github.zefronxd.zefgram.beta`), and a normal published release is meant for stable (`io.github.zefronxd.zefgram`). Drafts are ignored. The check runs when the app is resumed, at most once per hour, and can also be started manually from the settings.

## Building from source

Zefgram is built with GitHub Actions. To build your own copy:

1. Fork this repository.
2. Add these repository secrets: `TELEGRAM_API_ID`, `TELEGRAM_API_HASH`, `EASYGRAM_RELEASE_KEYSTORE_BASE64`, `EASYGRAM_RELEASE_STORE_PASSWORD`, `EASYGRAM_RELEASE_KEY_ALIAS` and `EASYGRAM_RELEASE_KEY_PASSWORD`.
3. Run the **Upstream merge and stable release** workflow from the Actions tab.
4. The signed APK appears under **Releases**.

You need your own `api_id` and `api_hash` from https://my.telegram.org.

## Source code and license

Zefgram is an independent project, not affiliated with the Telegram team. It is based on [Telegram for Android](https://github.com/DrKLO/Telegram) and on [Easygram](https://github.com/webarmour/Easygram) by webarmour. The terms for distributing the source code are in the [LICENSE](LICENSE) file (GNU GPL v2 or later).
