package com.smartbear.threescalesupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.WorkspaceImpl;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.plugins.auto.PluginImportMethod;
import com.eviware.soapui.support.SoapUIException;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.form.ValidationMessage;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;
import com.eviware.x.form.XFormFieldListener;
import com.eviware.x.form.XFormFieldValidator;
import com.eviware.x.form.support.ADialogBuilder;
import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;

import java.net.URL;
import java.util.List;

@PluginImportMethod(label = "3scale Developer Portal (REST)")
public class AddProjectFrom3ScaleAction extends AbstractSoapUIAction<WorkspaceImpl> {

    private boolean isProjectNameTyped = false;
    private Utils.APIListExtractionResult listExtractionResult = null;

    public AddProjectFrom3ScaleAction(){
        super("Create Project From 3scale", "Creates a new project from API specification on 3scale developer portal.");
    }

    @Override
    public void perform(final WorkspaceImpl target, Object param) {
        isProjectNameTyped = false;
        listExtractionResult = null;
        final XFormDialog dialog = ADialogBuilder.buildDialog(Form.class);
        dialog.getFormField(Form.DEVELOPER_PORTAL_URL).addFormFieldListener(new XFormFieldListener() {
            @Override
            public void valueChanged(XFormField sourceField, String newValue, String oldValue) {
                if(!isProjectNameTyped){
                    String defProjectName = getDefaultProjectName(dialog, newValue);
                    dialog.setValue(Form.PROJECT_NAME, defProjectName);
                }
            }
        });
        dialog.getFormField(Form.PROJECT_NAME).addFormFieldListener(new XFormFieldListener() {
            @Override
            public void valueChanged(XFormField sourceField, String newValue, String oldValue) {
                isProjectNameTyped = true;
            }
        });

        dialog.getFormField(Form.DEVELOPER_PORTAL_URL).addFormFieldValidator(new XFormFieldValidator() {
            @Override
            public ValidationMessage[] validateField(XFormField formField) {
                if(StringUtils.isNullOrEmpty(dialog.getValue(Form.DEVELOPER_PORTAL_URL))){
                    return new ValidationMessage[]{new ValidationMessage("Please enter the developer portal URL.", dialog.getFormField(Form.DEVELOPER_PORTAL_URL))};
                }

                if(StringUtils.isNullOrEmpty(dialog.getValue(Form.PROJECT_NAME))){
                    return new ValidationMessage[]{new ValidationMessage("Please enter project name.", dialog.getFormField(Form.PROJECT_NAME))};
                }

                URL portalUrl = Utils.stringToUrl(formField.getValue());
                if(portalUrl == null){
                    return new ValidationMessage[]{new ValidationMessage("Invalid developer portal URL.", formField)};
                }
                listExtractionResult = Utils.downloadAPIList(portalUrl);
                if(StringUtils.hasContent(listExtractionResult.error)){
                    return new ValidationMessage[]{new ValidationMessage(listExtractionResult.error, formField)};
                }
                return new ValidationMessage[0];
            }
        });

        if(dialog.show() && !listExtractionResult.canceled){
            List<ThreeScale.ApiDefLink> selectedAPIs = Utils.showSelectAPIDefDialog(listExtractionResult.apis);
            if(selectedAPIs != null){
                WsdlProject project;
                try {
                    project = target.createProject(dialog.getValue(Form.PROJECT_NAME), null);
                }
                catch(Exception e){
                    SoapUI.logError(e);
                    UISupport.showErrorMessage(String.format("Unable to create Project because of %s exception with \"%s\" message", e.getClass().getName(), e.getMessage()));
                    return;
                }
                List<RestService> services = Utils.importServices(selectedAPIs, project);
                if(services != null && services.size() != 0){
                    UISupport.select(services.get(0));
                }
                else{
                    target.removeProject(project);
                }
            }
        }
    }

    public String getDefaultProjectName(XFormDialog dialog, String newValue) {
        if (!StringUtils.hasContent(newValue)) return "";
        newValue = newValue.trim();
        int ix = newValue.lastIndexOf('.');
        if (ix > 0) {
            newValue = newValue.substring(0, ix);
        }

        ix = newValue.lastIndexOf('/');
        if (ix == -1) {
            ix = newValue.lastIndexOf('\\');
        }

        if (ix != -1) {
            newValue = newValue.substring(ix + 1);
        }
        if(newValue.toLowerCase().endsWith(".3scale")){
            newValue = newValue.substring(0, newValue.length() - ".3scale".length());
        }
        else{
            ix = newValue.toLowerCase().indexOf("developer."); // is www.developer.example.com allowed?
            if(ix != -1){
                newValue = newValue.substring(ix + "developer.".length(), newValue.length());
            }
        }
        return newValue;
    }


    @AForm(name = "Create Project From API Specification on 3scale Portal", description = "Creates a new Project from API specification on 3scale developer portal in this workspace")
    private interface Form {
        @AField(name = "Project Name", description = "Name of the project", type = AField.AFieldType.STRING)
        public final static String PROJECT_NAME = "Project Name";

        @AField(name = "Developer Portal URL", description = "Developer portal URL (i.e. developer.example.com or example.3scale.net)", type = AField.AFieldType.STRING)
        public final static String DEVELOPER_PORTAL_URL = "Developer Portal URL";

    }
}
