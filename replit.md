# PowerGuard - Android Power Protection App

## Overview
PowerGuard là một ứng dụng Android Kotlin được thiết kế để bảo vệ thiết bị khỏi việc tắt nguồn hoặc khởi động lại trái phép. Ứng dụng sử dụng AccessibilityService để phát hiện khi menu nguồn được mở và hiển thị màn hình xác thực mật khẩu.

## Project Structure
```
PowerGuard/
├── app/src/main/
│   ├── java/com/powerguard/
│   │   ├── MainActivity.kt           # Màn hình chính
│   │   ├── SettingsActivity.kt       # Màn hình cài đặt
│   │   ├── PowerGuardApp.kt          # Application class
│   │   ├── accessibility/
│   │   │   └── PowerMenuAccessibilityService.kt  # Phát hiện power menu
│   │   ├── overlay/
│   │   │   └── PasswordOverlayManager.kt         # Quản lý overlay mật khẩu
│   │   ├── service/
│   │   │   ├── PowerGuardService.kt              # Foreground service
│   │   │   ├── BootReceiver.kt                   # Tự động khởi động
│   │   │   └── ShutdownReceiver.kt               # Phát hiện shutdown
│   │   ├── shizuku/
│   │   │   ├── ShizukuHelper.kt                  # Shizuku integration
│   │   │   └── ShizukuUserService.kt             # Shizuku user service
│   │   └── utils/
│   │       ├── PasswordManager.kt                # Quản lý mật khẩu (encrypted)
│   │       └── PermissionHelper.kt               # Quản lý quyền
│   ├── res/                          # Resources (layouts, strings, etc.)
│   ├── aidl/                         # AIDL cho Shizuku
│   └── AndroidManifest.xml
├── .github/workflows/
│   └── android-build.yml             # GitHub Actions build APK
├── build.gradle.kts                  # Root build config
├── settings.gradle.kts               # Gradle settings
├── docs/index.html                   # Documentation page
└── server.js                         # Doc server for Replit
```

## Key Features
1. **Power Menu Detection**: Sử dụng AccessibilityService để phát hiện GlobalActionsDialog
2. **Password Overlay**: Hiển thị overlay nhập mật khẩu trên tất cả ứng dụng
3. **Encrypted Storage**: Mật khẩu được lưu trữ an toàn với EncryptedSharedPreferences
4. **Shizuku Integration**: Hỗ trợ Shizuku API cho chặn sâu hơn
5. **Auto-start**: Tự động khởi động dịch vụ bảo vệ khi thiết bị bật

## Build Instructions
### Using GitHub Actions (Recommended)
1. Push code to GitHub repository
2. GitHub Actions will automatically build APK
3. Download from Actions > Artifacts

### Manual Build
```bash
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK
```

## Required Permissions
- `SYSTEM_ALERT_WINDOW` - Overlay display
- `AccessibilityService` - Power menu detection
- `FOREGROUND_SERVICE` - Background protection
- `RECEIVE_BOOT_COMPLETED` - Auto-start

## Dependencies
- AndroidX Core, AppCompat, Material Design
- Shizuku API 13.1.5
- AndroidX Security Crypto

## How It Works
1. **Detection**: AccessibilityService monitors for GlobalActionsDialog (power menu)
2. **Dismiss**: Immediately performs GLOBAL_ACTION_BACK to close power menu
3. **Overlay**: Shows full-screen password overlay
4. **Validation**: 
   - Correct password → Allows user to retry power action
   - Wrong password → Returns to home screen

## Platform Limitations
Android's security model intentionally prevents apps from blocking power actions:
- **No API** to intercept hardware power button events
- **No API** to prevent system power menu actions
- Even with Shizuku (elevated permissions), true blocking is not possible
- Force shutdown (holding power >10s) cannot be prevented

**What this app provides:**
- A deterrent layer that makes unauthorized power-off more difficult
- Quick detection and response to power menu appearance
- Shizuku integration for potential future system-level features

## Shizuku Integration
- Binds to ShizukuUserService on startup if enabled
- Calls enableDeepBlocking() when power menu detected
- Calls disableDeepBlocking() when correct password entered
- Properly unbinds on service destruction
