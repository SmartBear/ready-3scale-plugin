package com.smartbear.threescalesupport;

import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.plugins.ActionConfiguration;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.smartbear.ActionGroups;

import java.net.URL;
import java.util.List;

@ActionConfiguration(actionGroup = ActionGroups.OPEN_PROJECT_ACTIONS, separatorBefore = true)
public class AddAPIFrom3ScaleAction extends AbstractSoapUIAction<WsdlProject> {

    public AddAPIFrom3ScaleAction() {
        super("Add API From 3Scale", "Adds API from the specification on 3Scale developer portal.");
    }




    @Override
    public void perform(WsdlProject wsdlProject, Object o) {
        Utils.APIListExtractionResult listExtractionResult;
        String urlString = null;
        while(true) {
            urlString = UISupport.getDialogs().prompt("Input developer portal URL (i.e. developer.example.com or example.3scale.net)", "Add API Specification from 3Scale", urlString);
            if (urlString == null) return;
            URL url = Utils.stringToUrl(urlString);
            if (url == null) {
                UISupport.showErrorMessage("Invalid URL");
                continue;
            }
            listExtractionResult = Utils.downloadAPIList(url);
            if (listExtractionResult.canceled) return;

            if (listExtractionResult.apis != null) break;
            UISupport.showErrorMessage(listExtractionResult.error);
        }

        List<ThreeScale.ApiDefLink> selectedAPIs = Utils.showSelectAPIDefDialog(listExtractionResult.apis);
        if(selectedAPIs != null){
            List<RestService> services = Utils.importServices(selectedAPIs, wsdlProject);
            if(services != null && services.size() != 0){
                UISupport.select(services.get(0));
            }
        }


    }
}
