package br.edu.ifrn.promptuario.modules.clinical.repository;

import br.edu.ifrn.promptuario.modules.clinical.model.ExamRequestHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamRequestHistoryRepository extends JpaRepository<ExamRequestHistory, String> {

    Page<ExamRequestHistory> findByExam_IdOrderByCreatedAtDesc(String examId, Pageable pageable);
}
