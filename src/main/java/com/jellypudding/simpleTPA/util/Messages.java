package com.jellypudding.simpleTPA.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

public final class Messages {

    private Messages() {
    }

    public static Component error(Object... parts) {
        return build(NamedTextColor.RED, parts);
    }

    public static Component info(Object... parts) {
        return build(NamedTextColor.YELLOW, parts);
    }

    public static Component success(Object... parts) {
        return build(NamedTextColor.GREEN, parts);
    }

    public static Component nameOf(Player player) {
        return player != null ? player.displayName() : Component.text("an offline player");
    }

    private static Component build(TextColor color, Object... parts) {
        TextComponent.Builder builder = Component.text();
        for (Object part : parts) {
            builder.append(part instanceof Component component
                    ? component
                    : Component.text(String.valueOf(part), color));
        }
        return builder.build();
    }
}
