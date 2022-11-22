package jupiterpi.vocabulum.discordbot.state.sessions;

import jupiterpi.vocabulum.core.sessions.Session;
import jupiterpi.vocabulum.core.sessions.selection.PortionBasedVocabularySelection;
import jupiterpi.vocabulum.core.sessions.selection.VocabularySelection;
import jupiterpi.vocabulum.core.vocabularies.Vocabulary;
import jupiterpi.vocabulum.core.vocabularies.translations.TranslationSequence;
import jupiterpi.vocabulum.core.vocabularies.translations.VocabularyTranslation;
import jupiterpi.vocabulum.core.vocabularies.translations.parts.container.InputMatchedPart;
import jupiterpi.vocabulum.discordbot.CoreService;
import jupiterpi.vocabulum.discordbot.state.State;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

import java.util.ArrayList;
import java.util.List;

public class SessionState implements State {
    private VocabularySelection selection;
    private Mode mode = null;
    private Direction direction = null;

    private Session session;

    /* constructor */

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

    /* configuration */

    private static final String BTN_MODE_CHAT = "mode_chat";
    private static final String BTN_MODE_CARDS = "mode_cards";

    private static final String BTN_DIRECTION_LG = "direction_lg";
    private static final String BTN_DIRECTION_GL = "direction_gl";
    private static final String BTN_DIRECTION_RAND = "direction_rand";

    @Override
    public void start(SlashCommandInteractionEvent event) {
        event.reply("Hi, willkommen zu deiner Abfrage!").queue();

        event.getChannel().sendMessage("Wähle Abfragemodus und -richtung aus:")
                .setComponents(makeConfigComponents())
                .queue();

        try {
            if (readyToStart()) startSession(event.getChannel().asPrivateChannel());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleConfigButtons(ButtonInteractionEvent event) {
        String btn = event.getButton().getId();
        boolean modeButton = btn.startsWith("mode");
        boolean directionButton = btn.startsWith("direction");

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

        try {
            if (readyToStart()) startSession(event.getChannel().asPrivateChannel());
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        Button directionButton_lg = Button.secondary(BTN_DIRECTION_LG, "Lat. -> DE");
        Button directionButton_rand = Button.secondary(BTN_DIRECTION_RAND, "zufällig");
        Button directionButton_gl = Button.secondary(BTN_DIRECTION_GL, "DE -> Lat.");
        if (direction == null) {
            components.add(ActionRow.of(
                    directionButton_lg, directionButton_rand, directionButton_gl
            ));
        } else {
            components.add(ActionRow.of(
                    directionButton_lg
                            .withStyle(direction == Direction.LG ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY)
                            .asDisabled(),
                    directionButton_rand
                            .withStyle(direction == Direction.RAND ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY)
                            .asDisabled(),
                    directionButton_gl
                            .withStyle(direction == Direction.GL ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY)
                            .asDisabled()
            ));
        }

        return components;
    }

    /* running */

    private PrivateChannel channel;

    private boolean readyToStart() {
        return mode != null && direction != null;
    }
    private void startSession(PrivateChannel channel) {
        this.channel = channel;

        try {
            session = new Session(selection);
            session.start();
        } catch (Session.SessionLifecycleException e) {
            e.printStackTrace();
        }

        channel.sendMessage(switch (direction) {
            case LG -> "Ich sage dir immer das lateinische Wort und du schreibst die deutschen Bedeutungen zurück.";
            case GL -> "Ich sage dir immer die deutsche Bedeutung, und du schreibst mir die lateinische Vokabel zurück.";
            case RAND -> "Ich sage dir manchmal die deutsche Bedeutung und manchmal die latinische Vokabel, und du schreibst mir das jeweils andere zurück.";
        }).queue();
        channel.sendMessage("Alles klar? Los geht's!\nDie erste Vokabel:").queue();

        setNextVocabulary();
        askNextVocabulary();
    }

    private Vocabulary currentVocabulary;
    private Direction.ResolvedDirection currentDirection;

    private void setNextVocabulary() {
        try {
            currentVocabulary = session.getNextVocabulary();
            currentDirection = direction.resolveRandom();
        } catch (Session.SessionLifecycleException e) {
            e.printStackTrace();
        }
    }

    private void askNextVocabulary() {
        String question = switch (currentDirection) {
            case LG -> makeLatinPart();
            case GL -> makeGermanPart();
        };
        MessageCreateAction msg = channel.sendMessage(question);
        if (mode == Mode.CARDS) msg.addActionRow(Button.primary(BTN_TURN, "Aufdecken"));
        msg.queue();
    }
    private String makeLatinPart() {
        return "**" + currentVocabulary.getBaseForm() + "**";
    }
    private String makeGermanPart() {
        List<String> translations = new ArrayList<>();
        for (VocabularyTranslation translation : currentVocabulary.getTranslations()) {
            translations.add(translation.isImportant()
                    ? "**" + translation.getTranslation() + "**"
                    : translation.getTranslation());
        }
        return String.join(", ", translations);
    }

    private static final String BTN_TURN = "turn";

    private void handleTurnButton(ButtonInteractionEvent event) {
        event.editMessage(makeLatinPart() + " — " + makeGermanPart()).queue();
        event.getMessage().editMessageComponents(makeSentimentComponents(null)).queue();
    }

    private static final String BTN_SENTIMENT_GOOD = "sentiment_good";
    private static final String BTN_SENTIMENT_PASSABLE = "sentiment_passable";
    private static final String BTN_SENTIMENT_BAD = "sentiment_bad";

    private void handleSentimentButtons(ButtonInteractionEvent event) {
        Sentiment sentiment = switch (event.getButton().getId()) {
            case BTN_SENTIMENT_GOOD -> Sentiment.GOOD;
            case BTN_SENTIMENT_PASSABLE -> Sentiment.PASSABLE;
            case BTN_SENTIMENT_BAD -> Sentiment.BAD;
            default -> null;
        };
        boolean passed = sentiment != Sentiment.BAD;

        event.editComponents(makeSentimentComponents(sentiment)).queue();

        handleFeedback(passed, event.getChannel());
    }

    private enum Sentiment {
        GOOD, PASSABLE, BAD
    }

    private LayoutComponent makeSentimentComponents(Sentiment sentiment) {
        Button sentimentButton_good = Button.secondary(BTN_SENTIMENT_GOOD, Emoji.fromUnicode("U+1F7E9"));
        Button sentimentButton_passable = Button.secondary(BTN_SENTIMENT_PASSABLE, Emoji.fromUnicode("U+1F7E8"));
        Button sentimentButton_bad = Button.secondary(BTN_SENTIMENT_BAD, Emoji.fromUnicode("U+1F7E5"));
        if (sentiment == null) {
            return ActionRow.of(
                    sentimentButton_bad, sentimentButton_passable, sentimentButton_good
            );
        } else {
            return ActionRow.of(
                    sentimentButton_bad
                            .withStyle(sentiment == Sentiment.BAD ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY)
                            .asDisabled(),
                    sentimentButton_passable
                            .withStyle(sentiment == Sentiment.PASSABLE ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY)
                            .asDisabled(),
                    sentimentButton_good
                            .withStyle(sentiment == Sentiment.GOOD ? ButtonStyle.PRIMARY : ButtonStyle.SECONDARY)
                            .asDisabled()
            );
        }
    }

    private void handleChatInput(MessageReceivedEvent event) {
        String input = event.getMessage().getContentDisplay();

        boolean passed;
        float score;
        String modeSpecificMessage = "";
        if (currentDirection == Direction.ResolvedDirection.LG) {

            List<TranslationSequence.ValidatedTranslation> translations = currentVocabulary.getTranslations().validateInput(input);
            int amountRight = 0;
            for (TranslationSequence.ValidatedTranslation translation : translations) {
                if (translation.isValid()) {
                    amountRight++;
                }
            }
            score = ((float) amountRight) / ((float) translations.size());
            passed = score >= 0.5f;

            List<String> translationsStr = new ArrayList<>();
            for (TranslationSequence.ValidatedTranslation translation : translations) {
                if (translation.isValid()) {
                    String str = "✅ ";
                    List<InputMatchedPart> inputMatchedParts = translation.getVocabularyTranslation().matchValidInput(translation.getInput());
                    for (InputMatchedPart part : inputMatchedParts) {
                        if (part.isDecorative()) {
                            str += part.getDecorativeString();
                        } else {
                            String partStr = part.getTranslationPart().getBasicString();
                            if (part.isMatched()) {
                                str += "**" + partStr + "**";
                            } else {
                                str += partStr;
                            }
                        }
                    }
                    translationsStr.add(str);
                } else {
                    translationsStr.add("❌ **" + translation.getVocabularyTranslation().getTranslation() + "**");
                }
            }
            modeSpecificMessage = String.join(" ", translationsStr);

        } else {

            String definition = currentVocabulary.getDefinition(CoreService.get().i18n);

            passed = input.trim().equalsIgnoreCase(definition);
            score = passed ? 1f : 0f;

            modeSpecificMessage = (passed ? "✅" : "❌") + " **" + definition + "**";

        }

        event.getChannel().sendMessage("Das ist **" +
                (passed
                        ? (score > 0.75f ? "richtig!" : "ungefähr richtig")
                        : "leider falsch"
                )
                + "**").queue();
        event.getChannel().sendMessage(modeSpecificMessage).queue();

        handleFeedback(passed, event.getChannel());
    }

    private void handleFeedback(boolean passed, MessageChannelUnion channel) {
        try {
            session.provideFeedback(currentVocabulary, passed);
        } catch (Session.SessionLifecycleException e) {
            e.printStackTrace();
        }

        if (session.isRoundDone()) {
            Session.Result result = session.getResult();
            String scoreStr = ((int) Math.floor(result.getScore() * 100)) + "%";
            channel.sendMessage("Du hast diese Runde Vokabeln durch, und **" + scoreStr + "** davon hast du richtig beantwortet.").queue();

            if (session.isAllDone()) {
                channel.sendMessage("Juhu! Jetzt hast du **alle Vokabeln fertig!** Herzlichen Glückwunsch.").queue();
                channel.sendMessage("Möchtest du die Abfrage beenden, oder alle Vokabeln nochmal wiederholen?")
                        .addActionRow(
                                Button.primary(BTN_REPEAT, "Wiederholen"),
                                Button.primary(BTN_EXIT, "Beenden")
                        )
                        .queue();
            } else {
                channel.sendMessage("Dann werde ich jetzt die Vokabeln, die du letzte Runde noch falsch hattest, nochmal wiederholen.").queue();
            }
        }

        if (!session.isAllDone()) {
            channel.sendMessage("Nächste Vokabel: ").queue();
            setNextVocabulary();
            askNextVocabulary();
        }
    }

    private void handleRepeatButton(ButtonInteractionEvent event) {
        disableAllButtons(event, true);

        event.getChannel().sendMessage("Alles klar, ich werde alle Vokabeln noch einmal wiederholen.").queue();
        event.getChannel().sendMessage("Die erste Vokabel:").queue();

        try {
            session.restart();
            setNextVocabulary();
            askNextVocabulary();
        } catch (Session.SessionLifecycleException e) {
            e.printStackTrace();
        }
    }

    private void handleExitButton(ButtonInteractionEvent event) {
        disableAllButtons(event, true);
        event.getChannel().sendMessage("Beendet!").queue();
    }

    private static final String BTN_REPEAT = "repeat";
    private static final String BTN_EXIT = "exit";

    /* event hooks */

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String btn = event.getButton().getId();

        if (btn.startsWith("mode") || btn.startsWith("direction")) {
            handleConfigButtons(event);
        }

        if (btn.equals(BTN_TURN)) {
            handleTurnButton(event);
        }

        if (btn.startsWith("sentiment")) {
            handleSentimentButtons(event);
        }

        if (btn.equals(BTN_REPEAT)) {
            handleRepeatButton(event);
        }

        if (btn.equals(BTN_EXIT)) {
            handleExitButton(event);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (mode == Mode.CHAT) {
            handleChatInput(event);
        }
    }

    @Override
    public void stop(MessageChannelUnion channel) {
        channel.sendMessage("*(Abfrage beendet!)*").queue();
    }

    /* util */

    private void disableAllButtons(ButtonInteractionEvent event, boolean response) {
        List<LayoutComponent> components = new ArrayList<>();
        for (LayoutComponent component : event.getMessage().getComponents()) {
            components.add(component.asDisabled());
        }
        if (response) {
            event.editComponents(components).queue();
        } else {
            event.getMessage().editMessageComponents(components).queue();
        }
    }
}