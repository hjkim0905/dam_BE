package app.dam.diary;

import app.dam.client.ClientZone;
import app.dam.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DiaryController {

    private final DiaryService diaries;

    DiaryController(DiaryService diaries) {
        this.diaries = diaries;
    }

    @GetMapping("/diaries")
    DiaryDto.Shelf shelf() {
        return diaries.shelf(CurrentUser.id());
    }

    /** 그 해의 책을 펼친다. 없으면 만들고, 있으면 그 책을 준다. 몇 번 불러도 한 권이다. */
    @PostMapping("/diaries")
    DiaryDto.Book open(
            @Valid @RequestBody DiaryDto.Open request,
            @RequestHeader(value = "X-Timezone", required = false) String timezone) {
        return diaries.open(CurrentUser.id(), request.year(), ClientZone.of(timezone));
    }

    @PatchMapping("/diaries/{id}")
    DiaryDto.Book recolor(@PathVariable Long id, @Valid @RequestBody DiaryDto.Recolor request) {
        return diaries.recolor(CurrentUser.id(), id, request.coverColor());
    }

    @GetMapping("/diaries/{id}/pages")
    DiaryDto.Pages pages(@PathVariable Long id) {
        return diaries.pages(CurrentUser.id(), id);
    }

    @PostMapping("/diaries/{id}/pages")
    @ResponseStatus(HttpStatus.CREATED)
    DiaryDto.Page addPage(@PathVariable Long id) {
        return diaries.addPage(CurrentUser.id(), id);
    }

    @DeleteMapping("/diary-pages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removePage(@PathVariable Long id) {
        diaries.removePage(CurrentUser.id(), id);
    }

    @PostMapping("/diary-pages/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    DiaryDto.Item addItem(@PathVariable Long id, @Valid @RequestBody DiaryDto.Add request) {
        return diaries.addItem(CurrentUser.id(), id, request);
    }

    @PatchMapping("/diary-items/{id}")
    DiaryDto.Item moveItem(@PathVariable Long id, @Valid @RequestBody DiaryDto.Move request) {
        return diaries.moveItem(CurrentUser.id(), id, request);
    }

    @DeleteMapping("/diary-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeItem(@PathVariable Long id) {
        diaries.removeItem(CurrentUser.id(), id);
    }
}
