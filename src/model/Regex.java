package model;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Regex {
    
    REGISTER(
            "^\\s*register\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)\\s+(?<passwordConfirm>\\S+)\\s+-n\\s+(?<nickname>\\S+)\\s+-e\\s+(?<email>\\S+)\\s+-g\\s+(?<gender>\\S+)\\s*$"
    ),
    PICK_QUESTION(
            "^\\s*pick\\s+question\\s+-q\\s+(?<questionnumber>\\S+)\\s+-a\\s+(?<answer>\\S+)\\s+-c\\s+(?<answerconfirm>\\S+)\\s*$"
    ),
    LOGIN(
            "^\\s*login\\s+-u\\s+(?<username>\\S+)\\s+-p\\s+(?<password>\\S+)(?:\\s+-stay-logged-in)?\\s*$"
    ),
    FORGET_PASSWORD(
            "^\\s*forget\\s+password\\s+-u\\s+(?<username>\\S+)\\s+-e\\s+(?<email>\\S+)\\s*$"
    ),
    ANSWER(
            "^\\s*answer\\s+-a\\s+(?<answer>\\S+)\\s*$"
    ),
    MENU_LOGOUT(
            "^\\s*menu\\s+logout\\s*$"
    ),

    
    MENU_ENTER_CHAPTER(
            "^\\s*menu\\s+enter\\s+chapter\\s+-c\\s+(?<chaptername>.+?)\\s*$"
    ),
    MENU_SHOW_CHAPTERS(
            "^\\s*menu\\s+show\\s+chapters\\s*$"
    ),
    MENU_SHOW_STAGES(
            "^\\s*(?:menu\\s+)?show\\s+stages\\s*$"
    ),
    MENU_SELECT_STAGE(
            "^\\s*(?:menu\\s+)?select\\s+stage\\s+-s\\s+(?<stage>\\d+)\\s*$"
    ),
    MENU_GREENHOUSE(
            "^\\s*(?:menu\\s+greenhouse|greenhouse\\s+menu)\\s*$"
    ),
    MENU_TRAVEL_LOG(
            "^\\s*(?:menu\\s+travel-log|travel-log\\s+menu)\\s*$"
    ),
    MENU_LEADERBOARD(
            "^\\s*menu\\s+leaderboard\\s*$"
    ),
    LEADERBOARD_SORT(
            "^\\s*leaderboard\\s+sort\\s+-c\\s+(?<column>\\S+)\\s+-o\\s+(?<order>asc|desc)\\s*$"
    ),
    MENU_COIN_WALLET(
            "^\\s*(?:menu\\s+coin-wallet|coin-wallet\\s+menu)\\s*$"
    ),
    MENU_GEM_WALLET(
            "^\\s*(?:menu\\s+gem-wallet|gem-wallet\\s+menu)\\s*$"
    ),
    MENU_SETTINGS_CHANGE_DIFFICULTY(
            "^\\s*menu\\s+settings\\s+change-difficulty\\s+-l\\s+(?<difficultylevel>\\S+)\\s*$"
    ),

    MENU_NEWS_SHOW_UNREAD(
            "^\\s*menu\\s+news\\s+show-unread\\s*$"
    ),
    MENU_NEWS_SHOW_ALL(
            "^\\s*menu\\s+news\\s+show-all\\s*$"
    ),
    MENU_CHEAT_ADD(
            "^\\s*menu\\s+cheat\\s+add\\s+(?<n>\\S+)\\s+(?<r>\\S+)\\s*$"
    ),
    TRAVEL_LOG_PAGE(
            "^\\s*travel\\s+log\\s+page\\s+(?<pagename>\\S+)\\s*$"
    ),
    TRAVEL_LOG_COLLECT(
            "^\\s*travel\\s+log\\s+collect\\s+-q\\s+(?<questid>\\S+)\\s*$"
    ),
    TRAVEL_LOG_PLAY_MINIGAME(
            "^\\s*travel\\s+log\\s+play\\s+-m\\s+(?<minigame>[a-zA-Z0-9,-]+)\\s+-l\\s+(?<level>[1-3])\\s*$"
    ),

    
    MENU_PROFILE_CHANGE_USERNAME(
            "^\\s*menu\\s+profile\\s+change-username\\s+-u\\s+(?<username>\\S+)\\s*$"
    ),
    MENU_PROFILE_CHANGE_NICKNAME(
            "^\\s*menu\\s+profile\\s+change-nickname\\s+-u\\s+(?<nickname>\\S+)\\s*$"
    ),
    MENU_PROFILE_CHANGE_EMAIL(
            "^\\s*menu\\s+profile\\s+change-email\\s+-e\\s+(?<email>\\S+)\\s*$"
    ),
    MENU_PROFILE_CHANGE_PASSWORD(
            "^\\s*menu\\s+profile\\s+change-password\\s+-p\\s+(?<newpassword>\\S+)\\s+-o\\s+(?<oldpassword>\\S+)\\s*$"
    ),
    MENU_PROFILE_SHOW_INFO(
            "^\\s*menu\\s+profile\\s+show-info\\s*$"
    ),

    
    MENU_COLLECTION_SHOW_PLANTS(
            "^\\s*menu\\s+collection\\s+show-plants\\s*$"
    ),
    MENU_COLLECTION_SHOW_ALL_PLANTS(
            "^\\s*menu\\s+collection\\s+show-all-plants\\s*$"
    ),
    MENU_COLLECTION_SHOW_ZOMBIES(
            "^\\s*menu\\s+collection\\s+show-zombies\\s*$"
    ),
    MENU_COLLECTION_SHOW_ALL_ZOMBIES(
            "^\\s*menu\\s+collection\\s+show-all-zombies\\s*$"
    ),
    MENU_COLLECTION_SHOW_PLANT(
            "^\\s*menu\\s+collection\\s+show-plant\\s+-p\\s+(?<plantname>.+?)\\s*$"
    ),
    MENU_COLLECTION_SHOW_ZOMBIE(
            "^\\s*menu\\s+collection\\s+show-zombie\\s+-z\\s+(?<zombiename>.+?)\\s*$"
    ),
    MENU_COLLECTION_UPGRADE_PLANT(
            "^\\s*menu\\s+collection\\s+upgrade-plant\\s+-p\\s+(?<plantname>.+?)\\s*$"
    ),
    MENU_COLLECTION_PURCHASE_PLANT(
            "^\\s*menu\\s+collection\\s+purchase-plant\\s+-p\\s+(?<plantname>.+?)\\s*$"
    ),

    
    SHOW_ALL_PLANTS(
            "^\\s*show\\s+all\\s+plants\\s*$"
    ),
    SHOW_AVAILABLE_PLANTS(
            "^\\s*show\\s+available\\s+plants\\s*$"
    ),
    ADD_PLANT(
            "^\\s*add\\s+plant\\s+-t\\s+(?<type>.+?)\\s*$"
    ),
    REMOVE_PLANT(
            "^\\s*remove\\s+plant\\s+-t\\s+(?<type>.+?)\\s*$"
    ),
    BOOST_PLANT(
            "^\\s*boost\\s+plant\\s+-t\\s+(?<type>.+?)\\s*$"
    ),
    UPGRADE_PLANT(
            "^\\s*upgrade\\s+plant\\s+-t\\s+(?<type>.+?)\\s*$"
    ),
    START_GAME(
            "^\\s*start\\s+game\\s*$"
    ),

    SHOW_GREENHOUSE(
            "^\\s*show\\s+greenhouse\\s*$"
    ),
    PLANT_POT_AT(
            "^\\s*plant\\s+pot\\s+at\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    COLLECT_POT(
            "^\\s*collect\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    GROW_POT(
            "^\\s*grow\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),

    
    PLANT_AT(
            "^\\s*plant\\s+-t\\s+(?<type>.+?)\\s+at\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    REMOVE_PLANT_AT(
            "^\\s*remove\\s+plant\\s+at\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    DIG_PLANT_AT(
            "^\\s*dig\\s+plant\\s+at\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    COLLECT_ITEM(
            "^\\s*collect\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    USE_PLANT_FOOD(
            "^\\s*use\\s+food\\s+at\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    SHOW_GARDEN(
            "^\\s*show\\s+garden\\s*$"
    ),
    SHOW_TILE(
            "^\\s*show\\s+tile\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    WAIT_SECONDS(
            "^\\s*wait\\s+(?<seconds>\\d+)\\s*$"
    ),
    ENTER_SHOP(
            "^\\s*(?:menu\\s+)?enter\\s+shop\\s*$"
    ),
    SHOPPING_LIST(
            "^\\s*(?:shop\\s+list|shopping\\s+list)\\s*$"
    ),
    SHOP_DAILY(
            "^\\s*shop\\s+daily\\s*$"
    ),
    SHOP_BUY(
            "^\\s*shop\\s+buy\\s+-i\\s+(?<itemid>\\S+)\\s+-n\\s+(?<count>\\d+)(?:\\s+-t\\s+(?<planttype>.+?))?\\s*$"
    ),

    
    MENU_ENTER(
            "^\\s*menu\\s+enter\\s+(?<menuname>.+?)\\s*$"
    ),
    MENU_SHOW_CURRENT(
            "^\\s*(?:menu\\s+show\\s+current|show\\s+current\\s+menu)\\s*$"
    ),

    
    ADVANCE_TIME(
            "^\\s*advance\\s+time\\s+-t\\s+(?<ticks>\\d+)\\s+ticks?\\s*$"
    ),
    SHOW_MAP(
            "^\\s*show\\s+map\\s*$"
    ),
    SHOW_SUN_AMOUNT(
            "^\\s*show\\s+sun\\s+amount\\s*$"
    ),
    SHOW_PLANT_FOOD_AMOUNT(
            "^\\s*show\\s+plant(?:-|\\s+)food\\s+amount\\s*$"
    ),
    SHOW_PLANT_STATUS(
            "^\\s*show\\s+plants?\\s+status\\s*$"
    ),
    SHOW_TILE_STATUS(
            "^\\s*show\\s+tile\\s+status\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    ZOMBIES_INFO(
            "^\\s*zombies\\s+info\\s*$"
    ),
    COLLECT_SUN(
            "^\\s*collect\\s+sun\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    PLANT_ON_FIELD(
            "^\\s*plant\\s+plant\\s+-t\\s+(?<type>.+?)\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    PLUCK_PLANT_FIELD(
            "^\\s*pluck\\s+plant\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    FEED_PLANT_FIELD(
            "^\\s*feed\\s+plant\\s+-l\\s*\\(\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)\\s*$"
    ),
    CHEAT_ADD_SUNS(
            "^\\s*cheat\\s+add\\s+-n\\s+(?<count>\\d+)\\s+suns\\s*$"
    ),
    CHEAT_ADD_PLANT_FOOD(
            "^\\s*cheat\\s+add-plant-food\\s*$"
    ),
    CHEAT_REMOVE_COOLDOWN(
            "^\\s*cheat\\s+remove-cooldown\\s*$"
    ),
    CHEAT_SPAWN_ZOMBIE(
            "^\\s*cheat\\s+spawn-zombie\\s+-t\\s+(?<type>\\S+)\\s+-l\\s*\\(?\\s*(?<x>\\d+)\\s*,\\s*(?<y>\\d+)\\s*\\)?\\s*$"
    ),
    RELEASE_THE_NUKE(
            "^\\s*release\\s+the\\s+nuke\\s*$"
    ),
    START_ZOMBIE_WAVES(
            "^\\s*start\\s+zombie\\s+waves\\s*$"
    ),

    
    MINIGAME_ADVANCE_TIME(
            "^\\s*advance\\s+time\\s+-t\\s+(?<ticks>\\d+)\\s+ticks?\\s*$"
    ),
    BEGHOULED_SWAP(
            "^\\s*swap\\s+-l\\s*\\(\\d+,\\s*\\d+\\)\\s+-l\\s*\\(\\d+,\\s*\\d+\\)\\s*$"
    ),
    BEGHOULED_UPGRADE(
            "^\\s*upgrade\\s+-t\\s+\\S+\\s*$"
    ),
    IZOMBIE_PLACE_ZOMBIE(
            "^\\s*place\\s+zombie\\s+-t\\s+\\S+\\s+-r\\s+\\d+\\s*$"
    ),
    VASEBREAKER_BREAK_VASE(
            "^\\s*break\\s+vase\\s+-l\\s*\\(\\d+,\\s*\\d+\\)\\s*$"
    ),
    VASEBREAKER_COLLECT_SEED(
            "^\\s*collect\\s+seed\\s+-l\\s*\\(\\d+,\\s*\\d+\\)\\s*$"
    ),
    WALLNUT_PLANT_NUT(
            "^\\s*plant\\s+nut\\s+-l\\s*\\(\\d+,\\s*\\d+\\)\\s*$"
    ),

    MENU_EXIT(
            "^\\s*menu\\s+exit\\s*$"
    );


    private final Pattern compiledPattern;

    Regex(String pattern) {
        this.compiledPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    public Pattern getPattern() {
        return compiledPattern;
    }

    public Matcher getMatcherRaw(String input) {
        return compiledPattern.matcher(input.trim());
    }
}