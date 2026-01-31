package com.guberan.testanalyzer.util;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Utility methods related to Java source file paths.
 */
public final class PathUtil {

    private PathUtil() {
        // utility class
    }

    /**
     * Derives the Fully Qualified Name (FQN) of a Java class from its filesystem path.
     *
     * <p>Example:
     * <pre>
     *   /repo/module-a/src/main/java/com/foo/Bar.java
     *   → com.foo.Bar
     * </pre>
     *
     * <p>The method:
     * <ol>
     *   <li>Locates the {@code src/main/java} source root</li>
     *   <li>Extracts the relative path after that root</li>
     *   <li>Removes the {@code .java} extension</li>
     *   <li>Replaces path separators with dots</li>
     * </ol>
     *
     * <p>If the provided file:
     * <ul>
     *   <li>is not under {@code src/main/java}, or</li>
     *   <li>does not end with {@code .java}</li>
     * </ul>
     * the method returns {@link Optional#empty()}.
     *
     * <p>This approach works transparently with multi-module projects, since the
     * source root may appear at any depth in the directory tree.
     *
     * @param projectRoot the project root directory (currently unused but kept for API symmetry/future extensions)
     * @param javaFile    the path to a Java source file
     * @return the fully qualified class name, or empty if the file is not a main Java source
     */
    public static Optional<String> fqnFromMainJavaPath(Path projectRoot, Path javaFile) {

        String norm = javaFile.toString().replace('\\', '/');

        int idx = norm.indexOf("/src/main/java/");
        if (idx < 0) return Optional.empty();

        String rel = norm.substring(idx + "/src/main/java/".length());

        if (!rel.endsWith(".java")) return Optional.empty();

        rel = rel.substring(0, rel.length() - ".java".length());
        rel = rel.replace('/', '.');

        return Optional.of(rel);
    }
}