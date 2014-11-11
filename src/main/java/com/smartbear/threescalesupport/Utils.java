package com.smartbear.threescalesupport;

import com.eviware.soapui.SoapUI;
import com.eviware.soapui.impl.rest.RestService;
import com.eviware.soapui.impl.wsdl.WsdlProject;
import com.eviware.soapui.support.StringUtils;
import com.eviware.soapui.support.UISupport;
import com.eviware.x.dialogs.Worker;
import com.eviware.x.dialogs.XProgressDialog;
import com.eviware.x.dialogs.XProgressMonitor;
import com.eviware.x.form.ValidationMessage;
import com.eviware.x.form.XFormDialog;
import com.eviware.x.form.XFormField;
import com.eviware.x.form.XFormFieldValidator;
import com.eviware.x.form.support.ADialogBuilder;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class Utils {
    public static URL stringToUrl(String s){
        if(StringUtils.isNullOrEmpty(s)) return null;
        try {
            return new URL(s);
        }
        catch (MalformedURLException e){
            return null;
        }
    }

    public static APIListExtractionResult downloadAPIList(URL developerPortalUrl){
        return APIListExtractor.downloadList(developerPortalUrl);
    }

    public static List<ThreeScale.ApiDefLink> showSelectAPIDefDialog(final List<ThreeScale.ApiDefLink> apis){
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

        if(dialog.show()) {
            int[] selected = apiListBox.getSelectedIndices();
            ArrayList<ThreeScale.ApiDefLink> selectedAPIs = new ArrayList<ThreeScale.ApiDefLink>();
            for (int no : selected) {
                selectedAPIs.add(apis.get(no));
            }
            return selectedAPIs;
        }
        else{
            return null;
        }

    }

    public static List<RestService> importServices(List<ThreeScale.ApiDefLink> links, WsdlProject project){
        return APIImporter.importServices(links, project);
    }

    public static class APIListExtractionResult{
        public List<ThreeScale.ApiDefLink> apis = null;
        public String error = null;
        public boolean canceled = false;

        public void addError(String errorText){
            apis = null;
            if(error == null) error = errorText; else error = error + "\n" + errorText;
        }

        public void cancel(){
            canceled = true;
            apis = null;
        }
    }


    private static class APIListExtractor implements Worker {
        private URL url;
        private XProgressDialog waitDialog;
        APIListExtractionResult result = new APIListExtractionResult();
        private String apiRetrievingError = null;

        private APIListExtractor(URL developerPortalUrl, XProgressDialog waitDialog){
            url = developerPortalUrl;
            this.waitDialog = waitDialog;
        }

        public static APIListExtractionResult downloadList(URL developerPortalUrl){
            APIListExtractor worker = new APIListExtractor(developerPortalUrl, UISupport.getDialogs().createProgressDialog("Getting APIs List...", 0, "", true));
            try {
                worker.waitDialog.run(worker);
            }
            catch(Exception e){
                SoapUI.logError(e);
                worker.result.addError(e.getMessage());
            }
            return worker.result;
        }

        @Override
        public Object construct(XProgressMonitor monitor) {
            try {
                result.apis = ThreeScale.getApiDefLinks(url);
            }
            catch (Throwable e){
                SoapUI.logError( e );
                apiRetrievingError = e.getMessage();
                if(StringUtils.isNullOrEmpty(apiRetrievingError)) apiRetrievingError = e.getClass().getName();
            }
            return null;
        }

        @Override
        public void finished() {
            if (result.canceled) return;
            waitDialog.setVisible(false);
            if(StringUtils.hasContent(apiRetrievingError)){
                result.addError("Unable to extract API Definition List from the specified 3Scale developer portal because of the following error:\n" + apiRetrievingError);
                return;
            }
            if(result.apis == null || result.apis.size() == 0){
                result.addError("No API is accessible at the specified URL or registered correctly.");
            }

        }

        @Override
        public boolean onCancel() {
            result.cancel();
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
                SoapUI.logError( e );
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
                    SoapUI.logError( e );
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


}
