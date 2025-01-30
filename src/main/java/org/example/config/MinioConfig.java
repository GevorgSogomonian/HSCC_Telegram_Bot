package org.example.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${spring.minio.url}")
    private String url;

    @Value("${spring.minio.accessKey}")
    private String accessKey;

    @Value("${spring.minio.secretKey}")
    private String secretKey;

    @Value("${spring.minio.bucketName}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .build();

        ensureBucketExists(minioClient, bucketName);

        return minioClient;
    }

    private void ensureBucketExists(MinioClient minioClient, String bucketName) {
        try {
            // Проверяем, существует ли бакет
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (!bucketExists) {
                // Если бакет не существует, создаем его
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                System.out.println("Bucket '" + bucketName + "' created successfully.");
            } else {
                System.out.println("Bucket '" + bucketName + "' already exists.");
            }
        } catch (Exception e) {
            System.err.println("Error occurred while ensuring bucket existence: " + e.getMessage());
            e.printStackTrace();
        }
    }
}