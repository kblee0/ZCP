package com.mup.pop3;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ConfigFile {
	private ConfigFile() {
	}

	
	public static File getFile(String name) {
		File file = new File(name);
		
		// Filename only
		if( file.getName().equals(name) ) {
			// 1. config/<Filename>
			File configFile  = new File("config/" + name);
			if( configFile.exists() ) {
				return configFile;
			}
			// 2. current dir is <base>/bin, config is <base>/config/<File>
			configFile = new File("../config/" + name);
			if( configFile.exists() ) {
				return configFile;
			}
		}
		// 3. ./<File>
		if( file.exists() ) {
			return file;
		}
		return null;
	}

	public static InputStream getResourceAsStream(String name) {
		InputStream resource = ConfigFile.class.getClassLoader().getResourceAsStream("config/" + name);
		
		if( resource == null ) {
			resource = ConfigFile.class.getClassLoader().getResourceAsStream(name);
		}
		return resource;
	}
	public static InputStream getStream( String name ) throws Exception {
		File config = getFile(name);
		
		if( config != null ) {
			return new FileInputStream(config);
		}
		return getResourceAsStream(name);
	}
}
