# React Native Urovo RFID & Scanner

A robust library for native integration with **Urovo** data collectors. Developed and tested on the **DT50P**; the **DT50P Lite** shares the same UHF RFID and barcode scanner features and is expected to work, but has not been tested. 
It provides full control over the **UHF RFID** radio module and the **Barcode Scanner (Laser)**, allowing for power adjustments, reading modes, and physical trigger interception.

**Platform:** Android only. Urovo handhelds run Android, so there is no iOS implementation.

---

## Installation

Install the library directly from GitHub:

```bash
npm install react-native-urovo-rfid
```

### Mandatory Requirement (Urovo SDK)
Due to copyright issues, Urovo's proprietary drivers are not included in the public repository. You need to provide them:

1. Obtain the files `URFIDLibrary-vX.aar` and `urovo_platform_sdk_vX.jar` from your supplier.
2. Create a `libs` folder inside `node_modules/react-native-urovo-rfid/android/` and paste both files there.
3. In your main application, open `android/app/build.gradle` and add the following line inside the `dependencies` block:

```gradle
dependencies {
    // ... other dependencies
    implementation files("../../node_modules/react-native-urovo-rfid/android/libs/URFIDLibrary-v2.5.1230.aar")
}
```

---

## API Reference: UHF RFID

Import the RFID module in your code:

```javascript
import { UrovoRfidNative } from 'react-native-urovo-rfid';
```

### Available Methods

| Method | Parameters | Return | Description |
|--------|------------|---------|-----------|
| `initAntenna()` | None | `Promise<boolean>` | Turns on the RFID board and initializes serial communication. Should be called when opening the app. |
| `setPower(power)` | `power: number` (0 to 30) | `Promise<boolean>` | Sets the antenna power. 0 for short distances, 30 for long distances. |
| `setBeep(isEnable)` | `isEnable: boolean` | `Promise<boolean>` | Enables/Disables the native motherboard beep when reading a TAG. |
| `startInventory()` | None | `Promise<boolean>` | Starts continuous reading of multiple TAGs. |
| `stopInventory()` | None | `Promise<boolean>` | Stops reading TAGs. |
| `startRadar(epc)` | `epc: string` | `Promise<boolean>` | Starts search mode. Beeps faster and returns RSSI the closer it gets to the TAG. |
| `stopRadar()` | None | `Promise<boolean>` | Stops search mode. |

---

## API Reference: Scanner (Laser)

Import the Scanner module in your code:

```javascript
import { UrovoScannerNative } from 'react-native-urovo-rfid';
```

### Available Methods

| Method | Parameters | Return | Description |
|--------|------------|---------|-----------|
| `setScannerTriggerMode(mode)` | `mode: 'HOST' \| 'CONTINUOUS' \| 'PULSE'` | `Promise<boolean>` | Changes the trigger behavior for the barcode reader. |

* **`HOST`**: Default. Hold the button to keep the laser on. Release to turn it off.
* **`PULSE`**: Toggle. Press the button once and release, the laser stays on until it reads something.
* **`CONTINUOUS`**: Hands-free. The laser stays on continuously.

---

## Listening to Hardware Events

The library triggers native events in real-time to Javascript. Use React Native's `DeviceEventEmitter` to listen to them:

```javascript
import { useEffect } from 'react';
import { DeviceEventEmitter } from 'react-native';

useEffect(() => {
  // 1. Listen for Read TAGs
  const rfidListener = DeviceEventEmitter.addListener('onRfidRead', (epc) => {
    console.log("Read TAG: ", epc);
  });

  // 2. Listen to Signal Strength (For Radar/Search mode)
  const radarListener = DeviceEventEmitter.addListener('onRadarRssi', (rssi) => {
    console.log("Target TAG signal strength: ", rssi); // from 0 to ~100
  });

  // 3. Listen to Physical Trigger (Collector's Yellow Button)
  const triggerListener = DeviceEventEmitter.addListener('onHardwareTrigger', (isDown) => {
    if (isDown === 'true') {
      console.log("Finger pressed the trigger!");
      // UrovoRfidNative.startInventory()
    } else {
      console.log("Finger released the trigger!");
      // UrovoRfidNative.stopInventory()
    }
  });

  return () => {
    rfidListener.remove();
    radarListener.remove();
    triggerListener.remove();
  };
}, []);
```

---

## Complete Usage Example (Custom Hook)

For the best experience, we recommend isolating the logic into a Custom Hook. Here is a practical example of implementing **Power Control** with debounce:

```javascript
import { useEffect, useRef } from 'react';
import { DeviceEventEmitter } from 'react-native';
import { UrovoRfidNative } from 'react-native-urovo-rfid';

export const useUrovo = (power = 30) => {
  const isReading = useRef(false);

  useEffect(() => {
    // Turns on the antenna when opening the app
    UrovoRfidNative.initAntenna().catch(console.error);
  }, []);

  useEffect(() => {
    // Updates power in real-time (with debounce to prevent freezing the serial port)
    const timer = setTimeout(() => {
        UrovoRfidNative.setPower(power).catch(console.error);
    }, 500);
    return () => clearTimeout(timer);
  }, [power]);

  useEffect(() => {
    const trigger = DeviceEventEmitter.addListener('onHardwareTrigger', (isDown) => {
      if (isDown === 'true') {
        UrovoRfidNative.startInventory();
      } else {
        UrovoRfidNative.stopInventory();
      }
    });
    return () => trigger.remove();
  }, []);
};
```

---

## Acknowledgments

This library was inspired by [react-native-urovo](https://github.com/iliapnmrv/react-native-urovo) by [@iliapnmrv](https://github.com/iliapnmrv), React Native bindings for Urovo barcode scanners.

---

## License

[MIT](LICENSE). The Urovo drivers (`.aar` and `.jar`) are proprietary, are not part of this repository and are not covered by this license.
