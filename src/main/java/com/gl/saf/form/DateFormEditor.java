package com.gl.saf.form;

import com.gl.saf.comp.JTextFieldEx;

import javax.swing.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A form editor component for {@link Date} values, providing a text field
 * that parses and formats dates using the Locale default format or a date pattern like "yyyy-MM-dd".
 */
public class DateFormEditor extends JTextFieldEx implements FormEditor<Date> {
    private final DateFormat dateFormat;
    private final FormElement<Date> formElement;

    /**
     * Constructs a new DateFormEditor.
     *
     * @param formPanel   the parent form panel
     * @param formElement the metadata defining the form element
     */
    public DateFormEditor(FormPanel formPanel, FormElement<Date> formElement) {
        this.formElement = formElement;
        this.dateFormat = formElement instanceof DateFormElement dateFormElement && dateFormElement.getDateFormatPattern() != null ?
                new SimpleDateFormat(dateFormElement.getDateFormatPattern()) :
                DateFormat.getDateInstance(DateFormat.SHORT);

        setToolTipText(FormPanel.getTooltipText(formElement));
        if (this.dateFormat instanceof SimpleDateFormat simpleDateFormat)
            setPlaceholderText(simpleDateFormat.toPattern());
        getDocument().addDocumentListener(formPanel);
        Date value = formElement.getDefaultValue();
        if (value != null) {
            setText(dateFormat.format(value));
        }
    }

    @Override
    public void setValue(Date value) {
        if (value != null) {
            setText(dateFormat.format((Date) value));
        } else {
            setText("");
        }
    }

    @Override
    public Date getValue() {
        String text = getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return dateFormat.parse(text);
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public String checkValidity(boolean strictCheck) {
        String text = getText();
        if (formElement.isMandatory() && (text == null || text.trim().isEmpty())) {
            return "Value is required";
        }
        if (text != null && !text.trim().isEmpty()) {
            try {
                dateFormat.parse(text);
            } catch (ParseException e) {
                return "Invalid date format (yyyy-MM-dd)";
            }
        }
        return null;
    }
}
