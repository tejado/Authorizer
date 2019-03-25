# Authorizer [![Build Status](https://travis-ci.org/tejado/Authorizer.svg?branch=master)](https://travis-ci.org/tejado/Authorizer)
A Password Manager for Android with USB Keyboard emulation.  
  
The idea behind Authorizer is, to use old smartphones as a hardware password manager only. To avoid manual typing of long and complex passwords everytime you need them, Authorizer pretends to be an USB keyboard (e.g. over an USB On-The-Go adapter). With a button press inside the App, it will automatically enters the password for you on your pc, laptop, tablet or main smartphone.  

<a href="https://f-droid.org/packages/net.tjado.passwdsafe/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80"/></a>
<a href="https://play.google.com/store/apps/details?id=net.tjado.passwdsafe" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80"/></a>

#### Video Demo:
[![Alt text](https://img.youtube.com/vi/KL2qjMogQMY/0.jpg)](https://www.youtube.com/watch?v=KL2qjMogQMY)
  
Authorizer is based on [PasswdSafe](https://sourceforge.net/projects/passwdsafe/) a Password Safe port for Android smartphones and [FreeOTP](https://github.com/freeotp/freeotp-android).  

## Features
* USB Keyboard emulation
* Different keyboard languages ([scancodes](https://en.wikipedia.org/wiki/Scancode))
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

#### Asymmetric encrypted backup on USB mass storage
The concept behind Authorizer is to have an offline device. As a consequence, it can't create password file backups over the network. To create backups in a comfortable way, Authorizer will open a backup dialog if it detects a new connected mass-storage (e.g. an USB stick connected over an USB On-The-Go adapter). By pressing "Backup now" in this dialog, a backup folder can be selected. It must contain a GPG public key with the file name "pubkey.asc". The default password file will be encrypted with this GPG key and stored in the selected folder.  
This feature can be enabled over the general App preference "Enable GPG backup on USB storage".

#### OTP integration
Besides standard username & password entries, Authorizer also supports two-factor authentication (2FA) over one-time passwords (OTP). Time-based (TOTP) and HMAC-based (HOTP) one-time passwords are supported.  
The OTP secret can be added to a password entry manually or by scanning a QR code. Afterwards, a press on the empty token field ("------") will generate a new OTP. It is also possible, to send the OTP automatically over USB by adding {OTP} as a placeholder directly in the username or password, e.g. {OTP} in the password "myPa$sword{OTP}" will be replaced with a newly generated OTP.  
Like username, password and other data, the OTP secret is stored in the password file.

#### Additional placeholders
In addition to the {OTP} placeholder, Authorizer also supports {TAB} and {RET} for the tabulator and return key. Adding these to the username and/or password will result in sending the respective key (tab or return) instead of the placeholder.
Example: if "peter{TAB}{OTP}" is set as the username and it will be send over USB, "peter" will be typed followed by the tabulator key followed by a newly generated OTP.

## Roadmap
* Bluetooth support
* NFC support
* Smartcard emulation (e.g. OpenPGP cards, etc.)
* Redesign of the App

##  Requirements for USB Keyboard emulation
* Android Kernel compiled with [Android Keyboard Gadget](https://github.com/pelya/android-keyboard-gadget)

## Contributions & Community
Contributions are highly welcome.  
For support & development discussions around Authorizer, feel free to join our [private Mattermost](https://mm.ramrod.top/signup_user_complete/?id=uhqbkjwkdt865p5i6q75nodmrc).

## Proof of Concept
Proof-of-Concept app for USB Keyboard emulation: [Authorizer-PoC](https://github.com/tejado/Authorizer-PoC)
