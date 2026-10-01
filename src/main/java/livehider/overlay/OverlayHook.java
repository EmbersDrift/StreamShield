package livehider.overlay;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import livehider.LiveHider;
import livehider.overlay.platform.Kernel32;
import livehider.overlay.platform.MinHook;
import dev.architectury.platform.Platform;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/** Owns only StreamShield's swap hook, keeping its callback alive while native code can call it. */
public final class OverlayHook {
    private static final CopyOnWriteArrayList<Handler> HANDLERS = new CopyOnWriteArrayList<>();
    private static MinHook minHook;
    private static MinHook.wglSwapBuffers callback;
    private static Function originalFunction;
    private static Pointer target;
    private static boolean created;
    private static boolean enabled;

    private OverlayHook() {}

    public interface Handler { void run(); }

    public static boolean isPlatformSupported() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        return os.startsWith("windows") && (arch.equals("amd64") || arch.equals("x86_64")
            || arch.equals("x64") || arch.equals("x86") || arch.equals("i386"));
    }

    public static void subscribe(Handler handler) { HANDLERS.addIfAbsent(handler); }
    public static void unsubscribe(Handler handler) { HANDLERS.remove(handler); }

    public static synchronized void init() {
        if (enabled) return;
        if (!isPlatformSupported()) throw new OverlayHookException("UNSUPPORTED_PLATFORM");
        if (created) throw new OverlayHookException("MH_REMOVE_FAILED");
        try {
            if (minHook == null) loadLibrary();
            // MH_ERROR_ALREADY_INITIALIZED=1 can belong to another user of the same MinHook library.
            int status = minHook.MH_Initialize();
            if (status != 0 && status != 1) throw new OverlayHookException("MH_INITIALIZE_FAILED");
            Pointer module = Kernel32.INSTANCE.GetModuleHandleA("opengl32.dll");
            if (module == null) throw new OverlayHookException("NATIVE_SYMBOL_MISSING");
            target = Kernel32.INSTANCE.GetProcAddress(module, "wglSwapBuffers");
            if (target == null) throw new OverlayHookException("NATIVE_SYMBOL_MISSING");
            PointerByReference original = new PointerByReference();
            callback = hDc -> {
                try {
                    for (Handler handler : HANDLERS) handler.run();
                } catch (Throwable error) {
                    // Never allow a Java rendering exception to skip the original native swap.
                    try {
                        LiveHider.reportOverlayFailure("COMPOSITE_FAILED");
                    } catch (Throwable ignored) {
                        // The native boundary must still forward the call even if notification fails.
                    }
                }
                return originalFunction.invokeInt(new Object[] { hDc }) != 0;
            };
            status = minHook.MH_CreateHook(target, callback, original);
            if (status != 0) throw new OverlayHookException("MH_CREATE_FAILED");
            created = true;
            if (original.getValue() == null) throw new OverlayHookException("NATIVE_SYMBOL_MISSING");
            originalFunction = Function.getFunction(original.getValue(), Function.ALT_CONVENTION);
            status = minHook.MH_EnableHook(target);
            if (status != 0) throw new OverlayHookException("MH_ENABLE_FAILED");
            enabled = true;
        } catch (RuntimeException | LinkageError error) {
            close();
            if (error instanceof OverlayHookException known) throw known;
            throw new OverlayHookException("NATIVE_LIBRARY_FAILED");
        }
    }

    private static void loadLibrary() {
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        boolean is64 = arch.equals("amd64") || arch.equals("x86_64") || arch.equals("x64");
        String resource = is64 ? "/lib/MinHook.x64.dll" : "/lib/MinHook.x86.dll";
        try (InputStream stream = OverlayHook.class.getResourceAsStream(resource)) {
            if (stream == null) throw new OverlayHookException("NATIVE_LIBRARY_FAILED");
            Path directory = Platform.getGameFolder().resolve("native").resolve("streamshield");
            Files.createDirectories(directory);
            Path library = directory.resolve(is64 ? "MinHook.x64.dll" : "MinHook.x86.dll");
            byte[] bytes = stream.readAllBytes();
            if (!Files.exists(library) || !Arrays.equals(Files.readAllBytes(library), bytes)) {
                Files.write(library, bytes);
            }
            // Use an explicit path rather than replacing the process-wide jna.library.path.
            minHook = Native.load(library.toAbsolutePath().toString(), MinHook.class);
        } catch (Exception error) {
            throw new OverlayHookException("NATIVE_LIBRARY_FAILED");
        }
    }

    public static synchronized void close() {
        HANDLERS.clear();
        if (!created) return;
        if (enabled && minHook.MH_DisableHook(target) != 0) {
            LiveHider.LOGGER.warn("StreamShield hook cleanup failed; retaining native callback.");
            return;
        }
        enabled = false;
        if (minHook.MH_RemoveHook(target) != 0) {
            LiveHider.LOGGER.warn("StreamShield hook removal failed; retaining native callback.");
            return;
        }
        created = false;
        callback = null;
        originalFunction = null;
        target = null;
        // Do not MH_Uninitialize: other mods may share the MinHook library.
    }
}
