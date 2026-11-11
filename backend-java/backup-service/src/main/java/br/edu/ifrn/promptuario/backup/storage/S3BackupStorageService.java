package br.edu.ifrn.promptuario.backup.storage;

import br.edu.ifrn.promptuario.backup.config.StorageProperties;
import br.edu.ifrn.promptuario.backup.model.BackupKind;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Persistência dos artefatos no MinIO (S3 compatível) via AWS SDK v2.
 *
 * <p>Estrutura de chaves:
 * <pre>backups/{kind}/{dbname}/{yyyy}/{MM}/{dd}/{filename}</pre>
 */
@Service
public class S3BackupStorageService {

    private static final Logger log = LoggerFactory.getLogger(S3BackupStorageService.class);

    /** Limite do protocolo S3 por requisição de deleção em lote. */
    private static final int DELETE_BATCH_SIZE = 1000;
    private static final String KEY_PREFIX = "backups";
    private static final String CONTENT_TYPE = "application/octet-stream";
    private static final int HTTP_NOT_FOUND = 404;

    private final S3Client s3Client;
    private final StorageProperties storageProperties;

    public S3BackupStorageService(S3Client s3Client, StorageProperties storageProperties) {
        this.s3Client = s3Client;
        this.storageProperties = storageProperties;
    }

    /** Bucket de backups configurado. */
    public String defaultBucket() {
        return storageProperties.bucketBackups();
    }

    /** Cria o bucket caso ele ainda não exista (idempotente). */
    public void ensureBucketExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            log.debug("Bucket '{}' já existe.", bucketName);
        } catch (NoSuchBucketException e) {
            createBucket(bucketName);
        } catch (S3Exception e) {
            // Alguns servidores S3 compatíveis (MinIO) respondem 404 genérico.
            if (e.statusCode() == HTTP_NOT_FOUND) {
                createBucket(bucketName);
                return;
            }
            throw e;
        }
    }

    /** Monta a chave do objeto preservando o layout histórico de pastas. */
    public String buildObjectKey(BackupKind kind, String dbname, String filename) {
        LocalDate date = LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC);
        return "%s/%s/%s/%04d/%02d/%02d/%s".formatted(
                KEY_PREFIX, kind.folder(), dbname,
                date.getYear(), date.getMonthValue(), date.getDayOfMonth(), filename);
    }

    /**
     * Envia o artefato local para o MinIO com metadados de integridade
     * (SHA-256, alvo, timestamp e tamanho).
     *
     * @return a chave do objeto gravado
     */
    public String uploadArtifact(Path localFile, String key, Map<String, String> metadata) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(defaultBucket())
                .key(key)
                .contentType(CONTENT_TYPE)
                .metadata(metadata)
                .build();
        s3Client.putObject(request, RequestBody.fromFile(localFile));
        log.info("Artefato enviado para s3://{}/{}", defaultBucket(), key);
        return key;
    }

    /**
     * Expurga em lote (até 1000 chaves por requisição) os objetos anteriores ao
     * cutoff.
     *
     * @return quantidade de objetos removidos
     */
    public int deleteObjectsOlderThan(Instant cutoff) {
        List<ObjectIdentifier> expired = new ArrayList<>();
        ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(defaultBucket()).build();

        for (var page : s3Client.listObjectsV2Paginator(request)) {
            for (S3Object object : page.contents()) {
                if (object.lastModified() != null && object.lastModified().isBefore(cutoff)) {
                    expired.add(ObjectIdentifier.builder().key(object.key()).build());
                }
            }
        }
        if (expired.isEmpty()) {
            return 0;
        }

        int removed = 0;
        for (int index = 0; index < expired.size(); index += DELETE_BATCH_SIZE) {
            List<ObjectIdentifier> batch =
                    expired.subList(index, Math.min(index + DELETE_BATCH_SIZE, expired.size()));
            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(defaultBucket())
                    .delete(Delete.builder().objects(batch).build())
                    .build());
            removed += batch.size();
        }
        log.info("Retenção MinIO: {} objeto(s) expirado(s) removido(s).", removed);
        return removed;
    }

    /**
     * Baixa um artefato do MinIO para um caminho local (diretório de staging),
     * permitindo restaurar diretamente a partir de uma chave remota.
     *
     * @return o caminho local gravado
     */
    public Path downloadArtifact(String key, Path destination) {
        try {
            if (destination.getParent() != null) {
                Files.createDirectories(destination.getParent());
            }
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(defaultBucket())
                    .key(key)
                    .build();
            s3Client.getObject(request, ResponseTransformer.toFile(destination));
            log.info("Artefato baixado do MinIO: s3://{}/{} -> {}", defaultBucket(), key, destination);
            return destination;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Falha ao preparar o destino de staging " + destination + ": " + e.getMessage(), e);
        }
    }

    private void createBucket(String bucketName) {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        log.info("Bucket '{}' criado.", bucketName);
    }
}
