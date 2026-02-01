package com.guberan.testanalyzer.service;

import com.guberan.testanalyzer.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ProjectScanner {

    /**
     * Recursively scans a project directory while skipping irrelevant folders
     * such as .git, build outputs, node_modules, etc.
     * and computes:
     * <ul>
     *   <li>Total number of regular files</li>
     *   <li>Number of files per extension (case-insensitive)</li>
     *   <li>List of all {@code .java} source files</li>
     * </ul>
     * <p>
     *
     * @param root the project root directory to scan
     * @return a {@link ScanResult} containing aggregated statistics
     * @throws RuntimeException if an I/O error occurs while walking the file tree
     */
    public ScanResult scan(Path root) {

        Map<String, Long> extensionCounts = new HashMap<>();
        List<Path> javaFiles = new ArrayList<>();
        AtomicLong totalFiles = new AtomicLong();

        // directories we never want to scan
        Set<String> excludedDirs = Set.of(".git", "target", "build", "node_modules", ".gradle", ".idea");

        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName().toString();

                    if (excludedDirs.contains(name)) {
                        return FileVisitResult.SKIP_SUBTREE; // 🚀 big win
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    if (!attrs.isRegularFile()) {
                        return FileVisitResult.CONTINUE;
                    }

                    totalFiles.incrementAndGet();

                    String ext = StringUtil.extensionOf(file.getFileName().toString());
                    extensionCounts.merge(ext, 1L, Long::sum);

                    if ("java".equals(ext)) {
                        javaFiles.add(file);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            throw new RuntimeException("Scanning failed: " + e.getMessage(), e);
        }

        long total = totalFiles.get();
        log.info("Scanned {} files", total);

        return new ScanResult(total, extensionCounts, javaFiles);
    }


    /**
     * Immutable result returned by {@link #scan(Path)}.
     *
     * @param totalFiles      total number of regular files discovered
     * @param extensionCounts mapping of file extension -> count
     * @param javaFiles       list of all detected .java files
     */
    public record ScanResult(long totalFiles, Map<String, Long> extensionCounts, List<Path> javaFiles) {
    }
}