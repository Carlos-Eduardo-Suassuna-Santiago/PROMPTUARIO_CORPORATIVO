package br.edu.ifrn.promptuario.reporting.repository;

import br.edu.ifrn.promptuario.reporting.model.ReportJob;
import br.edu.ifrn.promptuario.reporting.model.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReportJobRepository extends JpaRepository<ReportJob, String> {

    Page<ReportJob> findByRequestedByOrderByCreatedAtDesc(String requestedBy, Pageable pageable);

    Page<ReportJob> findByStatus(ReportStatus status, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE ReportJob job SET job.status = :running "
            + "WHERE job.id = :jobId AND job.status = :pending")
    int claimPendingJob(@Param("jobId") String jobId,
                        @Param("pending") ReportStatus pending,
                        @Param("running") ReportStatus running);
}