package be.quodlibet.boxable.richtext;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Internal utility for computing cryptographic digests used as cache keys.
 */
final class DigestUtils {

    private DigestUtils() {
        // utility class
    }

    /**
     * Returns the lowercase hex-encoded SHA-256 digest of the given bytes.
     *
     * @param data the bytes to hash
     * @return hex-encoded SHA-256 digest
     */
    static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by the Java specification; this should never happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
