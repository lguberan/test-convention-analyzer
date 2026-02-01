package com.guberan.testanalyzer.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class JavaAstService {

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