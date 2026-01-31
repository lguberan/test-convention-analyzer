package com.guberan.testanalyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class GitService {

    public Path cloneToTemp(String url) {
        try {
            Path dir = Files.createTempDirectory("test-analyzer-clone-");
            log.info("Cloning {} into {}", url, dir);

            Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(dir.toFile())
                    .setCloneAllBranches(false)
                    .call()
                    .close();

            return dir;
        } catch (Exception e) {
            throw new RuntimeException("Git clone failed: " + e.getMessage(), e);
        }
    }
}