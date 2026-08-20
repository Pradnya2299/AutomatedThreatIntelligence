package com.threatadvisor.ai.coderemediation.patch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenDependencyPatcherTest {

    @Test
    void upgradesLog4jCoreOnly() {
        String pom = """
                <project>
                  <dependencies>
                    <dependency>
                      <groupId>org.apache.logging.log4j</groupId>
                      <artifactId>log4j-core</artifactId>
                      <version>2.14.1</version>
                    </dependency>
                  </dependencies>
                </project>
                """;
        assertEquals("2.14.1", MavenDependencyPatcher.currentVersion(pom, "log4j-core"));
        String patched = MavenDependencyPatcher.upgrade(pom, "log4j-core", "2.17.1");
        assertTrue(patched.contains("<version>2.17.1</version>"));
        assertEquals("2.17.1", MavenDependencyPatcher.currentVersion(patched, "log4j-core"));
    }
}
