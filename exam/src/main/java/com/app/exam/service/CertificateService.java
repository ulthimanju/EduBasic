package com.app.exam.service;

import com.app.exam.domain.Certificate;
import com.app.exam.domain.StudentAttempt;
import com.app.exam.dto.CertificateResponse;
import com.app.exam.repository.CertificateRepository;
import com.app.exam.repository.StudentAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final StudentAttemptRepository attemptRepository;

    @Transactional
    public void generateCertificate(UUID attemptId) {
        StudentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        log.info("Generating certificate for attempt: {}", attemptId);

        String fileName = "cert_" + attemptId + ".pdf";
        String storagePath = "certificates/" + fileName;

        try {
            new File("certificates").mkdirs();
            
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 24);
                    contentStream.newLineAtOffset(100, 700);
                    contentStream.showText("CERTIFICATE OF COMPLETION");
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 16);
                    contentStream.newLineAtOffset(100, 650);
                    contentStream.showText("This is to certify that student");
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                    contentStream.newLineAtOffset(100, 620);
                    contentStream.showText(attempt.getStudentId().toString());
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 16);
                    contentStream.newLineAtOffset(100, 590);
                    contentStream.showText("has successfully completed the exam:");
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                    contentStream.newLineAtOffset(100, 560);
                    contentStream.showText(attempt.getExam().getTitle());
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                    contentStream.newLineAtOffset(100, 500);
                    contentStream.showText("Score: " + attempt.getScore());
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contentStream.newLineAtOffset(100, 450);
                    contentStream.showText("Date: " + OffsetDateTime.now());
                    contentStream.endText();
                }

                document.save(storagePath);
            }

            Certificate certificate = certificateRepository.findByAttemptId(attemptId)
                    .orElseGet(() -> {
                        Certificate c = new Certificate();
                        c.setAttempt(attempt);
                        return c;
                    });
            
            certificate.setCertificateUrl(storagePath); // In production, this would be S3 URL
            certificate.setIssuedAt(OffsetDateTime.now());
            certificateRepository.save(certificate);

            log.info("Certificate saved for attempt: {}", attemptId);

        } catch (Exception e) {
            log.error("Failed to generate certificate for attempt: {}", attemptId, e);
        }
    }

    public CertificateResponse getCertificate(UUID studentId, UUID attemptId) {
        Certificate certificate = certificateRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));

        if (!certificate.getAttempt().getStudentId().equals(studentId)) {
            throw new RuntimeException("Unauthorized: Attempt does not belong to student");
        }

        return CertificateResponse.builder()
                .id(certificate.getId())
                .attemptId(certificate.getAttempt().getId())
                .certificateUrl(certificate.getCertificateUrl())
                .issuedAt(certificate.getIssuedAt())
                .build();
    }
}
