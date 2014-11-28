package server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.storage.endpoint.rmi_server.DefaultQueryService;

public class DbConnectedServer {

	private static DefaultQueryService queryService = new DefaultQueryService();

	static Logger logger = LoggerFactory.getLogger(DbConnectedServer.class);

	public static void configureLogging() {
		ConsoleAppender consoleAppender = new ConsoleAppender();
		String pattern = "%d{yyyy-MM-dd HH:mm:ss} %-5p - %m%n";
		consoleAppender.setLayout(new PatternLayout(pattern));
		consoleAppender.setThreshold(Level.DEBUG);

		consoleAppender.activateOptions();
		org.apache.log4j.Logger.getRootLogger().addAppender(consoleAppender);
	}

	public static void main(String args[]) {

		configureLogging();

		acquireLocalHostIPInfo();

		try {
			queryService.launch();
		} catch (RemoteException e) {
			logger.error("Launching the query service failed");
			e.printStackTrace();
			System.exit(-1);
		}

		// register a shutdown hook if the program is canceled.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				queryService.terminate();
				logger.info("Query Server terminated");
			}
		});

		// lay down and await SIGTERM
		while (true) {
			try {
				synchronized (DbConnectedServer.class) {
					DbConnectedServer.class.wait();
				}
			} catch (InterruptedException e) {
				logger.error("DbConnectedServer was interrupted.");
				e.printStackTrace();
			}
		}
	}

	private static void acquireLocalHostIPInfo() {
		String hostname = "?";
		try {
			hostname = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logger.error("Could not get the localhost IP");
			e.printStackTrace();
			return;
		}
		logger.info("The host IP is {}", hostname);
	}
}
