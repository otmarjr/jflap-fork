/*
 * Retrieved from http://www.javamex.com/tutorials/memory/ascii_charsequence.shtml
 */
package automata;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class CompactCharSequence implements CharSequence, Serializable {

    static final long serialVersionUID = 1L;

    private static final String ENCODING = "ISO-8859-1";
    private final int offset;
    private final int end;
    private byte[] data;

    public CompactCharSequence(String str) {
        try {
            data = str.getBytes(ENCODING);
            offset = 0;
            end = data.length;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected: " + ENCODING + " not supported!");
        }
    }
    
    public void addPrefix(String prefix){
        
        byte[] newData = prefix.getBytes();
        byte[] temp = this.data;
        this.data = new byte[newData.length + temp.length];
        
        System.arraycopy(newData,0, this.data,0,newData.length);
        System.arraycopy(temp,0, this.data,newData.length,temp.length);
    }
    
    public void addSuffix(String suffix){
        byte[] temp = this.data;
        byte[] newData = suffix.getBytes();
        this.data = new byte[newData.length + temp.length];
        
        System.arraycopy(temp,0, this.data,0,temp.length);
        System.arraycopy(newData,0, this.data,temp.length,newData.length);
    }
    
    public void concatenateAfter(CompactCharSequence suffix){
        byte[] temp = this.data;
        byte[] newData = suffix.data;
        
        this.data = new byte[newData.length + temp.length];
        
        System.arraycopy(temp,0, this.data,0,temp.length);
        System.arraycopy(newData,0, this.data,temp.length,newData.length);
    }
    
    public boolean endsWith(String text){
        byte[] suffix = text.getBytes();
        
        int comparisonOffset = this.length() - text.length();
        
        if (this.length() < 0){
            return false;
        }
        else{
            for (int i=0;i<text.length();i++){
                if (suffix[i] != this.data[offset+comparisonOffset]){
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidPosition(int index){
        return getIx(index) < end;
    }
    
    private int getIx(int index){
        return index+offset;
    }
    
    public char charAt(int index) {
        if (!isValidPosition(index)) {
            throw new StringIndexOutOfBoundsException("Invalid index "
                    + index + " length " + length());
        }
        return (char) (data[getIx(index)] & 0xff);
    }

    public int length() {
        return end - offset;
    }

    public String toString() {
        try {
            return new String(data, offset, end - offset, ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected: " + ENCODING + " not supported");
        }
    }

    public CharSequence subSequence(int start, int end) {
        if (start < 0 || end >= (this.end - offset)) {
            throw new IllegalArgumentException("Illegal range "
                    + start + "-" + end + " for sequence of length " + length());
        }
        return new CompactCharSequence(data, start + offset, end + offset);
    }
    
    public CompactCharSequence compactSubSequence(int start, int end) {
        return (CompactCharSequence)subSequence(start, end);
    }
    
    public CompactCharSequence compactSubSequence(int start) {
        return (CompactCharSequence)subSequence(start, end - offset -1);
    }

    private CompactCharSequence(byte[] data, int offset, int end) {
        this.data = data;
        this.offset = offset;
        this.end = end;
    }
    
    @Override
    public boolean equals(Object other){
        if (!(other instanceof CompactCharSequence)){
            return false;
        }
        
        CompactCharSequence otherCS = (CompactCharSequence)other;
        
        if (otherCS.length() != this.length())
            return false;
        
        for (int i=0;i<this.length();i++){
            if (otherCS.charAt(i) != this.charAt(i))
                return false;
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Arrays.hashCode(this.data);
        return hash;
    }
}
