# IMSI Catcher Detector - Product Requirements Document

## Original Problem Statement
User uploaded a broken Android project for an "IMSI Catcher Detector" and asked to build it. The goals are:
1. Fix the Android app to produce a working APK
2. Build a web application version

## User Personas
- Security-conscious mobile users
- Privacy advocates and security researchers
- People wanting to detect potential IMSI catchers/fake cell towers

## Core Requirements
1. **Android App**: Functional APK that monitors cell towers, analyzes threats, shows encryption status
2. **Web App**: Browser-based dashboard for IMSI catcher detection with real-time monitoring

## What's Been Implemented

### Android App Fixes (Feb 2026)
- Removed Realm DB dependency (replaced with in-memory storage)
- Fixed DetectorViewModel crash (changed from ViewModel to AndroidViewModel for Context access)
- Fixed Models.kt (removed RealmObject, converted to pure Kotlin data classes)
- Fixed CellInfoProvider NR parsing (ssRsrq property access)
- Added Timber initialization in Application class
- Removed kapt plugin (no longer needed)
- Simplified DatabaseRepository to in-memory lists

### Web App (Feb 2026) - COMPLETE
**Backend (FastAPI + MongoDB)**:
- Threat Detection Engine with weighted scoring (encryption 45%, cell consistency 30%, signal 15%, protocol 10%)
- Simulated cell tower data with realistic values (operators, network types, signal strengths)
- 15% chance of threat injection for demo purposes
- REST API: /api/dashboard, /api/history, /api/cell-records, /api/settings, /api/stats, /api/session/*

**Frontend (React + Recharts)**:
- Dark security-themed dashboard with JetBrains Mono typography
- Threat Gauge (SVG circular gauge, 0-100 score)
- Real-time Signal Strength chart (Recharts)
- Cell Tower Info card with technical data
- Encryption Status card with cipher algorithm badges
- Threat Breakdown bars (4 categories)
- Detection Log (terminal-style event feed)
- History page with chronological threat events
- Settings page with monitoring configuration
- Session bar with START/STOP controls

## Architecture
```
/app
├── app/                          # Android project
│   ├── build.gradle.kts         # Dependencies (no Realm)
│   └── src/main/java/com/imsidetector/
│       ├── MainActivity.kt      # Compose UI entry
│       ├── IMSIDetectorApplication.kt # App init
│       ├── Models.kt            # Pure data classes
│       ├── DatabaseRepository.kt # In-memory storage
│       ├── DetectorViewModel.kt # AndroidViewModel
│       ├── CellInfoProvider.kt  # Cell tower data
│       └── ...                  # Analysis engines
├── backend/
│   └── server.py               # FastAPI + Detection Engine
├── frontend/src/
│   ├── App.js                  # Full dashboard app
│   ├── App.css                 # Dark theme + effects
│   └── index.css               # Tailwind variables
```

## Testing Status
- **Backend**: 100% (12/12 APIs tested)
- **Frontend**: 100% (9 components + 5 interactions tested)
- **Android**: User needs to rebuild APK and test on device

## Backlog
- P1: User rebuilds Android APK and tests on Pixel 9a
- P2: Add real cell tower database API integration (OpenCelliD)
- P2: Map visualization for cell tower locations
- P3: Push notifications for threat alerts
- P3: Export threat reports (PDF/CSV)
- P3: Multi-session comparison analytics
