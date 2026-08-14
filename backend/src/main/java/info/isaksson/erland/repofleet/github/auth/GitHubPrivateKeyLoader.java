package info.isaksson.erland.repofleet.github.auth;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@ApplicationScoped
public class GitHubPrivateKeyLoader {

    public PrivateKey load(String inlinePem, String pemPath) {
        String pem = resolvePem(inlinePem, pemPath);
        try {
            byte[] keyBytes;
            if (pem.contains("-----BEGIN PRIVATE KEY-----")) {
                keyBytes = decodePem(pem, "PRIVATE KEY");
            } else if (pem.contains("-----BEGIN RSA PRIVATE KEY-----")) {
                keyBytes = wrapPkcs1AsPkcs8(decodePem(pem, "RSA PRIVATE KEY"));
            } else {
                throw new IllegalArgumentException("Unsupported GitHub App private key PEM format.");
            }
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("Could not parse GitHub App private key.", ex);
        }
    }

    private String resolvePem(String inlinePem, String pemPath) {
        if (inlinePem != null && !inlinePem.isBlank()) {
            return inlinePem.replace("\\n", "\n").trim();
        }
        if (pemPath != null && !pemPath.isBlank()) {
            try {
                return Files.readString(Path.of(pemPath)).trim();
            } catch (IOException ex) {
                throw new IllegalArgumentException("Could not read GitHub App private key file.", ex);
            }
        }
        throw new GitHubAppNotConfiguredException("GitHub App private key is not configured.");
    }

    private byte[] decodePem(String pem, String type) {
        String base64 = pem
            .replace("-----BEGIN " + type + "-----", "")
            .replace("-----END " + type + "-----", "")
            .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private byte[] wrapPkcs1AsPkcs8(byte[] pkcs1) {
        byte[] version = {0x02, 0x01, 0x00};
        byte[] rsaAlgorithmIdentifier = {
            0x30, 0x0d,
            0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
            0x05, 0x00
        };
        byte[] privateKeyOctetString = der(0x04, pkcs1);
        byte[] body = concat(version, rsaAlgorithmIdentifier, privateKeyOctetString);
        return der(0x30, body);
    }

    private byte[] der(int tag, byte[] value) {
        return concat(new byte[]{(byte) tag}, derLength(value.length), value);
    }

    private byte[] derLength(int length) {
        if (length < 128) {
            return new byte[]{(byte) length};
        }
        int bytes = 0;
        int value = length;
        while (value > 0) {
            bytes++;
            value >>= 8;
        }
        byte[] result = new byte[bytes + 1];
        result[0] = (byte) (0x80 | bytes);
        for (int i = bytes; i > 0; i--) {
            result[i] = (byte) (length & 0xff);
            length >>= 8;
        }
        return result;
    }

    private byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] array : arrays) total += array.length;
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
