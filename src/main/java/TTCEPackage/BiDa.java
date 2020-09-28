package TTCEPackage;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.ShortByReference;
import com.sun.jna.win32.StdCallLibrary;

public class BiDa {
    public interface CLibrary extends StdCallLibrary {
        // DLL文件默认路径为项目根目录，若DLL文件存放在项目外，请使用绝对路径。（此处：(Platform.isWindows()?"msvcrt":"c")指本地动态库msvcrt.dll）
        CLibrary INSTANCE = (CLibrary) Native.load("F:\\必达门锁\\btlock57L.dll",
                CLibrary.class);

        // 声明将要调用的DLL中的方法,可以是多个方法(此处示例调用本地动态库msvcrt.dll中的printf()方法)
        int Read_Guest_Card(UnsignedByteByReference Port,
                            UnsignedByteByReference ReaderType, UnsignedByteByReference SectorNo,
                            String HotelPwd, IntByReference CardNo,
                            IntByReference GuestSN,IntByReference GuestIdx,
                            String DoorID, String SuitDoor, String PubDoor,
                            String BeginTime,String EndTime);
        //蜂鸣
        Integer Reader_Alarm(UnsignedByteByReference Port, UnsignedByteByReference ReaderType, UnsignedByteByReference AlarmCount);
        //整形流水号
        int SerialNo_FromNow();

        int Bin_Hex(Pointer Dest,String Source,int Len);

        int DBWrite_Guest_Lift(long Port,long ReaderType,long DataBaseType,long BeginAddr,
                               long EndAddr,long MaxLiftAddr,
        String BeginTime,String EndTime,String LiftData,String DataSource,String UserID,String Password,long Brand);
    }
}
