package com.justinrudolph.discordbot.repositories;

import com.justinrudolph.discordbot.models.Algo;
import com.justinrudolph.discordbot.models.AstonDevMember;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AlgoRepository extends CrudRepository<Algo, Long> {

    List<Algo> findAllByDateSubmitted(LocalDate date);

    List<Algo> findAllByDateSubmittedBetween(LocalDate start, LocalDate end);

    List<Algo> findByAstonDevMemberAndDateSubmittedBetween(AstonDevMember member, LocalDate start, LocalDate end);
}
