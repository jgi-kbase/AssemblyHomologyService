package us.kbase.assemblyhomology.service;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;


public class AppEventListener implements ServletContextListener {
	
	@Override
	public void contextInitialized(final ServletContextEvent arg0) {
		// do nothing for now
	}
	
	@Override
	public void contextDestroyed(final ServletContextEvent arg0) {
		//TODO TEST manually test this shuts down the mongo connection.
		//this seems very wrong, but for now I'm not sure how else to do it.
		AssemblyHomologyService.shutdown();
	}
}