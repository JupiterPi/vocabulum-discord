package jupiterpi.vocabulum.discordbot.components;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class AppListener extends ListenerAdapter {
    public static Component component() {
        return new Component()
                .slashCommands(
                        Commands.slash("delete", "Delete all messages")
                )
                .eventListeners(new AppListener());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("delete")) {
            event.reply("Processing...").queue();
            event.getChannel().asTextChannel().createCopy().queue(success ->
                    event.getChannel().delete().queue()
            );
        }
    }
}