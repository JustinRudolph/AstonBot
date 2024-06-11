package com.justinrudolph.discordbot.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "algo")
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Algo {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NonNull
    private String title;

    @NonNull
    private String link;

    @NonNull
    @Lob
    @Column(name = "code", columnDefinition = "LONGTEXT")
    private String code;

    @NonNull
    private LocalDate dateSubmitted;

    @ManyToOne
    @JoinColumn(name = "member_id")
    @NonNull
    private AstonDevMember astonDevMember;

}
