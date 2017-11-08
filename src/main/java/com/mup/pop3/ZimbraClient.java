package com.mup.pop3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ZimbraClient {
	private final static Logger log = LoggerFactory.getLogger(ZimbraClient.class);
	final int DEFAULT_TIMEOUT = 3 * 1000;
	final int DEFAULT_MAX_CONNECTION = 100;
	final int DEFAULT_MAX_ROUTE = 50;
	final int DEFAULT_SEARCH_LIMIT = 500; // MAX 999
	final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 Safari/537.36";
	CloseableHttpClient client = null;
	CookieStore cookieStore = null;

	int searchLimit = DEFAULT_SEARCH_LIMIT;
	String serviceUrl = null;
	private Cookie authTokenCookie = null;
	private String loginName = null;
	ObjectMapper mapper = null;

	public ZimbraClient() {
		client = HttpClients.createDefault();
		HttpClientBuilder builder = HttpClientBuilder.create();
		cookieStore = new BasicCookieStore();

		RequestConfig config = RequestConfig.custom().setConnectTimeout(DEFAULT_TIMEOUT)
				.setConnectionRequestTimeout(DEFAULT_TIMEOUT).setSocketTimeout(DEFAULT_TIMEOUT).build();

		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
		connManager.setMaxTotal(DEFAULT_MAX_CONNECTION);
		connManager.setDefaultMaxPerRoute(DEFAULT_MAX_ROUTE);

		builder.setDefaultCookieStore(cookieStore);
		builder.setRedirectStrategy(new LaxRedirectStrategy());
		builder.setDefaultRequestConfig(config);
		builder.setConnectionManager(connManager);
		builder.setUserAgent(DEFAULT_USER_AGENT);

		client = builder.build();
		
		mapper = new ObjectMapper();
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public void setSearchLimit(int searchLimit) {
		this.searchLimit = searchLimit;
	}

	public boolean logout() {
		final String uri = serviceUrl + "?loginOp=logout";
		HttpGet request = new HttpGet(uri);

		try {
			HttpResponse response = client.execute(request);
			log.debug("URL: {}, statusCode={}", uri, response.getStatusLine().getStatusCode());
			response.getEntity().getContent().close();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	public boolean login(String username, String password) {
		final String uri = serviceUrl;

		this.logout();

		HttpPost request = new HttpPost(uri);

		request.setHeader("Content-type", "application/x-www-form-urlencoded");
		request.setHeader("Referer", serviceUrl + "?loginOp=logout");

		List<NameValuePair> param = new ArrayList<NameValuePair>();
		param.add(new BasicNameValuePair("loginOp", "login"));
		param.add(new BasicNameValuePair("username", username));
		param.add(new BasicNameValuePair("password", password));
		try {
			request.setEntity(new UrlEncodedFormEntity(param));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return false;
		}

		try {
			HttpResponse response = client.execute(request);
			log.debug("URL: {}, statusCode={}", uri, response.getStatusLine().getStatusCode());

			if (response.getStatusLine().getStatusCode() != 200 && response.getStatusLine().getStatusCode() != 302) {
				log.error(EntityUtils.toString(response.getEntity()));
				return false;
			}
			response.getEntity().getContent().close();

			authTokenCookie = null;
			for (Cookie cookie : cookieStore.getCookies()) {
				if (cookie.getName().equals("ZM_AUTH_TOKEN")) {
					authTokenCookie = cookie;
				}
			}
			if (authTokenCookie == null) {
				return false;
			}
			loginName = username;
			log.debug("ZM_AUTH_TOKEN=" + authTokenCookie.getValue());

		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public List<String> getSearchRequest(String query) {
		final String uri = serviceUrl + "service/soap/SearchRequest";
		HttpPost request = new HttpPost(uri);
		List<String> result = new ArrayList<String>();

		try {
			String reqJson = this.getSearchRequestContent(query);

			request.setHeader("Content-Type", "application/json; charset=UTF-8");

			HttpClientContext ctx = new HttpClientContext();
			ctx.setCookieStore(new BasicCookieStore());

			StringEntity param = new StringEntity(reqJson);
			// param.setChunked(true);
			request.setEntity(param);

			HttpResponse response = client.execute(request, ctx);
			log.debug("URL: {}, statusCode={}, contentLength={}\n{}", uri, response.getStatusLine().getStatusCode(),
					request.getEntity().getContentLength(), reqJson);

			if (response.getStatusLine().getStatusCode() != 200) {
				log.error(EntityUtils.toString(response.getEntity()));
				return null;
			}

			InputStream content = response.getEntity().getContent();
			JsonNode jsonNode = mapper.readValue(content, JsonNode.class);
			log.debug(jsonNode.toString());

			JsonNode messageList = jsonNode.get("Body").get("SearchResponse").get("hit");
			for (JsonNode message : messageList) {
				result.add(message.get("id").asText());
				log.debug("message id: " + message.get("id").asText());
			}

			return result;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public InputStream getMessage(String id) {
		final String uri = serviceUrl + "service/home/~/?auth=co&view=text&id=" + id;
		HttpGet request = new HttpGet(uri);

		try {
			HttpResponse response = client.execute(request);
			log.debug("URL: {}, statusCode={}", uri, response.getStatusLine().getStatusCode());

			if (response.getStatusLine().getStatusCode() == 200) {
				return response.getEntity().getContent();
			}
			else {
				response.getEntity().getContent().close();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	private String getSearchRequestContent(String query) throws JsonProcessingException {
		Map<String, Object> jsonMain = new HashMap<String, Object>();
		
		Map<String, Object> jsonHeader = new HashMap<String, Object>();
		Map<String, Object> jsonContext = new HashMap<String, Object>();
		Map<String, Object> jsonFormat = new HashMap<String, Object>();
		Map<String, Object> jsonNosession = new HashMap<String, Object>();
		Map<String, Object> jsonAccount = new HashMap<String, Object>();
		Map<String, Object> jsonAuthToken = new HashMap<String, Object>();
		Map<String, Object> jsonBody = new HashMap<String, Object>();
		Map<String, Object> jsonSearchRequest = new HashMap<String, Object>();
		Map<String, Object> jsonQuery = new HashMap<String, Object>();

		jsonFormat.put("type", "js");

		jsonAccount.put("_content", loginName);
		jsonAccount.put("by", "name");

		jsonAuthToken.put("_content", authTokenCookie.getValue());

		jsonContext.put("_jsns", "urn:zimbra");
		jsonContext.put("format", jsonFormat);
		jsonContext.put("nosession", jsonNosession);
		jsonContext.put("account", jsonAccount);
		jsonContext.put("authToken", jsonAuthToken);

		jsonHeader.put("context", jsonContext);

		jsonQuery.put("_content", query);
		jsonSearchRequest.put("_jsns", "urn:zimbraMail");
		jsonSearchRequest.put("types", "message");
		jsonSearchRequest.put("limit", this.searchLimit);
		jsonSearchRequest.put("resultMode", "IDS");
		jsonSearchRequest.put("query", jsonQuery);

		jsonBody.put("SearchRequest", jsonSearchRequest);
		
		jsonMain.put("Header", jsonHeader);
		jsonMain.put("Body", jsonBody);
		
		log.debug(jsonMain.toString());
		String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMain);

		return json;
	}

	public void execute() throws Exception {
		login("91120617", "wlsWkdkagh#1");
		getSearchRequest("in:inbox");
		InputStream is = getMessage("20281");
		BufferedReader br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
		while(true) {
			String line = br.readLine();
			if( line == null ) {
				break;
			}
			log.debug(line);
		}
	}
}
