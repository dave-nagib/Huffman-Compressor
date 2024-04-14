import java.util.Arrays;

public record ByteWord(byte[] bytes) {

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ByteWord byteWord = (ByteWord) o;
        return Arrays.equals(bytes, byteWord.bytes);
    }
}
