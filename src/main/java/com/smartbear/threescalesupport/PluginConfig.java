package com.smartbear.threescalesupport;

import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;
import com.eviware.soapui.plugins.PluginDependency;

@PluginConfiguration(groupId = "com.smartbear.plugins", name = "ready-3scale-plugin", version = "1.0",
        autoDetect = true, description = "3Scale Support Plugin",
        infoUrl = "" )
@PluginDependency(groupId = "com.smartbear.soapui.plugins", name = "Swagger Plugin", minimumVersion = "2.0")
public class PluginConfig extends PluginAdapter {
}
