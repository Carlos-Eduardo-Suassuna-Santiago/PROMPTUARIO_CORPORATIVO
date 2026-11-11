package br.edu.ifrn.promptuario.backup.service;

import br.edu.ifrn.promptuario.backup.config.BackupProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Criptografia autenticada dos artefatos de backup (AES-256-GCM).
 *
 * <p>Layout do artefato gravado em disco/MinIO:
 * <pre>
 *   +----------------+-----------------------------------------------+
 *   | IV (12 bytes)  | ciphertext + tag de autenticação GCM (16 bytes) |
 *   +----------------+-----------------------------------------------+
 * </pre>
 *
 * <p>O IV é gerado por {@link SecureRandom} para cada arquivo e prefixado no
 * início do stream de saída, permitindo que a decifragem seja autocontida (não
 * há metadados externos nem reutilização de IV).
 */
@Service
public class CryptoService {

    /** Transformação autenticada exigida pela arquitetura. */
    public static final String TRANSFORMATION = "AES/GCM/NoPadding";

    /** Algoritmo da chave simétrica. */
    public static final String KEY_ALGORITHM = "AES";

    /** Tamanho do IV exigido pelo GCM (12 bytes). */
    public static final int GCM_IV_LENGTH = 12;

    /** Tamanho da tag de autenticação (128 bits). */
    public static final int GCM_TAG_LENGTH_BITS = 128;

    /** Tamanho da chave AES-256 (32 bytes). */
    public static final int AES_256_KEY_LENGTH_BYTES = 32;

    /** Rótulo gravado no manifesto quando há criptografia. */
    public static final String TRANSFORMATION_LABEL = "AES-256-GCM";

    /** Rótulo gravado no manifesto quando a criptografia está desabilitada. */
    public static final String PLAINTEXT_LABEL = "NONE";

    private static final Logger log = LoggerFactory.getLogger(CryptoService.class);

    private final SecureRandom secureRandom = new SecureRandom();
    private final BackupProperties backupProperties;

    public CryptoService(BackupProperties backupProperties) {
        this.backupProperties = backupProperties;
    }

    /** {@code true} quando a criptografia está habilitada na configuração. */
    public boolean isEnabled() {
        return backupProperties.encryptionEnabled();
    }

    /** Rótulo do algoritmo para o manifesto ({@value #TRANSFORMATION_LABEL} ou {@value #PLAINTEXT_LABEL}). */
    public String transformationLabel() {
        return isEnabled() ? TRANSFORMATION_LABEL : PLAINTEXT_LABEL;
    }

    /** Chave simétrica ativa, derivada do segredo configurado. */
    public SecretKey activeKey() {
        return deriveKey(backupProperties.encryptionSecret());
    }

    /**
     * Deriva uma chave AES-256 a partir do segredo textual.
     *
     * <p>Segredos com exatamente 32 bytes são usados como estão; caso contrário
     * aplica-se SHA-256 (que produz exatamente 32 bytes), garantindo sempre uma
     * chave de 256 bits — nunca uma chave curta/frágil.
     */
    public SecretKey deriveKey(String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length == AES_256_KEY_LENGTH_BYTES) {
            return new SecretKeySpec(raw, KEY_ALGORITHM);
        }
        try {
            byte[] derived = MessageDigest.getInstance("SHA-256").digest(raw);
            log.debug("Segredo de {} bytes derivado via SHA-256 para chave AES-256.", raw.length);
            return new SecretKeySpec(derived, KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível para derivação da chave AES", e);
        }
    }

    /**
     * Cria o stream de cifragem: gera um IV novo, grava os 12 bytes do IV no
     * início de {@code destination} e devolve o {@link CipherOutputStream}
     * pronto para receber o conteúdo a ser cifrado.
     */
    public CipherOutputStream createEncryptingStream(OutputStream destination, SecretKey key) throws IOException {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, key, iv);
        destination.write(iv);
        return new CipherOutputStream(destination, cipher);
    }

    /**
     * Cria o stream de decifragem: lê os 12 bytes do IV do início de
     * {@code source} e devolve o {@link CipherInputStream} pronto para leitura.
     */
    public CipherInputStream createDecryptingStream(InputStream source, SecretKey key) throws IOException {
        byte[] iv = source.readNBytes(GCM_IV_LENGTH);
        if (iv.length != GCM_IV_LENGTH) {
            throw new IOException("Artefato criptografado inválido: IV ausente ou truncado");
        }
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE, key, iv);
        return new CipherInputStream(source, cipher);
    }

    private Cipher initCipher(int mode, SecretKey key, byte[] iv) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(mode, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return cipher;
        } catch (GeneralSecurityException e) {
            throw new IOException("Falha ao inicializar o cipher " + TRANSFORMATION, e);
        }
    }
}
