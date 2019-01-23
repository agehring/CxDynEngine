package com.checkmarx.engine.vmware;


import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.checkmarx.engine.domain.EngineSize;
import com.checkmarx.engine.servers.CxEngines;
import com.checkmarx.engine.servers.CxEngines.CxServerRole;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.vmware.vim25.TaskInfoState;
import com.vmware.vim25.VirtualMachineCloneSpec;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineRelocateSpec;
import com.vmware.vim25.mo.*;


/**
 * Launches and terminates Vmware instances from a specified image.
 *  
 * @author sergio.pinto@checkmarx.com
 *
 */
@Component
@Profile("vmware")
public class VmwareClient{
	
    private static final Logger log = LoggerFactory.getLogger(VmwareClient.class);
    
    private ServiceInstance si;
	
	private final VmwareEngineConfig config;
	
	private final Folder vmsFolder = null;

	public VmwareClient(VmwareEngineConfig config) throws RemoteException, MalformedURLException {
		
		this.config = config;
		this.si = new ServiceInstance(new URL(config.getUrlStr()), config.getUsername(), config.getPassword(), config.getBypassSSLVerification());

		log.info("ctor(): {}", this);
	}
	
	public VirtualMachine launch(String name, EngineSize size) throws RemoteException {
		
		log.info("launch(): name {} size {}",name,size);

        InventoryNavigator inventoryNavigator = new InventoryNavigator(this.si.getRootFolder());

        VirtualMachine vm = (VirtualMachine) inventoryNavigator.searchManagedEntity("VirtualMachine",this.config.getTemplateVMName());

        if (vm == null) {
            throw new NullPointerException();
        }

        Folder vmsFolder = getVmsFolder();

        VirtualMachineConfigSpec config = new VirtualMachineConfigSpec();
        config.setNumCPUs(Integer.parseInt(this.config.getEngineCpuSizeMap().get(size.getName())));
        config.setMemoryMB(new Long(Integer.parseInt(this.config.getEngineMemSizeMap().get(size.getName()))));
        config.setAnnotation(VmwareVm.buildAnnotation(CxEngines.CX_ROLE_TAG,CxServerRole.ENGINE.toString()));

        VirtualMachineCloneSpec cloneSpec = new VirtualMachineCloneSpec();
        cloneSpec.setConfig(config);
        cloneSpec.setLocation(new VirtualMachineRelocateSpec());
        cloneSpec.setPowerOn(false);
        cloneSpec.setTemplate(false);

        Task task = vm.cloneVM_Task(vmsFolder, name, cloneSpec);

        TaskInfoState state;
        do {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
            state = task.getTaskInfo().getState();
        }
        while (state != TaskInfoState.error && state != TaskInfoState.success);

        if (state == TaskInfoState.success) {

            vm = (VirtualMachine) inventoryNavigator.searchManagedEntity("VirtualMachine",name);

            if (vm == null) {
                throw new NullPointerException();
            }

            vm = powerOn(vm);
            vm = waitForIp(vm);

        } else {
            log.error("Failed to clone virtual machine name:"+name);
        }

        return vm;
	}
	
	public VirtualMachine waitForIp(VirtualMachine vm){
		log.trace("waitForIp():");
		String ip;

        if (vm == null) {
            throw new NullPointerException();
        }
		
		do {
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.error(e.getMessage());
			}
			ip = vm.getSummary().getGuest().getIpAddress();
			
		}
		while (ip==null || !InetAddresses.isInetAddress(ip));
		
		return vm;
	}
	
	public List<VirtualMachine> findAll(String vmAnnotation) throws RemoteException {
		log.trace("findAll():");
		List<VirtualMachine> lvms = Lists.newArrayList();
		ManagedEntity[] mevms = getAllVms();
        VirtualMachine vm;
		for(ManagedEntity mevm : mevms){
			vm = (VirtualMachine) mevm;
			if(vm.getSummary().getConfig().annotation.equalsIgnoreCase(vmAnnotation)){
				lvms.add(vm);
			}
		}
		return lvms;
	}
	
	private ManagedEntity[] getAllVms() throws RemoteException {
		log.trace("getAllVms():");

		Folder vmsFolder = getVmsFolder();

        return new InventoryNavigator(vmsFolder).searchManagedEntities("VirtualMachine");
	}
	
	private Folder getVmsFolder() throws RemoteException{
		log.trace("getVmsFolder():");
		
		Folder rootFolder = this.si.getRootFolder();

        VirtualMachine vmTemp = (VirtualMachine) new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", this.config.getTemplateVMName());

		if(vmTemp==null){
            throw new NullPointerException();
        }

        Folder f = (Folder) vmTemp.getParent();

        if(f==null){
            throw new NullPointerException();
        }

		return f;
	}
	
	public VirtualMachine powerOn(VirtualMachine vm) throws RemoteException{
		log.trace("powerOn():");
		TaskInfoState state;

        if(vm==null){
            throw new NullPointerException();
        }

		Task task = vm.powerOnVM_Task(null);
        do {
            try {
                //TODO enable timeout config
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.error("powerOn(): RemoteException {}",e.getMessage());
            }
            state = task.getTaskInfo().getState();
        }
        while (state != TaskInfoState.error && state != TaskInfoState.success);

        if (state != TaskInfoState.success) {
            log.error("Failure -: Virtual Machine {} cannot be powered ON",vm.getName());
        }

		return vm;
	}
	
	public void stop(String vmName) throws RemoteException {
		log.trace("stop():");
		InventoryNavigator inventoryNavigator = new InventoryNavigator(this.si.getRootFolder());
        VirtualMachine vm = (VirtualMachine) inventoryNavigator.searchManagedEntity("VirtualMachine", vmName);

        if(vm==null){
            throw new NullPointerException();
        }

        try {
            vm.shutdownGuest();
        }catch (Throwable t){
            log.warn("Failed to stop Virtual Machine; vm={}; cause={}; message={}", vm.toString(), t, t.getMessage());
            throw new RuntimeException("Failed to stop virtual machine", t);
        }finally {
            log.info("action={}; Vm {};","stopInstance", vm.toString());
        }
	}

	public void stop(String vmName, boolean isToTerminate) throws RemoteException {
		log.trace("powerOn(): vmName {} terminate {}",vmName,isToTerminate);
		InventoryNavigator inventoryNavigator = new InventoryNavigator(this.si.getRootFolder());
        VirtualMachine vm = (VirtualMachine) inventoryNavigator.searchManagedEntity("VirtualMachine", vmName);

        if(vm==null){
            throw new NullPointerException();
        }

        try {
            if (isToTerminate) {
                log.warn("VirtualMachine {} termination", vm.getName());
                vm.destroy_Task();
            } else {
                vm.shutdownGuest();
            }
        }catch (Throwable t){
            log.warn("Failed to stop Virtual Machine; vm={}; cause={}; message={}", vm.toString(), t, t.getMessage());
            throw new RuntimeException("Failed to stop/remove virtual machine", t);
        }finally {
            log.info("action={}; terminate {}; Vm {};","stopInstance",isToTerminate, vm.toString());
        }
	}
	
	
	public VmwareEngineConfig getConfig() {
		return config;
    }
}