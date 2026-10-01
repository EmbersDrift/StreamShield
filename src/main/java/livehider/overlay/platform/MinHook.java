package livehider.overlay.platform;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.StdCallLibrary.StdCallCallback;

/**
 * JNA binding to the MinHook native library (bundled in resources, see {@code lib/MinHook.*.dll}).
 * Ported from obs-overlay (license: MIT, author zziger).
 */
public interface MinHook extends StdCallLibrary {
    int MH_Initialize();

    int MH_CreateHook(Pointer pTarget, MinHook.wglSwapBuffers callback, PointerByReference ppDetour);

    int MH_EnableHook(Pointer pTarget);

    int MH_DisableHook(Pointer pTarget);

    int MH_RemoveHook(Pointer pTarget);

    interface wglSwapBuffers extends StdCallCallback {
        boolean callback(Pointer hDc);
    }
}
