package jupiterpi.vocabulum.discordbot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AppListener extends ListenerAdapter {
    public static void init() {
        App.jda.upsertCommand("delete", "Delete all messages")
                .queue();
        App.jda.addEventListener(new AppListener());
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