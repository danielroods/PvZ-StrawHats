package view.screens.match.after;

import com.badlogic.gdx.graphics.Color;

import model.collections.animations.AnimationFactory;
import view.screens.generals.GameScreen;

import java.util.Map;

/**
 * Sequence played once the match outcome is decided, before actually leaving the game
 * screen: the board keeps rendering as-is for {@link #MATCH_END_HOLD_DURATION}s, then the
 * board shades into dark over {@link #MATCH_END_FADE_DURATION}s, then the "YOU WON"/"YOU
 * LOST" title PAM plays (intro clip, then loop) for {@link #MATCH_END_TITLE_DURATION}s,
 * and only then does the real "end game" command fire and the screen hand off to the
 * after-match menu.
 */
public class MatchEndSequence {

    enum MatchEndPhase { NONE, HOLD, FADE, TITLE }

    private static final float MATCH_END_HOLD_DURATION = 3f;
    private static final float MATCH_END_FADE_DURATION = 1f;
    private static final float MATCH_END_TITLE_DURATION = 5f;
    private static final float MATCH_END_SHADE_ALPHA = 0.55f;
    private static final String MATCH_WON_TITLE_PAM = "768/FULL/UI/JOUST/MATCH_RESULTS/YOU_WON_TEXT/YOU_WON_TEXT.PAM";
    private static final String MATCH_LOST_TITLE_PAM = "768/FULL/UI/JOUST/MATCH_RESULTS/YOU_LOST_TEXT/YOU_LOST_TEXT.PAM";
    private static final String[] TITLE_LOCALES =
            { "eng", "fre_fr", "ger_de", "ita_it", "por_br", "spa_es" };
    private static final Map<String, Boolean> MATCH_WON_TITLE_VISIBILITY =
            titleVisibility("you_won_text_");
    private static final Map<String, Boolean> MATCH_LOST_TITLE_VISIBILITY =
            titleVisibility("you_lost_text_");
    private static final float MATCH_END_TITLE_SCALE = 0.45f;
    private static final float MATCH_END_TITLE_OFFSET_X = -466f;
    private static final float MATCH_END_TITLE_OFFSET_Y = -201f;

    private static Map<String, Boolean> titleVisibility(String prefix) {
        Map<String, Boolean> visibility = new java.util.HashMap<>();
        for (String locale : TITLE_LOCALES) {
            visibility.put(prefix + locale, "eng".equals(locale));
        }
        return visibility;
    }

    private final GameScreen screen;

    private MatchEndPhase matchEndPhase = MatchEndPhase.NONE;
    private float matchEndPhaseTimer;
    private boolean matchEndWon;
    private float matchEndTitleAnimTime;

    public MatchEndSequence(GameScreen screen) {
        this.screen = screen;
    }

    MatchEndPhase phase() {
        return matchEndPhase;
    }

    public boolean isIdle() {
        return matchEndPhase == MatchEndPhase.NONE;
    }

    public void checkMatchEnd() {
        if (screen.matchFinished || matchEndPhase != MatchEndPhase.NONE) return;
        if (screen.session.isGameOver()) {
            startMatchEndSequence(false);
        } else if (screen.session.isGameWon()) {
            startMatchEndSequence(true);
        }
    }

    /**
     * Starts the win/lose sequence instead of ending the match immediately: the board keeps
     * rendering for a few seconds, then shades dark, then shows the outcome title, and only
     * once that has all played out does {@link #finishMatchEndSequence()} actually run the
     * "end game" command and hand off to the after-match menu.
     */
    public void startMatchEndSequence(boolean won) {
        matchEndWon = won;
        matchEndPhase = MatchEndPhase.HOLD;
        matchEndPhaseTimer = 0f;
        matchEndTitleAnimTime = 0f;
    }

    public void advanceMatchEndSequence(float delta) {
        if (matchEndPhase == MatchEndPhase.NONE) return;
        matchEndPhaseTimer += delta;
        switch (matchEndPhase) {
            case HOLD:
                if (matchEndPhaseTimer >= MATCH_END_HOLD_DURATION) {
                    matchEndPhase = MatchEndPhase.FADE;
                    matchEndPhaseTimer = 0f;
                }
                break;
            case FADE:
                if (matchEndPhaseTimer >= MATCH_END_FADE_DURATION) {
                    matchEndPhase = MatchEndPhase.TITLE;
                    matchEndPhaseTimer = 0f;
                }
                break;
            case TITLE:
                matchEndTitleAnimTime += delta;
                if (matchEndPhaseTimer >= MATCH_END_TITLE_DURATION) {
                    finishMatchEndSequence();
                }
                break;
            default:
                break;
        }
    }

    private void finishMatchEndSequence() {
        matchEndPhase = MatchEndPhase.NONE;
        screen.matchFinished = true;
        screen.onMatchEndSequenceFinished(matchEndWon);
        controller.ScreenManager.syncWithCurrentMenu();
    }

    /**
     * Darkens the board a little once the outcome is decided (FADE/TITLE phases), then draws
     * the "YOU WON"/"YOU LOST" title PAM on top during TITLE - "intro" for the clip's own
     * intro length, then "loop" for the rest of the title window. Only the single named title
     * image is shown, via the same elementVisibility mask drawPam uses for zombie armor.
     */
    public void drawMatchEndOverlay() {
        if (matchEndPhase != MatchEndPhase.FADE && matchEndPhase != MatchEndPhase.TITLE) return;

        float shadeProgress = matchEndPhase == MatchEndPhase.FADE
                ? Math.min(1f, matchEndPhaseTimer / MATCH_END_FADE_DURATION)
                : 1f;
        float viewW = screen.stage.getViewport().getWorldWidth();
        float viewH = screen.stage.getViewport().getWorldHeight();
        screen.batch.setColor(0f, 0f, 0f, MATCH_END_SHADE_ALPHA * shadeProgress);
        screen.batch.draw(screen.whitePixel, 0f, 0f, viewW, viewH);
        screen.batch.setColor(Color.WHITE);

        if (matchEndPhase != MatchEndPhase.TITLE) return;

        String path = matchEndWon ? MATCH_WON_TITLE_PAM : MATCH_LOST_TITLE_PAM;
        Map<String, Boolean> visibility = matchEndWon ? MATCH_WON_TITLE_VISIBILITY : MATCH_LOST_TITLE_VISIBILITY;
        float introDuration = AnimationFactory.clipDurationForPath(path, "intro");
        boolean introDone = introDuration > 0f && matchEndTitleAnimTime >= introDuration;
        String state = introDone ? "loop" : "intro";
        float clipTime = introDone ? matchEndTitleAnimTime - introDuration : matchEndTitleAnimTime;

        screen.drawPam(path, state, clipTime, viewW * 0.5f + MATCH_END_TITLE_OFFSET_X,
                viewH * 0.5f + MATCH_END_TITLE_OFFSET_Y, MATCH_END_TITLE_SCALE, false, visibility);
    }
}
