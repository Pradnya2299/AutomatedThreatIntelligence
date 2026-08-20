package com.threatadvisor.ai.coderemediation.workspace;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FixtureWorkspaceFactory {

    private FixtureWorkspaceFactory() {
    }

    public static Path materialize(String fixtureName, Path parent) {
        try {
            Path root = Files.createTempDirectory(parent, "ai-security-" + fixtureName + "-");
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:code-remediation-fixtures/" + fixtureName + "/**");
            if (resources.length == 0) {
                throw new IllegalStateException("Unknown fixture workspace: " + fixtureName);
            }
            String prefix = "code-remediation-fixtures/" + fixtureName + "/";
            for (Resource resource : resources) {
                if (!resource.isReadable() || resource.getFilename() == null) {
                    continue;
                }
                String url = resource.getURL().toString().replace('\\', '/');
                int idx = url.indexOf(prefix);
                if (idx < 0) {
                    continue;
                }
                String relative = url.substring(idx + prefix.length());
                if (relative.isBlank() || relative.endsWith("/")) {
                    continue;
                }
                Path dest = root.resolve(relative).normalize();
                if (!dest.startsWith(root)) {
                    continue;
                }
                Files.createDirectories(dest.getParent());
                try (InputStream in = resource.getInputStream()) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return root;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to materialize fixture " + fixtureName, ex);
        }
    }
}
