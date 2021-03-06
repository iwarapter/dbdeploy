package com.dbdeploy.scripts;

import com.dbdeploy.exceptions.UnrecognisedFilenameException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DirectoryScanner {

	final static Logger logger = Logger.getLogger(DirectoryScanner.class);
	
	private final FilenameParser filenameParser = new FilenameParser();
	private final String encoding;
	
	public DirectoryScanner(String encoding) {
        this.encoding = encoding;
    }
	
	public List<ChangeScript> getChangeScriptsForDirectory(File directory)  {
		try {
			logger.info("Reading change scripts from directory " + directory.getCanonicalPath() + "...");
		} catch (IOException e1) {
			// ignore
		}

		List<ChangeScript> scripts = new ArrayList<ChangeScript>();
		
		for (File file : directory.listFiles()) {
			if (file.isFile()) {
				String filename = file.getName();
				try {
					long id = filenameParser.extractIdFromFilename(filename);
					scripts.add(new ChangeScript(id, file, encoding));
				} catch (UnrecognisedFilenameException e) {
					// ignore
				}
			}
		}
		
		return scripts;

	}

}
