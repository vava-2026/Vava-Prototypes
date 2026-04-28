
LICENSE GENERATOR — Standalone script.

  Generates a signed, tamper-proof license JSON in the current directory.

Usage:
  java GenerateLicense.java <userEmail> <plan> <durationValue> <durationUnit> [outputFile]

  durationUnit: MINUTES | HOURS | DAYS | MONTHS

Examples:<br>
  java GenerateLicense.java alice@example.com PRO 30 DAYS<br>
  java GenerateLicense.java bob@example.com PRO 90 MINUTES<br>
  java GenerateLicense.java carol@company.com ENTERPRISE 12 MONTHS<br>

On first run, RSA key pair is generated and saved to:
  private_key.pem  — keep secret, used only for generation
  public_key.pem   — embed in your application

Output: <outputFile>.json (default: license_<uuid>.json)

License JSON structure:
{
  "licenseId"     : unique UUID per license,
  "userEmail"     : licensee identifier,
  "plan"          : license tier (PRO, ENTERPRISE, …),
  "issuedAt"      : ISO-8601 timestamp,
  "expiresAt"     : ISO-8601 timestamp,
  "nonce"         : random bytes (ensures uniqueness even for same params),
  "payload"       : AES-encrypted license data (base64),
  "payloadIv"     : AES IV (base64),
  "payloadKey"    : AES key encrypted with RSA private key (base64),
  "signature"     : RSA-SHA256 signature over canonical fields (base64),
  "publicKeyHint" : SHA-256 fingerprint of the public key used
}

Security model:
  - The license body fields (id, email, plan, dates, nonce) are signed with
    RSA-SHA256 using the PRIVATE key.  Your app verifies with the PUBLIC key.
  - Additionally, sensitive payload data is AES-256 encrypted; the AES key
    is wrapped (encrypted) with the PRIVATE key so only holder of the PUBLIC
    key can unwrap it — this is intentionally reversed from normal asymmetric
    encryption to allow the app to decrypt using the embedded public key.
    (In practice: sign = private, verify = public; encrypt = public, decrypt = private.
     Here we use private-key encryption / public-key decryption as a "seal"
     that any holder of the public key can open and verify originates from you.)

