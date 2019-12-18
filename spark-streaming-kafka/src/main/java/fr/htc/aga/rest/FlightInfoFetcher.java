package fr.htc.aga.rest;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author m.mellali
 *
 */
/*
 Class used to fetch flying data from a Rest API
 */
public class FlightInfoFetcher { // initialisation des objects
	final static String API_URL = "https://api.schiphol.nl/public-flights/flights";
	private String appId;
	private String appKey;
	private HttpClient httpClient;
	private HttpGet preparedHttpRequest;

	/**
	 * 
	 * @param appId
	 * @param apiKey
	 */
	 //constructor prend  cle , valeur )
	public FlightInfoFetcher(String appId, String apiKey) {
		this.appId = appId;
		this.appKey = apiKey;
		this.httpClient = HttpClients.createDefault();
		//header = ensembele cle valeur
		preparedHttpRequest = new HttpGet(API_URL); 	
		 // HttpRequest: la requete elle meme
		preparedHttpRequest.addHeader("ResourceVersion", "v4");
		preparedHttpRequest.addHeader("app_id", this.appId); // parametres
		preparedHttpRequest.addHeader("app_key", this.appKey); // les inclures dans le header de la requete
		preparedHttpRequest.addHeader("Accept", "application/json"); // recevoir de json

	}

	/**
	 * 
	 * @return
	 */
	public List<String> getFlights() {
		try {
			HttpResponse response = httpClient.execute(this.preparedHttpRequest); //executer la requete
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) { 
				// tester la requete : public static final int SC_OK = 200;
				String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8"); // qd Ok, je recupere responseBody
				 // creer un parseur pr parser ds un fichier json
				JSONParser parser = new JSONParser();
				JSONObject jsonObject = null; // initialer l object json
				try {
					// (JSONObject) :caster vers jsonobject
					jsonObject = (JSONObject) parser.parse(responseBody); 
				} catch (ParseException e) {
					return null;
				}
				// recuper les flights dans la liste de json flight car json est 
				//plus utile comme recuperer les element de la racine flight
				JSONArray flights = (JSONArray) jsonObject.get("flights");
				List<String> flightsAsSrring = new ArrayList(); 
				//convertir json vers string
				flights.forEach(jsonFlight -> flightsAsSrring.add(jsonFlight.toString())); 
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
	 * @param args
	 */
	public static void main(String[] args) {
		String appId = "ddf5a84d";
		String appKey = "cba9fc3b52ccc8e445ae7a01a8fc6157";

		FlightInfoFetcher fInfoFetcher = new FlightInfoFetcher(appId,appKey) ;
		
		fInfoFetcher.getFlights().forEach(System.out::println);
	}
}
