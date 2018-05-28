package de.witcom.bpm.example.leaveapproval;

import java.util.logging.Logger;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.ObjectValue;

public class GetUserProfileByUserID implements JavaDelegate {

	private final Logger LOGGER = Logger.getLogger(this.getClass().getName());
	
	@Override
	public void execute(DelegateExecution execution) throws Exception {
		String userId = (String) execution.getVariableLocal("userId");
		User user = execution.getProcessEngineServices().getIdentityService().createUserQuery().userId(userId).singleResult();
		if (user == null){
			LOGGER.severe("User "+userId+" not found");
		}
		ObjectValue typedCustomerValue =
				  Variables.objectValue(user).serializationDataFormat("application/json").create();
		LOGGER.info(typedCustomerValue.toString());
		execution.setVariableLocal("userProfile", typedCustomerValue);
	}

}
