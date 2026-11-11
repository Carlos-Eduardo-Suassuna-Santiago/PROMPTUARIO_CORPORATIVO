package br.edu.ifrn.promptuario.backup.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades do storage de objetos (MinIO/S3) sob o prefixo {@code app.storage.s3}.
 *
 * <p>Prefixo distinto de {@code promptuario.backup} para reutilizar a mesma
 * convenção já adotada pelos demais serviços do PROMPTUÁRIO.
 *
 * @param endpoint      URL do endpoint S3 (MinIO)
 * @param accessKey     chave de acesso
 * @param secretKey     chave secreta
 * @param region        região S3 (obrigatória para assinatura, mesmo no MinIO)
 * @param bucketBackups bucket onde os artefatos de backup são armazenados
 */
@Validated
@ConfigurationProperties(prefix = "app.storage.s3")
public record StorageProperties(
        @NotBlank String endpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String region,
        @NotBlank String bucketBackups) {
}
