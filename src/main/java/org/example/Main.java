package org.example;

public class Main {
    public static void main(String[] args) {
        FullGtfsMerger merger = new FullGtfsMerger();
        try {
            boolean success =
                    merger.mergeAllFeeds("C:\\Users\\irema\\IdeaProjects\\gtfsMerge\\src\\all_feeds",
                            "C:\\Users\\irema\\IdeaProjects\\gtfsMerge\\src\\merged","long");

            if (success) {
                System.out.println("Merge operation is successful.");
            } else {
                System.out.println("Merge operation failed.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

