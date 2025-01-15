package org.example.image;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
public class ImageService {

    private final MinioClient minioClient;
    private final ResizeService resizeService;

    @Value("${spring.minio.bucketName}")
    private String bucketName;

    public ImageService(MinioClient minioClient, ResizeService resizeService) {
        this.minioClient = minioClient;
        this.resizeService = resizeService;
    }

    public String uploadImage(MultipartFile file) throws Exception {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        MultipartFile resizedImage = resizeService.resizeImage(file);
        try (InputStream inputStream = resizedImage.getInputStream()) {
            log.info(String.format("""
                    Размер файла в байтах: %s
                    Тип файла: %s
                    Имя файла: %s""",
                    resizedImage.getSize(), resizedImage.getContentType(), fileName));
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, resizedImage.getSize(), -1)
                            .contentType(resizedImage.getContentType())
                            .build()
            );
        }

        return fileName;
    }

    public String getImageUrl(String fileName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .method(Method.GET)
                        .build()
        );
    }

    public InputStream getFile(String bucketName, String objectUrl) throws Exception {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectUrl)
                .build());
    }

    public void deleteImage(String bucketName, String objecUrl) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objecUrl)
                    .build());
            System.out.println("Изображение успешно удалено из MinIO: " + objecUrl);
        } catch (MinioException e) {
            e.printStackTrace();
            System.err.println("Ошибка при удалении изображения из MinIO: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Неизвестная ошибка при удалении изображения из MinIO.");
        }
    }
}