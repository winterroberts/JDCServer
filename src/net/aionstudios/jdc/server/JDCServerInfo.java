package net.aionstudios.jdc.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.aionstudios.jdc.database.DatabaseConnector;
import net.aionstudios.jdc.server.compression.BrotliCompressor;
import net.aionstudios.jdc.server.compression.CompressionEncoding;
import net.aionstudios.jdc.server.content.DependencyLoader;
import net.aionstudios.jdc.server.content.JDCLoader;
import net.aionstudios.jdc.server.content.Website;
import net.aionstudios.jdc.server.content.WebsiteManager;
import net.aionstudios.jdc.server.util.FormatUtils;
import net.aionstudios.jdc.server.util.LinkedJSONObject;

/**
 * Handles configuration files and application setting accessibility.
 * @author Winter Roberts
 */
public class JDCServerInfo {

	public static final String JDCS_VER = "1.1.0";
	public static final String JDCS_CONFIG = "./sites.json";
	public static final String CONFIG_DB = "./database.json";
	public static final String CONFIG_SERVER = "./config.json";
	
	private static JSONObject webconfig;
	private static JSONObject dbConfig;
	private static JSONObject serverConfig;
	
	private static int httpPort = 80;
	private static int httpsPort = 443;
	private static boolean enableBrotli = false;
	
	/**
	 * Reads configurable information when the server starts and handles setup if necessary.
	 * Should a config file not exist it will be created and the application terminated.
	 * @return True if the config was available and processed, false otherwise.
	 */
	public static boolean readConfigsAtStart() {
		webconfig = FormatUtils.getLinkedJsonObject();
		dbConfig = FormatUtils.getLinkedJsonObject();
		serverConfig = FormatUtils.getLinkedJsonObject();
		try {
			File dbcf = new File(JDCS_CONFIG);
			if(!dbcf.exists()) {
				dbcf.getParentFile().mkdirs();
				dbcf.createNewFile();
				JSONArray ws = new JSONArray();
				webconfig.put("websites", ws);
				writeConfig(webconfig, dbcf);
			} else {
				webconfig = readConfig(dbcf);
			}
			File dcf = new File(CONFIG_DB);
			if(!dcf.exists()) {
				dcf.getParentFile().mkdirs();
				dcf.createNewFile();
				dbConfig.put("hostname", "127.0.0.1");
				dbConfig.put("database", "db");
				dbConfig.put("username", "root");
				dbConfig.put("password", "password");
				dbConfig.put("port", 0);
				dbConfig.put("autoReconnect", true);
				dbConfig.put("timezone", "UTC");
				dbConfig.put("enabled", false);
				writeConfig(dbConfig, dcf);
			} else {
				dbConfig = readConfig(dcf);
			}
			File scf = new File(CONFIG_SERVER);
			if(!scf.exists()) {
				scf.getParentFile().mkdirs();
				scf.createNewFile();
				JSONObject fo = FormatUtils.getLinkedJsonObject();
				fo.put("brotli", true);
				serverConfig.put("enable_features", fo);
				serverConfig.put("http_port", 80);
				serverConfig.put("https_port", 443);
				writeConfig(serverConfig, scf);
			} else {
				serverConfig = readConfig(scf);
			}
			if(dbConfig.getBoolean("enabled")) {
				String hostname = dbConfig.getString("hostname");
				String database = dbConfig.getString("database");
				String username = dbConfig.getString("username");
				String password = dbConfig.getString("password");
				int port = dbConfig.getInt("port");
				boolean autoReconnect = dbConfig.has("autoReconnect")?dbConfig.getBoolean("autoReconnect"):true;
				String timezone = dbConfig.has("timezone")?dbConfig.getString("timezone"):"UTC";
				if(!Arrays.asList(TimeZone.getAvailableIDs()).contains(timezone)) {
					System.err.println("Failed connecting to database! No such timezone as '"+timezone+"' in config file!");
				} else {
					if(port > 0 && port < 65536) {
						DatabaseConnector.setupDatabase(hostname, database, Integer.toString(port), username, password, autoReconnect, timezone);
					} else {
						DatabaseConnector.setupDatabase(hostname, database, username, password, autoReconnect, timezone);
					}
				}
			}
			JSONArray sa = webconfig.getJSONArray("websites");
			for(int i = 0; i < sa.length(); i++) {
				JSONObject so = sa.getJSONObject(i);
				String name = so.getString("name");
				boolean sslOn = so.getBoolean("ssl_enabled");
				JSONArray addra = so.getJSONArray("addresses");
				String[] addresses = new String[addra.length()];
				for(int j = 0; j < addra.length(); j++) {
					addresses[j] = addra.getString(j);
				}
				new Website(name, addresses, sslOn);
			}
			httpPort = serverConfig.getInt("http_port");
			httpsPort = serverConfig.getInt("https_port");
			JSONObject fo = serverConfig.getJSONObject("enable_features");
			enableBrotli = fo.getBoolean("brotli");
			DependencyLoader.loadDependencies();
			JDCLoader.initializeClassLoader();
			WebsiteManager.connectContentProcessors();
			return true;
		} catch (IOException e) {
			System.err.println("Encountered an IOException during config file operations!");
			e.printStackTrace();
			return false;
		} catch (JSONException e) {
			System.err.println("Encountered an JSONException during config file operations!");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Writes the provided {@link JSONObject} to the file system, optimistically as a configuration file.
	 * @param j	The {@link JSONObject} to be serialized into the file system.
	 * @param f	The {@link File} object identifying where the {@link JSONObject} should be saved onto the file system.
	 * @return True if the file was written without error, false otherwise.
	 */
	public static boolean writeConfig(JSONObject j, File f) {
		try {
			if(!f.exists()) {
				f.getParentFile().mkdirs();
				f.createNewFile();
				System.out.println("Created config file '"+f.toString()+"'!");
			}
			PrintWriter writer;
			File temp = File.createTempFile("temp_json", null, f.getParentFile());
			writer = new PrintWriter(temp.toString(), "UTF-8");
			writer.println(j.toString(2));
			writer.close();
			Files.deleteIfExists(f.toPath());
			temp.renameTo(f);
			return true;
		} catch (IOException e) {
			System.err.println("Encountered an IOException while writing config: '"+f.toString()+"'!");
			e.printStackTrace();
			return false;
		} catch (JSONException e) {
			System.err.println("Encountered a JSONException while writing config: '"+f.toString()+"'!");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Deserializes a {@link JSONObject} from a file on the file system and returns it.
	 * @param f	The {@link File} object, representing a file containing JSON data on the file system.
	 * @return	A {@link JSONObject} representing the file provided or null if it could not be read.
	 */
	public static JSONObject readConfig(File f, boolean linked) {
		if(!f.exists()) {
			System.err.println("Failed reading config: '"+f.toString()+"'. No such file!");
			return null;
		}
		String jsonString = "";
		try (BufferedReader br = new BufferedReader(new FileReader(f.toString()))) {
		    for (String line; (line = br.readLine()) != null;) {
		    	jsonString += line;
		    }
		    br.close();
		    return linked?new LinkedJSONObject(jsonString):new JSONObject(jsonString);
		} catch (IOException e) {
			System.err.println("Encountered an IOException while reading config: '"+f.toString()+"'!");
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			System.err.println("Encountered a JSONException while reading config: '"+f.toString()+"'!");
			e.printStackTrace();
			return null;
		}
	}
	
	public static JSONObject readConfig(File f) {
		return readConfig(f, false);
	}
	
	/**
	 * Returns the content of the named {@link File} in the file system.
	 * @param f The {@link File} to be read from.
	 * @return The contents, a String, of the named {@link File}
	 */
	public static String readFile(File f) {
		if(!f.exists()) {
			System.err.println("Failed reading file: '"+f.toString()+"'. No such file!");
			return null;
		}
		String jsonString = "";
		try (BufferedReader br = new BufferedReader(new FileReader(f.toString()))) {
		    for (String line; (line = br.readLine()) != null;) {
		    	jsonString += line;
		    }
		    br.close();
		    return jsonString;
		} catch (IOException e) {
			System.err.println("Encountered an IOException while reading file: '"+f.toString()+"'!");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Most useful for users building to Java 7 as the Brotli library does
	 * not have support for it.
	 * @return True if brotli is enabled in the settings, false otherwise.
	 */
	public static boolean isEnableBrotli() {
		return enableBrotli;
	}

	/**
	 * Enables or disables {@link BrotliCompressor} as an available {@link CompressionEncoding} for
	 * the application.
	 * @param enableBrotli True to enabled, or false to disable.
	 */
	public static void setEnableBrotli(boolean enableBrotli) {
		JDCServerInfo.enableBrotli = enableBrotli;
	}

	/**
	 * @return The port which should be used for HTTP requests.
	 */
	public static int getHttpPort() {
		return httpPort;
	}

	/**
	 * @return The port which should be used for HTTPS requests.
	 */
	public static int getHttpsPort() {
		return httpsPort;
	}
	
}
