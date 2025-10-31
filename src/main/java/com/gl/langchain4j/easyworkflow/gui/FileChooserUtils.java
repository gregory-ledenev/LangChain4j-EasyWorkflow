/*
 *
 * Copyright 2025 Gregory Ledenev (gregory.ledenev37@gmail.com)
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * /
 */

package com.gl.langchain4j.easyworkflow.gui;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * A utility class for handling file choosers (both Swing JFileChooser and AWT FileDialog)
 * and common file operations like reading, writing, and copying.
 */
public class FileChooserUtils {
    public boolean useNativeFileChooser = false;
    protected JFileChooser fileChooser;
    protected FileFilter[] choosableFileFilters;
    protected FileFilter defaultChoosableFileFilter;
    protected Component owner;
    protected FileDialog fileDialog;

    /**
     * Default constructor.
     */
    public FileChooserUtils() {
    }

    /**
     * Constructs a FileChooserUtils with a specified owner component.
     *
     * @param oOwner The component that is the owner of the file chooser dialog.
     */
    public FileChooserUtils(Component oOwner) {
        owner = oOwner;
    }

    /**
     * Constructs a FileChooserUtils with an array of choosable file filters.
     * Each inner array should contain two strings: the description and the extensions (e.g., {"Text files", "txt"}).
     *
     * @param choosableFileFilters A 2D array of strings representing the file filters.
     */
    public FileChooserUtils(String[][] choosableFileFilters) {
        setChoosableFileFilters(choosableFileFilters);
    }

    /**
     * Constructs a FileChooserUtils with an array of FileFilter objects.
     *
     * @param choosableFileFilters An array of FileFilter objects.
     */
    public FileChooserUtils(FileFilter[] choosableFileFilters) {
        this.choosableFileFilters = choosableFileFilters;
    }

    /**
     * Returns a formatted string for a file overwrite prompt.
     *
     * @param file The file that already exists.
     * @return A string asking the user if they want to overwrite the file.
     */
    public static String getFileOverwritePrompt(File file) {
        return "File %s already exists.\nWould you like to overwrite it?".formatted(file.getAbsoluteFile());
    }

    /**
     * Returns a formatted string indicating that a file is read-only.
     *
     * @param file The read-only file.
     * @return A string message.
     */
    public static String getFileReadOnlyMessage(File file) {
        return "File %s is read-only.".formatted(file.getAbsoluteFile());
    }

    /**
     * Returns a formatted string indicating that a file cannot be read.
     *
     * @param file The file that cannot be read.
     * @return A string message.
     */
    public static String getFileCantReadMessage(File file) {
        return "File %s can't be read.".formatted(file.getAbsoluteFile());
    }

    /**
     * Copies the content of a source file to a destination file.
     *
     * @param source The source file to copy from.
     * @param destination The destination file to copy to.
     * @throws IOException If an I/O error occurs or the source file is not readable.
     */
    public static void copyFile(File source, File destination) throws IOException {
        if (!source.canRead()) {
            throw new IOException("Source file is not readable: " + source.getAbsolutePath());
        }
        try (FileInputStream inputStream = new FileInputStream(source);
             FileOutputStream outputStream = new FileOutputStream(destination)) {
            byte[] buffer = new byte[32768];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * Returns a BufferedWriter for the given file.
     *
     * @param aFile The file to write to.
     * @return A BufferedWriter for the file.
     * @throws FileNotFoundException If the file exists but is a directory rather than a regular file, or cannot be opened for any other reason.
     */
    public static BufferedWriter getFileBufferedWriter(File aFile)
            throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(aFile)));
    }

    /**
     * Returns a BufferedReader for the given file.
     *
     * @param aFile The file to read from.
     * @return A BufferedReader for the file.
     * @throws FileNotFoundException If the file does not exist, is a directory rather than a regular file, or for some other reason cannot be opened for reading.
     */
    public static BufferedReader getFileBufferedReader(File aFile)
            throws FileNotFoundException {
        return new BufferedReader(new InputStreamReader(new FileInputStream(aFile)));
    }

    /**
     * Reads the content of a file and returns it as a list of strings, where each string is a line from the file.
     *
     * @param file The file to read.
     * @return A List of strings, each representing a line from the file.
     * @throws IOException If an I/O error occurs.
     */
    public static List<String> getFileContentAsList(File file)
            throws IOException {
        List<String> result = new ArrayList<>();

        try (BufferedReader reader = getFileBufferedReader(file)) {
            String lineStr;
            while ((lineStr = reader.readLine()) != null) {
                result.add(lineStr);
            }
        }
        return result;
    }

    /**
     * Saves a list of objects to a file, writing each object's string representation on a new line.
     *
     * @param data The list of objects to save.
     * @param file The file to write to.
     * @throws Exception If an error occurs during file writing.
     */
    public static void saveListAsFile(List<?> data, File file)
            throws Exception {
        try (BufferedWriter writer = getFileBufferedWriter(file)) {

            for (Object datum : data) {
                writer.write(datum.toString());
                writer.newLine();
            }
        }
    }

    /**
     * Returns the default choosable file filter. If no default is explicitly set,
     * and there are choosable file filters, the first one is returned.
     *
     * @return The default FileFilter.
     */
    public FileFilter getDefaultChoosableFileFilter() {
        FileFilter result = defaultChoosableFileFilter;
        if (result == null && choosableFileFilters != null && choosableFileFilters.length > 0)
            result = choosableFileFilters[0];

        return result;
    }

    /**
     * Sets the default choosable file filter.
     *
     * @param defaultChoosableFileFilter The FileFilter to set as default.
     */
    public void setDefaultChoosableFileFilter(FileFilter defaultChoosableFileFilter) {
        this.defaultChoosableFileFilter = defaultChoosableFileFilter;
    }

    /**
     * Returns the array of choosable file filters.
     *
     * @return An array of FileFilter objects.
     */
    public FileFilter[] getChoosableFileFilters() {
        return choosableFileFilters;
    }

    /**
     * Sets the array of choosable file filters.
     *
     * @param choosableFileFilters An array of FileFilter objects.
     */
    public void setChoosableFileFilters(FileFilter[] choosableFileFilters) {
        this.choosableFileFilters = choosableFileFilters;
    }

    /**
     * Sets the choosable file filters from a 2D array of strings.
     * Each inner array should contain two strings: the description and the extensions (e.g., {"Text files", "txt"}).
     *
     * @param aChoosableFileFilters A 2D array of strings representing the file filters.
     */
    public void setChoosableFileFilters(String[][] aChoosableFileFilters) {
        FileFilter[] choosableFileFilters = new FileFilter[aChoosableFileFilters.length];
        for (int i = 0; i < aChoosableFileFilters.length; i++)
            choosableFileFilters[i] = new GenericFileFilter(aChoosableFileFilters[i][0], aChoosableFileFilters[i][1]);
        setChoosableFileFilters(choosableFileFilters);
    }

    /**
     * Returns the JFileChooser instance, creating it if it doesn't exist.
     * Adds all choosable file filters to the file chooser.
     *
     * @return The JFileChooser instance.
     */
    public JFileChooser getFileChooser() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            for (int i = 0; choosableFileFilters != null && i < choosableFileFilters.length; i++)
                fileChooser.addChoosableFileFilter(choosableFileFilters[i]);
            fileChooser.updateUI();
        }
        return fileChooser;
    }

    protected boolean isAcceptAllFileFilter(FileFilter fileFilter) {
        boolean result = true;
        for (int i = 0; choosableFileFilters != null && i < choosableFileFilters.length; i++) {
            if (choosableFileFilters[i] == fileFilter) {
                result = false;
                break;
            }
        }
        return result;
    }

    /**
     * Displays a "Save" file dialog and returns the absolute path of the selected file.
     *
     * @param fileName The initial file name to display in the dialog.
     * @return The absolute path of the selected file, or null if the dialog was cancelled.
     */
    public String chooseFileToSave(String fileName) {
        String result = null;

        JFileChooser fileChooser = getFileChooser();
        fileChooser.updateUI();
        fileChooser.setFileFilter(getDefaultChoosableFileFilter());
        fileChooser.setSelectedFile(new File(fileName));
        if (fileChooser.showSaveDialog(getTopLevelOwner()) == JFileChooser.APPROVE_OPTION) {
            result = appendDefaultExtension(fileChooser.getSelectedFile(), fileChooser).getAbsolutePath();
        }
        return result;
    }

    protected File appendDefaultExtension(File file, JFileChooser fileChooser) {
        FileFilter fileFilter = fileChooser.getFileFilter();

        File result = file;

        if (fileFilter instanceof GenericFileFilter) {
            if (!result.getAbsolutePath().contains(".") && !isAcceptAllFileFilter(fileFilter))
                result = new File(result.getAbsolutePath() + "." +
                        ((GenericFileFilter) fileFilter).getAcceptableExtensions()[0]);
        }

        return result;
    }

    /**
     * Displays a "Save" file dialog, allowing the user to choose a file to save.
     * If the file already exists, it prompts the user for overwrite confirmation.
     *
     * @param file The initial file to display in the dialog.
     * @return The selected File object, or null if the dialog was cancelled or the user chose not to overwrite.
     */
    public File chooseFileToSave(File file) {
        return chooseFileToSave(file, false);
    }

    /**
     * Displays a native "Save" file dialog (java.awt.FileDialog) and returns the selected file.
     * If the file already exists, it prompts the user for overwrite confirmation.
     *
     * @param file The initial file to display in the dialog.
     * @param forceChoose If true, the dialog will always be shown, even if a file is already provided.
     * @return The selected File object, or null if the dialog was cancelled or the user chose not to overwrite.
     */
    protected File chooseFileToSave_FileDialog(File file, boolean forceChoose) {
        File result = file;

        FileDialog dialog = getFileDialog();
        dialog.setMode(FileDialog.SAVE);

        if (getChoosableFileFilters() != null &&
                getChoosableFileFilters().length > 0
                && getChoosableFileFilters()[0] instanceof FilenameFilter)
            dialog.setFilenameFilter((FilenameFilter) getChoosableFileFilters()[0]);

        if (file == null) {
            dialog.setFile(null);
            dialog.setDirectory(null);
        } else {
            dialog.setFile(file.getName());
            dialog.setDirectory(file.getParent());
        }

        if (getTitle(owner) != null)
            dialog.setTitle(getTitle(owner));

        Component owner = getTopLevelOwner();
        String title = getTitle(owner);

        all:
        while (true) {
            if (result == null || forceChoose) {
                dialog.setVisible(true);
                if (dialog.getFile() != null) {
                    result = new File(dialog.getDirectory(), dialog.getFile());
                } else {
                    result = null;
                    break;
                }
            }
            if (result.exists()) {
                if (!result.canWrite()) {
                    JOptionPane.showMessageDialog(owner, getFileReadOnlyMessage(result), title,
                            JOptionPane.ERROR_MESSAGE);
                    result = null; // Force re-selection
                } else {
                    if (UISupport.isMac() || JOptionPane.showConfirmDialog(owner, getFileOverwritePrompt(result), title,
                            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        break;
                    }
                }
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Displays a "Save" file dialog (native or Swing one according to {@code useNativeFileChooser}), allowing the user
     * to choose a file to save. If the file already exists, it prompts the user for overwrite confirmation.
     *
     * @param file        The initial file to display in the dialog.
     * @param forceChoose If true, the dialog will always be shown, even if a file is already provided.
     * @return The selected File object, or null if the dialog was cancelled or the user chose not to overwrite.
     */
    public File chooseFileToSave(File file, boolean forceChoose) {
        return isUseNativeFileChooser() ?
                chooseFileToSave_FileDialog(file, forceChoose) :
                chooseFileToSave_FileChooser(file, forceChoose);
    }

    /**
     * Displays a Swing "Save" file dialog, allowing the user to choose a file to save.
     * If the file already exists, it prompts the user for overwrite confirmation.
     *
     * @param file The initial file to display in the dialog.
     * @param forceChoose If true, the dialog will always be shown, even if a file is already provided.
     * @return The selected File object, or null if the dialog was cancelled or the user chose not to overwrite.
     */
    protected File chooseFileToSave_FileChooser(File file, boolean forceChoose) {
        File result = file;

        JFileChooser fileChooser = getFileChooser();
        fileChooser.updateUI();

        setupDefaultFileFilter(file, fileChooser);

        fileChooser.setSelectedFile(file.getAbsoluteFile());
        Component owner = getTopLevelOwner();
        String title = getTitle(owner);
        all:
        while (true) {
            if (result == null || forceChoose) {
                if (fileChooser.showSaveDialog(owner) == JFileChooser.APPROVE_OPTION) {
                    result = appendDefaultExtension(fileChooser.getSelectedFile(), fileChooser);
                } else {
                    result = null;
                    break;
                }
            }

            if (result != null) {
                if (result.exists()) {
                    if (!result.canWrite()) {
                        JOptionPane.showMessageDialog(owner, getFileReadOnlyMessage(result), title,
                                JOptionPane.ERROR_MESSAGE);
                    } else {
                        switch (JOptionPane.showConfirmDialog(owner, getFileOverwritePrompt(result), title,
                                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE)) {
                            case JOptionPane.YES_OPTION:
                                break all;
                            case JOptionPane.NO_OPTION:
                                result = null;
                                break;
                            default:
                                result = null;
                                break all;
                        }
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Returns the default choosable file filter that accepts the given file.
     *
     * @param file The file to check against the filters.
     * @return A FileFilter that accepts the file, or null if no such filter is found.
     */
    public FileFilter getDefaultChoosableFileFilter(File file) {
        FileFilter result = null;

        for (int i = 0; file != null && choosableFileFilters != null && result == null &&
                i < choosableFileFilters.length; i++) {
            if (choosableFileFilters[i].accept(file))
                result = choosableFileFilters[i];
        }

        return result;
    }

    /**
     * Returns the owner component of this file chooser.
     *
     * @return The owner component.
     */
    public Component getOwner() {
        return owner;
    }

    /**
     * Sets the owner component for this file chooser.
     *
     * @param owner The component to set as the owner.
     */
    public void setOwner(Component owner) {
        this.owner = owner;
    }

    protected Component getTopLevelOwner() {
        Component result = owner;

        if (result == null) {
            Frame[] frames = JFrame.getFrames();
            for (int i = 0; result == null && frames != null && i < frames.length; i++) {
                if (frames[i].isActive())
                    result = frames[i];
            }
        }

        return result;
    }

    protected String getTitle(Component owner) {
        String result = null;

        if (owner instanceof Frame)
            result = ((Frame) owner).getTitle();
        else if (owner instanceof Dialog)
            result = ((Dialog) owner).getTitle();

        return result;
    }

    /**
     * Displays an "Open" file dialog that allows multiple file selections and returns the selected files.
     *
     * @param file The initial directory or file to display in the dialog.
     * @return An array of selected File objects, or null if the dialog was cancelled.
     */
    public File[] chooseFilesToOpen(File file) {
        File[] result = null;

        JFileChooser fileChooser = getFileChooser();
        fileChooser.updateUI();

        setupDefaultFileFilter(file, fileChooser);

        fileChooser.setSelectedFile(file);
        fileChooser.setMultiSelectionEnabled(true);
        if (fileChooser.showOpenDialog(getTopLevelOwner()) == JFileChooser.APPROVE_OPTION)
            result = fileChooser.getSelectedFiles();

        return result;
    }

    /**
     * Displays an "Open" file dialog and returns the selected file.
     * The choice between a native file dialog and a Swing JFileChooser depends on the {@code useNativeFileChooser} flag.
     *
     * @param file The initial directory or file to display in the dialog.
     * @return The selected File object, or null if the dialog was cancelled.
     */
    public File chooseFileToOpen(File file) {
        return isUseNativeFileChooser() ? chooseFileToOpen_FileDialog(file) : chooseFileToOpen_FileChooser(file);
    }

    /**
     * Displays a native "Open" file dialog (java.awt.FileDialog) and returns the selected file.
     *
     * @param file The initial directory or file to display in the dialog.
     * @return The selected File object, or null if the dialog was cancelled.
     */
    public File chooseFileToOpen_FileDialog(File file) {
        File result = null;

        FileDialog dialog = getFileDialog();

        if (getChoosableFileFilters() != null &&
                getChoosableFileFilters().length > 0
                && getChoosableFileFilters()[0] instanceof FilenameFilter)
            dialog.setFilenameFilter((FilenameFilter) getChoosableFileFilters()[0]);

        if (file == null) {
            dialog.setFile(null);
        } else {
            dialog.setFile(file.getName());
            dialog.setDirectory(file.getParent());
        }

        if (getTitle(owner) != null)
            dialog.setTitle(getTitle(owner));

        dialog.setVisible(true);
        if (dialog.getFile() != null) {
            result = new File(dialog.getDirectory(), dialog.getFile());
        }
        return result;
    }

    /**
     * Displays a Swing "Open" file dialog (javax.swing.JFileChooser) and returns the selected file.
     * Handles cases where the selected file cannot be read.
     *
     * @param file The initial directory or file to display in the dialog.
     * @return The selected File object, or null if the dialog was cancelled or the file cannot be read.
     */
    public File chooseFileToOpen_FileChooser(File file) {
        File result = null;

        JFileChooser fileChooser = getFileChooser();
        fileChooser.updateUI();

        setupDefaultFileFilter(file, fileChooser);

        fileChooser.setSelectedFile(file);
        Component owner = getTopLevelOwner();
        String title = getTitle(owner);
        all:
        while (true) {
            if (fileChooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
                result = fileChooser.getSelectedFile();
            } else { // If the user cancels the dialog
                break;
            }

            if (result != null) {
                if (!result.canRead()) {
                    JOptionPane.showMessageDialog(owner, getFileCantReadMessage(result), owner != null ? title : "Error",
                            JOptionPane.ERROR_MESSAGE);
                    result = null;
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return result;
    }

    private void setupDefaultFileFilter(File file, JFileChooser fileChooser) {
        FileFilter defaultFilter = getDefaultChoosableFileFilter(file);
        if (defaultFilter == null)
            defaultFilter = getDefaultChoosableFileFilter();
        if (defaultFilter != null)
            fileChooser.setFileFilter(defaultFilter);
    }

    /**
     * Checks if the native file chooser is currently in use.
     *
     * @return true if the native file chooser is used, false otherwise.
     */
    public boolean isUseNativeFileChooser() {
        return useNativeFileChooser;
    }

    /**
     * Sets whether to use the native file chooser or the Swing JFileChooser.
     * If the value changes, it resets the internal file chooser and file dialog instances.
     *
     * @param value true to use the native file chooser, false to use the Swing JFileChooser.
     */
    public void setUseNativeFileChooser(boolean value) {
        if (this.useNativeFileChooser != value) {
            this.useNativeFileChooser = value;
            fileChooser = null;
            if (fileDialog != null) {
                fileDialog.dispose();
                fileDialog = null;
            }
        }
    }

    /**
     * Returns the FileDialog instance, creating it if it doesn't exist or if the owner window has changed.
     *
     * @return The FileDialog instance.
     */
    public FileDialog getFileDialog() {
        Window w = owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        if (fileDialog == null || fileDialog.getOwner() != w) {
            if (fileDialog != null) {
                fileDialog.dispose();
                fileDialog = null;
            }
            fileDialog = w instanceof Frame ?
                    new FileDialog((Frame) w, getTitle(owner), FileDialog.LOAD) :
                    new FileDialog((Dialog) w, getTitle(owner), FileDialog.LOAD);
        }

        return fileDialog;
    }

    /**
     * A generic file filter that extends {@link FileFilter} and implements {@link FilenameFilter}.
     * It allows filtering files based on their extensions.
     */
    public static class GenericFileFilter
            extends FileFilter implements Cloneable, Serializable, FilenameFilter {

        /** The serial version UID for serialization. */
        @Serial
        private static final long serialVersionUID = 1;

        /**
         * The delimiter used to separate multiple file extensions in a single string.
         */
        public final static String FILE_EXTENSIONS_DELIMITER = ";";
        /**
         * The starting bracket character used in the file filter description to enclose extensions.
         */
        public final static String FILE_DESCRIPTION_START_BRACKET = "(";

        /**
         * A predefined {@link FileFilter} for image files, including JPEG, JPG, GIF, and PNG extensions.
         */
        public final static FileFilter FILE_FILTER_IMAGES =
                new GenericFileFilter("Image files", new String[]{"jpeg, jpg, gif, png"});
        /**
         * A predefined {@link FileFilter} for audio files, including WAV, AU, AIFF, RMF, and MID extensions.
         */
        public final static FileFilter FILE_FILTER_AUDIO =
                new GenericFileFilter("Audio files", new String[]
                        {"wav", "au", "aiff", "rmf", "mid"});
        /**
         * A predefined {@link FileFilter} for HTML files, including HTML and HTM extensions.
         */
        public final static FileFilter FILE_FILTER_HTML =
                new GenericFileFilter("HTML files", new String[]
                        {"html", "htm"});
        /**
         * A predefined {@link FileFilter} for XML files, including the XML extension.
         */
        public final static FileFilter FILE_FILTER_XML =
                new GenericFileFilter("XML files", new String[]
                        {"xml"});
        /**
         * A predefined {@link FileFilter} for text files, including the TXT extension.
         */
        public final static FileFilter FILE_FILTER_TEXT =
                new GenericFileFilter("Text files", new String[]
                        {"txt"});
        protected HashSet<String> acceptableExtensions = new HashSet<>();
        protected String description;

        /**
         * Constructs a GenericFileFilter with a description and an array of acceptable extensions.
         *
         * @param description The human-readable description of the filter.
         * @param extensions An array of file extensions (e.g., {"txt", "log"}).
         */
        public GenericFileFilter(String description, String[] extensions) {
            setAcceptableExtensions(extensions);
            setDescription(description);
        }

        /**
         * Constructs a GenericFileFilter with a description and a single string of acceptable extensions,
         * delimited by {@link #FILE_EXTENSIONS_DELIMITER}.
         * @param description The human-readable description of the filter.
         * @param extensions A string of file extensions (e.g., "txt;log").
         */
        public GenericFileFilter(String description, String extensions) {
            setAcceptableExtensions(extensions);
            setDescription(description);
        }

        protected static String getFileExtension(File file) {
            String fileName = file.getName();
            int ind = fileName.lastIndexOf('.');
            if (ind >= 0)
                return fileName.substring(ind + 1).toLowerCase();
            return null;
        }

        /**
         * Returns the human-readable description of this filter.
         *
         * @return The description string.
         */
        public String getDescription() {
            return description;
        }

        /**
         * Sets the description for this file filter.
         * The description will include the acceptable extensions in parentheses.
         * @param description The base description string.
         */
        protected void setDescription(String description) {
            if (description != null) {
                StringBuilder stringBuilder = new StringBuilder(description);
                if (!acceptableExtensions.isEmpty()) {
                    stringBuilder.append(" " + FILE_DESCRIPTION_START_BRACKET);

                    Iterator<String> extIterator = acceptableExtensions.iterator();
                    while (extIterator.hasNext()) {
                        stringBuilder.append("*.").append(extIterator.next());
                        if (extIterator.hasNext())
                            stringBuilder.append(FILE_EXTENSIONS_DELIMITER + " ");
                    }
                    stringBuilder.append(")");
                }
                this.description = stringBuilder.toString();
            }
        }

        /**
         * Returns an array of acceptable file extensions for this filter.
         *
         * @return An array of strings, each representing an acceptable file extension.
         */
        public String[] getAcceptableExtensions() {
            int i = 0;
            String[] result = new String[acceptableExtensions.size()];
            for (String aAcceptableExtension : acceptableExtensions) {
                result[i++] = aAcceptableExtension;
            }
            return result;
        }

        /**
         * Sets the acceptable file extensions for this filter from an array of strings.
         * Each extension is converted to lowercase and trimmed.
         *
         * @param acceptableExtensions An array of file extension strings.
         */
        protected void setAcceptableExtensions(String[] acceptableExtensions) {
            if (acceptableExtensions != null) {
                for (int i = 0; i < acceptableExtensions.length; i++)
                    this.acceptableExtensions.add(acceptableExtensions[i].toLowerCase().trim());
            }
        }

        /**
         * Sets the acceptable file extensions for this filter from a single string,
         * which is split by {@link #FILE_EXTENSIONS_DELIMITER}.
         *
         * @param acceptableExtensions A string containing file extensions separated by {@link #FILE_EXTENSIONS_DELIMITER}.
         */
        protected void setAcceptableExtensions(String acceptableExtensions) {
            if (acceptableExtensions != null)
                setAcceptableExtensions(acceptableExtensions.split(FILE_EXTENSIONS_DELIMITER));
        }

        /**
         * Determines whether the given file is accepted by this filter.
         * Directories are always accepted. Files are accepted if their extension matches one of the acceptable extensions.
         *
         * @param file The file to check.
         * @return true if the file is accepted, false otherwise.
         */
        public boolean accept(File file) {
            if (file.isDirectory())
                return true;

            String fileExt = getFileExtension(file);
            if (fileExt != null)
                return acceptableExtensions.contains(fileExt);

            return false;
        }

        /**
         * Determines whether a file with the given name in the specified directory is accepted by this filter.
         * This method is part of the {@link FilenameFilter} interface.
         *
         * @param dir The directory in which the file was found.
         * @param name The name of the file.
         * @return true if the file is accepted, false otherwise.
         */
        public boolean accept(File dir, String name) {
            return accept(new File(dir, name));
        }

        /**
         * Creates and returns a copy of this object.
         *
         * @return A clone of this instance.
         */
        public Object clone() {
            try {
                GenericFileFilter result = (GenericFileFilter) super.clone();
                result.acceptableExtensions = new HashSet<>(acceptableExtensions);
                return result;
            } catch (CloneNotSupportedException ex) {
                return null;
            }
        }
    }
}