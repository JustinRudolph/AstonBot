package com.justinrudolph.discordbot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HelloWorldCommand implements ICommand {

    @Override
    public String getName() {
        return "hello-world";
    }

    @Override
    public String getDescription() {
        return "Returns \"Hello World!\"";
    }

    @Override
    public List<OptionData> getOptions() {
        return null;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.reply("Hello World!").setEphemeral(true).queue();
    }
}
