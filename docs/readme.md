# Dynamic Engines Documentation


# Building the Engine Server Image

Dynamic Engines launches base images that have the Checkmarx Engine software preinstalled and configured. These steps can be run manually or you can perform them in automation tools like Hashicorp Packer or AWS EC2 Image Builder.


## Install Checkmarx
Run the Checkmarx installer and only select the Engine component. 

For example:
```
CxSetup.exe MANAGER=0 WEB=0 ENGINE=1 AUDIT=0 BI=0 /install /quiet ACCEPT_EULA=Y PORT=80 
```

The Checkmarx Engine installation depends on the .Net Framework 4.7.1 or higher and the Visual C++ redistributable(s). Both installers are bundled in the Checkmarx installation if you need them.

## Open the Host Firewall
Manually add a rule to the Windows Host Firewall to allow inbound connections to TCP port 80 and 443. Choose only port 443 if you will be using SSL. 

For example:
```
New-NetFirewallRule -DisplayName "CxScanEngine" -Direction Inbound -LocalPort 80,443 -Protocol TCP -Action Allow
```

## Set MAX_SCANS_PER_MACHINE to 1
Configure the Engine to only accept a single concurrent scan. Edit  ```C:\Program Files\Checkmarx\Checkmarx Engine Server\CxSourceAnalyzerEngine.WinService.exe.config``` and set the ```MAX_SCANS_PER_MACHINE``` key to `1`.

Example:
```xml
<?xml version="1.0" encoding="utf-8"?>
<configuration>
  ...
  <appSettings>
  ...
    <add key="MAX_SCANS_PER_MACHINE" value="1" />
...
```

You can programatically change this value with this powershell snippet:

```powershell
	# Configure the number of concurrent scans the engine should support. 
	# Default is 3. Field recommendations is 2. Dynamic Engines should use 1.
	$desiredConcurrentScans="1"
	
	# Discover some installation path information on the engine machine...
	[String] $ENGINE_CONFIG_FILE = 'CxSourceAnalyzerEngine.WinService.exe.config'
	[String] $ENGINE_KEY_NAME ='HKLM:\SOFTWARE\Checkmarx\Installation\Checkmarx Engine Server'
	[String] $cxEnginePath = Get-ItemPropertyValue -Path $($ENGINE_KEY_NAME) -Name 'Path' -ErrorAction SilentlyContinue
	[String] $cxEngineConfig = ("{0}\{1}" -f $cxEnginePath, $ENGINE_CONFIG_FILE) 
	
	# Edit the engine config file to set max engines
	[Xml]$xml = Get-Content $cxEngineConfig
	$obj = $xml.configuration.appSettings.add | where {$_.Key -eq "MAX_SCANS_PER_MACHINE" }
	Write-Host "MAX_SCANS_PER_MACHINE initial value is $($obj.value), will be set to $($desiredConcurrentScans)"
	$obj.value = $desiredConcurrentScans
	$xml.Save($cxEngineConfig)   
	
	# Check the value was written
	[Xml]$xml = Get-Content $cxEngineConfig
	$obj = $xml.configuration.appSettings.add | where {$_.Key -eq "MAX_SCANS_PER_MACHINE" }
	Write-Host "MAX_SCANS_PER_MACHINE value is now $($obj.value)"
	if ($obj.value -eq $desiredConcurrentScans) {
	Write-Host "Success!"
	} else {
	Write-Host "Failed to set the concurrent scans to $desiredConcurrentScans"
}

```

## Create the image
Shutdown the system using Windows Sysprep and create an image from your configured server.

# Preparing the Checkmarx Manager

## Block the queueing engine
Dynamic Engines depends on having a single engine that is blocked and registered with the LOC range to be handled by Dynamic Engines. The easiest way to do this is to run the Engine component on your Manager server, register it as Localhost, configure its LOC from 0 to 99999999, and then block that engine. The overhead of a blocked engine on the manager is minimal.

To set your queueing engine, log in and block an engine. Note the engine name as you will set it in the Dynamic Engines configuration file.


# Installing and Configuring the Dynamic Engines tool