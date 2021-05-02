package com.quakearts.auth.server.totpqrenroller.client.http;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quakearts.auth.server.totpqrenroller.client.http.exception.ConnectorException;
import com.quakearts.auth.server.totpqrenroller.client.http.model.ActivationRequest;
import com.quakearts.auth.server.totpqrenroller.client.http.model.ErrorResponse;
import com.quakearts.auth.server.totpqrenroller.client.http.model.ProvisioningResponse;
import com.quakearts.rest.client.HttpClient;
import com.quakearts.rest.client.HttpResponse;
import com.quakearts.rest.client.HttpVerb;
import com.quakearts.rest.client.exception.HttpClientException;

@Singleton
public class TOTPHttpClient extends HttpClient {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2968088214812254664L;
	static final String APPLICATION_JSON = "application/json";
	@Inject
	private ObjectMapper objectMapper;
	@Inject
	private TOTPHttpClientBuilder totpHttpClientBuilder;
	protected String file;
	
	@PostConstruct
	public void init() {
		totpHttpClientBuilder.setHttpClient(this);
		totpHttpClientBuilder.setURLAs(System.getProperty("totp.server.url", "http://localhost:8080/totp"));
	}

	public ProvisioningResponse provision(String deviceid) throws IOException, HttpClientException {
		return getHttpResponseUsing(file+"/provisioning/{0}",
				"{}", APPLICATION_JSON , HttpVerb.POST,
				encode(deviceid)).thenCoerceTo(ProvisioningResponse.class);
	}
	
	public void activate(String deviceid, ActivationRequest request) 
			throws IOException, HttpClientException {
		getHttpResponseUsing(file+"/provisioning/{0}",
				request, APPLICATION_JSON , HttpVerb.PUT,
				encode(deviceid));
	}
		
	private HttpResponseBuilder getHttpResponseUsing(String template, Object requestValue, String contentType,
			HttpVerb verb, Object... parameters) throws IOException, HttpClientException {
		HttpResponse httpResponse = sendRequest(withTemplate(template, parameters), 
				requestValue != null ? stringify(requestValue) : null, verb, contentType);
		if (httpResponse.getHttpCode() > 299) {
			throw nonSuccessResponseUsing(httpResponse);
		}

		return new HttpResponseBuilder(httpResponse);
	}

	private String stringify(Object requestValue) throws JsonProcessingException {
		return requestValue instanceof String?(String)requestValue:objectMapper.writeValueAsString(requestValue);
	}

	private String withTemplate(String template, Object... parameters) {
		return parameters.length > 0 ? format(template, parameters) : template;
	}

	private String encode(String format) throws UnsupportedEncodingException {
		return format!=null? URLEncoder.encode(format, "UTF-8"):"";
	}

	private class HttpResponseBuilder {
		HttpResponse httpResponse;

		private HttpResponseBuilder(HttpResponse httpResponse) {
			this.httpResponse = httpResponse;
		}

		private <T> T thenCoerceTo(Class<T> targetClass) throws IOException {
			return objectMapper.readValue(httpResponse.getOutput(), targetClass);
		}
	}

	private ConnectorException nonSuccessResponseUsing(HttpResponse httpResponse) throws IOException {
		return new ConnectorException(objectMapper.readValue(httpResponse.getOutput(), ErrorResponse.class),
				httpResponse.getHttpCode());
	}
}
