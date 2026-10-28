package app.dam.diary;

import app.dam.error.ApiException;
import app.dam.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 페이지에 붙인 것 하나. 둘이 같은 책을 꾸미므로 하나하나에 버전을 두고, 옮길 때
 * 내가 본 버전과 지금 버전이 다르면 덮어쓰지 않는다.
 */
@Entity
@Table(name = "diary_items")
public class DiaryItem {

    public enum Kind { PHOTO, STICKER, TEXT }

    /* 스티커와 틀의 이름은 프론트가 그리는 목록의 키다. 서버는 모양만 본다. 경로나 태그를
       흘려 넣을 수 없게 소문자와 하이픈만 받는다. */
    private static final Pattern NAME = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final int MAX_NAME = 32;
    private static final int MAX_TEXT = 80;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_id", nullable = false)
    private Long pageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Kind kind;

    @Column(name = "entry_id")
    private Long entryId;

    @Column(length = 32)
    private String sticker;

    @Column(length = 16)
    private String frame;

    @Column(length = 80)
    private String text;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(nullable = false)
    private double scale;

    @Column(nullable = false)
    private double rotation;

    @Column(nullable = false)
    private int layer;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected DiaryItem() {
    }

    private DiaryItem(Long pageId, Kind kind, Placement placement, int layer, Long createdBy) {
        Placement placed = placement.normalized();
        this.pageId = pageId;
        this.kind = kind;
        this.x = placed.x();
        this.y = placed.y();
        this.scale = placed.scale();
        this.rotation = placed.rotation();
        this.layer = layer;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static DiaryItem photo(Long pageId, Long entryId, String frame, Placement placement, int layer, Long by) {
        DiaryItem item = new DiaryItem(pageId, Kind.PHOTO, placement, layer, by);
        item.entryId = entryId;
        item.frame = name(frame);
        return item;
    }

    public static DiaryItem sticker(Long pageId, String sticker, Placement placement, int layer, Long by) {
        DiaryItem item = new DiaryItem(pageId, Kind.STICKER, placement, layer, by);
        item.sticker = name(sticker);
        return item;
    }

    public static DiaryItem text(Long pageId, String text, Placement placement, int layer, Long by) {
        DiaryItem item = new DiaryItem(pageId, Kind.TEXT, placement, layer, by);
        item.text = written(text);
        return item;
    }

    /** seen 은 고치기 전에 손에 들고 있던 버전이다. 그사이 바뀌었으면 부딪힌 것이다. */
    public void place(Placement placement, int layer, long seen) {
        if (seen != version) throw ApiException.of(ErrorCode.DIARY_ITEM_CONFLICT);
        Placement placed = placement.normalized();
        this.x = placed.x();
        this.y = placed.y();
        this.scale = placed.scale();
        this.rotation = placed.rotation();
        this.layer = layer;
        this.updatedAt = LocalDateTime.now();
    }

    public void rewrite(String text, long seen) {
        if (seen != version) throw ApiException.of(ErrorCode.DIARY_ITEM_CONFLICT);
        if (kind != Kind.TEXT) throw ApiException.of(ErrorCode.DIARY_ITEM_INVALID);
        this.text = written(text);
        this.updatedAt = LocalDateTime.now();
    }

    private static String name(String value) {
        if (value == null || value.length() > MAX_NAME || !NAME.matcher(value).matches()) {
            throw ApiException.of(ErrorCode.DIARY_ITEM_INVALID);
        }
        return value;
    }

    private static String written(String value) {
        String trimmed = value == null ? "" : value.strip();
        if (trimmed.isEmpty() || trimmed.codePointCount(0, trimmed.length()) > MAX_TEXT) {
            throw ApiException.of(ErrorCode.DIARY_ITEM_INVALID);
        }
        return trimmed;
    }

    public Long getId() {
        return id;
    }

    public Long getPageId() {
        return pageId;
    }

    public Kind getKind() {
        return kind;
    }

    public Long getEntryId() {
        return entryId;
    }

    public String getSticker() {
        return sticker;
    }

    public String getFrame() {
        return frame;
    }

    public String getText() {
        return text;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getScale() {
        return scale;
    }

    public double getRotation() {
        return rotation;
    }

    public int getLayer() {
        return layer;
    }

    public long getVersion() {
        return version;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
