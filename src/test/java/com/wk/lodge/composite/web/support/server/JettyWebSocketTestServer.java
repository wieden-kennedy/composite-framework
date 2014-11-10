package com.wk.lodge.composite.web.support.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Jetty based {@link WebSocketTestServer}.
 *
 * @author Rossen Stoyanchev
 */
public class JettyWebSocketTestServer implements WebSocketTestServer {

	private final Server jettyServer;

	private final int port;


	public JettyWebSocketTestServer(int port) {
		this.port = port;
		this.jettyServer = new Server(this.port);
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public void deployConfig(WebApplicationContext cxt) {
		ServletContextHandler contextHandler = new ServletContextHandler();
		ServletHolder servletHolder = new ServletHolder(new DispatcherServlet(cxt));
		contextHandler.addServlet(servletHolder, "/");
		this.jettyServer.setHandler(contextHandler);
	}

	@Override
	public void undeployConfig() {
		// Stopping jetty will undeploy the servlet
	}

	@Override
	public void start() throws Exception {
		this.jettyServer.start();
	}

	@Override
	public void stop() throws Exception {
		if (this.jettyServer.isRunning()) {
			this.jettyServer.stop();
		}
	}

}
