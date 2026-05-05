package com.dippycoder.novaEconomy.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

public final class SmallCapsUtil {

    private static final char[] SMALL = {
        'ᴀ','ʙ','ᴄ','ᴅ','ᴇ','ꜰ','ɢ','ʜ','ɪ','ᴊ','ᴋ','ʟ','ᴍ',
        'ɴ','ᴏ','ᴘ','ǫ','ʀ','s','ᴛ','ᴜ','ᴠ','ᴡ','x','ʏ','ᴢ'
    };

    private SmallCapsUtil() {}

    public static String convert(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (c >= 'a' && c <= 'z') sb.append(SMALL[c - 'a']);
            else if (c >= 'A' && c <= 'Z') sb.append(SMALL[c - 'A']);
            else sb.append(c);
        }
        return sb.toString();
    }

    public static Component applyToComponent(Component component) {
        Component result = component;
        if (component instanceof TextComponent tc) {
            result = tc.content(convert(tc.content()));
        }
        List<Component> newChildren = new ArrayList<>();
        for (Component child : result.children()) {
            newChildren.add(applyToComponent(child));
        }
        return result.children(newChildren);
    }
}
