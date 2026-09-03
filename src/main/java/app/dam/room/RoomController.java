package app.dam.room;

import app.dam.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService rooms;

    RoomController(RoomService rooms) {
        this.rooms = rooms;
    }

    @PostMapping("/invites")
    @ResponseStatus(HttpStatus.CREATED)
    RoomDto.InviteIssued issue() {
        return rooms.issueInvite(CurrentUser.id());
    }

    @PostMapping("/join")
    RoomDto.Joined join(@Valid @RequestBody RoomDto.JoinRequest request) {
        return rooms.join(CurrentUser.id(), request.code());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void leave() {
        rooms.leave(CurrentUser.id());
    }
}
