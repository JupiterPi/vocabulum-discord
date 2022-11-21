package jupiterpi.vocabulum.discordbot.state.sessions;

import jupiterpi.vocabulum.core.sessions.Session;
import jupiterpi.vocabulum.core.sessions.selection.PortionBasedVocabularySelection;
import jupiterpi.vocabulum.core.sessions.selection.VocabularySelection;
import jupiterpi.vocabulum.discordbot.state.State;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.ArrayList;
import java.util.List;

public class SessionState implements State {
    private VocabularySelection selection;
    private Mode mode = null;
    private Direction direction = null;

    private Session session;

    public SessionState(String selection) {
        this.selection = PortionBasedVocabularySelection.fromString(selection);
    }
    public SessionState setMode(Mode mode) {
        this.mode = mode;
        return this;
    }
    public SessionState setDirection(Direction direction) {
        this.direction = direction;
        return this;
    }

    private static final String BTN_MODE_CHAT = "mode_chat";
    private static final String BTN_MODE_CARDS = "mode_cards";

    private static final String BTN_DIRECTION_LG = "direction_lg";
    private static final String BTN_DIRECTION_GL = "direction_gl";
    private static final String BTN_DIRECTION_RAND = "direction_rand";

    @Override
    public void start(SlashCommandInteractionEvent event) {
        event.reply("Herzlich willkommen zu deiner Vokabelabfrage!").queue();

        event.getChannel().sendMessage("Wähle Abfragemodus und -richtung aus:")
                .setComponents(makeConfigComponents())
                .queue();
    }
    private List<LayoutComponent> makeConfigComponents() {
        List<LayoutComponent> components = new ArrayList<>();

        Button modeButton_chat = Button.secondary(BTN_MODE_CHAT, "Chat");
        Button modeButton_cards = Button.secondary(BTN_MODE_CARDS, "Karteikarten");
        if (mode == null) {
            components.add(ActionRow.of(
                    modeButton_chat, modeButton_cards
            ));
        } else {
            components.add(ActionRow.of(
                    modeButton_chat
                            .withStyle(mode == Mode.CHAT ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY)
                            .asDisabled(),
                    modeButton_cards
                            .withStyle(mode == Mode.CARDS ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY)
                            .asDisabled()
            ));
        }

        Button directionButton_gl = Button.secondary(BTN_DIRECTION_GL, "Lat. -> DE");
        Button directionButton_rand = Button.secondary(BTN_DIRECTION_RAND, "zufällig");
        Button directionButton_lg = Button.secondary(BTN_DIRECTION_LG, "DE -> Lat.");
        if (direction == null) {
            components.add(ActionRow.of(
                    directionButton_gl, directionButton_rand, directionButton_lg
            ));
        } else {
            components.add(ActionRow.of(
                    directionButton_gl
                            .withStyle(direction == Direction.GL ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY)
                            .asDisabled(),
                    directionButton_rand
                            .withStyle(direction == Direction.RAND ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY)
                            .asDisabled(),
                    directionButton_lg
                            .withStyle(direction == Direction.LG ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY)
                            .asDisabled()
            ));
        }

        return components;
    }

    private void startSession() {
        // ...
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String btn = event.getButton().getId();

        boolean modeButton = btn.startsWith("mode");
        boolean directionButton = btn.startsWith("direction");
        if (modeButton || directionButton) {
            if (modeButton && mode == null) {
                mode = switch (btn) {
                    case BTN_MODE_CARDS -> Mode.CARDS;
                    case BTN_MODE_CHAT -> Mode.CHAT;
                    default -> null;
                };
            }
            if (directionButton && direction == null) {
                direction = switch (btn) {
                    case BTN_DIRECTION_GL -> Direction.GL;
                    case BTN_DIRECTION_LG -> Direction.LG;
                    case BTN_DIRECTION_RAND -> Direction.RAND;
                    default -> null;
                };
            }
            event.editComponents(makeConfigComponents()).queue();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {

    }

    @Override
    public boolean stop() {
        return true;
    }
}