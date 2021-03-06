package a1s.learn.guava;

import com.google.common.util.concurrent.*;
import com.google.common.util.concurrent.Service.State;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;
import org.junit.experimental.categories.Categories;

public class ServiceTest extends TestCase {
	
	public volatile int count = 0;
	
	public volatile boolean serviceManagerHealthy = false;
	public volatile boolean serviceManagerStopped = false;
	public volatile boolean serviceManagerFailure = false;
	
	public volatile boolean serviceStarting = false;
	public volatile boolean serviceRunning = false;
	public volatile boolean serviceStopping = false;
	public volatile boolean serviceTerminated = false;
	public volatile boolean serviceFailure = false;

	public boolean isServiceStarting() {
		return serviceStarting;
	}

	public void setServiceStarting(boolean serviceStarting) {
		this.serviceStarting = serviceStarting;
	}

	public boolean isServiceRunning() {
		return serviceRunning;
	}

	public void setServiceRunning(boolean serviceRunning) {
		this.serviceRunning = serviceRunning;
	}

	public boolean isServiceStopping() {
		return serviceStopping;
	}

	public void setServiceStopping(boolean serviceStopping) {
		this.serviceStopping = serviceStopping;
	}

	public boolean isServiceTerminated() {
		return serviceTerminated;
	}

	public void setServiceTerminated(boolean serviceTerminated) {
		this.serviceTerminated = serviceTerminated;
	}

	public boolean isServiceFailure() {
		return serviceFailure;
	}

	public void setServiceFailure(boolean serviceFailure) {
		this.serviceFailure = serviceFailure;
	}
	
	public boolean isServiceManagerHealthy() {
		return serviceManagerHealthy;
	}

	public void setServiceManagerHealthy(boolean serviceManagerHealthy) {
		this.serviceManagerHealthy = serviceManagerHealthy;
	}

	public boolean isServiceManagerStopped() {
		return serviceManagerStopped;
	}

	public void setServiceManagerStopped(boolean serviceManagerStopped) {
		this.serviceManagerStopped = serviceManagerStopped;
	}

	public boolean isServiceManagerFailure() {
		return serviceManagerFailure;
	}

	public void setServiceManagerFailure(boolean serviceManagerFailure) {
		this.serviceManagerFailure = serviceManagerFailure;
	}
	
	public void startUp() {
		this.serviceManagerFailure = false;
		this.serviceManagerHealthy = false;
		this.serviceManagerStopped = false;
		
		this.serviceFailure = false;
		this.serviceRunning = false;
		this.serviceStarting = false;
		this.serviceStopping = false;
		this.serviceTerminated = false;
	}
	
	public void testService(){
		MyService s1 = new MyService(this);
		
		State state = s1.startAndWait();
		
		assertEquals(State.RUNNING, state);
		
		State state2 = s1.stopAndWait();
		
		assertEquals(State.TERMINATED, state2);
	}
	
	public void testServiceListener(){
		MyService s1 = new MyService(this);
		
		s1.addListener(new ServiceListener(this), MoreExecutors.sameThreadExecutor());
		
		assertFalse(serviceFailure);
		assertFalse(serviceStarting);
		assertFalse(serviceRunning);
		assertFalse(serviceStopping);
		assertFalse(serviceTerminated);
		
		s1.start();
		
		assertFalse(serviceFailure);
		assertTrue(serviceStarting);
		assertFalse(serviceRunning);
		assertFalse(serviceStopping);
		assertFalse(serviceTerminated);
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException ex) {
			//
		}
		
		assertFalse(serviceFailure);
		assertTrue(serviceStarting);
		assertTrue(serviceRunning);
		assertFalse(serviceStopping);
		assertFalse(serviceTerminated);
		
		s1.stop();
				
		assertFalse(serviceFailure);
		assertTrue(serviceStarting);
		assertTrue(serviceRunning);
		assertTrue(serviceStopping);
		assertFalse(serviceTerminated);
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException ex) {
			//
		}
		
		assertFalse(serviceFailure);
		assertTrue(serviceStarting);
		assertTrue(serviceRunning);
		assertTrue(serviceStopping);
		assertTrue(serviceTerminated);
	}
	
	public void testFailedService(){
		MyService s1 = new MyService(this, true);
		
		s1.start();
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException ex) {
			//
		}
		
		assertEquals(State.FAILED, s1.state());
	}
	
	public void testSimpleSuccessServiceManager() {
		Set<Service> services = new HashSet<Service>();
		
		MyService s1 = new MyService(this);
		MyService s2 = new MyService(this);
		MyService s3 = new MyService(this);
		
		services.add(s1);
		services.add(s2);
		services.add(s3);
		
		ServiceManager sm = new ServiceManager(services);
		
		sm.startAsync().awaitHealthy();
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			
		}
		
		assertTrue(s1.isRunning());
		assertTrue(s2.isRunning());
		assertTrue(s3.isRunning());

		sm.stopAsync().awaitStopped();
		
		assertFalse(s1.isRunning());
		assertFalse(s2.isRunning());
		assertFalse(s3.isRunning());
	}
	
	public void testListenerServiceManager() {
		Set<Service> services = new HashSet<Service>();
		
		MyService s1 = new MyService(this);
		
		services.add(s1);
		
		ServiceManager sm = new ServiceManager(services);
		
		// register listener to service manager
		sm.addListener(new ServiceManagerListener(this), MoreExecutors.sameThreadExecutor());
		
		// check preconditions
		assertFalse(this.serviceManagerFailure);
		assertFalse(this.serviceManagerStopped);
		assertFalse(this.serviceManagerHealthy);
		
		// start services and wait all services to be started
		sm.startAsync().awaitHealthy();
		
		// wait to call listener
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
			
		}
		
		// check conditions
		assertFalse(this.serviceManagerFailure);
		assertFalse(this.serviceManagerStopped);
		assertTrue(this.serviceManagerHealthy);
		
		// simulate some work
		try {
			Thread.sleep(2000);
		} catch (InterruptedException ex) {
			
		}
			
		assertTrue(s1.isRunning());

		// stop all services and wait all to be stopped
		sm.stopAsync().awaitStopped();
		
		// wait to run listener
		try {
			Thread.sleep(1000);
		} catch (InterruptedException ex) {
			
		}
		
		// check conditions
		assertFalse(s1.isRunning());
		
		assertFalse(this.serviceManagerFailure);
		assertTrue(this.serviceManagerStopped);
		assertTrue(this.serviceManagerHealthy);
	}
	
	public void testFailedListenerServiceManager() {
		Set<Service> services = new HashSet<Service>();
		
		MyService s1 = new MyService(this, true);
		
		services.add(s1);
		
		ServiceManager sm = new ServiceManager(services);
		
		// register listener to service manager
		sm.addListener(new ServiceManagerListener(this), MoreExecutors.sameThreadExecutor());
		
		// check preconditions
		assertFalse(this.serviceManagerFailure);
		assertFalse(this.serviceManagerStopped);
		assertFalse(this.serviceManagerHealthy);
		
		// start services and wait all services to be started
		sm.startAsync();
		
		// wait to start and call listener
		try {
			Thread.sleep(3000);
		} catch (InterruptedException ex) {
			
		}
		
		// check conditions
		assertTrue(this.serviceManagerFailure);
		assertTrue(this.serviceManagerStopped);
		assertFalse(this.serviceManagerHealthy);
		assertFalse(s1.isRunning());
		assertEquals(Service.State.FAILED, s1.state()); 
	}
	
	private static class ServiceListener implements Service.Listener {
		private ServiceTest test;
		
		public ServiceListener(ServiceTest test) {
			this.test = test;
		}
		
		@Override
		public void starting() {
			test.setServiceStarting(true);
		}

		@Override
		public void running() {
			test.setServiceRunning(true);
		}

		@Override
		public void stopping(State from) {
			test.setServiceStopping(true);
		}

		@Override
		public void terminated(State from) {
			test.setServiceTerminated(true);
		}

		@Override
		public void failed(State from, Throwable failure) {
			test.setServiceFailure(true);
		}
		
	}
	
	private static class ServiceManagerListener implements ServiceManager.Listener{
		private ServiceTest test;
		
		public ServiceManagerListener(ServiceTest test) {
			this.test = test;
		}
		
		@Override
		public void healthy() {
			test.setServiceManagerHealthy(true);
		}

		@Override
		public void stopped() {
			test.setServiceManagerStopped(true);
		}

		@Override
		public void failure(Service service) {
			test.setServiceManagerFailure(true);
		}
	
	}
	
	private static class MyService extends AbstractExecutionThreadService {

		protected ServiceTest test;
		protected boolean failOnStartup = false;
		
		public MyService(ServiceTest test) {
			this.test = test;
		}
		
		public MyService(ServiceTest test, boolean failOnStartup) {
			this.test = test;
			this.failOnStartup = failOnStartup;
		}

		@Override
		protected void startUp() throws Exception {
			super.startUp();

			// some heavy work
			Thread.sleep(2000);
			
			if (this.failOnStartup) {
				throw new Exception("Starting up failed");
			}
		}

		@Override
		protected void shutDown() throws Exception {
			Thread.sleep(2000);
			
			super.shutDown();
		}
		
		@Override
		protected void run() throws Exception {
			while (this.isRunning()) {
				synchronized(test) {
					test.count++;
				}
			}
		}
		
	}
	
}
