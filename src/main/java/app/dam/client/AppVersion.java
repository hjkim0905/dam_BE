package app.dam.client;

/**
 * 앱 버전을 견준다. 문자열로 비교하면 "1.10.0" 이 "1.9.0" 보다 작다고 나온다.
 * 자리마다 숫자로 봐야 한다.
 */
public final class AppVersion {

    private AppVersion() {
    }

    public static boolean isOlderThan(String version, String than) {
        int[] mine = parts(version);
        int[] need = parts(than);

        for (int i = 0; i < Math.max(mine.length, need.length); i++) {
            int a = i < mine.length ? mine[i] : 0;
            int b = i < need.length ? need[i] : 0;
            if (a != b) return a < b;
        }
        return false;
    }

    // 숫자가 아닌 자리는 0 으로 본다. 버전을 못 읽었다고 사람을 막아 세우면
    // 앱을 고칠 방법 없이 잠긴다.
    private static int[] parts(String version) {
        if (version == null || version.isBlank()) return new int[] {0};
        String[] pieces = version.split("\\.");
        int[] numbers = new int[pieces.length];
        for (int i = 0; i < pieces.length; i++) {
            try {
                numbers[i] = Integer.parseInt(pieces[i].trim());
            } catch (NumberFormatException e) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }
}
