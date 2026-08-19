package com.threatadvisor.ai.coderemediation.validation;

import java.nio.file.Path;
import java.util.List;

public interface ValidationRunner {
    List<ValidationOutcome> run(Path workspace, List<String[]> commands);
}
