package net.tjado.webauthn.models;


import androidx.annotation.NonNull;
import net.tjado.passwdsafe.file.PasswdFileData;
import net.tjado.webauthn.exceptions.VirgilException;
import org.pwsafe.lib.file.PwsRecord;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import javax.crypto.SecretKey;


public interface ICredentialSafe {

    public PublicKeyCredentialSource generateCredential(@NonNull String rpEntityId, String rpDisplayName,
                                                        @NonNull String u2fRpId) throws VirgilException;
    public PublicKeyCredentialSource generateCredential(@NonNull String rpEntityId, String rpDisplayName,
                                                        byte[] userHandle, String userName,
                                                        String userDisplayName, boolean generateHmacSecret) throws VirgilException ;
    public PublicKeyCredentialSource generateCredential(@NonNull String rpEntityId, String rpDisplayName,
                                                        byte[] userHandle, String userName,
                                                        String userDisplayName, boolean generateHmacSecret,
                                                        String u2fRpId) throws VirgilException;
    String keyToString(Key key) ;
    PublicKey base64ToPublicKey(String base64Key) throws NoSuchAlgorithmException, InvalidKeySpecException ;
    PrivateKey base64ToPrivateKey(String base64Key) throws NoSuchAlgorithmException, InvalidKeySpecException ;
    SecretKey base64ToSecretKey(String base64Key);
    PublicKeyCredentialSource recordToCredential(PasswdFileData fileData, PwsRecord rec);
    SecretKey generateSymmetricKey();
    void deleteCredential(PublicKeyCredentialSource credentialSource);
    void deleteAllCredentials(List<PublicKeyCredentialSource> credentialList);
    void deleteAllCredentials();
    List<PublicKeyCredentialSource> getAllCredentials();
    List<PublicKeyCredentialSource> getKeysForEntity(@NonNull String rpEntityId);
    PublicKeyCredentialSource getCredentialSourceById(@NonNull byte[] id) ;
    Boolean credentialsInHardware() ;
    int incrementCredentialUseCounter(PublicKeyCredentialSource credential);
    KeyPair keyAgreementPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;
}
