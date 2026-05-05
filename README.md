# NovaEconomy

A modern economy plugin for **Paper 1.21+** servers, featuring **multi-currency support**, **SQLite / MySQL / Redis storage**, **short amount formatting**, a **public API**, **Discord Integration**, **[Vault](https://github.com/milkbowl/Vault) & [VaultUnlocked](https://modrinth.com/plugin/vaultunlocked) support**, **[PAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) compatibility** and **fully customizable messages**.

---

## Features

### Commands

| Command | Description | Aliases | Permission |
|---|---|---|---|
| `/balance [player] [currency]` | Check your or another player's balance | `/bal`, `/money` | `novaeco.cmd.balance` / `.balance.other` |
| `/baltop [currency] [page]` | Show the richest players | `/balancetop`, `/etop` | `novaeco.cmd.baltop` |
| `/pay <player> <amount> [currency]` | Pay another player | — | `novaeco.cmd.pay` |
| `/eco currency add <name>` | Create a new currency | — | `novaeco.cmd.eco.currency` |
| `/eco currency remove <name>` | Remove a dynamic currency | — | `novaeco.cmd.eco.currency` |
| `/eco currency list` | List all currencies | — | `novaeco.cmd.eco.currency` |
| `/eco set <player> <currency> <amount>` | Set a player's balance | — | `novaeco.cmd.eco.set` |
| `/eco give <player> <currency> <amount>` | Give a player money | — | `novaeco.cmd.eco.give` |
| `/eco take <player> <currency> <amount>` | Take money from a player | — | `novaeco.cmd.eco.take` |
| `/eco reset <player> <currency>` | Reset a player's balance to default | — | `novaeco.cmd.eco.reset` |
| `/eco top [currency] [page]` | View the balance leaderboard | — | `novaeco.cmd.eco.top` |
| `/novaeconomy version` | Show plugin info and update status | `/novaeco` | `novaeco.cmd.novaeco` |
| `/novaeconomy reload` | Reload config, messages, currencies, and webhooks | `/novaeco` | `novaeco.cmd.novaeco.reload` |
| `/novaeconomy update` | Check for updates from GitHub | `/novaeco` | `novaeco.cmd.novaeco` |
| `/novaeconomy check-for-updates` | Toggle automatic update checking | `/novaeco` | `novaeco.cmd.novaeco` |
| `/novaeconomy help [page]` | Show paginated command list | `/novaeco` | `novaeco.cmd.novaeco` |

Amount arguments support **short notation**: `100k` → 100,000 · `1.5m` → 1,500,000 · `1b` → 1,000,000,000 · `1t` → 1,000,000,000,000. Can be disabled in `config.yml`.

---

### Currencies

- Define any number of built-in currencies in `config.yml` under the `currencies:` section.
- Create additional currencies at runtime with `/eco currency add <name>` — persisted in the configured database.
- Each currency has a `display-name`, `symbol`, `singular`, `plural`, `default-balance`, `decimals`, and optional `max-balance`.
- The **default currency** is set via `economy.default-currency` and is used when no currency is specified in commands.

---

### Pay

- Players can pay each other with `/pay <player> <amount> [currency]`.
- Configurable **minimum amount** (`pay.min-amount`).
- Configurable **tax** percentage deducted from the sender (`pay.tax`). Players with `novaeco.pay.bypass-tax` are exempt.
- Self-pay can be enabled with `pay.self-pay: true` (requires `novaeco.pay.self`).

---

### Vault & VaultUnlocked

Configurable via `vault.mode` in `config.yml`:

| Mode | Description |
|---|---|
| `VAULT` | Registers a standard Vault Economy provider (single currency, widest plugin compatibility) |
| `VAULT_UNLOCKED` | Registers a VaultUnlocked Economy provider (full multi-currency support) |
| `BOTH` | Registers both simultaneously (default) |

The standard Vault provider maps to `vault.currency` (default: your default currency). The VaultUnlocked provider exposes all registered currencies.

---

### API

Other plugins can depend on NovaEconomy and use the public API:

```java
// In your plugin's onEnable or command:
NovaEconomyAPI eco = NovaEconomyProvider.get();

double balance = eco.getBalance(player.getUniqueId(), "COINS");
eco.deposit(player.getUniqueId(), "COINS", 500.0);
boolean success = eco.withdraw(player.getUniqueId(), "COINS", 100.0);
boolean rich    = eco.has(player.getUniqueId(), "COINS", 1000.0);
List<String> currencies = eco.getCurrencies();
```

Add NovaEconomy as a `depend` or `softdepend` in your `plugin.yml` and include its jar as a `compileOnly` dependency.

---

### Permissions

| Permission | Description | Default |
|---|---|---|
| `novaeco.*` | All permissions | op |
| `novaeco.cmd.novaeco` | Access to `/novaeconomy` | op |
| `novaeco.cmd.novaeco.reload` | Reload the plugin | op |
| `novaeco.cmd.eco` | Access to `/eco` | op |
| `novaeco.cmd.eco.currency` | Manage currencies | op |
| `novaeco.cmd.eco.set` | Set player balances | op |
| `novaeco.cmd.eco.give` | Give money to players | op |
| `novaeco.cmd.eco.take` | Take money from players | op |
| `novaeco.cmd.eco.reset` | Reset player balances | op |
| `novaeco.cmd.eco.top` | View balance leaderboard | op |
| `novaeco.cmd.balance` | Check own balance | true |
| `novaeco.cmd.balance.other` | Check another player's balance | op |
| `novaeco.cmd.baltop` | View top balances | true |
| `novaeco.cmd.pay` | Pay another player | true |
| `novaeco.pay.bypass-tax` | Pay without tax deduction | op |
| `novaeco.pay.self` | Pay yourself (if self-pay is enabled) | false |

Permission nodes can be **overridden** in `config.yml` under `permissions:`:

```yaml
permissions:
  cmd.balance: "mynetwork.balance"
  cmd.pay: "mynetwork.pay"
```

---

### Configuration

Everything is configurable in `config.yml`:

- Language selection (`general.language`)
- Automatic update checking (`general.check-for-updates`)
- Storage backend (`database.type`: `SQLITE`, `MYSQL`, or `REDIS`)
- MySQL connection settings (`database.mysql.*`)
- Redis connection settings (`database.redis.*`)
- Default currency (`economy.default-currency`)
- Short amount formatting toggle (`economy.short-amount-formatting`)
- SmallCaps toggle for all messages (`chat.small-caps.enabled`)
- Pay minimum, tax rate, and self-pay (`pay.*`)
- Vault mode (`vault.mode`) and Vault currency mapping (`vault.currency`)
- Full currency definitions (`currencies.*`)

---

### Messages & Localization

All messages live in `plugins/NovaEconomy/messages/<lang>.yml` and are written in **MiniMessage** format. The plugin ships `en.yml` as the default.

**Automatic language selection** — the plugin reads each player's Minecraft client language (`player.locale()`) and picks the matching YAML file automatically (e.g. a German client gets `de.yml` or `de_de.yml`). If no matching file exists, it falls back to the configured default.

SmallCaps is applied to every message when enabled.

---

### Discord Webhooks

Optional integration that posts embed messages to a Discord channel via a webhook URL.

Configure in `plugins/NovaEconomy/webhooks.yml`:

```yaml
enabled: true
webhook-url: "https://discord.com/api/webhooks/..."
events:
  set: true
  give: true
  take: true
  reset: true
  pay: true
```

Events that trigger a webhook embed:

| Event | Color | Placeholders |
|---|---|---|
| Balance set | Orange | `{player}` `{currency}` `{amount}` `{actor}` |
| Balance given | Green | `{player}` `{currency}` `{amount}` `{actor}` |
| Balance taken | Red | `{player}` `{currency}` `{amount}` `{actor}` |
| Balance reset | Blue | `{player}` `{currency}` `{actor}` |
| Payment | Green | `{sender}` `{receiver}` `{currency}` `{amount}` |

Embed titles, colors, and fields are fully customizable in `webhooks.yml`.

---

### Database

NovaEconomy supports three storage backends, configured via `database.type` in `config.yml`:

| Backend | Description |
|---|---|
| `SQLITE` (default) | File-based, zero-config. Data lives in `plugins/NovaEconomy/db/economy.db`. |
| `MYSQL` | MySQL or MariaDB via a HikariCP connection pool. Configure host, port, credentials, and pool size under `database.mysql`. |
| `REDIS` | In-memory store. Balances use hashes; `/baltop` rankings use sorted sets. Configure under `database.redis`. |

**Switching backends:** change `database.type` and restart. There is no automatic data migration between backends.
> **Please note:** Redis and MySQL aren't tested! Use at your own risk and make backups before switching!

> **Redis note:** data lives in memory. Enable Redis persistence (AOF or RDB) to survive restarts, or use MySQL/SQLite for durability.

---

## Dependencies

| Dependency | Type | Purpose |
|---|---|---|
| Paper 1.21+ | Required | Server API |
| Vault | Required: Option 1 | Standard single-currency economy hook |
| VaultUnlocked | Required: Option 2 | Multi-currency economy hook |
| PlaceholderAPI | Optional | PAPI placeholder support in messages |

---

## Installation

1. Drop `NovaEconomy-<version>.jar` into your `plugins/` folder.
2. Restart the server — `plugins/NovaEconomy/config.yml`, `messages/en.yml`, and `webhooks.yml` are generated automatically.
3. Edit `config.yml` to set your default currency, Vault mode, pay settings, and more.
4. **Database:** by default SQLite is used with no setup required. To switch to MySQL or Redis, set `database.type` and fill in the connection details under `database.mysql` or `database.redis`.
5. To enable Discord webhooks, edit `webhooks.yml`, set `enabled: true`, and paste your webhook URL.
6. To add another language, create `plugins/NovaEconomy/messages/<lang>.yml` (e.g. `de.yml`) using `en.yml` as a template.
