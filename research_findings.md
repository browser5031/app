# IMSI Catcher Detection Research Findings

## Key Detection Techniques (from AIMSICD project)

### Implemented Detection Tests:
1. **LAC/CID Consistency (ID:2)** - Detect when Location Area Code or Cell ID changes unexpectedly
2. **TMSI Changes (ID:4)** - Monitor Temporary Mobile Subscriber Identity allocation anomalies
3. **Ciphering Consistency (ID:5)** - Alert when encryption is disabled or downgraded
4. **A5/x Downgrade (ID:6)** - Detect cipher suite downgrades (A5/0 = no encryption)
5. **Neighbors Consistency (ID:7)** - Verify neighboring cell information validity
6. **RX Signal Quality (ID:9)** - Monitor signal quality metrics
7. **LTE/3G Downgrade (ID:18)** - Detect forced downgrades from LTE to 3G/2G
8. **Silent SMS/WAP Push (ID:13)** - Detect silent messages (Class 0 SMS)
9. **Silent App Install (ID:14)** - Monitor for unauthorized app installations
10. **Silent Calls (ID:15)** - Detect incoming calls without notification

## Android Telephony APIs Available

### Core Classes:
- **TelephonyManager** - Primary API for cell information
  - `getAllCellInfo()` - Get all cell information (preferred over deprecated methods)
  - `getNetworkOperatorName()` - Get operator name
  - `getNetworkCountryIso()` - Get country code
  - `getSimSerialNumber()` - Get SIM serial
  - `getSubscriberId()` - Get IMSI

- **ServiceState** - Network registration state
  - `getOperatorAlphaLong()` / `getOperatorAlphaShort()`
  - `getIsManualSelection()` - Manual vs automatic network selection
  - `getRoaming()` - Roaming status
  - `getState()` - Service state (in service, searching, etc.)

- **CellInfo** (base class) - Cell information container
  - `CellInfoLte` - LTE cell info (TAC, CI, PCI, EARFCN, RSRP, RSRQ)
  - `CellInfoGsm` - GSM cell info (LAC, CID, ARFCN, BSIC, RSSI)
  - `CellInfoWcdma` - WCDMA/3G cell info (LAC, CID, UARFCN, PSC, RSCP)
  - `CellInfoNr` - 5G NR cell info

- **CellSignalStrength** (base class)
  - `getDbm()` - Signal strength in dBm
  - `getLevel()` - Signal level 0-4
  - `getAsuLevel()` - Arbitrary Strength Unit

- **PhoneStateListener** - Monitor telephony state changes
  - `onCellInfoChanged()` - Called when cell info changes
  - `onSignalStrengthsChanged()` - Called when signal changes
  - `onServiceStateChanged()` - Called when service state changes

## Advanced Detection Strategies

### 1. Signal Anomaly Detection
- Track RSSI/RSRP trends over time
- Detect sudden signal strength changes
- Monitor signal quality degradation

### 2. Cell Tower Behavior Analysis
- Track LAC/TAC (Location Area Code/Tracking Area Code) changes
- Monitor CID (Cell ID) consistency
- Detect rapid cell changes (tower hopping)
- Verify ARFCN (Absolute Radio Frequency Channel Number) validity

### 3. Encryption Monitoring
- Check cipher status (A5/0 = no encryption = RED FLAG)
- Monitor for cipher downgrades
- Track encryption capability changes

### 4. Network Behavior Profiling
- Baseline normal network behavior
- Detect deviations from baseline
- Monitor neighbor cell lists
- Track TMSI allocation patterns

### 5. Protocol Analysis
- Monitor for unusual SMS patterns (Class 0 = silent SMS)
- Detect WAP Push anomalies
- Track call patterns
- Monitor data connection changes

## Modern Android Considerations

### Android 12+ Permissions:
- `READ_PHONE_STATE` - Access phone state
- `ACCESS_FINE_LOCATION` - For cell tower triangulation
- `ACCESS_COARSE_LOCATION` - For cell tower location
- `READ_PRECISE_PHONE_STATE` - Android 12+ for detailed cell info
- `RECEIVE_SMS` - Monitor incoming SMS

### Background Monitoring:
- Use `TelephonyCallback` (Android 12+) for state changes
- Implement foreground service for continuous monitoring
- Use WorkManager for periodic checks

### Data Storage:
- Realm database for efficient cell history storage
- SQLite alternative for simpler implementations
- Store: timestamp, LAC/TAC, CID, signal strength, encryption status

## UI/UX Considerations

### Dashboard Should Display:
- Current threat level (color-coded)
- Active cell tower info (LAC/TAC/CID)
- Signal strength visualization
- Encryption status indicator
- Recent alerts/anomalies
- Historical threat timeline

### Map Integration:
- Show current cell tower location (if available)
- Display neighbor cells
- Visualize signal strength coverage
- Mark suspicious towers

### Alerts:
- Real-time notifications for threats
- Detailed threat explanations
- Recommended actions
- Historical alert log

## Material You Design Integration
- Use dynamic color system based on Android theme
- Implement adaptive layouts for different screen sizes
- Use Material Design 3 components
- Smooth transitions and animations
- Dark mode support
