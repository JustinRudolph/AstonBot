package com.justinrudolph.discordbot.commands;

import com.justinrudolph.discordbot.config.DiscordConfig;
import com.justinrudolph.discordbot.models.Algo;
import com.justinrudolph.discordbot.models.AstonDevMember;
import com.justinrudolph.discordbot.repositories.AlgoRepository;
import com.justinrudolph.discordbot.repositories.MemberRepository;
import com.justinrudolph.discordbot.services.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PostAlgoCommand implements ICommand {

    private final AlgoRepository algoRepository;
    private final MemberRepository memberRepository;
    private final DiscordConfig config;

    public PostAlgoCommand(MemberRepository memberRepository, AlgoRepository algoRepository, DiscordConfig config) {
        this.algoRepository = algoRepository;
        this.memberRepository = memberRepository;
        this.config = config;
    }

    @Override
    public String getName() {
        return "post-algo";
    }

    @Override
    public String getDescription() {
        return "Saves daily algorithm submission";
    }

    @Override
    public List<OptionData> getOptions() {
        List<OptionData> options = new ArrayList<>();
        options.add(new OptionData(OptionType.STRING, "title", "The title of the problem", true));
        options.add(new OptionData(OptionType.STRING, "link", "The url of the problem", true));
        options.add(new OptionData(OptionType.STRING, "code", "Your solution to the problem", true));
        return options;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String title = event.getOption("title").getAsString();
        String link = event.getOption("link").getAsString();
        String code = event.getOption("code").getAsString();
        LocalDate dateSubmitted = LocalDate.now();
        Long userId = event.getMember().getIdLong();
        AstonDevMember submittedBy = memberRepository.findById(userId).orElse(null);
        if (submittedBy == null) {
            String name = event.getMember().getNickname();
            System.out.println("Member '"+name+"' not found in DB");
            return;
        }

        Algo algo = new Algo(title, link, code, dateSubmitted, submittedBy);
        algoRepository.save(algo);
        TextChannel channel = event.getGuild().getTextChannelById(config.getMenteeAlgoLogChannelId());
        displayForMentors(algo, channel);
        event.reply("Your algo has been submitted successfully").queue();
    }

    private void displayForMentors (Algo algo, TextChannel channel) {
        String message = "**Mentee: "+algo.getAstonDevMember().getName()+"**\n";
        message += "*Algo Title:* "+algo.getTitle()+"\n";
        message += "*Algo Link:* <"+algo.getLink()+">\n";
        message += "```java\n"+algo.getCode()+"```\n\n";
        channel.sendMessage(message).queue();
    }
}
