package io.github.AaditS22.asmsimulator.frontend;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// DISCLAIMER: This class was largely written with the help of LLMs
public final class AsmHighlighter {

    private static final String S_DEFAULT   = "-fx-fill: #BBBBBB;";
    private static final String S_COMMENT   = "-fx-fill: #6A8759;";
    private static final String S_STRING    = "-fx-fill: #6A8759;";
    private static final String S_LABEL     = "-fx-fill: #C792EA; -fx-font-weight: bold;";
    private static final String S_SECTION   = "-fx-fill: #C792EA; -fx-font-weight: bold;";
    private static final String S_DIRECTIVE = "-fx-fill: #F78C6C;";
    private static final String S_MNEMONIC  = "-fx-fill: #E8A845; -fx-font-weight: bold;";
    private static final String S_REGISTER  = "-fx-fill: #56A6E8;";
    private static final String S_IMMEDIATE = "-fx-fill: #A8D880;";

    private static final String MNEMONICS =
            "mov[a-z]*|add[a-z]*|sub[a-z]*|imul[a-z]*|mul[a-z]*|idiv[a-z]*|div[a-z]*|" +
                    "and[a-z]*|or[a-z]*|xor[a-z]*|not[a-z]*|neg[a-z]*|cmp[a-z]*|test[a-z]*|" +
                    "shl[a-z]*|shr[a-z]*|sar[a-z]*|sal[a-z]*|lea[a-z]*|" +
                    "push[a-z]*|pop[a-z]*|inc[a-z]*|dec[a-z]*|" +
                    "jmp|je|jne|jg|jge|jl|jle|ja|jae|jb|jbe|jz|jnz|js|jns|jo|jno|jc|jnc|" +
                    "call|ret|nop|cdq|cqto|cltd|cltq|syscall|int|hlt|leave|enter|" +
                    "movzb[a-z]*|movzw[a-z]*|movs[a-z]*";

    private static final Pattern PATTERN = Pattern.compile(
            "(?m)(?<COMMENT>#[^\\n]*)" +
                    "|(?<STRING>\"(?:[^\"\\\\]|\\\\.)*\")" +
                    "|(?<SECTION>\\.(?:text|data|bss|rodata)(?=\\s|$))" +
                    "|(?<DIRECTIVE>\\.(?:quad|long|word|byte|ascii|asciz|string|" +
                    "skip|zero|globl|global|equ|set|align|comm|lcomm|fill|space|type|size|section)(?=\\s|$|,))" +
                    "|(?<LABEL>^[ \\t]*[A-Za-z_.][A-Za-z0-9_.]*:)" +
                    "|(?<MNEMONIC>(?<![A-Za-z0-9_.])(?:" + MNEMONICS + ")(?=[ \\t,\\n#]|$))" +
                    "|(?<REGISTER>%[a-zA-Z0-9]+)" +
                    "|(?<IMMEDIATE>\\$(?:0x[0-9a-fA-F]+|-?[0-9]+|0b[01]+))"
    );

    private AsmHighlighter() {}

    /**
     * Gets the highlighting for the given text
     * @param text the text to highlight
     * @return the highlighting for the given text as StyleSpans
     */
    public static StyleSpans<String> computeHighlighting(String text) {
        if (text.isEmpty()) {
            StyleSpansBuilder<String> b = new StyleSpansBuilder<>();
            b.add(S_DEFAULT, 0);
            return b.create();
        }
        StyleSpansBuilder<String> builder = new StyleSpansBuilder<>();
        Matcher matcher = PATTERN.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                builder.add(S_DEFAULT, matcher.start() - lastEnd);
            }
            builder.add(resolveStyle(matcher), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            builder.add(S_DEFAULT, text.length() - lastEnd);
        }
        return builder.create();
    }

    private static String resolveStyle(Matcher m) {
        if (m.group("COMMENT")   != null) return S_COMMENT;
        if (m.group("STRING")    != null) return S_STRING;
        if (m.group("SECTION")   != null) return S_SECTION;
        if (m.group("DIRECTIVE") != null) return S_DIRECTIVE;
        if (m.group("LABEL")     != null) return S_LABEL;
        if (m.group("MNEMONIC")  != null) return S_MNEMONIC;
        if (m.group("REGISTER")  != null) return S_REGISTER;
        if (m.group("IMMEDIATE") != null) return S_IMMEDIATE;
        return S_DEFAULT;
    }
}