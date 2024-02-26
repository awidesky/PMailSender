package com.awidesky.pMailsender;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * https://stackoverflow.com/a/12733172
 * */
public class ConfigFilePathGetter {

	public static String getProjectPath() {
		
		return Stream.of(
					classLocationBased(), 
					propertyBased(),
					fileBased()
				).map(ret -> {
					File f = new File(ret).getAbsoluteFile();
					if (!f.isDirectory())
						ret = f.getParentFile().getAbsolutePath();
					if (System.getProperty("jpackage.app-path") != null) {
						ret += File.separator + "app";
					}
					return ret;
				}).map(s -> new File(s + File.separator + "config.txt"))
				.filter(File::exists)
				.map(File::getParentFile)
				.map(File::getAbsolutePath)
				.findFirst()
				.orElse(".")
				+ File.separator;
	}
	
	
	/**
	 * Get project path by find Class path as an URL and decode as string
	 * 
	 * Code from https://stackoverflow.com/a/12733172
	 * doesn't work in IDE(points bin folder of project root)
	 * */
	private static String classLocationBased() {
		return urlToFile(getLocation(MailSender.class)).getAbsolutePath();
	}
	/**
	 * Get project path by getting system property java.class.path
	 * 
	 * doesn't work in IDE(points bin folder of project root)
	 * */
	private static String propertyBased() {
		return System.getProperty("java.class.path");
	}
	/**
	 * Get project path by getting absolute path of new File("")
	 * 
	 * This actually get a working directory, not a path of actual working directory.
	 * It works at most cases, but not when running the jar by command prompt whose working directory is not where jar file located.   
	 * */
	private static String fileBased() {
		return new File("").getAbsolutePath();
	}
	
	
	
	
	/**
	 * Gets the base location of the given class.
	 * <p>
	 * If the class is directly on the file system (e.g.,
	 * "/path/to/my/package/MyClass.class") then it will return the base directory
	 * (e.g., "file:/path/to").
	 * </p>
	 * <p>
	 * If the class is within a JAR file (e.g.,
	 * "/path/to/my-jar.jar!/my/package/MyClass.class") then it will return the
	 * path to the JAR (e.g., "file:/path/to/my-jar.jar").
	 * </p>
	 *
	 * @param c The class whose location is desired.
	 * @see FileUtils#urlToFile(URL) to convert the result to a {@link File}.
	 */
	private static URL getLocation(final Class<?> c) {
	    if (c == null) return null; // could not load the class

	    // try the easy way first
	    try {
	        final URL codeSourceLocation =
	            c.getProtectionDomain().getCodeSource().getLocation();
	        if (codeSourceLocation != null) return codeSourceLocation;
	    }
	    catch (final SecurityException | NullPointerException e) {
	    	e.printStackTrace();
	    }

	    // NB: The easy way failed, so we try the hard way. We ask for the class
	    // itself as a resource, then strip the class's path from the URL string,
	    // leaving the base path.

	    // get the class's raw resource path
	    final URL classResource = c.getResource(c.getSimpleName() + ".class");
	    if (classResource == null) return null; // cannot find class resource

	    final String url = classResource.toString();
	    final String suffix = c.getCanonicalName().replace('.', '/') + ".class";
	    if (!url.endsWith(suffix)) return null; // weird URL

	    // strip the class's path from the URL string
	    final String base = url.substring(0, url.length() - suffix.length());

	    String path = base;

	    // remove the "jar:" prefix and "!/" suffix, if present
	    if (path.startsWith("jar:")) path = path.substring(4, path.length() - 2);

	    try {
	        return new URL(path);
	    }
	    catch (final MalformedURLException e) {
	    	e.printStackTrace();
	        return null;
	    }
	} 

	/**
	 * Converts the given {@link URL} to its corresponding {@link File}.
	 * <p>
	 * This method is similar to calling {@code new File(url.toURI())} except that
	 * it also handles "jar:file:" URLs, returning the path to the JAR file.
	 * </p>
	 * 
	 * @param url The URL to convert.
	 * @return A file path suitable for use with e.g. {@link FileInputStream}
	 * @throws IllegalArgumentException if the URL does not correspond to a file.
	 */
	private static File urlToFile(final URL url) {
	    return url == null ? null : urlToFile(url.toString());
	}

	private static final Pattern FILEURLPATTERN = Pattern.compile("file:[A-Za-z]:.*");
	/**
	 * Converts the given URL string to its corresponding {@link File}.
	 * 
	 * @param url The URL to convert.
	 * @return A file path suitable for use with e.g. {@link FileInputStream}
	 * @throws IllegalArgumentException if the URL does not correspond to a file.
	 */
	private static File urlToFile(final String url) {
	    String path = url;
	    if (path.startsWith("jar:")) {
	        // remove "jar:" prefix and "!/" suffix
	        final int index = path.indexOf("!/");
	        path = path.substring(4, index);
	    }
	    try {
	        if (System.getProperty("os.name").startsWith("Windows") && FILEURLPATTERN.matcher(path).matches()) {
	            path = "file:/" + path.substring(5);
	        }
	        return new File(new URL(path).toURI());
	    }
	    catch (final MalformedURLException | URISyntaxException e) {
	    	e.printStackTrace();
	    }
	    if (path.startsWith("file:")) {
	        // pass through the URL as-is, minus "file:" prefix
	        path = path.substring(5);
	        return new File(path);
	    }
	    return null;
	}
}
