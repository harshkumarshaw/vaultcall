# VaultCall — Agent Build Quick Reference

## Files in This Package

| File | Purpose |
|---|---|
| `CLAUDE.md` | Master instructions — agent reads this first every session |
| `AGENT_PROMPTS.md` | Copy-paste prompts for each build session |
| `instructions/01_project_setup.md` | Gradle, manifest, app class |
| `instructions/02_data_layer.md` | Models, DAOs, DB, repositories |
| `instructions/03_security_layer.md` | Encryption, Keystore, biometric |
| `instructions/04_telecom_foundation.md` | InCallService, CallScreeningService |
| `instructions/05_rules_engine.md` | Rules logic + UI |
| `instructions/06_to_12_modules.md` | Voicemail, transcription, dialer, settings, polish |

---

## One-Day Build Order

| Session | Module | Est. Time |
|---|---|---|
| 1 | Project Setup | 45 min |
| 2 | Data Layer | 60 min |
| 3 | Security Layer | 60 min |
| 4 | Telecom Foundation | 90 min |
| 5 | Rules Engine | 75 min |
| 6 | Voicemail + Transcription | 90 min |
| 7 | Screening + Dialer | 90 min |
| 8 | Settings + Notifications + Polish | 75 min |
| — | Testing + Bug Fixes | 60 min |
| **Total** | | **~10.5 hours** |

---

## Before You Start

1. Download Whisper tiny ONNX:
   `https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin`
   Or ONNX version: search "whisper-tiny.onnx" on HuggingFace
   Place at: `app/src/main/assets/whisper-tiny.onnx`

2. Disable carrier voicemail on your SIM:
   - Airtel: dial `*#62#` then disable
   - Jio: call 1800-889-9999
   - VI: dial `*#67#`

3. Enable Developer Options on test device

4. Have a second phone ready to call your test device

---

## Critical Things That Will Break If Wrong

1. **Must be default dialer** — nothing works without this
2. **Foreground service type** must be `phoneCall` in manifest
3. **SQLCipher** must be initialized before any Room operation
4. **ONNX model** must have `noCompress` in aaptOptions
5. **Temp files** must always be deleted after decryption
6. **POST_NOTIFICATIONS** permission must be requested on Android 13+

---

## Quick ADB Commands

```bash
# Install APK
adb install -r app/build/outputs/apk/release/app-release.apk

# View logs from VaultCall only
adb logcat -s VaultCall

# Check if app is default dialer
adb shell dumpsys telecom | grep "Default dialer"

# Clear app data (reset to fresh install)
adb shell pm clear com.vaultcall

# Grant permissions via ADB (for testing)
adb shell pm grant com.vaultcall android.permission.RECORD_AUDIO
adb shell pm grant com.vaultcall android.permission.READ_PHONE_STATE
adb shell pm grant com.vaultcall android.permission.ANSWER_PHONE_CALLS
```
