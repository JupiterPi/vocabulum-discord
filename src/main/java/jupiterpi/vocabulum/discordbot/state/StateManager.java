package jupiterpi.vocabulum.discordbot.state;

import jupiterpi.vocabulum.discordbot.components.Component;
import jupiterpi.vocabulum.discordbot.state.sessions.Direction;
import jupiterpi.vocabulum.discordbot.state.sessions.Mode;
import jupiterpi.vocabulum.discordbot.state.sessions.SessionState;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
                                .addOption(OptionType.STRING, SESSION_COMMAND_SELECTION_OPTION, "Auswahl von Vokabeln, die abgefragt werden sollen (z. B. 33:1_2,34)", true, false)
                                .addOption(OptionType.STRING, SESSION_COMMAND_MODE_OPTION, "Abfragemodus: \"C\" (Chat) oder \"K\" (Karteikarten)", false, true)
                                .addOption(OptionType.STRING, SESSION_COMMAND_DIRECTION_OPTION, "Abfragerichtung: \"ld\" (Lat. -> DE), \"dl\" (DE -> Lat.) oder \"zuf\" (zufällig)", false, true)
                )
                .eventListeners(new StateManager());
    }

    private static final String SEARCH_COMMAND = "suche";
    private static final String SEARCH_COMMAND_QUERY_OPTION = "suchbegriff";

    private static final String SESSION_COMMAND = "abfrage";
    private static final String SESSION_COMMAND_SELECTION_OPTION = "vokabeln";
    private static final String SESSION_COMMAND_MODE_OPTION = "modus";
    private static final String SESSION_COMMAND_DIRECTION_OPTION = "richtung";

    private static final String SESSION_COMMAND_MODE_OPTION_CHAT = "C";
    private static final String SESSION_COMMAND_MODE_OPTION_CARDS = "K";

    private static final String SESSION_COMMAND_DIRECTION_OPTION_LG = "ld";
    private static final String SESSION_COMMAND_DIRECTION_OPTION_GL = "dl";
    private static final String SESSION_COMMAND_DIRECTION_OPTION_RAND = "zuf";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getChannel().getType() == ChannelType.PRIVATE) {
            User user = event.getChannel().asPrivateChannel().getUser();

            State state = states.get(user.getId());
            if (state != null) {
                state.stop(event.getChannel());
                /*if (!stoppable) {
                    event.reply("Um diesen Befehl zu verwenden, musst du zuerst die aktuelle Aktivität beenden.").setEphemeral(true).queue();
                    return;
                } else {
                    state = null;
                }*/
            }

            if (event.getName().equals(SEARCH_COMMAND)) {
                state = new SearchState(event.getOption(SEARCH_COMMAND_QUERY_OPTION).getAsString());
            }
            if (event.getName().equals(SESSION_COMMAND)) {
                OptionMapping directionOption = event.getOption(SESSION_COMMAND_DIRECTION_OPTION);
                Direction direction = directionOption != null ? (switch (directionOption.getAsString()) {
                    case SESSION_COMMAND_DIRECTION_OPTION_GL -> Direction.GL;
                    case SESSION_COMMAND_DIRECTION_OPTION_LG -> Direction.LG;
                    case SESSION_COMMAND_DIRECTION_OPTION_RAND -> Direction.RAND;
                    default -> null;
                }) : null;

                OptionMapping modeOption = event.getOption(SESSION_COMMAND_MODE_OPTION);
                Mode mode = modeOption != null ? (switch (modeOption.getAsString()) {
                    case SESSION_COMMAND_MODE_OPTION_CHAT -> Mode.CHAT;
                    case SESSION_COMMAND_MODE_OPTION_CARDS -> Mode.CARDS;
                    default -> null;
                }) : null;

                state = new SessionState(event.getOption(SESSION_COMMAND_SELECTION_OPTION).getAsString())
                        .setDirection(direction)
                        .setMode(mode);
            }

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
        if (event.getAuthor().isBot()) return;

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
        String cmd = event.getName();
        String option = event.getFocusedOption().getName();

        if (cmd.equals(SEARCH_COMMAND) && option.equals(SEARCH_COMMAND_QUERY_OPTION)) {
            SearchState.handleQueryAutocomplete(event);
        }

        if (cmd.equals(SESSION_COMMAND)) {
            if (option.equals(SESSION_COMMAND_MODE_OPTION)) {
                handleAutocompleteFromList(event, SESSION_COMMAND_MODE_OPTION_CHAT, SESSION_COMMAND_MODE_OPTION_CARDS);
            }
            if (option.equals(SESSION_COMMAND_DIRECTION_OPTION)) {
                handleAutocompleteFromList(event, SESSION_COMMAND_DIRECTION_OPTION_LG, SESSION_COMMAND_DIRECTION_OPTION_GL, SESSION_COMMAND_DIRECTION_OPTION_RAND);
            }
        }
    }
    private void handleAutocompleteFromList(CommandAutoCompleteInteractionEvent event, String... options) {
        String input = event.getFocusedOption().getValue();
        List<Command.Choice> choices = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(input.toLowerCase())) choices.add(new Command.Choice(option, option));
        }
        event.replyChoices(choices).queue();
    }
}