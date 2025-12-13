# LED Lamp Control App

A modern Android application for controlling LED lighting systems via Bluetooth Low Energy (BLE).

## Features

### Core Functionality
- **Bluetooth Connection**: Scan and connect to LED lamps via Bluetooth Low Energy
- **Color Picker**: Interactive color wheel for selecting custom colors
- **Brightness Control**: Adjustable brightness slider (0-100%)
- **Quick Presets**: 
  - Bright Light (100% white)
  - Warm Light (70% warm white)
  - Night Mode (30% soft red)
- **Timer Modes**:
  - Morning Mode (gradual bright light)
  - Evening Mode (warm dimmed light)

### Modern UI/UX
- Material Design 3 components
- Responsive layouts for mobile and tablet devices
- Card-based interface for better organization
- Real-time color preview
- Clear connection status indicators

## Technical Details

### Permissions
The app requests only necessary permissions:
- **Bluetooth**: For LED lamp communication
- **Location**: Required for Bluetooth scanning on Android 10+
- **Wi-Fi**: For future Wi-Fi-based control (optional)

### Architecture
- **Service-based Bluetooth management**: `BluetoothLeServiceSingle` handles all BLE operations
- **Custom Views**: `ColorPickerView` provides an interactive color wheel
- **MVVM-ready structure**: Easy to extend with ViewModels and LiveData

### Requirements
- Android 5.0 (API 21) or higher
- Bluetooth Low Energy support
- Target SDK: Android 14 (API 34)

## Project Structure
```
app/
├── src/main/
│   ├── java/com/ledlamp/
│   │   ├── MainActivity.kt              # Main control panel
│   │   ├── adapters/
│   │   │   └── DeviceListAdapter.kt     # Device list adapter
│   │   ├── bluetooth/
│   │   │   └── BluetoothLeServiceSingle.kt  # BLE service
│   │   ├── models/
│   │   │   └── BleDevice.kt             # Device model
│   │   └── views/
│   │       └── ColorPickerView.kt       # Color picker widget
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml        # Mobile layout
│       │   ├── dialog_device_list.xml   # Device scan dialog
│       │   └── item_device.xml          # Device list item
│       ├── layout-sw600dp/
│       │   └── activity_main.xml        # Tablet layout
│       └── values/
│           ├── colors.xml               # Color definitions
│           ├── strings.xml              # String resources
│           └── themes.xml               # App theme
```

## Building the App
```bash
./gradlew assembleDebug
```

## Installation
```bash
./gradlew installDebug
```

## Design Principles
- **Simplicity**: Focused on essential LED control features
- **Usability**: Intuitive controls with immediate visual feedback
- **Responsiveness**: Adapts to different screen sizes
- **Modern**: Material Design 3 with clean aesthetics

## Future Enhancements
- Wi-Fi connectivity support
- Custom timer schedules
- Scene memory/favorites
- Multi-device control
- Firmware update capability
