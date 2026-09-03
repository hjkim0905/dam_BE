package app.dam.entry;

import app.dam.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/entries")
public class EntryController {

    private final EntryService entries;

    EntryController(EntryService entries) {
        this.entries = entries;
    }

    @PostMapping
    EntryDto.Kept keep(@Valid @RequestBody EntryDto.Keep request) {
        return entries.keep(CurrentUser.id(), request);
    }

    /** from 과 to 는 필수다. 전체 조회를 열어두면 기록이 쌓일수록 느려진다. */
    @GetMapping
    EntryDto.Page list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "MINE") EntryDto.View view) {
        return entries.list(CurrentUser.id(), from, to, view);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable Long id) {
        entries.remove(CurrentUser.id(), id);
    }
}
