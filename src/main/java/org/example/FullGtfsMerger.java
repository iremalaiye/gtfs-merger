package org.example;

import java.io.*;
import java.util.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;


import java.nio.file.Files;
import java.util.zip.*;

/**
 * FullGtfsMerger handles merging multiple GTFS feed folders into a single output folder.
 */

public class FullGtfsMerger {

    private static final Map<String, String[]> PRIMARY_ID_FIELDS = new HashMap<>();
    static {
        // Define ID fields for each GTFS file
        PRIMARY_ID_FIELDS.put("agency.txt", new String[]{"agency_id"});
        PRIMARY_ID_FIELDS.put("routes.txt", new String[]{"route_id"});
        PRIMARY_ID_FIELDS.put("trips.txt", new String[]{"trip_id"});
        PRIMARY_ID_FIELDS.put("stop_times.txt", new String[]{"trip_id", "stop_sequence"});

        PRIMARY_ID_FIELDS.put("stops.txt", new String[]{"stop_id"});
        PRIMARY_ID_FIELDS.put("calendar.txt", new String[]{"service_id"});
        PRIMARY_ID_FIELDS.put("calendar_dates.txt", new String[]{"service_id", "date"});
        PRIMARY_ID_FIELDS.put("levels.txt", new String[]{"level_id"});
        PRIMARY_ID_FIELDS.put("feed_info.txt", new String[]{});

        PRIMARY_ID_FIELDS.put("shapes.txt", new String[]{"shape_id", "shape_pt_sequence"});
        PRIMARY_ID_FIELDS.put("frequencies.txt", new String[]{"trip_id", "start_time"});
        PRIMARY_ID_FIELDS.put("translations.txt", new String[]{"table_name", "field_name", "language", "record_id", "record_sub_id", "field_value"});
    }

    /**
     * Merges multiple GTFS feeds located in subfolders of a given root directory into a single output folder.
     *
     * <p>This method expects the {@code rootFolder} to contain multiple subdirectories,
     * each representing a separate GTFS feed. It validates the input, collects all feed
     * directories, and delegates the merging process to {@link #mergeFeedDirs(File[], String, String)}.</p>
     *
     * @param rootFolder   the root folder containing subdirectories, each representing a GTFS feed
     * @param outputFolder the target folder where the merged GTFS output will be saved
     * @param headerChoice headerChoice Determines how the reference header is chosen:
     *                    "long"  → Choose the header with the most columns.
     *                    "short" → Choose the header with the fewest columns.
     *
     * @return {@code true} if the feeds were merged successfully, {@code false} otherwise
     * @throws IOException              if an I/O error occurs while reading or writing files
     * @throws CsvValidationException   if an error occurs while parsing CSV files
     * @throws IllegalArgumentException if {@code rootFolder} or {@code outputFolder} is {@code null}
     */

 // Merge from folders (no zip)
    public boolean mergeFeedsFromFolders(String rootFolder, String outputFolder, String headerChoice)
            throws IOException, CsvValidationException {

        //null controls
        if (rootFolder == null || outputFolder == null) {
            throw new IllegalArgumentException("Input/output folder cannot be null");
        }

        //gtfs feed folder
        File root = new File(rootFolder);

        // Validate that the root folder exists and is a directory
        if (!root.exists() || !root.isDirectory()) {
            System.out.println("Root folder does not exist or is not a directory");
            return false;
        }

        //Gets all subfolders inside the root folder (each representing a GTFS feed)
        File[] feedDirs = root.listFiles(File::isDirectory);

        //If the root folder does not contain any subfolders, the merge operation cannot be performed.
        if (feedDirs == null || feedDirs.length == 0) {
            System.out.println("No feeds found!");
            return false;
        }

        // Pass the feed directories to the merge function
        return mergeFeedDirs(feedDirs, outputFolder, headerChoice);
    }


    /**
     * Merges multiple GTFS feeds from ZIP files located inside a root directory.
     * <p>
     * This method looks for all ZIP files in the specified {@code rootFolder},
     * extracts each ZIP file into a temporary folder, and then merges the GTFS feeds
     * contained within these folders into a single output folder.
     * </p>
     *
     * @param rootFolder   The root folder containing GTFS ZIP files.
     * @param outputFolder The destination folder where the merged GTFS feed will be saved.
     * @param headerChoice headerChoice Determines how the reference header is chosen:
     *                    "long"  → Choose the header with the most columns.
     *                    "short" → Choose the header with the fewest columns.
     * @return {@code true} if the merge was successful,
     *         {@code false} if no ZIP files were found or root folder is invalid.
     * @throws IOException              If any file/folder operation fails (e.g., reading ZIP or writing output).
     * @throws CsvValidationException   If an error occurs while parsing GTFS CSV files inside ZIPs.
     * @throws IllegalArgumentException If {@code rootFolder} or {@code outputFolder} is {@code null}.
     */
 //Merge from zip files
    public boolean mergeFeedsFromZips(String rootFolder, String outputFolder, String headerChoice)
            throws IOException, CsvValidationException {

        //null controls
        if (rootFolder == null || outputFolder == null) {
            throw new IllegalArgumentException("Input/output folder cannot be null");
        }

        // Root folder containing ZIP files
        File root = new File(rootFolder);

        // Validate that the root folder exists and is a directory
        if (!root.exists() || !root.isDirectory()) {
            System.out.println("Root folder does not exist or is not a directory");
            return false;
        }

        // List all ZIP files in the root folder
        // If the ZIP files does not exist, notify the user.
        File[] zipFiles = root.listFiles(f -> f.isFile() && f.getName().endsWith(".zip"));
        if (zipFiles == null || zipFiles.length == 0) {
            System.out.println("No ZIP files found!");
            return false;
        }

        // Extract each ZIP into a temporary folder
        //temporary folders are added to the tempDirs list.
        List<File> tempDirs = new ArrayList<>();
        for (File zip : zipFiles) {
            File tempDir = Files.createTempDirectory(zip.getName().replace(".zip","")).toFile();
            unzip(zip, tempDir);   // unzip the file into tempDir
            tempDirs.add(tempDir);
        }
        //The tempDirs list is converted to an array.
        File[] feedDirs = tempDirs.toArray(new File[0]);

        //  Merge feeds from extracted ZIP folders
        return mergeFeedDirs(feedDirs, outputFolder, headerChoice);
    }



    /**
     * Merges all GTFS files from multiple feed directories into a single output folder.
     * <p>
     * This method loops through each GTFS file defined in {@code PRIMARY_ID_FIELDS}
     * and calls {@link #mergeFileIfExists(File[], String, String, String)} to perform the merge.
     * It also ensures that the output folder exists and is not inside the input feed directories.
     * </p>
     *
     * @param feedDirs     Array of GTFS feed directories to merge.
     * @param outputFolder The folder where the merged GTFS files will be saved.
     * @param headerChoice headerChoice Determines how the reference header is chosen:
     *                    "long"  → Choose the header with the most columns.
     *                    "short" → Choose the header with the fewest columns.
     * @return {@code true} if the merge is successful.
     * @throws IOException              If there is a problem reading or writing files.
     * @throws CsvValidationException   If there is a problem parsing CSV data.
     * @throws IllegalArgumentException If the output folder is inside one of the input feed directories.
     */
// merge
    private boolean mergeFeedDirs(File[] feedDirs, String outputFolder, String headerChoice)
            throws IOException, CsvValidationException {

        //merged folder
        File outDir = new File(outputFolder);

        //Create the output folder if it doesn't exist
        if (!outDir.exists()) outDir.mkdirs();

        if (outDir.getCanonicalPath().startsWith(feedDirs[0].getParentFile().getCanonicalPath())) {
            throw new IllegalArgumentException("Output folder cannot be inside input folder");
        }

        String choice = (headerChoice != null) ? headerChoice.toLowerCase() : "long";

        // Loop through all GTFS files:
        // Call mergeFileIfExists for each filename in PRIMARY_ID_FIELDS
        for (String fileName : PRIMARY_ID_FIELDS.keySet()) {
            mergeFileIfExists(feedDirs, outputFolder, fileName, choice);
        }

        return true;
    }

    /**
     * Extracts the contents of a ZIP file into a specified destination directory.
     * <p>
     * This method iterates through each entry in the ZIP file. If the entry is a directory,
     * it creates the directory in the destination folder. If the entry is a file, it extracts
     * the file to the destination folder, preserving the folder structure.
     * </p>
     *
     * @param zipFile The ZIP file to be extracted.
     * @param destDir The destination directory where the contents of the ZIP file will be extracted.
     * @throws IOException If there is an error reading the ZIP file or writing files to the destination.
     */
// Unzip Method
    private void unzip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) newFile.mkdirs();
                else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) fos.write(buffer, 0, len);
                    }
                }
                zis.closeEntry();
            }
        }
    }


    /**
     * Performs the merge operation for a specific file if it exists in any feed folder.
     *
     * @param feedDirs     Array of feed folder paths to check for the file.
     * @param outputFolder Path of the folder where the merged file will be saved.
     * @param fileName     Name of the file to merge (e.g., "agency.txt").
     * @param headerChoice Header type to use in the merged file; "long" or "short".
     *
     * @throws IOException             If there is a problem reading or writing files.
     * @throws CsvValidationException  If there is a problem reading CSV data.
     */

    private void mergeFileIfExists(File[] feedDirs, String outputFolder, String fileName, String headerChoice) throws IOException,CsvValidationException {

        // find files matching fileName in feed folders
        // This creates a list of files to merge.

        // Check the file name and create a list for merging.
        // For example, fileName = "agency.txt"
        // If [feed1, feed2] is in feedDirs:
        // - Does feed1/agency.txt exist? If so, add it to the list.
        // - Does feed2/agency.txt exist? If so, add it to the list.
        // Result: files list = [feed1/agency.txt, feed2/agency.txt]
        List<File> files = new ArrayList<>();
        for (File dir : feedDirs) {
            File f = new File(dir, fileName);
            if (f.exists()) files.add(f);
        }

        // If the file doesn't exist, there's no need to merge.
        if (files.isEmpty()) return;


        // We get which fields of the file will be used as ID (single or multiple ID)
        // "agency.txt" → ["agency_id"]
        String[] idFields = PRIMARY_ID_FIELDS.get(fileName);

        // Output path of the file to be merged
        File outputFile = new File(outputFolder, fileName);

        // Call the appropriate merge method based on single ID or multiple IDs
        if (idFields == null || idFields.length == 0) {
            mergeFileSingleId(files, outputFile, null, headerChoice);
        } else if (idFields.length == 1) {
            mergeFileSingleId(files, outputFile, idFields[0], headerChoice);
        } else {
            mergeFileMultipleId(files, outputFile, idFields, headerChoice);
        }
    }

    /**
     * Selects the appropriate CSV header from a list of input files based on the specified choice.
     * <p>
     * This method reads the first line (header) from each CSV file in the input list and collects them.
     * Then, depending on the {@code headerChoice} parameter, it selects:
     * <ul>
     *     <li>{@code "long"}: the header with the most columns</li>
     *     <li>{@code "short"}: the header with the fewest columns</li>
     *     <li>any other value: the first header found</li>
     * </ul>
     * </p>
     *
     * @param inputFiles   The list of CSV files to examine for headers.
     * @param headerChoice The selection criteria for choosing the header: "long", "short", or any other value.
     * @return The selected header as an array of strings.
     * @throws IOException If there is a problem reading any of the CSV files or if no valid headers are found.
     * @throws CsvValidationException If a CSV file is malformed.
     */
    private String[] selectHeader(List<File> inputFiles, String headerChoice) throws IOException, CsvValidationException {
        // List to store the headers from all input files
        List<String[]> headers = new ArrayList<>();

        // Loop through each input file
        for (File file : inputFiles) {
            try (CSVReader reader = new CSVReader(new FileReader(file))) {
                String[] line = reader.readNext();
                // If the header exists and has columns, add it to the headers list
                if (line != null && line.length > 0) headers.add(line);
            }
        }

        // If no valid headers were found in any file, throw an exception
        if (headers.isEmpty()) throw new IOException("No valid headers found in input files.");

        // Select the header based on the user's choice

        switch (headerChoice) {
            case "long":   // Return the header with the most columns
                return headers.stream().max(Comparator.comparingInt(a -> a.length)).orElse(headers.get(0));
            case "short":  // Return the header with the fewest columns
                return headers.stream().min(Comparator.comparingInt(a -> a.length)).orElse(headers.get(0));
            default:   // Default: return the first header found
                return headers.get(0);
        }
    }


    /**
     * Merges multiple CSV files that use multiple columns as unique identifiers (composite keys).
     * <p>
     * This method reads each CSV file in {@code inputFiles}, aligns their columns with a reference header
     * (selected based on {@code headerChoice}), and merges rows into a single output file. Rows with the same
     * combination of ID fields are considered duplicates, and the latest encountered row will overwrite the previous one.
     * </p>
     *
     * @param inputFiles  The list of CSV files to merge.
     * @param outputFile  The file where the merged CSV will be written.
     * @param idFields    Array of column names used as unique identifiers for merging rows.
     * @param headerChoice Determines which header to use from the input files: "long" for the header with the most columns,
     *                     "short" for the header with the fewest columns
     * @throws IOException If there is a problem reading from or writing to a file.
     * @throws CsvValidationException If any input CSV file is malformed.
     */
    private void mergeFileMultipleId(List<File> inputFiles, File outputFile, String[] idFields, String headerChoice) throws IOException, CsvValidationException {

        // Select the reference header based on headerChoice ("long" or "short")
        String[] refHeader = selectHeader(inputFiles, headerChoice);

        // Map each column name in refHeader to its index for easy lookup
        Map<String, Integer> refIndex = new HashMap<>();
        for (int i = 0; i < refHeader.length; i++) refIndex.put(refHeader[i], i);

        // Determine the indexes of the ID fields in the reference header
        int[] idIndexes = new int[idFields.length];
        for (int i = 0; i < idFields.length; i++) idIndexes[i] = refIndex.getOrDefault(idFields[i], -1);

        //Map to store merged rows keyed by concatenated ID values
        //Key → ID
        // Value → row
        Map<String, String[]> idToRow = new LinkedHashMap<>();

        // Loop through each input CSV file
        for (File file : inputFiles) {
            try (CSVReader reader = new CSVReader(new FileReader(file))) {
                // Read the header of the current file
                String[] fileHeader = reader.readNext();
                if (fileHeader == null) continue; // skip empty files

                // Map column names in current file to their indexes
                Map<String, Integer> fileIndex = new HashMap<>();
                for (int i = 0; i < fileHeader.length; i++) fileIndex.put(fileHeader[i], i);

                // Read each row in the CSV file
                String[] row;
                while ((row = reader.readNext()) != null) {
                    if (row.length == 0) continue; // skip empty rows

                    // Align row with reference header (fill missing columns with "")
                    String[] alignedRow = new String[refHeader.length];
                    for (String col : refHeader) {
                        Integer idx = fileIndex.get(col);
                        alignedRow[refIndex.get(col)] = (idx != null && idx < row.length) ? row[idx] : "";
                    }

                    // Build a unique key using all ID fields
                    StringBuilder keyBuilder = new StringBuilder();
                    for (int idx : idIndexes) if (idx != -1) keyBuilder.append(alignedRow[idx]).append("_");

                    // Add the row to the map (overwrites duplicates with the same key)
                    idToRow.put(keyBuilder.toString(), alignedRow);
                }
            }
        }
        // Write merged data to the output CSV file
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(refHeader); // write the header first
            for (String[] row : idToRow.values()) writer.writeNext(row); // write each merged row
        }
    }

    /**
     * Merges multiple CSV files that use a single column as a unique identifier.
     * <p>
     * This method reads each CSV file in {@code inputFiles}, aligns their columns with a reference header
     * (selected based on {@code headerChoice}), and merges rows into a single output file. Rows with the same
     * value in the ID column are considered duplicates, and the latest encountered row will overwrite the previous one.
     * If the specified ID column does not exist, a unique key is generated for each row using UUID.
     * </p>
     *
     * @param inputFiles The list of CSV files to merge.
     * @param outputFile The file where the merged CSV will be written.
     * @param idField    The name of the column used as a unique identifier for merging rows. If null or not present,
     *                   UUIDs are generated for each row.
     * @param headerChoice Determines which header to use from the input files: "long" for the header with the most columns,
     *                     "short" for the header with the fewest columns
     * @throws IOException If there is a problem reading from or writing to a file.
     * @throws CsvValidationException If any input CSV file is malformed.
     */
    private void mergeFileSingleId(List<File> inputFiles, File outputFile, String idField, String headerChoice) throws IOException,CsvValidationException {
        // Select the reference header based on the user's choice ("long" or "short")
        String[] refHeader = selectHeader(inputFiles, headerChoice);


        // Map each column name in the reference header to its index
        Map<String, Integer> refIndex = new HashMap<>();
        for (int i = 0; i < refHeader.length; i++) refIndex.put(refHeader[i], i);

        // Find the index of the ID column
        int idIndex = refIndex.getOrDefault(idField, -1);

        // Map to temporarily store merged rows
        // Key → ID (unique), Value → entire row
        Map<String, String[]> idToRow = new LinkedHashMap<>();


        // Loop through each CSV file
        for (File file : inputFiles) {
            try (CSVReader reader = new CSVReader(new FileReader(file))) {
                // Read the file header
                String[] fileHeader = reader.readNext();
                if (fileHeader == null) continue;
                // Map column names in this file to their indices
                Map<String, Integer> fileIndex = new HashMap<>();
                for (int i = 0; i < fileHeader.length; i++) fileIndex.put(fileHeader[i], i);

                // Read each row from the CSV
                String[] row;
                while ((row = reader.readNext()) != null) {
                    if (row.length == 0) continue;
                    // Align the row with the reference header (fill missing columns with "")
                    String[] alignedRow = new String[refHeader.length];
                    for (String col : refHeader) {
                        Integer idx = fileIndex.get(col);
                        alignedRow[refIndex.get(col)] = (idx != null && idx < row.length) ? row[idx] : "";
                    }

                    // If ID column does not exist, create a unique key using UUID
                    if (idIndex == -1) idToRow.put(UUID.randomUUID().toString(), alignedRow);
                    else {
                        // If ID exists, use its value as the key
                        String idValue = alignedRow[idIndex];
                        if (idValue != null && !idValue.isEmpty()) idToRow.put(idValue, alignedRow);
                    }
                }
            }
        }
        // Write the merged data to the output CSV file
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            writer.writeNext(refHeader);
            for (String[] row : idToRow.values()) writer.writeNext(row);
        }
    }


}
