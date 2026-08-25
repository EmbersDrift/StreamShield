package livehider.overlay.platform;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;

/**
 * JNA binding to the Win32 Kernel32 module, used to locate <code>opengl32!wglSwapBuffers</code>.
 * Ported from obs-overlay (license: MIT, author zziger).
 */
public interface Kernel32 extends StdCallLibrary {
    Kernel32 INSTANCE = Native.load("Kernel32", Kernel32.class);

    Pointer GetModuleHandleA(String moduleName);

    Pointer GetProcAddress(Pointer hModule, String procName);
}
