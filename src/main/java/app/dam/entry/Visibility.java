package app.dam.entry;

import java.util.List;

/**
 * 방에 남아 있는 사람 중 누구의 기록을 보여줄지 고른다. 방으로만 거르면 나간
 * 사람의 기록이 남은 사람 화면에 계속 보인다.
 */
public final class Visibility {

    private Visibility() {
    }

    public static List<Long> whose(List<Long> currentMembers, Long me, EntryDto.View view) {
        return view == EntryDto.View.THEIRS
                ? currentMembers.stream().filter(id -> !id.equals(me)).toList()
                : currentMembers;
    }
}
