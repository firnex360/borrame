package parcial.services;

import parcial.logic.Form;

public class FormServices extends GeneralClass<Form> {

    private static FormServices instance;

    private FormServices() {
        super(Form.class);
    }

    public static FormServices getInstance() {
        if (instance == null) {
            instance = new FormServices();
        }
        return instance;
    }


}