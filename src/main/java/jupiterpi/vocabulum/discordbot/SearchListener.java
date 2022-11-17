package jupiterpi.vocabulum.discordbot;

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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SearchListener extends ListenerAdapter {
    public static void init() {
        App.jda.upsertCommand(SEARCH_COMMAND, "Vokabelsuche")
                .addOption(OptionType.STRING, SEARCH_COMMAND_QUERY_OPTION, "Vokabel, nach der gesucht werden soll, oder ein Teil davon", true, false)
                .queue();
        App.jda.addEventListener(new SearchListener());
    }

    private static final String SEARCH_COMMAND = "suche";
    private static final String SEARCH_COMMAND_QUERY_OPTION = "suchbegriff";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals(SEARCH_COMMAND)) {
            String query = event.getOption(SEARCH_COMMAND_QUERY_OPTION).getAsString();

            List<IdentificationResult> identificationResults = Database.get().getWordbase().identifyWord(query, true);
            identificationResults.sort(Comparator
                    .comparing((IdentificationResult i) -> i.getVocabulary().getBaseForm().indexOf(query))
                    .thenComparing(i -> i.getVocabulary().getBaseForm().length())
            );

            event.reply("Ich habe **" + identificationResults.size() + "** Vokabeln für **" + query + "** gefunden:").queue();

            for (IdentificationResult result : identificationResults) {
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

                event.getChannel().sendMessage(queryDisplay).addEmbeds(embed.build()).queue();
            }
        }
    }
}