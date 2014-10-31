package com.smartbear.threescalesupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.plugins.ActionConfiguration;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.action.support.AbstractSoapUIAction;
import com.eviware.x.dialogs.Worker;
import com.eviware.x.dialogs.XProgressDialog;
import com.eviware.x.dialogs.XProgressMonitor;
import com.eviware.x.form.ValidationMessage;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;
import com.eviware.x.form.XFormFieldValidator;
import com.eviware.x.form.support.ADialogBuilder;
import com.jayway.jsonpath.internal.JsonReader;
import com.smartbear.ActionGroups;

import javax.json.JsonObject;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Dimension;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@ActionConfiguration(actionGroup = ActionGroups.OPEN_PROJECT_ACTIONS)
public class AddAPIFrom3ScaleAction extends AbstractSoapUIAction<WsdlProject> {

    public AddAPIFrom3ScaleAction() {
        super("Add API From 3Scale", "Adds API from 3Scale");
    }


    private static class APIListExtractor implements Worker{
        private URL url;
        private List<ThreeScale.ApiDefLink> apis;
        private String apiRetrievingError;
        private XProgressDialog waitDialog;
        private boolean cancelled = false;


        private APIListExtractor(URL developerPortalUrl, XProgressDialog waitDialog){
            url = developerPortalUrl;
            this.waitDialog = waitDialog;
        }

        public static List<ThreeScale.ApiDefLink> downloadList(URL developerPortalUrl){
            APIListExtractor worker = new APIListExtractor(developerPortalUrl, UISupport.getDialogs().createProgressDialog("Getting APIs List...", 0, "", true));
            try {
                worker.waitDialog.run(worker);
            }
            catch(Exception e){
                UISupport.showErrorMessage(e.getMessage());
            }
            return worker.apis;
        }

        @Override
        public Object construct(XProgressMonitor monitor) {
            try {
                apis = ThreeScale.getApiDefLinks(url);
            }
            catch (Throwable e){
                apiRetrievingError = e.getMessage();
            }
            return null;
        }

        @Override
        public void finished() {
            if (cancelled) {
                apis = null;
                return;
            }
            waitDialog.setVisible(false);
            if(StringUtils.hasContent(apiRetrievingError)){
                UISupport.showErrorMessage("Unable to extract API Definition List from the specified 3Scale developer portal because of the following error:\n" + apiRetrievingError);
                return;
            }
            if(apis == null || apis.size() == 0){
                apis = null;
                UISupport.showErrorMessage("No API is accessible at the specified URL.");
            }

        }

        @Override
        public boolean onCancel() {
            cancelled = true;
            waitDialog.setVisible(false);
            return true;
        }

    }


    private static class APIImporter implements Worker {
        private XProgressDialog waitDialog;
        private boolean cancelled = false;
        private List<ThreeScale.ApiDefLink> links;
        private WsdlProject project;
        private List<RestService> addedServices = new ArrayList<RestService>();

        String errors = "";

        private APIImporter(XProgressDialog waitDialog, List<ThreeScale.ApiDefLink> links, WsdlProject project){
            this.waitDialog = waitDialog;
            this.links = links;
            this.project = project;
        }

        public static List<RestService> importServices(List<ThreeScale.ApiDefLink> links, WsdlProject project){
            APIImporter worker = new APIImporter(UISupport.getDialogs().createProgressDialog("Importing APIs...", 100, "", true), links, project);
            try {
                worker.waitDialog.run(worker);
            }
            catch (Exception e) {
                UISupport.showErrorMessage(e.getMessage());
            }
            if (worker.addedServices != null && worker.addedServices.size() > 0) return worker.addedServices; else return null;
        }

        @Override
        public Object construct(XProgressMonitor monitor) {
            for(ThreeScale.ApiDefLink link : links){
                if(cancelled) break;
                RestService service;
                try{
                    service = ThreeScale.importAPItoProject(link, project);
                }
                catch(Throwable e){
                    errors = errors + String.format("Importing of \"%s\" API has failed with \"%s\" error.\n", link.name, e.getMessage());
                    continue;
                }
                addedServices.add(service);
            }
            return null;
        }

        @Override
        public void finished() {
            if (cancelled) {
                return;
            }
            waitDialog.setVisible(false);
            if(StringUtils.hasContent(errors)){
                UISupport.showErrorMessage(errors);
            }
        }

        @Override
        public boolean onCancel() {
            cancelled = true;
            waitDialog.setVisible(false);
            return true;
        }
    }

    private URL stringToUrl(String s){
        if(StringUtils.isNullOrEmpty(s)) return null;
        try {
            return new URL(s);
        }
        catch (MalformedURLException e){
            return null;
        }
    }



    @Override
    public void perform(WsdlProject wsdlProject, Object o) {
        final String urlString = UISupport.getDialogs().prompt("Input developer portal URL (i.e. developer.example.com or example.3scale.net)", "Add API Specification from 3Scale");
        if(urlString == null) return;
        URL url = stringToUrl(urlString);
        if(url == null) {
            UISupport.showErrorMessage("Invalid URL");
            return;
        }
        final List<ThreeScale.ApiDefLink> apis = APIListExtractor.downloadList(url);
        if(apis == null) return;
        final XFormDialog dialog = ADialogBuilder.buildDialog(SelectAPIFrom3ScaleForm.class);
        ListModel<String> listBoxModel = new AbstractListModel<String>() {
            @Override
            public int getSize() {
                return apis.size();
            }

            @Override
            public String getElementAt(int index) {
                return apis.get(index).name;
            }
        };
        final JList apiListBox = new JList(listBoxModel);
        dialog.getFormField(SelectAPIFrom3ScaleForm.NAME).setProperty("component", new JScrollPane(apiListBox));
        dialog.getFormField(SelectAPIFrom3ScaleForm.NAME).setProperty("preferredSize", new Dimension(500, 150));
        dialog.getFormField(SelectAPIFrom3ScaleForm.DESCRIPTION).setProperty("preferredSize", new Dimension(500, 50));
        dialog.setValue(SelectAPIFrom3ScaleForm.DESCRIPTION, null);
        dialog.setValue(SelectAPIFrom3ScaleForm.SPEC, null);
        dialog.setValue(SelectAPIFrom3ScaleForm.SYSTEM_NAME, null);

        apiListBox.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int[] selected = apiListBox.getSelectedIndices();
                if(selected != null && selected.length == 1) {
                    int selectedNo = selected[0];
                    dialog.getFormField(SelectAPIFrom3ScaleForm.SYSTEM_NAME).setValue(apis.get(selectedNo).systemName);
                    dialog.getFormField(SelectAPIFrom3ScaleForm.DESCRIPTION).setValue(apis.get(selectedNo).description);
                    dialog.getFormField(SelectAPIFrom3ScaleForm.SPEC).setValue(apis.get(selectedNo).path);
                }
                else{
                    dialog.getFormField(SelectAPIFrom3ScaleForm.SYSTEM_NAME).setValue(null);
                    dialog.getFormField(SelectAPIFrom3ScaleForm.DESCRIPTION).setValue(null);
                    dialog.getFormField(SelectAPIFrom3ScaleForm.SPEC).setValue(null);
                }
            }
        });
        apiListBox.setSelectedIndex(-1);

        dialog.getFormField(SelectAPIFrom3ScaleForm.NAME).addFormFieldValidator(new XFormFieldValidator() {
            @Override
            public ValidationMessage[] validateField(XFormField formField) {
                int[] selected = apiListBox.getSelectedIndices();
                if(selected == null || selected.length == 0) return new ValidationMessage[]{ new ValidationMessage("Please select at least one API specification to add.", formField)}; else  return new ValidationMessage[0];
            }
        });

        if(dialog.show()){
            int[] selected = apiListBox.getSelectedIndices();
            ArrayList<ThreeScale.ApiDefLink> selectedAPIs = new ArrayList<ThreeScale.ApiDefLink>();
            for(int no: selected){
                selectedAPIs.add(apis.get(no));
            }
            List<RestService> services = APIImporter.importServices(selectedAPIs, wsdlProject);
            if(services != null && services.size() != 0){
                UISupport.select(services.get(0));
            }

        }


    }
}
