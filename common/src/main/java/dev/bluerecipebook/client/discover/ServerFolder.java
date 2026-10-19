package dev.bluerecipebook.client.discover;

import com.google.common.hash.Hashing;
import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Path segments under {@code config/bluerecipebook/remote} for one remote world.
 * Each segment is a valid folder name on Windows, macOS, and Linux.
 * Two different addresses never share one, and two LAN names share one only when they differ in case alone.
 * DNS is not consulted.
 */
final class ServerFolder {
    private static final int DEFAULT_PORT = 25565;
    private static final String SAFE = "abcdefghijklmnopqrstuvwxyz0123456789.-_";
    /** Characters a Windows, macOS, or Linux folder name cannot hold, plus this encoding's own markers. */
    private static final String RESERVED = "<>:\"/\\|?*%~";
    /** Longest encoded name kept readable, in UTF-8 bytes. File systems cap one name at 255. */
    private static final int MAX_NAME = 200;
    /** Device names Windows 10 rejects as a folder, including when an extension follows. */
    private static final Set<String> DEVICE_NAMES = Set.of(
            "con", "prn", "aux", "nul",
            "com0", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9",
            "lpt0", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9");

    private ServerFolder() {
    }

    /**
     * Returns {@code server}, the host, and the port for a typed address, or an empty list when it is blank.
     * A missing port is 25565. The host is lower case punycode without a trailing dot, and an IP literal uses one spelling.
     * An address that does not parse becomes one encoded segment after {@code server}.
     */
    static List<String> address(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            HostAndPort parsed = HostAndPort.fromString(raw).withDefaultPort(DEFAULT_PORT);
            String canonical = canonicalHost(parsed.getHost());
            if (canonical.isEmpty()) {
                return List.of("server", folderName(raw));
            }
            return List.of("server", folderName(canonical), Integer.toString(parsed.getPort()));
        } catch (IllegalArgumentException exception) {
            return List.of("server", folderName(raw));
        }
    }

    /**
     * Returns {@code lan} and the name a LAN world announces, {@code <host player> - <world name>}, or an empty list when it is blank.
     * The port is new each time a world is opened to LAN, and the host address can change, so neither is part of the folder.
     */
    static List<String> lan(String announced) {
        if (announced == null || announced.isBlank()) {
            return List.of();
        }
        return List.of("lan", readableName(announced));
    }

    /** Returns {@code realms} and the realm id, which stays the same when Realms moves the realm to another host. */
    static List<String> realm(long id) {
        return List.of("realms", Long.toString(id));
    }

    private static String canonicalHost(String host) {
        if (host.isEmpty()) {
            return "";
        }
        if (InetAddresses.isInetAddress(host)) {
            return InetAddresses.toAddrString(InetAddresses.forString(host));
        }
        // IDN turns a full-width or ideographic dot into '.', so the trailing dot is dropped after it.
        String ascii = IDN.toASCII(host).toLowerCase(Locale.ROOT);
        return ascii.endsWith(".") ? ascii.substring(0, ascii.length() - 1) : ascii;
    }

    private static String folderName(String value) {
        return encode(value, cp -> cp < 128 && SAFE.indexOf(cp) >= 0, ".");
    }

    // Lower case and NFC, because Windows and macOS compare names that way. Names that differ only in case share a folder.
    private static String readableName(String value) {
        String text = Normalizer.normalize(value, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        return encode(text, cp -> !Character.isISOControl(cp) && RESERVED.indexOf(cp) < 0, ". ");
    }

    // Windows 10 rejects CON, AUX, and the other device names, including "con.example.com", and strips trailing dots and spaces.
    private static String encode(String value, IntPredicate allowed, String trailing) {
        boolean device = deviceName(value);
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            int count = Character.charCount(cp);
            boolean keep = allowed.test(cp) && !trailingRun(value, i, trailing) && !(device && i == 0);
            if (keep) {
                out.appendCodePoint(cp);
            } else {
                percentEncode(out, value.substring(i, i + count));
            }
            i += count;
        }
        String name = out.toString();
        if (name.getBytes(StandardCharsets.UTF_8).length > MAX_NAME) {
            // '~' is always encoded above, so a hashed name cannot equal an encoded one.
            return "~" + Hashing.sha256().hashString(value, StandardCharsets.UTF_8);
        }
        return name;
    }

    private static boolean deviceName(String value) {
        int dot = value.indexOf('.');
        String stem = dot < 0 ? value : value.substring(0, dot);
        return DEVICE_NAMES.contains(stem.stripTrailing());
    }

    private static boolean trailingRun(String value, int index, String trailing) {
        for (int i = index; i < value.length(); i++) {
            if (trailing.indexOf(value.charAt(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    private static void percentEncode(StringBuilder out, String text) {
        for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
            int value = b & 0xFF;
            out.append('%');
            out.append(Character.forDigit(value >>> 4, 16));
            out.append(Character.forDigit(value & 0xF, 16));
        }
    }
}
