package com.justinrudolph.discordbot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class DiscordConfig {

    @Value("${discord.bot.token}")
    private String botToken;

    @Value("${discord.guild.aston}")
    private long astonGuildId;

    //region    ROLE ID'S
    @Value("${discord.role.mentor}")
    private long mentorRoleId;

    @Value("${discord.role.mentee}")
    private long menteeRoleId;

    @Value("${discord.role.bots}")
    private long botRoleId;
    //endregion


    //region    CHANNEL ID'S
    @Value("${discord.guild.channel.uploadAlgo}")
    private long uploadAlgoChannelId;

    @Value("${discord.guild.channel.general}")
    private long generalChannelId;

    @Value("${discord.guild.channel.algoLog}")
    private long menteeAlgoLogChannelId;

    @Value("${discord.guild.channel.weeklyAlgoReport}")
    private long weeklyAlgoReportChannelId;

    @Value("${discord.guild.channel.currentMentees}")
    private long currentMenteesChannelId;
    //endregion
}
