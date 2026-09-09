# TA Coin Offer (project demo)

This is a fictional in-game TA offer. It does not process real payments.

## Run

Start the existing server normally. `ServerLauncher` now starts the web portal on port `8080` as well as the game server on `7777`.

Open:

    http:

The first server start creates `server-data/TAData.json` and inserts this demo TA code:

    TA-DEMO-2026

## Flow

1. A player opens the TA Coin Offer from Main Menu or Shop -> Coins & Gems.
2. The game opens the TA web portal.
3. A TA enters the TA code.
4. The TA enters a group ID, a game account username, and the score added to the group.
5. Every completed 0.25 score unit grants 10,000 in-game coins.
6. The reward is written to the server account data immediately.
7. If the player is online, the coin balance is pushed to the game immediately.
8. The player's existing account email is recorded in `TAData.json` with the transaction.
9. Email delivery is optional and disabled by default.

## Email

Copy `server-data-ta.properties` to `server-data/ta.properties`, then set SMTP values and `smtp.enabled=true`.

Do not commit SMTP passwords. For Gmail, use an app password rather than the normal account password.

## Web port

The server launcher accepts:

    --web-port 8080

Example:

    ... ServerLauncher --port 7777 --web-port 8080 --data server-data
