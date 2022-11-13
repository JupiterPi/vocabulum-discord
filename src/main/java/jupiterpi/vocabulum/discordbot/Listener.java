package jupiterpi.vocabulum.discordbot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Listener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmd = event.getName();

        if (cmd.equals("abfrage")) {
            event.reply("Starte Abfrage: " + event.getOption("vokabeln").getAsString()).queue();
        }
    }
}