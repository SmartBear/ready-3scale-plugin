package com.smartbear.threescalesupport;

import com.eviware.x.form.support.AField;
import com.eviware.x.form.support.AForm;

@AForm(name = "Select API to Import", description = "Please select from the list which API specification(s) you want to import to the project.")
public interface SelectAPIFrom3ScaleForm {
    @AField(description = "API Name", type = AField.AFieldType.COMPONENT)
    public final static String NAME = "Name";

    @AField(description = "API System Name", type = AField.AFieldType.LABEL)
    public final static String SYSTEM_NAME = "System Name";

    @AField(description = "API Description", type = AField.AFieldType.INFORMATION)
    public final static String DESCRIPTION = "Description";

//    @AField( name = "Generate Virt", description = "Generate a REST Virt", type = AField.AFieldType.BOOLEAN)
//    public final static String GENERATE_MOCK = "Generate Virt";

    @AField(description = "API Definition", type = AField.AFieldType.LABEL)
    public final static String SPEC = "Definition";

}
