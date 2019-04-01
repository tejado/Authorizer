# Authorizer [![Build Status](https://travis-ci.org/tejado/Authorizer.svg?branch=master)](https://travis-ci.org/tejado/Authorizer)
A Password Manager for Android with Auto-Type over USB and Bluetooth, OTP and much more.
  
The idea behind Authorizer is, to use old smartphones as a hardware password manager only. To avoid manual typing of long and complex passwords everytime you need them, Authorizer provides Auto-Type features over USB and Bluetooth. It pretends to be a keyboard (e.g. over an USB On-The-Go adapter) and with a button press inside the app, it will automatically type the password for you on your pc, laptop, tablet or other smartphone.  

<a href="https://f-droid.org/packages/net.tjado.passwdsafe/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=net.tjado.passwdsafe" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80"/></a>
  
Authorizer is based on [PasswdSafe](https://sourceforge.net/projects/passwdsafe/) a Password Safe port for Android and [FreeOTP](https://github.com/freeotp/freeotp-android).  

## Features
* Auto-Type over USB
* Auto-Type over Bluetooth (experimental)
* Different keyboard languages ([HID Usage Tables](https://www.usb.org/document-library/hid-usage-tables-112))
  * English (US)
  * English (GB)
  * German
  * German (Apple)
  * German (CH)
  * French (CH)
  * Neo 2 (Layer 1,2 and 3)
* Asymmetric encrypted backup on USB mass storage
* OTP integration (TOTP/HOTP)
* Tree list style
* Icons

### Features in Detail

#### Auto-Type over USB and Bluetooth
<a href="https://www.youtube.com/watch?v=KL2qjMogQMY"><img src="https://img.youtube.com/vi/KL2qjMogQMY/0.jpg" align="right" height="200" alt="Authorizer PoC YouTube video"></a>
Authorizer is able to pretend to be an HID Keyboard so it can auto-type the credentials over USB and Bluetooth.  
There are Auto-Type buttons at the password entry view. If a button is pressed longer, a different keyboard layout can be choosen. Additional, there is a USB Quick Auto-Type button in the TreeView which auto-types the respective password on a long press.  
There are different settings per password entry like delimiter and the password return suffix. In the general App preferences a default keyboard layout can be choosen.

Auto-Type over USB requires currently an Android Kernel compiled with [Android Keyboard Gadget](https://github.com/pelya/android-keyboard-gadget) and root access.

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
* NFC support
* Smartcard emulation (e.g. OpenPGP cards, etc.)
* CTAP + CTAP2 integration for U2F and WebAuthn
* Redesign of the App
* Replacing Android Keyboard Gadget with FunctionFS HID integration (Kernel >=3.18 required)
* Refactoring the HID Keyboard Layout code

##  Requirements
* For Auto-Type over USB: an Android Kernel compiled with [Android Keyboard Gadget](https://github.com/pelya/android-keyboard-gadget) is required
* For Auto-Type over Bluetooth: Android Pie or higher is required. Tested devices:
  * Samsung Galaxy S8: WORKING
  * HTC One M8 (LineageOS 16.0): WORKING

## Contributions & Community
Contributions are highly welcome.  
For support & development discussions around Authorizer, feel free to contact me.

## Proof of Concept
Proof-of-Concept app for Auto-Typing (USB Keyboard emulation): [Authorizer-PoC](https://github.com/tejado/Authorizer-PoC)
