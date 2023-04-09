package net.tjado.webauthn.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class Pkcs8PemParser {

    public static final String TAG = "Pkcs8PemParser";

    public enum Type {
        KEY,
        CERT
    }

    public static byte[] toBytes(String input, Type type) throws IOException {
        StringBuilder pkcs8Lines = new StringBuilder();
        BufferedReader rdr = new BufferedReader(new StringReader(input));
        String line;
        while ((line = rdr.readLine()) != null) {
            pkcs8Lines.append(line);
        }

        String pkcs8Pem = pkcs8Lines.toString();
        switch (type) {
            case KEY:
                pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
                pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
                break;
            case CERT:
                pkcs8Pem = pkcs8Pem.replace("-----BEGIN CERTIFICATE-----", "");
                pkcs8Pem = pkcs8Pem.replace("-----END CERTIFICATE-----", "");
                break;
            default:
                // unhandled
        }
        pkcs8Pem = pkcs8Pem.replaceAll("\\s+","");
        return Base64.getDecoder().decode(pkcs8Pem);
    }

    public static PrivateKey toPrivateKey(String pk, KeyFactory kf)
            throws IOException, InvalidKeySpecException {
        byte[] encodedBytes = Pkcs8PemParser.toBytes(pk, Type.KEY);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedBytes);
        return kf.generatePrivate(keySpec);
    }

    public static Certificate toCertificate(String cert, CertificateFactory cf)
            throws IOException, CertificateException {
        byte[] bCert = Pkcs8PemParser.toBytes(cert, Type.CERT);
        return cf.generateCertificate(new ByteArrayInputStream(bCert));
    }
}
