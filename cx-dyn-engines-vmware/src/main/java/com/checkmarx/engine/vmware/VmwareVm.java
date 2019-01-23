package com.checkmarx.engine.vmware;

import javax.validation.constraints.NotNull;

import com.google.common.base.MoreObjects;
import com.vmware.vim25.mo.*;

public class VmwareVm {

	public enum InstanceState {
		RESETTING(0), RUNNING(16), SHUTTINGDOWN(32), STANDBY(48), NOTRUNNING(64), UNKNOWN(9999);

		private final int code;

		public int getCode() {
			return code;
		}

		InstanceState(int code) {
			this.code = code;
		}

		public static InstanceState from(int code) {
			for (InstanceState state : InstanceState.values()) {
				if (code == state.code)
					return state;
			}
			return InstanceState.UNKNOWN;
		}

		private static InstanceState fromName(String stateName) {
			for (InstanceState state : InstanceState.values()) {
				if (stateName.equalsIgnoreCase(state.toString()))
					return state;
			}
			return InstanceState.UNKNOWN;
		}
	}

	private VmwareVm() {
		// static class
	}

	public static String getName(@NotNull VirtualMachine vm) {
		return vm.getSummary().getConfig().getName();
	}
	
	public static int getLaunchTime(@NotNull VirtualMachine vm) {
		return vm.getSummary().getQuickStats().getUptimeSeconds();
	}
	
	public static String getUUID(@NotNull VirtualMachine vm) {
		return vm.getSummary().getConfig().uuid;
	}
	
	public static int getMemory(@NotNull VirtualMachine vm) {
		return vm.getConfig().getHardware().memoryMB;
	}
	
	public static int getCpu(@NotNull VirtualMachine vm) {
		return vm.getConfig().getHardware().numCPU;
	}
	
	public static String getIpAddress(@NotNull VirtualMachine vm) {
		return vm.getSummary().getGuest().getIpAddress();
	}

	private static InstanceState getState(VirtualMachine vm) {
		// return vm == null ? InstanceState.UNKNOWN :
		// InstanceState.from(vm.getState().getCode());
		return vm == null ? InstanceState.UNKNOWN : InstanceState.fromName(vm.getGuest().guestState);
	}

	public static boolean isProvisioned(@NotNull VirtualMachine vm) {
		return !isTerminated(vm);
	}

	public static boolean isTerminated(@NotNull VirtualMachine vm) {
		final InstanceState state = getState(vm);
		switch (state) {
		case SHUTTINGDOWN:
		case UNKNOWN:
			return true;
		default:
			return false;
		}
	}

	public static boolean isRunning(@NotNull VirtualMachine vm) {
		// final Instance instance = determineStatus(instanceId, null);
		final InstanceState state = getState(vm);
		return InstanceState.RUNNING.equals(state);
	}

	public static String print(@NotNull VirtualMachine vm) {
		if (vm == null)
			return "null";

		return MoreObjects.toStringHelper(vm).add("id", getUUID(vm)).add("name", getName(vm))
				.toString();
	}
	
	public static String buildAnnotation(String tagOne,String tagTwo){
		return tagOne+"|"+tagTwo;
	}
}
