package com.blocklang.release.task;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTask {
	static final Logger logger = LoggerFactory.getLogger(AbstractTask.class);
	
	protected AppBuildContext appBuildContext;
	
	public AbstractTask(AppBuildContext appBuildContext) {
		this.appBuildContext = appBuildContext;
	}

	protected boolean runCommand(Path workingDirectory, List<String> commands) {
		ProcessBuilder processBuilder = new ProcessBuilder(commands).directory(workingDirectory.toFile());
		
		try {
			
			Path logFileDir = appBuildContext.getLogDirectory();
			String logFileName = appBuildContext.getLogFileName();
			Files.createDirectories(logFileDir);
			processBuilder.redirectErrorStream(true);
			processBuilder.redirectOutput(Redirect.appendTo(logFileDir.resolve(logFileName).toFile()));
			
			Process process = processBuilder.start();
			
			if(process.isAlive()) {
				process.waitFor();
			}
			return process.exitValue() == 0;
		} catch (IOException | InterruptedException e) {
			logger.error(e.getMessage(), e);
		}
		return false;
	}

}