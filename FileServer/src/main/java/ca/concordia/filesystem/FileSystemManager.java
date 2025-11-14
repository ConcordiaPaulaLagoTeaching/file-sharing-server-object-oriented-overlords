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
    private final boolean[] blockUsed;
    private final int[] blockNext;

    public FileSystemManager(String filename, int totalSize) {
        try {
            disk = new RandomAccessFile(filename, "rw");
            int minSize = BLOCK_SIZE * MAXBLOCKS;
            if (totalSize < minSize) totalSize = minSize;
            if (disk.length() < totalSize) disk.setLength(totalSize);
            inodeTable = new FEntry[MAXFILES];
            blockUsed = new boolean[MAXBLOCKS];
            blockNext = new int[MAXBLOCKS];
            for (int i = 0; i < MAXBLOCKS; i++) {
                blockUsed[i] = false;
                blockNext[i] = -1;
            }
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

    private int countFreeBlocks() {
        int c = 0;
        for (boolean used : blockUsed) if (!used) c++;
        return c;
    }

    private int allocateBlock() throws Exception {
        for (int i = 0; i < MAXBLOCKS; i++) {
            if (!blockUsed[i]) {
                blockUsed[i] = true;
                blockNext[i] = -1;
                return i;
            }
        }
        throw new Exception("No free disk block available.");
    }

    private void freeBlockChain(int startBlock) throws IOException {
        int current = startBlock;
        while (current != -1) {
            int next = blockNext[current];
            blockUsed[current] = false;
            blockNext[current] = -1;
            long offset = (long) current * BLOCK_SIZE;
            disk.seek(offset);
            disk.write(new byte[BLOCK_SIZE]);
            current = next;
        }
    }

    public void createFile(String fileName) throws Exception {
        globalLock.lock();
        try {
            if (fileName.length() > 11) {
                throw new Exception("Filename must be at most 11 characters.");
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
            FEntry newFile = new FEntry(fileName, (short) 0, (short) -1);
            inodeTable[inodeIndex] = newFile;
            System.out.println("File created: " + fileName);
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
            int requiredBlocks = (bytes.length + BLOCK_SIZE - 1) / BLOCK_SIZE;
            if (requiredBlocks == 0) requiredBlocks = 1;
            int free = countFreeBlocks();
            int oldHead = file.getFirstBlock();
            if (oldHead != -1) {
                int count = 0;
                int cur = oldHead;
                while (cur != -1) {
                    count++;
                    cur = blockNext[cur];
                }
                free += count;
            }
            if (requiredBlocks > free) throw new Exception("Not enough space for file.");
            if (oldHead != -1) freeBlockChain(oldHead);
            int head = -1;
            int prev = -1;
            for (int i = 0; i < requiredBlocks; i++) {
                int b = allocateBlock();
                if (head == -1) head = b;
                if (prev != -1) blockNext[prev] = b;
                prev = b;
            }
            file.setFirstBlock((short) head);
            file.setFilesize((short) bytes.length);
            int remaining = bytes.length;
            int offsetInData = 0;
            int current = head;
            while (current != -1) {
                long offset = (long) current * BLOCK_SIZE;
                disk.seek(offset);
                int toWrite = Math.min(remaining, BLOCK_SIZE);
                if (toWrite > 0) {
                    disk.write(bytes, offsetInData, toWrite);
                    if (toWrite < BLOCK_SIZE) disk.write(new byte[BLOCK_SIZE - toWrite]);
                } else {
                    disk.write(new byte[BLOCK_SIZE]);
                }
                remaining -= toWrite;
                offsetInData += toWrite;
                current = blockNext[current];
            }
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
            int head = file.getFirstBlock();
            if (head == -1) return "";
            byte[] buffer = new byte[size];
            int remaining = size;
            int offsetInData = 0;
            int current = head;
            while (current != -1 && remaining > 0) {
                long offset = (long) current * BLOCK_SIZE;
                disk.seek(offset);
                int toRead = Math.min(remaining, BLOCK_SIZE);
                int read = disk.read(buffer, offsetInData, toRead);
                if (read <= 0) break;
                remaining -= read;
                offsetInData += read;
                current = blockNext[current];
            }
            return new String(buffer, 0, size - remaining);
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
            int head = file.getFirstBlock();
            if (head != -1) freeBlockChain(head);
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
        if (sb.length() == 0) sb.append("No files on disk.\n");
        return sb.toString();
    }
}
