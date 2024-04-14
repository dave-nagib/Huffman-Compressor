import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class HuffmanCodeManager {

    private final Map<ByteWord, BitVector> codewords = new HashMap<>(1024);
    private long originalByteLength;
    public static final int MAX_BUFFER_SIZE = 524288000; // 500 MB
    private FileInputStream byteReader;
    private FileOutputStream byteWriter;
    private ByteBuffer dictionaryBuffer;
    private byte n;

    public String compress(String path, byte wordSize) {
        // Set n
        this.n = wordSize;
        // Get the original byte size of the original file and create output file
        File input = new File(path);
        originalByteLength = input.length();
        String outputPath = input.getParent() + File.separator + "20010545." + n + "." + input.getName() + ".hc";
        File output = new File(outputPath);
        try {
            output.createNewFile();
        } catch (Exception e) {
            System.out.println("Problem creating output file: " + e.getMessage());
        }
        // Write compressed file
        try {
            // Prepare reader and writer
            byteReader = new FileInputStream(path);
            byteWriter = new FileOutputStream(output);
            // Get the frequencies of the n-byte characters and reset it to the first position
            Map<ByteWord, Integer> freqTable = getFrequencies();
            byteReader.close();
            byteReader = new FileInputStream(path);
            // Build the Huffman tree
            HTNode root = buildHuffmanTree(freqTable);
            // Write the dictionary header along with the encoding
            dictionaryBuffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
            writeHeader(root);
            // Prepare read and write buffers
            byte[] readBuffer, word;
            int readOffset;
            BitBuffer writeBuffer = new BitBuffer(MAX_BUFFER_SIZE);
            int readBufferSize = (int) Math.min(originalByteLength, (long) (MAX_BUFFER_SIZE/n)*n); // Read the whole file or a multiple of n
            // Start writing the encoded data
            while ((readBuffer = byteReader.readNBytes(readBufferSize)).length != 0) { // Fill read buffer
                readOffset = 0;
                // Get words from read buffer
                while (readOffset < readBuffer.length){
                    if (readOffset+n <= readBuffer.length) { // Next word is n bytes
                        word = new byte[n];
                        System.arraycopy(readBuffer, readOffset, word, 0, n);
                    } else { // Next word is less than n bytes
                        word = new byte[readBuffer.length - readOffset];
                        System.arraycopy(readBuffer, readOffset, word, 0, readBuffer.length - readOffset);
                    }
                    readOffset += n;
                    BitVector code = codewords.get(new ByteWord(word));
                    if (writeBuffer.hasOverflow())
                        byteWriter.write(writeBuffer.getFullBuffer());
                    writeBuffer.appendBits(code);
                }
            }
            if (!writeBuffer.isEmpty()) {
                if (writeBuffer.hasOverflow())
                    byteWriter.write(writeBuffer.getFullBuffer());
                byteWriter.write(writeBuffer.getFromBuffer());
            }
        } catch (Exception e) {
            System.out.println("Error in compressing the file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeIO();
        }
        return outputPath;
    }

    private Map<ByteWord,Integer> getFrequencies() {
        ByteWord word;
        byte[] wordBytes, readBuffer;
        HashMap<ByteWord, Integer> frequencyTable = new HashMap<>();
        int length = 0, incompleteWordLen = 0, readOffset;
        int readBufferSize = (int) Math.min(originalByteLength, (long) (MAX_BUFFER_SIZE/n)*n); // Read the whole file or a multiple of n

        try {
            // Loop until the bytes read are 0
            while ((readBuffer = byteReader.readNBytes(readBufferSize)).length != 0){
                readOffset = 0;
                while (readOffset < readBuffer.length) {
                    // Check for last word
                    if (readOffset+n <= readBuffer.length) {
                        wordBytes = new byte[n];
                        System.arraycopy(readBuffer, readOffset, wordBytes, 0, n);
                    } else {
                        // The last word will be smaller than n
                        wordBytes = new byte[readBuffer.length - readOffset];
                        System.arraycopy(readBuffer, readOffset, wordBytes, 0,readBuffer.length - readOffset);
                    }
                    readOffset += n;
                    word = new ByteWord(wordBytes);
                    frequencyTable.compute(word, (k,v) -> (v == null)? 1 : v+1);
                    length += n;
                }
            }

            assert originalByteLength == length - n + incompleteWordLen;

        } catch (Exception e) {
            System.out.println("Error in collecting the frequencies: " + e.getMessage());
        }

        return frequencyTable;
    }

    private HTNode buildHuffmanTree(Map<ByteWord, Integer> frequencyTable) {
        ArrayList<HTNode> leaves = new ArrayList<>();
        // Loop over the frequency table, and add everything to the leaves list
        for (Map.Entry<ByteWord, Integer> character : frequencyTable.entrySet())
            leaves.add(new HTNode(character.getKey(), character.getValue()));
        // Create a priority queue of HTNodes to start the building process
        PriorityQueue<HTNode> pq = new PriorityQueue<>(leaves);
        // Start the building loop
        while (pq.size() > 1) {
            HTNode leftChild = pq.poll(), rightChild = pq.poll();
            HTNode newNode = new HTNode(null, leftChild.frequency + rightChild.frequency);
            newNode.setLeft(leftChild);
            newNode.setRight(rightChild);
            pq.add(newNode);
        }
        // Return the root of the final tree
        return pq.poll();
    }

    private void writeHeader(HTNode huffmanTree) throws IOException {
        // Write word size in the original file
        byteWriter.write(n);
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(originalByteLength);
        byteWriter.write(bb.array());
        // Get the encoding of all characters (put it in a map)
        BitVector init = new BitVector();
        getAndWriteEncoding(huffmanTree, init);
        if (dictionaryBuffer.remaining() < dictionaryBuffer.capacity()) {
            byte[] dataChunk = new byte[dictionaryBuffer.position()];
            dictionaryBuffer.rewind();
            dictionaryBuffer.get(dataChunk);
            byteWriter.write(dataChunk);
            dictionaryBuffer.clear();
        }
    }


    private void getAndWriteEncoding(HTNode node, BitVector currPath) throws IOException {

        if (node.left == null && node.right == null) {
            codewords.put(node.character, currPath);
            byte[] entry = new byte[n+1];
            System.arraycopy(node.character.bytes(), 0, entry, 0, node.character.bytes().length);
            entry[n] = (byte) currPath.getLength();
            if (dictionaryBuffer.remaining() < n+1) {
                byte[] dataChunk = new byte[dictionaryBuffer.position()];
                dictionaryBuffer.rewind();
                dictionaryBuffer.get(dataChunk);
                byteWriter.write(dataChunk);
                dictionaryBuffer.clear();
            }
            dictionaryBuffer.put(entry);
            return;
        }

        BitVector leftPath = currPath.clone(), rightPath = currPath.clone();
        leftPath.append(false);
        rightPath.append(true);

        getAndWriteEncoding(node.left, leftPath);
        getAndWriteEncoding(node.right, rightPath);
    }

    public void decompress(String path) {
        // Prepare and create output decompressed file
        File compressed = new File(path);
        String compressedName = compressed.getName();
        String decompressedPath = compressed.getParent() + File.separator + "extracted." + compressedName.substring(0, compressedName.length()-3);
        File decompressed = new File(decompressedPath);
        try {
            decompressed.createNewFile();
        } catch (Exception e) {
            System.out.println("Problem creating output file: " + e.getMessage());
        }
        // Read compressed file and write decompressed file
        try {
            // Prepare reader and writer
            byteReader = new FileInputStream(path);
            byteWriter = new FileOutputStream(decompressedPath);
            // Read header
            n = (byte) byteReader.read();
            originalByteLength = ByteBuffer.wrap(byteReader.readNBytes(8)).getLong();
            // Read and reconstruct the huffman tree used in encoding the file
            byte[] firstEntry = byteReader.readNBytes(n+1);
            byte[] firstWord = new byte[n];
            System.arraycopy(firstEntry, 0, firstWord, 0, n);
            RHTNode RHTRoot = new RHTNode();
            reconstructHuffmanTree(firstWord, firstEntry[n], 0, RHTRoot, "");
            assert RHTRoot.left != null && RHTRoot.right != null;
            // Using the reconstructed tree, parse the compressed file and write the output file
            writeDecompressedData(RHTRoot);
            assert originalByteLength == 0;
        } catch (Exception e) {
            System.out.println("Error while decompressing the file: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeIO();
        }
    }

    private void closeIO() {
        try {
            byteReader.close();
            byteWriter.close();
        } catch (IOException ex) {
            System.out.println("IO Error in closing the reader and writer.");
        }
    }

    private void reconstructHuffmanTree(byte[] currWord, int codeLength, int currDepth, RHTNode currNode, String bitStr) throws IOException {
        // If the depth of the current node is equal to the length of the code,
        // this node is terminal with the current word corresponding to the current bit string
        if (codeLength == currDepth) {
            currNode.word = currWord;
            return;
        }
        // If this is an internal node, reconstruct the left subtree using the same word and code length
        currNode.left = new RHTNode();
        reconstructHuffmanTree(currWord, codeLength, currDepth+1, currNode.left, bitStr + "0");
        // Read a new word and code length to reconstruct the right subtree with
        currNode.right = new RHTNode();
        byte[] nextEntry = byteReader.readNBytes(n+1), nextWord = new byte[n]; // Read word and the length of its code
        System.arraycopy(nextEntry, 0, nextWord, 0, n);
        reconstructHuffmanTree(nextWord, nextEntry[n], currDepth+1, currNode.right, bitStr + "1");
    }

    private void writeDecompressedData(RHTNode RHTRoot) throws IOException {
        int readBufferSize = (int) Math.min(originalByteLength, (long) (MAX_BUFFER_SIZE/n)*n); // Read the whole file or a multiple of n
        byte[] readBuffer;
        ByteBuffer writeBuffer = ByteBuffer.allocate((int) Math.min(originalByteLength, (long) (MAX_BUFFER_SIZE/n)*n));
        RHTNode pointer;
        while (originalByteLength != 0) {
            readBuffer = byteReader.readNBytes(readBufferSize);
            pointer = RHTRoot;
            for (byte b : readBuffer) {
                for (int bitIndex = 7; bitIndex > -1; bitIndex--) {
                    if (pointer.word != null) {
                        if (originalByteLength >= n) {
                            if (writeBuffer.remaining() < n) {
                                byte[] data = new byte[writeBuffer.position()];
                                writeBuffer.rewind();
                                writeBuffer.get(data);
                                byteWriter.write(data);
                                writeBuffer.clear();
                            }
                            writeBuffer.put(pointer.word);
                            pointer = RHTRoot;
                            originalByteLength -= n;
                        } else {
                            if (originalByteLength > 0){
                                byte[] lastWord = new byte[(int)originalByteLength];
                                System.arraycopy(pointer.word, 0, lastWord, 0, lastWord.length);
                                if (writeBuffer.remaining() < writeBuffer.capacity()) {
                                    byte[] data = new byte[writeBuffer.position()];
                                    writeBuffer.rewind();
                                    writeBuffer.get(data);
                                    byteWriter.write(data);
                                    writeBuffer.clear();
                                }
                                byteWriter.write(lastWord);
                                originalByteLength -= lastWord.length;
                            }
                            return;
                        }
                    }
                    if (originalByteLength <= 0) {
                        if (writeBuffer.remaining() < writeBuffer.capacity()) {
                            byte[] data = new byte[writeBuffer.position()];
                            writeBuffer.rewind();
                            writeBuffer.get(data);
                            byteWriter.write(data);
                            writeBuffer.clear();
                        }
                        return;
                    }
                    pointer = ((b & (1<<bitIndex)) == 0)? pointer.left : pointer.right;
                }
            }
        }
    }

    public static void main(String[] args) {
        // I acknowledge that I am aware of the academic integrity guidelines of this course,
        // and that I worked on this assignment independently without any unauthorized help.
        HuffmanCodeManager manager = new HuffmanCodeManager();
        if (args[0].equals("c")) {
            long start = System.currentTimeMillis();
            String outPath = manager.compress(args[1], Byte.parseByte(args[2]));
            long end = System.currentTimeMillis();
            System.out.println("Compression time: " + (end-start) + "ms = " + (float)(end-start)/1000 + "s");
            File input = new File(args[1]), output = new File(outPath);
            System.out.println("Compression ratio: " + ((float)output.length()/input.length())*100 + "%");
        } else if (args[0].equals("d")) {
            long start = System.currentTimeMillis();
            manager.decompress(args[1]);
            long end = System.currentTimeMillis();
            System.out.println("Decompression time: " + (end-start) + "ms = " + (float)(end-start)/1000 + "s");
        }
    }
}
