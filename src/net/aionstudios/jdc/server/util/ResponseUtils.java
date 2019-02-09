package net.aionstudios.jdc.server.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import net.aionstudios.jdc.content.Cookie;
import net.aionstudios.jdc.content.RequestVariables;
import net.aionstudios.jdc.content.ResponseCode;
import net.aionstudios.jdc.server.compression.GZIPCompressor;
import net.aionstudios.jdc.server.content.GeneratorResponse;
import net.aionstudios.jdc.server.content.Website;

public class ResponseUtils {
	
	/**
	 * Generates a response to the client.
	 * 
	 * @param he The HTTPExchange on which to respond.
	 * @param httpResponseCode The HTTP response code.
	 * @param response The response (likely a serialized {@link JSONObject}).
	 * @return True if the response was sent successfully, false otherwise.
	 */
	public static boolean generateHTTPResponse(GeneratorResponse gResponse, HttpExchange he, RequestVariables vars, File page, Website w, boolean acceptGzip) {
		String response = gResponse.getResponse();
		ResponseCode rc = gResponse.getResponseCode();
		String redirect = vars!=null ? vars.getRedirect() : null;
		if(!(rc.getCode() >= 100)) {
			rc = ResponseCode.OK;
		}
		if(rc.getCode()>=400) {
			try {
				Headers respHeaders = he.getResponseHeaders();
				respHeaders.set("Content-Type", vars.getContentType());
				respHeaders.set("Content-Encoding", acceptGzip ? "gzip" : "deflate");
				String errorResp = w.getErrorContent(rc, he, vars);
				byte[] errRBytes = errorResp.getBytes(StandardCharsets.UTF_8);
				he.sendResponseHeaders(rc.getCode(), errRBytes.length);
				OutputStream os = he.getResponseBody();
				os.write(errRBytes);
				os.close();
				return true;
			} catch (IOException e) {
				return false;
			}
		}
		try {
			if(response!=null&&!response.isEmpty()) {
				Headers respHeaders = he.getResponseHeaders();
				if(vars!=null) {
					for(Cookie c : vars.getCookieManager().getNewCookies()) {
						respHeaders.add("Set-Cookie", c.makeSetterString());
					}
				}
				if(redirect!=null) {
					rc = ResponseCode.FOUND_REDIRECT;
					respHeaders.set("Location", vars.getRedirect());
				}
				respHeaders.set("Content-Type", vars.getContentType());
				respHeaders.set("Content-Encoding", acceptGzip ? "gzip" : "deflate");
				respHeaders.set("Last-Modified", FormatUtils.getLastModifiedAsHTTPString(System.currentTimeMillis()));
				byte[] respBytes = acceptGzip ? GZIPCompressor.compress(response) : response.getBytes(StandardCharsets.UTF_8);
				he.sendResponseHeaders(rc.getCode(), respBytes.length);
				OutputStream os = he.getResponseBody();
				os.write(respBytes);
				os.close();
				return true;
			} else {
				generateHTTPResponse(new GeneratorResponse("", ResponseCode.NO_CONTENT), he, vars, page, w, acceptGzip);
			}
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Responds with a file over HTTP.
	 * 
	 * @param rc		The HTTP {@link ResponseCode}.
	 * @param he		The {@link HttpExchange} handling this request.
	 * @param vars		The {@link RequestVariables} containing information about the request and response.
	 * @param file		The file to be streamed in response to the user.
	 * @param w			The {@link Website} on which the request was made.
	 * @return			True if the file was successfully transferred, false otherwise.
	 */
	public static boolean fileHTTPResponse(ResponseCode rc, HttpExchange he, RequestVariables vars, File file, Website w, boolean acceptGzip) {
		try {
			if (!file.isFile()) {
				generateHTTPResponse(new GeneratorResponse("", ResponseCode.NOT_FOUND), he, vars, file, w, acceptGzip);
	        } else {
	              // Object exists and is a file: accept with response code 200.
	              String mime = "";
	              if(file.getCanonicalPath().endsWith(".html")) mime = "text/html";
	              if(file.getCanonicalPath().endsWith(".htm")) mime = "text/html";
	              if(file.getCanonicalPath().endsWith(".jdc")) mime = "text/html";
	              if(file.getCanonicalPath().endsWith(".js")) mime = "application/javascript";
	              if(file.getCanonicalPath().endsWith(".css")) mime = "text/css";
	              if(file.getCanonicalPath().endsWith(".svg")) mime = "image/svg+xml";

	              Headers h = he.getResponseHeaders();
	              if(mime.length()>0) {
	            	  h.set("Content-Type", mime);
	              }
	              h.set("Content-Encoding", acceptGzip ? "gzip" : "deflate");
	              h.set("Last-Modified", FormatUtils.getLastModifiedAsHTTPString(file.lastModified()));
	              
	              Calendar date = Calendar.getInstance();
	              date.setTime(new Date());
	              date.add(Calendar.YEAR,1);
	              h.set("Expires", FormatUtils.getLastModifiedAsHTTPString(date.getTime()));
	              
	              he.sendResponseHeaders(200, 0);              
	              
	              if(acceptGzip) {
	            	  final byte[] buffer = new byte[1024];
	            	  FileInputStream fs = new FileInputStream(file);
	            	  GZIPOutputStream os = new GZIPOutputStream(he.getResponseBody());
		              int count;
		              while ((count = fs.read(buffer)) > 0) {
		                os.write(buffer,0,count);
		              }
		              fs.close();
		              os.finish();
		              os.close();
	              } else {
	            	  OutputStream os = he.getResponseBody();
		              FileInputStream fs = new FileInputStream(file);
		              final byte[] buffer = new byte[0x10000];
		              int count = 0;
		              while ((count = fs.read(buffer)) >= 0) {
		                os.write(buffer,0,count);
		              }
		              fs.close();
		              os.close();
	              }
	              return true;
	            }  
		} catch (IOException e) {
			return false;
		}
		return true;
	}

}
