package com.justinrudolph.discordbot.services;

import com.justinrudolph.discordbot.commands.HelloWorldCommand;
import com.justinrudolph.discordbot.commands.PostAlgoCommand;
import com.justinrudolph.discordbot.config.DiscordConfig;
import com.justinrudolph.discordbot.listeners.RoleUpdateListener;
import com.justinrudolph.discordbot.models.Algo;
import com.justinrudolph.discordbot.models.AstonDevMember;
import com.justinrudolph.discordbot.repositories.AlgoRepository;
import com.justinrudolph.discordbot.repositories.MemberRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class DiscordBot {

    private final DiscordConfig config;
    private final RoleUpdateListener roleUpdateListener;
    private final CommandManager commandManager;
    private final HelloWorldCommand helloWorldCommand;
    private final PostAlgoCommand postAlgoCommand;
    private final MemberRepository memberRepository;
    private final AlgoRepository algoRepository;

    private static JDA api;

    @PostConstruct
    public void init() {
        try {
            commandManager.add(helloWorldCommand);
            commandManager.add(postAlgoCommand);
            api = JDABuilder
                    .createDefault(config.getBotToken())
                    .enableIntents(GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS))
                    .setActivity(Activity.customStatus("Shut up baby I know it"))
                    .addEventListeners(roleUpdateListener)
                    .addEventListeners(commandManager)
                    .build()
                    .awaitReady();
            fetchAndSaveGuildMembers();
            Guild guild = getGuild();
            Role botRole = guild.getRoleById(config.getBotRoleId());
            SelfUser botUser = api.getSelfUser();
            Member botMember = guild.retrieveMember(botUser).complete();
            guild.addRoleToMember(botMember, botRole).queue();
        } catch (InvalidTokenException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static JDA getApi() {
        return api;
    }

    public Guild getGuild() {
        return getApi().getGuildById(config.getAstonGuildId());
    }

    private void fetchAndSaveGuildMembers() {
        Guild guild = getGuild();
        if (guild == null) {
            System.out.println("Guild not found");
            return;
        }

        List<Member> members = guild.getMembers();
        for (Member member : members) {
            AstonDevMember astonDevMember = new AstonDevMember();
            astonDevMember.setId(member.getIdLong());
            astonDevMember.setName(member.getEffectiveName());
            List<String> roles = member.getRoles().stream()
                    .map(Role::getName)
                    .toList();
            astonDevMember.setRoles(roles);
            memberRepository.save(astonDevMember);
        }
    }

    //region    SCHEDULED TASKS
    @Scheduled(cron = "0 45 9 ? * MON-FRI")
    private void sendStandupReminder() {
        Role role = getRoleByName("mentee");
        String message = role.getAsMention() + " Standup in 15 minutes.";
        TextChannel channel = getTextChannelById(config.getCurrentMenteesChannelId());
        channel.sendMessage(message).queue();
    }

    @Scheduled(cron = "0 0 13 ? * MON-FRI")
    private void sendAlgoReminder() {
        List<AstonDevMember> mentees = getMembersWithoutSubmission();
        if (mentees.isEmpty()) {
            System.out.println("No mentee found without submission");
            return;
        }
        String message = "Make sure to complete your daily algo!";
        for (AstonDevMember mentee : mentees) {
            api.openPrivateChannelById(mentee.getId()).queue(privateChannel -> {
                privateChannel.sendMessage(message).queue();
            });
        }
    }

    @Scheduled(cron = "0 30 9 ? * MON")
    private void sendTimecardReminder() {
        TextChannel channel = getTextChannelById(config.getCurrentMenteesChannelId());
        Role menteeRole = getRoleByName("mentees");
        String message = menteeRole.getAsMention() + " Remember to submit your hours for last week!";
        channel.sendMessage(message).queue();
    }

    @Scheduled(cron = "0 0 0 * * MON")
    private void sendWeeklyAlgoReport() {
        String message = "\\n\\n============================\\n\\n";
        message += "***WEEKLY MENTEE ALGO REPORT***\n\n";
        message += "============================\n\n";
        LocalDate today = LocalDate.now();
        LocalDate startOfPreviousWeek = today.minusWeeks(1);
        List<AstonDevMember> mentees = memberRepository.findByRolesContainingIgnoreCase("mentee");
        for (AstonDevMember mentee : mentees) {
            List<Algo> algos = algoRepository.findByAstonDevMemberAndDateSubmittedBetween(mentee, startOfPreviousWeek, today);
            message += "- - - - - - - - - - - - - - - - - - - \\n\\n";
            message += "**"+mentee.getName()+"** completed **"+algos.size()+"** algos this week.\n\n";
            for (Algo algo : algos) {
                message += "**Date:**"+algo.getDateSubmitted()+"\n";
                message += "**Title:**"+algo.getTitle()+"\n";
                message += "**Link:**"+algo.getLink()+"\n\n";
            }
            message += "- - - - - - - - - - - - - - - - - - - \\n\\n";
        }
        TextChannel channel = getTextChannelById(config.getWeeklyAlgoReportChannelId());
        channel.sendMessage(message).queue();
    }
    //endregion

    //region    HELPER METHODS
    private List<AstonDevMember> getMembersWithoutSubmission() {
        LocalDate today = LocalDate.now();
        List<Algo> algos = algoRepository.findAllByDateSubmitted(today);
        List<AstonDevMember> submitted = new ArrayList<>();
        for (Algo algo : algos) {
            submitted.add(algo.getAstonDevMember());
        }
        List<AstonDevMember> unsubmitted = (List<AstonDevMember>) memberRepository.findAll();
        unsubmitted.removeAll(submitted);
        return unsubmitted;
    }

    private Role getRoleByName(String roleName) {
        Guild guild = getGuild();
        List<Role> roles = guild.getRolesByName(roleName, true);
        if (roles.isEmpty()) {
            System.out.println("No role found with the name '"+roleName+"'.");
            throw new RuntimeException("No role found with the name '"+roleName+"'.");
        }
        return roles.get(0);
    }

    private TextChannel getTextChannelById(Long id) {
        Guild guild = getGuild();
        TextChannel channel = guild.getTextChannelById(id);
        if (channel == null) {
            System.out.println("No channel found with ID: "+id);
            throw new RuntimeException("No channel found with ID: "+id);
        }
        return channel;
    }
    //endregion
}
