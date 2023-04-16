# Authorizer ![CI](https://github.com/tejado/Authorizer/workflows/CI/badge.svg?branch=master&event=push)
<img src="https://user-images.githubusercontent.com/3774136/232328062-ed34e91c-d239-499f-9a48-b8f6a19820ed.png" align="right" height="400" alt="Authorizer Screenshot">
**Transform Android devices into secure, offline password managers with USB/Bluetooth Auto-Type, OTP, and FIDO support.**
  
Use your Android device as a dedicated hardware password manager. It avoids manual typing of lengthy and complicated passwords by offering USB and Bluetooth Auto-Type features. Acting as a keyboard, Authorizer enables users to automatically input passwords on their PC, laptop, tablet, or another smartphone with a simple in-app button press.

By having your Authorizer-device offline using airplane mode, you create a physical separation between your credentials and the devices commonly used for daily activities. Similar to Security Keys but with enhanced functionality and comfort.
This concept helps reduce the likelihood of password breaches and unauthorized access, ensuring stored credentials remain secure from online threats and unrelated apps.

Encrypted offline backups eliminates the risk of security breaches in online service-backends, such as the recent LastPass hack. Even if you don't fully trust the Authorizer app, you can maintain security as long as your Authorizer-device's underlying OS provides network isolation and data encryption.

<a href="https://f-droid.org/packages/net.tjado.passwdsafe/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=net.tjado.passwdsafe" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80"/></a>
<a href="https://github.com/tejado/Authorizer/releases" target="_blank">
<img src="https://user-images.githubusercontent.com/3774136/232325067-c6e08d0b-0383-4d80-8e09-b6bf2a55c170.png" alt="Get it on GitHub" height="80"/></a>

## Table of Contents
- [Features](#features)
- [Getting started ](#getting-started)
	- [Device Recommendation](#device-recommendation)
	- [Requirements](#requirements)
	- [Compatibility](#compatibility)
- [Features in Detail](#features-in-detail)
- [Roadmap](#roadmap)
- [FAQ](#faq)
- [Limitations and Known Issues](#limitations-and-known-issues)
- [Contributions & Community](#contributions--community)
- [Special Thanks](#special-thanks)
- [Privacy Policy](#privacy-policy)

## Features
- Auto-Type over USB
- Auto-Type over Bluetooth
- Auto-type keyboard layouts for English, German, French, Apple and Neo 2
- Bluetooth FIDO U2F & WebAuthn integration
- OTP integration (TOTP/HOTP)
- Asymmetric encrypted offline backup
- Auto-type keyboard-"commands" like TAB or ENTER for Username and Password fields
- Predefined usernames with placeholders for fast record creation
- Tree list
- Icons
- pwsafe3 file compatible (*)
- Yubikey support (*)
- Biometric protection of your file password (*)
- Time-based file-close (*)
- Screen off can trigger file-close (*)
- Read-only file mode (*)
- Record protection (*)
- Local file backup (*)
- Password expirations (*)
- Password Policies (*)
- Password History (*)
- Notes (*)
- Groups (*)
- Shortcut & alias records (*)
- Home screen widgets (*)
- Powerful Search (*)
- Light & Dark mode (*)

\* As Authorizer has its foundation in PasswdSafe for Android, it has also adopted these features.

## Getting started 
  
### Device Recommendation
For security and privacy reasons, the recommended device is any maintained Google Pixel with GrapheneOS.  
Other devices might work as well. But as a lot of smartphone vendors are not publishing its underyling kernel and Android source, they can't be recommended.

### Requirements
Authorizer can run on every Android device with version 5 or higher (Lollipop: API/SDK level 21).  
For Bluetooth features, minimum version 9 is required (Pie: API/SDK level 28). Higher is recommended for stability reasons.

For Auto-Type over USB, low-level root permissions are required to run **[USB Gadget Tool](https://github.com/tejado/android-usb-gadget)**.  
Authorizer does not require root permissions when it is allowed to write to /dev/hidg1 natively (file permissions and selinux needs to be configured for this).

### Compatibility

| Features          | Windows | Linux | MacOS | iOS | Android |
| ----------------- | :-----: | :---: | :---: | :---: | :-----: |
| AutoType - USB    |    X    |   X   |   X   |   X   |    X    |
| AutoType - Bluetooth |    X    |   X   |   X   |   X   |    X    |
| FIDO U2F          |    X    |   X   |       |       |    X    |
| FIDO WebAuthn     |    X    |   X   |       |       |    X    |

## Features in Detail

#### Auto-Type over USB and Bluetooth
Authorizer is able to pretend to be an HID Keyboard so it can auto-type the credentials over USB and Bluetooth.  
There are Auto-Type buttons at the password entry view. If a button is pressed longer, a different keyboard layout can be choosen. Additional, there is a USB Quick Auto-Type button in the TreeView which auto-types the respective password on a long press.  
There are different settings per password entry like delimiter and the password return suffix. In the general App preferences a default keyboard layout can be choosen.

**Auto-Type over USB requires support of the USB HID device role. This can be enabled with my [USB Gadget Tool](https://github.com/tejado/android-usb-gadget).** 

Auto-Type over Bluetooth is currently an experimental feature and only available on Android Pie (9.0) or higher.

#### Asymmetric encrypted backup on USB mass storage
The concept behind Authorizer is to have an offline device. As a consequence, it can't create password file backups over the network. To create backups in a comfortable way, Authorizer will open a backup dialog if it detects a new connected mass-storage (e.g. an USB stick connected over an USB On-The-Go adapter). By pressing "Backup now" in this dialog, a backup folder can be selected. It must contain a GPG public key with the file name "pubkey.asc". The default password file will be encrypted with this GPG key and stored in the selected folder.  
This feature can be enabled over the general App preference "Enable GPG backup on USB storage".

#### OTP integration
Besides standard username & password entries, Authorizer also supports two-factor authentication (2FA) over one-time passwords (OTP). Time-based (TOTP) and HMAC-based (HOTP) one-time passwords are supported.  
The OTP secret can be added to a password entry manually or by scanning a QR code. Afterwards, a press on the empty token field ("------") will generate a new OTP. It is also possible, to auto-type the OTP over USB or Bluetooth by adding {OTP} as a placeholder directly in the username or password, e.g. {OTP} in the password "myPa$sword{OTP}" will be replaced with a newly generated OTP.  
Like username, password and other data, the OTP secret is stored in the password file.

#### Additional Auto-Type placeholders
In addition to the {OTP} placeholder, Authorizer also supports {TAB} and {RET} for the tabulator and return key. Adding these to the username and/or password will result in auto-typing the respective key (tab or return) instead of the placeholder.  
Example: if "peter{TAB}{OTP}" is set as the username, "peter" followed by the tabulator key and a newly generated OTP will be auto-typed.

## Roadmap
Please see [Authorizer Roadmap](https://github.com/users/tejado/projects/3).

## FAQ
In progress

## Limitations and Known Issues
- When Authorizer creates or modifies psafe3 files, it will add extra fields like auto-type settings, FIDO keys, and icons, which may not be displayed when using other software that supports psafe3.
- Running Authorizer app on tablets is currently not tested.
- The experience of Bluetooth-stack stability can differ between devices, as it is dependent on both the Android version and the specific device being used.
- Due to limitations in the Bluetooth-stack, Authorizer can only be paired as Keyboard OR as FIDO Security key and not both.
- It is important to unpair from the other device as well to prevent unexpected behavior, when establishing a new pairing under a separate profile (like Keyboard or FIDO).
- FIDO U2F & WebAuthn is currently not compatible with Apple MacOS and Apple iOS, as they expecting a different HID_REPORT_SIZE.
- Currently, FIDO credentials can't be added to existing records.


## Contributions & Community
Contributions are highly welcome.  
For contributions, discussions and questions around Authorizer, feel free to
- Create an [issue](https://github.com/tejado/Authorizer/issues)
- Create a discussion [discussion](https://github.com/tejado/Authorizer/discussions)

Please note that I am not interested in further localization of Authorizer, except for auto-type keyboard layouts.

## Special Thanks
Authorizer is based on 
- [PasswdSafe](https://sourceforge.net/projects/passwdsafe/) a Password Safe port for Android 
- [FreeOTP](https://github.com/freeotp/freeotp-android)
- [WioKey](https://github.com/WIOsense/wiokey-android)
- and [many further](https://github.com/tejado/Authorizer/tree/master/lib/src/main/assets)

## Privacy Policy
Authorizer does not collect any data from your mobile device.
- Camera access is used only for scanning OTP QR codes.
- Location access only used for Bluetooth device scanning and it is optional.

If you believe this policy has been violated in any way, please file an [issue](https://github.com/tejado/Authorizer/issues).

