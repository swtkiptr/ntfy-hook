# ntfy Hook

<div align="center">
  <img src="logo-white.png" alt="ntfy Hook Logo" width="300">
</div>

<div align="center">
  <strong>Your notifications, anywhere, anytime.</strong>
  <p>A simple and reliable Android app for forwarding notifications to ntfy servers.</p>
</div>

<div align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL%20v3-blue.svg" alt="License: GPL v3"></a>
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg" alt="Platform: Android">
  <img src="https://img.shields.io/badge/Version-1.0.6-orange.svg" alt="Version: 1.0.6">
</div>

<br>

## 📱 Overview

ntfy Hook forwards Android notifications to any ntfy server, offering:
- Cross-device notification syncing
- Selective app forwarding
- Notification history
- Real-time connection status

Forked from [ntfy-relay](https://github.com/eja/ntfy-relay) with enhanced reliability.

## ✨ Features

- Choose apps for notification forwarding
- View notification history
- Auto-restart after device reboot
- Simple setup with server URL

## 📸 Screenshots

<div align="center">
  <img src="screenshot/1.jpg" alt="Main Screen" width="150">
  <img src="screenshot/2.png" alt="Main Screen" width="150">
  <img src="screenshot/3.png" alt="Main Screen" width="150">
  <img src="screenshot/4.png" alt="Main Screen" width="150">
</div>

## 🚀 Improvements

- Fixed service persistence
- Better error handling
- Improved UI with status indicators
- Reliable server connections
- Resolved force-close bugs

## 🛠️ How It Works

1. Monitors selected app notifications
2. Forwards them to your ntfy server via HTTPS
3. Runs as a foreground service
4. Shows real-time status

## 📋 Installation

### From Source
1. Clone: `git clone https://github.com/swtkiptr/ntfy-hook.git`
2. Build in Android Studio
3. Install and grant notification access

### Direct Install
1. Download APK from [releases](https://github.com/swtkiptr/ntfy-hook/releases)
2. Install (enable "unknown sources")
3. Grant notification access

### F-Droid
1. Coming soon to F-Droid
2. Will be available at: [F-Droid](https://f-droid.org)
3. Includes reproducible builds and F-Droid metadata
4. Follows F-Droid best practices

## ⚙️ Configuration

1. Enter ntfy server URL
2. Select apps to monitor
3. Enable service
4. Grant permissions

## 📚 Usage

1. Grant notification access
2. Set ntfy server URL
3. Select apps
4. Enable service
5. Monitor status and history

## 🌐 ntfy.sh Tutorial

1. Choose a topic: `https://ntfy.sh/my-topic`
2. Test with: `curl -d "Test" https://ntfy.sh/my-topic`
3. Subscribe via ntfy app or website
4. Add topic URL to ntfy Hook

## 🔧 Technical Details

- Built with Kotlin
- Uses foreground service
- Stores data with SharedPreferences and Gson
- Includes error logging and recovery

## 📄 License

[GPL-3.0](LICENSE)

## 🙏 Acknowledgements

- Original: [Eja](https://github.com/eja/ntfy-relay)
- Enhanced by: [swtkiptr](https://github.com/swtkiptr)

For issues or contributions, visit the [GitHub repository](https://github.com/swtkiptr/ntfy-hook).
