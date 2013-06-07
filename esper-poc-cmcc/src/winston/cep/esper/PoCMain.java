package winston.cep.esper;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceDestroyedException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.deploy.DeploymentException;
import com.espertech.esper.client.deploy.DeploymentOptions;
import com.espertech.esper.client.deploy.EPDeploymentAdmin;
import com.espertech.esper.client.deploy.Module;
import com.espertech.esper.client.deploy.ParseException;
import com.espertech.esperio.AdapterInputSource;
import com.espertech.esperio.csv.CSVInputAdapter;
import com.espertech.esperio.csv.CSVInputAdapterSpec;

public class PoCMain implements Runnable {
	private static final Log log = LogFactory.getLog(PoCMain.class);

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		 
		SimpleLayout layout = new SimpleLayout();
		ConsoleAppender appender = new ConsoleAppender(new SimpleLayout());
		Logger.getRootLogger().addAppender(appender);
		Logger.getRootLogger().setLevel((Level) Level.WARN);

		new PoCMain().run();
		
		long stopTime = System.currentTimeMillis();
	    long elapsedTime = stopTime - startTime;
	    System.out.println("Total time elapsed (s): " + elapsedTime/1000);
	}

	public PoCMain() {
	}

	public void run() {

		Configuration configuration = new Configuration();
		configuration.addEventType(TourEvent.class);
		configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
		
		configuration.getEngineDefaults().getThreading().setThreadPoolInbound(true);
		configuration.getEngineDefaults().getThreading().setThreadPoolInboundNumThreads(1);
		configuration.getEngineDefaults().getThreading().setThreadPoolOutbound(true);
		configuration.getEngineDefaults().getThreading().setThreadPoolOutboundNumThreads(1);
//		configuration.getEngineDefaults().getThreading().setThreadPoolTimerExec(true);
//		configuration.getEngineDefaults().getThreading().setThreadPoolTimerExecNumThreads(8);
//		configuration.getEngineDefaults().getThreading().setThreadPoolRouteExec(true);
//		configuration.getEngineDefaults().getThreading().setThreadPoolRouteExecNumThreads(8);

		EPServiceProvider epService = EPServiceProviderManager.getDefaultProvider(configuration);
		epService.initialize();

		try {
			EPDeploymentAdmin deployAdmin = epService.getEPAdministrator().getDeploymentAdmin();
			Module module = deployAdmin.read("module.epl");
			
			DeploymentOptions options = new DeploymentOptions();
			//options.setIsolatedServiceProvider("validation");	// we isolate any statements 
			//options.setValidateOnly(true);	// validate leaving no started statements
			//options.setFailFast(false); // do not fail on first error
			
			deployAdmin.deploy(module, options);
		} catch (EPServiceDestroyedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeploymentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		EPStatement cepStatement = epService.getEPAdministrator().getStatement("totaltouristsnodup");
		
		cepStatement.addListener(new MyEventListener());

		CSVInputAdapterSpec spec = new CSVInputAdapterSpec(new AdapterInputSource(new File("C:/Users/Winston_Oracle/Desktop/files/data.csv")), "TourEvent");
		spec.setEventsPerSec(1000);
		spec.setLooping(false);
		String[] propertyOrder = {"imsi", "signalingTime", "lac", "cell", "mytime"};
				
		spec.setPropertyOrder(propertyOrder);

		spec.setTimestampColumn("mytime");
		spec.setUsingExternalTimer(true);
		
		(new CSVInputAdapter(epService, spec)).start();
	}
}