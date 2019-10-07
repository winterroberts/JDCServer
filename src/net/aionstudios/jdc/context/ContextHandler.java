package net.aionstudios.jdc.context;

import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import net.aionstudios.jdc.content.OutgoingRequest;
import net.aionstudios.jdc.content.RequestVariables;
import net.aionstudios.jdc.content.ResponseCode;
import net.aionstudios.jdc.server.compression.CompressionEncoding;
import net.aionstudios.jdc.server.content.GeneratorResponse;
import net.aionstudios.jdc.server.content.PageParser;
import net.aionstudios.jdc.server.content.Website;
import net.aionstudios.jdc.server.content.WebsiteManager;
import net.aionstudios.jdc.server.util.RequestUtils;
import net.aionstudios.jdc.server.util.ResponseUtils;
import net.aionstudios.jdc.service.OutgoingRequestService;

/**
 * Handles standard HTTP Contexts.
 * @author Winter
 *
 */
public class ContextHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange he) {
		long nanoStart = System.nanoTime();
		/*TODO
		 * extend information stored about websites in the website file
		 * to include the url(s) at which a website should accept calls.
		 * 
		 * Direct server exchanges in this file in order to locate the
		 * desired website and execute JDC as necessary.
		 * 
		 * Cron, Call, Live
		 */
		String[] requestSplit;
		if(he.getRequestURI().toString().contains("?")) {
			requestSplit = he.getRequestURI().toString().split("\\?", 2);
		} else {
			requestSplit = new String[2];
			requestSplit[0] = he.getRequestURI().toString();
			requestSplit[1] = "";
		}
		String requestContext = "";
		if(requestSplit[0].endsWith("/")) {
			requestSplit[0] = requestSplit[0] + "index.jdc";
		}
		Map<String, String> getQuery = new HashMap<String, String>();
		if(requestSplit.length>1) {
			requestContext = requestSplit[0];
			getQuery = RequestUtils.resolveGetQuery(requestSplit[1]);
		}
		Map<String, String> postQuery = new HashMap<String, String>();
		if(he.getRequestMethod().equalsIgnoreCase("POST")) {
			postQuery = RequestUtils.resolvePostQuery(he);
		}
		Map<String, String> cookies = new HashMap<String, String>();
		cookies = RequestUtils.resolveCookies(he);
		String hostName = he.getRequestHeaders().getFirst("Host").split(":")[0];
		CompressionEncoding ce = CompressionEncoding.NONE;
		if(he.getRequestHeaders().containsKey("Accept-Encoding")) {
			String accept = he.getRequestHeaders().getFirst("Accept-Encoding");
			if(accept.contains("br")&&!accept.contains("br;q=0")&&!accept.contains("br; q=0")){
				ce = CompressionEncoding.BR;
			} else if(accept.contains("gzip")&&!accept.contains("gzip;q=0")&&!accept.contains("gzip; q=0")){
				ce = CompressionEncoding.GZIP;
			} else if (accept.contains("deflate")&&!accept.contains("deflate;q=0")&&!accept.contains("deflate; q=0")) {
				ce = CompressionEncoding.DEFLATE;
			}
		}
		Website wb = WebsiteManager.getWebsiteByAddress(hostName);
		String proxyUrl = wb.getProxyManager().getProxyUrl(requestSplit[0]);
		if(proxyUrl!=null) {
			OutgoingRequest or = new OutgoingRequest("", null);
			String rp = OutgoingRequestService.executePost(proxyUrl+ (requestSplit[1].length()>0 ? "?" : "") + requestSplit[1], OutgoingRequestService.postMapToString(postQuery), or);
			RequestVariables v = new RequestVariables(null, null, null, null);
			or.getLastHeader("Content-Type");
			v.setContentType(or.getLastHeader("Content-Type"));
			v.setRedirect(or.getLastHeader("Location"));
			ResponseUtils.generateHTTPResponse(new GeneratorResponse(or.getContent(), v.getResponseCode()), he, v, null, wb, ce);
			return;
		}
		RequestVariables vars = new RequestVariables(postQuery, getQuery, cookies, requestSplit[0]);
		if(requestSplit[0].endsWith(".jdc")) {
			ResponseUtils.generateHTTPResponse(PageParser.parseGeneratePage(wb, he, vars, wb.getContentFile(requestSplit[0])), he, vars, wb.getContentFile(requestSplit[0]), wb, ce);
			return;
		} else {
			ResponseUtils.fileHTTPResponse(ResponseCode.OK, he, vars, wb.getContentFile(requestSplit[0]), wb, ce);
			return;
		}
	}

}
