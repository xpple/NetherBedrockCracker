package dev.xpple.netherbedrockcracker.buildscript;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.tasks.Exec;

public abstract class CreateJavaBindingsTask extends Exec {

    private static final String EXTENSION = Os.isFamily(Os.FAMILY_WINDOWS) ? ".bat" : "";

    {
        // always run task
        this.getOutputs().upToDateWhen(_ -> false);

        this.setWorkingDir(this.getProject().getRootDir());
        this.setStandardOutput(System.out);
        this.commandLine("./jextract/build/jextract/bin/jextract" + EXTENSION, "--output", "src/main/java", "--use-system-load-library", "--target-package", "com.github.netherbedrockcracker", "--header-class-name", "NetherBedrockCracker", "@includes.txt", "src/main/rust/bedrock_cracker/netherbedrockcracker.h");
    }
}
