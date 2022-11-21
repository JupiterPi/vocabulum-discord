package jupiterpi.vocabulum.discordbot.state;

import jupiterpi.vocabulum.discordbot.Component;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.HashMap;
import java.util.Map;

public class StateManager extends ListenerAdapter {
    private Map<String, State> states = new HashMap<>();

    public StateManager() {}

    public static Component component() {
        return new Component()
                .slashCommands(
                        Commands.slash(SEARCH_COMMAND, "Nach Vokabeln suchen")
                                .addOption(OptionType.STRING, SEARCH_COMMAND_QUERY_OPTION, "Vokabel, nach der gesucht werden soll, oder ein Teil davon", true, true),
                        Commands.slash(SESSION_COMMAND, "Eine Vokabelabfrage starten")
                                .addOption(OptionType.STRING, SESSION_COMMAND_SELECTION_OPTION, "Auswahl von Vokabeln, die abgefragt werden sollen", true, false)
                                .addOption(OptionType.STRING, SESSION_COMMAND_MODE_OPTION, "Abfragemodus (\"Chat\" oder \"Cards\")", false, false)
                                .addOption(OptionType.STRING, SESSION_COMMAND_DIRECTION_OPTION, "Abfragerichtung (\"lg\", \"gl\" oder \"rand\")", false, false)
                )
                .eventListeners(new StateManager());
    }

    private static final String SEARCH_COMMAND = "suche";
    private static final String SEARCH_COMMAND_QUERY_OPTION = "suchbegriff";

    private static final String SESSION_COMMAND = "abfrage";
    private static final String SESSION_COMMAND_SELECTION_OPTION = "vokabeln";
    private static final String SESSION_COMMAND_MODE_OPTION = "modus";
    private static final String SESSION_COMMAND_DIRECTION_OPTION = "richtung";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE) {
            User user = event.getChannel().asPrivateChannel().getUser();

            State state = states.get(user.getId());
            if (state != null) {
                boolean stoppable = state.stop();
                if (!stoppable) {
                    event.reply("Um diesen Befehl zu verwenden, musst du zuerst die aktuelle Aktivität beenden.").setEphemeral(true).queue();
                    return;
                } else {
                    state = null;
                }
            }

            state = switch (event.getName()) {
                case SEARCH_COMMAND -> new SearchState(event.getOption(SEARCH_COMMAND_QUERY_OPTION).getAsString());
                case SESSION_COMMAND -> null;
                default -> null;
            };
            if (state == null) {
                event.reply("Der Befehl konnte nicht verarbeitet werden!").setEphemeral(true).queue();
            } else {
                state.start(event);
            }
            states.put(user.getId(), state);
        }
    }

    // forward events to state

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE) {
            PrivateChannel privateChannel = event.getChannel().asPrivateChannel();
            State state = states.get(privateChannel.getUser().getId());
            if (state != null) {
                state.onMessageReceived(event);
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE) {
            PrivateChannel privateChannel = event.getChannel().asPrivateChannel();
            State state = states.get(privateChannel.getUser().getId());
            if (state != null) {
                state.onButtonInteraction(event);
            }
        }
    }

    // other handlers


    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals(SEARCH_COMMAND) && event.getFocusedOption().getName().equals(SEARCH_COMMAND_QUERY_OPTION)) {
            SearchState.handleQueryAutocomplete(event);
        }
    }
}