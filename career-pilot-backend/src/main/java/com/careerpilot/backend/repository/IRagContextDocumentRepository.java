package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.RagContextDocument;
import com.careerpilot.backend.entity.ENUMs.DocType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IRagContextDocumentRepository extends JpaRepository<RagContextDocument, Long> {
    List<RagContextDocument> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<RagContextDocument> findByUserIdAndDocTypeOrderByCreatedAtDesc(Long userId, DocType docType);
}
