package io.bankbridge;
import spark.Service;

import static spark.Spark.get;
import static spark.Spark.port;

public class MockRemotes {

	public static void main(String[] args) throws Exception {
		port(1234);
		startServer(null);
	}

	public static void startRBBRoute(Service service){
		if (null == service){
			get("/rbb", (request, response) -> "{\n" +
					"\"bic\":\"1234\",\n" +
					"\"countryCode\":\"GB\",\n" +
					"\"auth\":\"OAUTH\"\n" +
					"}");
		}else {
			service.get("/rbb", (request, response) -> "{\n" +
					"\"bic\":\"1234\",\n" +
					"\"countryCode\":\"GB\",\n" +
					"\"auth\":\"OAUTH\"\n" +
					"}");
		}
	}

	public static void startCSRoute(Service service){
		if (null == service){
			get("/cs", (request, response) -> "{\n" +
					"\"bic\":\"5678\",\n" +
					"\"countryCode\":\"CH\",\n" +
					"\"auth\":\"OpenID\"\n" +
					"}");
		}else {
			service.get("/cs", (request, response) -> "{\n" +
					"\"bic\":\"5678\",\n" +
					"\"countryCode\":\"CH\",\n" +
					"\"auth\":\"OpenID\"\n" +
					"}");
		}
	}

	public static void startBESRoute(Service service){
		if (null == service){
			get("/bes", (request, response) -> "{\n" +
					"\"name\":\"Banco de espiritu santo\",\n" +
					"\"countryCode\":\"PT\",\n" +
					"\"auth\":\"SSL\"\n" +
					"}");
		}else {
			service.get("/bes", (request, response) -> "{\n" +
					"\"name\":\"Banco de espiritu santo\",\n" +
					"\"countryCode\":\"PT\",\n" +
					"\"auth\":\"SSL\"\n" +
					"}");
		}
	}

	public static void startServer(Service service){
		startRBBRoute(service);
		startCSRoute(service);
		startBESRoute(service);
	}
}