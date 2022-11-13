package jupiterpi.vocabulum.discordbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

public class App {
    public static void main(String[] args) throws InterruptedException {
        JDA jda = JDABuilder
                .createDefault(ConfigFile.getProperty("token"))
                .setActivity(Activity.playing("Vocabulum"))
                .build();
        jda.awaitReady();
        System.out.println("Loaded!");
    }
}