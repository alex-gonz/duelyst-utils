package sdk.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.commons.lang.SystemUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketListener;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

public class ChromeUtil {
	private static final int DEFAULT_PORT = 9222;
	
	private static final String CHROME_WIN_REG_LOC = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\Google Chrome";
	private static final String CHROME_LINUX_PATH = "/usr/bin/google-chrome";
	private static final String CHROMIUM_LINUX_PATH = "/usr/bin/chromium-browser";
	
	private static int msgId = 0;
	public static final Map<Integer, String> callbacks = new HashMap<Integer, String>();
	
	public static Process launchDebug(String chromePath, String url) throws IOException {
		return new ProcessBuilder(chromePath, url, "--remote-debugging-port=" + DEFAULT_PORT, "--user-data-dir=remote-profile").start();
	}
	
	public static JsonArray getJsonResponse() throws IOException, ClientProtocolException {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpGet httpGet = new HttpGet("http://localhost:" + DEFAULT_PORT + "/json");
		CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
		
		try {
		    BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
		    return Json.createReader(reader).readArray();
		} finally {
		    httpResponse.close();
		    httpClient.close();
		}
	}
	
	public static List<ChromeWsUrl> getWsUrl(JsonArray array) {
		List<ChromeWsUrl> results = new ArrayList<ChromeWsUrl>();
		
		for(JsonValue value : array) {
			JsonObject object = (JsonObject)value;
			results.add(new ChromeWsUrl(object.getString("id"), object.getString("title"), object.getString("webSocketDebuggerUrl")));
		}
		
		return results;
	}
	
	public static WebSocket connectWebSocket(String url, WebSocketListener listener) throws IOException, WebSocketException {
		WebSocket webSocket = new WebSocketFactory().createSocket(url);
		webSocket.addListener(listener);
		webSocket.connect();
		
		return webSocket;
	}
	
	public static void enableWsConsole(WebSocket webSocket, boolean enable) {
		enableWsDomain(webSocket, "Console", enable);
	}
	
	public static void enableWsRuntime(WebSocket webSocket, boolean enable) {
		enableWsDomain(webSocket, "Runtime", enable);
	}
	
	private static void enableWsDomain(WebSocket webSocket, String domain, boolean enable) {
		JsonObject object = Json.createObjectBuilder()
	       	    .add("id", ++msgId)
	       	    .add("method", domain + (enable ? ".enable" : ".disable"))
	       	    .build();
		
		sendText(webSocket, msgId, "enableWs" + domain + ":" + enable, object.toString());
	}
	
	public static void getObjectProperties(WebSocket webSocket, String objectId) {
		JsonObject object = Json.createObjectBuilder()
	       	    .add("id", ++msgId)
	       	    .add("method", "Runtime.getProperties")
	       	    .add("params", Json.createObjectBuilder()
	       	    	.add("objectId", objectId))
	       	    .build();
		
		sendText(webSocket, msgId, "getObjectProperties:" + objectId, object.toString());
	}
	
	private static void sendText(WebSocket webSocket, int msgId, String description, String text) {
		callbacks.put(msgId, description);
		webSocket.sendText(text);
		System.out.println(description + " message sent (" + msgId + ")");
	}
	
	public static String getChromePath(String oldPath) {
		File chrome = new File(oldPath);
		if (chrome.exists()) {
			return chrome.getPath();
		}
		
		if (SystemUtils.IS_OS_WINDOWS) {
			if (Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, CHROME_WIN_REG_LOC)) {
				if (Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, CHROME_WIN_REG_LOC, "InstallLocation")) {
					return Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, CHROME_WIN_REG_LOC, "InstallLocation") + "\\chrome.exe";
				}
			}
		} else if (SystemUtils.IS_OS_LINUX) {
			chrome = new File(CHROME_LINUX_PATH);
			if (chrome.exists()) {
				return chrome.getPath();
			}
			
			chrome = new File(CHROMIUM_LINUX_PATH);
			if (chrome.exists()) {
				return chrome.getPath();
			}
		}
		
		return "";
	}
}
