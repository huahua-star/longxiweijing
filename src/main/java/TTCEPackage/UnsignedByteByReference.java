package TTCEPackage;

import com.sun.jna.ptr.ByReference;

public class UnsignedByteByReference extends ByReference {

    public UnsignedByteByReference() {
        this((byte)0);
    }

    public UnsignedByteByReference(byte value) {
        super(1);
        setValue(value);
    }

    public void setValue(byte value) {
        getPointer().setByte(0, value);
    }

    public int getValue() {
        byte b = getPointer().getByte(0);
        if(b<0){
            return getPointer().getByte(0)&0xff;
        }
        return getPointer().getByte(0);
    }
}
