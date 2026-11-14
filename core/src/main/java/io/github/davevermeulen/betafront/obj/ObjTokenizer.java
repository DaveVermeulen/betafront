/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 *
 * Ported from NeoForge 1.21.1 (net.neoforged.neoforge.client.model.obj.ObjTokenizer)
 * for Betafront on Babric b1.7.3. Changes: removed Guava/joptsimple dependencies,
 * trims surrounding whitespace, tracks line numbers for error messages.
 */

package io.github.davevermeulen.betafront.obj;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A tokenizer for OBJ and MTL files.
 * <p>
 * Joins split lines and ignores comments.
 */
public class ObjTokenizer implements AutoCloseable {
    private final BufferedReader lineReader;
    private int lineNumber;

    public ObjTokenizer(InputStream inputStream) {
        this.lineReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    /**
     * @return the 1-based number of the last physical line read, for error messages.
     */
    public int getLineNumber() {
        return lineNumber;
    }

    private String nextLine() throws IOException {
        String line = lineReader.readLine();
        if (line != null) {
            lineNumber++;
            line = line.strip();
            // Only whole-line comments: "#" inside a line is meaningful (MTL "map_Kd #texture0").
            if (line.startsWith("#"))
                line = "";
        }
        return line;
    }

    /**
     * Reads the next logical line and splits it on whitespace.
     *
     * @return the tokens of the line, or {@code null} at end of input. When {@code ignoreEmptyLines}
     *         is false an empty line returns an empty array.
     */
    public String[] readAndSplitLine(boolean ignoreEmptyLines) throws IOException {
        do {
            String currentLine = nextLine();
            if (currentLine == null)
                return null;

            List<String> lineParts = new ArrayList<>();

            if (!currentLine.isEmpty()) {
                boolean hasContinuation;
                do {
                    hasContinuation = currentLine.endsWith("\\");
                    String tmp = hasContinuation ? currentLine.substring(0, currentLine.length() - 1) : currentLine;

                    for (String part : tmp.split("[\t ]+"))
                        if (!part.isEmpty())
                            lineParts.add(part);

                    if (hasContinuation) {
                        currentLine = nextLine();
                        if (currentLine == null || currentLine.isEmpty())
                            break;
                    }
                } while (hasContinuation);
            }

            if (!lineParts.isEmpty())
                return lineParts.toArray(new String[0]);
        } while (ignoreEmptyLines);

        return new String[0];
    }

    @Override
    public void close() throws IOException {
        lineReader.close();
    }
}
