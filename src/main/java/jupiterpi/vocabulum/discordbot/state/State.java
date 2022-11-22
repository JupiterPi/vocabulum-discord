package jupiterpi.vocabulum.discordbot.state;

import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface State {
    void start(SlashCommandInteractionEvent event);
    void onMessageReceived(MessageReceivedEvent event);
    void onButtonInteraction(ButtonInteractionEvent event);
    void stop(MessageChannelUnion channel);
}