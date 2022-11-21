package jupiterpi.vocabulum.discordbot;

import jupiterpi.vocabulum.discordbot.state.StateManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.ArrayList;
import java.util.List;

public class App {
    public static JDA jda;

    public static void main(String[] args) throws InterruptedException {
        CoreService.get();

        jda = JDABuilder
                .createDefault(ConfigFile.getProperty("token"))
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .setActivity(Activity.playing("Vocabulum"))
                .build();
        jda.awaitReady();
        System.out.println("Loaded!");

        List<Component> components = List.of(
                AppListener.component(),
                StartMessage.component(),
                StateManager.component()
        );

        List<SlashCommandData> slashCommands = new ArrayList<>();
        for (Component component : components) {
            slashCommands.addAll(component.getSlashCommands());
            for (Object eventListener : component.getEventListeners()) {
                jda.addEventListener(eventListener);
            }
        }
        jda.updateCommands().addCommands(slashCommands).queue();
    }
}