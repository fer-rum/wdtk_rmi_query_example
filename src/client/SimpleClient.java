package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.wikidata.wdtk.storage.endpoint.rmi_client.WdtkRmiClient;
import org.wikidata.wdtk.storage.endpoint.shared.WdtkItemQueryResult;
import org.wikidata.wdtk.storage.endpoint.shared.WdtkQueryResult;
import org.wikidata.wdtk.storage.endpoint.shared.WdtkQueryState;

public class SimpleClient {

	// TODO graceful shutdown

	private static WdtkRmiClient queryClient = new WdtkRmiClient();

	public static void configureLogging() {
		ConsoleAppender consoleAppender = new ConsoleAppender();
		String pattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p - %m%n";
		consoleAppender.setLayout(new PatternLayout(pattern));
		consoleAppender.setThreshold(Level.DEBUG);

		consoleAppender.activateOptions();
		Logger.getRootLogger().addAppender(consoleAppender);
	}

	public static String readLine(String prompt) {
		System.out.print(">> " + prompt + ": ");
		return readLine();
	}

	public static String readLine() {
		String s = "";
		try {
			InputStreamReader converter = new InputStreamReader(System.in);
			BufferedReader in = new BufferedReader(converter);
			s = in.readLine();
		} catch (Exception e) {
			System.out.println("Error while reading input: " + e);
		}
		return s;
	}

	public static void main(String args[]) {

		configureLogging();

		String uri = readLine("Input the URI of the server endpoint");
		while (!queryClient.isConnected()) {
			queryClient.connect(uri);
		}
		
		while (true) {

			String query = readLine("Query item #");
			queryClient.queryForItem(Integer.parseInt(query));

			WdtkQueryState state = queryClient.getQueryUpdate();

			// wait for the processing to finish
			while (state == WdtkQueryState.PENDING
					|| state == WdtkQueryState.PROCESSING) {
				System.out.println("State is " + state);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				state = queryClient.getQueryUpdate();
			}

			// now we may have a result
			System.out.println(">> State: " + state);

			List<WdtkQueryResult> results = queryClient.getCurrentResult();

			if (results == null) {
				System.out.println("No results");
			} else {
				System.out.println(results.size() + " entries");
				for (WdtkQueryResult result : results) {

					WdtkItemQueryResult castedResult = (WdtkItemQueryResult) result;
					System.out.println(">> Result: "
							+ castedResult.getResultDocument().toString());
				}
			}
		}

		// okay, done
	}
}
