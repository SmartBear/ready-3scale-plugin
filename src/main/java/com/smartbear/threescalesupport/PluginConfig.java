package com.smartbear.threescalesupport;

import com.eviware.soapui.plugins.PluginAdapter;
import com.eviware.soapui.plugins.PluginConfiguration;

@PluginConfiguration(groupId = "com.smartbear.plugins", name = "3scale Plugin", version = "1.0.1",
        autoDetect = true, description = "Adds actions to import APIs from 3scale hosted developer portals",
        infoUrl = "https://github.com/SmartBear/ready-3scale-plugin" )
public class PluginConfig extends PluginAdapter {
}
