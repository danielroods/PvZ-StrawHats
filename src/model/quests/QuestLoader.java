package model.quests;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.user_data.User;
import model.user_data.UserState;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public class QuestLoader {
    private static List<GameQuest> templateQuests = new ArrayList<>();
    private static boolean templatesLoaded = false;

    public static void loadTemplates(String path) {
        Gson gson = new Gson();
        try (Reader reader = openQuestResource(path)) {
            if (reader == null) {
                System.err.println("QuestLoader: could not find quest file at any known location for '" + path + "'. Quests will be unavailable.");
                templateQuests = new ArrayList<>();
                templatesLoaded = false;
                return;
            }
            Type listType = new TypeToken<List<GameQuest>>() {}.getType();
            List<GameQuest> loaded = gson.fromJson(reader, listType);
            templateQuests = (loaded != null) ? loaded : new ArrayList<>();
            templatesLoaded = true;
        } catch (IOException e) {
            System.err.println("QuestLoader: failed to read quest file '" + path + "': " + e.getMessage());
            templateQuests = new ArrayList<>();
            templatesLoaded = false;
        }
    }

    private static Reader openQuestResource(String path) throws IOException {
        List<String> candidates = new ArrayList<>();
        candidates.add(path);
        candidates.add("src/resource/Quest.json");
        candidates.add("resource/Quest.json");
        candidates.add("./resource/Quest.json");

        for (String candidate : candidates) {
            try {
                return new FileReader(candidate, StandardCharsets.UTF_8);
            } catch (FileNotFoundException ignored) {
                // try next candidate
            }
        }

        String[] classpathCandidates = { "/Quest.json", "/resource/Quest.json", "Quest.json" };
        for (String cp : classpathCandidates) {
            InputStream is = QuestLoader.class.getResourceAsStream(cp);
            if (is != null) {
                return new InputStreamReader(is, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void initializeActiveQuestsForUser() {
        if (User.currentUser == null) return;
        if (!templatesLoaded) return;

        UserState state = User.currentUser.userState;
        if (state.activeQuests == null) state.activeQuests = new ArrayList<>();

        Random random = new Random();
        long now = Instant.now().getEpochSecond();

        Map<String, GameQuest> existingById = new HashMap<>();
        for (GameQuest quest : state.activeQuests) {
            if (quest != null && quest.getId() != null) {
                existingById.put(quest.getId(), quest);
            }
        }

        List<GameQuest> refreshed = new ArrayList<>();
        for (GameQuest template : templateQuests) {
            GameQuest existing = existingById.get(template.getId());

            if (existing != null && !existing.isExpired(now)) {
                refreshed.add(existing);
                continue;
            }

            GameQuest activeCopy = cloneQuest(template);
            activeCopy.setProgress(0);
            activeCopy.setCompleted(false);
            activeCopy.setAssignedAtEpochSecond(now);

            if (activeCopy.getCriteria() != null) {
                for (QuestCriterion criterion : activeCopy.getCriteria()) {
                    Map<String, Object> params = criterion.getParams();
                    if (params == null) continue;

                    if (params.containsKey("variableOptions")) {
                        Object rawOptions = params.get("variableOptions");
                        if (rawOptions instanceof List<?> options && !options.isEmpty()) {
                            Object selectedOption = options.get(random.nextInt(options.size()));
                            Object variableParam = params.get("variableParam");

                            if (variableParam instanceof String paramName) {
                                params.put(paramName, selectedOption);
                            } else if (selectedOption instanceof Number number) {
                                criterion.setTarget(number.intValue());
                            }
                        }
                    }

                    if ("n".equals(params.get("targetRowIndex"))) {
                        params.put("targetRowIndex", random.nextInt(5));
                    }
                    if ("n".equals(params.get("targetColumnIndex"))) {
                        params.put("targetColumnIndex", random.nextInt(9));
                    }
                }
            }
            refreshed.add(activeCopy);
        }

        state.activeQuests = refreshed;
        saveActiveQuestsProgress();
    }

    private static GameQuest cloneQuest(GameQuest src) {
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(src), GameQuest.class);
    }

    public static void saveActiveQuestsProgress() {
        if (User.currentUser == null) return;
        User.save();
    }

    public static void loadActiveQuestsProgress() {
        initializeActiveQuestsForUser();
    }

    public static List<GameQuest> getAllQuests() {
        if (User.currentUser == null || User.currentUser.userState.activeQuests == null) {
            return new ArrayList<>();
        }
        return User.currentUser.userState.activeQuests;
    }

    public static List<GameQuest> getTemplateQuests() {
        return templateQuests;
    }
}
