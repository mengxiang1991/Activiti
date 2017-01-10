package org.activiti.standalone.cfg;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.cmd.AbstractCustomSqlExecution;
import org.activiti.engine.impl.cmd.CustomSqlExecution;
import org.activiti.engine.impl.test.ResourceActivitiTestCase;
import org.activiti.engine.task.Task;

/**
 * @author jbarrez
 */
public class CustomMybatisMapperTest extends ResourceActivitiTestCase {
	
	public CustomMybatisMapperTest() {
		super("org/activiti/standalone/cfg/custom-mybatis-mappers-activiti.cfg.xml");
	}
	
	public void testSelectTaskColumns() {
		
		// Create test data
		for (int i=0; i<5; i++) {
			Task task = taskService.newTask();
			task.setName(i + "");
			taskService.saveTask(task);
		}
		
		// Fetch the columns we're interested in
		CustomSqlExecution<MyTestMapper, List<Map<String, Object>>> customSqlExecution = 
				new AbstractCustomSqlExecution<MyTestMapper, List<Map<String, Object>>>(MyTestMapper.class) {
			
			public List<Map<String, Object>> execute(MyTestMapper customMapper) {
				return customMapper.selectTasks();
			}
		
		};
		
		// Verify
		List<Map<String, Object>> tasks = managementService.executeCustomSql(customSqlExecution);
		assertEquals(5, tasks.size());
		for (int i=0; i<5; i++) {
			Map<String, Object> task = tasks.get(i);
			assertNotNull(getResultObject("ID", task));
			assertNotNull(getResultObject("NAME", task));
			assertNotNull(getResultObject("CREATETIME", task));
		}
		
		// Cleanup
		for (Task task : taskService.createTaskQuery().list()) {
			taskService.deleteTask(task.getId());
			historyService.deleteHistoricTaskInstance(task.getId());
		}
		
	}
	
	public void testFetchTaskWithSpecificVariable() {
		
		// Create test data
		for (int i=0; i<5; i++) {
			Task task = taskService.newTask();
			task.setName(i + "");
			taskService.saveTask(task);
			
			taskService.setVariable(task.getId(), "myVar", Long.valueOf(task.getId()) * 2);
			taskService.setVariable(task.getId(), "myVar2", "SomeOtherValue");
		}
		
		// Fetch data with custom query
		CustomSqlExecution<MyTestMapper, List<Map<String, Object>>> customSqlExecution = 
				new AbstractCustomSqlExecution<MyTestMapper, List<Map<String, Object>>>(MyTestMapper.class) {
			
			public List<Map<String, Object>> execute(MyTestMapper customMapper) {
				return customMapper.selectTaskWithSpecificVariable("myVar");
			}
		
		};
		
		// Verify
		List<Map<String, Object>> results = managementService.executeCustomSql(customSqlExecution);
		assertEquals(5, results.size());
		for (int i=0; i<5; i++) {
			Map<String, Object> result = results.get(i);
			Long id = Long.valueOf((String) getResultObject("TASKID", result));
			Long variableValue = null;
			Object variableObjectValue = getResultObject("VARIABLEVALUE", result);
			if (variableObjectValue instanceof BigDecimal) { // in oracle it's BigDecimal
			  variableValue = ((BigDecimal)variableObjectValue).longValue();
			} else {
			  variableValue = (Long) variableObjectValue;
			}
			assertEquals(id * 2, variableValue.longValue());
		}
		
		// Cleanup
		for (Task task : taskService.createTaskQuery().list()) {
			taskService.deleteTask(task.getId());
			historyService.deleteHistoricTaskInstance(task.getId());
		}
		
	}
	
	protected Object getResultObject(String key, Map<String, Object> result) {
		Object resultObject = result.get(key);
		if (resultObject == null) { // postgresql requires lower case
			resultObject = result.get(key.toLowerCase());
		}
		return resultObject;
	}
	
}
