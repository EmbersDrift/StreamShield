package livehider.overlay;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import dev.architectury.platform.Platform;
import livehider.LiveHider;
import livehider.overlay.platform.Kernel32;
import livehider.overlay.platform.MinHook;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Hooks {@code opengl32!wglSwapBuffers} via MinHook so we can run a render pass right at the
 * frame swap. This is the mechanism that lets the player see the overlay while OBS (which grabs
 * the main framebuffer during swap) does not.
 * Ported from obs-overlay (license: MIT, author zziger).
 */
public class OverlayHook {
    private static boolean libraryInitialized = false;
    private static PointerByReference reference;
    private static final List<Handler> handlerList = new ArrayList<>();
    private static MinHook minHook;

    public interface Handler {
        void run();
    }

    public static void subscribe(Handler handler) {
        handlerList.add(handler);
    }

    public static void unsubscribe(Handler handler) {
        handlerList.remove(handler);
    }

    public static MinHook getMinHook() {
        if (minHook == null) {
            minHook = Native.load("MinHook", MinHook.class);
        }
        return minHook;
    }

    public static void init() {
        if (libraryInitialized) {
            return;
        }
        initLibrary();
        initHook();
        libraryInitialized = true;
    }

    private static void initLibrary() {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            throw new OverlayHookException("OBS Overlay is only supported on Windows");
        }

        String arch = System.getProperty("os.arch").toLowerCase();
        if (arch.contains("aarch")) {
            throw new OverlayHookException("OBS Overlay is only supported on x64 and x86 systems");
        }

        boolean is64 = arch.equals("x86_64") || arch.equals("amd64") || arch.equals("x64") || arch.equals("ia64");
        InputStream libFile = LiveHider.class.getResourceAsStream(is64 ? "/lib/MinHook.x64.dll" : "/lib/MinHook.x86.dll");
        if (libFile == null) {
            throw new OverlayHookException("Failed to get MinHook dll");
        }

        File nativeDir = new File(Platform.getGameFolder().toAbsolutePath().toString().concat("/native"));
        File copyLibFile = new File(nativeDir, "MinHook.dll");
        nativeDir.mkdir();

        try (FileOutputStream fos = new FileOutputStream(copyLibFile)) {
            copyLibFile.createNewFile();
            IOUtils.copy(libFile, fos);
        } catch (IOException e) {
            throw new OverlayHookException("Failed to copy dependency dll");
        }

        System.setProperty("jna.library.path", nativeDir.getAbsolutePath());
        LiveHider.LOGGER.info("Copied dependency DLL successfully");
    }

    private static void initHook() {
        Pointer module = Kernel32.INSTANCE.GetModuleHandleA("opengl32.dll");
        Pointer proc = Kernel32.INSTANCE.GetProcAddress(module, "wglSwapBuffers");

        try {
            MinHook hook = getMinHook();
            hook.MH_Initialize();
            reference = new PointerByReference();
            hook.MH_CreateHook(proc, hDc -> {
                for (Handler handler : handlerList) {
                    handler.run();
                }
                Function origFunction = Function.getFunction(reference.getValue(), Function.ALT_CONVENTION);
                return (Boolean) origFunction.invoke(Boolean.class, new Object[]{hDc});
            }, reference);
            hook.MH_EnableHook(proc);
        } catch (Exception e) {
            LiveHider.LOGGER.error("Failed to initialize MinHook", e);
            throw e;
        }
    }
}
