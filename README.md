# GTFS Merger

**GTFS Merger**, is a Java-based tool that **merges** multiple **GTFS (General Transit Feed Specification) datasets** into a single directory.

## Purpose and Benefits  
GTFS Merger **combines multiple GTFS datasets** into a single directory with a unified standard format. It **prevents** **duplicate** or **conflicting records** by merging rows based on ID fields, ensuring clean and consistent transit data. This enables you to collect **public transport data** from different sources and work with it efficiently, making analysis and management easier.

## Features
- Merges all files within **GTFS feed folders** or **ZIPs** (agency.txt, routes.txt, trips.txt, stop_times.txt, etc.).  
- Merges rows based on ID fields and **prevents duplicate data**.  
- Provides the option to select the reference header based on the number of **long** or **short** columns.
- Reads and writes CSV files using OpenCSV.
- Saves merged files to the specified output folder.


## Requirements

-  Java 8 or higher

-  Maven (if building from source; all dependencies including OpenCSV are managed via Maven)

  > **Note:** If you use the shaded JAR, no Maven or additional dependencies are required — OpenCSV and all other dependencies are already included.

## Included Files  

- `gtfs-1.0-shaded.jar` → Executable JAR containing all dependencies.
- `gtfs-1.0-javadoc.jar` → JavaDoc documentation; shows method parameters and descriptions when imported in the IDE.(optional)
  
## Usage of Gtfs Merger
 Using Shaded JAR (No Maven Needed)
 1. Download the Shaded JAR file from releases.  
 `releases/gtfs-1.0-shaded.jar`

2. Add it to another Java project:

- Create a folder named `releases/` in the project.
- Copy the Shaded JAR to this folder.  
- In IntelliJ: Go to File → Project Structure → Modules → Dependencies → + → JARs or directories and add the JAR.

3. Download the javadoc.jar file from releases. (Optional)     
 `releases/gtfs-1.0-javadoc.jar`
  
   > **Note:** This step is optional. The shaded JAR works perfectly fine without attaching the JavaDoc. It’s only for a better developer experience.
4. Adding JavaDoc to a Shaded JAR  (Optional)  
   If you want to see parameter descriptions and JavaDocs in your IDE (like IntelliJ):
- In IntelliJ: Go to File → Project Structure → Modules → Dependencies.

- Find **gtfs-1.0-shaded.jar** in the list.

- Right-click it → Edit (or double-click).

- In the window that opens, find the "Attach JavaDoc..." button (in some versions, it may also be + → Attach JavaDoc).

- Select **gtfs-1.0-javadoc.jar** and click OK.

  From now on, when you hover over FullGtfsMerger or mergeAllFeeds in the IDE, the parameter descriptions and JavaDocs will appear.  
  
  > **Note:** This step is optional. The shaded JAR works perfectly fine without attaching the JavaDoc. It’s only for a better developer experience.
5. Prepare your feed folders  
 Gather all your GTFS feeds under one root folder. Feeds can be either **folders** or **ZIP files**.  

   If your feeds are already extracted as **folders**:  
            <your_gtfs_folders_path>   
                     ├─ GTFS_İzmir/  
                     ├─ GTFS_Antep/  
                     └─ GTFS_Muğla/
      
   If your feeds are still in **ZIP** format:  
            <your_gtfs_zips_path>  
                     ├─ izmir_gtfs.zip  
                     ├─ antep_gtfs.zip  
                     └─ mugla_gtfs.zip  
      

 6. Start the merge process using Java code.
    
    Replace  **"<your_gtfs_feeds_path>"** and  **"<your_output_path>"** with your own folder paths.
       
    You can merge GTFS feeds either from **extracted folders** or from **ZIP files**:       
     - If your feeds are already extracted as **subfolders** in a root folder, use **mergeFeedsFromFolders()**.     
     - If your feeds are still in **ZIP** format inside a root folder, use **mergeFeedsFromZips()**.
       
    You can also set the header preference to **"long"** or **"short."** This determines which CSV header will be used as the reference when merging files:    
      - "long"  → Choose the header with the most columns.      
      - "short" → Choose the header with the fewest columns.    

**Example: Merge from folders**
```java
import org.example.FullGtfsMerger;

public class Main {
    public static void main(String[] args) {
        FullGtfsMerger merger = new FullGtfsMerger();
        try {
            boolean success = merger.mergeFeedsFromFolders(
                "<your_gtfs_feeds_path>", // folder containing GTFS feed folders
                "<your_output_path>",     // folder to save merged files
                "long"                    //headerChoice determines how the reference header is chosen:
                                       // "long"  → Choose the header with the most columns.
                                      // "short" → Choose the header with the fewest columns.
            );

            if (success) System.out.println("Merge operation successful.");
            else System.out.println("Merge operation failed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
  ```
**Example: Merge from zip files**
```java
import org.example.FullGtfsMerger;

public class Main {
    public static void main(String[] args) {
        FullGtfsMerger merger = new FullGtfsMerger();
        try {
            boolean success = merger.mergeFeedsFromZips(
                "<your_gtfs_zips_path>",  // folder containing GTFS ZIP files
                "<your_output_path>",     // folder to save merged files
                "short"                 //headerChoice determines how the reference header is chosen:
                                       // "long"  → Choose the header with the most columns.
                                      // "short" → Choose the header with the fewest columns.
            );

            if (success) System.out.println("Merge operation successful.");
            else System.out.println("Merge operation failed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
