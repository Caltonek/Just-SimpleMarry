package pl.caltonek.simpleMarry.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class ColorUtil {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private ColorUtil() {}

    public static Component color(String input) {
        if (input == null || input.isEmpty()) return Component.empty();
        return MM.deserialize(parseLegacyAndHex(input));
    }

    public static String parseLegacyAndHex(String str) {
        int len = str.length();
        StringBuilder sb = new StringBuilder(len + 16);
        char[] chars = str.toCharArray();

        for (int i = 0; i < len; i++) {
            char c = chars[i];

            if ((c == '&' || c == '§') && i + 1 < len) {
                char next = Character.toLowerCase(chars[i + 1]);

                // Bungee hex
                if (next == 'x' && i + 13 < len) {
                    boolean isBungee = true;
                    for (int k = 2; k <= 12; k += 2) {
                        char sub = chars[i + k];
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
                String tag = switch (next) {
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

    private static boolean isHex(char[] chars, int start, int count) {
        for (int i = start; i < start + count; i++) {
            char ch = chars[i];
            if (!((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F'))) return false;
        }
        return true;
    }
}