package dev.xpple.netherbedrockcracker.buildscript;

import org.gradle.api.tasks.Exec;

public abstract class CreateHeaderFileTask extends Exec {
    {
        // always run task
        this.getOutputs().upToDateWhen(_ -> false);

        this.setWorkingDir(this.getProject().getRootDir().toPath().resolve("src").resolve("main").resolve("rust"));
        this.setStandardOutput(System.out);
        this.commandLine("cbindgen", "--config", "../../../cbindgen.toml", "--crate", "bedrock_cracker", "--output", "bedrock_cracker/netherbedrockcracker.h");
    }
}
