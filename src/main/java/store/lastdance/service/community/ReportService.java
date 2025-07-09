package store.lastdance.service.community;

import store.lastdance.dto.community.report.ReportRequestDTO;
import store.lastdance.dto.community.report.ReportResponseDTO;
import java.util.UUID;

public interface ReportService {
    ReportResponseDTO createReport(ReportRequestDTO request, UUID reporterId);
}