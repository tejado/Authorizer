package net.tjado.webauthn.fido.ctap2;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Arrays;

public class Messages {
    // This size is chosen such that it can be transmitted via the HID protocol even if the maximal
    // report size is 48 bytes.
    static public final long MAX_CBOR_MSG_SIZE = 4096L;

    public enum RequestCommandCTAP2 {
        MakeCredential(0x01),
        GetAssertion(0x02),
        GetNextAssertion(0x08),
        GetInfo(0x04),
        ClientPIN(0x06),
        Reset(0x07),
        Selection(0x0B);

        public final byte value;
        private static final ImmutableMap<Byte, RequestCommandCTAP2> REVERSE_MAP = Maps.uniqueIndex(
                Arrays.asList(RequestCommandCTAP2.values()),
                RequestCommandCTAP2::getValue
        );

        private byte getValue() {
            return this.value;
        }

        RequestCommandCTAP2(byte value) {
            this.value = value;
        }
        RequestCommandCTAP2(int value) {
            this((byte)value);
        }

        public static RequestCommandCTAP2 fromByte(byte value) {
            try {
                return REVERSE_MAP.get(value);
            } catch (Exception e) {
                return null;
            }

        }
    }

    public enum AttestationType {
        NONE("none"),
        SELF("packed"),
        BASIC("packed"),
        ANDROID_KEYSTORE("android-key");

        public final String format;

        AttestationType(String format) {
            this.format = format;
        }
    }
    
    static public final long MAKE_CREDENTIAL_CLIENT_DATA_HASH = 0x1L;
    static public final long MAKE_CREDENTIAL_RP = 0x2L;
    static public final long MAKE_CREDENTIAL_USER = 0x3L;
    static public final long MAKE_CREDENTIAL_PUB_KEY_CRED_PARAMS = 0x4L;
    static public final long MAKE_CREDENTIAL_EXCLUDE_LIST = 0x5L;
    static public final long MAKE_CREDENTIAL_EXTENSIONS = 0x6L;
    static public final long MAKE_CREDENTIAL_OPTIONS = 0x7L;
    static public final long MAKE_CREDENTIAL_PIN_AUTH = 0x8L;
    static public final long MAKE_CREDENTIAL_PIN_PROTOCOL = 0x9L;
    static public final long MAKE_CREDENTIAL_RESPONSE_FMT = 0x1L;
    static public final long MAKE_CREDENTIAL_RESPONSE_AUTH_DATA = 0x2L;
    static public final long MAKE_CREDENTIAL_RESPONSE_ATT_STMT = 0x3L;
    static public final long GET_ASSERTION_RP_ID = 0x1L;
    static public final long GET_ASSERTION_CLIENT_DATA_HASH = 0x2L;
    static public final long GET_ASSERTION_ALLOW_LIST = 0x3L;
    static public final long GET_ASSERTION_EXTENSIONS = 0x4L;
    static public final long GET_ASSERTION_OPTIONS = 0x5L;
    static public final long GET_ASSERTION_PIN_AUTH = 0x6L;
    static public final long GET_ASSERTION_PIN_PROTOCOL = 0x7L;
    static public final long GET_ASSERTION_RESPONSE_CREDENTIAL = 0x1L;
    static public final long GET_ASSERTION_RESPONSE_AUTH_DATA = 0x2L;
    static public final long GET_ASSERTION_RESPONSE_SIGNATURE = 0x3L;
    static public final long GET_ASSERTION_RESPONSE_USER = 0x4L;
    static public final long GET_ASSERTION_RESPONSE_NUMBER_OF_CREDENTIALS = 0x5L;
    static public final long GET_INFO_RESPONSE_VERSIONS = 0x1L;
    static public final long GET_INFO_RESPONSE_EXTENSIONS = 0x2L;
    static public final long GET_INFO_RESPONSE_AAGUID = 0x3L;
    static public final long GET_INFO_RESPONSE_OPTIONS = 0x4L;
    static public final long GET_INFO_RESPONSE_MAX_MSG_SIZE = 0x5L;
    static public final long GET_INFO_RESPONSE_PIN_PROTOCOLS = 0x6L;
    // These parts of the GetInfo response are not yet in the public CTAP spec, but have been picked up
    // from
    // https://chromium.googlesource.com/chromium/src/+/acef6fd7468307321aeab22853f2b6d0d5d6462a
    static public final long GET_INFO_RESPONSE_MAX_CREDENTIAL_COUNT_IN_LIST = 0x7L;
    static public final long GET_INFO_RESPONSE_MAX_CREDENTIAL_ID_LENGTH = 0x8L;
    // These parts of the GetInfo response are also not yet public, but have been picked up from
    // https://groups.google.com/a/fidoalliance.org/d/msg/fido-dev/zFbMGu8rfJQ/WE5Wo6tiAgAJ
    static public final long GET_INFO_RESPONSE_TRANSPORTS = 0x9L;
    static public final long GET_INFO_RESPONSE_ALGORITHMS = 0xAL;
    static public final long CLIENT_PIN_PIN_PROTOCOL = 0x1L;
    static public final long CLIENT_PIN_SUB_COMMAND = 0x2L;
    static public final long CLIENT_PIN_KEY_AGREEMENT = 0x3L;
    static public final long CLIENT_PIN_PIN_AUTH = 0x4L;
    static public final long CLIENT_PIN_NEW_PIN_ENC = 0x5L;
    static public final long CLIENT_PIN_PIN_HASH_ENC = 0x6L;
    static public final long CLIENT_PIN_SUB_COMMAND_GET_RETRIES = 0x1L;
    static public final long CLIENT_PIN_SUB_COMMAND_GET_KEY_AGREEMENT = 0x2L;
    static public final long CLIENT_PIN_SUB_COMMAND_SET_PIN = 0x3L;
    static public final long CLIENT_PIN_SUB_COMMAND_CHANGE_PIN = 0x4L;
    static public final long CLIENT_PIN_SUB_COMMAND_GET_PIN_TOKEN = 0x05L;
    static public final long CLIENT_PIN_RESPONSE_KEY_AGREEMENT = 0x1L;
    static public final long CLIENT_PIN_RESPONSE_PIN_TOKEN = 0x2L;
    static public final long CLIENT_PIN_RESPONSE_RETRIES = 0x03L;
    static public final long COSE_ID_ES256 = -7L;
    static public final long COSE_ID_ECDH = -25L;
//    @ExperimentalUnsignedTypes
//    val COSE_KEY_ES256_TEMPLATE = mapOf(
//            1L to CborLong(2), // kty: EC2 key type
//            3L to CborLong(COSE_ID_ES256), // alg: ES256 signature algorithm
//            -1L to CborLong(1) // crv: P-256 curve
//    )
////    @ExperimentalUnsignedTypes
//    val COSE_KEY_ECDH_TEMPLATE = mapOf(
//            1L to CborLong(2), // kty: EC2 key type
//            3L to CborLong(COSE_ID_ECDH), // alg: ECDH key agreement algorithm
//            -1L to CborLong(1) // crv: P-256 curve
//    )
    static public final long COSE_KEY_EC256_X = -2L;
    static public final long COSE_KEY_EC256_Y = -3L;

    final byte FLAGS_USER_PRESENT = (byte)1;
    final byte FLAGS_USER_VERIFIED = (byte)(1 << 2);
    final byte FLAGS_AT_INCLUDED = (byte)(1 << 6);
    final byte FLAGS_ED_INCLUDED = (byte)(1 << 7);
    
    static public final long HMAC_SECRET_KEY_AGREEMENT = 0x1L;
    static public final long HMAC_SECRET_SALT_ENC = 0x2L;
    static public final long HMAC_SECRET_SALT_AUTH = 0x3L;
}
