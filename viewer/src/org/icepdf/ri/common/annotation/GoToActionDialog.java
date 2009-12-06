/*
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * "The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is ICEpdf 3.0 open source software code, released
 * May 1st, 2009. The Initial Developer of the Original Code is ICEsoft
 * Technologies Canada, Corp. Portions created by ICEsoft are Copyright (C)
 * 2004-2009 ICEsoft Technologies Canada, Corp. All Rights Reserved.
 *
 * Contributor(s): _____________________.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"
 * License), in which case the provisions of the LGPL License are
 * applicable instead of those above. If you wish to allow use of your
 * version of this file only under the terms of the LGPL License and not to
 * allow others to use your version of this file under the MPL, indicate
 * your decision by deleting the provisions above and replace them with
 * the notice and other provisions required by the LGPL License. If you do
 * not delete the provisions above, a recipient may use your version of
 * this file under either the MPL or the LGPL License."
 *
 */
package org.icepdf.ri.common.annotation;

import org.icepdf.core.pobjects.Destination;
import org.icepdf.core.pobjects.Name;
import org.icepdf.core.pobjects.NameTree;
import org.icepdf.core.pobjects.Reference;
import org.icepdf.core.pobjects.actions.ActionFactory;
import org.icepdf.core.pobjects.actions.GoToAction;
import org.icepdf.core.pobjects.annotations.Annotation;
import org.icepdf.core.pobjects.annotations.LinkAnnotation;
import org.icepdf.core.views.swing.AnnotationComponentImpl;
import org.icepdf.ri.common.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;
import java.util.Vector;

/**
 * GoTo Action panel used for setting an GoTo Action type properties.  GoTo
 * actions store a PDF Destination data structure which can either be a named
 * destination or a vector of properties that specifies a page location.
 *
 * @since 4.0
 */
public class GoToActionDialog extends AnnotationDialogAdapter
        implements ActionListener, ItemListener {

    public static final String EMPTY_DESTINATION = "      ";

    private SwingController controller;
    private ResourceBundle messageBundle;
    private AnnotationComponentImpl currentAnnotaiton;
    private ActionsPanel actionsPanel;

    // state full ui elements.
    private GridBagConstraints constraints;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton implicitDestination;
    private JRadioButton namedDestination;

    // controls for explicit destinations
    private JComboBox implicitDestTypeComboBox;
    private JTextField pageNumberTextField;
    private JTextField topTextField;
    private JTextField bottomTextField;
    private JTextField leftTextField;
    private JTextField rightTextField;
    private JTextField zoomTextField;
    private JButton viewPositionButton;

    // named destination fields.
    private JLabel destinationName;
    private JButton viewNamedDesButton;
    private NameTreeDialog nameTreeDialog;

    public GoToActionDialog(SwingController controller,
                            ActionsPanel actionsPanel) {
        super(controller.getViewerFrame(), true);
        this.controller = controller;
        this.messageBundle = this.controller.getMessageBundle();
        this.actionsPanel = actionsPanel;

        setTitle(messageBundle.getString(
                "viewer.utilityPane.action.dialog.goto.title"));
        // setup gui components.
        setGui();
    }

    /**
     * Copies state information from the annotation so it can pre respresented
     * in the UI.  This method does not modify the annotaiton object in any way.
     * State saving should handled with save state call.
     *
     * @param annotaiton annotation to be updated by dialog.
     */
    public void setAnnotationComponent(AnnotationComponentImpl annotaiton) {

        // get a reference so we can setup a save on dialog close
        currentAnnotaiton = annotaiton;

        org.icepdf.core.pobjects.actions.Action action =
                currentAnnotaiton.getAnnotation().getAction();

        // get the destination object, doesn't matter where it comes from.
        Destination dest = null;
        if (action != null && action instanceof GoToAction) {
            dest = ((GoToAction) action).getDestination();
        }
        // alternatively we can have a dest field on Link annotations
        else if (action == null &&
                currentAnnotaiton.getAnnotation() instanceof LinkAnnotation) {
            LinkAnnotation linkAnnotation =
                    (LinkAnnotation) currentAnnotaiton.getAnnotation();
            dest = linkAnnotation.getDestination();
        }
        // check to see of we have a name tree in the document, if not we
        // disable the controls for named destinations
        if (controller.getDocument().getCatalog().getNameTree() == null) {
            implicitDestinationFieldsEnabled(true);
            clearImplicitDestinations(true);
            namedDestination.setEnabled(false);
        } else {
            namedDestination.setEnabled(true);
        }

        // start gui value assignments.
        if (dest != null) {
            // first clear all previous values.
            clearImplicitDestinations(false);
            clearImplicitDestinations(true);
            // implicit assignment
            if (dest.getNamedDestination() == null) {
                implicitDestinationFieldsEnabled(true);
                Name type = dest.getType();
                applySelectedValue(implicitDestTypeComboBox, type);
                // set field visibility for type
                enableFitTypeFields(type);
                // type assignment.
                applyTypeValues(dest, type);
                // finally assign the page number
                pageNumberTextField.setText(String.valueOf(controller.getDocument()
                        .getPageTree().getPageNumber(dest.getPageReference()) + 1));
            }
            // named assignment
            else {
                // enable GUI elements.
                implicitDestinationFieldsEnabled(false);
                // assign name to name label
                destinationName.setText(dest.getNamedDestination().toString());
            }
        }
    }

    /**
     * Utility or saving the complicated state of a GoTo action.
     */
    private void saveActionState() {

        Annotation annotation = currentAnnotaiton.getAnnotation();
        Destination destination;

        // create a new implicit destination
        if (implicitDestination.isSelected()) {
            Name fitType = (Name) ((ValueLabelItem) implicitDestTypeComboBox
                    .getSelectedItem()).getValue();
            int pageNumber = Integer.parseInt(pageNumberTextField.getText());
            Reference pageReference = controller.getDocument().getPageTree()
                    .getPageReference(pageNumber - 1);
            Vector destVector = null;
            if (fitType.equals(Destination.TYPE_FIT) ||
                    fitType.equals(Destination.TYPE_FITB)) {
                destVector = Destination.destinationSyntax(pageReference, fitType);
            }
            // just top enabled
            else if (fitType.equals(Destination.TYPE_FITH) ||
                    fitType.equals(Destination.TYPE_FITBH) ||
                    fitType.equals(Destination.TYPE_FITV) ||
                    fitType.equals(Destination.TYPE_FITBV)) {
                Object top = parseFloat(topTextField.getText());
                destVector = Destination.destinationSyntax(
                        pageReference, fitType, top);
            }
            // special xyz case
            else if (fitType.equals(Destination.TYPE_XYZ)) {
                Object left = parseFloat(leftTextField.getText());
                Object top = parseFloat(topTextField.getText());
                Object zoom = parseFloat(zoomTextField.getText());
                destVector = Destination.destinationSyntax(
                        pageReference, fitType, left, top, zoom);
            }
            // special FitR
            else if (fitType.equals(Destination.TYPE_FITR)) {
                Object left = parseFloat(leftTextField.getText());
                Object bottom = parseFloat(leftTextField.getText());
                Object right = parseFloat(leftTextField.getText());
                Object top = parseFloat(leftTextField.getText());
                destVector = Destination.destinationSyntax(
                        pageReference, fitType, left, bottom, right, top);
            }
            destination = new Destination(annotation.getLibrary(), destVector);
        }
        // otherwise a simple named destination
        else {
            destination = new Destination(annotation.getLibrary(),
                    new Name(destinationName.getText()));
        }
        GoToAction action = (GoToAction) annotation.getAction();

        // if no previous action then we have a 'new' or old 'dest' that
        // that is getting updated.  VERY IMPORTANT, dest are replaced with
        // similar GoToActions under the current implementation.
        if (action == null) {
            action = (GoToAction)
                    ActionFactory.buildAction(annotation.getLibrary(),
                            ActionFactory.GOTO_ACTION);
            action.setDestination(destination);
            annotation.addAction(action);
            actionsPanel.clearActionList();
            actionsPanel.addActionToList(action);
        } else {
            // set new destination value and merge the change back into the
            // annotation.
            action.setDestination(destination);
            annotation.updateAction(action);
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == okButton) {
            // todo validate action State before save proceeds.
            if (true) {
                // if all is
                saveActionState();
                dispose();
            }
        } else if (e.getSource() == cancelButton) {
            // disposes this dialog
            dispose();
        } else if (e.getSource() == viewNamedDesButton) {
            // test implementation of a NameJTree for destinations.
            NameTree nameTree = controller.getDocument().getCatalog().getNameTree();
            if (nameTree != null) {
                // create new dialog instance.
                nameTreeDialog = new NameTreeDialog(
                        controller,
                        true, nameTree);
                nameTreeDialog.setDestinationName(destinationName);
                // add the nameTree instance.
                nameTreeDialog.setVisible(true);
                nameTreeDialog.dispose();
            }
        }
        // very special button that gets the current view position coords. 
        else if (e.getSource() == viewPositionButton) {

        }
    }

    @Override
    public void dispose() {
        setVisible(false);
        super.dispose();
        // dispose the name tree if someone opened
        if (nameTreeDialog != null) {
            nameTreeDialog.dispose();
        }
    }

    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED ||
                e.getStateChange() == ItemEvent.DESELECTED) {
            // enable/disable field sets for the two destinations types.
            if (e.getSource() == implicitDestination) {
                implicitDestinationFieldsEnabled(e.getStateChange() == ItemEvent.SELECTED);
                // check for an empty type and if so assign fit
                if (implicitDestination.isSelected()) {
                    if (implicitDestTypeComboBox.getSelectedItem() == null) {
                        applySelectedValue(implicitDestTypeComboBox,
                                Destination.TYPE_FIT);
                        enableFitTypeFields(Destination.TYPE_FIT);
                    }
                }
            }
            // handle enabled state of top,bottom,left right and zoom.
            else if (e.getSource() == implicitDestTypeComboBox) {
                ValueLabelItem valueItem = (ValueLabelItem) e.getItem();
                Name fitType = (Name) valueItem.getValue();
                enableFitTypeFields(fitType);
            }
        }
    }

    /**
     * Method to create and customize the actions section of the panel
     *
     */
    protected void setGui() {

        /**
         * Place GUI elements on dialog
         */

        JPanel goToActionPanel = new JPanel();

        goToActionPanel.setAlignmentY(JPanel.TOP_ALIGNMENT);
        GridBagLayout layout = new GridBagLayout();
        goToActionPanel.setLayout(layout);

        constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 1.0;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(5, 5, 5, 5);

        /**
         *  Create explicit layout
         */
        // main panel for implicit fields, more work need for other fit types.
        JPanel explicitDestinationSubpane = new JPanel(new GridLayout(4, 4, 10, 5));
        explicitDestinationSubpane.setBorder(new EmptyBorder(0, 40, 0, 0));
        // use current view location
//        explicitDestinationSubpane.add(new JLabel(messageBundle.getString(
//                "viewer.utilityPane.action.dialog.goto.current.label")));
//        viewPositionButton = new JButton(messageBundle.getString(
//                "viewer.utilityPane.action.dialog.goto.current"));
//        viewPositionButton.addActionListener(this);
//        explicitDestinationSubpane.add(viewPositionButton);
        // filler
//        explicitDestinationSubpane.add(new JLabel());
//        explicitDestinationSubpane.add(new JLabel());
        // fit type - currently only xyz
        explicitDestinationSubpane.add(new JLabel(messageBundle.getString(
                "viewer.utilityPane.action.dialog.goto.type.label")));
        implicitDestTypeComboBox = buildImplicitDestTypes();
        implicitDestTypeComboBox.addItemListener(this);
        explicitDestinationSubpane.add(implicitDestTypeComboBox);
        // page assignment
        explicitDestinationSubpane.add(new JLabel(messageBundle.getString(
                "viewer.utilityPane.action.dialog.goto.page.label")));
        pageNumberTextField = buildDocumentPageNumbers();
        explicitDestinationSubpane.add(pageNumberTextField);
        // top position
        explicitDestinationSubpane.add(new JLabel(messageBundle.getString(
                "viewer.utilityPane.action.dialog.goto.top.label")));
        topTextField = buildFloatTextField();
        explicitDestinationSubpane.add(topTextField);
        // bottom position
        explicitDestinationSubpane.add(new JLabel(messageBundle.getString(
                "viewer.utilityPane.action.dialog.goto.bottom.label")));
        bottomTextField = buildFloatTextField();
        explicitDestinationSubpane.add(bottomTextField);
        // left position
        explicitDestinationSubpane.add(new JLabel(messageBundle.getString(
                "viewer.utilityPane.action.dialog.goto.left.label")));
        leftTextField = buildFloatTextField();
        explicitDestinationSubpane.add(leftTextField);
        // right position
        explicitDestinationSubpane.add(new JLabel(messageBundle.getString(
                "viewer.utilityPane.action.dialog.goto.right.label")));
        rightTextField = buildFloatTextField();
        explicitDestinationSubpane.add(rightTextField);
        // zoom level
        explicitDestinationSubpane.add(new JLabel(messageBundle.getString(
                "viewer.utilityPane.action.dialog.goto.zoom.label")));
        zoomTextField = buildFloatTextField();
        explicitDestinationSubpane.add(zoomTextField);
        // filler
        explicitDestinationSubpane.add(new JLabel());
        explicitDestinationSubpane.add(new JLabel());
        // put the explicit destinations fields into one container.
        JPanel pageNumberPane = new JPanel(new BorderLayout(5, 5));
        implicitDestination = new JRadioButton(messageBundle.getString(
                "viewer.utilityPane.action.dialog.goto.explicitDestination.title"), true);
        implicitDestination.addItemListener(this);
        pageNumberPane.add(implicitDestination, BorderLayout.NORTH);
        pageNumberPane.add(explicitDestinationSubpane, BorderLayout.CENTER);

        /**
         * Setup Named destinations
         */
        JPanel namedDestSubpane = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        namedDestSubpane.setBorder(new EmptyBorder(0, 40, 0, 0));
        // name of named dest..
        namedDestSubpane.add(new JLabel(messageBundle.getString(
                "viewer.utilityPane.action.dialog.goto.name.label")));
        destinationName = new JLabel(EMPTY_DESTINATION);
        namedDestSubpane.add(destinationName);
        // browse button to show named destination tree.
        viewNamedDesButton = new JButton(messageBundle.getString(
                "viewer.utilityPane.action.dialog.goto.browse"));
        viewNamedDesButton.addActionListener(this);
        namedDestSubpane.add(viewNamedDesButton);
        // put the named destination into one container.
        JPanel namedDestPane = new JPanel(new BorderLayout(5, 5));
        namedDestination =
                new JRadioButton(messageBundle.getString(
                        "viewer.utilityPane.action.dialog.goto.nameDestination.title"), false);
        namedDestPane.add(namedDestination, BorderLayout.NORTH);
        namedDestPane.add(namedDestSubpane, BorderLayout.CENTER);

        // Button group to link the two panels toggled functionality.
        ButtonGroup actionButtonGroup = new ButtonGroup();
        actionButtonGroup.add(implicitDestination);
        actionButtonGroup.add(namedDestination);

        // ok button to save changes and close the dialog.
        okButton = new JButton(messageBundle.getString("viewer.button.ok.label"));
        okButton.setMnemonic(messageBundle.getString("viewer.button.ok.mnemonic").charAt(0));
        okButton.addActionListener(this);
        cancelButton = new JButton(messageBundle.getString("viewer.button.cancel.label"));
        cancelButton.setMnemonic(messageBundle.getString("viewer.button.cancel.mnemonic").charAt(0));
        cancelButton.addActionListener(this);
        // panel for OK and cancel
        JPanel okCancelPanel = new JPanel(new FlowLayout());
        okCancelPanel.add(okButton);
        okCancelPanel.add(cancelButton);

        // add values
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;

        addGB(goToActionPanel, pageNumberPane, 0, 0, 1, 1);
        addGB(goToActionPanel, namedDestPane, 0, 1, 1, 1);

        constraints.insets = new Insets(15, 5, 5, 5);
        constraints.anchor = GridBagConstraints.CENTER;
        addGB(goToActionPanel, okCancelPanel, 0, 2, 1, 1);

        this.getContentPane().add(goToActionPanel);

        setSize(new Dimension(500, 325));
        setLocationRelativeTo(controller.getViewerFrame());

    }

    private Object parseFloat(String fieldValue) {
        try {
            return Float.parseFloat(fieldValue);
        } catch (NumberFormatException e) {
            // empty on purpose
        }
        return "null";
    }

    private void applyTypeValues(Destination dest, Name type) {
        if (Destination.TYPE_XYZ.equals(type)) {
            leftTextField.setText(String.valueOf(dest.getLeft()));
            topTextField.setText(String.valueOf(dest.getTop()));
            zoomTextField.setText(String.valueOf(dest.getZoom()));
        } else if (Destination.TYPE_FIT.equals(type)) {
            // nothing to do
        } else if (Destination.TYPE_FITH.equals(type)) {
            // get top value
            topTextField.setText(String.valueOf(dest.getTop()));
        } else if (Destination.TYPE_FITV.equals(type)) {
            // get left value
            leftTextField.setText(String.valueOf(dest.getLeft()));
        } else if (Destination.TYPE_FITR.equals(type)) {
            // left, bottom right and top.
            leftTextField.setText(String.valueOf(dest.getLeft()));
            rightTextField.setText(String.valueOf(dest.getRight()));
            topTextField.setText(String.valueOf(dest.getTop()));
            bottomTextField.setText(String.valueOf(dest.getBottom()));
        } else if (Destination.TYPE_FITB.equals(type)) {
            // nothing to do.
        } else if (Destination.TYPE_FITH.equals(type)) {
            // get the top
            topTextField.setText(String.valueOf(dest.getTop()));
        } else if (Destination.TYPE_FITBV.equals(type)) {
            // get the left
            leftTextField.setText(String.valueOf(dest.getLeft()));
        }
    }

    /**
     * Utility for building input field that handles page number limits for the
     * current document.
     *
     * @return pageNumber text field with listeners for validation.
     */
    private JTextField buildDocumentPageNumbers() {
        final JTextField textField = new JTextField();
        textField.setInputVerifier(new PageNumberTextFieldInputVerifier());
        textField.addKeyListener(new PageNumberTextFieldKeyListener());
        textField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                Object src = e.getSource();
                if (src == null)
                    return;
                if (src == textField) {
                    String fieldValue = textField.getText();
                    int currentValue = Integer.parseInt(fieldValue);
                    int maxValue = controller.getDocument().getNumberOfPages();
                    if (currentValue > maxValue)
                        textField.setText(String.valueOf(maxValue));
                }
            }
        });
        // start off with page 1.
        textField.setText("1");
        return textField;
    }

    /**
     * Utility for building input field that handles page number limits for the
     * current document.
     *
     * @return pageNumber text field with listeners for validation.
     */
    private JTextField buildFloatTextField() {
        final JTextField textField = new JTextField();
        textField.setInputVerifier(new FloatTextFieldInputVerifier());
        textField.addKeyListener(new FloatTextFieldKeyListener());
        textField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                Object src = e.getSource();
                if (src == null)
                    return;
                if (src == textField) {
                    String fieldValue = textField.getText();
                    float currentValue = Float.parseFloat(fieldValue);
                    textField.setText(String.valueOf(currentValue));
                }
            }
        });

        return textField;
    }

    /**
     * Builds destination types combo box.
     *
     * @return combo box of possilbe implict destination types.
     */
    private JComboBox buildImplicitDestTypes() {
        ValueLabelItem[] destTypes = new ValueLabelItem[]{
                new ValueLabelItem(Destination.TYPE_XYZ,
                        messageBundle.getString(
                                "viewer.utilityPane.action.dialog.goto.type.xyz.label")),
                new ValueLabelItem(Destination.TYPE_FITH,
                        messageBundle.getString(
                                "viewer.utilityPane.action.dialog.goto.type.fith.label")),
                new ValueLabelItem(Destination.TYPE_FITR,
                        messageBundle.getString(
                                "viewer.utilityPane.action.dialog.goto.type.fitr.label")),
                new ValueLabelItem(Destination.TYPE_FIT,
                        messageBundle.getString(
                                "viewer.utilityPane.action.dialog.goto.type.fit.label")),
                new ValueLabelItem(Destination.TYPE_FITB,
                        messageBundle.getString(
                                "viewer.utilityPane.action.dialog.goto.type.fitb.label")),
                new ValueLabelItem(Destination.TYPE_FITBH,
                        messageBundle.getString(
                                "viewer.utilityPane.action.dialog.goto.type.fitbh.label")),
                new ValueLabelItem(Destination.TYPE_FITBV,
                        messageBundle.getString(
                                "viewer.utilityPane.action.dialog.goto.type.fitbv.label")),
                new ValueLabelItem(Destination.TYPE_FITBV,
                        messageBundle.getString(
                                "viewer.utilityPane.action.dialog.goto.type.fitbv.label")),
        };
        return new JComboBox(destTypes);
    }

    /**
     * Gridbag constructor helper
     * @param layout panel to invoke layout on 
     * @param component component to add to grid
     * @param x         row
     * @param y         col
     * @param rowSpan   rowspan value
     * @param colSpan   colspan value
     */
    private void addGB(JPanel layout, Component component,
                       int x, int y,
                       int rowSpan, int colSpan) {
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = rowSpan;
        constraints.gridheight = colSpan;
        layout.add(component, constraints);
    }

    /**
     * Enables fields for destinations
     *
     * @param isImplictDestSelected true enables all implicit destination fields,
     *                              false enables all named destinations
     */
    private void implicitDestinationFieldsEnabled(boolean isImplictDestSelected) {

        // radio selection
        implicitDestination.setSelected(isImplictDestSelected);
        namedDestination.setSelected(!isImplictDestSelected);

        // implicit dest fields
        pageNumberTextField.setEnabled(isImplictDestSelected);
        implicitDestTypeComboBox.setEnabled(isImplictDestSelected);
        leftTextField.setEnabled(isImplictDestSelected);
        topTextField.setEnabled(isImplictDestSelected);
        zoomTextField.setEnabled(isImplictDestSelected);
//        viewPositionButton.setEnabled(isImplictDestSelected);
        // named fields
        destinationName.setEnabled(!isImplictDestSelected);
        viewNamedDesButton.setEnabled(!isImplictDestSelected);
    }

    /**
     * Clears fields for destinations
     *
     * @param isImplictDestSelected true clears all implicit destination fields,
     *                              false clears all named destinations
     */
    private void clearImplicitDestinations(boolean isImplictDestSelected) {
        // implicit
        if (!isImplictDestSelected) {
            pageNumberTextField.setText("");
            implicitDestTypeComboBox.setSelectedIndex(-1);
            leftTextField.setText("");
            topTextField.setText("");
            zoomTextField.setText("");
        }
        // named
        else {
            destinationName.setText(EMPTY_DESTINATION);
        }
    }

    /**
     * Assigns the fit type and applies the field enabled state logic for the
     * respective view type.
     *
     * @param fitType destination fit type to apply
     */
    private void enableFitTypeFields(Name fitType) {
        if (fitType.equals(Destination.TYPE_FIT) ||
                fitType.equals(Destination.TYPE_FITB)) {
            // disable all fields
            setFitTypesEnabled(false, false, false, false, false);
        }
        // just top enabled
        else if (fitType.equals(Destination.TYPE_FITH) ||
                fitType.equals(Destination.TYPE_FITBH)) {
            setFitTypesEnabled(true, false, false, false, false);
        }
        // Just left enabled
        else if (fitType.equals(Destination.TYPE_FITV) ||
                fitType.equals(Destination.TYPE_FITBV)) {
            setFitTypesEnabled(false, false, true, false, false);
        }
        // special xyz case
        else if (fitType.equals(Destination.TYPE_XYZ)) {
            setFitTypesEnabled(true, false, true, false, true);
        }
        // special FitR
        else if (fitType.equals(Destination.TYPE_FITR)) {
            setFitTypesEnabled(true, true, true, true, false);
        }
    }

    /**
     * Sets the enabled state of the input fields associated with implicit
     * destination fit types.
     *
     * @param top    top coordinat input field.
     * @param bottom bottom coordinat input field.
     * @param left   left coordinat input field.
     * @param right  right coordinat input field.
     * @param zoom   view port zoom value field.
     */
    private void setFitTypesEnabled(boolean top, boolean bottom,
                                    boolean left, boolean right, boolean zoom) {
        topTextField.setEnabled(top);
        bottomTextField.setEnabled(bottom);
        leftTextField.setEnabled(left);
        rightTextField.setEnabled(right);
        zoomTextField.setEnabled(zoom);
    }

    /**
     * Apply selected values to combo box. If a match can not be found
     * no values is applied.
     *
     * @param comboBox combo box to update
     * @param value    value to assing.
     */
    private void applySelectedValue(JComboBox comboBox, Object value) {
        comboBox.removeItemListener(this);
        ValueLabelItem currentItem;
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            currentItem = (ValueLabelItem) comboBox.getItemAt(i);
            if (currentItem.getValue().equals(value)) {
                comboBox.setSelectedIndex(i);
                break;
            }
        }
        comboBox.addItemListener(this);
    }

    /**
     * Class to associate with a JComboBox
     * Used to allow us to display different text to the user than we set in the backend
     */
    class ValueLabelItem {
        private Object value;
        private String label;

        public ValueLabelItem(Object value, String label) {
            this.value = value;
            this.label = label;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String toString() {
            return label;
        }
    }
}