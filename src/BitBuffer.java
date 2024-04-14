public class BitBuffer {
    private byte[] buffer, overflow;
    private final int bufferLength;
    private int byteIndex, bitIndex;

    public BitBuffer(int capacity) {
        buffer = new byte[capacity];
        bufferLength = capacity;
        byteIndex = 0;
        bitIndex = 7;
        overflow = null;
    }

    public void appendBits(BitVector bitVector) {
        for (int i=0 ; i<bitVector.getLength() ; i++) {
            try { // An exception could occur where we had reached the end of the buffer bytes
                if (bitVector.get(i)) {
                    buffer[byteIndex] |= (byte) (1 << bitIndex);
                }
                bitIndex--;
            } catch (ArrayIndexOutOfBoundsException e) { // The exception is caught and the overflow bits are written
                putOverflow(bitVector, i);
                return;
            }

            if (bitIndex == -1) {
                byteIndex++;
                bitIndex = 7;
            }
        }
    }

    public byte[] getFullBuffer() {
        assert hasOverflow();
        byte[] ret = buffer.clone();
        buffer = new byte[bufferLength];
        System.arraycopy(overflow, 0, buffer, 0, overflow.length);
        byteIndex -= bufferLength;
        overflow = null;
        return ret;
    }

    public byte[] getFromBuffer() {
        assert !hasOverflow();
        byte[] ret = (bitIndex == 7)? new byte[byteIndex] : new byte[byteIndex+1];
        System.arraycopy(buffer, 0, ret, 0, (bitIndex == 7)? byteIndex : byteIndex+1);
        buffer = new byte[bufferLength];
        byteIndex = 0;
        bitIndex = 7;
        return ret;
    }

    private void putOverflow(BitVector bitVector, int fromIndex) {
        // Calculate how long the overflow buffer should be
        overflow = new byte[(bitVector.getLength() - fromIndex)/8 + 1];
        // Fill the overflow array (no try catch here because the number of needed bytes is computed)
        for (int i=fromIndex ; i<bitVector.getLength() ; i++) {
            if (bitVector.get(i)) overflow[byteIndex-bufferLength] |= (byte) (1 << bitIndex);
            bitIndex--;
            if (bitIndex == -1) {
                byteIndex++;
                bitIndex = 7;
            }
        }
    }

    public boolean hasOverflow() {
        return overflow != null;
    }

    public boolean isEmpty() {
        return byteIndex == 0 && bitIndex == 7;
    }
}
