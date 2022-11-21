package jupiterpi.vocabulum.discordbot.state.sessions;

public enum Direction {
    LG, // latin -> german
    GL, // german -> latin
    RAND;

    public ResolvedDirection resolveRandom() {
        if (this == Direction.RAND) {
            return Math.random() > 0.5 ? ResolvedDirection.LG : ResolvedDirection.GL;
        } else {
            return switch (this) {
                case LG -> ResolvedDirection.LG;
                case GL -> ResolvedDirection.GL;
                default -> null;
            };
        }
    }

    public enum ResolvedDirection {
        LG, GL
    }
}