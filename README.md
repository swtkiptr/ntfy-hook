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
  <img src="https://img.shields.io/badge/Version-1.0.0-orange.svg" alt="Version: 1.0.0">
</div>

<br>

## 📱 Overview

ntfy Hook seamlessly bridges your Android device notifications to any ntfy server, allowing you to:
- Receive your phone notifications on computers, tablets, and other devices
- Selectively forward notifications from specific apps
- Keep a history of all sent notifications
- Monitor connection status with real-time indicators

This fork by swtkiptr enhances the original [ntfy relay](https://github.com/eja/ntfy-relay) project with improved reliability and additional features.

---

## ✨ Key Features

- **Selective Notification Forwarding**: Choose exactly which apps can send notifications through your server
- **Real-time Status Monitoring**: Color-coded indicators show connection and service status at a glance
- **Complete Notification History**: Browse through a complete log of all forwarded notifications
- **Automatic Recovery**: Service restarts after device reboot, maintaining persistent connections
- **Simple Configuration**: Easy setup with minimal steps - just add your server URL and select apps

---

## 📸 Screenshots

<div align="center">
  <img src="screenshot/1.jpg" alt="Main Screen" width="200">

</div>

---

## 🚀 Improvements in this Fork

- **Fixed Service Persistence**: Notifications continue after phone restarts
- **Enhanced Error Handling**: Better recovery from connection issues
- **Improved UI**: Status indicators and notification display enhancements
- **Reliable Connection Management**: Keeps consistent connection to ntfy servers
- **Force Close Resolution**: Fixed critical bugs in the notification service

---

## 🛠️ How It Works

ntfy Hook uses Android's notification listener service to:
1. Monitor for new notifications from your selected apps
2. Securely forward notification content to your configured ntfy server via HTTPS
3. Maintain a persistent connection through a foreground service
4. Provide real-time feedback on service and connection status

---

## 📋 Installation

### Option 1: From Source
1. Clone this repository: `git clone https://github.com/swtkiptr/ntfy-relay.git`
2. Open in Android Studio
3. Build and install on your device
4. Open the app and grant notification access when prompted
5. Enable the notification service in the app settings

### Option 2: Direct Install
1. Download the latest APK from the [releases section](https://github.com/swtkiptr/ntfy-relay/releases)
2. Install on your Android device (ensure "Install from unknown sources" is enabled)
3. Open the app and grant notification access when prompted
4. Enable the notification service in the app settings

---

## ⚙️ Configuration

1. **Enter ntfy Server URL**: Provide the URL of your ntfy server
2. **Select Apps**: Choose which applications to monitor for notifications
3. **Enable Service**: Toggle the switch to start the notification service
4. **Grant Permissions**: Allow notification access when prompted

---

## 📚 Usage Guide

1. Start the app and ensure notification access is granted
2. Enter your ntfy server URL in the provided field
3. Tap "Select Apps" to choose which app notifications to forward
4. Enable the service with the toggle switch
5. Check the status indicators to confirm proper connection
6. View your notification history anytime through the history screen

---

## 🌐 Tutorial: How to Create an ntfy URL in ntfy.sh

`ntfy.sh` is a simple and powerful service for managing notification topics. Follow these steps to create an ntfy URL:

### Step 1: Choose a Topic
A "topic" is a unique identifier for your notifications. It acts like a channel where all your notifications will be sent. For example:
```
https://ntfy.sh/my-topic
```
Replace `my-topic` with a unique name (e.g., `alerts`, `device-notifications`, etc.).

### Step 2: Test Your ntfy Topic
Send a test notification to your topic using `curl`:
```bash
curl -d "Hello, ntfy!" https://ntfy.sh/my-topic
```
This will send a notification to the topic `my-topic`. You can replace the message content (`Hello, ntfy!`) with your desired notification text.

### Step 3: Subscribe to the Topic
Use the ntfy app or any compatible tool to subscribe to your topic:
1. Open the ntfy app or visit the ntfy.sh website.
2. Enter your topic URL (e.g., `https://ntfy.sh/my-topic`) to subscribe and start receiving notifications.

### Step 4: Secure Your Topic (Optional)
To make your topic private, you can use basic authentication. For example:
```bash
curl -u username:password -d "Secure message" https://ntfy.sh/my-private-topic
```
Replace `username` and `password` with your credentials and `my-private-topic` with your topic name.

### Step 5: Integrate With ntfy Hook
1. Open the ntfy Hook app on your Android device.
2. Enter your ntfy topic URL (e.g., `https://ntfy.sh/my-topic`) in the server URL field.
3. Configure other settings (e.g., select apps to forward).
4. Start the service and enjoy seamless notification forwarding!

---

## 🔧 Technical Details

- Built with Kotlin for modern Android development
- Implements foreground service for reliable background operation
- Uses SharedPreferences with Gson for efficient data management
- Features comprehensive error logging and recovery mechanisms
- Organized with specialized manager classes for maintainability

---

## 📄 License

This project is licensed under the [GPL-3.0](LICENSE) - see the license file for details.

---

## 🙏 Acknowledgements

- Original project by [Eja](https://github.com/eja/ntfy-relay)
- Enhanced and maintained by [swtkiptr](https://github.com/swtkiptr)

---

This README was generated with ❤️ by swtkiptr. For any issues or feature requests, please open an issue on the GitHub repository. Feel free to contribute and help improve this project!

--- 