package app.dam.diary;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class DiaryDto {

    private DiaryDto() {
    }

    /** 책장에 꽂힌 책. shared 면 방의 둘이 함께 꾸미는 책이다. */
    public record Book(Long id, int year, String coverColor, boolean shared, long pageCount) {
    }

    public record Shelf(List<Book> books) {
    }

    public record Open(@NotNull @Min(2000) @Max(9999) Integer year) {
    }

    public record Recolor(@NotNull @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String coverColor) {
    }

    /**
     * 붙인 것 하나. 사진이면 그 기록의 날짜와 사진 주소가 함께 온다. 날짜를 따로 적지
     * 않아도 사진에 날짜가 따라붙게 하려는 것이다.
     */
    public record Item(
            Long id,
            DiaryItem.Kind kind,
            Long entryId,
            String photoUrl,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
            String color,
            String sticker,
            String frame,
            String text,
            double x,
            double y,
            double scale,
            double rotation,
            int layer,
            long version,
            Long createdBy) {
    }

    public record Page(Long id, int position, List<Item> items) {
    }

    public record Pages(Book book, List<Page> pages) {
    }

    public record Add(
            @NotNull DiaryItem.Kind kind,
            Long entryId,
            @Size(max = 32) String sticker,
            @Size(max = 16) String frame,
            @Size(max = 200) String text,
            @NotNull Double x,
            @NotNull Double y,
            @NotNull Double scale,
            @NotNull Double rotation) {

        Placement placement() {
            return new Placement(x, y, scale, rotation);
        }
    }

    public record Move(
            @NotNull Double x,
            @NotNull Double y,
            @NotNull Double scale,
            @NotNull Double rotation,
            @NotNull Integer layer,
            @Size(max = 200) String text,
            @NotNull Long version) {

        Placement placement() {
            return new Placement(x, y, scale, rotation);
        }
    }
}
