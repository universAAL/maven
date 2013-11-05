package ${package};

import org.universAAL.middleware.container.ModuleActivator;
import org.universAAL.middleware.container.ModuleContext;
import org.universAAL.middleware.container.utils.LogUtils;

public class ProjectActivator implements BundleActivator {

	static ModuleContext context;
	
	public void start(ModuleContext ctxt) throws Exception {	
		context = ctxt;
		LogUtils.logDebug(context, getClass(), "start", "Starting.");
		/*
		 * uAAL stuff
		 */
		
		LogUtils.logDebug(context, getClass(), "start", "Started.");
	}


	public void stop(ModuleContext ctxt) throws Exception {
		LogUtils.logDebug(context, getClass(), "start", "Stopping.");
		/*
		 * close uAAL stuff
		 */
		
		LogUtils.logDebug(context, getClass(), "start", "Stopped.");

	}

}
