# 📣 Family Ringer

An Android app that lets parents send loud alerts to their kids' phones — even when the phone is on silent.

---

## ✨ Features

- **One app, two modes**: Install on all phones. Parent mode sends, Child mode receives.
- **Bypasses silent mode**: Uses `USAGE_ALARM` audio stream — rings even on silent/DND.
- **Configurable alerts**: Set custom messages (Dinner, Training, Bed time, etc.)
- **Select recipients**: Choose which kids get each alert.
- **Full-screen alert**: Kid's screen lights up and shows the message with a dismiss button.
- **Auto-stops**: Alarm automatically stops after 30 seconds if not dismissed.

---

## 🚀 Setup Guide

### Step 1 — Firebase Project (one-time, ~5 minutes)

1. Go to [Firebase Console](https://console.firebase.google.com/) and create a new project (e.g. "FamilyRinger")
2. Add an Android app:
   - Package name: `com.familyringer`
   - Download the `google-services.json` file
   - Place it in `app/google-services.json`
3. Generate a **Service Account key** for FCM v1 API:
   - Firebase Console → ⚙️ Project Settings → **Service Accounts** tab
   - Click **"Generate new private key"** → confirm → a `.json` file downloads
   - This file contains your `project_id`, `client_email`, and `private_key`
   - **Keep this file private** — it grants full FCM send access to your project

> ℹ️ The old "Server Key" (Legacy FCM API) was shut down by Google. The app now uses the **FCM v1 HTTP API** with OAuth2, authenticated via a service account JWT. No extra libraries needed — Android's built-in `java.security` handles RSA signing.

### Step 2 — Build & Install

1. Open this project in **Android Studio**
2. Make sure `google-services.json` is in the `app/` folder
3. Build and install on **all family phones** (parent + kids)

### Step 3 — Set Up Parent Phone

1. Open Family Ringer
2. Go to **Settings** (gear icon)
3. Select **Parent mode**
4. Paste your **FCM Server Key**
5. Add your kids — you'll need each kid's FCM token (see Step 4)
6. Customize your alert messages
7. Tap **Save**

### Step 4 — Set Up Each Child's Phone

1. Open Family Ringer on the child's phone
2. Go to **Settings**
3. Select **Child mode**
4. Enter the child's name
5. Tap **"Copy My Token"** — this copies their unique FCM token
6. Share this token with the parent (paste it into a text message or email)
7. Parent adds the token in Settings → Kids section
8. Tap **Save**

### Step 5 — Test It!

1. On the parent phone, tap a child's avatar to select them (turns orange = selected)
2. Tap any alert button (e.g. "🍽️ Dinner is ready!")
3. The child's phone will ring loudly and show a full-screen alert!

---

## 🔔 How Silent Mode Bypass Works

Android has separate volume streams. By using `AudioAttributes.USAGE_ALARM`, the app plays through the **alarm stream**, which:
- Is NOT affected by the ringer/silent switch
- Is NOT muted by Do Not Disturb (unless DND blocks all interruptions)
- Always plays at the alarm volume level

The notification channel is also configured with `USAGE_ALARM` so the system notification sound also bypasses silent mode.

---

## 📁 Project Structure

```
app/src/main/java/com/familyringer/
├── MainActivity.java          — Parent/Child main screen
├── SetupActivity.java         — Settings screen
├── AlertActivity.java         — Full-screen alert on child device
├── AlarmService.java          — Foreground service playing alarm sound
├── FamilyMessagingService.java — FCM push receiver
├── FcmSender.java             — Sends FCM HTTP request
├── Kid.java                   — Data model
├── KidAdapter.java            — Kid selector chips
├── AlertAdapter.java          — Alert buttons
├── KidEditAdapter.java        — Kids list in settings
└── AlertEditAdapter.java      — Alerts list in settings
```

---

## ⚠️ Important Notes

### Alert Sound
The project references `R.raw.alert_sound`. You need to:
1. Create folder: `app/src/main/res/raw/`
2. Add a sound file named `alert_sound.mp3` (or `.ogg`)
3. You can use any free alarm sound — search "free alarm sound mp3"

### FCM Legacy API
This app uses the FCM Legacy HTTP API which is simpler for personal projects. Google plans to deprecate it eventually — if needed, you can upgrade to FCM v1 API (requires OAuth2 tokens).

### Battery Optimization
On some Android phones (especially Xiaomi, Huawei, Samsung), aggressive battery optimization can delay or block notifications. Go to:
- Settings → Battery → App Battery Usage → Family Ringer → **Unrestricted**

### DND (Do Not Disturb)
For the alert to truly bypass DND:
- The notification channel must be set to ALARM importance
- OR grant the app **Do Not Disturb access** in system settings
- The app requests `ACCESS_NOTIFICATION_POLICY` permission for this

---

## 🔧 Troubleshooting

| Problem | Solution |
|---------|----------|
| Alert not received | Check FCM token is correct in parent settings |
| Sound not playing | Ensure `alert_sound.mp3` exists in `res/raw/` |
| Phone still silent | Enable "Unrestricted battery" for the app |
| FCM send error 401 | Service account JSON is invalid or missing |
| FCM send error 403 | Firebase Messaging API not enabled — go to Firebase Console → Project Settings → Service Accounts |

---

## 📱 Tested On

- Android 8.0+ (API 26+)
- Works on: Pixel, Samsung Galaxy, OnePlus
- May need extra battery configuration on: Xiaomi (MIUI), Huawei (EMUI)
