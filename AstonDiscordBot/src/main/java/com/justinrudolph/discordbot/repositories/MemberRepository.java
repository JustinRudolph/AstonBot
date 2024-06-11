package com.justinrudolph.discordbot.repositories;

import com.justinrudolph.discordbot.models.AstonDevMember;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends CrudRepository<AstonDevMember, Long> {

    List<AstonDevMember> findByRolesContainingIgnoreCase(String roleName);

}
