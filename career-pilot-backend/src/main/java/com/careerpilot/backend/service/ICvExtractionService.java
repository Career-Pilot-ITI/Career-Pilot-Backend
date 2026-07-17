package com.careerpilot.backend.service;

import java.io.IOException;
import java.nio.file.Path;


public interface ICvExtractionService {
  String extractCv(Path filepath) throws IOException;
}
