package com.grocerystore.api.fashion;

import java.util.Set;

/**
 * A rough body shape read from bust, waist, and hip, used only to suggest fits. It is a styling guide, not a
 * measurement: the garment is always cut to the numbers themselves.
 */
public enum BodyShape {
    ROUND("Round", "Your waist is close to your bust and hips. Straight and A-line shapes fall cleanly from the shoulder, and a V neck lengthens the line.",
            Set.of("STRAIGHT", "A_LINE", "RELAXED")),
    PEAR("Pear", "Your hips are wider than your bust. A-line and flared shapes balance the hips, and a boat or square neck widens the shoulder line.",
            Set.of("A_LINE", "FLARED", "FIT_AND_FLARE")),
    INVERTED_TRIANGLE("Inverted triangle", "Your bust or shoulders are wider than your hips. A V neck softens the top, and A-line or flared hems add balance below.",
            Set.of("A_LINE", "FLARED", "PEPLUM")),
    HOURGLASS("Hourglass", "Your bust and hips are close and your waist is clearly smaller. Shapes that follow the waist suit you.",
            Set.of("FITTED", "FIT_AND_FLARE", "STRAIGHT")),
    RECTANGLE("Rectangle", "Your bust, waist, and hips are fairly even. Peplum, tiered, and fit and flare shapes add curve.",
            Set.of("PEPLUM", "TIERED", "FIT_AND_FLARE", "FLARED"));

    // Differences in centimetres. About 9 cm is the usual 3.5 inch rule of thumb.
    private static final int SIDE_DIFFERENCE_CM = 9;
    private static final int DEFINED_WAIST_CM = 23;

    public final String label;
    public final String advice;
    /** Fit option codes that tend to suit this shape. Codes not offered for a garment are simply ignored. */
    public final Set<String> suitedFits;

    BodyShape(String label, String advice, Set<String> suitedFits) {
        this.label = label;
        this.advice = advice;
        this.suitedFits = suitedFits;
    }

    public static BodyShape of(int bustCm, int waistCm, int hipCm) {
        if (waistCm * 100 >= hipCm * 85 && waistCm >= bustCm - 5) {
            return ROUND;
        }
        if (hipCm - bustCm >= SIDE_DIFFERENCE_CM) {
            return PEAR;
        }
        if (bustCm - hipCm >= SIDE_DIFFERENCE_CM) {
            return INVERTED_TRIANGLE;
        }
        if (bustCm - waistCm >= DEFINED_WAIST_CM || hipCm - waistCm >= DEFINED_WAIST_CM) {
            return HOURGLASS;
        }
        return RECTANGLE;
    }
}
