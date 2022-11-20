package jupiterpi.vocabulum.discordbot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class StartMessage extends ListenerAdapter {
    public static void init() {
        App.jda.addEventListener(new StartMessage());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
        if (event.getMessage().getContentDisplay().equals("!startmsg")) {
            Guild guild = App.jda.getGuildById(ConfigFile.getProperty("guild-id"));
            if (event.getGuild().getId().equals(guild.getId())) {
                Role admin = guild.getRoleById(ConfigFile.getProperty("admin-role-id"));
                if (event.getMember().getRoles().contains(admin)) {
                    event.getMessage().delete().queue();
                    event.getChannel().sendMessage(ConfigFile.getProperty("start-message-text"))
                            .addActionRow(Button.primary("start-bot", "Starten"))
                            .queue();
                }
            }
        }
    }



    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getButton().getId().equals("start-bot")) {
            event.getUser().openPrivateChannel().queue(channel -> {
                channel.sendMessage("Herzlich Willkommen! Du kannst jetzt Vocabulum hier in Discord verwenden. Die folgenden Befehle sind verfügbar: /search, /abfrage").queue();
            });
            event.reply("Du hast eine Direktnachricht erhalten.").setEphemeral(true).queue();
        }
    }
}