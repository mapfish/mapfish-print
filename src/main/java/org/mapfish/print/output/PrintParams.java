package org.mapfish.print.output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.mapfish.print.config.Config;
import org.mapfish.print.utils.PJsonObject;

public class PrintParams {
	public Config config;
	public File configDir;
	public PJsonObject jsonSpec;
	public OutputStream outputStream;
	public String referer;

	public PrintParams(Config config, File configDir, PJsonObject jsonSpec,
			OutputStream outputStream, String referer) {
		this.config = config;
		this.configDir = configDir;
		this.jsonSpec = jsonSpec;
		this.outputStream = outputStream;
		this.referer = referer;
	}

	/**
	 * Create a new params object with a different output stream
	 * @param newOut
	 * @return
	 */
	public PrintParams withOutput(FileOutputStream newOut) {
		return new PrintParams(config, configDir, jsonSpec, newOut, referer);
	}
}