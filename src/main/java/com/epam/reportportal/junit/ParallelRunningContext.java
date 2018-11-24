/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/agent-java-junit/
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.junit;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ParallelRunningContext {
	// map of currently running tests.
	// key- testName, value - test id
	private ConcurrentMap<Class<?>, String> runningTests;
	// map of currently running suites.
	// key- suite name, value - suite id
	private ConcurrentMap<String, String> runningSuites;
	// map of finished methods
	// key - test value- set of finished methods
	private ConcurrentMap<Class<?>, Set<String>> finishedMethods;

	// map of running methods
	// key - method name value- method id
	private ConcurrentMap<String, RunningMethod> runningMethods;
	private volatile String launchId = "";
	// map of finished test from the specified suites
	// key- suite, value - tests
	private ConcurrentMap<String, Set<Class<?>>> finishedTests;
	// key- method , value - status
	private ConcurrentMap<Method, String> methodStatuses;

	public ParallelRunningContext() {
		runningTests = new ConcurrentHashMap<Class<?>, String>();
		runningSuites = new ConcurrentHashMap<String, String>();
		finishedMethods = new ConcurrentHashMap<Class<?>, Set<String>>();
		runningMethods = new ConcurrentHashMap<String, RunningMethod>();
		finishedTests = new ConcurrentHashMap<String, Set<Class<?>>>();
		methodStatuses = new ConcurrentHashMap<Method, String>();
	}

	public String getRunningSuiteId(String suiteName) {
		return null == suiteName ? null : runningSuites.get(suiteName);
	}

	public String getRunningTestId(Class<?> test) {
		return runningTests.get(test);
	}

	public void setRunningSuiteId(String suiteName, String id) {
		runningSuites.put(suiteName, id);
	}

	public void setRunningTestId(Class<?> test, String id) {
		runningTests.put(test, id);
	}

	public synchronized void addFinishedMethod(Class<?> test, String currentMethod) {
		Set<String> methods = finishedMethods.get(test);
		if (methods == null) {
			methods = new HashSet<String>();
		}
		methods.add(currentMethod);
		finishedMethods.put(test, methods);
	}

	public Set<String> getFinishedMethods(Class<?> test) {
		return finishedMethods.get(test);
	}

	public synchronized boolean tryStart(String method) {
		return runningMethods.get(method) == null;
	}
	
	public synchronized void addRunningMethod(String method, String id) {
		RunningMethod runningMethod = runningMethods.get(method);
		if(runningMethod == null)
			runningMethod = new RunningMethod(id,0);
		runningMethod.level++;
		runningMethods.put(method, runningMethod);
	}

	public boolean tryFinish(String method) {
		RunningMethod runningMethod = runningMethods.get(method);
		runningMethod.level--;
		// System.out.println("try Finish " + method + ", level:" + runningMethod.level);
		return runningMethod.level == 0;
	}

	public String getRunningMethodId(String method) {
		return runningMethods.get(method).id;
	}

	public String getLaunchId() {
		return launchId;
	}

	public void setLaunchId(String launchId) {
		this.launchId = launchId;
	}

	public synchronized void addFinishedTest(String suite, Class<?> test) {
		Set<Class<?>> tests = finishedTests.get(suite);
		if (tests == null) {
			tests = new HashSet<Class<?>>();
		}
		tests.add(test);
		finishedTests.put(suite, tests);
	}

	public Set<Class<?>> getFinishedTests(String suite) {
		return finishedTests.get(suite);
	}

	public void addStatus(Method method, String status) {
		methodStatuses.put(method, status);
	}

	public String getStatus(Method method) {
		return methodStatuses.get(method);
	}

	private static class RunningMethod {
		String id;
		int level;

		public RunningMethod(String id, int level) {
			this.id = id;
			this.level = level;
		}
	}
}
