package be.quodlibet.boxable.richtext.alignment;

import be.quodlibet.boxable.richtext.TextAlignment;

import java.util.EnumMap;
import java.util.Map;

/**
 * Flyweight factory returning the singleton {@link AlignmentStrategy}
 * for a given {@link TextAlignment}.
 * <p>
 * <strong>Open/Closed Principle:</strong> to add a new alignment mode,
 * implement {@link AlignmentStrategy} and register it here.
 * </p>
 */
public final class AlignmentStrategyFactory {

    private static final Map<TextAlignment, AlignmentStrategy> STRATEGIES = new EnumMap<>(TextAlignment.class);

    static {
        STRATEGIES.put(TextAlignment.LEFT,    LeftAlignmentStrategy.INSTANCE);
        STRATEGIES.put(TextAlignment.CENTER,  CenterAlignmentStrategy.INSTANCE);
        STRATEGIES.put(TextAlignment.RIGHT,   RightAlignmentStrategy.INSTANCE);
        STRATEGIES.put(TextAlignment.JUSTIFY, JustifyAlignmentStrategy.INSTANCE);
    }

    private AlignmentStrategyFactory() {}

    /**
     * Returns the strategy for the given alignment.
     *
     * @param alignment the desired alignment (nullable — defaults to LEFT)
     * @return the corresponding strategy singleton
     */
    public static AlignmentStrategy get(TextAlignment alignment) {
        if (alignment == null) {
            alignment = TextAlignment.LEFT;
        }
        return STRATEGIES.getOrDefault(alignment, LeftAlignmentStrategy.INSTANCE);
    }
}

