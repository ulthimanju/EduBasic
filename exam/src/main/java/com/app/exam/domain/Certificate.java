package com.app.exam.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Getter
@Setter
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", unique = true)
    private StudentAttempt attempt;

    @Column(name = "certificate_url", length = 500)
    private String certificateUrl;

    @Column(name = "issued_at")
    private OffsetDateTime issuedAt = OffsetDateTime.now();
}
