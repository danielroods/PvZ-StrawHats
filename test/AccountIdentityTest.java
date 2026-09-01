import model.user_data.User;
import model.user_data.UserState;
import net.dto.AccountDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountIdentityTest {

    private static User account(String username) {
        return new User(username, "Passw0rd!", username + "-nick",
                username + "@example.com", "male");
    }

    @Test
    void anAccountKeepsItsIdentityAcrossARename() {
        User user = account("luffy");
        String id = user.accountId();

        user.username = "pirate-king";

        assertEquals(id, user.accountId(),
                "renaming the profile must not turn the player into a different account");
    }

    @Test
    void accountsSavedBeforeIdsExistedGetTheSameIdOnEverySide() {
        User onTheClient = account("zoro");
        User onTheServer = account("zoro");
        onTheClient.accountId = null;
        onTheServer.accountId = null;

        assertEquals(onTheClient.accountId(), onTheServer.accountId(),
                "both sides derive one id for a pre-existing account, so it is never forked");
        assertNotEquals(onTheClient.accountId(), account("nami").accountId());
    }

    @Test
    void twoFreshAccountsAreNeverTheSameAccount() {
        assertNotEquals(account("sanji").accountId(), account("sanji").accountId());
    }

    @Test
    void savingMovesTheStateToANewerRevision() {
        UserState state = new UserState(new ArrayList<>(), 0, 0, 0);
        UserState served = new UserState(new ArrayList<>(), 0, 0, 0);

        assertFalse(state.isNewerThan(served), "an untouched copy never wins over another");

        state.markSaved();
        assertTrue(state.isNewerThan(served), "offline progress is what gets uploaded");
        assertFalse(served.isNewerThan(state), "and the copy behind it stays behind");

        served.markSaved();
        served.markSaved();
        assertTrue(served.isNewerThan(state),
                "a save built on older state never overwrites the newer copy");
    }

    @Test
    void theAccountSurvivesTheRoundTripToTheServerAndBack() {
        User user = account("robin");
        user.setSecurityQuestion("1. What is the name of your first pet?", "poneglyph");
        user.profilePicture = "assets/images/ui/avatar_robin.png";
        user.syncedPasswordHash = user.passwordHash;

        AccountDto dto = AccountDto.of(user);
        User rebuilt = account("placeholder");
        dto.applyTo(rebuilt);

        assertEquals(user.accountId(), rebuilt.accountId());
        assertEquals(user.username, rebuilt.username);
        assertEquals(user.nickname, rebuilt.nickname);
        assertEquals(user.email, rebuilt.email);
        assertEquals(user.profilePicture, rebuilt.profilePicture);
        assertEquals(user.securityQuestion, rebuilt.securityQuestion);
        assertTrue(rebuilt.checkPassword("Passw0rd!"),
                "the same credentials keep working, online or off");
        assertTrue(rebuilt.checkSecurityAnswer("poneglyph"));
    }

    @Test
    void aPartialAccountNeverBlanksOutWhatItDidNotSend() {
        User user = account("franky");
        String hash = user.passwordHash;

        AccountDto sparse = new AccountDto();
        sparse.nickname = "Cyborg";
        sparse.applyTo(user);

        assertEquals("Cyborg", user.nickname);
        assertEquals("franky", user.username, "an unsent username leaves the account alone");
        assertEquals(hash, user.passwordHash, "an unsent credential is never cleared");
    }
}
