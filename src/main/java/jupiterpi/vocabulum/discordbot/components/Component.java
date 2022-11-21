package jupiterpi.vocabulum.discordbot.components;

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.ArrayList;
import java.util.List;

public class Component {
    private List<SlashCommandData> slashCommands = new ArrayList<>();
    private List<Object> eventListeners = new ArrayList<>();

    /* builders */

    public Component slashCommands(SlashCommandData... slashCommands) {
        this.slashCommands = List.of(slashCommands);
        return this;
    }

    public Component eventListeners(Object... eventListeners) {
        this.eventListeners = List.of(eventListeners);
        return this;
    }

    /* getters */

    public List<SlashCommandData> getSlashCommands() {
        return slashCommands;
    }

    public List<Object> getEventListeners() {
        return eventListeners;
    }
}