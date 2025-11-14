package ca.concordia.filesystem;

import ca.concordia.filesystem.datastructures.FEntry;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.locks.ReentrantLock;

public class FileSystemManager {

    private static final int MAXFILES = 5;
    private static final int MAXBLOCKS = 10;
    private static final int BLOCK_SIZE = 128;

    private final RandomAccessFile disk;
    private final ReentrantLock globalLock = new ReentrantLock();
    private final FEntry[] inodeTable;
    private final boolean[] freeBlockList;

    public FileSystemManager(String filename, int totalSize) {
        try {
            disk = new RandomAccessFile(filename, "rw");
            if (disk.length() < totalSize) disk.setLength(totalSize);
            inodeTable = new FEntry[MAXFILES];
            freeBlockList = new boolean[MAXBLOCKS];
            for (int i = 0; i < MAXBLOCKS; i++) freeBlockList[i] = true;
            System.out.println("The file system has been initialized: " + filename);
        } catch (IOException e) {
            throw new RuntimeException("Error: the file system failed to initialize", e);
        }
    }

    private FEntry findFile(String fileName) {
        for (FEntry entry : inodeTable)
            if (entry != null && fileName.equals(entry.getFilename()))
                return entry;
        return null;
    }

    private int findInodeIndex(String fileName) {
        for (int i = 0; i < inodeTable.length; i++) {
            FEntry entry = inodeTable[i];
            if (entry != null && fileName.equals(entry.getFilename()))
                return i;
        }
        return -1;
    }

    private int allocateBlock() throws Exception {
        for (int i = 0; i < MAXBLOCKS; i++) {
            if (freeBlockList[i]) {
                freeBlockList[i] = false;
                return i;
            }
        }
        throw new Exception("No free disk block available.");
    }

    public void createFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            if (fileName.length() > 11) {
                throw new Exception("Filename must be 11 characters or less as per assignment instructions.");
            }
            if (findFile(fileName) != null) throw new Exception("File already exists.");
            int inodeIndex = -1;
            for (int i = 0; i < MAXFILES; i++) {
                if (inodeTable[i] == null) {
                    inodeIndex = i;
                    break;
                }
            }
            if (inodeIndex == -1) throw new Exception("No free inode available.");
            int blockIndex = allocateBlock();
            FEntry newFile = new FEntry(fileName, (short) 0, (short) blockIndex);
            inodeTable[inodeIndex] = newFile;
            System.out.println("File created: " + fileName + " (block " + blockIndex + ")");
        } finally {
            globalLock.unlock();
        }
    }

    public void writeFile(String fileName, String data) throws Exception {
        globalLock.lock();
        try {
            FEntry file = findFile(fileName);
            if (file == null) throw new Exception("File not found.");
            byte[] bytes = data.getBytes();
            if (bytes.length > BLOCK_SIZE)
                throw new Exception("File too large for a single block (" + bytes.length + " bytes).");
            int blockIndex = file.getFirstBlock();
            long offset = (long) blockIndex * BLOCK_SIZE;
            disk.seek(offset);
            disk.write(bytes);
            if (bytes.length < BLOCK_SIZE) disk.write(new byte[BLOCK_SIZE - bytes.length]);
            file.setFilesize((short) bytes.length);
            System.out.println("You wrote " + bytes.length + " bytes in the file: " + fileName);
        } finally {
            globalLock.unlock();
        }
    }

    public String readFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            FEntry file = findFile(fileName);
            if (file == null) throw new Exception("File not found.");
            int size = file.getFilesize();
            if (size <= 0) return "";
            int blockIndex = file.getFirstBlock();
            long offset = (long) blockIndex * BLOCK_SIZE;
            disk.seek(offset);
            byte[] buffer = new byte[size];
            int read = disk.read(buffer);
            if (read <= 0) return "";
            if (read < size) {
                byte[] tmp = new byte[read];
                System.arraycopy(buffer, 0, tmp, 0, read);
                buffer = tmp;
            }
            return new String(buffer);
        } finally {
            globalLock.unlock();
        }
    }

    public void deleteFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            int inodeIndex = findInodeIndex(fileName);
            if (inodeIndex == -1) throw new Exception("File not found.");
            FEntry file = inodeTable[inodeIndex];
            int blockIndex = file.getFirstBlock();
            if (blockIndex >= 0 && blockIndex < MAXBLOCKS)
                freeBlockList[blockIndex] = true;
            long offset = (long) blockIndex * BLOCK_SIZE;
            disk.seek(offset);
            disk.write(new byte[BLOCK_SIZE]);
            inodeTable[inodeIndex] = null;
            System.out.println("File deleted: " + fileName);
        } finally {
            globalLock.unlock();
        }
    }

    public void listFiles() {
        System.out.println("Files on disk:");
        for (FEntry entry : inodeTable)
            if (entry != null)
                System.out.println(" - " + entry.getFilename() + " (" + entry.getFilesize() + " bytes)");
    }

    public String listFilesToString() {
        StringBuilder sb = new StringBuilder();
        for (FEntry entry : inodeTable) {
            if (entry != null) {
                sb.append(" - ").append(entry.getFilename())
                        .append(" (").append(entry.getFilesize()).append(" bytes)\n");
            }
        }
        return sb.toString();
    }
}
