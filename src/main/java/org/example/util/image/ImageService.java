package org.example.util.image;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final MinioClient minioClient;

    @Value("${spring.minio.bucketName}")
    private String bucketName;

    public String uploadImage(MultipartFile file) throws Exception {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
        try (InputStream inputStream = file.getInputStream()) {
            log.info(String.format("""
                    Размер файла в байтах: %s
                    Тип файла: %s
                    Имя файла: %s""",
                    file.getSize(), file.getContentType(), fileName));
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        return fileName;
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