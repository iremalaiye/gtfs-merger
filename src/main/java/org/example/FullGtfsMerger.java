package org.example;

import java.io.*;
import java.util.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
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
     * Merges all GTFS feed folders into a single output folder.
     *
     * @param rootFolder   Path to the main folder containing GTFS feed subfolders.
     *
     * @param outputFolder Path to the folder where merged GTFS files will be saved.
     *
     * @param headerChoice Determines how the reference header is chosen:
     *                     - "long"  → Choose the header with the most columns.
     *                     - "short" → Choose the header with the fewest columns.
     *
     * @return true if merging is successful, false if no feeds found or invalid configuration.
     * @throws IOException              If file reading/writing fails.
     * @throws CsvValidationException   If CSV format is invalid.
     */
    public boolean mergeAllFeeds(String rootFolder, String outputFolder, String headerChoice) throws IOException,CsvValidationException{

        //gtfs feed folder
        File root = new File(rootFolder);
        //merged folder
        File outDir = new File(outputFolder);
        //Create the output folder if it doesn't exist
        if (!outDir.exists()) outDir.mkdirs();

        //Gets all subfolders inside the root folder
        File[] feedDirs = root.listFiles(File::isDirectory);

        //If the root folder does not contain any subfolders, the merge operation cannot be performed.
        if (feedDirs == null || feedDirs.length == 0) {
            System.out.println("No feeds found!");
            return false;
        }

        if (outDir.getCanonicalPath().startsWith(root.getCanonicalPath())) {
            throw new IllegalArgumentException("Output folder cannot be the same as or inside the input folder.");
        }


        // Loop through all GTFS files:
        // Call mergeFileIfExists for each filename in PRIMARY_ID_FIELDS
        for (String fileName : PRIMARY_ID_FIELDS.keySet()) {
            mergeFileIfExists(feedDirs, outputFolder, fileName, headerChoice);
        }

        return true;
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
        switch (headerChoice.toLowerCase()) {
            case "long":   // Return the header with the most columns
                return headers.stream().max(Comparator.comparingInt(a -> a.length)).orElse(headers.get(0));
            case "short":  // Return the header with the fewest columns
                return headers.stream().min(Comparator.comparingInt(a -> a.length)).orElse(headers.get(0));
            default:   // Default: return the first header found
                return headers.get(0);
        }
    }


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

    // Finds files with the given name in the provided directories
        private File[] getFilesForName(File[] dirs, String name) {
            return Arrays.stream(dirs)
                    .map(dir -> new File(dir, name))
                    .filter(File::exists)
                    .toArray(File[]::new);
        }

}
