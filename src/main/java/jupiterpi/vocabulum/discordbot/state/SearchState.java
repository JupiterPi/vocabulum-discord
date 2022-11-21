package jupiterpi.vocabulum.discordbot.state;

import jupiterpi.vocabulum.core.db.Database;
import jupiterpi.vocabulum.core.db.wordbase.IdentificationResult;
import jupiterpi.vocabulum.core.vocabularies.Vocabulary;
import jupiterpi.vocabulum.core.vocabularies.VocabularyForm;
import jupiterpi.vocabulum.core.vocabularies.conjugated.Verb;
import jupiterpi.vocabulum.core.vocabularies.conjugated.form.VerbForm;
import jupiterpi.vocabulum.core.vocabularies.declined.adjectives.Adjective;
import jupiterpi.vocabulum.core.vocabularies.declined.adjectives.AdjectiveForm;
import jupiterpi.vocabulum.core.vocabularies.declined.nouns.Noun;
import jupiterpi.vocabulum.core.vocabularies.declined.nouns.NounForm;
import jupiterpi.vocabulum.core.vocabularies.inflexible.Inflexible;
import jupiterpi.vocabulum.core.vocabularies.translations.VocabularyTranslation;
import jupiterpi.vocabulum.discordbot.CoreService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SearchState implements State {
    private String query;
    private List<IdentificationResult> results;
    private int currentPagination;
    private Message paginationMessage;

    private static final int RESULTS_PER_PAGE = 5;

    public SearchState(String query) {
        this.query = query;
    }

    private static final String BTN_MORE = "search_more";

    @Override
    public void start(SlashCommandInteractionEvent event) {
        results = Database.get().getWordbase().identifyWord(query, true);
        results.sort(Comparator
                .comparing((IdentificationResult i) -> i.makePrimaryFoundForm().indexOf(query))
                .thenComparing(i -> i.makePrimaryFoundForm().length())
                /*.thenComparing(
                        Comparator.comparing((IdentificationResult i) -> i.makePrimaryFoundForm().length()).reversed()
                )*/
        );

        int amount = results.size();
        event.reply("Ich habe **" + amount + "** Vokabeln für **" + query + "** gefunden:").queue();

        currentPagination = 0;
        printCurrentPage(event.getChannel());

        if (amount > RESULTS_PER_PAGE) {
            printPaginationFooter(event.getChannel());
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getButton().getId().equals(BTN_MORE)) {
            event.getMessage().editMessageComponents(ActionRow.of(event.getButton().asDisabled())).queue();

            int pagesAmount = (int) Math.ceil(results.size() / 5f);
            if (currentPagination < pagesAmount) {
                currentPagination++;
            } else {
                event.reply("Keine weiteren Ergebnisse.").setEphemeral(true).queue();
                return;
            }
            event.reply("Seite **" + (currentPagination+1) + "** der Ergebnisse:").queue();
            printCurrentPage(event.getChannel());
            printPaginationFooter(event.getChannel());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {}

    Button moreButton = Button.primary(BTN_MORE, "Mehr Ergebnisse zeigen");
    private void printPaginationFooter(MessageChannelUnion channel) {
        int pagesAmount = (int) Math.ceil(results.size() / (float) RESULTS_PER_PAGE);
        MessageCreateAction messageCreateAction = channel.sendMessage("Zeigt **" + Math.min((currentPagination+1)*RESULTS_PER_PAGE, results.size()) + "** von **" + results.size() + "** Ergebnissen");
        if (currentPagination < pagesAmount-1) {
            messageCreateAction.addActionRow(
                    moreButton
            );
        }
        messageCreateAction.queue(message -> paginationMessage = message);
    }

    private void printCurrentPage(MessageChannelUnion channel) {
        for (int i = currentPagination * RESULTS_PER_PAGE; i < results.size() && i < (currentPagination+1) * RESULTS_PER_PAGE; i++) {
            printIdentificationResult(results.get(i), channel);
        }
    }

    private void printIdentificationResult(IdentificationResult result, MessageChannelUnion channel) {
        Vocabulary vocabulary = result.getVocabulary();

        VocabularyForm form = result.getForms().get(0);
        String matchedForm = switch (vocabulary.getKind()) {
            case NOUN -> ((Noun) vocabulary).makeFormOrDash((NounForm) form);
            case ADJECTIVE -> ((Adjective) vocabulary).makeFormOrDash((AdjectiveForm) form);
            case VERB -> ((Verb) vocabulary).makeFormOrDash((VerbForm) form);
            case INFLEXIBLE -> null;
        };
        int matchStart = matchedForm.indexOf(query);
        int matchEnd = matchStart + query.length();
        String queryDisplay = matchedForm.substring(0, matchStart) + "**" + matchedForm.substring(matchStart, matchEnd) + "**" + matchedForm.substring(matchEnd);

        List<String> translationsStr = new ArrayList<>();
        for (VocabularyTranslation translation : vocabulary.getTranslations()) {
            translationsStr.add(translation.getTranslation());
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(vocabulary.getDefinition(CoreService.get().i18n) + " — " + String.join(", ", translationsStr));

        // meta
        embed.addField("Lektion", vocabulary.getPortion(), true);
        Vocabulary.Kind kind = vocabulary.getKind();
        if (kind == Vocabulary.Kind.NOUN) {
            Noun noun = (Noun) vocabulary;
            embed.addField("Deklinationsschema", switch (noun.getDeclensionSchema()) {
                case "a" -> "a-Deklination";
                case "o" -> "o-Deklination";
                case "cons" -> "konsonantische Deklination";
                case "e" -> "e-Deklination";
                case "u" -> "u-Deklination";
                default -> noun.getDeclensionSchema();
            }, true);
        } else if (kind == Vocabulary.Kind.ADJECTIVE) {
            Adjective adjective = (Adjective) vocabulary;
        } else if (kind == Vocabulary.Kind.VERB) {
            Verb verb = (Verb) vocabulary;
            embed.addField("Konjugationsschema", switch (verb.getConjugationSchema()) {
                case "a" -> "a-Konjugation";
                case "e" -> "e-Konjugation";
                case "ii" -> "i-Konjugation";
                case "cons" -> "konsonantische Konjugation";
                case "i" -> "kurzvokalische i-Konjugation";
                default -> verb.getConjugationSchema();
            }, true);
        } else if (kind == Vocabulary.Kind.INFLEXIBLE) {
            Inflexible inflexible = (Inflexible) vocabulary;
        }

        // color
        embed.setColor(switch (vocabulary.getKind()) {
            case NOUN -> new Color(5, 131, 242);
            case VERB -> new Color(242, 29, 29);
            case ADJECTIVE -> new Color(3, 166, 60);
            case INFLEXIBLE -> new Color(242, 159, 5);
        });

        channel.sendMessage(queryDisplay + " (" + form.formToString(CoreService.get().i18n) + " +" + (result.getForms().size()-1) + ")").addEmbeds(embed.build()).queue();
    }

    @Override
    public boolean stop() {
        return true;
    }
}