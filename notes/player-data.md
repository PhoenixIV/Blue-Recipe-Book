# Player data

Per-player files for one save live in `<player uuid>/` under that save's folder. Later data for that player and save goes in the same player folder. The files there are `unlock-order.txt`, `strict-order.txt`, `held-items.txt`, and `waiting-toasts.txt`. What those lines mean is [Recipe order](recipe-order.md).

The save's folder holds one `format.json`, beside the player folders: `{"format":1,"since":"0.1.0"}`. `format` is the layout of the player files. `since` is the mod version that introduced that layout, and it changes only when `format` does. A missing file is format 1, and opening the folder writes it. Leaving the world and joining again reads the file again. A higher `format`, or a file that is not this object, disconnects. The screen names `since` when the file has one, and the folder to delete, relative to the game directory. The player files are left as they are.

## Local save

`<world>/bluerecipebook/<player uuid>/`, next to `playerdata` and `advancements`. Copying or deleting the world includes it.

## Remote

A remote world goes under `config/bluerecipebook/remote/`, split into `server/`, `lan/`, and `realms/`, because this client cannot write that save. Copying the world does not take those files with it. Two machines do not share them. The directory is `remote`: a world is the save, and the nether and the end are dimensions inside it.

### Server address

`config/bluerecipebook/remote/server/<host>/<port>/`.

The host is parsed the way vanilla parses a server address. A missing port is 25565. One trailing dot is ignored, including a full-width or ideographic dot. An international name is stored as lower-case punycode. An IP literal is rewritten to one spelling, so `[::1]` and a mapped IPv4 address meet their plain forms. A leading zero in an IPv4 octet is left as typed, because it is ambiguous. DNS is not consulted, so an IP and a domain stay two folders; renaming the host folder joins them.

A character a folder name cannot contain is percent-encoded, and so is an upper-case letter, so two addresses never share a folder on a case-insensitive disk. A string that does not parse is one percent-encoded segment under `server/`, with no port folder. A name longer than 200 bytes after encoding is `~` and its SHA-256 instead.

A transfer packet keeps the folder of the address the player typed, because vanilla reconnects with the same server entry. A proxy network is one folder for all its backend servers.

A LAN world typed into Direct Connect is a server address. Its port is in the folder, and that port is new each time the host opens the world to LAN.

### LAN world

Joined from the LAN list: `config/bluerecipebook/remote/lan/<announced name>/`.

The announced name is `<host player> - <world name>`. It is stored in lower case and Unicode NFC. Spaces and non-ASCII letters stay. Percent-encoding is limited to `< > : " / \ | ? * % ~`, control characters, a trailing dot or space, and a Windows device name such as `con`.

The address is left out. The port is new each time the world is opened to LAN, and the host's address can change.

#### Limits

The announced name is the world's `LevelName`. The edit screen rewrites that and leaves the save folder alone, and the save folder is never sent to a guest. Renaming the world starts a new folder. The files stay under the previous name, and renaming back finds them. An open LAN session is unaffected, because the world has to be closed to rename it.

Two worlds of one host with the same name share a folder, and so do names that differ only in case. Vanilla does not make world names unique, so several worlds left as "New World" are one folder. Files for those worlds mix. Nothing is deleted.

The login packet carries a hash of the world seed, `BiomeManager.obfuscateSeed`: SHA-256 of the seed that generates the world, kept as one long. It is stable for that seed and a rename does not change it. It is not used. A server or a mod can replace it with a fixed or random value, and two worlds generated from one seed produce the same hash. Following a rename would take a remembered folder id, the name, that hash, and the world's game time. That is not built.

### Realms

`config/bluerecipebook/remote/realms/<realm id>/`.

The server entry vanilla builds for a realm has the realm's host and no port, so two realms on one host machine would share an address folder, and a realm moved to another machine would start a new one. `RealmsConnectMixin` records `RealmsServer.id` and the realm's name when `RealmsConnect.connect` starts. The id is a plain number. Each join writes the current realm name to `realm-name.txt` in that folder, so a renamed realm keeps its files and the name stays current.
