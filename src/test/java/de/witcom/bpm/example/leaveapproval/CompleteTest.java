package de.witcom.bpm.example.leaveapproval;

import org.apache.ibatis.logging.LogFactory;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Test case starting an in-memory database-backed Process Engine.
 */
public class CompleteTest {

	private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

	@ClassRule
	@Rule
	public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

	private static User demoUser;
	
	private static final String PROCESS_DEFINITION_KEY = "leave-approval-process";

	static {
		LogFactory.useSlf4jLogging(); // MyBatis
	}

	@Before
	public void setup() {
		init(rule.getProcessEngine());
		demoUser =  rule.getIdentityService().newUser("carsten");
		demoUser.setEmail("c.buchberger@witcom.de");
		demoUser.setFirstName("Carsten");
	}

	/**
	 * Just tests if the process definition is deployable.
	 */
	@Test
	@Ignore
	@Deployment(resources = {"leave-approval.bpmn","manager_decision.dmn"})
	public void testParsingAndDeployment() {
		// nothing is done here, as we just want to check for exceptions during deployment
	}

	@Test
	@Deployment(resources = {"leave-approval-simple.bpmn","manager_decision.dmn"})
	public void testCompletePath() throws ParseException {

		SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		String substitute = "peter";

		ObjectValue typedCustomerValue =
				  Variables.objectValue(demoUser).serializationDataFormat("application/json").create();
		
		Map<String, Object> variables = new HashMap<>();

		
		variables.put("LEAVE_START_DATE",  dateformat.parse("2018-04-19T00:00:00"));
		variables.put("LEAVE_END_DATE",  dateformat.parse("2018-04-25T00:00:00"));
		variables.put("REASON_FOR_LEAVE",  "urlaub");
		variables.put("SUBSTITUTE",  substitute);
		variables.put("leaveRequester", demoUser.getId());
		variables.put("leaveRequesterObject", typedCustomerValue);
		 
		//rule.getRuntimeService().
		
		ProcessInstance processInstance = rule.getRuntimeService().startProcessInstanceByKey(PROCESS_DEFINITION_KEY,variables);
		TaskService taskService = rule.getTaskService();
		String instanceId =  processInstance.getProcessInstanceId();
		LOGGER.info("Started process-instance with ID " + processInstance.getProcessInstanceId());
		
		assertThat(processInstance).isStarted().task().hasDefinitionKey("confirmSubstitute").isAssignedTo(substitute);
		
		Task confirmSubstitute = taskService.createTaskQuery()
			      .taskDefinitionKey("confirmSubstitute")
			      .processInstanceId(processInstance.getId())
			      .singleResult();
		
		variables.put("substitute_approved", true);
		taskService.complete(confirmSubstitute.getId(),variables);
		
		LOGGER.warning("Anzahl Tasks: " + taskService.createTaskQuery().count());
		assertThat(processInstance).task().hasDefinitionKey("confirmTeamManager");
		
		Task confirmTeamManager = taskService.createTaskQuery()
	      .taskDefinitionKey("confirmTeamManager")
	      .processInstanceId(processInstance.getId())
	      .singleResult();

		variables.put("managerApproved", true);
		taskService.complete(confirmTeamManager.getId(),variables);
		
		assertThat(processInstance).task().hasDefinitionKey("UpdateLeaveDatabase").hasCandidateGroup("assistance");
		
		Task updateLeaveDatabase = taskService.createTaskQuery()
			      .taskDefinitionKey("UpdateLeaveDatabase")
			      .processInstanceId(processInstance.getId())
			      .singleResult();

		//updateLeaveDatabase.se
		taskService.setAssignee(updateLeaveDatabase.getId(), "assistant1");
		taskService.complete(updateLeaveDatabase.getId(),variables);
	}

}
