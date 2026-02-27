# IMSI Catcher Detector - Design & Architecture Document

## Executive Overview

The IMSI Catcher Detector is a comprehensive Android application designed to identify and alert users to the presence of fake cellular base stations (IMSI catchers). The app combines advanced signal analysis, cell tower monitoring, encryption verification, and network behavior profiling to provide real-time threat detection with a modern Material You interface.

## Core Architecture

### Detection Engine

The detection system operates through multiple parallel analysis channels that feed into a unified threat scoring algorithm:

**Signal Analysis Module** monitors real-time signal strength (RSSI/RSRP in dBm), signal quality metrics, and timing advance values. The system establishes baseline signal patterns for each location and detects anomalies such as sudden signal strength changes, quality degradation, or unusual timing advance values that deviate from expected cellular behavior.

**Cell Tower Behavior Module** tracks Location Area Code (LAC) and Tracking Area Code (TAC) changes, Cell ID consistency, and neighbor cell relationships. This module detects rapid cell changes, inconsistent neighbor cell lists, and cell tower configurations that deviate from known legitimate tower patterns.

**Encryption Monitoring Module** continuously verifies the cipher status of the active connection. It detects when encryption is disabled (A5/0 cipher), monitors for cipher suite downgrades, and alerts when the device is forced from LTE to 3G/2G networks—a common IMSI catcher tactic.

**Network Behavior Profiler** establishes baseline patterns for the user's normal network behavior, including typical signal strength ranges, expected cell tower sequences, and normal registration patterns. Deviations from these baselines trigger investigation and scoring.

**Protocol Analysis Module** monitors for silent SMS messages (Class 0 SMS that appear as notifications), WAP Push anomalies, unusual call patterns, and unexpected data connection changes that may indicate surveillance attempts.

### Threat Scoring System

Each detection module produces independent threat indicators that feed into a unified threat score (0-100). The scoring system uses weighted contributions:

- Encryption downgrade or disabled: 40 points (highest severity)
- LAC/TAC inconsistency with signal anomaly: 25 points
- Rapid cell changes with signal anomaly: 20 points
- Neighbor cell list inconsistency: 15 points
- Signal anomaly alone: 10 points
- Protocol anomaly (silent SMS): 15 points

The final threat level is categorized as:
- **Green (0-20):** Normal operation
- **Yellow (21-50):** Potential anomaly detected, requires investigation
- **Orange (51-75):** Significant threat indicators present
- **Red (76-100):** High probability IMSI catcher detected

## Feature Set

### Real-Time Monitoring
The app continuously monitors cell information through the Android TelephonyManager API, receiving callbacks when cell info changes or signal strength changes. The monitoring runs as a foreground service to ensure continuous operation even when the app is backgrounded.

### Dashboard
The main dashboard displays the current threat level with color-coded indicator, active cell tower information (LAC/TAC/CID), signal strength visualization, encryption status, and a timeline of recent threat events. Users can tap any event for detailed analysis.

### Cell Tower Map
An interactive map displays the current cell tower location (when available through signal triangulation), neighboring cells, signal strength coverage visualization, and historical tower locations. Users can identify patterns and unusual tower placements.

### Threat History
A detailed log of all detected anomalies with timestamps, severity levels, affected parameters, and recommended actions. Users can export this data for analysis or sharing with security professionals.

### Network Baseline
The app learns the user's normal network patterns over time and allows manual baseline adjustment. Users can mark specific locations or times as "trusted" to refine the detection algorithm.

### Detailed Cell Information
Technical details about the current cell connection including LAC, TAC, CID, ARFCN (for GSM), EARFCN (for LTE), PCI (Physical Cell ID), RSSI/RSRP values, RSRQ, timing advance, and encryption cipher status.

### Alert System
Real-time notifications for detected threats with severity levels. Users can configure notification preferences and receive detailed alerts explaining what was detected and why it's concerning.

## Android API Integration

### Permissions Required
The app requires `READ_PHONE_STATE` for basic cell monitoring, `READ_PRECISE_PHONE_STATE` (Android 12+) for advanced cell information, `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` for cell tower triangulation, `RECEIVE_SMS` for silent SMS detection, and `POST_NOTIFICATIONS` (Android 13+) for alert notifications.

### Telephony APIs Used
The implementation uses `TelephonyManager.getAllCellInfo()` to retrieve all cell information, `TelephonyCallback` (Android 12+) for state change notifications, `ServiceState` for network registration information, and `CellInfo` subclasses (`CellInfoLte`, `CellInfoGsm`, `CellInfoWcdma`, `CellInfoNr`) for detailed cell data.

### Background Service
A `ForegroundService` maintains continuous monitoring even when the app is backgrounded. The service uses `TelephonyCallback` to receive cell state changes and processes them through the detection engine.

## Data Storage

### Realm Database Schema
The app uses Realm for efficient local storage with the following entities:

**CellTowerRecord** stores timestamp, LAC/TAC, CID, signal strength (dBm), signal quality, encryption cipher, and location coordinates.

**ThreatEvent** records timestamp, threat type, severity score, affected parameters, detected anomaly details, and recommended action.

**BaselineProfile** maintains location-based or time-based baseline patterns including expected signal ranges, typical cell towers, and normal registration patterns.

**SMSLog** tracks incoming SMS with timestamp, sender, content preview, and classification (normal/suspicious).

## UI/UX Design Philosophy

### Material You Integration
The app fully embraces Material Design 3 with dynamic color theming based on the device's system theme. The interface adapts to the user's color preferences while maintaining accessibility standards.

### Information Hierarchy
The dashboard prioritizes threat level and current status at the top, followed by detailed cell information, and historical context below. Users can drill down into any section for more details without losing context.

### Visual Feedback
Color-coded threat indicators (green/yellow/orange/red) provide instant visual feedback. Smooth animations and transitions make state changes clear and intuitive. Signal strength is visualized as a waveform or bar chart over time.

### Accessibility
All interactive elements maintain sufficient contrast ratios for visibility. Text sizes are configurable. The app supports screen readers and provides descriptive labels for all controls.

## Technical Stack

**Language:** Kotlin with coroutines for asynchronous operations

**UI Framework:** Jetpack Compose with Material Design 3

**Database:** Realm for local data persistence

**Background Processing:** WorkManager for periodic tasks, ForegroundService for continuous monitoring

**Location Services:** Android Location API for cell tower triangulation

**Mapping:** Google Maps API (via Manus proxy) for visualization

**Networking:** Retrofit for any API calls

**Logging:** Timber for structured logging

**Testing:** JUnit and Espresso for UI testing

## Security Considerations

### Data Privacy
All collected cell information is stored locally on the device. No data is transmitted to external servers without explicit user consent. Users can clear all collected data at any time.

### Permission Handling
The app requests permissions only when needed and explains why each permission is required. Users can revoke permissions at any time through Android settings.

### Encryption
All local database storage uses Realm's built-in encryption. Sensitive parameters are never logged to system logs.

## Performance Optimization

### Battery Efficiency
The app uses efficient callbacks instead of polling. Cell monitoring runs only when necessary. Background tasks are batched and scheduled during low-power periods when possible.

### Memory Management
Cell information is processed in streams rather than loading entire histories into memory. The database uses pagination for historical data retrieval.

### Network Efficiency
Map tiles are cached locally. Cell tower data is cached with appropriate TTL values.

## Deployment & Distribution

The app will be built as a release APK with proper signing. It can be distributed through Google Play Store, F-Droid, or direct APK installation. The app targets Android 11+ (API level 30+) to ensure access to modern APIs while maintaining reasonable device compatibility.

## Future Enhancements

Potential future features include machine learning-based anomaly detection, crowdsourced IMSI catcher reports, integration with telecom regulatory databases, advanced visualization of network topology, and support for additional network types (5G SA, satellite communications).
