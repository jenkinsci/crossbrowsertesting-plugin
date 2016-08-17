package org.jenkinsci.plugins.cbt_jenkins;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;



public class Request {
	
	String username = null;
	String password = null;
	
	private String requestURL = "http://crossbrowsertesting.com/api/v3/";
	
	public Request(String path, String username, String password) {
		this.username = username;
		this.password = password;
		
		requestURL += path;
	}
	public Request(String path) {		
		requestURL += path;
	}	
	public Request() {}
	
	public String get(String urlStr) throws IOException{
		/*
		 * Get request
		 * returns JSON as a string
		 */
		URL url = new URL(requestURL + urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		if (username != null && password != null) {
			String userpassEncoding = Base64.encodeBase64String((username+":"+password).getBytes());
			conn.setRequestProperty("Authorization", "Basic " + userpassEncoding);
		}
		if (conn.getResponseCode() != 200) {
			throw new IOException(conn.getResponseMessage());
		}
		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();
		conn.disconnect();
		return sb.toString();
	}
	public String post(String urlStr, Map<String, String> params) throws IOException {
		/*
		 * POST request
		 * returns JSON as a string
		 */
		String urlParameters = "";
		int index = 1;
    	for (Map.Entry<String, String> entry : params.entrySet()) {
    		urlParameters += entry.getKey() +"=" + entry.getValue();
    		if (index < params.size()) {
    			urlParameters += "&";
    		}
    		index++;
    	}
		URL url = new URL(requestURL + urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		if (username != null && password != null) {
			String userpassEncoding = Base64.encodeBase64String((username+":"+password).getBytes());
			conn.setRequestProperty("Authorization", "Basic " + userpassEncoding);
		}
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
		if (conn.getResponseCode() != 200) {
			throw new IOException(conn.getResponseMessage());
		}
		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();
		conn.disconnect();
		return sb.toString();
	}
	public String delete(String urlStr) throws IOException {
		/*
		 * POST request
		 * returns JSON as a string
		 */
		String urlParameters = "";
		URL url = new URL(requestURL + urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("DELETE");
		if (username != null && password != null) {
			String userpassEncoding = Base64.encodeBase64String((username+":"+password).getBytes());
			conn.setRequestProperty("Authorization", "Basic " + userpassEncoding);
		}
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
		if (conn.getResponseCode() != 200) {
			throw new IOException(conn.getResponseMessage());
		}
		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();
		conn.disconnect();
		return sb.toString();
	}
}
