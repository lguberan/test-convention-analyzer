package com.guberan.testanalyzer.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class JavaAstService {
    private static final Logger log = LoggerFactory.getLogger(JavaAstService.class);

    private final JavaParser parser;

    public JavaAstService() {
        var cfg = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_25);
        this.parser = new JavaParser(cfg);
    }

    public Optional<CompilationUnit> parse(Path javaFile) {
        try {
            var result = parser.parse(javaFile);
            if (result.isSuccessful() && result.getResult().isPresent()) {
                return result.getResult();
            }
            log.debug("Parse issues in {}: {}", javaFile, result.getProblems());
            return Optional.empty();
        } catch (IOException e) {
            log.debug("Parse IO error in {}: {}", javaFile, e.getMessage());
            return Optional.empty();
        }
    }
}