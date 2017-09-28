package com.bolyndevelopment.owner.runlogger2;

import android.graphics.Color;

/**
 * Created by Bobby Jones on 9/13/2017.
 */

public class ColorUtils {

    static final int COLOR_RED = Color.parseColor("#FF5252");
    static final int COLOR_VIOLET = Color.parseColor("#E040FB");
    static final int COLOR_BLUE = Color.parseColor("#44BAFF");
    static final int COLOR_GREEN = Color.parseColor("#00E676");
    static final int COLOR_ORANGE = Color.parseColor("#FFAB40");
    static final int COLOR_LITE_GREEN = Color.parseColor("#B2FF59");
    static final int COLOR_YELLOW = Color.parseColor("#FFEB3B");
    static final int COLOR_STONE = Color.parseColor("#90A4AE");

    public static final int[] COLORS = new int[]{COLOR_BLUE, COLOR_VIOLET, COLOR_GREEN, COLOR_ORANGE, COLOR_RED, COLOR_YELLOW, COLOR_STONE};
    private static final float DARKEN_SATURATION = 1.1f;
    private static final float DARKEN_INTENSITY = 0.9f;
    private static int COLOR_INDEX = 0;


    public static final int pickColor() {
        return COLORS[(int) Math.round(Math.random() * (COLORS.length - 1))];
    }

    public static final int nextColor() {
        if (COLOR_INDEX >= COLORS.length) {
            COLOR_INDEX = 0;
        }
        return COLORS[COLOR_INDEX++];
    }
    public static int darkenColor(int color) {
        float[] hsv = new float[3];
        int alpha = Color.alpha(color);
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(hsv[1] * DARKEN_SATURATION, 1.0f);
        hsv[2] = hsv[2] * DARKEN_INTENSITY;
        int tempColor = Color.HSVToColor(hsv);
        return Color.argb(alpha, Color.red(tempColor), Color.green(tempColor), Color.blue(tempColor));
    }

    static int getCardioColor(String exercise) {
        switch (exercise) {
            case "Biking":
                return R.drawable.bike_cardio;
            case "Elliptical":
                return R.drawable.elliptical_cardio;
            case "Exercise Bike":
                return R.drawable.exercise_bike_cardio;
            case "Hiking":
                return R.drawable.hike_cardio;
            case "Jogging":
                return R.drawable.jog_cardio;
            case "Jump Rope":
                return R.drawable.jump_rope_cardio;
            case "Rowing":
                return R.drawable.row_cardio;
            case "Rowing Machine":
                return R.drawable.row_machine;
            case "Running":
                return R.drawable.run_cardio;
            case "Stair Master":
                return R.drawable.swim_cardio;
            case "Swimming":
                return R.drawable.swim_cardio;
            case "Treadmill":
                return R.drawable.treadmill_cardio;
            case "Walking":
                return R.drawable.walk_cardio;
            default:
                return R.drawable.walk_cardio;
        }
    }

}
