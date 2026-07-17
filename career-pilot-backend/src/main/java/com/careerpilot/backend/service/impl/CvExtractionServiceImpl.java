package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.service.ICvExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class CvExtractionServiceImpl implements ICvExtractionService {

    private final Tika tika = new Tika();

    @Override
    public String extractCv(Path path) throws IOException {
        if (!Files.exists(path) || Files.size(path) == 0) {
            throw new IOException("File not found or empty: " + path);
        }
        long bytes = Files.size(path);
        if (bytes > 1024 * 1024 * 10) {
            throw new IOException("File size exceeds 10MB limit");
        }

        try (InputStream is = Files.newInputStream(path)) {
            return tika.parseToString(is);
        } catch (TikaException e) {
            log.error("Tika failed to extract text from: {}", path, e);
            throw new IOException("Failed to extract text from CV file", e);
        }
    }
}
