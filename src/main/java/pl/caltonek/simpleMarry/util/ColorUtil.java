package pl.caltonek.simpleMarry.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private static final Pattern CMI_GRADIENT_PATTERN = Pattern.compile("\\{#([0-9a-fA-F]{6})\\}>(.*?)\\{#([0-9a-fA-F]{6})<\\}");
    private static final Pattern LEGACY_GRADIENT_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})>(.*?)&#([0-9a-fA-F]{6})<");

    private ColorUtil() {}

    public static @NotNull Component color(final @Nullable String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        String parsed = parsePatterns(input);
        parsed = parseLegacyAndHex(parsed);
        return MM.deserialize(parsed);
    }

    public static @NotNull String colorize(final @Nullable String input) {
        if (input == null || input.isEmpty()) return "";
        return LEGACY_SERIALIZER.serialize(color(input));
    }

    public static @NotNull String parsePatterns(final @Nullable String str) {
        if (str == null) return "";

        String result = str;
        Matcher cmiMatcher = CMI_GRADIENT_PATTERN.matcher(result);
        while (cmiMatcher.find()) {
            final String start = cmiMatcher.group(1);
            final String content = cmiMatcher.group(2);
            final String end = cmiMatcher.group(3);
            result = result.replace(cmiMatcher.group(0), "<gradient:#" + start + ":#" + end + ">" + content + "</gradient>");
            cmiMatcher = CMI_GRADIENT_PATTERN.matcher(result);
        }

        Matcher legacyMatcher = LEGACY_GRADIENT_PATTERN.matcher(result);
        while (legacyMatcher.find()) {
            final String start = legacyMatcher.group(1);
            final String content = legacyMatcher.group(2);
            final String end = legacyMatcher.group(3);
            result = result.replace(legacyMatcher.group(0), "<gradient:#" + start + ":#" + end + ">" + content + "</gradient>");
            legacyMatcher = LEGACY_GRADIENT_PATTERN.matcher(result);
        }

        return result;
    }

    public static @NotNull String parseLegacyAndHex(final @NotNull String str) {
        final int len = str.length();
        final StringBuilder sb = new StringBuilder(len + 16);
        final char[] chars = str.toCharArray();

        for (int i = 0; i < len; i++) {
            final char c = chars[i];

            if ((c == '&' || c == '§') && i + 1 < len) {
                final char next = Character.toLowerCase(chars[i + 1]);

                // Bungee hex
                if (next == 'x' && i + 13 < len) {
                    boolean isBungee = true;
                    for (int k = 2; k <= 12; k += 2) {
                        final char sub = chars[i + k];
                        if (sub != '&' && sub != '§') { isBungee = false; break; }
                    }
                    if (isBungee) {
                        sb.append("<#")
                                .append(chars[i + 3]).append(chars[i + 5])
                                .append(chars[i + 7]).append(chars[i + 9])
                                .append(chars[i + 11]).append(chars[i + 13])
                                .append('>');
                        i += 13;
                        continue;
                    }
                }

                // &#rrggbb, &#rgb
                if (next == '#' && i + 4 < len) {
                    if (i + 7 < len && isHex(chars, i + 2, 6)) {
                        sb.append("<#").append(chars, i + 2, 6).append('>');
                        i += 7;
                        continue;
                    } else if (isHex(chars, i + 2, 3)) {
                        sb.append("<#")
                                .append(chars[i + 2]).append(chars[i + 2])
                                .append(chars[i + 3]).append(chars[i + 3])
                                .append(chars[i + 4]).append(chars[i + 4])
                                .append('>');
                        i += 4;
                        continue;
                    }
                }

                // Legacy
                final String tag = switch (next) {
                    case '0' -> "<black>";
                    case '1' -> "<dark_blue>";
                    case '2' -> "<dark_green>";
                    case '3' -> "<dark_aqua>";
                    case '4' -> "<dark_red>";
                    case '5' -> "<dark_purple>";
                    case '6' -> "<gold>";
                    case '7' -> "<gray>";
                    case '8' -> "<dark_gray>";
                    case '9' -> "<blue>";
                    case 'a' -> "<green>";
                    case 'b' -> "<aqua>";
                    case 'c' -> "<red>";
                    case 'd' -> "<light_purple>";
                    case 'e' -> "<yellow>";
                    case 'f' -> "<white>";
                    case 'k' -> "<obfuscated>";
                    case 'l' -> "<bold>";
                    case 'm' -> "<strikethrough>";
                    case 'n' -> "<underlined>";
                    case 'o' -> "<italic>";
                    case 'r' -> "<reset>";
                    default -> null;
                };

                if (tag != null) {
                    sb.append(tag);
                    i++;
                    continue;
                }
            }

            // CMI Hex
            if (c == '{' && i + 1 < len && chars[i + 1] == '#') {
                if (i + 8 < len && chars[i + 8] == '}' && isHex(chars, i + 2, 6)) {
                    sb.append("<#").append(chars, i + 2, 6).append('>');
                    i += 8;
                    continue;
                } else if (i + 5 < len && chars[i + 5] == '}' && isHex(chars, i + 2, 3)) {
                    sb.append("<#")
                            .append(chars[i + 2]).append(chars[i + 2])
                            .append(chars[i + 3]).append(chars[i + 3])
                            .append(chars[i + 4]).append(chars[i + 4])
                            .append('>');
                    i += 5;
                    continue;
                }
            }

            // MiniMessage Hex
            if (c == '<' && i + 1 < len && chars[i + 1] == '#') {
                if (i + 5 < len && chars[i + 5] == '>' && isHex(chars, i + 2, 3)) {
                    sb.append("<#")
                            .append(chars[i + 2]).append(chars[i + 2])
                            .append(chars[i + 3]).append(chars[i + 3])
                            .append(chars[i + 4]).append(chars[i + 4])
                            .append('>');
                    i += 5;
                    continue;
                }
            }

            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean isHex(final char @NotNull [] chars, final int start, final int count) {
        for (int i = start; i < start + count; i++) {
            final char ch = chars[i];
            if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))) return false;
        }
        return true;
    }
}