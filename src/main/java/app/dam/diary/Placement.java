package app.dam.diary;

import app.dam.error.ApiException;
import app.dam.error.ErrorCode;

/**
 * 페이지 위에 붙인 자리. x, y 는 페이지 폭과 높이를 1 로 둔 비율이라 화면 크기와 무관하다.
 * 손가락이 페이지 밖까지 끌고 갈 수 있으므로 가장자리에 걸칠 만큼은 받아 준다.
 */
public record Placement(double x, double y, double scale, double rotation) {

    public static final double MIN_POSITION = -0.25;
    public static final double MAX_POSITION = 1.25;
    public static final double MIN_SCALE = 0.15;
    public static final double MAX_SCALE = 4;

    public Placement normalized() {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(scale) || !Double.isFinite(rotation)) {
            throw ApiException.of(ErrorCode.DIARY_ITEM_INVALID);
        }
        return new Placement(clamp(x, MIN_POSITION, MAX_POSITION), clamp(y, MIN_POSITION, MAX_POSITION),
                clamp(scale, MIN_SCALE, MAX_SCALE), wrap(rotation));
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    /** 몇 바퀴를 돌렸든 (-π, π] 안의 같은 각도로 접는다. 두 손가락으로 계속 돌리면 끝없이 커진다. */
    private static double wrap(double angle) {
        double turned = angle % (2 * Math.PI);
        if (turned > Math.PI) return turned - 2 * Math.PI;
        if (turned <= -Math.PI) return turned + 2 * Math.PI;
        return turned;
    }
}
