package jupiterpi.vocabulum.discordbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class App {
    public static void main(String[] args) throws InterruptedException {
        JDA jda = JDABuilder
                .createDefault(ConfigFile.getProperty("token"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new Listener())
                .setActivity(Activity.playing("Vocabulum"))
                .build();
        jda.awaitReady();
        System.out.println("Loaded!");

        jda.updateCommands().addCommands(
                /*Commands.slash("ping", "Pings you back")
                        .addOption(OptionType.STRING, "message", "Message to read you back", false)*/
                Commands.slash("abfrage", "Startet eine Vokabelabfrage")
                        .addOption(OptionType.STRING, "vokabeln", "Auswahl der abzufragenden Vokabeln", false)
        ).queue();
    }
}