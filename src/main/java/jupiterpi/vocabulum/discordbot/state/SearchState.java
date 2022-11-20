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
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SearchState implements State {
    private String query;
    private List<IdentificationResult> results;
    private int currentPagination;

    private static final int RESULTS_PER_PAGE = 5;

    public SearchState(String query) {
        this.query = query;
    }

    private static final String BTN_PAGE_BACK = "search_page_back";
    private static final String BTN_PAGE_FORWARD = "search_page_forward";

    @Override
    public void start(SlashCommandInteractionEvent event) {
        results = Database.get().getWordbase().identifyWord(query, true);
        results.sort(Comparator
                .comparing((IdentificationResult i) -> i.getVocabulary().getBaseForm().indexOf(query))
                .thenComparing(i -> i.getVocabulary().getBaseForm().length())
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
        String btn = event.getButton().getId();
        if (btn.equals(BTN_PAGE_BACK) || btn.equals(BTN_PAGE_FORWARD)) {
            if (btn.equals(BTN_PAGE_BACK)) {
                if (currentPagination > 0) {
                    currentPagination--;
                } else {
                    event.reply("Keine vorhergehende Seite.").setEphemeral(true).queue();
                    return;
                }
            }
            if (btn.equals(BTN_PAGE_FORWARD)) {
                int pagesAmount = (int) Math.ceil(results.size() / 5f);
                if (currentPagination < pagesAmount) {
                    currentPagination++;
                } else {
                    event.reply("Keine weitere Seite.").setEphemeral(true).queue();
                    return;
                }
            }
            event.reply("Seite **" + (currentPagination+1) + "** der Ergebnisse:").queue();
            printCurrentPage(event.getChannel());
            printPaginationFooter(event.getChannel());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {}

    private void printPaginationFooter(MessageChannelUnion channel) {
        int pagesAmount = (int) Math.ceil(results.size() / 5f);

        Button backButton = Button.secondary(BTN_PAGE_BACK, Emoji.fromUnicode("U+2B05"));
        Button forwardButton = Button.secondary(BTN_PAGE_FORWARD, Emoji.fromUnicode("U+27A1"));

        channel.sendMessage("Seite **" + (currentPagination+1) + "** von **" + pagesAmount + "**")
                .addActionRow(
                        currentPagination == 0 ? backButton.asDisabled() : backButton,
                        currentPagination == pagesAmount-1 ? forwardButton.asDisabled() : forwardButton
                )
                .queue();
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