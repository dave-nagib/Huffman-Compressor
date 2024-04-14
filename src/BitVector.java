import java.util.BitSet;

public class BitVector extends BitSet {
    private int length;

    public BitVector() {
        super();
        this.length = 0;
    }

    public BitVector(int length) {
        super(length);
        this.length = length;
    }

    public void append(boolean bit) {
        this.set(length, bit);
        length++;
    }

    public int getLength() {
        return length;
    }

    @Override
    public BitVector clone() {
        BitVector cloned = (BitVector) super.clone();
        cloned.length = this.length;
        return cloned;
    }

    @Override
    public String toString() {
        StringBuilder bits = new StringBuilder();
        for (int i=0 ; i<length ; i++) {
            bits.append(this.get(i) ? 1 : 0);
        }
        return bits.toString();
    }

    public byte[] toByteArray() {
        byte lastBits = (byte) (getLength()%8);
        byte[] ret;
        if (lastBits == 0) {
            ret = new byte[getLength() / 8 + 1];
        } else {
            ret = new byte[getLength() / 8 + 2];
            ret[0] = lastBits;
        }
        int byteIndex = 1, bitIndex = 7;
        for (int i=0 ; i<getLength() ; i++) {
            if (get(i)) ret[byteIndex] |= (byte) (1<<bitIndex);
            bitIndex--;
            if (bitIndex == -1) {
                byteIndex++;
                bitIndex = 7;
            }
        }

        return ret;
    }

    public static BitVector parseBitVector(String s) {
        BitVector ret = new BitVector();
        for (int i=0 ; i<s.length() ; i++) {
            ret.append(s.charAt(i) == '1');
        }
        return ret;
    }
}
