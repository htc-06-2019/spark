package fr.htc.aga.rest;

import static fr.htc.aga.common.Constants.API_REST_ID;
import static fr.htc.aga.common.Constants.API_REST_KEY;
import static fr.htc.aga.common.Constants.API_URL;
import static fr.htc.aga.common.Constants.CHARSET_ENCODING;
import static fr.htc.aga.common.Constants.FLIGHTS_SERVICE_PATH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 Class used to fetch flying data from a Rest API
 */
public class FlightInfoFetcher {

	private String appId;
	private String appKey;
	private HttpClient httpClient;
	private HttpGet preparedHttpRequest;

	/**
	 * les deux paramètres d'authontification fournits après inscription pour se
	 * connecter l'api rest
	 * 
	 * @param appId
	 * @param apiKey 
	 *  etablir un lien avec l'API rest en utilisant un Http getter
	 * 
	 */
	public FlightInfoFetcher(String appId, String apiKey) {
		this.appId = appId;
		this.appKey = apiKey;
		this.httpClient = HttpClients.createDefault();
		preparedHttpRequest = new HttpGet(API_URL);
		preparedHttpRequest.addHeader("ResourceVersion", "v4");
		preparedHttpRequest.addHeader("app_id", this.appId);
		preparedHttpRequest.addHeader("app_key", this.appKey);
		preparedHttpRequest.addHeader("Accept", "application/json");

	}

	/**
	 * cette methode permet de recupérer tous les vols de la journée 
	 * sous format JSON 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getFlights() {
		try {
			// ici on crée une réponse http qui va etablir une liaison client en exécutant
			// les param definit dans preparedHttpRequest
			HttpResponse response = httpClient.execute(this.preparedHttpRequest);
			// ici on test si le status de la reponse est ok alors on recupere le body de la reponse http encodé en UTF-8  
			// et on crée un JSONParser et un JSONOject  
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String responseBody = EntityUtils.toString(response.getEntity(), CHARSET_ENCODING);

				JSONParser parser = new JSONParser();
				JSONObject jsonObject = null;
				try {
					jsonObject = (JSONObject) parser.parse(responseBody);
				} catch (ParseException e) {
					return null;
				}
				// icei à partir du JSONObject on recupere les vols qui se trouvent sous le champs "flights"
				JSONArray flights = (JSONArray) jsonObject.get(FLIGHTS_SERVICE_PATH);
				List<String> flightsAsSrring = new ArrayList<String>();
				flights.forEach(x -> flightsAsSrring.add(x.toString()));
				return flightsAsSrring;
			} else {
				return null;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Test it
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		FlightInfoFetcher fInfoFetcher = new FlightInfoFetcher(API_REST_ID, API_REST_KEY);

		fInfoFetcher.getFlights().forEach(System.out::println);
	}
}
