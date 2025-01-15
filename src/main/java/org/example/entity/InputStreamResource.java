package org.example.entity;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamResource implements AutoCloseable {
    private final InputStream inputStream;

    public InputStreamResource(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}