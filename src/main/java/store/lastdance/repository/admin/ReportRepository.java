package store.lastdance.repository.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import store.lastdance.domain.admin.Report;
import store.lastdance.domain.admin.ReportStatus;

import java.util.Collection;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, Long> {
    long countByReportedUserId(UUID userId);

    Collection<Report> findByReportedUserId(UUID userId);

    Page<Report> findAll(Specification<Report> spec, Pageable pageable);

    boolean existsByReportId(Long reportId);

    Report findByReportId(Long reportId);

    Object countByStatus(ReportStatus reportStatus);
}
