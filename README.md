# GTFS Merger

**GTFS Merger**, is a Java-based tool that merges multiple GTFS (General Transit Feed Specification) datasets into a single directory.
This allows you to collect public transport data from different sources and work with it in a unified, standard format.

## Features
- Merges all files within GTFS folders (`agency.txt`, `routes.txt`, `trips.txt`, `stop_times.txt`, etc.).
- Merges rows based on ID fields and prevents duplicate data.
- Provides the option to select the reference header based on the number of long or short columns.
- Reads and writes CSV files using OpenCSV.
- Saves merged files to the specified output folder.

## Requirements

-  Java 11 or higher.

-  If you use the shaded JAR, no Maven or additional dependencies are required — OpenCSV and all other dependencies are already included.


## Installation

 Using Shaded JAR (No Maven Needed)
1. Download the Shaded JAR file from releases
`gtfs-1.0-shaded.jar`

2. Add it to another Java project:

- Create a folder named `releases/` in the project.
- Copy the Shaded JAR to this folder.  
- In IntelliJ: Go to File → Project Structure → Modules → Dependencies → + → JARs or directories and add the JAR.

## Usage of Gtfs Merger

## Usage:
```java

import org.example.FullGtfsMerger;

    public static void main(String[] args) {
        FullGtfsMerger merger = new FullGtfsMerger();
        try {
            boolean success =
                    merger.mergeAllFeeds(
                "path/to/all_feeds",   // folder containing GTFS feed subfolders
                "path/to/merged",      // output folder for merged files
                "long"                 // headerChoice determines how the reference header is chosen:
                                       // "long"  → Choose the header with the most columns.
                                       // "short" → Choose the header with the fewest columns.
            );

            if (success) {
                System.out.println("Merge operation is successful.");
            } else {
                System.out.println("Merge operation failed.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
