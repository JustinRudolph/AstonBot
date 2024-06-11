package com.justinrudolph.discordbot.listeners;

import com.justinrudolph.discordbot.models.AstonDevMember;
import com.justinrudolph.discordbot.repositories.MemberRepository;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RoleUpdateListener extends ListenerAdapter {

    private final MemberRepository memberRepository;

    public RoleUpdateListener(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        Long userId = event.getMember().getIdLong();
        AstonDevMember user = memberRepository.findById(userId).orElse(null);
        if (user == null) {
            user = new AstonDevMember();
            user.setId(userId);
            user.setName(event.getMember().getEffectiveName());
            user.setRoles(new ArrayList<>());
        }
        List<String> roleNames = event.getRoles().stream()
                                .map(Role::getName)
                                .toList();
        user.addRoles(roleNames);
        memberRepository.save(user);
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        Long userId = event.getMember().getIdLong();
        AstonDevMember user = memberRepository.findById(userId).orElse(null);
        List<String> roleNames = event.getRoles().stream()
                                .map(Role::getName)
                                .toList();
        user.removeRoles(roleNames);
        memberRepository.save(user);

    }
}
