import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `O pacote 'react-native-urovo-rfid' não parece estar linkado. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

export const UrovoRfidNative = NativeModules.UrovoRfidNative
  ? NativeModules.UrovoRfidNative
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

// Exportando o Módulo do Laser Scanner
export const UrovoScannerNative = NativeModules.UrovoScannerNative
  ? NativeModules.UrovoScannerNative
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );