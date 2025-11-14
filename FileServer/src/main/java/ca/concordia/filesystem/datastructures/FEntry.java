package ca.concordia.filesystem.datastructures;

public class FEntry {

    private String filename;
    private short filesize;
    private short firstBlock;

    public FEntry(String filename, short filesize, short firstBlock) {
        this.filename = filename;
        this.filesize = filesize;
        this.firstBlock = firstBlock;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public short getFilesize() {
        return filesize;
    }

    public void setFilesize(short filesize) {
        this.filesize = filesize;
    }

    public short getFirstBlock() {
        return firstBlock;
    }

    public void setFirstBlock(short firstBlock) {
        this.firstBlock = firstBlock;
    }
}
