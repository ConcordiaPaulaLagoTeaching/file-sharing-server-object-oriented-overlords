package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;

import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private final int MAXFILES = 5;
    private final int MAXBLOCKS = 10;
    private final static FileSystemManager instance;
    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();

    private static final int BLOCK_SIZE = 128; // Example block size

    private FEntry[] inodeTable; // Array of inodes
    private boolean[] freeBlockList; // Bitmap for free blocks

    private FEntry findFile(String fileName) {
        for (FEntry entry : inodeTable) {
            if (entry != null && entry.inUse && entry.name.equals(fileName)) {
                return entry;
            }
        }
        return null;
    }
    public FileSystemManager(String filename, int totalSize) {
        // Initialize the file system manager with a file
        try {
           disk = new RandomAccessFile(filename, "rw");

           inodeTable = new FEntry[MAXFILES];
           freeBlockList = new boolean[MAXFILES];
           for (int i = 0; i < MAXBLOCKS; i++) {
               freeBlockList[i] = true;
           }
           System.out.println("The file system has been intialized: " + filename);

        } catch (Exception e){
            throw new RuntimeException("Error the file sytem has failed to initialize",e);
        }

    }


    public void createFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            FEntry file = findFile(fileName);
            if (file != null) {
                throw new Exception("File already exists");
            }
            }
            int inodeIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                if (inodeTable[i] == null || !inodeTable[i].inUse)
                    inodeIndex = i;
                break;
            }
            if (inodeIndex == -1)
                throw new Exception("No free inode available");

            int blockIndex = -1;
            for (int i = 0; i < MAXBLOCKS; i++) {
                if (freeBlockList[i]) {
                    blockIndex = i;
                    freeBlockList[i] = false;
                    break;
                }
            }
            if (blockIndex == -1)
                throw new Exception("No free disk block available.");

            FEntry newFile = new FEntry();
            newFile.name = fileName;
            newFile.startBlock = blockIndex;
            newFile.size = 0;
            newFile.inUse = true;
            inodeTable[inodeIndex] = newFile;
            System.out.println("File created: " + fileName + " (block " + blockIndex + ")");

        }finally {
            globalLock.unlock();
        }
    }
    public void writeFile(String fileName, String data) throws Exception {
        globalLock.lock();
        try {
            FEntry file = findFile(fileName);
            if (file == null)
                throw new Exception("File not found.");

            disk.seek(file.startBlock * BLOCK_SIZE);

            byte[] bytes = data.getBytes();
            disk.write(bytes);

            file.size = bytes.length;

            System.out.println("You wrote " + bytes.length + " bytes in the file: " + fileName);
        } finally {
            globalLock.unlock();
        }
    }
    public String readFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            FEntry file = findFile(fileName);
            if (file == null)
                throw new Exception("File not found.");

            disk.seek(file.startBlock * BLOCK_SIZE);

            byte[] buffer = new byte[file.size];
            disk.read(buffer);

            return new String(buffer);
        } finally {
            globalLock.unlock();
        }
    }
    public void deleteFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            FEntry file = findFile(fileName);
            if (file == null || !file.inUse) {
                throw new Exception("File not found.");
            }
            freeBlockList[file.startBlock] = true;
            file.inUse = false;
            file.name = null;
            file.size = 0;
            file.startBlock = -1;

        } finally {
            globalLock.unlock();
        }
    }

    public void listFiles() {
        System.out.println("Files on disk:");
        for (FEntry entry : inodeTable) {
            if (entry != null && entry.inUse) {
                System.out.println(" - " + entry.name + " (" + entry.size + " bytes)");
            }
        }
    }
    // TODO: Add readFile, writeFile and other required methods,
}
