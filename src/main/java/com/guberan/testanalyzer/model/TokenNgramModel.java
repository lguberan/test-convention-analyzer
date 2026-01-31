package com.guberan.testanalyzer.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class TokenNgramModel {

    private static final String START = "^";
    private static final String END = "$";

    private static final Pattern TOKEN_SPLIT =
            Pattern.compile("(?<!^)(?=[A-Z])|[_\\-]");

    private final Map<String, Long> unigrams = new HashMap<>();

    // prev -> (next -> count)
    private final Map<String, Map<String, Long>> bigrams = new HashMap<>();

    // (prev1, prev2) -> (next -> count)
    private final Map<Pair, Map<String, Long>> trigrams = new HashMap<>();

    private long sequences = 0;

    public static List<String> tokenize(String s) {
        if (s == null || s.isBlank()) return List.of();
        String[] raw = TOKEN_SPLIT.split(s);
        List<String> out = new ArrayList<>(raw.length);
        for (String r : raw) {
            if (r == null || r.isBlank()) continue;
            out.add(r.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private static List<Map.Entry<String, Long>> topK(Map<String, Long> m, int k) {
        var list = new ArrayList<>(m.entrySet());
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return list.size() <= k ? list : list.subList(0, k);
    }

    /**
     * Ingest one method name (token sequence).
     */
    public void acceptMethodName(String methodName) {
        List<String> tokens = tokenize(methodName);
        if (tokens.isEmpty()) return;

        sequences++;

        // Add boundaries: ^, ^, ...tokens..., $
        List<String> seq = new ArrayList<>(tokens.size() + 3);
        seq.add(START);
        seq.add(START);
        seq.addAll(tokens);
        seq.add(END);

        // unigrams
        for (String t : tokens) {
            unigrams.merge(t, 1L, Long::sum);
        }

        // bigrams + trigrams
        for (int i = 0; i < seq.size() - 1; i++) {
            addBigram(seq.get(i), seq.get(i + 1));
        }
        for (int i = 0; i < seq.size() - 2; i++) {
            addTrigram(seq.get(i), seq.get(i + 1), seq.get(i + 2));
        }
    }

    /**
     * Top-k tokens.
     */
    public List<Map.Entry<String, Long>> topUnigrams(int k) {
        return topK(unigrams, k);
    }

    /**
     * Top-k bigrams (flattened).
     */
    public List<Bigram> topBigrams(int k) {
        List<Bigram> out = new ArrayList<>();
        for (var e1 : bigrams.entrySet()) {
            for (var e2 : e1.getValue().entrySet()) {
                out.add(new Bigram(e1.getKey(), e2.getKey(), e2.getValue()));
            }
        }
        out.sort((a, b) -> Long.compare(b.count(), a.count()));
        return out.size() <= k ? out : out.subList(0, k);
    }

    /**
     * Top-k trigrams (flattened).
     */
    public List<Trigram> topTrigrams(int k) {
        List<Trigram> out = new ArrayList<>();
        for (var e1 : trigrams.entrySet()) {
            Pair prev = e1.getKey();
            for (var e2 : e1.getValue().entrySet()) {
                out.add(new Trigram(prev.a(), prev.b(), e2.getKey(), e2.getValue()));
            }
        }
        out.sort((a, b) -> Long.compare(b.count(), a.count()));
        return out.size() <= k ? out : out.subList(0, k);
    }

    /**
     * P(next | prev) from bigram counts.
     */
    public double pBigram(String prev, String next) {
        Map<String, Long> nexts = bigrams.get(prev);
        if (nexts == null) return 0.0;
        long denom = 0;
        for (long c : nexts.values()) denom += c;
        if (denom == 0) return 0.0;
        return nexts.getOrDefault(next, 0L) / (double) denom;
    }

    /**
     * P(next | prev1, prev2) from trigram counts.
     */
    public double pTrigram(String prev1, String prev2, String next) {
        Map<String, Long> nexts = trigrams.get(new Pair(prev1, prev2));
        if (nexts == null) return 0.0;
        long denom = 0;
        for (long c : nexts.values()) denom += c;
        if (denom == 0) return 0.0;
        return nexts.getOrDefault(next, 0L) / (double) denom;
    }

    // ----- internal helpers -----

    /**
     * Returns the most likely next token given prev1, prev2 (greedy).
     */
    public Optional<String> mostLikelyNext(String prev1, String prev2) {
        Map<String, Long> nexts = trigrams.get(new Pair(prev1, prev2));
        if (nexts == null || nexts.isEmpty()) return Optional.empty();
        return nexts.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey);
    }

    /**
     * Generate a likely token sequence from trigram model (greedy).
     */
    public List<String> generateGreedy(int maxTokens) {
        List<String> out = new ArrayList<>();
        String prev1 = START;
        String prev2 = START;

        for (int i = 0; i < maxTokens; i++) {
            String next = mostLikelyNext(prev1, prev2).orElse(END);
            if (END.equals(next)) break;
            out.add(next);
            prev1 = prev2;
            prev2 = next;
        }
        return out;
    }

    private void addBigram(String prev, String next) {
        bigrams.computeIfAbsent(prev, k -> new HashMap<>())
                .merge(next, 1L, Long::sum);
    }

    private void addTrigram(String prev1, String prev2, String next) {
        trigrams.computeIfAbsent(new Pair(prev1, prev2), k -> new HashMap<>())
                .merge(next, 1L, Long::sum);
    }

    public long getSequences() {
        return sequences;
    }

    public record Bigram(String prev, String next, long count) {
    }

    public record Trigram(String prev1, String prev2, String next, long count) {
    }

    /**
     * Small value-object key for trigram contexts.
     */
    public record Pair(String a, String b) {
    }
}