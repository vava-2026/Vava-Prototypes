import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;

public class GenerateLicense {

    private static final String PRIVATE_KEY_FILE = "private_key.pem";
    private static final String PUBLIC_KEY_FILE  = "public_key.pem";

    private static final int RSA_KEY_SIZE = 2048;

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Usage: java GenerateLicense.java <email> <plan> <durationValue> <MINUTES|HOURS|DAYS|MONTHS> [outputFile]");
            System.exit(1);
        }

        String userEmail = args[0];
        String plan = args[1].toUpperCase();
        long durationValue = Long.parseLong(args[2]);
        String durationUnit = args[3].toUpperCase();
        String outputFileName = args.length >= 5 ? args[4] : null;

        KeyPair keyPair = loadOrGenerateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        Instant now = Instant.now();
        Instant expiry = computeExpiry(now, durationValue, durationUnit);

        DateTimeFormatter iso = DateTimeFormatter.ISO_INSTANT;
        String issuedAt = iso.format(now);
        String expiresAt = iso.format(expiry);

        String licenseId = UUID.randomUUID().toString();
        String nonce = generateNonce(32);

        String publicKeyHint = sha256Hex(publicKey.getEncoded());

        String canonical = String.join("|", licenseId, userEmail, plan, issuedAt, expiresAt, nonce);

        String signature = rsaSign(canonical, privateKey);

        String payloadJson = buildPayloadJson(licenseId, userEmail, plan, issuedAt, expiresAt, nonce);

        byte[] aesKeyBytes = generateAesKey();
        byte[] iv = generateIv();
        byte[] encrypted = aesEncrypt(payloadJson.getBytes("UTF-8"), aesKeyBytes, iv);

        String payloadB64 = Base64.getEncoder().encodeToString(encrypted);
        String payloadIvB64 = Base64.getEncoder().encodeToString(iv);

        String payloadKeyB64 = rsaEncryptWithPrivate(aesKeyBytes, privateKey);

        String licenseJson = buildLicenseJson(
                licenseId, userEmail, plan, issuedAt, expiresAt,
                nonce, payloadB64, payloadIvB64, payloadKeyB64,
                signature, publicKeyHint);

        if (outputFileName == null) {
            outputFileName = "license_" + licenseId + ".json";
        } else if (!outputFileName.endsWith(".json")) {
            outputFileName += ".json";
        }

        Files.writeString(Path.of(outputFileName), licenseJson);

        System.out.println("License generated:  " + outputFileName);
        System.out.println("License ID:         " + licenseId);
        System.out.println("User:               " + userEmail);
        System.out.println("Plan:               " + plan);
        System.out.println("Issued at:          " + issuedAt);
        System.out.println("Expires at:         " + expiresAt);
        System.out.println("Public key hint:    " + publicKeyHint.substring(0, 16) + "...");
    }

    private static KeyPair loadOrGenerateKeyPair() throws Exception {
        File privateFile = new File(PRIVATE_KEY_FILE);
        File publicFile  = new File(PUBLIC_KEY_FILE);

        if (privateFile.exists() && publicFile.exists()) {
            System.out.println("Loading existing RSA key pair from disk...");
            PrivateKey priv = loadPrivateKey(PRIVATE_KEY_FILE);
            PublicKey pub = loadPublicKey(PUBLIC_KEY_FILE);
            return new KeyPair(pub, priv);
        }

        System.out.println("Generating new RSA-" + RSA_KEY_SIZE + " key pair.");
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(RSA_KEY_SIZE, new SecureRandom());
        KeyPair pair = gen.generateKeyPair();

        savePem(PRIVATE_KEY_FILE, "PRIVATE KEY", pair.getPrivate().getEncoded());
        savePem(PUBLIC_KEY_FILE, "PUBLIC KEY", pair.getPublic().getEncoded());

        System.out.println("Saved: " + PRIVATE_KEY_FILE + " (keep secret!)");
        System.out.println("Saved: " + PUBLIC_KEY_FILE  + " (embed in app)");
        return pair;
    }

    private static void savePem(String path, String type, byte[] data) throws IOException {
        String encoded = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(data);
        String pem = "-----BEGIN " + type + "-----\n" + encoded + "\n-----END " + type + "-----\n";
        Files.writeString(Path.of(path), pem);
    }

    private static PrivateKey loadPrivateKey(String path) throws Exception {
        String pem = Files.readString(Path.of(path));
        byte[] decoded = decodePem(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private static PublicKey loadPublicKey(String path) throws Exception {
        String pem = Files.readString(Path.of(path));
        byte[] decoded = decodePem(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static byte[] decodePem(String pem) {
        String stripped = pem
                .replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(stripped);
    }

    private static Instant computeExpiry(Instant from, long value, String unit) {
        return switch (unit) {
            case "MINUTES" -> from.plus(Duration.ofMinutes(value));
            case "HOURS" -> from.plus(Duration.ofHours(value));
            case "DAYS" -> from.plus(Duration.ofDays(value));
            case "MONTHS" -> from.atZone(ZoneOffset.UTC)
                    .plusMonths(value).toInstant();
            default -> throw new IllegalArgumentException("Unknown unit: " + unit
                    + ". Use MINUTES, HOURS, DAYS, or MONTHS.");
        };
    }

    private static String generateNonce(int bytes) {
        byte[] buf = new byte[bytes];
        new SecureRandom().nextBytes(buf);
        return Base64.getEncoder().encodeToString(buf);
    }

    private static String rsaSign(String data, PrivateKey key) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(key);
        sig.update(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(sig.sign());
    }

    private static String rsaEncryptWithPrivate(byte[] data, PrivateKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        Signature sig = Signature.getInstance("NONEwithRSA");
        sig.initSign(key);
        byte[] padded = new byte[256];
        Arrays.fill(padded, (byte) 0x00);
        byte[] marker = "AESKEY:".getBytes("UTF-8");
        System.arraycopy(marker, 0, padded, 0, marker.length);
        System.arraycopy(data, 0, padded, marker.length, data.length);
        return rsaRawEncryptPrivate(data, key);
    }

    private static String rsaRawEncryptPrivate(byte[] data, PrivateKey privateKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(data);
        byte[] signedKey = sig.sign();
        byte[] derivedKey = Arrays.copyOf(sha256Bytes(signedKey), 32);
        byte[] iv = Arrays.copyOf(sha256Bytes(("IV:" + Base64.getEncoder().encodeToString(signedKey)).getBytes()), 16);
        byte[] encAesKey = aesEncrypt(data, derivedKey, iv);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(signedKey.length);
        dos.write(signedKey);
        dos.write(encAesKey);
        dos.flush();
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static byte[] generateAesKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }

    private static byte[] generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    private static byte[] aesEncrypt(byte[] data, byte[] key, byte[] iv) throws Exception {
        SecretKeySpec   keySpec    = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec     = new IvParameterSpec(iv);
        Cipher          cipher     = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        return cipher.doFinal(data);
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] sha256Bytes(byte[] data) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }

    private static String buildPayloadJson(
            String id, String email, String plan,
            String issuedAt, String expiresAt, String nonce) {

        return "{\n" +
                "  \"licenseId\": \"" + id + "\",\n" +
                "  \"userEmail\": \"" + email + "\",\n" +
                "  \"plan\": \"" + plan + "\",\n" +
                "  \"issuedAt\": \"" + issuedAt + "\",\n" +
                "  \"expiresAt\": \"" + expiresAt + "\",\n" +
                "  \"nonce\": \"" + nonce + "\"\n"  +
                "}";
    }

    private static String buildLicenseJson(
            String licenseId, String userEmail, String plan,
            String issuedAt, String expiresAt, String nonce,
            String payload, String payloadIv, String payloadKey,
            String signature, String publicKeyHint) {

        return "{\n" +
                "  \"licenseId\": \"" + licenseId + "\",\n" +
                "  \"userEmail\": \"" + userEmail + "\",\n" +
                "  \"plan\": \"" + plan + "\",\n" +
                "  \"issuedAt\": \"" + issuedAt + "\",\n" +
                "  \"expiresAt\": \"" + expiresAt + "\",\n" +
                "  \"nonce\": \"" + nonce + "\",\n" +
                "  \"payload\": \"" + payload + "\",\n" +
                "  \"payloadIv\": \"" + payloadIv + "\",\n" +
                "  \"payloadKey\": \"" + payloadKey + "\",\n" +
                "  \"signature\": \"" + signature + "\",\n" +
                "  \"publicKeyHint\": \"" + publicKeyHint + "\"\n" +
                "}\n";
    }
}
