package net;

public final class Protocol {

    public static final int VERSION = 1;
    public static final int DEFAULT_PORT = 7777;
    public static final String DEFAULT_HOST = "127.0.0.1";

    public static final long PING_INTERVAL_MILLIS = 10_000L;
    public static final long SILENCE_TIMEOUT_MILLIS = 30_000L;

    public static final String HELLO = "HELLO";
    public static final String OK = "OK";
    public static final String ERR = "ERR";
    public static final String PING = "PING";
    public static final String PONG = "PONG";

    public static final String REGISTER = "REGISTER";
    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";
    public static final String FORGOT_PASSWORD_START = "FORGOT_PASSWORD_START";
    public static final String FORGOT_PASSWORD_ANSWER = "FORGOT_PASSWORD_ANSWER";
    public static final String PROFILE_UPDATE = "PROFILE_UPDATE";
    public static final String STATE_PUSH = "STATE_PUSH";
    public static final String STATE_PULL = "STATE_PULL";

    public static final String ONLINE_LIST = "ONLINE_LIST";
    public static final String CHALLENGE = "CHALLENGE";
    public static final String CHALLENGE_INCOMING = "CHALLENGE_INCOMING";
    public static final String CHALLENGE_RESPOND = "CHALLENGE_RESPOND";
    public static final String CHALLENGE_DECLINED = "CHALLENGE_DECLINED";
    public static final String CHALLENGE_EXPIRED = "CHALLENGE_EXPIRED";
    public static final String QUEUE_JOIN = "QUEUE_JOIN";
    public static final String QUEUE_LEAVE = "QUEUE_LEAVE";
    public static final String MATCH_FOUND = "MATCH_FOUND";

    public static final String MATCH_READY = "MATCH_READY";
    public static final String MATCH_INTENT = "MATCH_INTENT";
    public static final String MATCH_SNAPSHOT = "MATCH_SNAPSHOT";
    public static final String MATCH_EVENT = "MATCH_EVENT";
    public static final String MATCH_END = "MATCH_END";
    public static final String MATCH_LEAVE = "MATCH_LEAVE";

    public static final String REACTION_SEND = "REACTION_SEND";
    public static final String REACTION = "REACTION";

    public static final String LEADERBOARD = "LEADERBOARD";
    public static final String BONUS_SCORE_SUBMIT = "BONUS_SCORE_SUBMIT";

    public static final String INTENT_PLACE_ZOMBIE = "PLACE_ZOMBIE";
    public static final String INTENT_PLANT = "PLANT";
    public static final String INTENT_DIG = "DIG";
    public static final String INTENT_COLLECT_SUN = "COLLECT_SUN";
    public static final String INTENT_USE_PLANT_FOOD = "USE_PLANT_FOOD";

    public static final String EVENT_BRAIN_EATEN = "BRAIN_EATEN";
    public static final String EVENT_INTENT_REJECTED = "INTENT_REJECTED";
    public static final String EVENT_SUN_COLLECTED = "SUN_COLLECTED";

    public static final String ERR_VERSION = "VERSION";
    public static final String ERR_NOT_LOGGED_IN = "NOT_LOGGED_IN";
    public static final String ERR_USERNAME_TAKEN = "USERNAME_TAKEN";
    public static final String ERR_BAD_CREDENTIALS = "BAD_CREDENTIALS";
    public static final String ERR_ALREADY_ONLINE = "ALREADY_ONLINE";
    public static final String ERR_NO_SUCH_USER = "NO_SUCH_USER";
    public static final String ERR_USER_OFFLINE = "USER_OFFLINE";
    public static final String ERR_USER_BUSY = "USER_BUSY";
    public static final String ERR_SELF_CHALLENGE = "SELF_CHALLENGE";
    public static final String ERR_VALIDATION = "VALIDATION";
    public static final String ERR_RATE_LIMITED = "RATE_LIMITED";
    public static final String ERR_NO_SUCH_MATCH = "NO_SUCH_MATCH";
    public static final String ERR_BAD_REQUEST = "BAD_REQUEST";
    public static final String ERR_INTERNAL = "INTERNAL";

    public static final String ROLE_PLANTS = "PLANTS";
    public static final String ROLE_ZOMBIES = "ZOMBIES";

    public static final String REACTION_TEXT = "TEXT";
    public static final String REACTION_EMOJI = "EMOJI";
    public static final String REACTION_STICKER = "STICKER";

    public static final String[] REACTION_TEXTS = {
        "Nice try!",
        "Brainz incoming!",
        "Too easy.",
    };

    private Protocol() {
    }
}
