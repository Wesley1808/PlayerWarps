package me.wesley1808.playerwarps.util;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import net.minecraft.network.chat.Component;

public class Formatter {
    private static final NodeParser PARSER = NodeParser.builder()
            .simplifiedTextFormat()
            .requireSafe()
            .quickText()
            .build();

    public static Component parse(String input) {
        return PARSER.parseText(input, ParserContext.of());
    }
}
