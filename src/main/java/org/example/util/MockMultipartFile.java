package org.example.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockMultipartFile implements MultipartFile {

    private String name;
    private String originalFileName;
    private String contentType;
    private byte[] fileContent;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFileName;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return fileContent.length == 0;
    }

    @Override
    public long getSize() {
        return fileContent.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return fileContent;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(fileContent);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        try (InputStream inputStream = getInputStream()) {
            java.nio.file.Files.copy(inputStream, dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}