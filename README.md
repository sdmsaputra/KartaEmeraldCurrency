# KartaEmeraldCurrency

KartaEmeraldCurrency is a modern, high-performance economy plugin for PaperMC servers that uses the vanilla Emerald item as its core currency. It features a virtual bank system, interactive GUIs, and a powerful developer API, making it a versatile and stable choice for any server.

Built to be efficient and safe, all database operations are handled asynchronously to prevent server lag.

**Compatibility:** PaperMC 1.19.4 – 1.21.8 (Java 17+)

## Features

- **Emerald-Based Economy:** Uses physical Emeralds for deposits and withdrawals.
- **Virtual Bank:** Securely store your emeralds in a virtual bank account, safe from inventory loss.
- **Interactive GUIs:** A modern and easy-to-use GUI for all common actions (`/emerald`).
- **Vault Integration:** Can act as the primary economy provider for any Vault-compatible plugin.
- **PlaceholderAPI Support:** A full set of placeholders to display economic data on scoreboards, chat, etc.
- **Developer API:** A clean, asynchronous API for other plugins to interact with the economy.
- **Async & Performant:** All database I/O is asynchronous to ensure your server remains lag-free.
- **Configurable:** Almost every aspect, from fees to messages, can be customized.

## Installation

1.  Download the latest release from the [Releases](https://github.com/your-repo-link/releases) page.
2.  Place the `KartaEmeraldCurrency-x.x.x.jar` file into your server's `/plugins` directory.
3.  Restart your server. The default configuration files will be generated in `/plugins/KartaEmeraldCurrency/`.

## Commands & Permissions

### User Commands (`/emerald` or `/kec`)
| Command | Permission | Description |
|---|---|---|
| `/emerald` | `kec.gui` | Opens the main interactive GUI. |
| `/emerald balance` | `kec.balance` | Shows your current bank, wallet, and total balance. |
| `/emerald pay <player> <amount>` | `kec.pay` | Pays another player from your bank account. |
| `/emerald deposit <amount>` | `kec.deposit` | Deposits physical emeralds into your bank. |
| `/emerald withdraw <amount>` | `kec.withdraw` | Withdraws emeralds from your bank to your inventory. |
| `/emerald top` | `kec.top` | Shows the leaderboard of the richest players. |
| `/emerald help` | `kec.help` | Displays a help message. |

### Admin Commands (`/emeraldadmin` or `/kecadmin`)
All admin commands require the base permission `kec.admin` or granular permissions.

| Command | Permission | Description |
|---|---|---|
| `/kecadmin set <player> <amount>` | `kec.admin.set` | Sets a player's bank balance. |
| `/kecadmin add <player> <amount>` | `kec.admin.add` | Adds to a player's bank balance. |
| `/kecadmin remove <player> <amount>` | `kec.admin.remove` | Removes from a player's bank balance. |
| `/kecadmin give <player> <amount>` | `kec.admin.give` | Gives a player physical emeralds. |
| `/kecadmin take <player> <amount>` | `kec.admin.take` | Takes physical emeralds from a player. |
| `/kecadmin reload` | `kec.admin.reload` | Reloads the configuration files. |

## Placeholders

Requires [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.624/).

- `%kartaemerald_balance%` - Player's balance (source configurable: BANK or TOTAL).
- `%kartaemerald_balance_formatted%` - Player's balance formatted (e.g., 1.2k).
- `%kartaemerald_balance_comma%` - Player's balance with commas (e.g., 1,234,567).
- `%kartaemerald_bank%` - Player's bank balance.
- `%kartaemerald_wallet%` - Player's physical emerald count in inventory.
- `%kartaemerald_top_<1-10>_name%` - Name of the Nth player on the leaderboard.
- `%kartaemerald_top_<1-10>_amount%` - Balance of the Nth player on the leaderboard.

## Developer API

KartaEmeraldCurrency provides a clean, easy-to-use API for developers. All data-related methods are asynchronous and return a `CompletableFuture`.

### Accessing the API

First, get the service from Bukkit's `ServicesManager`.

```java
import com.minekarta.kec.api.KartaEmeraldService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class MyPlugin {
    private KartaEmeraldService economyService;

    public void onEnable() {
        RegisteredServiceProvider<KartaEmeraldService> rsp = Bukkit.getServicesManager().getRegistration(KartaEmeraldService.class);
        if (rsp != null) {
            this.economyService = rsp.getProvider();
        }
    }
}
```

### Example Usage: Paying a Player

```java
import com.minekarta.kec.api.TransferReason;

public void rewardPlayer(Player player, long amount) {
    if (economyService != null) {
        // Transfer from no one (server) by using a null UUID for the 'from' parameter is not supported.
        // Instead, use the addBankBalance method for server-to-player transactions.
        economyService.addBankBalance(player.getUniqueId(), amount).thenAccept(success -> {
            if (success) {
                player.sendMessage("You have been rewarded " + amount + " emeralds!");
            }
        });
    }
}
```
