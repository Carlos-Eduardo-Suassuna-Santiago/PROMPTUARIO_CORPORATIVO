package br.edu.ifrn.promptuario.backup.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Cliente S3 apontado para o MinIO.
 *
 * <p>{@code forcePathStyle(true)} é obrigatório em MinIO: sem ele o SDK
 * tenta resolver buckets por DNS ({@code bucket.host}), o que falha em
 * ambientes de rede interna.
 */
@Configuration
public class S3Config {

    @Bean
    public S3Client backupS3Client(StorageProperties storageProperties) {
        return S3Client.builder()
                .endpointOverride(URI.create(storageProperties.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(storageProperties.accessKey(), storageProperties.secretKey())))
                .region(Region.of(storageProperties.region()))
                .forcePathStyle(true)
                .build();
    }
}
