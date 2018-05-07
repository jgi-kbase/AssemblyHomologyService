package us.kbase.test.assemblyhomology;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

public class StandaloneAuthServer {

	private Integer port = null;
	private Server server;
	
	// pass 0 for random port
	public void start(final int port) throws Exception {
		server = new Server(port);

		final ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		server.setHandler(context);

		final ServletHolder jerseyServlet = context.addServlet(ServletContainer.class, "/*");
		jerseyServlet.setInitOrder(1);
		jerseyServlet.setInitParameter("javax.ws.rs.Application",
				"us.kbase.assemblyhomology.service.AssemblyHomologyService");
		server.start();
		this.port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
		server.join();
	}
	
	public void stop() throws Exception {
		server.stop();
		port = null;
	}
	
	public Integer getPort() {
		return port;
	}
	
	public static class ServerThread extends Thread {
		private final StandaloneAuthServer server;
		
		public ServerThread(final StandaloneAuthServer server) {
			this.server = server;
		}
		
		public void run() {
			try {
				server.start(0);
			} catch (Exception e) {
				System.err.println("Can't start server:");
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		new StandaloneAuthServer().start(Integer.valueOf(args[0]));
	}
}
