package com.dbdeploy;

import com.dbdeploy.exceptions.DbDeployException;
import com.dbdeploy.exceptions.UsageException;
import com.dbdeploy.scripts.ChangeScript;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Controller {

	final static Logger logger = Logger.getLogger(Controller.class);

	private final AvailableChangeScriptsProvider availableChangeScriptsProvider;
	private final AppliedChangesProvider appliedChangesProvider;
	private final ChangeScriptApplier changeScriptApplier;
	private final ChangeScriptApplier undoScriptApplier;

	private final PrettyPrinter prettyPrinter = new PrettyPrinter();

	public Controller(AvailableChangeScriptsProvider availableChangeScriptsProvider,
					  AppliedChangesProvider appliedChangesProvider,
					  ChangeScriptApplier changeScriptApplier, ChangeScriptApplier undoScriptApplier) {
		this.availableChangeScriptsProvider = availableChangeScriptsProvider;
		this.appliedChangesProvider = appliedChangesProvider;
		this.changeScriptApplier = changeScriptApplier;
		this.undoScriptApplier = undoScriptApplier;
	}

	public void processChangeScripts(Long lastChangeToApply) throws DbDeployException, IOException {
		if (lastChangeToApply != Long.MAX_VALUE) {
			logger.info("Only applying changes up and including change script #" + lastChangeToApply);
		}

		List<ChangeScript> scripts = availableChangeScriptsProvider.getAvailableChangeScripts();
		List<Long> applied = appliedChangesProvider.getAppliedChanges();
		List<ChangeScript> toApply = identifyChangesToApply(lastChangeToApply, scripts, applied);

		logStatus(scripts, applied, toApply);

        changeScriptApplier.apply(Collections.unmodifiableList(toApply));

        if (undoScriptApplier != null) {
			logger.info("Generating undo scripts...");
			Collections.reverse(toApply);
            undoScriptApplier.apply(Collections.unmodifiableList(toApply));
        }
	}

    private void logStatus(List<ChangeScript> scripts, List<Long> applied, List<ChangeScript> toApply) {
		logger.info("Changes currently applied to database:\n  " + prettyPrinter.format(applied));
		logger.info("Scripts available:\n  " + prettyPrinter.formatChangeScriptList(scripts));
		logger.info("To be applied:\n  " + prettyPrinter.formatChangeScriptList(toApply));
	}

	private List<ChangeScript> identifyChangesToApply(Long lastChangeToApply, List<ChangeScript> scripts, List<Long> applied) {
		List<ChangeScript> result = new ArrayList<ChangeScript>();

		for (ChangeScript script : scripts) {
			if (script.getId() > lastChangeToApply)
				break;

			if (!applied.contains(script.getId())) {
				if(!applied.isEmpty() && applied.get(applied.size() - 1) > script.getId()){
					UsageException.throwForSequentialOrdering(script.getId() + " is not less than " + applied.get(applied.size() - 1) + " changes must be applied in order.");
				}
				result.add(script);
			}
		}

		return result;
	}
}